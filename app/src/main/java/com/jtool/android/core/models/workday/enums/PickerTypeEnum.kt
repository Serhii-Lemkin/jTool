package com.jtool.android.core.models.workday.enums

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
enum class PickerTypeEnum {
    DATE,
    TIME,
    MODE
}
