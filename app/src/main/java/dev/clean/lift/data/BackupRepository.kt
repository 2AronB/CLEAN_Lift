package dev.clean.lift.data

import android.content.Context
import android.database.Cursor
import androidx.room.Room
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.clean.lift.widgets.WidgetProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

data class BackupSummary(val createdAt: Long, val counts: Map<String, Int>, val profileName: String)

/** Logical snapshots, not live SQLite file copies: WAL records and all source identities are included. */
class BackupRepository(private val context: Context, private val db: WorkoutDatabase,
                       private val checkpoint: (String) -> Unit = {}) {
    companion object {
        const val MAX_BYTES = 8 * 1024 * 1024
        const val PENDING = "__backup_pending_settings_v1"
        const val GENERATION = "__backup_generation_v1"
        private const val PREFIX = "__backup_"
        private const val FORMAT = "CLEAN-Workout-Logger"
        private val longColumns = setOf("id", "exerciseId", "workoutId", "workoutExerciseId", "templateId", "healthRecordId",
            "startedAt", "finishedAt", "epochDay", "weightMilliUnits", "valueMilliUnits")
        // Parent tables precede children. Deletion uses the reverse order with FK enforcement on.
        val tables = listOf("exercises", "workout_templates", "workouts", "workout_exercises", "workout_sets",
            "template_exercises", "body_measurements", "health_records", "widget_attempts", "app_metadata")
        private val lock = Mutex()

        fun readLimited(input: InputStream): ByteArray {
            val result = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(result.size() + count <= MAX_BYTES) { "Backup exceeds the 8 MiB limit" }
                result.write(buffer, 0, count)
            }
            return result.toByteArray()
        }
        internal fun digest(payload: JSONObject): String = MessageDigest.getInstance("SHA-256")
            .digest(payload.toString().toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private val preferences = BackupPreferences(context, checkpoint)
    private data class Column(val name: String, val type: String, val required: Boolean)
    private fun columns(sql: SupportSQLiteDatabase, table: String): List<Column> = sql.query("PRAGMA table_info(`$table`)").use { cursor ->
        buildList { while (cursor.moveToNext()) add(Column(cursor.getString(1), cursor.getString(2), cursor.getInt(3) != 0)) }.sortedBy { it.name }
    }
    private fun value(cursor: Cursor, index: Int): Any = when (cursor.getType(index)) {
        Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
        Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
        Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index).also { require(it.isFinite()) }
        Cursor.FIELD_TYPE_STRING -> cursor.getString(index)
        else -> error("Unsupported stored type")
    }

    suspend fun export(): ByteArray = withContext(Dispatchers.IO) { lock.withLock {
        require(db.dao().metadata(PENDING) == null) { "Finish the pending restore first" }
        val payload = db.withTransaction {
            val sql = db.openHelper.writableDatabase
            val data = JSONObject()
            val sequences = JSONObject()
            var estimatedBytes = 0L
            var rowCount = 0
            tables.forEach { table ->
                val names = columns(sql, table).map { it.name }
                val rows = JSONArray()
                val ordering = if (table == "app_metadata") "key" else "id"
                sql.query("SELECT ${names.joinToString { "`$it`" }} FROM `$table` ORDER BY `$ordering`").use { cursor ->
                    while (cursor.moveToNext()) {
                        if (table == "app_metadata" && cursor.getString(names.indexOf("key")).startsWith(PREFIX)) continue
                        val row = JSONArray().also { names.indices.forEach { index -> it.put(value(cursor, index)) } }
                        // Bound memory before constructing the full pretty-printed document.
                        estimatedBytes += row.toString().toByteArray(Charsets.UTF_8).size + names.size * 28L + 128
                        require(++rowCount <= 100_000 && estimatedBytes <= MAX_BYTES) { "Backup exceeds supported size" }
                        rows.put(row)
                    }
                }
                data.put(table, JSONObject().put("columns", JSONArray(names)).put("rows", rows))
                if (table != "app_metadata") {
                    val seq = sql.query("SELECT seq FROM sqlite_sequence WHERE name = ?", arrayOf(table)).use { if (it.moveToFirst()) it.getLong(0) else 0L }
                    sequences.put(table, seq)
                }
            }
            JSONObject().put("format", FORMAT).put("formatVersion", 1).put("databaseVersion", sql.version)
                .put("createdAt", System.currentTimeMillis()).put("tables", data).put("sequences", sequences)
                .put("preferences", preferences.capture())
        }
        val bytes = JSONObject().put("payload", payload).put("sha256", digest(payload)).toString(2).toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "Backup exceeds the 8 MiB limit" }
        preflight(payload) // Do not advertise a restorable file that the same version cannot load.
        bytes
    } }

    private fun decode(bytes: ByteArray): JSONObject {
        require(bytes.size in 1..MAX_BYTES) { "Empty or oversized backup" }
        val reader = JSONTokener(bytes.toString(Charsets.UTF_8))
        val root = reader.nextValue() as? JSONObject ?: error("Not a backup")
        require(reader.nextClean() == '\u0000') { "Unexpected data after backup" }
        require(root.keys().asSequence().toSet() == setOf("payload", "sha256"))
        val payload = root.getJSONObject("payload")
        require(root.get("sha256") == digest(payload)) { "Backup checksum does not match" }
        require(payload.keys().asSequence().toSet() == setOf("format", "formatVersion", "databaseVersion", "createdAt", "tables", "sequences", "preferences"))
        require(payload.get("format") == FORMAT && payload.get("formatVersion") == 1 && payload.get("databaseVersion") == 8) { "Unsupported backup format or database version" }
        require(integer(payload.get("createdAt")) >= 0)
        require(payload.getJSONObject("tables").keys().asSequence().toSet() == tables.toSet()) { "Incomplete backup tables" }
        require(payload.getJSONObject("sequences").keys().asSequence().toSet() == tables.filter { it != "app_metadata" }.toSet())
        preferences.validate(payload.getJSONObject("preferences"))
        return payload
    }

    private fun integer(value: Any): Long {
        require(value is Int || value is Long) { "Expected an exact integer" }
        return (value as Number).toLong()
    }

    private suspend fun preflight(payload: JSONObject) {
        // Never point validation at production data. SQL constraints validate links/uniqueness too.
        val scratch = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try { scratch.withTransaction { replaceTables(scratch.openHelper.writableDatabase, payload) } }
        finally { scratch.close() }
    }

    private fun replaceTables(sql: SupportSQLiteDatabase, payload: JSONObject) {
        tables.asReversed().forEach { sql.execSQL("DELETE FROM `$it`") }
        var totalRows = 0
        tables.forEach { table ->
            val section = payload.getJSONObject("tables").getJSONObject(table)
            require(section.keys().asSequence().toSet() == setOf("columns", "rows"))
            val expected = columns(sql, table)
            val names = section.getJSONArray("columns")
            require(names.length() == expected.size && expected.indices.all { names.get(it) == expected[it].name }) { "Unexpected columns in $table" }
            val rows = section.getJSONArray("rows")
            totalRows += rows.length()
            require(totalRows <= 100_000) { "Too many backup rows" }
            val statement = sql.compileStatement("INSERT INTO `$table` (${expected.joinToString { "`${it.name}`" }}) VALUES (${expected.joinToString { "?" }})")
            try {
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONArray(i)
                    require(row.length() == expected.size)
                    statement.clearBindings()
                    expected.forEachIndexed { index, column ->
                        val raw = row.get(index)
                        if (raw == JSONObject.NULL) { require(!column.required); statement.bindNull(index + 1) }
                        else when (column.type) {
                            "INTEGER" -> { val number = integer(raw)
                                if (column.name == "id") require(number > 0)
                                if (column.name !in longColumns) require(number in Int.MIN_VALUE..Int.MAX_VALUE)
                                statement.bindLong(index + 1, number)
                            }
                            "REAL" -> { require(raw is Number && raw.toDouble().isFinite()); statement.bindDouble(index + 1, raw.toDouble()) }
                            "TEXT" -> { require(raw is String)
                                if (table == "app_metadata" && column.name == "key") require(!raw.startsWith(PREFIX))
                                statement.bindString(index + 1, raw)
                            }
                            else -> error("Unsupported column type")
                        }
                    }
                    statement.executeInsert()
                }
            } finally { statement.close() }
            if (table != "app_metadata") {
                val seq = integer(payload.getJSONObject("sequences").get(table))
                val maximum = sql.query("SELECT COALESCE(MAX(id), 0) FROM `$table`").use { it.moveToFirst(); it.getLong(0) }
                require(seq >= maximum)
                sql.execSQL("DELETE FROM sqlite_sequence WHERE name = ?", arrayOf(table))
                if (seq > 0) sql.execSQL("INSERT INTO sqlite_sequence(name,seq) VALUES (?,?)", arrayOf<Any>(table, seq))
            }
        }
        validateData(sql)
    }

    private fun validateData(sql: SupportSQLiteDatabase) {
        fun reject(query: String) { sql.query(query).use { require(!it.moveToFirst()) { "Invalid or inconsistent backup records" } } }
        reject("PRAGMA foreign_key_check")
        reject("SELECT id FROM workouts WHERE startedAt < 0 OR finishedAt < startedAt OR overrideCount < 0")
        sql.query("SELECT COUNT(*) FROM workouts WHERE finishedAt IS NULL").use { it.moveToFirst(); require(it.getInt(0) <= 1) }
        reject("SELECT id FROM workout_sets WHERE position < 0 OR weightMilliUnits < 0 OR weightMilliUnits > ${SetInput.MAX_WEIGHT_MILLI} OR reps < 1 OR reps > ${SetInput.MAX_REPS} OR weightUnit NOT IN ('KG','LB') OR rpeTenths < 0 OR rpeTenths > 100")
        for (table in listOf("workout_exercises", "template_exercises")) {
            reject("SELECT id FROM `$table` WHERE position < 0 OR plannedSets < 1 OR repMin < 1 OR repMax < repMin OR restSeconds < 0 OR circuitRound < 1")
        }
        reject("SELECT id FROM workout_exercises WHERE completed NOT IN (0,1)")
        reject("SELECT id FROM body_measurements WHERE epochDay < 0 OR epochDay > 2932896 OR valueMilliUnits < 1 OR valueMilliUnits > 1000000 OR (metric = 'BODY_WEIGHT' AND unit NOT IN ('KG','LB')) OR (metric != 'BODY_WEIGHT' AND unit NOT IN ('CM','IN'))")
        fun known(table: String, column: String, allowed: Set<String>) {
            sql.query("SELECT DISTINCT `$column` FROM `$table`").use { while (it.moveToNext()) require(it.getString(0) in allowed) { "Unknown $column" } }
        }
        known("body_measurements", "metric", BodyMetric.entries.map { it.name }.toSet())
        known("health_records", "kind", HealthKind.entries.map { it.name }.toSet())
        known("health_records", "gender", setOf("", "M", "F"))
        reject("SELECT id FROM health_records WHERE epochDay < 0 OR epochDay > 2932896 OR formulaVersion < 1 OR result <= 0 OR age < 1 OR age > 120")
        known("widget_attempts", "protocol", WidgetProtocol.entries.map { it.name }.toSet())
        known("widget_attempts", "mode", setOf("Entered", "Guided"))
        reject("SELECT id FROM widget_attempts WHERE complete NOT IN (0,1) OR (complete = 1 AND result IS NULL) OR result < 0 OR epochDay < 0 OR epochDay > 2932896 OR length(token) < 1 OR length(token) > 80")
        reject("SELECT a.id FROM widget_attempts a JOIN health_records h ON a.healthRecordId = h.id WHERE a.epochDay != h.epochDay OR h.kind != CASE a.protocol WHEN 'COOPER' THEN 'COOPER' WHEN 'ROCKPORT' THEN 'ROCKPORT' WHEN 'AIR_FORCE' THEN 'WAIST_HEIGHT' ELSE '' END")
    }

    suspend fun preview(bytes: ByteArray): BackupSummary = withContext(Dispatchers.IO) { lock.withLock {
        val payload = decode(bytes)
        preflight(payload)
        summary(payload)
    } }
    private fun summary(payload: JSONObject): BackupSummary {
        val profile = payload.getJSONObject("preferences").getJSONObject("local-profile")
        return BackupSummary(payload.getLong("createdAt"), tables.associateWith {
            payload.getJSONObject("tables").getJSONObject(it).getJSONArray("rows").length()
        }, profile.optJSONObject("name")?.getString("value").orEmpty())
    }
    suspend fun currentCounts(): Map<String, Int> = withContext(Dispatchers.IO) { db.withTransaction {
        tables.associateWith { table -> db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM `$table`").use { it.moveToFirst(); it.getInt(0) } }
    } }

    suspend fun restore(bytes: ByteArray) = withContext(Dispatchers.IO) { lock.withLock {
        require(db.dao().metadata(PENDING) == null) { "Finish the pending restore first" }
        val payload = decode(bytes)
        preflight(payload) // Revalidate retained bytes immediately before replacement, below the UI.
        db.withTransaction {
            replaceTables(db.openHelper.writableDatabase, payload)
            db.dao().insertMetadata(AppMetadata(PENDING, payload.getJSONObject("preferences").toString()))
            db.dao().insertMetadata(AppMetadata(GENERATION, UUID.randomUUID().toString()))
            checkpoint("beforeCommit")
        }
        checkpoint("afterCommit")
        finishSettings()
    } }

    private suspend fun finishSettings(): Boolean {
        val pending = db.dao().metadata(PENDING) ?: return false
        preferences.apply(JSONObject(pending))
        checkpoint("afterPreferences")
        db.withTransaction { db.openHelper.writableDatabase.execSQL("DELETE FROM app_metadata WHERE `key` = ?", arrayOf(PENDING)) }
        return true
    }
    suspend fun recoverPending(): Boolean = withContext(Dispatchers.IO) { lock.withLock { finishSettings() } }
    suspend fun hasPending(): Boolean = withContext(Dispatchers.IO) { db.dao().metadata(PENDING) != null }
    suspend fun generation(): String = withContext(Dispatchers.IO) { db.dao().metadata(GENERATION).orEmpty() }
}
