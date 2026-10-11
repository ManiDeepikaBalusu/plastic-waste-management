import urllib.request
import json

req = urllib.request.Request('http://localhost:8080/api/v1/auth/login', data=json.dumps({'email': 'admin@plasticwaste.org', 'password': 'AdminPassword2026!'}).encode('utf-8'), headers={'Content-Type': 'application/json'})
res = urllib.request.urlopen(req)
token = json.loads(res.read())['token']

req2 = urllib.request.Request('http://localhost:8080/api/v1/admin/users', headers={'Authorization': 'Bearer ' + token})
res2 = urllib.request.urlopen(req2)
users = json.loads(res2.read())
print("LIVE DATABASE USERS:")
for u in users:
    print(f"Email: {u.get('email')} | Role: {u.get('role')} | Name: {u.get('fullName')}")
