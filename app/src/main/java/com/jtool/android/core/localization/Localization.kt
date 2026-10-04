package com.jtool.android.core.localization

import android.util.Log
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.LayoutDirection
import com.jtool.android.core.localization.domains.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Serializable
data class LanguageMetadata(
    val name: String,
    val shortCode: String,
    val locale: String,
    val direction: String = "ltr",
)

@Serializable
data class LocaleFileData(
    val name: String,
    val shortCode: String,
    val locale: String,
    val direction: String = "ltr",
    val strings: Map<String, String>,
)

object Loc {
    private val json = Json { ignoreUnknownKeys = true }
    private var loadedStrings by mutableStateOf<Map<String, String>>(emptyMap())
    private var metadata by mutableStateOf<LanguageMetadata?>(null)
    
    var lang by mutableStateOf("en")

    val layoutDirection: LayoutDirection 
        get() = if (metadata?.direction == "rtl") LayoutDirection.Rtl else LayoutDirection.Ltr

    fun load(context: Context, locale: String) {
        try {
            val jsonString = context.assets.open("locales/$locale.json").bufferedReader().use { it.readText() }
            val data = json.decodeFromString<LocaleFileData>(jsonString)
            loadedStrings = data.strings
            metadata = LanguageMetadata(data.name, data.shortCode, data.locale, data.direction)
            lang = locale
        } catch (e: Exception) {
            Log.e("Loc", "Failed to load locale: $locale", e)
        }
    }

    fun getAvailableLanguages(context: Context): List<LanguageMetadata> {
        val files = context.assets.list("locales") ?: return emptyList()
        return files.mapNotNull { fileName ->
            try {
                val jsonString = context.assets.open("locales/$fileName").bufferedReader().use { it.readText() }
                val data = json.decodeFromString<LocaleFileData>(jsonString)
                LanguageMetadata(data.name, data.shortCode, data.locale, data.direction)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun s(key: String, fallback: String): String {
        return loadedStrings[key] ?: fallback
    }

    val Launcher = LauncherLoc
    val Workday = WorkdayLoc
    val Calendar = CalendarLoc
    val Admin = AdminLoc
    val TimeCalc = TimeCalcLoc
    val ShopList = ShopListLoc
    val Tracker = TrackerLoc
    val Notes = NotesLoc
    val Comments = CommentsLoc
    val Links = LinksLoc
    val Cleaning = CleaningLoc
    val Settings = SettingsLoc
    val Support = SupportLoc
    val General = GeneralLoc
    val Crash = CrashLoc

    fun getDayOfWeekName(day: DayOfWeek, textStyle: TextStyle = TextStyle.FULL): String {
        return day.getDisplayName(textStyle, Locale.forLanguageTag(lang))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    fun getMonthName(month: java.time.Month, textStyle: TextStyle = TextStyle.FULL): String {
        val style = if (textStyle == TextStyle.FULL) TextStyle.FULL_STANDALONE else textStyle
        return month.getDisplayName(style, Locale.forLanguageTag(lang))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    fun getAisleName(aisle: com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum): String {
        return when (aisle) {
            com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum.COMMON -> ShopList.aisle_common
            com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum.VEGGIES -> ShopList.aisle_veggies
            com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum.MEAT -> ShopList.aisle_meat
            com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum.FISH -> ShopList.aisle_fish
            com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum.DAIRY -> ShopList.aisle_dairy
            com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum.BAKERY -> ShopList.aisle_bakery
            com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum.DRINKS -> ShopList.aisle_drinks
            com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum.FROZEN -> ShopList.aisle_frozen
            com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum.SNACKS -> ShopList.aisle_snacks
            com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum.HOUSEHOLD -> ShopList.aisle_household
            com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum.PERSONAL -> ShopList.aisle_personal
            com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum.PETS -> ShopList.aisle_pets
            com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum.PANTRY -> ShopList.aisle_pantry
        }
    }

    fun getCleaningFrequency(days: Int): String {
        return RecurrenceFormatter.formatEveryXDays(days, lang)
    }
}
