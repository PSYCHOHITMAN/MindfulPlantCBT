package com.mindfulplant.cbt.util

import android.content.Context
import androidx.annotation.StringRes

/**
 * Text that is turned into a string only when it is shown on screen.
 *
 * ViewModels and repositories have no access to the app's translated string
 * resources, so they pass a [UiText] instead of a hard-coded English sentence.
 * The Fragment resolves it with [asString], using the current app language.
 *
 * [Raw] is for text that is already final, such as an error message written by
 * the API.
 */
sealed class UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText()
    data class Raw(val value: String) : UiText()

    fun asString(context: Context): String = when (this) {
        is Res -> context.getString(id, *args.toTypedArray())
        is Raw -> value
    }
}

fun uiText(@StringRes id: Int, vararg args: Any): UiText = UiText.Res(id, args.toList())
