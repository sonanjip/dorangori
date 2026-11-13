package io.github.taehunkim.dorangori

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import io.github.taehunkim.dorangori.data.db.ALL_MIGRATIONS
import io.github.taehunkim.dorangori.data.db.AppDatabase
import org.junit.Rule
import org.junit.Test

/** Every schema version must reach the current one through the registered migrations (no destructive fallback). Version 1 is the first. */
class MigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test fun createsVersion1() {
        helper.createDatabase("migration-test", 1).close()
    }

    @Test fun migratesToLatest() {
        helper.createDatabase("migration-test-latest", 1).close()
        helper.runMigrationsAndValidate("migration-test-latest", AppDatabase.VERSION, true, *ALL_MIGRATIONS).close()
    }
}
