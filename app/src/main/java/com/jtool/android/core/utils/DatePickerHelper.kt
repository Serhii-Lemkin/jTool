package com.jtool.android.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.SelectableDates
import com.jtool.android.core.settings.SettingsManager
import java.time.DayOfWeek
import java.util.Locale

fun getLocaleForWeekStart(startDay: DayOfWeek): Locale {
    return when (startDay) {
        DayOfWeek.SUNDAY -> Locale.US
        DayOfWeek.MONDAY -> Locale.UK
        DayOfWeek.SATURDAY -> Locale("ar", "SA")
        DayOfWeek.FRIDAY -> Locale("ar", "EG")
        else -> Locale.UK
    }
}

@Composable
fun rememberCustomDatePickerState(
    initialSelectedDateMillis: Long? = null,
    initialDisplayedMonthMillis: Long? = initialSelectedDateMillis,
    yearRange: IntRange = DatePickerDefaults.YearRange,
    initialDisplayMode: DisplayMode = DisplayMode.Picker,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
): DatePickerState {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val worktimeOptions = remember { settingsManager.getWorktimeOptions() }
    val startDay = try { DayOfWeek.valueOf(worktimeOptions.weeklyStartDay) } catch (_: Exception) { DayOfWeek.MONDAY }
    val locale = remember(startDay) { getLocaleForWeekStart(startDay) }
    
    return remember(locale, initialSelectedDateMillis) {
        DatePickerState(
            locale = locale,
            initialSelectedDateMillis = initialSelectedDateMillis,
            initialDisplayedMonthMillis = initialDisplayedMonthMillis,
            yearRange = yearRange,
            initialDisplayMode = initialDisplayMode,
            selectableDates = selectableDates
        )
    }
}
