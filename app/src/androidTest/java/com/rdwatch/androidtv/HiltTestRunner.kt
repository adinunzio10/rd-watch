package com.rdwatch.androidtv

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom test runner for running Hilt Android tests.
 * This runner replaces the standard Application with HiltTestApplication
 * to enable proper dependency injection in instrumentation tests.
 */
class HiltTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}