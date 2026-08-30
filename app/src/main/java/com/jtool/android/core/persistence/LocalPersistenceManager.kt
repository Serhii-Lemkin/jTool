package com.jtool.android.core.persistence

import android.content.Context
import com.jtool.android.core.models.common.enums.PersistencePrefixEnum

abstract class LocalPersistenceManager<T>(
    protected val context: Context,
    private val prefix: PersistencePrefixEnum
) {
    protected fun getPrefs(groupId: String?) = context.getSharedPreferences("${prefix.prefix}_${groupId ?: "none"}", Context.MODE_PRIVATE)

    abstract fun save(items: List<T>, groupId: String?)
    abstract fun load(groupId: String?): List<T>

    fun clearCache(groupId: String?) {
        getPrefs(groupId).edit().clear().apply()
    }
}
