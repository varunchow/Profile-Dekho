# ProfileDekho Security Enhancement - Implementation Progress

## ✅ PHASE 1: SECURITY & AUTH FOUNDATION - COMPLETED

### 1. Dependencies Added to `pom.xml`
- ✅ `spring-boot-starter-security`
- ✅ `spring-security-oauth2-client`  
- ✅ `spring-security-oauth2-jose`
- ✅ `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (JWT library)
- ✅ `spring-boot-starter-webflux` (WebClient for non-blocking HTTP)
- ✅ `jsoup` (HTML parsing for InterviewBit)

### 2. Security Components Created

**`JwtUtil.java`** - JWT Token Management
- Generate access tokens (24hr default expiry)
- Generate refresh tokens (7-day default expiry)
- Validate and extract tokens
- Extract claims and usernames

**`JwtAuthFilter.java`** - Request Interceptor
- Extracts JWT from `Authorization: Bearer <token>` header
- Validates token on every protected request
- Sets authentication in SecurityContextHolder

**`SecurityConfig.java`** - Spring Security Configuration
- Configures CORS (locked to `${app.frontend-url}`, not `*`)
- Enables OAuth2 login flow
- Disables CSRF (stateless JWT authentication)
- Adds JWT filter to security chain
- Denies access to `/api/auth/users` (was returning all users unauthenticated)

**`OAuth2SuccessHandler.java`** - OAuth2 Callback Handler
- Handles Google OAuth2 login
- Handles GitHub OAuth2 login
- Creates/updates user record on OAuth success
- Issues JWT tokens after successful OAuth authentication
- Redirects to frontend with token in URL

### 3. AuthController Updated
- ✅ Removed SHA-256 hashing (insecure)
- ✅ Replaced with BCryptPasswordEncoder
- ✅ Removed fake OAuth endpoints (`/api/auth/google`, `/api/auth/github`)
- ✅ Removed the `$2a$10$pdBcryptHash_` backdoor bypass
- ✅ Added proper `/register` endpoint with JWT issuance
- ✅ Added proper `/login` endpoint with JWT issuance
- ✅ Added `/refresh` endpoint for token refresh
- ✅ Removed `/api/auth/users` (was publicly exposing all users)
- ✅ Locked down `@CrossOrigin` from `*` to configurable origin

### 4. ProfileController Updated
- ✅ Locked down `@CrossOrigin` from `*` to configurable origin
- ✅ Added `@PreAuthorize("authentication.name == #username")` to refresh endpoint
- ✅ Added new `/api/profiles/{username}/refresh` endpoint (requires authentication)
- ✅ Profile read endpoints remain public (intentional - showcase purpose)

---

## 📋 PHASE 2: Real Data-Fetching Clients - READY TO IMPLEMENT

### Structure to Create:
```
src/main/java/com/profiledekho/app/service/clients/
├── CodeforcesClient.java
├── GitHubClient.java
├── LeetCodeClient.java
├── CodeChefClient.java
├── InterviewBitClient.java
└── PlatformStats.java (dto)
```

### Tasks:
1. **CodeforcesClient** - Fetch via official REST API
   - `user.info` → basic info + rating
   - `user.rating` → rating history
   - `user.status` → recent submissions for heatmap

2. **GitHubClient** - Fetch via official REST API + GraphQL
   - Requires personal access token (GitHub settings)
   - Contribution calendar via GraphQL
   - Public repos, stars, followers

3. **LeetCodeClient** - Fetch via unofficial GraphQL
   - LeetCode official GraphQL endpoint
   - Problem stats (solved, easy/medium/hard counts)
   - Contest ranking and history
   - Submission calendar

4. **CodeChefClient** - Fetch via unofficial API
   - Use `codechef-api.vercel.app` or scrape public profiles
   - Rating, solved problems, contests
   - Flag as "unofficial" in response

5. **InterviewBitClient** - Fetch with privacy detection
   - Server-side page scraping (Jsoup)
   - Return 4 states:
     - `{valid: true, private: false, solved: 150, ...}`
     - `{valid: true, private: false, solved: 50, contests: 0, ...}` (no contests)
     - `{valid: false, private: true}`
     - `{valid: false, error: "not_found"}`

### Parallel Fetch Pattern:
```java
Mono.zip(
    codeforcesClient.fetchStats(handle),
    githubClient.fetchStats(handle),
    leetcodeClient.fetchStats(handle),
    codechefClient.fetchStats(handle),
    interviewbitClient.fetchStats(handle)
)
.timeout(Duration.ofSeconds(5))
.subscribe(...)
```

---

## 📋 PHASE 3: Auto-Refresh on Visit - READY TO IMPLEMENT

### Tasks:
1. Add `lastFetchedAt` timestamp field to `UserProfile` model
2. Implement `/api/profiles/{username}/refresh` (already added, needs backend completion)
3. Frontend auto-calls refresh on login if `lastFetchedAt` > 1 hour
4. Add lightweight in-memory TTL cache (per-platform, 5-10 minute validity)
5. Frontend shows loading skeleton while refresh is in flight

---

## 📋 PHASE 4: Heatmaps - READY TO IMPLEMENT

### Backend Tasks:
1. Extract daily submission counts from each platform
2. Aggregate into `{date: "2024-01-15", count: 5}[]` per platform
3. Return in UserProfile response

### Frontend Tasks:
1. Install `react-calendar-heatmap` or `@uiw/react-heat-map`
2. Create `<Heatmap>` component
3. Display per-platform heatmaps + optional combined view

---

## 📋 PHASE 5: Contest Rating Visualizations - READY TO IMPLEMENT

### Backend Tasks:
1. Fetch rating history per platform (Codeforces, LeetCode, CodeChef)
2. Structure: `{platform, date, rating}[]`
3. Include max rating, current rating, rank per platform

### Frontend Tasks:
1. Use Recharts `LineChart` for rating trends
2. Compare across platforms in a combined chart
3. Summary card per platform: max/current rating, contests attended

---

## 📋 PHASE 6: Frontend Cleanup - READY TO IMPLEMENT

### Tasks:
1. Remove all HackerRank references from UI
2. Remove all GFG (GeeksforGeeks) references
3. Update InterviewBit card to handle 4 privacy states:
   - Show message if private
   - Show message if not found
   - Show stats if public
4. Remove HackerRank fields from state/components/pages

---

## 🔧 Environment Variables Required

Create `.env` (or set in deployment):

```properties
# JWT Configuration
jwt.secret-key=your-secret-key-at-least-32-characters-long-change-in-production
jwt.expiration=86400000
jwt.refresh-expiration=604800000

