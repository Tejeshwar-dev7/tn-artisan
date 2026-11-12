"""Export the storefront from a fresh local demo database for GitHub Pages."""
from concurrent.futures import ThreadPoolExecutor
from html import escape, unescape
from pathlib import Path
import re
import shutil
from urllib.parse import urlsplit
from urllib.request import urlopen

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "docs"
STATIC = ROOT / "src/main/resources/static"
LOCAL = "http://127.0.0.1:8081"
REPOSITORY = "https://github.com/Tejeshwar-dev7/tn-artisan"
PUBLIC_ROUTES = {"/", "/products", "/about", "/mission", "/for-sellers", "/for-customers", "/faq", "/reviews"}
ATTRIBUTES = re.compile(r'(href|src)="(/[^"/][^"]*|/)"')
FORMS = re.compile(r'<form\b[^>]*>.*?</form>', re.I | re.S)


def route_key(link):
    parts = urlsplit(unescape(link))
    if parts.path == "/products" and re.fullmatch(r"category=\d+", parts.query):
        return f"/products?{parts.query}"
    return parts.path


def page_name(route):
    if route == "/":
        return "index.html"
    return route.lstrip("/").replace("/", "-").replace("?category=", "-category-") + ".html"


def fetch(route):
    with urlopen(LOCAL + route, timeout=20) as response:
        return response.read().decode("utf-8")


def apply_preview_header(markup, filename):
    nav_items = (
        ("index.html", "Home"), ("products.html", "Marketplace"),
        ("about.html", "Artisans &amp; NGOs"), ("for-sellers.html", "Sell with us"),
        ("reviews.html", "Reviews"), ("faq.html", "Help"),
    )
    active = "products.html" if filename.startswith("products-") else filename
    links = []
    for href, label in nav_items:
        current = ' aria-current="page"' if active == href else ""
        links.append(f'<a href="{href}"{current}>{label}</a>')
    header = f"""
<header class="preview-header">
  <div class="preview-header-inner">
    <a class="preview-brand" href="index.html"><img src="images/tn-mark.svg" width="38" height="38" alt="">TN ARTISAN</a>
    <nav aria-label="Main navigation">{''.join(links)}</nav>
    <form class="preview-search" role="search" action="products.html" method="get">
      <label for="preview-query" class="preview-visually-hidden">Search handmade crafts</label>
      <input id="preview-query" name="q" type="search" placeholder="Search handmade crafts" required maxlength="120">
      <button type="submit">Search</button>
    </form>
  </div>
</header>"""
    markup = re.sub(r'<header class="(?:tn-header|preview-header)">.*?</header>', lambda _: header, markup, count=1, flags=re.S)
    banner = (f'<aside class="preview-notice">Portfolio preview. '
              f'<a href="{REPOSITORY}#readme">Run the full app ↗</a></aside>')
    markup = re.sub(r'(<main>)(?:<aside[^>]*>.*?</aside>)?', lambda m: m[1] + banner, markup, count=1, flags=re.S)
    if 'href="css/preview.css"' not in markup:
        markup = markup.replace("</head>", '<link rel="stylesheet" href="css/preview.css"></head>', 1)
    if 'src="js/preview.js"' not in markup:
        markup = markup.replace("</body>", '<script src="js/preview.js" defer></script></body>', 1)
    if filename == "products.html" and 'id="preview-results"' not in markup:
        markup = markup.replace('<div class="listing-grid marketplace-grid">',
            '<p id="preview-results" class="preview-results" role="status" hidden></p><div class="listing-grid marketplace-grid">', 1)
    return markup


def export():
    OUTPUT.mkdir(exist_ok=True)
    (OUTPUT / ".nojekyll").touch()
    pages = {}
    pending = set(PUBLIC_ROUTES)
    with ThreadPoolExecutor(max_workers=1) as workers:
        while pending:
            batch = sorted(pending)
            pending.clear()
            for route, markup in zip(batch, workers.map(fetch, batch)):
                pages[route] = markup
                for attr, link in ATTRIBUTES.findall(markup):
                    key = route_key(link)
                    if attr == "href" and key not in pages and key not in batch and (
                        re.fullmatch(r"/(?:products|artisan|passport)/\d+", key)
                        or re.fullmatch(r"/products\?category=\d+", key)
                    ):
                        pending.add(key)

    assets = {"css/preview.css", "js/preview.js"}
    for route, markup in pages.items():
        markup = FORMS.sub("", markup)
        markup = re.sub(r'<a\b[^>]*href="/(?:cart|account|checkout|login|register|artisan-area|collection)(?:[/?#][^"]*)?"[^>]*>.*?</a>', "", markup, flags=re.I | re.S)
        markup = re.sub(r'<p[^>]*>Reviews are available to signed-in customers after delivery\.</p>', "", markup)
        markup = re.sub(r'<input[^>]+name="_csrf"[^>]*>', "", markup)

        def rewrite(match):
            attribute, link = match.groups()
            key = route_key(link)
            fragment = urlsplit(unescape(link)).fragment
            if key in pages:
                target = page_name(key) + ("#" + fragment if fragment else "")
            elif key.startswith(("/images/", "/css/", "/js/")):
                target = key.lstrip("/")
                assets.add(target)
            elif attribute == "href":
                target = page_name("/products")
            else:
                raise ValueError(f"Unexpected asset {key}")
            return f'{attribute}="{escape(target, quote=True)}"'

        markup = ATTRIBUTES.sub(rewrite, markup)
        markup = apply_preview_header(markup, page_name(route))
        (OUTPUT / page_name(route)).write_text(markup, encoding="utf-8")

    for name in sorted(assets):
        source = STATIC / name
        destination = OUTPUT / name
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)
    print(f"Exported {len(pages)} pages and {len(assets)} assets.")


if __name__ == "__main__":
    export()

