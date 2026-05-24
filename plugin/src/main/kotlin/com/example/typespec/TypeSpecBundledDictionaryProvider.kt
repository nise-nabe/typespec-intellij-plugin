package com.example.typespec

import com.intellij.spellchecker.BundledDictionaryProvider

class TypeSpecBundledDictionaryProvider : BundledDictionaryProvider {
    override fun getBundledDictionaries(): Array<String> = arrayOf("typespec.dic")
}
