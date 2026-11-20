package dev.clean.lift.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.OnConflictStrategy
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import dev.clean.lift.widgets.WidgetAttempt
import dev.clean.lift.widgets.WidgetAttemptDao

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts WHERE finishedAt IS NOT NULL AND generatorSession IS NOT NULL ORDER BY finishedAt DESC, id DESC")
    fun observeGeneratedHistory(): Flow<List<Workout>>
    @Query("SELECT * FROM exercises WHERE catalogKey = :key LIMIT 1")
    suspend fun catalogExercise(key: String): Exercise?
    @Query("UPDATE workout_templates SET generatorSession = :session WHERE id = :id")
    suspend fun setGeneratorSession(id: Long, session: String?): Int
    @Transaction @Query("SELECT * FROM workout_templates ORDER BY normalizedName, id")
    fun observeTemplates(): Flow<List<TemplateWithExercises>>
    @Transaction @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun template(id: Long): TemplateWithExercises?
    @Insert suspend fun insertTemplate(template: WorkoutTemplate): Long
    @Query("UPDATE workout_templates SET name = :name, normalizedName = :normalized WHERE id = :id")
    suspend fun renameTemplate(id: Long, name: String, normalized: String): Int
    @Query("DELETE FROM template_exercises WHERE templateId = :id") suspend fun clearTemplateExercises(id: Long)
    @Query("DELETE FROM workout_templates WHERE id = :id") suspend fun deleteTemplate(id: Long): Int
    @Insert suspend fun insertTemplateExercise(entry: TemplateExercise): Long
    @Query("UPDATE workouts SET overrideCount = overrideCount + 1 WHERE id = :id AND finishedAt IS NULL")
    suspend fun overridePlan(id: Long): Int
    @Query("UPDATE workout_exercises SET completed = :completed WHERE id = :id")
    suspend fun markCompleted(id: Long, completed: Boolean): Int
    @Query("SELECT value FROM app_metadata WHERE `key` = :key") suspend fun metadata(key: String): String?
    @Insert suspend fun insertMetadata(metadata: AppMetadata)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun seedExercise(exercise: Exercise): Long

    @Transaction
    @Query("SELECT * FROM workouts WHERE finishedAt IS NULL ORDER BY startedAt DESC, id DESC LIMIT 1")
    fun observeActive(): Flow<WorkoutWithExercises?>
    @Transaction
    @Query("SELECT * FROM workouts WHERE finishedAt IS NOT NULL ORDER BY finishedAt DESC, id DESC LIMIT 1")
    fun observeLatestFinished(): Flow<WorkoutWithExercises?>
    @Query("""
        SELECT w.id, w.startedAt, w.finishedAt, w.templateName,
            (SELECT COUNT(*) FROM workout_exercises e WHERE e.workoutId = w.id) AS exerciseCount,
            (SELECT COUNT(*) FROM workout_sets s JOIN workout_exercises e ON s.workoutExerciseId = e.id WHERE e.workoutId = w.id) AS setCount
        FROM workouts w WHERE w.finishedAt IS NOT NULL AND (
            :query = '' OR instr(lower(COALESCE(w.templateName, '')), lower(:query)) > 0
            OR EXISTS (SELECT 1 FROM workout_exercises x WHERE x.workoutId = w.id AND instr(lower(x.exerciseName), lower(:query)) > 0)
        )
        ORDER BY w.finishedAt DESC, w.id DESC LIMIT :limit
    """)
    fun observeCompleted(limit: Int, query: String = ""): Flow<List<WorkoutSummary>>
    @Transaction @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeWorkout(id: Long): Flow<WorkoutWithExercises?>
    @Transaction @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun workoutDetails(id: Long): WorkoutWithExercises?
    @Query("SELECT * FROM workouts WHERE finishedAt IS NULL ORDER BY startedAt DESC, id DESC LIMIT 1")
    suspend fun activeWorkout(): Workout?
    @Query("SELECT * FROM exercises WHERE id = :id") suspend fun exercise(id: Long): Exercise?
    @Query("SELECT * FROM workout_exercises WHERE id = :id") suspend fun workoutExercise(id: Long): WorkoutExercise?
    @Query("SELECT * FROM workout_sets WHERE id = :id") suspend fun set(id: Long): WorkoutSet?
    @Query("UPDATE workout_sets SET weightMilliUnits = :weight, weightUnit = :unit, reps = :reps WHERE id = :id")
    suspend fun updateSet(id: Long, weight: Long, unit: WeightUnit, reps: Int): Int
    @Query("DELETE FROM workout_sets WHERE id = :id") suspend fun deleteSet(id: Long): Int
    @Query("DELETE FROM workout_exercises WHERE id = :id") suspend fun deleteWorkoutExercise(id: Long): Int
    @Query("UPDATE workouts SET finishedAt = :finishedAt WHERE id = :id AND finishedAt IS NULL")
    suspend fun finishWorkout(id: Long, finishedAt: Long): Int
    @Query("DELETE FROM workouts WHERE id = :id AND finishedAt IS NULL") suspend fun cancelWorkout(id: Long): Int
    @Query("SELECT * FROM exercises ORDER BY normalizedName, id")
    fun observeExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises ORDER BY normalizedName, id")
    suspend fun exercises(): List<Exercise>

    @Insert suspend fun insertExercise(exercise: Exercise): Long

    // Updating only the catalog name leaves historical exercise snapshots intact.
    @Query("UPDATE exercises SET name = :name, normalizedName = :normalizedName WHERE id = :id")
    suspend fun renameExercise(id: Long, name: String, normalizedName: String): Int

    @Insert suspend fun insertWorkout(workout: Workout): Long
    @Insert suspend fun insertWorkoutExercise(exercise: WorkoutExercise): Long
    @Insert suspend fun insertSet(set: WorkoutSet): Long

    @Query("SELECT * FROM workouts WHERE id = :id") suspend fun workout(id: Long): Workout?
    @Query("SELECT * FROM workout_exercises WHERE workoutId = :id ORDER BY position")
    suspend fun workoutExercises(id: Long): List<WorkoutExercise>
    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :id ORDER BY position")
    suspend fun sets(id: Long): List<WorkoutSet>
}

