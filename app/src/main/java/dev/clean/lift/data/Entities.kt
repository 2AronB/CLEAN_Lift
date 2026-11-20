package dev.clean.lift.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.ColumnInfo

@Entity(tableName = "exercises", indices = [Index(value = ["normalizedName"], unique = true), Index(value = ["catalogKey"], unique = true)])
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val normalizedName: String,
    @ColumnInfo(defaultValue = "NULL") val catalogKey: String? = null
)

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val notes: String = "",
    @ColumnInfo(defaultValue = "NULL") val templateName: String? = null,
    @ColumnInfo(defaultValue = "0") val overrideCount: Int = 0,
    @ColumnInfo(defaultValue = "NULL") val generatorSession: String? = null
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(entity = Workout::class, parentColumns = ["id"], childColumns = ["workoutId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("exerciseId"), Index(value = ["workoutId", "position"], unique = true)]
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    // Preserve the name used when logging, even if the catalog entry is renamed later.
    val exerciseName: String,
    val position: Int,
    @ColumnInfo(defaultValue = "NULL") val plannedSets: Int? = null,
    @ColumnInfo(defaultValue = "0") val completed: Boolean = false,
    @ColumnInfo(defaultValue = "NULL") val catalogKey: String? = null,
    @ColumnInfo(defaultValue = "NULL") val repMin: Int? = null,
    @ColumnInfo(defaultValue = "NULL") val repMax: Int? = null,
    @ColumnInfo(defaultValue = "NULL") val restSeconds: Int? = null,
    @ColumnInfo(defaultValue = "NULL") val circuitRound: Int? = null
)

enum class WeightUnit { KG, LB }

@Entity(
    tableName = "workout_sets",
    foreignKeys = [ForeignKey(entity = WorkoutExercise::class, parentColumns = ["id"], childColumns = ["workoutExerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["workoutExerciseId", "position"], unique = true)]
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val position: Int,
    // Integer thousandths avoid floating-point rounding of entered weights.
    val weightMilliUnits: Long,
    val weightUnit: WeightUnit,
    val reps: Int,
    val rpeTenths: Int? = null,
    val notes: String = ""
)

@Entity(tableName = "app_metadata")
data class AppMetadata(@PrimaryKey val key: String, val value: String)

data class ExerciseWithSets(
    @Embedded val exercise: WorkoutExercise,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId") val sets: List<WorkoutSet>
)

data class WorkoutWithExercises(
    @Embedded val workout: Workout,
    @Relation(entity = WorkoutExercise::class, parentColumn = "id", entityColumn = "workoutId")
    val exercises: List<ExerciseWithSets>
) {
    fun ordered(): WorkoutWithExercises = copy(exercises = exercises.sortedBy { it.exercise.position }
        .map { it.copy(sets = it.sets.sortedBy { set -> set.position }) })
    val setCount: Int get() = exercises.sumOf { it.sets.size }
}

data class WorkoutSummary(
    val id: Long,
    val startedAt: Long,
    val finishedAt: Long,
    val exerciseCount: Int,
    val setCount: Int,
    val templateName: String? = null
)

@Entity(tableName = "workout_templates", indices = [Index(value = ["normalizedName"], unique = true)])
data class WorkoutTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val normalizedName: String,
    @ColumnInfo(defaultValue = "NULL") val generatorSession: String? = null
)

@Entity(tableName = "template_exercises", foreignKeys = [
    ForeignKey(entity = WorkoutTemplate::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.RESTRICT)
], indices = [Index("exerciseId"), Index(value = ["templateId", "position"], unique = true)])
data class TemplateExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val exerciseId: Long,
    val position: Int,
    // Null means the user explicitly chooses when this exercise is done.
    val plannedSets: Int?,
    @ColumnInfo(defaultValue = "NULL") val repMin: Int? = null,
    @ColumnInfo(defaultValue = "NULL") val repMax: Int? = null,
    @ColumnInfo(defaultValue = "NULL") val restSeconds: Int? = null,
    @ColumnInfo(defaultValue = "NULL") val circuitRound: Int? = null
)

data class TemplateWithExercises(
    @Embedded val template: WorkoutTemplate,
    @Relation(parentColumn = "id", entityColumn = "templateId") val exercises: List<TemplateExercise>
)

data class PlannedExercise(val exerciseId: Long, val sets: Int?, val repMin: Int? = null, val repMax: Int? = null, val restSeconds: Int? = null,
    val circuitRound: Int? = null)
