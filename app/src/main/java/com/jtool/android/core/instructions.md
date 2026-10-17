# Gemini System Instructions & Guidelines

## 1. Code Generation & Refactoring
* **No Comments:** Never add inline comments or documentation blocks to generated code.
*   **Firestore Rules Management**: When updating the database schema or making any changes that affect access control, ensure the `core/firestore.rules` file is updated to the latest version. Never update this file without explicit confirmation from the developer. NEVER simplify the rules for convenience; only modify them if technically required for a new feature while maintaining maximum security.
*   **Strong Typing & Generics**: Strictly enforce strong typing across all data models. Abstract shared functionality using interfaces and generic implementations where applicable.
*   **String IDs**: All data models and Firestore documents must use `String` for their `id` property. Never use numeric types for identifiers.
*   **Enum Extraction, Naming & Location**: All `enum class` definitions must be extracted into their own dedicated files. Every enum class name must end with the `Enum` suffix (e.g., `SupportStatusEnum`). These files must be placed within the `core/models` directory under a relevant category's `enums` subfolder (e.g., `core/models/support/enums/SupportStatusEnum.kt`) or in `core/models/common/enums/` if shared across multiple domains. Never define enums inside other classes or feature logic files.
*   **Strict Type Definitions**: Never use `Any` (or `Object`) as a class placeholder. Always use an existing class or create a new dedicated class if a suitable model does not exist.
* **Enum-Based Parameters:** When building generic components or functions accessed via string parameters, map all string options to an enum (string-based or numeric). Never accept loose string parameters directly, and ensure existing code remains compatible without breakage.
* **Component Extraction Prompting:** Whenever an opportunity arises to modularize code, prompt the developer to extract the logic into a reusable file and add it to the Admin Panel "Kitchen Sink."
* **File Size Limit:** Strictly keep all Kotlin files under 500 lines. If a file exceeds or is nearing this limit, split it into smaller helper classes, components, or sub-viewmodels immediately.
* **Database Field Stability**: Never modify, rename, or remove existing data model property names to maintain database schema stability and prevent connection errors.
* **Timestamp-Based Dates & Times**: Always use `Long` timestamps (UTC milliseconds) for all date and time properties in data models and Firestore. Never store dates or times as `String` (e.g., "2023-10-01" or "09:00"). For UI display, convert these timestamps dynamically in the model or view layer.

## 2. Verification & Build Safety
* **Build Verification**: Always verify that the project builds successfully before finishing a request. If compilation errors are introduced, they must be resolved immediately.
* **Testing**: Run relevant tests (unit or UI) if applicable to ensure no regressions.

## 3. Localization & Internationalization
* **Dynamic Resource Strings**: Never hardcode user-facing strings. Add all new display text to the Localization Service across all supported language files.
* **Dynamic Language Resolution**: Never hardcode lists of supported languages. Always query and resolve language options dynamically from the implemented language resource files at runtime.
* **Gender-Neutral Phrasing**: All user-facing strings across all languages (specifically Hebrew and Ukrainian) must use strictly gender-neutral language. 
    *   **No Slashes**: Avoid gendered slashes (e.g., `את/ה`, `בטוח/ה`, `בחר/і`).
    *   **Impersonal/Infinitive Forms**: Rephrase verbs to use infinitives (e.g., `חישוב`, `בחירה`) or impersonal/passive constructions (e.g., `בפעולה זאת... האם להמשיך?`, `відхилено`) to ensure a professional and inclusive tone for all users.

## 4. Page Consistency & Architectural Changes
* **Structural Consistency:** New pages must conform strictly to existing project layouts, structural patterns, and implementation styles.
* **Base Screen & Centralized Loading**: All screens MUST implement `BaseScreen` as their root layout wrapper. Loading states (`isLoading`) must be delegated to `BaseScreen`'s centralized loader rather than instantiating ad-hoc local loading boxes or progress indicators within individual screen implementations.
* **Red Container Delegation**: `BaseScreen` automatically embeds `RedContainer` (`core/components/layout/RedContainer.kt`), which manages all floating action buttons, status indicators, and side-switching gestures. Never instantiate separate floating containers on individual screens.
* **Change Approvals:** Any proposed deviations or new architectural implementations must be backed by a clear technical justification and explicitly prompted to the user for approval before applying.

