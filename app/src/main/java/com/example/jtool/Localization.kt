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
    val jNotes get() = t("Notes", "Нотатки")
    val newNote get() = t("New Note", "Нова нотатка")

    // Links
    val jLinks get() = t("Links", "Посилання")
    val searchLinks get() = t("Search links...", "Пошук посилань...")
    val linkTitle get() = t("Title", "Назва")
    val linkUrl get() = t("URL", "Посилання")
    val addLink get() = t("Add Link", "Додати посилання")

    // Cleaning
    val jCleaning get() = t("Cleaning", "Прибирання")
    val taskTitle get() = t("Task Title", "Назва завдання")
    val everyXDays get() = t("Every X Days", "Кожні X днів")
    val recurring get() = t("Recurring", "Повторюване")
    val addTask get() = t("Add Task", "Додати завдання")
    val lastDone get() = t("Last done", "Востаннє")
    val oneTime get() = t("One-time", "Один раз")
    val everyDays get() = t("Every %d days", "Кожні %d днів")

    // Settings
    val displayName get() = t("Display Name", "Ім'я користувача")
    val userSettings get() = t("User Settings", "Налаштування користувача")
    val syncTitle get() = t("Sync", "Синхронізація")
    val syncSubtitle get() = t("Cloud sync for your data", "Хмарна синхронізація ваших даних")
    val appUpdates get() = t("App Updates", "Оновлення")
    val appUpdatesSubtitle get() = t("Check and install new versions", "Перевірка та встановлення нових версій")
    val visibilityTitle get() = t("App Visibility", "Видимість додатків")
    val visibilitySubtitle get() = t("Hide or show apps on main menu", "Приховати або показати додатки в меню")
    val back get() = t("< Back", "< Назад")
    val cloudFolder get() = t("Cloud Folder", "Хмарна папка")
    val selectFolderDesc get() = t("Select the shared Google Drive folder for sync.", "Виберіть спільну папку Google Drive для синхронізації.")
    val linkSyncFolder get() = t("Link Sync Folder", "Прив'язати папку")
    val pushData get() = t("Push Data", "Відправити")
    val pullData get() = t("Pull Data", "Отримати")
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
    val upToDateMsg get() = t("I'm on version %d and I'm up to date.", "У мене версія %d, оновлення не потрібні.")
    val updateAvailableMsg get() = t("I'm on version %d and there is version %d available.", "У мене версія %d, а вже доступна версія %d.")
    val linkFolderToUpdate get() = t("You must link a shared folder in 'Sync' first to check for updates.", "Спочатку прив'яжіть папку в меню 'Синхронізація'.")

    // General
    val by get() = t("By", "Від")
}
