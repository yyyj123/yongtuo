"""Check the public-only gateway before exposing it through a tunnel."""
import urllib.request,urllib.error,sys
base=sys.argv[1] if len(sys.argv)>1 else 'http://127.0.0.1:8086'
def request(path,method='GET'):
    try: return urllib.request.urlopen(urllib.request.Request(base+path,method=method),timeout=45)
    except urllib.error.HTTPError as error: return error
for path in ['/','/products','/articles','/api/v1/public/products','/images/ai-preview/products.png']:
    response=request(path);assert response.status==200,(path,response.status)
    assert 'noindex' in response.headers.get('X-Robots-Tag',''),path
for path in ['/manage','/manage/','/manage/assets/index.js','/api/v1/admin/products','/api/v1/auth/login','/actuator/health','/.env','/api/v1/public/../admin/products']:
    response=request(path);assert response.status==404,(path,response.status)
assert request('/api/v1/public/products','POST').status==405
assert b'Disallow: /' in request('/robots.txt').read()
print('PASS: public pages and data accessible; management, private APIs and writes blocked; preview noindex enabled')
