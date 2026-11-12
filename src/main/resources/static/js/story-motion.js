(() => {
  const revealItems = document.querySelectorAll('[data-reveal]');
  if ('IntersectionObserver' in window) {
    const observer = new IntersectionObserver((entries, current) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible');
          current.unobserve(entry.target);
        }
      });
    }, { threshold: 0.12, rootMargin: '0px 0px -35px 0px' });
    revealItems.forEach((item) => observer.observe(item));
  } else {
    revealItems.forEach((item) => item.classList.add('is-visible'));
  }

  const progress = document.querySelector('.home-scroll-progress span');
  if (progress) {
    const update = () => {
      const range = document.documentElement.scrollHeight - window.innerHeight;
      const percentage = range > 0 ? Math.min(100, (window.scrollY / range) * 100) : 0;
      progress.style.width = `${percentage}%`;
    };
    window.addEventListener('scroll', update, { passive: true });
    window.addEventListener('resize', update);
    update();
  }
})();

if(!matchMedia('(prefers-reduced-motion: reduce)').matches){let ticking=false;const photo=document.querySelector('.tn-home-photo');function move(){if(photo){const box=photo.parentElement.getBoundingClientRect();photo.style.transform='scale(1.08) translateY('+Math.max(-22,Math.min(22,-box.top*.05))+'px)';}ticking=false;}addEventListener('scroll',()=>{if(!ticking){requestAnimationFrame(move);ticking=true;}},{passive:true});move();}
