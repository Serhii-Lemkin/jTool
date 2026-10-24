# Task Instructions: Data Handling, Models & Database Rules

Use this guide when creating or updating data models, modifying Firestore documents, working with schema rules, managing user session state, or defining enums.

---

## 1. Data Models & Type Safety
* **Strict Typing**: Strictly enforce strong typing across all data models. Never use `Any` or `Object` as placeholders. Always use an existing class or define a dedicated model class.
* **Default Values for All Properties**: Every data class property MUST have a default value assigned to support seamless Firestore deserialization via `toObject()`.
* **String IDs**: All data models and Firestore documents MUST use `String` for their `id` property. Never use numeric types for primary identifiers.
* **Timestamp-Based Dates & Times**: Always use `Long` timestamps (UTC milliseconds) for all date and time properties in data models and Firestore. Never store dates or times as `String` (e.g., `"2023-10-01"` or `"09:00"`). For UI display, convert timestamps dynamically in the model or view layer.
* **Database Field Stability**: Never modify, rename, or remove existing data model property names to maintain database schema stability and prevent connection errors across client versions.
* **Firestore Property Keys**: When referencing Firestore field names in code or queries, use `Property::name.name` (e.g., `User::isReviewer.name`) rather than raw string literals.

---

## 2. Enum Extraction & Naming Rules
* **Enum Suffix**: Every `enum class` name MUST end with the `Enum` suffix (e.g., `SupportStatusEnum`, `SupportTypeEnum`).
* **Dedicated Files & Location**: All `enum class` definitions MUST be extracted into their own dedicated files within the `core/models` directory under a relevant category's `enums` subfolder (e.g., `core/models/support/enums/SupportStatusEnum.kt`) or in `core/models/common/enums/` if shared across multiple domains.
* **No Nested Enums**: Never define enums inside other classes, interfaces, or feature logic files.
* **Enum-Based Parameters**: When building generic components or functions accessed via string parameters, map all string options to an enum (string-based or numeric). Never accept loose string parameters directly.

---

## 3. User Data Encapsulation & Cache Isolation
* **Settings Encapsulation in `UserData`**: Every new feature or domain settings set added to `UserData` (Firestore `user_data` collection) MUST be encapsulated into its own dedicated data class object (e.g., `LauncherConfig`, `CalendarConfig`, `WorktimeOptions`) rather than adding flat top-level fields to `UserData`.
* **User Change Awareness & Cache Isolation**: The app must be strictly user-change aware. Before loading any data or initializing view models, applications must actively check whether the current authenticated user UID (`FirebaseAuth.getInstance().currentUser?.uid`) matches the last active user UID via `SettingsManager.checkAndPurgeIfUserChanged(...)`. If a user change is detected (login, logout, or account switch), all local caches, SharedPreferences, and persistence files must be instantly purged, and in-memory state flows reset.

---

## 4. Firestore Security Rules Management
* **Location**: Rules reside in [`app/src/main/java/com/jtool/android/core/firestore.rules`](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/core/firestore.rules).
* **Updates**: When updating database schema or making changes that affect access control, ensure `core/firestore.rules` is updated to match. Never update this file without explicit confirmation from the developer.
* **Maximum Security**: NEVER simplify or loosen security rules for convenience. Only modify them if technically required for a feature while maintaining maximum security access checks.
