package com.jtool.android.core.measurement

import android.content.Context
import com.jtool.android.core.localization.Loc
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MeasurementUnit(
    val id: String = "",
    val aliases: List<String> = emptyList()
)

class MeasurementManager(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    val units: List<MeasurementUnit>

    init {
        units = try {
            val content = context.assets.open("measurements.json").bufferedReader().use { it.readText() }
            json.decodeFromString<List<MeasurementUnit>>(content)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun resolveUnit(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        val lower = trimmed.lowercase()

        val exactMatch = units.find { unit ->
            unit.id.lowercase() == lower || unit.aliases.any { it.lowercase() == lower }
        }

        return exactMatch?.id ?: trimmed
    }

    fun getSuggestions(query: String): List<Pair<String, String>> {
        val trimmed = query.trim().lowercase()
        return if (trimmed.isEmpty()) {
            units.map { it.id to getLocalizedUnitName(it.id) }
        } else {
            units.filter { unit ->
                unit.id.lowercase().contains(trimmed) ||
                unit.aliases.any { it.lowercase().contains(trimmed) } ||
                getLocalizedUnitName(unit.id).lowercase().contains(trimmed)
            }.map { it.id to getLocalizedUnitName(it.id) }
        }
    }

    fun getLocalizedUnitName(unitKey: String): String {
        if (unitKey.isEmpty()) return ""
        val key = "shoplist_unit_$unitKey"
        return Loc.s(key, unitKey)
    }
}
