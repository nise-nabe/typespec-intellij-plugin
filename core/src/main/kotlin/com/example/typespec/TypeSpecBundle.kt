package com.example.typespec

import com.intellij.DynamicBundle

private const val BUNDLE = "messages.TypeSpecBundle"

object TypeSpecBundle : DynamicBundle(BUNDLE) {
    fun message(key: String, vararg params: Any): String = getMessage(key, *params)
}
