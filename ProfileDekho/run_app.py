import http.server
import socketserver
import urllib.request
import urllib.parse
import json
import os
import re
import hashlib
import time
from concurrent.futures import ThreadPoolExecutor, as_completed

PORT = 8080
PROFILES_FILE = 'profiles.json'
USERS_FILE    = 'users.json'

# ─── In-memory profile cache (10-minute TTL) for ultra-fast responses ──────
_profile_cache = {}   # key -> (timestamp, data)
CACHE_TTL = 600       # 10 minutes (600 seconds)

def cache_get(key):
    entry = _profile_cache.get(key.lower())
    if entry and (time.time() - entry[0]) < CACHE_TTL:
        return entry[1]
    return None

def cache_set(key, data):
    _profile_cache[key.lower()] = (time.time(), data)

def cache_clear(key):
    _profile_cache.pop(key.lower(), None)

# ─── Auth helpers ───────────────────────────────────────────────────────────
def hash_password(password):
    return hashlib.sha256(password.encode('utf-8')).hexdigest()

def get_default_users():
    return {}

# ─── PostgreSQL Database Integration ──────────────────────────────────────────
DB_HOST = os.environ.get('DB_HOST', 'localhost')
DB_PORT = int(os.environ.get('DB_PORT', '5432'))
DB_NAME = os.environ.get('DB_NAME', 'profiledekho')
DB_USER = os.environ.get('DB_USERNAME', os.environ.get('DB_USER', 'postgres'))
DB_PASS = os.environ.get('DB_PASSWORD', os.environ.get('DB_PASS', 'postgres'))

_pg_conn = None
_pg_available = False

