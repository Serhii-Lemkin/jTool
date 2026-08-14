# Implementation Plan - Refactoring Large Files and Code Cleanup

This plan addresses the request to split files longer than 500 lines into smaller helper files, remove unused imports/comments, and fix existing warnings/errors.

## Proposed Changes

### 1. Refactor `GroupSyncManager.kt` (~938 lines)

I will split `GroupSyncManager` into several smaller components to improve maintainability and reduce clutter.

#### [NEW] [DataParser.kt](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/core/sync/helpers/DataParser.kt)
Extract all parsing logic (`parseShopping`, `parseLinks`, `parseCleaning`, `parseNotes`, `deserializeComments`) into this helper object.

#### [NEW] [RTDBMigrationHelper.kt](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/core/sync/helpers/RTDBMigrationHelper.kt)
Extract the `migrateFromRTDB` method and its associated clearing and migration sub-methods (`clearAllFirestoreData`, `clearSubCollections`, `clearCommentReplies`, `migrateComments`).

#### [MODIFY] [GroupSyncManager.kt](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/core/sync/GroupSyncManager.kt)
- Remove the extracted methods.
- Delegate parsing and migration tasks to the new helpers.
- Clean up unused imports and comments.

---

### 2. Refactor `GroupsScreen.kt` (~557 lines)

I will split the massive `GroupsScreen` Composable into smaller, reusable sub-composables.

#### [NEW] [GroupComponents.kt](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/features/settings/groups/components/GroupComponents.kt)
Extract UI components such as:
- `GroupListItem`
- `CreateGroupDialog`
- `JoinGroupDialog`
- `InviteUserDialog`

#### [MODIFY] [GroupsScreen.kt](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/features/settings/groups/GroupsScreen.kt)
- Simplify the main `GroupsScreen` by using the new components.
- Clean up unused imports and comments.

---

### 3. General Cleanup and Error Fixes

#### [MODIFY] [MainActivity.kt](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/MainActivity.kt)
- Fix deprecation warnings for `onActivityResult` and `startUpdateFlowForResult` by using the modern `ActivityResultLauncher` and the recommended App Update flow.
- Fix styling warnings (missing line breaks, trailing commas).

#### [MODIFY] [AppUpdatesScreen.kt](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/features/settings/updates/AppUpdatesScreen.kt)
- Remove unused parameters (`groupSyncManager`, `settingsManager`, `onBack`).
- Use `String.toUri()` KTX extension.
- Fix logic warnings and unused variables.

#### [PROJECT-WIDE] Unused Imports and Comments
Scan all modified files and remove:
- Unused imports.
- Unintentional comments (keeping those with specific instructions).

## Verification Plan

### Automated Tests
- Run `:app:assembleDebug` to ensure the project still builds.
- (Optional) Run unit tests if they exist and are relevant to the refactored logic.

### Manual Verification
- Verify the Groups screen still works (listing groups, creating/joining).
- Verify App Updates check still functions.
- Ensure the app starts without issues.
