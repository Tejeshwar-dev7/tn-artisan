document.addEventListener('DOMContentLoaded',()=>{const r=document.querySelector('#role'),f=document.querySelector('#artisanFields');if(r&&f){const s=()=>{f.hidden=r.value!=='ARTISAN';f.querySelectorAll('input').forEach(i=>i.required=r.value==='ARTISAN')};r.addEventListener('change',s);s();if(new URLSearchParams(location.search).get('role')==='ARTISAN'){r.value='ARTISAN';s()}}});

const signInForm = document.querySelector('#account-sign-in');
if (signInForm) {
  document.querySelectorAll('[data-demo-email]').forEach(button => {
    button.addEventListener('click', () => {
      signInForm.elements.namedItem('username').value = button.dataset.demoEmail;
      signInForm.elements.namedItem('password').value = signInForm.dataset.demoPassword;
      signInForm.querySelector('button[type="submit"], button:not([type])').focus();
    });
  });
}
