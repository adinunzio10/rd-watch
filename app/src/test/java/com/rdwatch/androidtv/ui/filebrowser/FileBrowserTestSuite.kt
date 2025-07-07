package com.rdwatch.androidtv.ui.filebrowser

import com.rdwatch.androidtv.ui.filebrowser.algorithms.SortFilterAlgorithmsTest
import com.rdwatch.androidtv.ui.filebrowser.models.FileTypeTest
import com.rdwatch.androidtv.ui.filebrowser.repository.PaginationTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite for the Direct Account File Browser feature.
 * Runs all unit tests for the file browser functionality.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    AccountFileBrowserViewModelTest::class,
    PaginationTest::class,
    FileTypeTest::class,
    SortFilterAlgorithmsTest::class
)
class FileBrowserUnitTestSuite

/**
 * Integration and UI test suite for the Direct Account File Browser feature.
 * Note: These require AndroidTest environment to run.
 */
// @RunWith(Suite::class)
// @Suite.SuiteClasses(
//     FocusNavigationTest::class,
//     BulkSelectionTest::class,
//     ErrorStatesTest::class,
//     FileBrowserIntegrationTest::class
// )
// class FileBrowserAndroidTestSuite