import re,http.cookiejar,urllib.request,urllib.parse,urllib.error,json,uuid
base='http://localhost:8081'
def client():return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def request(c,path,data=None):
 with c.open(base+path,data=urllib.parse.urlencode(data).encode() if data is not None else None,timeout=15) as r:
  return urllib.parse.urlsplit(r.url).path,r.read()
def form(c,page,action,**fields):
 body=request(c,page)[1].decode()
 fields['_csrf']=re.search(r'name="_csrf" value="([^"]+)"',body)[1]
 return request(c,action,fields)
c=client()
for path in ['/','/products','/collection','/collection/bronze-parvathi-9','/about','/faq','/reviews','/products/1','/login','/register','/css/tn.css','/images/tn-mark.svg','/images/pottery.jpg','/images/kanchipuram.jpg']:
 assert request(c,path)[1],path
body=request(c,'/collection')[1].decode()
assert body.count('class="listing-card"')==40
assert 'Bronze Parvathi' in request(c,'/collection?q=Parvathi')[1].decode()
for item in json.load(open('src/main/resources/tn-catalog.json',encoding='utf-8')):
 path='/collection/'+item['id']
 assert request(c,path)[0]==path
email='smoke-'+uuid.uuid4().hex+'@example.test'
password='Demo-test-123'
path,body=form(c,'/login','/login',username=email,password=password)
assert path=='/register' and email in body.decode()
assert form(c,'/register','/register',email=email,password=password,name='Demo Buyer',role='CUSTOMER')[0]=='/login'
assert form(c,'/login','/login',username=email,password=password)[0]=='/account'
assert form(c,'/products/1','/cart/add/1',quantity='1')[0]=='/cart'
assert form(c,'/checkout','/checkout',address='123 Testing Street Chennai 600001')[0]=='/checkout/payment'
assert request(c,'/checkout/qr')[1].startswith(b'\x89PNG')
code=re.search(r'<code>([A-F0-9]{8})</code>',request(c,'/checkout/payment')[1].decode())[1]
assert form(c,'/checkout/payment','/checkout/confirm',code='notright')[0]=='/checkout/payment'
order,body=form(c,'/checkout/payment','/checkout/confirm',code=code)
assert order.startswith('/customer/orders/') and 'Demo QR confirmed' in body.decode()
assert 'Illustrated route' in body.decode()
try: form(c,'/products/1','/customer/reviews/1',rating='5',body='Good quality and workmanship.')
except urllib.error.HTTPError as e: assert e.code==403
else: raise AssertionError('Review before delivery was accepted')
admin=client()
assert form(admin,'/login','/login',username='admin@artisanconnect.local',password='Admin@123')[0]=='/admin'
assert form(admin,'/admin','/admin/order/'+order.split('/')[-1],status='DELIVERED')[0]=='/admin'
assert form(c,'/products/1','/customer/reviews/1',rating='5',body='Good quality and workmanship.')[0]=='/products/1'
assert 'Good quality and workmanship.' in request(c,'/reviews')[1].decode()
print('Public pages, 40 item pages, signup routing, checkout QR, tracking and delivered review passed')
