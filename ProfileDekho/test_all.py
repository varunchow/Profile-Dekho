import urllib.request, json, time, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

def test_full_pipeline():
    # Start a server thread or test handler functions directly
    from run_app import (
        generate_otp, verify_otp, fetch_codechef, fetch_leetcode, fetch_codeforces,
        fetch_live_platform_data, load_profiles, save_profiles, load_users, save_users
    )

    print("=== TEST 1: OTP Generation & Verification ===")
    email = "varun.test@gmail.com"
    otp = generate_otp(email, name="Varun Kumar")
    print(f"Generated OTP: {otp}")
    assert len(otp) == 6, "OTP should be 6 digits"

    # Test invalid OTP
    ok, msg = verify_otp(email, "000000")
    print(f"Verify invalid OTP -> ok={ok}, msg={msg}")
    assert not ok, "Invalid OTP should fail"

    # Re-generate and verify valid OTP
    otp2 = generate_otp(email, name="Varun Kumar")
    ok, name_out = verify_otp(email, otp2)
    print(f"Verify valid OTP -> ok={ok}, name_out={name_out}")
    assert ok, "Valid OTP should pass"
    assert name_out == "Varun Kumar", "Name should match"

    print("\n=== TEST 2: Platform Live Fetching (CodeChef, Codeforces, LeetCode) ===")
    cc_data, cc_err = fetch_codechef("tourist")
    print(f"CodeChef tourist: {cc_data}, err={cc_err}")
    assert cc_data is not None and cc_data.get("valid"), "CodeChef tourist should be valid"
    assert cc_data.get("rating") > 0, "CodeChef rating should be > 0"
    assert cc_data.get("contests") > 0, "CodeChef contests should be > 0"

    cf_data, cf_err = fetch_codeforces("tourist")
    print(f"Codeforces tourist: solved={cf_data.get('solved')}, rating={cf_data.get('rating')}, rank={cf_data.get('rankName')}")
    assert cf_data is not None and cf_data.get("valid"), "Codeforces tourist should be valid"

    lc_data, lc_err = fetch_leetcode("neal_wu")
    print(f"LeetCode neal_wu: solved={lc_data.get('solved')}, rating={lc_data.get('rating')}")
    assert lc_data is not None and lc_data.get("valid"), "LeetCode neal_wu should be valid"

    print("\n=== TEST 3: Profile Aggregation with Name vs Email Separation ===")
    profile = fetch_live_platform_data(
        username="varun.test@gmail.com",
        leetcode="neal_wu",
        codeforces="tourist",
        codechef="tourist",
        hackerrank="",
        interviewbit="",
        github="torvalds",
        name="Varun Chow",
        bio="Full Stack AI Engineer"
    )
    print(f"Profile username: {profile['username']}")
    print(f"Profile display name: {profile['name']}")
    print(f"Profile bio: {profile['bio']}")
    print(f"Total Solved: {profile['totalSolved']}")
    print(f"Contests: {profile['totalContests']}")
    print(f"Max Rating: {profile['maxRating']}")
    print(f"Platform Ratings keys: {list(profile['platformRatings'].keys())}")

    assert profile['username'] == 'varun.test', "Username should be stripped of @gmail.com"
    assert profile['name'] == 'Varun Chow', "Name should be Varun Chow, not email"
    assert '@' not in profile['name'], "Profile name must never contain email @"
    assert set(profile['platformRatings'].keys()) == {'codeforces', 'codechef', 'leetcode'}, "Contest ratings should only contain codeforces, codechef, leetcode"

    print("\n✅ ALL TESTS PASSED SUCCESSFULLY!")

if __name__ == '__main__':
    test_full_pipeline()
