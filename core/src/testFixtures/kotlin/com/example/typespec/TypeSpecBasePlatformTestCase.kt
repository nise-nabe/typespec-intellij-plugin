package com.example.typespec

import com.intellij.testFramework.fixtures.BasePlatformTestCase

abstract class TypeSpecBasePlatformTestCase : BasePlatformTestCase() {
    override fun setUp() {
        suppressUltimatePostStartupConstructorErrors()
        super.setUp()
    }

    companion object {
        init {
            suppressUltimatePostStartupConstructorErrors()
        }
    }
}
