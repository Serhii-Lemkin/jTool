# Gemini System Instructions & Architectural Index

This document serves as the master entry point for project guidelines, architectural standards, and task-specific skill instructions.

---

## 1. Universal Principles (Applies to EVERY Request)
Whenever modifying code, fixing bugs, or implementing features, strictly adhere to these baseline rules:
1. **No Comments**: Never add inline comments, docstrings, or KDoc blocks to generated code. Keep code self-documenting, concise, and clean.
2. **Minimal Code Changes & Regression Safety**: Prioritize making the least amount of code changes necessary. Keep edits targeted and surgical, ensuring existing functionality remains intact without regressions.
3. **Build Verification**: Always verify that the project builds successfully (`gradle_build("app:assembleDebug")`) before finishing a request. Resolve any introduced compilation errors immediately.
4. **Localization via `Loc`**: Never hardcode user-facing strings. Add all new display text to `Loc` (`core/localization/Loc.kt`) and domain localization files using gender-neutral phrasing across all supported languages.
5. **Single Official Changelog, Categorization & Version Header**: Continuously update the single official changelog file at [`website/y/changelogs/changelog.txt`](file:///var/home/serhii/AndroidStudioProjects/jTool/website/y/changelogs/changelog.txt) whenever user-facing changes are made. Always preserve the version header (`[Version]\nv2.8.3`). Strictly categorize changelog entries as follows:
   - `[Big Changes]`: Reserved strictly for major new features, new full screens/modules, or fundamental new architectural shifts (e.g., Share Import Wizard, Universal Slide-Up sheets, Dedicated Reminders screen, Service-Worker Upload Queueing).
   - `[Minor Changes]`: For secondary features, UI options, secondary settings, tag additions, or minor enhancements (e.g., Local Vault Pinning to Top, Preferred Date Format setting, Expanded tags, custom checkbox styles).
   - `[Bug Fixes]`: Specifically for resolving crashes, broken behavior, double loaders/extensions, or unexpected UI glitches (e.g., Crash Protection for Updating Records, Double Extension Removal, Top Search Auto-Clearing).
   Do not maintain duplicate changelogs in `app/`. When starting a new version, move the previous version's release notes to `website/y/changelogs/history/{version}.txt` and update the historical changelogs page.
6. **Centralized Web Links**: All website links (Privacy Policy, Landing page, Feature Learn-More pages) MUST route through `WebUtils.openAppPage` using dynamically fetched and cached URLs from Firestore (`app_options/config`). Never hardcode website URLs or duplicate `Intent.ACTION_VIEW` logic across individual screens.
7. **Instruction Updating Strategy**: When asked to update instructions, determine where the rule belongs. If it is a common instruction applying to ALL requests, add it directly to this main `instructions.md`. If it touches a specific workflow or specialized task, create or update a dedicated skill `.md` file in `core/ai/`.

---

## 2. Specialized Task Skills Index
Depending on the task being performed, you MUST consult and follow the corresponding specialized skill document in the `core/ai/` directory:

### 📊 [Data Handling, Models & Database Rules](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/core/ai/data_handling.md)
* **When to use**: Defining data models, updating Firestore schema/rules, creating enums, managing `UserData` encapsulation, handling timestamps/IDs, or handling user change cache purges.
* **Key Topics**: Strong typing (no `Any`/`Object`), default property values, `String` IDs, `Long` UTC timestamps, `Enum` suffix, Firestore `Property::name.name` keys, `core/firestore.rules` security, `SettingsManager.checkAndPurgeIfUserChanged`.

### ⚡ [Background File Transfers & Actions Container](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/core/ai/background_transfers.md)
* **When to use**: Handling background file uploads, downloads, public exports, notifications, floating progress indicators, or Actions Container gestures.
* **Key Topics**: Queue managers & background services (`FileUploadService`, `FileDownloadService`, `FileExportService`), FAB color coding (Blue Upload, Green Download, Orange Export, Red Notif), `FloatingProgressIndicator` delegation, Actions Container >50% drag side persistence, `LauncherScreen` header icons sync.

### 📱 [UI Design: Screens, Sheets & Components](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/core/ai/ui_sheets_screens_components.md)
* **When to use**: Creating or updating UI screens, slide-up bottom sheets, input controls, pickers, filters, pull-to-refresh, or paging.
* **Key Topics**: Mandatory `BaseScreen` layout wrapper, centralized loader, Actions Container delegation, slide-up `ModalBottomSheet` naming & subfolder placement (no Cancel buttons inside sheets), `Switch` inputs over `Checkbox`, `PullToRefreshBox`, `pagingLimit` paging pattern, `Modifier.imePadding()`.

### 🚀 [Creating New Apps & Feature Modules](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/core/ai/new_app_or_feature_creation.md)
* **When to use**: Adding a new app module, creating a new feature directory, or registering screens in the launcher.
* **Key Topics**: Registering in `AppProperties.allApps` for automatic Launcher, Search, Gestures, and Visibility wiring; `new_app_creation_guide.md` workflow; ViewModel structure (`BaseSyncViewModel`, `UiState`, `Event`).

### 🛠️ [Code Changes, Build Verification & Reporting](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/core/ai/code_changes_and_reporting.md)
* **When to use**: Preparing code output, verifying builds, handling localization, or writing developer changelog summaries.
* **Key Topics**: Code formatting, minimal surgical diffs, 500-line file limit, gender-neutral localization rules, `gradle_build` verification, single official changelog at `website/y/changelogs/changelog.txt`.

### 🌐 [Website & App Landing Pages Sync](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/core/ai/website_and_app_pages.md)
* **When to use**: Updating website landing pages, app feature descriptions, or synchronization between Android apps and website descriptions.
* **Key Topics**: Repository website path [`website/`](file:///var/home/serhii/AndroidStudioProjects/jTool/website/), app "Learn More" links, `WebUtils` dynamic URL fetching & local caching, Alpha testing modal access flow (`jackflit51997@gmail.com`), single changelog file at [`website/y/changelogs/changelog.txt`](file:///var/home/serhii/AndroidStudioProjects/jTool/website/y/changelogs/changelog.txt), website i18n (`en.json`, `uk.json`, `he.json`).

---

# Global Setup for New Chats
To maintain consistency across all sessions, copy the following block into **Android Studio > Settings > Tools > Gemini > Custom Instructions**:

```text
Follow the architectural guidelines in 'app/src/main/java/com/jtool/android/core/instructions.md' and task skill files in 'core/ai/'.
- No comments/KDoc in code.
- Minimal code changes & no regressions.
- Strict typing (no Any/Object), String IDs, default property values, UTC timestamps.
- All Enums must have 'Enum' suffix and stay in 'core/models/.../enums/'.
- All UI strings must use Localization Service (Loc) with gender-neutral phrasing.
- All screens must use BaseScreen wrapper; slide-up sheets use 'Sheet' suffix in dedicated folder (no Cancel buttons).
- Use Switch over Checkbox.
- Route file transfers via background services & queue managers with Actions Container color rules.
- Maintain single official changelog at 'website/y/changelogs/changelog.txt' with version header.
- Keep website/ source files updated when app features or descriptions are updated.
- Route all web links via WebUtils.openAppPage with dynamic db fetching and local caching.
- Add common instructions to main instructions.md and workflow-specific rules to new/existing core/ai/ skill files.
```