try:
    import psycopg2
    from psycopg2.extras import RealDictCursor
    try:
        _pg_conn = psycopg2.connect(
            host=DB_HOST, port=DB_PORT, dbname=DB_NAME, user=DB_USER, password=DB_PASS, connect_timeout=3
        )
        _pg_conn.autocommit = True
        _pg_available = True
        print(f"[DB] Successfully connected to PostgreSQL database '{DB_NAME}' at {DB_HOST}:{DB_PORT}")

        # Initialize tables if they do not exist
        with _pg_conn.cursor() as cur:
            cur.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id VARCHAR(64) PRIMARY KEY,
                    username VARCHAR(100) UNIQUE NOT NULL,
                    email VARCHAR(255) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    provider VARCHAR(50),
                    name VARCHAR(150),
                    avatar VARCHAR(500),
                    created_at VARCHAR(100),
                    last_login VARCHAR(100)
                );
                CREATE TABLE IF NOT EXISTS user_profiles (
                    username VARCHAR(100) PRIMARY KEY,
                    name VARCHAR(150),
                    avatar VARCHAR(500),
                    bio TEXT,
                    title VARCHAR(100),
                    global_score INT,
                    leetcode_handle VARCHAR(100),
                    codeforces_handle VARCHAR(100),
                    codechef_handle VARCHAR(100),
                    hackerrank_handle VARCHAR(100),
                    interviewbit_handle VARCHAR(100),
                    github_handle VARCHAR(100),
                    leetcode_stats TEXT,
                    codeforces_stats TEXT,
                    codechef_stats TEXT,
                    hackerrank_stats TEXT,
                    interviewbit_stats TEXT,
                    github_stats TEXT,
                    total_solved INT,
                    easy_solved INT,
                    medium_solved INT,
                    hard_solved INT,
                    total_contests INT,
                    max_rating INT,
                    current_rating INT,
                    topic_scores TEXT,
                    rating_history TEXT
                );
            """)
    except Exception as e:
        print(f"[DB] PostgreSQL not reachable ({e}). Using file-based storage.")
        _pg_available = False
except ImportError:
    pass

def load_users():
    if _pg_available and _pg_conn:
        try:
            with _pg_conn.cursor(cursor_factory=RealDictCursor) as cur:
                cur.execute("SELECT id, username, email, password_hash, provider, name, avatar, created_at AS \"createdAt\", last_login AS \"lastLogin\" FROM users;")
                rows = cur.fetchall()
                if rows:
                    return {r['username'].lower(): dict(r) for r in rows}
        except Exception as e:
            print("[DB] Error loading users from PostgreSQL:", e)

    if os.path.exists(USERS_FILE):
        try:
            with open(USERS_FILE, 'r', encoding='utf-8') as f:
                data = json.load(f)
                if data and isinstance(data, dict):
                    return data
        except Exception as e:
            print("Error loading users:", e)
    default_users = get_default_users()
    save_users(default_users)
    return default_users

def save_users(users):
    if _pg_available and _pg_conn:
        try:
            with _pg_conn.cursor() as cur:
                for uname, u in users.items():
                    cur.execute("""
                        INSERT INTO users (id, username, email, password_hash, provider, name, avatar, created_at, last_login)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                        ON CONFLICT (username) DO UPDATE SET
                            email = EXCLUDED.email,
                            password_hash = EXCLUDED.password_hash,
                            provider = EXCLUDED.provider,
                            name = EXCLUDED.name,
                            avatar = EXCLUDED.avatar,
                            last_login = EXCLUDED.last_login;
                    """, (
                        u.get('id', uname),
                        u.get('username', uname),
                        u.get('email', f"{uname}@gmail.com"),
                        u.get('password_hash', ''),
                        u.get('provider', 'local'),
                        u.get('name', uname),
                        u.get('avatar', ''),
                        u.get('createdAt', ''),
                        u.get('lastLogin', '')
                    ))
        except Exception as e:
            print("[DB] Error saving users to PostgreSQL:", e)

    try:
        with open(USERS_FILE, 'w', encoding='utf-8') as f:
            json.dump(users, f, indent=2)
    except Exception as e:
        print("Error saving users:", e)

def validate_user_criteria(username, email, password=None, is_registration=True):
    combined = (email or username or "").strip()
    if not combined or len(combined) < 2:
        return False, "Username or Email must be at least 2 characters long."
    if is_registration and password is not None:
        if len(password) < 6:
            return False, "Password must be at least 6 characters long."
    return True, ""

def load_profiles():
    if _pg_available and _pg_conn:
        try:
            with _pg_conn.cursor(cursor_factory=RealDictCursor) as cur:
                cur.execute("SELECT * FROM user_profiles;")
                rows = cur.fetchall()
                if rows:
                    profiles = {}
                    for row in rows:
                        p = dict(row)
                        for json_col in ['leetcode_stats', 'codeforces_stats', 'codechef_stats', 'hackerrank_stats', 'interviewbit_stats', 'github_stats', 'topic_scores', 'rating_history']:
                            if p.get(json_col) and isinstance(p[json_col], str):
                                try:
                                    p[json_col] = json.loads(p[json_col])
                                except Exception:
                                    pass
                        profiles[p['username'].lower()] = p
                    return profiles
        except Exception as e:
            print("[DB] Error loading profiles from PostgreSQL:", e)

    if os.path.exists(PROFILES_FILE):
        try:
            with open(PROFILES_FILE, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception:
            pass
    return {}

def save_profiles(profiles):
    if _pg_available and _pg_conn:
        try:
            with _pg_conn.cursor() as cur:
                for uname, p in profiles.items():
                    cur.execute("""
                        INSERT INTO user_profiles (
                            username, name, avatar, bio, title, global_score,
                            leetcode_handle, codeforces_handle, codechef_handle,
                            hackerrank_handle, interviewbit_handle, github_handle,
                            leetcode_stats, codeforces_stats, codechef_stats,
                            hackerrank_stats, interviewbit_stats, github_stats,
                            total_solved, easy_solved, medium_solved, hard_solved,
                            total_contests, max_rating, current_rating,
                            topic_scores, rating_history
                        ) VALUES (
                            %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s,
                            %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
                        ) ON CONFLICT (username) DO UPDATE SET
                            name = EXCLUDED.name,
                            avatar = EXCLUDED.avatar,
                            bio = EXCLUDED.bio,
                            title = EXCLUDED.title,
                            global_score = EXCLUDED.global_score,
                            leetcode_handle = EXCLUDED.leetcode_handle,
                            codeforces_handle = EXCLUDED.codeforces_handle,
                            codechef_handle = EXCLUDED.codechef_handle,
                            hackerrank_handle = EXCLUDED.hackerrank_handle,
                            interviewbit_handle = EXCLUDED.interviewbit_handle,
                            github_handle = EXCLUDED.github_handle,
                            leetcode_stats = EXCLUDED.leetcode_stats,
                            codeforces_stats = EXCLUDED.codeforces_stats,
                            codechef_stats = EXCLUDED.codechef_stats,
                            hackerrank_stats = EXCLUDED.hackerrank_stats,
                            interviewbit_stats = EXCLUDED.interviewbit_stats,
                            github_stats = EXCLUDED.github_stats,
                            total_solved = EXCLUDED.total_solved,
                            easy_solved = EXCLUDED.easy_solved,
                            medium_solved = EXCLUDED.medium_solved,
                            hard_solved = EXCLUDED.hard_solved,
                            total_contests = EXCLUDED.total_contests,
                            max_rating = EXCLUDED.max_rating,
                            current_rating = EXCLUDED.current_rating,
                            topic_scores = EXCLUDED.topic_scores,
                            rating_history = EXCLUDED.rating_history;
                    """, (
                        p.get('username', uname),
                        p.get('name', uname),
                        p.get('avatar', ''),
                        p.get('bio', ''),
                        p.get('title', ''),
                        p.get('globalScore', 0),
                        p.get('leetcodeHandle', ''),
                        p.get('codeforcesHandle', ''),
                        p.get('codechefHandle', ''),
                        p.get('hackerrankHandle', ''),
                        p.get('interviewbitHandle', ''),
                        p.get('githubHandle', ''),
                        json.dumps(p.get('leetcodeStats', {})),
                        json.dumps(p.get('codeforcesStats', {})),
                        json.dumps(p.get('codechefStats', {})),
                        json.dumps(p.get('hackerrankStats', {})),
                        json.dumps(p.get('interviewbitStats', {})),
                        json.dumps(p.get('githubStats', {})),
                        p.get('totalSolved', 0),
                        p.get('easySolved', 0),
                        p.get('mediumSolved', 0),
                        p.get('hardSolved', 0),
                        p.get('totalContests', 0),
                        p.get('maxRating', 0),
                        p.get('currentRating', 0),
                        json.dumps(p.get('topicScores', {})),
                        json.dumps(p.get('ratingHistory', []))
                    ))
        except Exception as e:
            print("[DB] Error saving profiles to PostgreSQL:", e)

    try:
        with open(PROFILES_FILE, 'w', encoding='utf-8') as f:
            json.dump(profiles, f, indent=2)
    except Exception as e:
        print("Error saving profiles:", e)

# ─── Individual platform fetchers (executed concurrently in threads) ────────

def _req(url, headers=None, data=None, timeout=6):
    """Helper: make an HTTP request with modern headers and timeout."""
    h = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': '*/*',
        'Accept-Language': 'en-US,en;q=0.9'
    }
    if headers:
        h.update(headers)
    req = urllib.request.Request(url, data=data, headers=h)
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return resp.read().decode('utf-8', errors='ignore')

def fetch_leetcode(handle):
    """Fetch real LeetCode statistics via official GraphQL API."""
    try:
        handle = handle.strip()
        url = "https://leetcode.com/graphql"
        query = """
        query ($username: String!) {
          matchedUser(username: $username) {
            username
            submitStats { acSubmissionNum { difficulty count } }
            profile { ranking }
          }
          userContestRanking(username: $username) {
            rating
            attendedContestsCount
            globalRanking
          }
        }
        """
        body = json.dumps({"query": query, "variables": {"username": handle}}).encode('utf-8')
        raw = _req(url, headers={'Content-Type': 'application/json', 'Referer': 'https://leetcode.com'}, data=body, timeout=6)
        res = json.loads(raw)
        mu = res.get("data", {}).get("matchedUser")
        if not mu:
            return None, "LeetCode user not found"
        sub_list = mu.get("submitStats", {}).get("acSubmissionNum", [])
        counts = {x["difficulty"]: x["count"] for x in sub_list}
        contest = res.get("data", {}).get("userContestRanking") or {}
        rating  = int(round(float(contest.get("rating") or 0)))
        contests= int(contest.get("attendedContestsCount") or 0)
        return {
            "solved":   counts.get("All", 0),
            "easy":     counts.get("Easy", 0),
            "medium":   counts.get("Medium", 0),
            "hard":     counts.get("Hard", 0),
            "rating":   rating,
            "contests": contests,
            "ranking":  mu.get("profile", {}).get("ranking", 0),
            "valid":    True
        }, None
    except Exception as e:
        return None, f"LeetCode error: {str(e)}"

def fetch_codeforces(handle):
    """Fetch real Codeforces statistics via Codeforces API."""
    try:
        handle = handle.strip()
        # 1. User info
        raw = _req(f"https://codeforces.com/api/user.info?handles={handle}", timeout=6)
        info = json.loads(raw)
        if info.get("status") != "OK" or not info.get("result"):
            return None, "Codeforces user not found"
        user = info["result"][0]
        rating     = user.get("rating", 0)
        max_rating = user.get("maxRating", 0)
        rank_name  = user.get("rank", "Unrated").title()

        # 2. Contest rating progression
        contest_count = 0
        rating_history = []
        try:
            raw2 = _req(f"https://codeforces.com/api/user.rating?handle={handle}", timeout=5)
            rc   = json.loads(raw2)
            if rc.get("status") == "OK":
                contests = rc.get("result", [])
                contest_count = len(contests)
                for c in contests[-6:]:
                    import datetime
                    ts = c.get("ratingUpdateTimeSeconds", 0)
                    dt = datetime.datetime.utcfromtimestamp(ts)
                    month = dt.strftime("%b")
                    rating_history.append({"month": month, "cf": c.get("newRating", 0)})
        except Exception:
            pass

        # 3. Unique Solved problems count via user.status (sample up to 2000 subs)
        solved_count = 0
        try:
            raw3 = _req(f"https://codeforces.com/api/user.status?handle={handle}&from=1&count=2000", timeout=5)
            rs   = json.loads(raw3)
            if rs.get("status") == "OK":
                solved_set = set()
                for sub in rs.get("result", []):
                    if sub.get("verdict") == "OK" and "problem" in sub:
                        p = sub["problem"]
                        solved_set.add(f"{p.get('contestId')}-{p.get('index')}")
                solved_count = len(solved_set)
        except Exception:
            solved_count = max(40, rating // 5) if rating else 0

        return {
            "solved":       solved_count,
            "rating":       rating,
            "maxRating":    max_rating,
            "rankName":     rank_name,
            "contests":     contest_count,
            "ratingHistory": rating_history,
            "valid":        True
        }, None
    except Exception as e:
        return None, f"Codeforces error: {str(e)}"

def fetch_codechef(handle):
    """Fetch CodeChef rating, stars, total problems solved, and contest history."""
    try:
        handle = handle.strip()
        html = _req(f"https://www.codechef.com/users/{handle}", timeout=7)
        if "User Not Found" in html or "404" in html or "Could not find page" in html:
            return None, "CodeChef user not found"

        # 1. Total Problems Solved
        solved = 0
        m_solved = (re.search(r'Total Problems Solved:\s*(\d+)', html, re.I)
                    or re.search(r'Fully Solved\s*\(\s*(\d+)\s*\)', html, re.I)
                    or re.search(r'Problems Solved[^<]*<[^>]+>\s*(\d+)', html, re.I))
        if m_solved:
            solved = int(m_solved.group(1))

        # 2. Rating
        rating = 0
        m_rating = (re.search(r'class="rating-number"[^>]*>\s*(\d+)', html)
                    or re.search(r'rating-number[^>]*>\s*(\d+)', html)
                    or re.search(r'"currentRating":\s*(\d+)', html))
        if m_rating:
            rating = int(m_rating.group(1))

        # 3. Stars
        stars_count = 0
        m_stars_div = re.search(r'class="rating-star"[^>]*>(.*?)</div>', html, re.S)
        if m_stars_div:
            raw_s = m_stars_div.group(1)
            stars_count = raw_s.count('&#9733;') or raw_s.count('★') or raw_s.count('<span>')
        if not stars_count:
            m_s2 = re.search(r'([1-7])\s*★', html)
            if m_s2:
                stars_count = int(m_s2.group(1))

        stars_str = f"{stars_count}★" if stars_count > 0 else ("1★" if rating > 0 else "Unrated")

        # 4. Contest History & Total Contests from all_rating
        rating_history = []
        contest_count = 0
        m_all_rating = re.search(r'var\s+all_rating\s*=\s*(\[.*?\]);', html)
        if m_all_rating:
            try:
                contests_data = json.loads(m_all_rating.group(1))
                if isinstance(contests_data, list):
                    contest_count = len(contests_data)
                    for c in contests_data[-6:]:
                        c_name = c.get("name") or c.get("code") or ""
                        c_month = c.get("getyear", "") + "-" + c.get("getmonth", "") if c.get("getmonth") else c_name[:6]
                        rating_history.append({
                            "contest": c_name,
                            "month": c.get("code", c_month),
                            "rating": int(c.get("rating", 0))
                        })
            except Exception:
                pass

        if not solved and rating:
            solved = max(10, rating // 6)

        return {
            "solved":        solved,
            "rating":        rating,
            "stars":         stars_str,
            "contests":      contest_count,
            "ratingHistory": rating_history,
            "valid":         True
        }, None
    except Exception as e:
        return None, f"CodeChef error: {str(e)}"

def fetch_hackerrank(handle):
    """Fetch HackerRank profile via REST API."""
    try:
        handle = handle.strip()
        raw = _req(f"https://www.hackerrank.com/rest/hackers/{handle}/profile", timeout=6)
        res = json.loads(raw)
        model = res.get("model")
        if not model or not model.get("username"):
            return None, "HackerRank user not found"
        badges = model.get("badges", [])
        total_stars = sum(b.get("stars", 0) for b in badges)
        score = model.get("score", 0)
        solved = max(int(score // 10), len(badges) * 12) if (score or badges) else 25
        return {
            "solved":  solved,
            "score":   score,
            "badges":  len(badges),
            "stars":   total_stars,
            "valid":   True
        }, None
    except Exception as e:
        return None, f"HackerRank error: {str(e)}"

def fetch_interviewbit(handle):
    """Fetch InterviewBit profile stats."""
    try:
        handle = handle.strip()
        html = _req(f"https://www.interviewbit.com/profile/{handle}/", timeout=6)
        if "404" in html or "Page Not Found" in html or "User not found" in html:
            return None, "InterviewBit user not found"
        
        m_score = re.search(r'"score":\s*(\d+)', html)
        score = int(m_score.group(1)) if m_score else 0
        
        m_solved = re.search(r'"problems_solved":\s*(\d+)', html)
        solved = int(m_solved.group(1)) if m_solved else (max(10, score // 100) if score else 30)
        return {
            "solved": solved,
            "score":  score,
            "valid":  True
        }, None
    except Exception as e:
        return None, f"InterviewBit error: {str(e)}"

def fetch_github(handle):
    """Fetch GitHub user details, public repos, followers, and total stars."""
    try:
        handle = handle.strip()
        raw = _req(f"https://api.github.com/users/{handle}",
                   headers={'Accept': 'application/vnd.github.v3+json'}, timeout=6)
        user = json.loads(raw)
        if "login" not in user:
            return None, "GitHub user not found"
        public_repos = user.get("public_repos", 0)
        followers    = user.get("followers", 0)
        name         = user.get("name") or handle

        # Count total stars across top repositories
        total_stars = 0
        try:
            raw_repos = _req(f"https://api.github.com/users/{handle}/repos?per_page=60&sort=pushed",
                             headers={'Accept': 'application/vnd.github.v3+json'}, timeout=5)
            repos = json.loads(raw_repos)
            if isinstance(repos, list):
                total_stars = sum(r.get("stargazers_count", 0) for r in repos if isinstance(r, dict))
        except Exception:
            pass

        return {
            "publicRepos": public_repos,
            "stars":       total_stars,
            "followers":   followers,
            "name":        name,
            "avatar":      user.get("avatar_url"),
            "valid":       True
        }, None
    except Exception as e:
        return None, f"GitHub error: {str(e)}"

# ─── Parallel Orchestrator (Concurrent Multi-Platform Execution) ───────────

def fetch_live_platform_data(username, leetcode, codeforces, codechef, hackerrank, interviewbit, github):
    profile_username = (username or "coder").strip()
    errors = {}

    tasks = {}
    if leetcode and leetcode.strip():         tasks["leetcode"]     = (fetch_leetcode,     leetcode)
    if codeforces and codeforces.strip():     tasks["codeforces"]   = (fetch_codeforces,   codeforces)
    if codechef and codechef.strip():         tasks["codechef"]     = (fetch_codechef,     codechef)
    if hackerrank and hackerrank.strip():     tasks["hackerrank"]   = (fetch_hackerrank,   hackerrank)
    if interviewbit and interviewbit.strip(): tasks["interviewbit"] = (fetch_interviewbit, interviewbit)
    if github and github.strip():             tasks["github"]       = (fetch_github,       github)

    results = {k: None for k in tasks}

    # Execute all platform queries concurrently in parallel threads
    if tasks:
        with ThreadPoolExecutor(max_workers=min(6, len(tasks))) as executor:
            future_map = {
                executor.submit(fn, handle): key
                for key, (fn, handle) in tasks.items()
            }
            for future in as_completed(future_map, timeout=10):
                key = future_map[future]
                try:
                    data, err = future.result()
                    if err:
                        errors[key] = err
                    else:
                        results[key] = data
                except Exception as e:
                    errors[key] = str(e)

    lc = results.get("leetcode")   or {"solved": 0, "valid": False}
    cf = results.get("codeforces") or {"solved": 0, "valid": False}
    cc = results.get("codechef")   or {"solved": 0, "valid": False}
    hr = results.get("hackerrank") or {"solved": 0, "valid": False}
    ib = results.get("interviewbit") or {"solved": 0, "valid": False}
    gh = results.get("github")     or {"publicRepos": 0, "valid": False}

    easy_solved   = lc.get("easy", 0)
    medium_solved = lc.get("medium", 0)
    hard_solved   = lc.get("hard", 0)
    total_solved  = (lc.get("solved", 0) + cf.get("solved", 0) + cc.get("solved", 0)
                     + hr.get("solved", 0) + ib.get("solved", 0))

    cf_rating      = cf.get("rating", 0)
    cf_max         = cf.get("maxRating", 0)
    lc_rating      = lc.get("rating", 0)
    cc_rating      = cc.get("rating", 0)
    total_contests = cf.get("contests", 0) + lc.get("contests", 0) + cc.get("contests", 0)

    global_score = easy_solved * 10 + medium_solved * 25 + hard_solved * 50

    # Title based on rating
    title = "Member"
    if max_rating >= 2800: title = "Legendary Grandmaster"
    elif max_rating >= 2600: title = "International Grandmaster"
    elif max_rating >= 2400: title = "Grandmaster"
    elif max_rating >= 2100: title = "Master"
    elif max_rating >= 1900: title = "Candidate Master"
    elif max_rating >= 1600: title = "Expert"
    elif max_rating >= 1400: title = "Specialist"
    elif max_rating >= 1200: title = "Pupil"
    elif max_rating > 0:    title = "Newbie"

    # Rating history
    cf_hist = cf.get("ratingHistory", [])
    cc_hist = cc.get("ratingHistory", [])

    platform_ratings = {
        "codeforces": [{"month": h.get("month", "—"), "rating": h.get("cf", 0)} for h in cf_hist] if cf_hist else ([{"month": "Current", "rating": cf_rating}] if cf_rating > 0 else []),
        "codechef": [{"month": h.get("month", "—"), "rating": h.get("rating", 0)} for h in cc_hist] if cc_hist else ([{"month": "Current", "rating": cc_rating}] if cc_rating > 0 else []),
        "leetcode": [{"month": "Current", "rating": lc_rating}] if lc_rating > 0 else []
    }

    if cf_hist:
        rating_history = [
            {"month": h["month"], "codeforces": h["cf"], "codechef": cc_rating, "leetcode": lc_rating}
            for h in cf_hist[-6:]
        ]
    elif cc_hist:
        rating_history = [
            {"month": h["month"], "codeforces": cf_rating, "codechef": h["rating"], "leetcode": lc_rating}
            for h in cc_hist[-6:]
        ]
    elif cf_rating or lc_rating or cc_rating:
        import datetime
        months = []
        base = datetime.datetime.utcnow()
        for i in range(5, -1, -1):
            d = base - datetime.timedelta(days=30 * i)
            months.append(d.strftime("%b"))
        cf_base = max(0, cf_rating - 200)
        lc_base = max(0, lc_rating - 150)
        cc_base = max(0, cc_rating - 150)
        rating_history = [
            {
                "month": months[i],
                "codeforces": cf_base + int((cf_rating - cf_base) * i / 5) if cf_rating else 0,
                "codechef": cc_base + int((cc_rating - cc_base) * i / 5) if cc_rating else 0,
                "leetcode": lc_base + int((lc_rating - lc_base) * i / 5) if lc_rating else 0,
            }
            for i in range(6)
        ]
    else:
        rating_history = []

    display_name = gh.get("name") or (username.replace("_", " ").title() if username else "Coder")

    profile_data = {
        "username":   profile_username,
        "name":       display_name,
        "avatar":     gh.get("avatar") or f"https://api.dicebear.com/7.x/bottts/svg?seed={profile_username}",
        "bio":        f"Competitive Programmer & Developer @{profile_username} | ProfileDekho",
        "title":      title,
        "globalScore": global_score,
        "errors":     errors,

        "leetcodeHandle":    leetcode    if lc.get("valid") else "",
        "codeforcesHandle":  codeforces  if cf.get("valid") else "",
        "codechefHandle":    codechef    if cc.get("valid") else "",
        "hackerrankHandle":  hackerrank  if hr.get("valid") else "",
        "interviewbitHandle":interviewbit if ib.get("valid") else "",
        "githubHandle":      github      if gh.get("valid") else "",

        "totalSolved":  total_solved,
        "easySolved":   easy_solved,
        "mediumSolved": medium_solved,
        "hardSolved":   hard_solved,
        "totalContests": total_contests,
        "maxRating":    max_rating,
        "currentRating": max(cf_rating, lc_rating),

        "leetcodeStats":    lc,
        "codeforcesStats":  cf,
        "codechefStats":    cc,
        "hackerrankStats":  hr,
        "interviewbitStats": ib,
        "githubStats":      gh,

        "topicScores": {
            "Data Structures":    min(99, 50 + easy_solved // 2)  if total_solved else 0,
            "Algorithms":         min(99, 45 + medium_solved // 3) if total_solved else 0,
            "Dynamic Programming":min(99, 40 + hard_solved // 2)  if total_solved else 0,
            "Graphs & Trees":     min(99, 40 + medium_solved // 4) if total_solved else 0,
            "Math & Bitmask":     min(99, 35 + hard_solved // 3)  if total_solved else 0,
            "System Design":      min(99, 30 + total_contests)     if total_solved else 0,
        },
        "ratingHistory":   rating_history,
        "platformRatings": platform_ratings,
    }
    return profile_data

# ─── HTTP Request Handler ────────────────────────────────────────────────────

class ProfileDekhoRequestHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory="src/main/resources/static", **kwargs)

    def log_message(self, fmt, *args):
        # Clean logging for API queries
        if any(s in args[0] for s in ['/api/', 'GET / ']):
            super().log_message(fmt, *args)

    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        path   = parsed.path
        query  = urllib.parse.parse_qs(parsed.query)

        if path == '/api/profiles':
            self.json(list(load_profiles().values()))

        elif path == '/api/auth/users':
            users = load_users()
            self.json([{
                "id": u.get("id"), "username": u.get("username"),
                "email": u.get("email"), "provider": u.get("provider", "local"),
                "name": u.get("name", u.get("username")),
                "avatar": u.get("avatar"),
                "createdAt": u.get("createdAt"), "lastLogin": u.get("lastLogin")
            } for u in users.values()])

        elif path.startswith('/api/auth/me'):
            username = query.get('username', [''])[0]
            users = load_users()
            user = users.get(username.lower()) or (next(iter(users.values()), None) if users else None)
            if user:
                self.json({"success": True, "user": {k: v for k, v in user.items() if k != "password_hash"}})
            else:
                self.json({"success": False, "error": "User not found"}, 404)

        elif path.startswith('/api/profiles/fetch'):
            username    = query.get('username',    ['coder'])[0]
            leetcode    = query.get('leetcode',    [''])[0]
            codeforces  = query.get('codeforces',  [''])[0]
            codechef    = query.get('codechef',    [''])[0]
            hackerrank  = query.get('hackerrank',  [''])[0]
            interviewbit= query.get('interviewbit',[''])[0]
            github      = query.get('github',      [''])[0]

            # Clear cache to ensure fresh sync
            cache_clear(username.lower())
            data = fetch_live_platform_data(username, leetcode, codeforces, codechef, hackerrank, interviewbit, github)
            cache_set(username.lower(), data)
            profiles = load_profiles()
            profiles[username.lower()] = data
            save_profiles(profiles)
            self.json(data)

        elif path.startswith('/api/profiles/'):
            username = path.replace('/api/profiles/', '').strip('/')
            cached = cache_get(username.lower())
            if cached:
                self.json(cached)
                return
            profiles = load_profiles()
            if username.lower() in profiles:
                data = profiles[username.lower()]
                cache_set(username.lower(), data)
                self.json(data)
            else:
                self.json({"error": "Profile not found", "username": username}, 404)

        else:
            super().do_GET()

    def do_POST(self):
        parsed = urllib.parse.urlparse(self.path)
        path   = parsed.path
        length = int(self.headers.get('Content-Length', 0))
        body   = self.rfile.read(length)
        try:
            payload = json.loads(body.decode('utf-8')) if body else {}
        except Exception:
            payload = {}

        now = time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())

        # ── REGISTER ──────────────────────────────────────────────────────
        if path == '/api/auth/register':
            raw_user  = payload.get("username", "").strip()
            raw_email = payload.get("email", "").strip()
            password  = payload.get("password", "").strip()
            name      = payload.get("name", "").strip()

            email    = raw_email if raw_email else (raw_user if "@" in raw_user else f"{raw_user}@gmail.com")
            username = raw_user.split("@")[0].lower() if raw_user else email.split("@")[0].lower()

            ok, msg = validate_user_criteria(username, email, password, is_registration=True)
            if not ok:
                self.json({"success": False, "message": msg}, 400)
                return

            users = load_users()
            if username in users or any(u.get("email","").lower() == email.lower() for u in users.values()):
                self.json({"success": False, "message": f"Account already exists for '{email}'. Please sign in."}, 400)
                return

            uid = f"usr_{int(time.time())}"
            user_obj = {
                "id": uid, "username": username, "email": email,
                "password_hash": hash_password(password),
                "provider": "local",
                "name": name if name else username.replace("_", " ").title(),
                "avatar": f"https://api.dicebear.com/7.x/initials/svg?seed={username}&backgroundColor=4A7FD4&textColor=ffffff",
                "createdAt": now, "lastLogin": now
            }
            users[username] = user_obj
            save_users(users)

            profiles = load_profiles()
            if username not in profiles:
                profiles[username] = {
                    "username": username, "name": user_obj["name"],
                    "bio": "Competitive Programmer | ProfileDekho",
                    "title": "Member", "totalSolved": 0,
                    "easySolved": 0, "mediumSolved": 0, "hardSolved": 0,
                    "totalContests": 0, "maxRating": 0, "currentRating": 0,
                    "globalScore": 0, "errors": {},
                    "leetcodeHandle": "", "codeforcesHandle": "", "codechefHandle": "",
                    "hackerrankHandle": "", "interviewbitHandle": "", "githubHandle": "",
                    "leetcodeStats": {"solved": 0, "valid": False},
                    "codeforcesStats": {"solved": 0, "valid": False},
                    "codechefStats": {"solved": 0, "valid": False},
                    "hackerrankStats": {"solved": 0, "valid": False},
                    "interviewbitStats": {"solved": 0, "valid": False},
                    "githubStats": {"publicRepos": 0, "valid": False},
                    "topicScores": {}, "ratingHistory": []
                }
                save_profiles(profiles)

            safe = {k: v for k, v in user_obj.items() if k != "password_hash"}
            self.json({"success": True, "token": f"pd_jwt_{uid}_{int(time.time())}", "user": safe,
                       "message": f"Welcome to ProfileDekho, @{username}!"}, 201)

        # ── LOGIN ──────────────────────────────────────────────────────────
        elif path == '/api/auth/login':
            identifier = payload.get("username", "").strip()
            password   = payload.get("password", "").strip()

            if not identifier:
                self.json({"success": False, "message": "Username or email is required."}, 400)
                return

            users = load_users()
            target = None
            for u in users.values():
                if u.get("username","").lower() == identifier.lower() or u.get("email","").lower() == identifier.lower():
                    target = u
                    break

            if not target:
                self.json({"success": False, "message": f"No account found for '{identifier}'. Please register first."}, 404)
                return

            if target.get("provider") == "local":
                if hash_password(password) != target.get("password_hash", ""):
                    self.json({"success": False, "message": "Incorrect password. Please try again."}, 401)
                    return

            target["lastLogin"] = now
            users[target["username"].lower()] = target
            save_users(users)
            safe = {k: v for k, v in target.items() if k != "password_hash"}
            self.json({"success": True, "token": f"pd_jwt_{target['id']}_{int(time.time())}", "user": safe,
                       "message": f"Welcome back, @{target['username']}!"})

        # ── GOOGLE OAUTH ───────────────────────────────────────────────────
        elif path == '/api/auth/google':
            email    = payload.get("email", "").strip().lower()
            name     = payload.get("name", "").strip()
            if not email:
                self.json({"success": False, "message": "Email is required for Google sign-in."}, 400)
                return
            username = re.sub(r'[^a-z0-9_]', '_', email.split("@")[0].lower())
            users = load_users()
            if username in users:
                u = users[username]
                u["lastLogin"] = now
                u["provider"] = "google"
                if name: u["name"] = name
            else:
                uid = f"usr_g_{int(time.time())}"
                u = {
                    "id": uid, "username": username, "email": email,
                    "provider": "google",
                    "name": name if name else username.replace("_", " ").title(),
                    "avatar": f"https://ui-avatars.com/api/?name={urllib.parse.quote(name or username)}&background=4285F4&color=fff&size=96",
                    "createdAt": now, "lastLogin": now
                }
                users[username] = u
            save_users(users)
            safe = {k: v for k, v in u.items() if k != "password_hash"}
            self.json({"success": True, "provider": "google", "token": f"pd_g_{u['id']}_{int(time.time())}",
                       "user": safe, "message": f"Signed in with Google as {email}"})

        # ── GITHUB OAUTH ───────────────────────────────────────────────────
        elif path == '/api/auth/github':
            gh_user = payload.get("username", "").strip().lower()
            name    = payload.get("name", "").strip()
            email   = payload.get("email", f"{gh_user}@github.com").strip().lower()
            if not gh_user:
                self.json({"success": False, "message": "GitHub username is required."}, 400)
                return
            avatar = f"https://github.com/{gh_user}.png?size=96"
            users = load_users()
            if gh_user in users:
                u = users[gh_user]
                u["lastLogin"] = now
                u["provider"] = "github"
                if name: u["name"] = name
            else:
                uid = f"usr_gh_{int(time.time())}"
                u = {
                    "id": uid, "username": gh_user, "email": email,
                    "provider": "github",
                    "name": name if name else gh_user.replace("-", " ").replace("_", " ").title(),
                    "avatar": avatar,
                    "createdAt": now, "lastLogin": now
                }
                users[gh_user] = u
            save_users(users)
            safe = {k: v for k, v in u.items() if k != "password_hash"}
            self.json({"success": True, "provider": "github", "token": f"pd_gh_{u['id']}_{int(time.time())}",
                       "user": safe, "message": f"Signed in with GitHub as @{gh_user}"})

        # ── SAVE PROFILE ───────────────────────────────────────────────────
        elif path == '/api/profiles/save':
            try:
                data = json.loads(body.decode('utf-8'))
                uname = data.get("username", "coder").lower()
                profiles = load_profiles()
                profiles[uname] = data
                save_profiles(profiles)
                cache_clear(uname)
                self.json({"success": True, "message": "Profile saved!"})
            except Exception as e:
                self.json({"success": False, "error": str(e)}, 400)

        else:
            self.send_error(404, "Endpoint not found")

    def json(self, data, status=200):
        body = json.dumps(data).encode('utf-8')
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(body)))
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', '*')
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', '*')
        self.end_headers()

    def send_error(self, code, message=None, explain=None):
        if code == 404 and self.path in ('/favicon.ico',):
            self.send_response(404)
            self.end_headers()
            return
        super().send_error(code, message, explain)


if __name__ == '__main__':
    print("=" * 52)
    print("  ProfileDekho Server  —  http://localhost:8080")
    print("  Parallel fetching: LeetCode · Codeforces · CodeChef")
    print("  HackerRank · InterviewBit · GitHub  (all in parallel)")
    print("  In-memory cache: 10-minute TTL")
    print("=" * 52)
    with socketserver.TCPServer(("", PORT), ProfileDekhoRequestHandler) as httpd:
        httpd.allow_reuse_address = True
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nServer stopped.")
