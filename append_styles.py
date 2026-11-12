with open('website/y/scripts/components.js', 'r', encoding='utf-8') as f:
    content = f.read()

# Prepend styles for custom elements to ensure they behave properly
css = """
const style = document.createElement('style');
style.textContent = `
    jtool-navbar, jtool-footer, jtool-viz-modal {
        display: block;
        width: 100%;
    }
`;
document.head.appendChild(style);

"""

if 'jtool-navbar {' not in content:
    with open('website/y/scripts/components.js', 'w', encoding='utf-8') as f:
        f.write(css + content)
