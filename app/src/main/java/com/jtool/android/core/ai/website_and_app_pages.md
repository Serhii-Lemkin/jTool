# Task Instructions: Website & App Landing Pages Sync

Use this guide when updating app descriptions, adding new features to jTool apps, or modifying website pages hosted inside the repository under `website/`.

---

## 1. Repository Website Files Location
The official jTool public website source files are maintained directly within this git repository at:
* Website Root: [`website/`](file:///var/home/serhii/AndroidStudioProjects/jTool/website/)
* Public HTML Assets: [`website/y/`](file:///var/home/serhii/AndroidStudioProjects/jTool/website/y/)
* Standalone Red Container Page: [`website/y/red-container.html`](file:///var/home/serhii/AndroidStudioProjects/jTool/website/y/red-container.html)
* App Features Page: [`website/y/app-features.html`](file:///var/home/serhii/AndroidStudioProjects/jTool/website/y/app-features.html)
* Localization Files: [`website/y/locales/`](file:///var/home/serhii/AndroidStudioProjects/jTool/website/y/locales/) (`en.json`, `uk.json`, `he.json`)
* Single Official Changelog File: [`website/y/changelogs/changelog.txt`](file:///var/home/serhii/AndroidStudioProjects/jTool/website/y/changelogs/changelog.txt)

---

## 2. Mandatory Synchronization & Single Changelog Rule
Whenever app features, descriptions, or capabilities are created, modified, or updated in the Android application code:
1. **App Web Link**: Ensure the Android app screen top bar actions menu includes "Learn More" via `WebUtils.openAppPage(context, settingsManager, dataManager, "<app-path>")` passing `lang` and `theme` query parameters.
2. **Website Page Updates**: Update the corresponding HTML page inside `website/y/` (`vaults.html`, `calendar.html`, `work-tracker.html`, `shopping-list.html`, `personal-hub.html`, `cleaning.html`, `trendify.html`, `privacy.html`, `index.html`, `changelog.html`, `red-container.html`, `app-features.html`).
3. **Locale Key Sync**: Ensure all localized feature descriptions in `website/y/locales/en.json`, `uk.json`, and `he.json` match the new capabilities across all supported languages.
4. **Single Official Changelog**: Update ONLY [`website/y/changelogs/changelog.txt`](file:///var/home/serhii/AndroidStudioProjects/jTool/website/y/changelogs/changelog.txt). Do NOT create or maintain duplicate changelog files in `app/`.
5. **Always Include Version Header**: Every changelog update MUST preserve the version header at the top (`[Version]\nv2.8.0 (Build 59)`). The developer will purge historical entries after a new version release.

---

## 3. Alpha Testing Access Flow & Community Messaging
* **Alpha Mode**: jTool is currently in active Alpha.
* **Access Request & Messaging**: Do NOT claim or offer "Free Lifetime Access". Encourage users to connect, test new features, and help shape the app. The "Become a Tester" button triggers an Alpha modal instructing users to submit their Google email address to `jackflit51997@gmail.com` so the developer can add them to the authorized Google Play testing pool.

---

## 4. Dynamic URL Resolution & WebUtils Centralization
* **No Hardcoded Base Domains**: Base website URLs MUST NOT be hardcoded in app screens or utility classes. `WebUtils.openAppPage` dynamically fetches the base domain from Firestore (`app_options/config` document, fields `website` or `privacy`) via `dataManager.getAppOptions()`.
* **Local Phone Caching**: The base domain URL is automatically cached in SharedPreferences via `SettingsManager.setCachedWebsiteUrl(...)`. If offline or when Firestore is inaccessible, `WebUtils` falls back to `SettingsManager.getCachedWebsiteUrl()`.
* **Single Centralized Handler**: ALL external website navigation across the app (Privacy Policy, Landing/About App page, Feature Learn-More pages) MUST route through `WebUtils.openAppPage(context, settings, dataManager, path)`. Never duplicate URL construction or `Intent.ACTION_VIEW` logic in individual screens.

---

## 5. Comprehensive Feature Documentation & Settings Standards
When creating or updating app guide pages on the website (e.g. `work-tracker.html`, `vaults.html`, `calendar.html`, `shopping-list.html`):
1. **Complete Coverage**: Explain every aspect and mechanic users need to fully understand how the app works (e.g., Quick Start vs Event-Linked vs Weekly Schedule modes, auto-logging to Google Calendar, tracking any calendar event, overtime calculation, settings options).
2. **Every Setting Explained**: Every app feature page MUST include a dedicated **Settings Section** detailing EVERY setting and configuration option available in the Android app for that module (e.g., Weekly Start Day, Target Shift Duration, Work Shift Color Category, Google Calendar Auto-Logging Destination).
3. **Pro Tips**: Include a dedicated **Pro Tips** section highlighting non-obvious power-user workflows (e.g., configuring dedicated secondary Google Calendars, desk timer high-contrast mode, aisle auto-grouping).
4. **FAQ Section**: Include a **Frequently Asked Questions (FAQ)** section answering common points of confusion (e.g., tracking non-work events, forgetting to stop timers, auto-logging mechanics).

---

## 6. Standalone Red Container Page & Mandatory Link Rule
* **Red Container Page**: [`website/y/red-container.html`](file:///var/home/serhii/AndroidStudioProjects/jTool/website/y/red-container.html) serves as the standalone explanation page for JTools's floating action hub and background task architecture.
* **Content**: Documents side-switching drag gestures (>50% screen width), edge collapsing into color-coded pills, persistent state across app launches, FAB colors (Blue Upload, Green Download, Orange Export, Red Notification, Purple Primary Action), and Launcher top app bar header sync.
* **Mandatory Feature Link**: Every feature app page that embeds or utilizes the Red Container MUST include a link banner or button pointing to `/red-container`.

---

## 7. Mandatory App Material Icons Parity & Exact 1:1 UI Replicas
When illustrating app features on website pages (e.g. timers, input containers, floating action bars, list items):
1. **Mandatory Material Icons Parity**: All website pages MUST use the exact Google Material Icons font glyphs (`schedule`, `calendar_month`, `shopping_cart`, `description`, `brush`, `link`, `show_chart`, `calculate`, `folder_special`, `widgets`) used in the Android app.
2. **Exact 1:1 UI Replicas**: Visual replicas on website pages MUST be exact 1:1 representations of Android app screens (e.g., Smart Shopping with Aisle chips, Checkboxes, Author names, Quantity pills, and Fixed Bottom Adding Bar; Workday Tracker screen with top segmented tabs, event selector row, 220px circular clock widget, and bottom action controls), avoiding generic creative adaptations.
3. **Multi-Screen & Sheet Coverage**: For features with multiple screens or bottom sheets (e.g. Edit Item Sheet, Today Events Sheet, Settings Sheet), display separate 1:1 UI replicas for each screen/sheet.
4. **Interactive Replicas**: Where applicable (e.g., Red Container widget side-switching at >50% drag, Launcher app hiding replica), make replicas interactive so users can drag, toggle, or test gestures directly on the website.
