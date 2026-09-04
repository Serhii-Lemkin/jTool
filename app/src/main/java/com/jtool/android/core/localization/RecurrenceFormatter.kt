package com.jtool.android.core.localization

object RecurrenceFormatter {

    fun formatEveryXDays(days: Int, languageCode: String): String {
        val count = if (days < 1) 1 else days

        return when (languageCode.lowercase()) {
            "uk" -> formatUkrainian(count)
            "he" -> formatHebrew(count)
            else -> formatEnglish(count)
        }
    }

    private fun formatEnglish(days: Int): String {
        return if (days == 1) Loc.Cleaning.recurrence_everyDay else Loc.Cleaning.recurrence_everyXDays.format(days)
    }

    private fun formatHebrew(days: Int): String {
        return when (days) {
            1 -> Loc.Cleaning.recurrence_everyDay
            2 -> Loc.Cleaning.recurrence_everyTwoDays
            else -> Loc.Cleaning.recurrence_everyXDays.format(days)
        }
    }

    private fun formatUkrainian(days: Int): String {
        if (days == 1) return Loc.Cleaning.recurrence_everyDay

        val lastDigit = days % 10
        val lastTwoDigits = days % 100

        return when {
            lastDigit == 1 && lastTwoDigits != 11 -> Loc.Cleaning.recurrence_uk_1.format(days)

            (lastDigit in 2..4) && (lastTwoDigits !in 12..14) -> Loc.Cleaning.recurrence_uk_2_4.format(days)

            else -> Loc.Cleaning.recurrence_uk_5.format(days)
        }
    }
}
