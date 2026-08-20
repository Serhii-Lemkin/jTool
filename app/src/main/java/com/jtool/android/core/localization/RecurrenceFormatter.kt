package com.jtool.android.core.localization

object RecurrenceFormatter {

    /**
     * Formats a day count into a natural phrase: "Every X days".
     *
     * @param days The number of days (interval).
     * @param languageCode "en" or "uk".
     * @return Formatted string.
     */
    fun formatEveryXDays(days: Int, languageCode: String): String {
        val count = if (days < 1) 1 else days

        return when (languageCode.lowercase()) {
            "uk" -> formatUkrainian(count)
            "he" -> formatHebrew(count)
            else -> formatEnglish(count)
        }
    }

    private fun formatEnglish(days: Int): String {
        return if (days == 1) Loc.cleaning_recurrence_everyDay else Loc.cleaning_recurrence_everyXDays.format(days)
    }

    private fun formatHebrew(days: Int): String {
        return when (days) {
            1 -> Loc.cleaning_recurrence_everyDay
            2 -> Loc.cleaning_recurrence_everyTwoDays
            else -> Loc.cleaning_recurrence_everyXDays.format(days)
        }
    }

    private fun formatUkrainian(days: Int): String {
        // Special natural case for 1
        if (days == 1) return Loc.cleaning_recurrence_everyDay

        val lastDigit = days % 10
        val lastTwoDigits = days % 100

        return when {
            // Numbers ending in 1 but not 11 (e.g., 1, 21, 31...)
            lastDigit == 1 && lastTwoDigits != 11 -> Loc.cleaning_recurrence_uk_1.format(days)

            // Numbers ending in 2, 3, 4 but not 12, 13, 14 (e.g., 2, 3, 4, 22, 23, 24...)
            (lastDigit in 2..4) && (lastTwoDigits !in 12..14) -> Loc.cleaning_recurrence_uk_2_4.format(days)

            // All other numbers (ending in 5-9, 0, or the teens 11-14)
            else -> Loc.cleaning_recurrence_uk_5.format(days)
        }
    }
}
