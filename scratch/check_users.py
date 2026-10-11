import json
import urllib.request

login_req = urllib.request.Request(
    'http://localhost:8080/api/v1/auth/login',
    data=json.dumps({"email": "admin@plasticwaste.org", "password": "AdminPassword2026!"}).encode('utf-8'),
    headers={"Content-Type": "application/json"}
)

try:
    with urllib.request.urlopen(login_req) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        token = data.get('token')
        print(f"Logged in successfully. Token obtained: {token[:15]}...")
        
        users_req = urllib.request.Request(
            'http://localhost:8080/api/v1/admin/users',
            headers={"Authorization": f"Bearer {token}"}
        )
        with urllib.request.urlopen(users_req) as u_resp:
            users = json.loads(u_resp.read().decode('utf-8'))
            print("REGISTERED USERS FROM AUTHORIZED ADMIN API:")
            for u in users:
                print(f" - Email: {u['email']} | Role: {u['role']} | Name: {u['fullName']} | Active: {u['isActive']}")
except Exception as e:
    print(f"Error: {e}")
