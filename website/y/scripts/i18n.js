const translations = {};
const langNames = {
    'en': '🌐 English',
    'uk': '🌐 Українська',
    'he': '🌐 עברית'
};

async function loadTranslations(lang) {
    try {
        const response = await fetch(`/locales/${lang}.json`);
        if (!response.ok) {
            if (lang !== 'en') return loadTranslations('en');
            return;
        }
        translations[lang] = await response.json();

        // Update DOM
        document.querySelectorAll('[data-i18n]').forEach(el => {
            const key = el.getAttribute('data-i18n');
            if (translations[lang][key]) {
                if (el.tagName === 'TITLE') {
                    document.title = translations[lang][key];
                } else if (el.tagName === 'META' && el.name === 'description') {
                    el.content = translations[lang][key];
                } else {
                    el.innerHTML = translations[lang][key];
                }
            }
        });

        document.documentElement.lang = lang;
        if (lang === 'he') {
            document.documentElement.dir = 'rtl';
            document.body.classList.add('rtl');
        } else {
            document.documentElement.dir = 'ltr';
            document.body.classList.remove('rtl');
        }

        // Update the Dropdown Text
        const langBtn = document.getElementById('langDropdown');
        if (langBtn && langNames[lang]) {
            langBtn.innerText = langNames[lang];
        }

        updateUrlParams('lang', lang);
        updateLinksWithParams();

    } catch (e) {
        console.error('Failed to load translations', e);
    }
}

function switchLanguage(lang) {
    localStorage.setItem('lang', lang);
    loadTranslations(lang);
}

function switchTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);

    const icon = theme === 'light' ? '☀️' : '🌙';
    document.querySelectorAll('.theme-icon').forEach(el => el.textContent = icon);

    updateUrlParams('theme', theme);
    updateLinksWithParams();
}

function updateUrlParams(key, value) {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get(key) !== value) {
        urlParams.set(key, value);
        const newUrl = window.location.pathname + '?' + urlParams.toString();
        window.history.replaceState({}, '', newUrl);
    }
}

function updateLinksWithParams() {
    const urlParams = new URLSearchParams(window.location.search);
    document.querySelectorAll('a').forEach(a => {
        const href = a.getAttribute('href');
        // Check if it's a relative link (starts with / but not //)
        if (href && href.startsWith('/') && !href.startsWith('//')) {
            const url = new URL(a.href, window.location.origin);
            if (urlParams.has('lang')) url.searchParams.set('lang', urlParams.get('lang'));
            if (urlParams.has('theme')) url.searchParams.set('theme', urlParams.get('theme'));
            a.href = url.pathname + url.search;
        }
    });
}

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);

    // Theme Init
    // If no theme is selected, default to dark, or system preference
    let theme = urlParams.get('theme') || localStorage.getItem('theme');
    if (!theme) {
        if (window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches) {
            theme = 'light';
        } else {
            theme = 'dark';
        }
    }
    if (!['light', 'dark'].includes(theme)) theme = 'dark';
    switchTheme(theme);

    // Language Init
    let lang = urlParams.get('lang') || localStorage.getItem('lang') || 'en';
    if (!['en', 'uk', 'he'].includes(lang)) lang = 'en';
    localStorage.setItem('lang', lang);
    loadTranslations(lang);
});
