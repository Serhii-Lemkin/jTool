package com.jtool.android.core.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.SelectableDates
import com.jtool.android.core.settings.SettingsManager
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.util.Locale

fun getLocaleForDateFormat(dateFormat: String): Locale {
    return when (dateFormat) {
        "MM/DD/YYYY" -> Locale.US
        "YYYY/MM/DD" -> Locale.CANADA
        else -> Locale.UK
    }
}

fun getPreferredDateFormatPattern(context: Context): String {
    val settingsManager = SettingsManager(context)
    return when (settingsManager.getDateFormat()) {
        "MM/DD/YYYY" -> "MM/dd/yyyy"
        "YYYY/MM/DD" -> "yyyy/MM/dd"
        else -> "dd/MM/yyyy"
    }
}

fun getPreferredSimpleDateFormat(context: Context): SimpleDateFormat {
    val pattern = getPreferredDateFormatPattern(context)
    return SimpleDateFormat(pattern, Locale.getDefault())
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
    val dateFormat = settingsManager.getDateFormat()
    val locale = remember(dateFormat) { getLocaleForDateFormat(dateFormat) }
    
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
