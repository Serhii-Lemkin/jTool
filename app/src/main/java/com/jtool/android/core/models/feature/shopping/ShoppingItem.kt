package com.jtool.android.core.models.feature.shopping

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum

@Keep
@IgnoreExtraProperties
data class ShoppingItem(
    var id: String = "",
    val text: String = "",
    @get:PropertyName("isChecked")
    val isChecked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val lastUpdatedBy: String = "",
    val aisle: ShoppingAisleEnum = ShoppingAisleEnum.COMMON
)
