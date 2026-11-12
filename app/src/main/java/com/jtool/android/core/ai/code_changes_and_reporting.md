# Task Instructions: Code Changes, Build Verification & Reporting

Use this guide for code generation standards, localization rules, build verification, and developer changelog reporting across all tasks.

---

## 1. Universal Coding Rules
* **No Comments**: NEVER add inline comments, docstrings, or KDoc blocks to generated code. Keep code self-documenting, concise, and clean.
* **Minimal Code Changes**: When fixing bugs or modifying features, prioritize making the least amount of code changes necessary. Keep edits targeted and surgical, ensuring existing functionality remains intact without regressions.
* **File Size Limit**: Strictly keep all Kotlin files under 500 lines. If a file exceeds or is nearing this limit, split it into smaller helper classes, components, or sub-viewmodels immediately.

---

## 2. Localization & Internationalization
* **Dynamic Resource Strings**: Never hardcode user-facing strings in UI code or viewmodels. Add all new display text to `Loc` (`core/localization/Loc.kt`) and domain localization files across all supported language resource files.
* **Dynamic Language Resolution**: Never hardcode lists of supported languages. Always query and resolve language options dynamically from implemented language resource files at runtime.
* **Gender-Neutral Phrasing**: All user-facing strings across all languages (specifically Hebrew and Ukrainian) must use strictly gender-neutral language.
    * **No Slashes**: Avoid gendered slashes (e.g., `את/ה`, `בטוח/ה`, `בחר/і`).
    * **Impersonal/Infinitive Forms**: Rephrase verbs to use infinitives or impersonal/passive constructions to ensure an inclusive tone for all users.

---

## 3. Build Verification & Safety
* **Build Verification**: Always run project build verification (`gradle_build("app:assembleDebug")`) before finishing a task. If compilation errors are introduced, resolve them immediately.
* **Testing**: Run relevant tests if applicable to ensure no regressions.

---

## 4. Single Website Changelog & Version Reporting
* **Single Official Changelog**: Maintain ONLY ONE active changelog file located at [`website/y/changelogs/changelog.txt`](file:///var/home/serhii/AndroidStudioProjects/jTool/website/y/changelogs/changelog.txt). When starting a new version, move the previous version's changelog to `website/y/changelogs/history/{version}.txt` and update historical notes. Never create or update duplicate changelog files in `app/`.
* **Always Include Version Header**: Every changelog update MUST include the version header section at the very top:
  ```text
  [Version]
  v2.8.3
  ```
  Ensure the version name and build code match `versionName` and `versionCode` in `app/build.gradle.kts`.
* **Release Notes Format**: The changelog MUST be 100% copy-paste ready for Google Play Console release notes (concise, user-friendly language, plain text bullet points, free of developer jargon or code paths).
  Organize updates into distinct sections:
  1. `[Version]` — Current version name and build number
  2. `[Big Changes]` — Major new features or fundamental shifts in functionality/architecture
  3. `[Minor Changes]` — Small features, secondary settings, UI options (e.g. Pinning vaults, Date format settings)
  4. `[Bug Fixes]` — Specifically for resolving crashes, broken behavior, double loaders, or regressions
* **Post-Release Purging**: The developer will purge historical entries in `website/y/changelogs/changelog.txt` after a new version release.
* **Technical Summaries**: End chat responses with a clear summary of modifications, including technical implementation details and an assessment of potential risks introduced by the changes.
