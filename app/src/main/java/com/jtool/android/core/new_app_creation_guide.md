# jTool - New App Module Creation Guide

This guide details the complete step-by-step workflow for creating and integrating a new application module into the jTool Android app. Following this structure ensures seamless integration with navigation, the launcher grid, search, gesture triggers, localization, offline caching, and Firestore group sync.

---

## 1. Screen Enum Registration
File: `app/src/main/java/com/jtool/android/core/models/common/enums/AppScreenEnum.kt`
- Add an enum constant for the new app with a unique string `id`:
```kotlin
enum class AppScreenEnum(val id: String) {
    // ...
    MY_NEW_APP("MyNewApp")
}
```

---

## 2. Localization Setup
1. **Domain Localization Object**:
   Create `app/src/main/java/com/jtool/android/core/localization/domains/MyNewAppLoc.kt`:
```kotlin
package com.jtool.android.core.localization.domains

import com.jtool.android.core.localization.Loc

object MyNewAppLoc {
    val myNewApp get() = Loc.s("mynewapp_title", "My New App")
}
```
2. **Register in `Localization.kt`**:
   In `app/src/main/java/com/jtool/android/core/localization/Localization.kt`, add:
```kotlin
val MyNewApp = MyNewAppLoc
```
3. **Add Locale Keys**:
   In `app/src/main/assets/locales/en.json`, `he.json`, `uk.json`:
   Add string keys (e.g. `"mynewapp_title": "My New App"`).

---

## 3. Data Models & Enums
Path: `app/src/main/java/com/jtool/android/core/models/feature/mynewapp/`
- Every model must be annotated with `@Keep` and `@IgnoreExtraProperties`.
- Property `id` must be a `String`.
- All properties must have default values for Firestore compatibility.
- Use `@Serializable` if persisted in local SharedPreferences via Kotlinx Serialization.
- **Enums**: Must be placed in `core/models/feature/mynewapp/enums/MyNewAppStatusEnum.kt` and end with the `Enum` suffix.

---

## 4. Feature Implementation Architecture
Path: `app/src/main/java/com/jtool/android/features/mynewapp/`
1. **`MyNewAppUiState.kt`**: Data class representing UI state (`isLoading: Boolean = false`, data items, filter options, dialog states).
2. **`MyNewAppEvent.kt`**: Sealed interface for user events.
3. **`MyNewAppViewModel.kt`**: ViewModel extending `BaseSyncViewModel()` or `ViewModel()`.
4. **`MyNewAppManager.kt`**: Local persistence manager for offline caching in `SharedPreferences` or JSON if applicable.
5. **`MyNewAppScreen.kt`**: UI Composable:
   - Root layout MUST wrap content using `BaseScreen`.
   - Title set via `GenericTitle` (supports search, filters, back navigation).
   - Primary action uses `UnifiedFAB` or `BaseScreen`'s FAB delegation.
   - Popups: Slide-up `ModalBottomSheet` ending with `Sheet` suffix in `components/my_new_app_sheet/MyNewAppSheet.kt`. No 'Cancel' buttons inside sheets.

---

## 5. Navigation & Screen Properties Registration
File: `app/src/main/java/com/jtool/android/ui/navigation/screens/MyNewApp.kt`
Create the screen object extending `AppProperties`:
```kotlin
package com.jtool.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.features.mynewapp.MyNewAppScreen

object MyNewApp : AppProperties(
    id = AppScreenEnum.MY_NEW_APP,
    icon = Icons.Default.Star,
    getName = { Loc.MyNewApp.myNewApp }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        MyNewAppScreen(
            dataManager = deps.dataManager,
            settingsManager = deps.settingsManager,
            onBack = { deps.setScreen(AppScreenEnum.LAUNCHER) }
        )
    }
}
```

### CRITICAL: Registering in `AppProperties.Companion`
File: `app/src/main/java/com/jtool/android/ui/navigation/AppProperties.kt`
Add your new app object to `AppProperties.allApps`:
```kotlin
companion object {
    val allApps = listOf(WorkTime, Calendar, ShopList, Notes, Cleaning, Links, Trendify, TimeCalc, Vaults, MyNewApp)
    val allScreens = listOf(..., MyNewApp)
}
```

### What Registering in `allApps` Automatically Gives You:
By adding `MyNewApp` to `AppProperties.allApps`:
- ✅ Automatically appears on the main **Launcher Screen** grid.
- ✅ Participates in **Drag & Drop reordering** and user custom layout sorting.
- ✅ Supports 1, 2, 3, 4 column grid layouts with adaptive side-by-side vs stacked cards.
- ✅ Automatically appears in **Hidden Apps** drawer management.
- ✅ Automatically appears in **Gestures** setup sheet (`LauncherGesturesSection`) with its icon and label for Swipe/Gesture shortcuts.
- ✅ Participates in **Search** filtering on the Launcher main menu.

---

## 6. Firestore Rules & Sync (If Group-Synced)
File: `app/src/main/java/com/jtool/android/core/firestore.rules`
If the app stores shared group data under `groups/{groupId}/mynewapp`:
```javascript
match /groups/{groupId}/mynewapp/{docId} {
  allow read, write: if isGroupMember(groupId);
}
```

---

## 7. Strict Code Style & Project Rules
- **No Comments**: Never add inline comments or KDoc blocks to generated Kotlin code.
- **File Size**: Strictly keep all Kotlin files under 500 lines. Split large screens into sub-composables in a `components/` subfolder.
- **Strict Typing**: No `Any` or `Object` placeholders.
- **Gender-Neutral Loc**: All user-facing strings across all languages must be strictly gender-neutral.
