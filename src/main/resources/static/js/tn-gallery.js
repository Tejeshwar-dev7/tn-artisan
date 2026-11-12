document.querySelectorAll('[data-gallery]').forEach(g=>{
 const main=g.querySelector('[data-gallery-main]'), thumbs=[...g.querySelectorAll('[data-gallery-photo]')];
 let i=0,x=null;
 const show=n=>{i=(n+thumbs.length)%thumbs.length;const url=thumbs[i].dataset.galleryPhoto;main.href=url;main.querySelector('img').src=url;main.classList.remove('zoomed');g.querySelector('[data-zoom]').setAttribute('aria-pressed','false');thumbs.forEach((b,j)=>b.setAttribute('aria-pressed',String(i===j)));};
 thumbs.forEach((b,j)=>b.addEventListener('click',()=>show(j)));
 for(const [selector,delta] of [['[data-prev]',-1],['[data-next]',1]]){const b=g.querySelector(selector);b.disabled=thumbs.length<2;b.addEventListener('click',()=>show(i+delta));}
 g.querySelector('[data-zoom]').addEventListener('click',e=>e.currentTarget.setAttribute('aria-pressed',String(main.classList.toggle('zoomed'))));
 main.addEventListener('touchstart',e=>x=e.changedTouches[0].clientX,{passive:true});
 main.addEventListener('touchend',e=>{const dx=e.changedTouches[0].clientX-x;if(x!==null&&Math.abs(dx)>42){e.preventDefault();show(i+(dx<0?1:-1));}x=null;},{passive:false});
});
document.querySelectorAll('.listing-photo img,.tn-main-photo img,.gallery-thumbs img').forEach(img=>{
 const fail=()=>{if(img.parentElement.querySelector('.tn-photo-unavailable'))return;const note=document.createElement('span');note.className='tn-photo-unavailable';note.textContent='Photo unavailable';img.insertAdjacentElement('afterend',note);img.hidden=true;};
 img.addEventListener('error',fail);if(img.complete&&!img.naturalWidth)fail();
 img.addEventListener('load',()=>{img.hidden=false;img.parentElement.querySelector('.tn-photo-unavailable')?.remove();});
});