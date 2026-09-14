"""Read-only gateway checks; no credentials or live content mutations."""
import json
import re
import sys
from urllib.request import Request, urlopen
from urllib.error import HTTPError

base = sys.argv[1].rstrip('/')

def request(path, method='GET', data=None):
    req = Request(base + path, method=method, data=data,
                  headers={'Content-Type': 'application/json'})
    try:
        with urlopen(req, timeout=30) as response:
            return response.status, response.read().decode()
    except HTTPError as error:
        return error.code, error.read().decode()

status, html = request('/manage/login')
assert status == 200 and 'id="app"' in html, (status, 'admin shell missing')
assets = re.findall(r'(?:src|href)="(/manage/assets/[^\"]+)"', html)
assert assets, 'admin assets missing'
for asset in assets:
    assert request(asset)[0] == 200, asset
assert request('/manage/assets/missing-file.js')[0] == 404
for method in ('GET', 'PUT', 'DELETE'):
    assert request('/api/v1/admin/contact', method, b'[]' if method == 'PUT' else None)[0] == 401
assert request('/api/v1/admin/auth/me')[0] == 401
status, body = request('/api/v1/admin/auth/refresh', 'POST', b'{"refreshToken":"invalid-gateway-test-token"}')
assert status == 401, (status, 'invalid refresh token must be rejected by auth')
for path in ('/actuator/health', '/swagger/index.html', '/v3/api-docs', '/.env', '/api/private'):
    assert request(path)[0] == 404, path
assert request('/api/v1/public/contact')[0] == 200
assert request('/api/v1/public/contact', 'POST', b'{}')[0] == 405
assert request('/', 'POST', b'{}')[0] == 405
print('PASS: admin shell/assets, unauthenticated read/write rejection, invalid session rejection, private route blocks and public read-only access')
