(()=>{
 const money=new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR'});let busy=false;
 async function refresh(){if(busy||document.hidden)return;busy=true;const live=document.getElementById('seller-live');try{
 const response=await fetch('/artisan-area/summary',{cache:'no-store',headers:{Accept:'application/json'}});if(!response.ok||!response.headers.get('content-type')?.includes('json'))throw Error('Sign in again to refresh your dashboard.');const data=await response.json();
 for(const key of ['orders','confirmed','pending','commission'])document.getElementById('metric-'+key).textContent=key==='orders'?data[key]:money.format(data[key]);
 const body=document.getElementById('seller-sales');body.replaceChildren(...data.recent.map(row=>{const tr=document.createElement('tr');['AC-'+row.id,row.paymentStatus.replaceAll('_',' '),money.format(row.gross),money.format(row.commission),money.format(row.net)].forEach(value=>{const td=document.createElement('td');td.textContent=value;tr.append(td);});return tr;}));
 if(!data.recent.length){const tr=document.createElement('tr'),td=document.createElement('td');td.colSpan=5;td.textContent='Your first customer order will appear here.';tr.append(td);body.append(tr);}
 const orders=document.getElementById('seller-orders');if(!orders.contains(document.activeElement)&&!orders.dataset.editing){const page=await fetch('/artisan-area',{cache:'no-store'});if(page.ok){const doc=new DOMParser().parseFromString(await page.text(),'text/html');const updated=doc.getElementById('seller-orders');if(updated)orders.replaceWith(updated);const listings=doc.getElementById('seller-listings');if(listings)document.getElementById('seller-listings').replaceWith(listings);}}
 live.textContent='Updated '+new Date().toLocaleTimeString('en-IN')+' · Checks for new orders every 10 seconds.';
 }catch(error){live.textContent='Updates paused. Check your connection or sign in again.';}finally{busy=false;}}
 document.addEventListener('input',event=>{const section=event.target.closest('#seller-orders');if(section)section.dataset.editing='true';});
 refresh();setInterval(refresh,10000);document.addEventListener('visibilitychange',()=>{if(!document.hidden)refresh();});
})();