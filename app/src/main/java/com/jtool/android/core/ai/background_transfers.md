# Task Instructions: Background Transfers & Red Container Architecture

Use this guide when implementing file uploads, downloads, public exports, notifications, or managing floating action buttons (FABs) and progress indicators inside the Red Container or Launcher header.

---

## 1. Centralized Background Services & Queue Managers
All file operations across the application MUST run through dedicated background foreground services and queue managers. Never perform file uploads, downloads, or exports directly on the UI thread or within ViewModels:
1. **File Uploads**: `FileUploadQueueManager` & `FileUploadService` (`core/services/FileUploadService.kt`)
2. **Offline Cache Downloads**: `FileDownloadQueueManager` & `FileDownloadService` (`core/services/FileDownloadService.kt`)
3. **Public Exports to Downloads**: `FileExportQueueManager` & `FileExportService` (`core/services/FileExportService.kt`)
4. **Notifications**: `NotificationQueueManager` (`core/services/NotificationQueueManager.kt`)

---

## 2. Color Coding Rules for Floating Action Buttons & Indicators
Every FAB and status indicator inside the Red Container has a strictly enforced, distinct color scheme:
* **Blue (`Color(0xFF2563EB)`)**: Active File Uploads (`FloatingUploadProgressIndicator`)
* **Green (`Color(0xFF16A34A)`)**: Active Offline Cache Downloads (`FloatingDownloadProgressIndicator`)
* **Orange (`Color(0xFFEA580C)`)**: Active Public Exports (`FloatingExportProgressIndicator`)
* **Red (`MaterialTheme.colorScheme.error`)**: Unread Notifications
* **Primary (`MaterialTheme.colorScheme.primary`)**: Child Screen Actions (`UnifiedFAB` / `floatingActionButton`)

---

## 3. Creating FABs & Progress Indicators
* **Delegation to `FloatingProgressIndicator`**: Always delegate UI rendering to `FloatingProgressIndicator` (`features/vaults/components/floating_progress_indicator/FloatingProgressIndicator.kt`). This handles circular progress rings, sizing, spring entrance/exit animations (`scaleIn` / `scaleOut`), and click callbacks.
* **Spring Animation Specs**: Use bouncy spring specs (`dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow`) for button entrance/exit transitions.

---

## 4. Red Container Gestures & Side Persistence
* **Deliberate Side-Switching**: Dragging the Red Container horizontally across **more than 50% of the screen width** (`screenWidth / 2`) flips the container side between Right and Left, persisting the choice locally in `SettingsManager`. Dragging less than 50% springs back without changing sides.
* **Edge Collapsing**: Dragging the container towards the screen edge collapses it into a vertical stack of small, color-coded edge pills.
* **Persistent Collapsed State**: The collapsed or expanded state is saved in `SettingsManager` and remains preserved across screen changes and app launches until the user expands it again via tap or drag.

---

## 5. Main Menu / Launcher Header Sync (`LauncherScreen.kt`)
Whenever active tasks or notifications exist in `FileUploadQueueManager`, `FileDownloadQueueManager`, `FileExportQueueManager`, or `NotificationQueueManager`:
* `LauncherScreen` MUST render corresponding smaller color-coded icon buttons (Blue Upload, Green Download, Orange Export, Red Notification) in its top app bar header next to the notification bell.
* Tapping any header button opens its respective progress sheet (`UploadProgressSheet`, `DownloadProgressSheet`, `ExportProgressSheet`, or `NotificationsSheet`).
