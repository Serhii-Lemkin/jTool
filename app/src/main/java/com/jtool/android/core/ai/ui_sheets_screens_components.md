# Task Instructions: Building Screens, Sheets & UI Components

Use this guide when building or modifying screens, slide-up bottom sheets, floating action containers, pickers, filters, lists, or custom UI components.

---

## 1. Page Consistency & Screen Architecture
* **`BaseScreen` Requirement**: All screens MUST implement `BaseScreen` (`core/components/layout/BaseScreen.kt`) as their root layout wrapper. Loading states (`isLoading`) must be delegated to `BaseScreen`'s centralized loader rather than instantiating local loading boxes or progress indicators within individual screens.
* **Red Container Delegation**: `BaseScreen` automatically embeds `RedContainer` (`core/components/layout/RedContainer.kt`), which manages all floating action buttons, status indicators, and side-switching gestures. Never instantiate separate floating containers on individual screens.
* **Structural Consistency**: New pages must conform strictly to existing project layouts, structural patterns, and implementation styles.

---

## 2. Slide-Up Popups (Sheets)
* **Slide-Up Sheets Pattern**: All primary selection dialogs, confirmation prompts, and input pickers must use a slide-up sheet pattern (e.g., `ModalBottomSheet`) instead of standard centered `AlertDialogs` or system dialogs.
* **Modular Directory Structure**: Each Sheet must reside in its own dedicated subfolder within a `components` directory.
* **Naming Convention**: All Sheet components and their containing subfolders must end with the `Sheet` suffix in their names (e.g., `features/settings/groups/components/invite_sheet/InviteSheet.kt`).
* **Feature Integration**: Sheets should be reusable and isolated from main screen logic, exposing only necessary callbacks for state management.
* **No Cancel Buttons in Sheets**: Never add 'Cancel' buttons inside BottomSheets or slide-up Sheets. Since all Sheets can be dismissed naturally by swiping down or tapping outside, explicit Cancel buttons are redundant and strictly prohibited. Only primary action/confirmation buttons should be rendered inside Sheets.

---

## 3. Input Controls & Selection Components
* **Switch Inputs Over Checkboxes**: Always use `Switch` (sliding toggle) for all boolean selections, checkable list items, and binary filter options instead of standard square `Checkbox` components.
* **Keyboard Obstruction**: Always ensure input fields and action buttons are not obstructed by the software keyboard. Use `Modifier.imePadding()` or `WindowInsets.ime` where appropriate to keep focused elements visible.

---

## 4. Pull-to-Refresh & Paging
* **Refresh Implementation**: Use `androidx.compose.material3.pulltorefresh.PullToRefreshBox`. Bind `isRefreshing` to `uiState.isLoading` and trigger a `Refresh` event.
    * **ViewModel Pattern**: In the `Refresh` handler, call `performRefresh`. Reset local query state if applicable, perform manual fetch, and resume real-time listeners.
* **Paging Implementation**:
    * Maintain a `pagingLimit` in `UiState` (default e.g., 15).
    * Perform filtering/sorting on the full dataset in a `remember` block within the Composable.
    * Derive `pagedData` using `fullData.take(uiState.pagingLimit)`.
    * Show a "Load More" `TextButton` in the `LazyColumn` footer if `fullData.size > uiState.pagingLimit`.
    * The `LoadMore` event should increment `pagingLimit` by page size.
