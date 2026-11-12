import os, re

html_files = [f for f in os.listdir('website/y') if f.endswith('.html')]

nav_pattern = re.compile(r'<nav class="navbar navbar-expand-lg top-nav fixed-top">.*?</nav>', re.DOTALL)
footer_pattern = re.compile(r'<footer class="mt-auto py-4 border-top".*?</footer>', re.DOTALL)
modal_pattern = re.compile(r'<div class="modal fade" id="vizDisclaimerModal".*?</div>\s*</div>\s*</div>', re.DOTALL)

for f in html_files:
    path = os.path.join('website/y', f)
    with open(path, 'r', encoding='utf-8') as file:
        content = file.read()
    
    # Check if components.js is already included to prevent duplication
    if '<script src="/scripts/components.js"></script>' not in content:
        content = content.replace('</head>', '    <script src="/scripts/components.js"></script>\n</head>')

    content = nav_pattern.sub('<jtool-navbar></jtool-navbar>', content)
    content = footer_pattern.sub('<jtool-footer></jtool-footer>', content)
    content = modal_pattern.sub('<jtool-viz-modal></jtool-viz-modal>', content)
    
    with open(path, 'w', encoding='utf-8') as file:
        file.write(content)
        
print("Replacement completed.")
