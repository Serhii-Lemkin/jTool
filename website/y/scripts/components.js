
const style = document.createElement('style');
style.textContent = `
    jtool-navbar, jtool-footer, jtool-viz-modal {
        display: block;
        width: 100%;
    }
`;
document.head.appendChild(style);

class JtoolNavbar extends HTMLElement {
    connectedCallback() {
        this.innerHTML = `
    <nav class="navbar navbar-expand-lg top-nav fixed-top">
        <div class="container">
            <a class="navbar-brand nav-logo d-flex align-items-center gap-2 fw-bold" href="/">
                <img src="/512.png" alt="JTools" class="nav-logo-img">
                <span><span class="text-info">J</span>Tools</span>
            </a>
            <button class="navbar-toggler border-0" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarNav">
                <ul class="navbar-nav ms-auto align-items-center">
                    <li class="nav-item"><a class="nav-link px-3" href="/" data-i18n="nav_home">Home</a></li>
                    <li class="nav-item"><a class="nav-link px-3" href="/app-features" data-i18n="nav_app_features">App Features</a></li>
                    <li class="nav-item"><a class="nav-link px-3" href="/changelog" data-i18n="nav_changelog">Changelog</a></li>
                    <li class="nav-item"><a class="nav-link px-3" href="/privacy" data-i18n="nav_privacy">Privacy</a></li>

                    <li class="nav-item dropdown ms-lg-3 mt-3 mt-lg-0">
                        <a class="nav-link dropdown-toggle" href="#" id="themeDropdown" role="button" data-bs-toggle="dropdown">
                            <span class="theme-icon">🌙</span> <span data-i18n="nav_theme">Theme</span>
                        </a>
                        <ul class="dropdown-menu dropdown-menu-end custom-dropdown">
                            <li><a class="dropdown-item" href="#" onclick="switchTheme('dark'); return false;">🌙 <span data-i18n="theme_dark">Dark</span></a></li>
                            <li><a class="dropdown-item" href="#" onclick="switchTheme('light'); return false;">☀️ <span data-i18n="theme_light">Light</span></a></li>
                        </ul>
                    </li>
                    <li class="nav-item dropdown ms-lg-3 mt-3 mt-lg-0">
                        <a class="nav-link dropdown-toggle" href="#" id="langDropdown" role="button" data-bs-toggle="dropdown">
                            🌐 English
                        </a>
                        <ul class="dropdown-menu dropdown-menu-end custom-dropdown">
                            <li><a class="dropdown-item" href="#" onclick="switchLanguage('en'); return false;">English</a></li>
                            <li><a class="dropdown-item" href="#" onclick="switchLanguage('uk'); return false;">Українська</a></li>
                            <li><a class="dropdown-item" href="#" onclick="switchLanguage('he'); return false;">עברית</a></li>
                        </ul>
                    </li>
                </ul>
            </div>
        </div>
    </nav>`;
    }
}
customElements.define('jtool-navbar', JtoolNavbar);

class JtoolFooter extends HTMLElement {
    connectedCallback() {
        this.innerHTML = `
    <footer class="mt-auto py-4 border-top" style="border-color: var(--border) !important;">
        <div class="container text-center text-secondary fs-6">
            <div class="mb-2">
                <a href="/privacy" class="text-info text-decoration-none fw-semibold" data-i18n="footer_privacy">Privacy Policy</a>
            </div>
            <div data-i18n="footer_copy">&copy; 2026 JTools App. Built by <strong class="strong-text">Serhii Lemkin</strong> for the Android ecosystem.</div>
        </div>
    </footer>`;
    }
}
customElements.define('jtool-footer', JtoolFooter);

class JtoolVizModal extends HTMLElement {
    connectedCallback() {
        this.innerHTML = `
        <div class="modal fade" id="vizDisclaimerModal" tabindex="-1" aria-labelledby="vizDisclaimerModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content custom-card p-3" style="background-color: var(--dropdown-bg); backdrop-filter: blur(20px);">
                    <div class="modal-header border-0 pb-0">
                        <h5 class="modal-title fw-bold text-info d-flex align-items-center gap-2" id="vizDisclaimerModalLabel">
                            <span class="material-icons text-info">help_outline</span>
                            <span data-i18n="viz_disclaimer_title">Visualization Disclaimer</span>
                        </h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body text-start py-3">
                        <p class="text-secondary mb-0 fs-6" data-i18n="viz_disclaimer_body">
                            Note: This visualization provides a feel and understanding of the app's layout rather than representing a 100% pixel-perfect snapshot of every screen state.
                        </p>
                    </div>
                    <div class="modal-footer border-0 pt-0">
                        <button type="button" class="btn btn-outline-info rounded-pill px-4" data-bs-dismiss="modal" data-i18n="viz_disclaimer_close">Got it</button>
                    </div>
                </div>
            </div>
        </div>`;
    }
}
customElements.define('jtool-viz-modal', JtoolVizModal);