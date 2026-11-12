(() => {
  const landing = document.querySelector('.craft-landing');
  if (!landing) return;
  if ('IntersectionObserver' in window && !matchMedia('(prefers-reduced-motion: reduce)').matches) {
    const observer = new IntersectionObserver(entries => {
      entries.forEach(entry => entry.target.classList.toggle('in-view', entry.isIntersecting));
    }, { threshold: 0.08 });
    landing.querySelectorAll('[data-motion]').forEach((element, index) => {
      element.style.setProperty('--stagger', (index % 4) * 45 + 'ms');
      observer.observe(element);
    });
    landing.classList.add('motion-ready');
  }
  const buttons = [...landing.querySelectorAll('[data-save]')];
  const filter = document.getElementById('saved-filter');
  const empty = document.getElementById('saved-empty');
  if (!filter || !empty) return;
  const storageKey = 'tn-artisan-saved-products';
  let saved = new Set();
  let onlySaved = false;
  try {
    const stored = JSON.parse(localStorage.getItem(storageKey) || '[]');
    if (Array.isArray(stored)) saved = new Set(stored);
  } catch {}
  function renderSaved() {
    let visible = 0;
    for (const button of buttons) {
      const selected = saved.has(button.dataset.save);
      button.hidden = false;
      button.setAttribute('aria-pressed', String(selected));
      button.setAttribute('aria-label', selected ? 'Remove saved product' : 'Save product');
      button.textContent = selected ? '\u2665' : '\u2661';
      button.closest('article').hidden = onlySaved && !selected;
      if (!onlySaved || selected) visible++;
    }
    filter.hidden = false;
    filter.textContent = onlySaved ? 'Show all featured pieces' : 'Saved pieces';
    filter.setAttribute('aria-pressed', String(onlySaved));
    empty.hidden = !onlySaved || visible > 0;
  }
  buttons.forEach(button => button.addEventListener('click', () => {
    const id = button.dataset.save;
    if (saved.has(id)) saved.delete(id);
    else saved.add(id);
    try { localStorage.setItem(storageKey, JSON.stringify([...saved])); } catch {}
    renderSaved();
  }));
  filter.addEventListener('click', () => {
    onlySaved = !onlySaved;
    renderSaved();
  });
  renderSaved();
})();