# CORS Configuration
app.frontend-url=http://localhost:5173

# OAuth2 Credentials (from Google Cloud Console & GitHub)
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET

spring.security.oauth2.client.registration.github.client-id=YOUR_GITHUB_CLIENT_ID
spring.security.oauth2.client.registration.github.client-secret=YOUR_GITHUB_CLIENT_SECRET

# Platform-specific tokens (optional)
github.personal-access-token=ghp_XXXXXXXXXXXXXXXX (for extended API access)
```

---

## 📝 Next Steps

1. **Before Proceeding:** Register OAuth apps
   - [Google Cloud Console](https://console.cloud.google.com)
   - [GitHub Developer Settings](https://github.com/settings/developers)

2. **Phase 2:** Implement all 5 platform clients (start with Codeforces)

3. **Phase 3:** Wire up auto-refresh logic

4. **Phase 4-6:** Implement visualizations and cleanup

---

## ⚠️ BREAKING CHANGES

- Old fake OAuth endpoints (`/api/auth/google`, `/api/auth/github`) removed
- Old fake bearer tokens (e.g., `"pd_jwt_token_..."`) no longer issued
- `/api/auth/users` endpoint now denied (was security risk)
- Frontend must send JWT in `Authorization: Bearer <token>` header
- CORS restricted to single origin (not `*`)

**Frontend MUST update:**
- Store JWT from login/register response
- Send JWT in every authenticated request header
- Handle OAuth2 redirect callback
- Display loading state during profile refresh

---

## 🛡️ Security Improvements

✅ Passwords now BCrypt-hashed (not SHA-256)
✅ JWT-based stateless auth (not fake tokens)
✅ Real OAuth2 flow (not client-side forgery)
✅ CORS restricted to single origin
✅ Sensitive endpoints require authentication
✅ Token expiry + refresh token pattern
✅ No secrets in code (use environment variables)
✅ Rate-limiting ready (not yet implemented)

---

**Last Updated:** 2026-09-01
**Status:** Phase 1 Complete ✅ → Ready for Phase 2 🚀
