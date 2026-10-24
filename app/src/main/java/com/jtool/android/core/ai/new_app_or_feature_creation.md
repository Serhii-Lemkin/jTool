# Task Instructions: Creating New Apps & Feature Modules

Use this guide when adding a new app module, creating new feature packages, or wiring new screens into the jTool launcher system.

---

## 1. Step-by-Step Creation Workflow
Whenever asked to create or add a new app module to jTool, follow the architectural guide in [`app/src/main/java/com/jtool/android/core/new_app_creation_guide.md`](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/core/new_app_creation_guide.md):

1. **Register App in `AppProperties.allApps`**:
   Add the new app object definition to `AppProperties.allApps` (`core/models/app/AppProperties.kt`). Registering it here automatically wires it into:
   - Launcher grid & List views
   - Drag & Drop reordering
   - Global Search & Gestures system
   - Visibility controls with zero extra boilerplate

2. **Define App Route & Navigation Screen**:
   Add the screen route object in `ui/navigation/screens/` and wire navigation in `AppNavigation.kt`.

3. **Create Feature Directory Structure**:
   Structure feature logic inside `features/<feature_name>/`:
   - `components/` - Sub-components and slide-up Sheets (e.g., `components/<name_sheet>/<NameSheet>.kt`)
   - `logic/` - `UiState.kt`, `Event.kt`, and `ViewModel.kt`
   - `screens/` - Main screen layout composable (e.g., `<FeatureName>Screen.kt`)

4. **ViewModel & State Management**:
   - Inherit from `BaseSyncViewModel`.
   - Use `MutableStateFlow` with immutable `StateFlow` for state.
   - Handle loading states via `_isLoading` flow mapped to `UiState.isLoading`.
   - Support `Refresh` event calling `performRefresh`.

5. **Screen Implementation**:
   - Wrap main screen layout in `BaseScreen`.
   - Pass `title`, `isLoading`, `onBack`, `onRefresh`, and search/actions handlers.
   - Use `UnifiedFAB` for primary action FAB.
