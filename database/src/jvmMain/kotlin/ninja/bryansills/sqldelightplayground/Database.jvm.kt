package ninja.bryansills.sqldelightplayground

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return JdbcSqliteDriver(
            url = JdbcSqliteDriver.IN_MEMORY,
            schema = Database.Schema.synchronous(),
            migrateEmptySchema = true,
            properties = Properties().apply { put("foreign_keys", "true") },
        )
    }
}
