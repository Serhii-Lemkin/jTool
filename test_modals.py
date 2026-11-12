import os, re
html_files = [f for f in os.listdir('website/y') if f.endswith('.html')]
modals = {}
for f in html_files:
    with open('website/y/' + f, 'r', encoding='utf-8') as file:
        content = file.read()
        match = re.search(r'<div class="modal fade" id="vizDisclaimerModal".*?</div>\s*</div>\s*</div>', content, re.DOTALL)
        if match:
            m = match.group(0)
            if m not in modals:
                modals[m] = []
            modals[m].append(f)

print(f"Number of unique modals: {len(modals)}")
for i, (m, files) in enumerate(modals.items()):
    print(f"--- Modal {i} ({len(files)} files) ---")
    print(files)
