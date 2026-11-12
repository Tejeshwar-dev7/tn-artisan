(() => {
  const input = document.getElementById('preview-query');
  const results = document.getElementById('preview-results');
  const query = new URLSearchParams(location.search).get('q')?.trim();
  if (!query || !input || !results) return;

  input.value = query;
  const terms = query.toLowerCase().split(/\s+/);
  let count = 0;
  for (const card of document.querySelectorAll('.marketplace-card')) {
    const searchable = card.textContent.toLowerCase();
    const matches = terms.every(term => searchable.includes(term));
    card.hidden = !matches;
    if (matches) count++;
  }

  results.textContent = count
    ? count + (count === 1 ? ' result' : ' results') + ' for “' + query + '”.'
    : 'No crafts found for “' + query + '”.';
  const reset = document.createElement('a');
  reset.href = 'products.html';
  reset.textContent = 'Clear search';
  results.append(reset);
  results.hidden = false;
})();
