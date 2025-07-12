package com.rdwatch.androidtv.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rdwatch.androidtv.data.migrations.Migrations
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    
    private val TEST_DB = "migration-test"
    
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        listOf(),
        FrameworkSQLiteOpenHelperFactory()
    )
    
    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        var db = helper.createDatabase(TEST_DB, 1).apply {
            // Database has schema version 1. Insert some data using SQL queries.
            // You cannot use DAO classes because they expect the latest schema.
            execSQL("INSERT INTO movies (id, title, description) VALUES (1, 'Test Movie', 'Test Description')")
            
            // Prepare for the next version.
            close()
        }
        
        // Re-open the database with version 2 and provide MIGRATION_1_2 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, Migrations.MIGRATION_1_2)
        
        // MigrationTestHelper automatically verifies the schema changes,
        // but you can also validate that the data was preserved.
        val cursor = db.query("SELECT * FROM movies WHERE id = 1")
        assert(cursor.moveToFirst())
        assert(cursor.getString(cursor.getColumnIndexOrThrow("title")) == "Test Movie")
        cursor.close()
        
        // Verify new tables exist
        val usersCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='users'")
        assert(usersCursor.moveToFirst())
        usersCursor.close()
        
        val sitesCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='sites'")
        assert(sitesCursor.moveToFirst())
        sitesCursor.close()
        
        db.close()
    }
    
    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Create earliest version of the database.
        helper.createDatabase(TEST_DB, 1).apply {
            close()
        }
        
        // Open latest version of the database. Room will validate the schema
        // once all migrations execute.
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            TEST_DB
        ).addMigrations(*Migrations.ALL_MIGRATIONS).build().apply {
            openHelper.writableDatabase
            close()
        }
    }
}