@Database(entities = [Exercise::class, Workout::class, WorkoutExercise::class, WorkoutSet::class, AppMetadata::class, WorkoutTemplate::class, TemplateExercise::class, BodyMeasurement::class, HealthRecord::class, WidgetAttempt::class], version = 8, exportSchema = true)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun dao(): WorkoutDao
    abstract fun progressDao(): ProgressDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun healthDao(): HealthDao
    abstract fun widgetAttemptDao(): WidgetAttemptDao

    companion object {
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS widget_attempts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, token TEXT NOT NULL, protocol TEXT NOT NULL, version TEXT NOT NULL, epochDay INTEGER NOT NULL, result REAL, complete INTEGER NOT NULL, mode TEXT NOT NULL, details TEXT NOT NULL, deviations TEXT NOT NULL, healthRecordId INTEGER, FOREIGN KEY(healthRecordId) REFERENCES health_records(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_widget_attempts_token ON widget_attempts(token)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_widget_attempts_protocol_epochDay ON widget_attempts(protocol, epochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_widget_attempts_healthRecordId ON widget_attempts(healthRecordId)")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS health_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, kind TEXT NOT NULL, epochDay INTEGER NOT NULL, result REAL NOT NULL, formulaVersion INTEGER NOT NULL, heightCm REAL, weightKg REAL, waistCm REAL, abdomenCm REAL, neckCm REAL, hipsCm REAL, age INTEGER, gender TEXT NOT NULL, timeSeconds INTEGER, distanceMetres REAL, heartRate INTEGER, notes TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_health_records_kind_epochDay ON health_records(kind, epochDay)")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN catalogKey TEXT DEFAULT NULL")
                db.execSQL("CREATE UNIQUE INDEX index_exercises_catalogKey ON exercises(catalogKey)")
                db.execSQL("ALTER TABLE workouts ADD COLUMN generatorSession TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE workout_templates ADD COLUMN generatorSession TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN catalogKey TEXT DEFAULT NULL")
                for (table in listOf("template_exercises", "workout_exercises")) {
                    for (column in listOf("repMin", "repMax", "restSeconds")) {
                        db.execSQL("ALTER TABLE $table ADD COLUMN $column INTEGER DEFAULT NULL")
                    }
                }
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN circuitRound INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE template_exercises ADD COLUMN circuitRound INTEGER DEFAULT NULL")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS body_measurements (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, metric TEXT NOT NULL, epochDay INTEGER NOT NULL, valueMilliUnits INTEGER NOT NULL, unit TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_body_measurements_metric_epochDay ON body_measurements(metric, epochDay)")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workouts ADD COLUMN templateName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE workouts ADD COLUMN overrideCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN plannedSets INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN completed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS workout_templates (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, normalizedName TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_workout_templates_normalizedName ON workout_templates(normalizedName)")
                db.execSQL("CREATE TABLE IF NOT EXISTS template_exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, templateId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, position INTEGER NOT NULL, plannedSets INTEGER, FOREIGN KEY(templateId) REFERENCES workout_templates(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_template_exercises_exerciseId ON template_exercises(exerciseId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_template_exercises_templateId_position ON template_exercises(templateId, position)")
            }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `app_metadata` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))")
            }
        }
        @Volatile private var instance: WorkoutDatabase? = null

        fun get(context: Context): WorkoutDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, WorkoutDatabase::class.java, "workout-logger.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                // Never use destructive migration: future schema changes must preserve data.
                .build().also { instance = it }
        }
    }
}
