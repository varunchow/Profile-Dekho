import urllib.request, re, json

def test_hr(handle):
    req = urllib.request.Request(f'https://www.hackerrank.com/rest/hackers/{handle}/profile', headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'})
    try:
        resp = urllib.request.urlopen(req, timeout=10)
        data = json.loads(resp.read().decode('utf-8'))
        print("HR data for", handle, ":", data.get("model", {}).get("username"))
    except Exception as e:
        print("HR error for", handle, ":", e)

def test_ib(handle):
    req = urllib.request.Request(f'https://www.interviewbit.com/profile/{handle}/', headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'})
    try:
        resp = urllib.request.urlopen(req, timeout=10)
        html = resp.read().decode('utf-8')
        print("IB len for", handle, ":", len(html))
    except Exception as e:
        print("IB error for", handle, ":", e)

if __name__ == '__main__':
    test_hr('tourist')
    test_hr('gennady')
    test_ib('tourist')
