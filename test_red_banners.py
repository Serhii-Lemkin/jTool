import os, re
html_files = [f for f in os.listdir('website/y') if f.endswith('.html')]
banners = {}
for f in html_files:
    with open('website/y/' + f, 'r', encoding='utf-8') as file:
        content = file.read()
        match = re.search(r'<!-- Red Container Link Banner -->.*?</div>\s*</div>', content, re.DOTALL)
        if match:
            b = match.group(0)
            if b not in banners:
                banners[b] = []
            banners[b].append(f)

for i, (b, files) in enumerate(banners.items()):
    print(f"--- Banner {i} ({len(files)} files) ---")
    print(files)
