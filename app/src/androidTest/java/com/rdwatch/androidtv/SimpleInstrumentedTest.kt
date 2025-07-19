package com.rdwatch.androidtv

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Simple instrumented test to verify Android test framework is working
 */
@RunWith(AndroidJUnit4::class)
class SimpleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.rdwatch.androidtv", appContext.packageName)
    }

    @Test
    fun testAndroidFramework() {
        // Simple test to verify the testing framework works
        assertTrue("Android test framework should work", true)
    }
}
