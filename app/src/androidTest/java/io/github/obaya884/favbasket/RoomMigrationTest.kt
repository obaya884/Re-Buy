package io.github.obaya884.favbasket

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.obaya884.favbasket.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {
    private val TEST_DB = "migration-test"

    private val ALL_MIGRATIONS = arrayOf(
        AppDatabase.MIGRATION_1_2
    )

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        var db = helper.createDatabase(TEST_DB, 1).apply {
            // Database has schema version 1. Insert some data using SQL queries.
            // You can't use DAO classes because they expect the latest schema.

            execSQL(
                """
                    INSERT INTO items (id, name, status, categoryId, createdAt, updatedAt)
                    VALUES (1, 'item1', 'status1', 1, '2022-01-01 11:00:00', '2022-01-01 11:00:00')
            """.trimIndent()
            )

            execSQL(
                """
                    INSERT INTO categories (id, name, createdAt, updatedAt)
                    VALUES (1, 'category1', '2022-01-01 11:00:00', '2022-01-01 11:00:00')
            """.trimIndent()
            )

            // Prepare for the next version.
            close()
        }

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
        val itemTableCursor = db.query("SELECT * FROM items WHERE id = 1")
        if (itemTableCursor.moveToFirst()) {
            assertEquals(1, itemTableCursor.getInt(itemTableCursor.getColumnIndex("id")))
            assertEquals("item1", itemTableCursor.getString(itemTableCursor.getColumnIndex("name")))
            assertEquals(
                "status1",
                itemTableCursor.getString(itemTableCursor.getColumnIndex("status"))
            )
            assertEquals(1, itemTableCursor.getInt(itemTableCursor.getColumnIndex("categoryId")))
            assertNotNull(
                itemTableCursor.getString(itemTableCursor.getColumnIndex("createdAt"))
            )
            assertNotNull(
                itemTableCursor.getString(itemTableCursor.getColumnIndex("updatedAt"))
            )
        } else {
            fail("Data was not migrated properly")
        }
        itemTableCursor.close()

        val categoryTableCursor = db.query("SELECT * FROM categories WHERE id = 1")
        if (categoryTableCursor.moveToFirst()) {
            assertEquals(1, categoryTableCursor.getInt(categoryTableCursor.getColumnIndex("id")))
            assertEquals(
                "category1",
                categoryTableCursor.getString(categoryTableCursor.getColumnIndex("name"))
            )
            assertNotNull(
                categoryTableCursor.getString(categoryTableCursor.getColumnIndex("createdAt"))
            )
            assertNotNull(
                categoryTableCursor.getString(categoryTableCursor.getColumnIndex("updatedAt"))
            )
        } else {
            fail("Data was not migrated properly")
        }
        categoryTableCursor.close()

    }

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        helper.createDatabase(TEST_DB, 1).apply {
            close()
        }

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            TEST_DB
        ).addMigrations(*ALL_MIGRATIONS).build().apply {
            openHelper.writableDatabase.close()
        }

    }
}
