import urllib.request
import urllib.parse
import json
import os
import sys

sys.stdout.reconfigure(encoding='utf-8')

def fix_url(raw_url):
    """URL-encode non-ASCII characters in URL path."""
    from urllib.parse import urlparse, urlunparse, quote
    parsed = urlparse(raw_url)
    # Encode the path component
    encoded_path = quote(parsed.path, safe='/:@!$&()*+,;=')
    return urlunparse((parsed.scheme, parsed.netloc, encoded_path, parsed.params, parsed.query, parsed.fragment))

# Get file list
api_url = 'https://api.github.com/repos/Sanotsu/china-food-composition-data/contents/json_data_vision_251206_Qwen2-5-VL-72B-Instruct'
req = urllib.request.Request(api_url, headers={'User-Agent': 'Mozilla/5.0'})
files = json.loads(urllib.request.urlopen(req).read())

print(f'Found {len(files)} files')

assets_dir = os.path.join('app', 'src', 'main', 'assets')
os.makedirs(assets_dir, exist_ok=True)

all_items = []
total = 0
failed = []

for f in files:
    name = f['name']
    if not name.endswith('.json'):
        continue
    try:
        url = fix_url(f['download_url'])
        req2 = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        data = json.loads(urllib.request.urlopen(req2).read())
        n = len(data)
        total += n
        all_items.extend(data)
        print(f'  {name}: {n} items')
    except Exception as e:
        failed.append(name)
        print(f'  {name}: FAILED - {e}')

print(f'\nTotal: {total} items from {len(files)-len(failed)} files')
if failed:
    print(f'Failed: {failed}')

merged = os.path.join(assets_dir, 'nutrition_database.json')
with open(merged, 'w', encoding='utf-8') as f:
    json.dump(all_items, f, ensure_ascii=False, separators=(',', ':'))

print(f'Saved: {merged} ({os.path.getsize(merged):,} bytes)')
