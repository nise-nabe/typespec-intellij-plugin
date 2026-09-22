package com.example.typespec

import com.intellij.spellchecker.SpellCheckerManager

class TypeSpecBundledDictionaryProviderTest : TypeSpecBasePlatformTestCase() {
    fun testBundledDictionariesAreResolvableFromProviderClass() {
        for (name in TypeSpecBundledDictionaryProvider().bundledDictionaries) {
            assertNotNull(
                "Dictionary resource must resolve via provider class: $name",
                TypeSpecBundledDictionaryProvider::class.java.getResource(name),
            )
        }
    }

    fun testTypespecIsNotReportedAsTypo() {
        assertFalse(SpellCheckerManager.getInstance(project).hasProblem("typespec"))
    }
}
