import urllib.request
import json
import urllib.error

passwords_to_try = [
    "AdminPassword2026!",
    "VolunteerPassword2026!",
    "CitizenPassword123!",
    "TestPassword123!",
    "Password123!",
    "SecurePass2026!",
    "Deepu@125"
]

users_to_check = [
    "admin@plasticwaste.org",
    "volunteer@plasticwaste.org",
    "citizen_test@example.com",
    "testcitizen1@example.com",
    "test_citizen_live@waste.org",
    "hanuman@gmail.com",
    "d63473895@gmail.com",
    "user_1791733039@plasticwaste.org"
]

for email in users_to_check:
    found = False
    for pwd in passwords_to_try:
        req = urllib.request.Request(
            'http://localhost:8080/api/v1/auth/login',
            data=json.dumps({'email': email, 'password': pwd}).encode('utf-8'),
            headers={'Content-Type': 'application/json'}
        )
        try:
            with urllib.request.urlopen(req) as resp:
                if resp.status == 200:
                    data = json.loads(resp.read().decode('utf-8'))
                    role = data['user']['role']
                    name = data['user']['fullName']
                    print(f"VERIFIED: {email} | Password: {pwd} | Role: {role} | Name: {name}")
                    found = True
                    break
        except urllib.error.HTTPError:
            pass
    if not found:
        print(f"Could not verify known password for: {email}")
