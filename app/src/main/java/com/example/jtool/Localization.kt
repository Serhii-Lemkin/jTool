package com.example.jtool

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Loc {
    var lang by mutableStateOf("en")

    fun t(en: String, uk: String): String {
        return if (lang == "uk") uk else en
    }

    // Launcher
    val menuTitle get() = t("Tool Menu", "Меню")
    val hello get() = t("Hello", "Привіт")
    val settings get() = t("Settings", "Налаштування")

    // Workday
    val today get() = t("Today", "Сьогодні")
    val requiredHours get() = t("Required Hours", "Робочі години")
    val startTime get() = t("Start Time", "Початок")
    val calculate get() = t("Calculate", "Обчислити")
    val remember get() = t("Remember", "Запам'ятати")
    val forget get() = t("Forget", "Забути")
    val forgetTitle get() = t("Forget Time?", "Забути час?")
    val forgetConfirm get() = t("Do you want to delete the saved start time for today?", "Бажаєте видалити збережений час початку на сьогодні?")
    val finishAt get() = t("Finish at", "Завершення о")
    val timeLeft get() = t("Time left", "Залишилось")
    val workFinished get() = t("Work day finished!", "Робочий день закінчено!")
    val delete get() = t("Delete", "Видалити")
    val cancel get() = t("Cancel", "Скасувати")
    val invalidFormat get() = t("Invalid format. Try 6.13, 613, or 06:13", "Невірний формат. Спробуйте 6.13, 613 або 06:13")
    val noWorkScheduled get() = t("No work hours scheduled for today!", "На сьогодні робочих годин не заплановано!")

    // Time Calc
    val baseDateTime get() = t("Base Date/Time", "Базовий Час/Дата")
    val addSub get() = t("Add / Subtract", "Додати / Відняти")
    val add get() = t("Add", "Додати")
    val sub get() = t("Sub", "Відняти")
    val history get() = t("History (Tap to restore)", "Історія (Натисніть для відновлення)")
    val added get() = t("Added", "Додано")
    val subtracted get() = t("Subtracted", "Віднято")
    val resultedIn get() = t("Resulted in", "Результат")

    // ShopList
    val shoppingList get() = t("ShopList", "Покупки")
    val addItem get() = t("Add Item", "Додати товар")
    val sortByAisle get() = t("Sort by Aisle", "Сортувати за відділом")
    val recentlyChecked get() = t("Recently Checked", "Нещодавно куплені")
    val restore get() = t("Restore", "Відновити")
    val itemAlreadyInList get() = t("Item already in list", "Товар вже у списку")
    val restoredExisting get() = t("Restored existing item", "Відновлено існуючий товар")

    // Notes
    val notes get() = t("Notes", "Нотатки")
    val newNote get() = t("New Note", "Нова нотатка")

    // Links
    val links get() = t("Links", "Посилання")
    val searchLinks get() = t("Search links...", "Пошук посилань...")
    val linkTitle get() = t("Title", "Назва")
    val linkUrl get() = t("URL", "Посилання")
    val addLink get() = t("Add Link", "Додати посилання")

    // Cleaning
    val cleaning get() = t("Cleaning", "Прибирання")
    val taskTitle get() = t("Task Title", "Назва завдання")
    val everyXDays get() = t("Every X Days", "Кожні X днів")
    val recurring get() = t("Recurring", "Повторюване")
    val addTask get() = t("Add Task", "Додати завдання")
    val lastDone get() = t("Last done", "Востаннє")
    val oneTime get() = t("One-time", "Один раз")
    val everyDays get() = t("Every %d days", "Кожні %d днів")
    val moreOptions get() = t("More", "Більше")
    val update get() = t("Update", "Оновити")
    val disable get() = t("Disable", "Вимкнути")
    val restoreTask get() = t("Restore", "Відновити")
    val showDisabled get() = t("Show Disabled Tasks", "Показати вимкнені")
    val activeTasks get() = t("Active Tasks", "Активні завдання")
    val editTask get() = t("Edit Task", "Редагувати завдання")

    // Settings
    val displayName get() = t("Display Name", "Ім'я користувача")
    val userSettings get() = t("User Settings", "Налаштування користувача")
    val requestsTitle get() = t("Requests", "Запити")
    val requestsSubtitle get() = t("Send feedback or feature requests", "Надіслати відгук або пропозицію")
    val enterRequest get() = t("Enter your request here...", "Введіть ваш запит тут...")
    val sendRequest get() = t("Send Request", "Надіслати запит")
    val requestSent get() = t("Request sent!", "Запит надіслано!")
    val syncTitle get() = t("Sync", "Синхронізація")
    val syncSubtitle get() = t("Cloud sync for your data", "Синхронізація ваших даних з хмарою")
    val appUpdates get() = t("App Updates", "Оновлення")
    val appUpdatesSubtitle get() = t("Check and install new versions", "Перевірка та встановлення нових версій")
    val visibilityTitle get() = t("App Visibility", "Видимість додатків")
    val visibilitySubtitle get() = t("Hide or show apps on main menu", "Приховати або показати додатки в меню")
    val layoutType get() = t("Menu Layout", "Вигляд меню")
    val gridLayout get() = t("Grid", "Сітка")
    val listLayout get() = t("List", "Список")
    val back get() = t("< Back", "< Назад")
    val cloudFolder get() = t("Cloud Folder", "Папка у хмарі")
    val selectFolderDesc get() = t("Select the shared Google Drive folder for sync.", "Виберіть спільну папку Google Drive для синхронізації.")
    val linkSyncFolder get() = t("Link Sync Folder", "Прив'язати папку")
    val pushData get() = t("Push Data", "Відправити")
    val pullData get() = t("Pull Data", "Отримати")
    val openInDrive get() = t("Open in Drive", "Відкрити в Drive")
    val syncInterval get() = t("Sync Interval (seconds)", "Інтервал синхронізації (сек)")
    val unlinkFolder get() = t("Unlink Folder", "Відв'язати папку")
    val manualBackup get() = t("Manual Backup File", "Резервна копія (файл)")
    val export get() = t("Export", "Експорт")
    val import get() = t("Import", "Імпорт")
    val versionInfo get() = t("Version Info", "Інформація про версію")
    val checkForUpdates get() = t("Check for Updates", "Перевірити оновлення")
    val updateNow get() = t("Update Now", "Оновити зараз")
    val updateAvailableTitle get() = t("Update Available", "Доступне оновлення")
    val later get() = t("Later", "Пізніше")
    val install get() = t("Install", "Встановити")
    val goToUpdates get() = t("Go to Updates", "До оновлень")
    val upToDateMsg get() = t("I'm on version %d and I'm up to date.", "У мене версія %d, оновлення не потрібні.")
    val updateAvailableMsg get() = t("I'm on version %d and there is version %d available.", "У мене версія %d, а вже доступна версія %d.")
    val linkFolderToUpdate get() = t("You must link a shared folder in 'Sync' first to check for updates.", "Спочатку прив'яжіть папку в меню 'Синхронізація'.")
    val openSecuritySettings get() = t("Open Security Settings", "Відкрити налаштування безпеки")
    val autoBlockerNote get() = t("Note: If update fails, ensure 'Auto Blocker' is disabled in your phone's Security settings.", "Примітка: Якщо оновлення не вдалося, переконайтеся, що 'Автоматичне блокування' вимкнено в налаштуваннях безпеки.")

    // Workday Edits
    val editWorkHours get() = t("Edit Work Hours", "Редагувати робочі години")
    val setHoursManually get() = t("Set your work hours manually to start.", "Встановіть робочі години вручну для початку.")
    val save get() = t("Save", "Зберегти")

    // General
    val by get() = t("By", "Від")
    val login get() = t("Login", "Увійти")
    val logout get() = t("Logout", "Вийти")
    val signInRequired get() = t("Sign In Required", "Необхідно увійти")
    val signInRequiredMsg get() = t("Please sign in with Google to access this shared feature.", "Будь ласка, увійдіть через Google, щоб отримати доступ до цієї спільної функції.")
    val goToSettings get() = t("Go to Settings", "Перейти до налаштувань")
    val signInWithGoogle get() = t("Sign in with Google", "Увійти через Google")
    val cloudSyncFirebase get() = t("Cloud Sync (Firebase)", "Хмарна синхронізація")
    val appearanceLanguage get() = t("Appearance & Language", "Вигляд та Мова")
    val darkMode get() = t("Dark Mode", "Темний режим")
    val language get() = t("Language", "Мова")

    // Groups
    val linkedGroupId get() = t("Linked Group ID:", "ID прив'язаної групи:")
    val shareIdDesc get() = t("Share this ID with others to let them join.", "Поділіться цим ID з іншими, щоб вони могли приєднатися.")
    val unlinkGroup get() = t("Leave/Unlink Group", "Залишити/Відв'язати групу")
    val activeGroup get() = t("Active Group:", "Активна група:")
    val noneSelected get() = t("None selected", "Не вибрано")
    val addNewGroup get() = t("Add New Group", "Додати нову групу")
    val groupName get() = t("Group Name", "Назва групи")
    val createNewGroup get() = t("Create New Group", "Створити нову групу")
    val groupIdToJoin get() = t("Group ID to Join", "ID групи для вступу")
    val joinGroup get() = t("Join Group", "Приєднатися до групи")
    val or get() = t("OR", "АБО")
    val manualSync get() = t("Manual Sync (Merge)", "Синхронізувати (Об'єднати)")
    val syncing get() = t("Syncing...", "Синхронізація...")
    val groups get() = t("Groups", "Групи")
    val groupList get() = t("Group List", "Список груп")
    val groupInvite get() = t("Invite to Group", "Запросити до групи")
    val enterEmail get() = t("Enter user email...", "Введіть email користувача...")
    val inviteSent get() = t("Invite sent!", "Запрошення надіслано!")
    val confirmUnlinkTitle get() = t("Unlink Group?", "Відв'язати групу?")
    val confirmUnlinkMsg get() = t("Are you sure you want to leave this group?", "Ви впевнені, что хочете залишити цю групу?")
    val copyId get() = t("Copy ID", "Копіювати ID")
    val invitePopupTitle get() = t("Group Invitation", "Запрошення до групи")
    val invitePopupMsg get() = t("You have been invited to join group: %s", "Вас запросили приєднатися до групи: %s")
    val accept get() = t("Accept", "Прийняти")
    val decline get() = t("Decline", "Відхилити")
}