## 5. UI Patterns: Popups, Paging & Refresh
* **Slide-Up Popups (Sheets)**: All primary selection dialogs, confirmation prompts, and input pickers must use a slide-up popup pattern (e.g., `ModalBottomSheet`) instead of standard centered `AlertDialogs` or system dialogs. This ensures a consistent interaction model across the application.
    *   **Modular Structure**: Each Sheet must reside in its own dedicated subfolder within a `components` directory.
    *   **Naming Convention**: All Sheet components and their containing subfolders must end with the `Sheet` suffix in their names (e.g., `features/settings/groups/components/invite_sheet/InviteSheet.kt`).
    *   **Feature Integration**: Sheets should be reusable and isolated from the main screen logic, exposing only necessary callbacks for state management.
    *   **No Cancel Buttons in Sheets**: Never add 'Cancel' buttons inside BottomSheets or slide-up Sheets. Since all Sheets can be dismissed naturally by swiping down or tapping outside, explicit Cancel buttons are redundant and strictly prohibited. Only primary action/confirmation buttons should be rendered inside Sheets.
* **Switch Inputs Over Checkboxes**: Always use `Switch` (sliding toggle) for all boolean selections, checkable list items, and binary filter options instead of standard square `Checkbox` components.
*   **Refresh Implementation**: Use `androidx.compose.material3.pulltorefresh.PullToRefreshBox`. Bind `isRefreshing` to `uiState.isLoading` and trigger a `Refresh` event.
    *   **ViewModel Pattern**: In the `Refresh` handler, first set `isLoading = true` and `data = emptyList()`. Perform a manual fetch if needed, then resume the real-time listener. This ensures the UI is reset to the server's state and prevents "expired" cached data from lingering.
*   **Paging Implementation**: 
    *   Maintain a `pagingLimit` in the `UiState` (default e.g., 15).
    *   Perform filtering/sorting on the full dataset in a `remember` block within the Composable.
    *   Derive `pagedData` using `fullData.take(uiState.pagingLimit)`.
    *   Show a "Load More" `TextButton` in the `LazyColumn` footer if `fullData.size > uiState.pagingLimit`.
    *   The `LoadMore` event should increment `pagingLimit` by the page size.

## 6. Background File Transfers & Red Container Architecture

### A. Centralized Background Services & Queue Managers
All file operations across the application MUST run through dedicated background foreground services and queue managers. Never perform file uploads, downloads, or exports directly on the UI thread or within view models:
1. **File Uploads**: `FileUploadQueueManager` & `FileUploadService` (`core/services/FileUploadService.kt`)
2. **Offline Cache Downloads**: `FileDownloadQueueManager` & `FileDownloadService` (`core/services/FileDownloadService.kt`)
3. **Public Exports to Downloads**: `FileExportQueueManager` & `FileExportService` (`core/services/FileExportService.kt`)
4. **Notifications**: `NotificationQueueManager` (`core/services/NotificationQueueManager.kt`)

### B. Color Coding Rules for Floating Action Buttons
Every FAB and status indicator inside the Red Container has a strictly enforced, distinct color:
* **Blue (`Color(0xFF2563EB)`)**: Active File Uploads (`FloatingUploadProgressIndicator`)
* **Green (`Color(0xFF16A34A)`)**: Active Offline Cache Downloads (`FloatingDownloadProgressIndicator`)
* **Orange (`Color(0xFFEA580C)`)**: Active Public Exports (`FloatingExportProgressIndicator`)
* **Red (`MaterialTheme.colorScheme.error`)**: Unread Notifications
* **Primary (`MaterialTheme.colorScheme.primary`)**: Child Screen Actions (`UnifiedFAB` / `floatingActionButton`)

