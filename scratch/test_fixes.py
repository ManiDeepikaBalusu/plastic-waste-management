import urllib.request
import urllib.parse
import json


BASE_URL = "https://plastic-waste-management-3.onrender.com/api/v1"

def test_unauthenticated_protected_endpoints():
    print("=== Testing Unauthenticated Requests to Protected Endpoints ===")
    endpoints = [
        "/assignments/my-tasks",
        "/volunteer/profile",
        "/complaints"
    ]
    for ep in endpoints:
        req = urllib.request.Request(f"{BASE_URL}{ep}")
        try:
            urllib.request.urlopen(req)
            assert False, f"Expected 401 for unauthenticated {ep}, but succeeded!"
        except urllib.error.HTTPError as e:
            print(f"GET {ep} -> Status: {e.code}")
            assert e.code == 401, f"Expected 401, got {e.code}"
    print("SUCCESS: All unauthenticated requests correctly rejected with 401.\n")

def test_cors_preflight():
    print("=== Testing CORS Preflight for Allowed Origins ===")
    origins = [
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:3000"
    ]
    for origin in origins:
        req = urllib.request.Request(
            f"{BASE_URL}/complaints",
            headers={
                "Origin": origin,
                "Access-Control-Request-Method": "GET",
                "Access-Control-Request-Headers": "Authorization,Content-Type"
            },
            method="OPTIONS"
        )
        with urllib.request.urlopen(req) as resp:
            allow_origin = resp.headers.get("Access-Control-Allow-Origin")
            allow_cred = resp.headers.get("Access-Control-Allow-Credentials")
            print(f"Origin: {origin} -> Status: {resp.status}, Allow-Origin: {allow_origin}, Allow-Credentials: {allow_cred}")
            assert allow_origin == origin, f"CORS origin {origin} rejected!"
            assert allow_cred == "true", f"Allow-Credentials not true!"
    print("SUCCESS: CORS preflight properly configured for dev origins.\n")

def test_admin_auth_and_complaints():
    print("=== Testing Admin Login & Protected /complaints Endpoint ===")
    login_data = json.dumps({
        "email": "admin@plasticwaste.org",
        "password": "AdminPassword2026!"
    }).encode("utf-8")
    req = urllib.request.Request(
        f"{BASE_URL}/auth/login",
        data=login_data,
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req) as resp:
        assert resp.status == 200
        data = json.loads(resp.read().decode("utf-8"))
        token = data["token"]
        user = data["user"]
        print(f"Logged in as: {user['fullName']} ({user['email']}), Role: {user['role']}")

    # Check /complaints with Bearer token
    req_comp = urllib.request.Request(
        f"{BASE_URL}/complaints",
        headers={"Authorization": f"Bearer {token}"}
    )
    with urllib.request.urlopen(req_comp) as resp:
        print(f"GET /complaints with Bearer token -> Status: {resp.status}")
        assert resp.status == 200
        complaints = json.loads(resp.read().decode("utf-8"))
        print(f"Fetched {len(complaints)} complaints successfully.")
    print("SUCCESS: Admin authentication and /complaints access verified.\n")

def test_tile_url():
    print("=== Testing Leaflet Tile Provider Availability ===")
    tile_url = "https://tile.openstreetmap.org/13/4823/3055.png"
    req = urllib.request.Request(tile_url, headers={"User-Agent": "EcoClean-App/1.0"})
    with urllib.request.urlopen(req) as resp:
        print(f"GET {tile_url} -> Status: {resp.status}, Content-Type: {resp.headers.get('content-type')}")
        assert resp.status == 200
    print("SUCCESS: OpenStreetMap standard tile endpoint is active, returning image/png.\n")

if __name__ == "__main__":
    test_unauthenticated_protected_endpoints()
    test_cors_preflight()
    test_admin_auth_and_complaints()
    test_tile_url()
    print("ALL VERIFICATION CHECKS PASSED SUCCESSFULLY!")