### C. Creating New FABs & Indicators
When creating new floating action buttons or progress indicators:
* **Delegation to `FloatingProgressIndicator`**: Always delegate UI rendering to `FloatingProgressIndicator` (`features/vaults/components/floating_progress_indicator/FloatingProgressIndicator.kt`). This handles circular progress rings, sizing, spring entrance/exit animations (`scaleIn` / `scaleOut`), and click callbacks.
* **Spring Animation Specs**: Use bouncy spring specs (`dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow`) for button entrance/exit transitions.

### D. Red Container Gestures & Side Persistence
* **Deliberate Side-Switching**: Dragging the Red Container horizontally across **more than 50% of the screen width** (`screenWidth / 2`) flips the container side between Right and Left, persisting the choice locally in `SettingsManager`. Dragging less than 50% springs back without changing sides.
* **Edge Collapsing**: Dragging the container towards the screen edge collapses it into a vertical stack of small, color-coded edge pills.
* **Persistent Collapsed State**: The collapsed or expanded state is saved in `SettingsManager` and remains preserved across screen changes and app launches until the user expands it again via tap or drag.

### E. Main Menu / Launcher Header Sync (`LauncherScreen.kt`)
Whenever active tasks or notifications exist in `FileUploadQueueManager`, `FileDownloadQueueManager`, `FileExportQueueManager`, or `NotificationQueueManager`:
* `LauncherScreen` MUST render corresponding smaller color-coded icon buttons (Blue Upload, Green Download, Orange Export, Red Notification) in its top app bar header next to the notification bell.
* Tapping any header button opens its respective progress sheet (`UploadProgressSheet`, `DownloadProgressSheet`, `ExportProgressSheet`, or `NotificationsSheet`).

## 7. State & Async Handling
* **Safe Loading States:** Guarantee every loading state execution path (success, failure, cancellation, or exception) resolves properly to prevent deadlocks or frozen UIs.
* **Keyboard Obstruction:** Always ensure that input fields and action buttons are not obstructed by the software keyboard. Use `Modifier.imePadding()` or `WindowInsets.ime` where appropriate to keep the focused element visible.
* **User Change Awareness & Cache Isolation**: The app must be strictly user-change aware. Before loading any data or initializing view models, applications must actively check whether the current authenticated user UID (`FirebaseAuth.getInstance().currentUser?.uid`) matches the last active user UID via `SettingsManager.checkAndPurgeIfUserChanged(...)`. If a user change is detected (login, logout, or account switch), all local caches, SharedPreferences, and persistence files must be instantly purged. In-memory state flows must start empty. This prevents stale or sensitive data from the previous user from showing up even for a split second.
* **Error Notification:** If an unresolved loading state risk arises, flag the issue to the developer immediately to determine a resolution.

## 8. Change Logs & Developer Reporting
* **Technical Summaries:** End responses with a clear summary of modifications, including technical implementation details and an assessment of potential risks introduced by the changes.

---

# Global Setup for New Chats
To maintain consistency across all sessions, copy the following block into **Android Studio > Settings > Tools > Gemini > Custom Instructions**:

```text
Follow the architectural guidelines in 'app/src/main/java/com/jtool/android/core/instructions.md'.
- No comments/KDoc in code.
- Strict typing (no Any/Object).
- All Enums must have 'Enum' suffix and stay in 'core/models/.../enums/'.
- All IDs must be Strings.
- Data models must have default values for all properties (Firestore compatibility).
- Use Property::name.name for Firestore keys.
- Use Localization Service (Loc) for all UI strings.
- Route file transfers via background services & queue managers.
- Respect Red Container color rules (Blue=Upload, Green=Download, Orange=Export, Red=Notif).
- All FAB progress indicators must delegate to FloatingProgressIndicator.
- Always use Switch instead of Checkbox. No Cancel buttons in Sheets.
```
