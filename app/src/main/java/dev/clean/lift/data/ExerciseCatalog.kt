package dev.clean.lift.data

enum class MuscleRegion { CHEST, BACK, SHOULDERS, ARMS, HIPS, THIGHS, CALVES, TRUNK }
enum class Muscle(val region: MuscleRegion) {
    PECS(MuscleRegion.CHEST), LATS(MuscleRegion.BACK), UPPER_BACK(MuscleRegion.BACK),
    FRONT_DELTS(MuscleRegion.SHOULDERS), SIDE_DELTS(MuscleRegion.SHOULDERS),
    REAR_DELTS(MuscleRegion.SHOULDERS), ROTATOR_CUFF(MuscleRegion.SHOULDERS),
    BICEPS(MuscleRegion.ARMS), TRICEPS(MuscleRegion.ARMS), FOREARMS(MuscleRegion.ARMS),
    GLUTES(MuscleRegion.HIPS), HIP_ABDUCTORS(MuscleRegion.HIPS), QUADS(MuscleRegion.THIGHS),
    HAMSTRINGS(MuscleRegion.THIGHS), ADDUCTORS(MuscleRegion.THIGHS),
    GASTROCNEMIUS(MuscleRegion.CALVES), SOLEUS(MuscleRegion.CALVES),
    ABS(MuscleRegion.TRUNK), OBLIQUES(MuscleRegion.TRUNK), SPINAL_EXTENSORS(MuscleRegion.TRUNK)
}
enum class Movement { SQUAT, HINGE, SPLIT_SQUAT, HORIZONTAL_PUSH, VERTICAL_PUSH, HORIZONTAL_PULL,
    VERTICAL_PULL, HIP_EXTENSION, KNEE_EXTENSION, KNEE_FLEXION, CALF_RAISE, SHOULDER_ADDUCTION,
    SHOULDER_ABDUCTION, ELBOW_FLEXION, ELBOW_EXTENSION, EXTERNAL_ROTATION, ANTI_EXTENSION,
    ANTI_ROTATION, ANTI_LATERAL_FLEXION, CARRY, JUMP, OLYMPIC, SCAPULAR_RETRACTION, TRUNK_FLEXION,
    HIP_ABDUCTION, HIP_ADDUCTION, WRIST_FLEXION }
enum class Equipment { FLOOR_SPACE, BARBELL, PLATES, RACK, SAFETIES, FLAT_BENCH, ADJUSTABLE_BENCH,
    DUMBBELL_SINGLE, DUMBBELL_PAIR, KETTLEBELL, HIGH_CABLE, LOW_CABLE, CABLE_HANDLE, CABLE_BAR,
    CABLE_ROPE, PULLDOWN_SEAT, ROW_SEAT, LEG_PRESS_MACHINE, LEG_EXTENSION_MACHINE,
    SEATED_LEG_CURL_MACHINE, PULL_UP_BAR, DIP_BARS, REAR_FOOT_SUPPORT, BOX, CLEAR_PATH, PLATFORM, LOW_FIXED_BAR }
enum class Coverage { COMPOUND, ISOLATION, INTEGRATED }
enum class Stability { MACHINE_GUIDED, BODY_SUPPORTED, FREE_STANDING, UNILATERAL_BALANCE }
enum class Skill { BASIC, MODERATE, ADVANCED }
enum class LoadConvention { TOTAL_EXTERNAL, PER_HAND, SINGLE_IMPLEMENT, BODYWEIGHT_ONLY, MACHINE_DISPLAY }
enum class RecordingMode { LOAD_REPS, REPS_ONLY, TIME, LOAD_DISTANCE }
enum class Discipline { RESISTANCE, CALISTHENICS, POWER, OLYMPIC_COMPETITION, OLYMPIC_DERIVATIVE }
enum class RangeOfMotion { STANDARD, ISOMETRIC, PARTIAL }
enum class Intent { CONTROLLED, FAST_CONCENTRIC, BALLISTIC }

// Modifiers never mutate catalog definitions or imply interchangeable historical loads.
data class ExerciseModifiers(
    val range: RangeOfMotion = RangeOfMotion.STANDARD,
    val intent: Intent = Intent.CONTROLLED,
    val pauseSeconds: Int = 0,
    val eccentricSeconds: Int? = null
)
data class CatalogExercise(
    val key: String,
    val family: String,
    val name: String,
    val legacyNames: Set<String>,
    val primary: Set<Muscle>,
    val secondary: Set<Muscle>,
    val stabilizers: Set<Muscle>,
    val movements: Set<Movement>,
    // OR between groups, AND within a group; quantities/load availability belong to profiles.
    val equipmentAlternatives: List<Set<Equipment>>,
    val coverage: Coverage,
    val stability: Stability,
    val skill: Skill,
    val load: LoadConvention,
    val recording: RecordingMode,
    val discipline: Discipline,
    val unilateral: Boolean,
    val baseRange: RangeOfMotion,
    val allowedIntents: Set<Intent>,
    val sourceIds: Set<String> = setOf("editorial-v1")
) {
    fun availableWith(available: Set<Equipment>): Boolean = equipmentAlternatives.any { available.containsAll(it) }
    fun supports(modifiers: ExerciseModifiers): Boolean =
        modifiers.intent in allowedIntents && modifiers.pauseSeconds in 0..5 &&
            (modifiers.eccentricSeconds == null || modifiers.eccentricSeconds in 1..10) &&
            (modifiers.range == baseRange || (baseRange == RangeOfMotion.STANDARD && modifiers.range == RangeOfMotion.PARTIAL)) &&
            (modifiers.intent == Intent.CONTROLLED ||
                (modifiers.pauseSeconds == 0 && modifiers.eccentricSeconds == null && modifiers.range == baseRange)) &&
            (baseRange != RangeOfMotion.ISOMETRIC || (modifiers.pauseSeconds == 0 && modifiers.eccentricSeconds == null))
}

object ExerciseCatalog {
    const val VERSION = 3
    val sources = mapOf(
        "editorial-v1" to "docs/features/EXERCISE-CATALOG.md",
        "ace-rdl" to "https://www.acefitness.org/continuing-education/certified/may-2025/8865/the-ace-do-it-better-series-the-romanian-deadlift/",
        "iwf-lifts" to "https://iwf.sport/weightlifting_/the-two-lifts/",
        "ace-scapular" to "https://www.acefitness.org/resources/everyone/exercise-library/249/prone-scapular-shoulder-stabilization-series-i-y-t-w-o-formation/"
    )
    private fun muscles(vararg values: Muscle) = values.toSet()
    private fun gear(vararg values: Equipment) = listOf(values.toSet())
    private val barFloor = gear(Equipment.BARBELL, Equipment.PLATES, Equipment.FLOOR_SPACE)
    private val barRack = gear(Equipment.BARBELL, Equipment.PLATES, Equipment.RACK, Equipment.SAFETIES, Equipment.FLOOR_SPACE)
    private val dumbbells = gear(Equipment.DUMBBELL_PAIR, Equipment.FLOOR_SPACE)
    private val floor = gear(Equipment.FLOOR_SPACE)
    private fun lift(
        key: String, name: String, legacy: String? = null, family: String = key,
        primary: Set<Muscle>, movement: Movement, equipment: List<Set<Equipment>>,
        secondary: Set<Muscle> = emptySet(), stabilizers: Set<Muscle> = emptySet(),
        coverage: Coverage = Coverage.COMPOUND, stability: Stability = Stability.FREE_STANDING,
        skill: Skill = Skill.BASIC, load: LoadConvention = LoadConvention.TOTAL_EXTERNAL,
        recording: RecordingMode = RecordingMode.LOAD_REPS, discipline: Discipline = Discipline.RESISTANCE,
        unilateral: Boolean = false, range: RangeOfMotion = RangeOfMotion.STANDARD,
        intents: Set<Intent> = setOf(Intent.CONTROLLED), sources: Set<String> = setOf("editorial-v1")
    ) = CatalogExercise(key, family, name, setOfNotNull(legacy), primary, secondary, stabilizers,
        setOf(movement), equipment, coverage, stability, skill, load, recording, discipline, unilateral, range, intents, sources)

    val exercises: List<CatalogExercise> = listOf(
        lift("back-squat-barbell", "Barbell Back Squat", "Squat", "squat", muscles(Muscle.QUADS, Muscle.GLUTES), Movement.SQUAT, barRack, secondary = muscles(Muscle.ADDUCTORS), stabilizers = muscles(Muscle.ABS, Muscle.SPINAL_EXTENSORS), skill = Skill.MODERATE),
        lift("front-squat-barbell", "Barbell Front Squat", "Front Squat", "squat", muscles(Muscle.QUADS, Muscle.GLUTES), Movement.SQUAT, barRack, stabilizers = muscles(Muscle.ABS, Muscle.UPPER_BACK), skill = Skill.MODERATE),
        lift("goblet-squat", "Goblet Squat", "Goblet Squat", "squat", muscles(Muscle.QUADS, Muscle.GLUTES), Movement.SQUAT, gear(Equipment.DUMBBELL_SINGLE, Equipment.FLOOR_SPACE) + gear(Equipment.KETTLEBELL, Equipment.FLOOR_SPACE), stabilizers = muscles(Muscle.ABS), load = LoadConvention.SINGLE_IMPLEMENT),
        lift("leg-press", "Leg Press Machine", "Leg Press", "squat", muscles(Muscle.QUADS, Muscle.GLUTES), Movement.SQUAT, gear(Equipment.LEG_PRESS_MACHINE), stability = Stability.MACHINE_GUIDED, load = LoadConvention.MACHINE_DISPLAY),
        lift("leg-extension", "Leg Extension Machine", "Leg Extension", primary = muscles(Muscle.QUADS), movement = Movement.KNEE_EXTENSION, equipment = gear(Equipment.LEG_EXTENSION_MACHINE), coverage = Coverage.ISOLATION, stability = Stability.MACHINE_GUIDED, load = LoadConvention.MACHINE_DISPLAY),
        lift("leg-curl-seated", "Seated Leg Curl", "Leg Curl", primary = muscles(Muscle.HAMSTRINGS), movement = Movement.KNEE_FLEXION, equipment = gear(Equipment.SEATED_LEG_CURL_MACHINE), coverage = Coverage.ISOLATION, stability = Stability.MACHINE_GUIDED, load = LoadConvention.MACHINE_DISPLAY),
        lift("deadlift-conventional", "Conventional Barbell Deadlift", "Deadlift", "hinge", muscles(Muscle.GLUTES, Muscle.HAMSTRINGS), Movement.HINGE, barFloor, secondary = muscles(Muscle.QUADS, Muscle.ADDUCTORS), stabilizers = muscles(Muscle.SPINAL_EXTENSORS, Muscle.ABS, Muscle.FOREARMS), skill = Skill.MODERATE),
        lift("rdl-barbell", "Barbell Romanian Deadlift", "Romanian Deadlift", "hinge", muscles(Muscle.HAMSTRINGS, Muscle.GLUTES), Movement.HINGE, barFloor, secondary = muscles(Muscle.ADDUCTORS), stabilizers = muscles(Muscle.SPINAL_EXTENSORS, Muscle.FOREARMS), skill = Skill.MODERATE, sources = setOf("editorial-v1", "ace-rdl")),
        lift("hip-thrust-barbell", "Barbell Hip Thrust", "Hip Thrust", primary = muscles(Muscle.GLUTES), movement = Movement.HIP_EXTENSION, equipment = gear(Equipment.BARBELL, Equipment.PLATES, Equipment.FLAT_BENCH, Equipment.FLOOR_SPACE), secondary = muscles(Muscle.HAMSTRINGS), stability = Stability.BODY_SUPPORTED),
        lift("lunge-dumbbell", "Dumbbell Reverse Lunge", "Lunge", "split-squat", muscles(Muscle.QUADS, Muscle.GLUTES), Movement.SPLIT_SQUAT, dumbbells, stabilizers = muscles(Muscle.HIP_ABDUCTORS, Muscle.ABS), stability = Stability.UNILATERAL_BALANCE, load = LoadConvention.PER_HAND, unilateral = true),
        lift("split-squat-rear-elevated-db", "Dumbbell Rear-foot Elevated Split Squat", "Bulgarian Split Squat", "split-squat", muscles(Muscle.QUADS, Muscle.GLUTES), Movement.SPLIT_SQUAT, gear(Equipment.DUMBBELL_PAIR, Equipment.REAR_FOOT_SUPPORT, Equipment.FLOOR_SPACE), stabilizers = muscles(Muscle.HIP_ABDUCTORS), stability = Stability.UNILATERAL_BALANCE, skill = Skill.MODERATE, load = LoadConvention.PER_HAND, unilateral = true),
        lift("calf-raise-standing-db", "Standing Dumbbell Calf Raise", "Calf Raise", primary = muscles(Muscle.GASTROCNEMIUS, Muscle.SOLEUS), movement = Movement.CALF_RAISE, equipment = dumbbells, coverage = Coverage.ISOLATION, load = LoadConvention.PER_HAND),
        lift("bench-barbell", "Flat Barbell Bench Press", "Bench Press", "chest-press", muscles(Muscle.PECS), Movement.HORIZONTAL_PUSH, gear(Equipment.BARBELL, Equipment.PLATES, Equipment.RACK, Equipment.SAFETIES, Equipment.FLAT_BENCH), secondary = muscles(Muscle.TRICEPS, Muscle.FRONT_DELTS), stability = Stability.BODY_SUPPORTED),
        lift("bench-incline-barbell", "Incline Barbell Bench Press", "Incline Bench Press", "chest-press", muscles(Muscle.PECS, Muscle.FRONT_DELTS), Movement.HORIZONTAL_PUSH, gear(Equipment.BARBELL, Equipment.PLATES, Equipment.RACK, Equipment.SAFETIES, Equipment.ADJUSTABLE_BENCH), secondary = muscles(Muscle.TRICEPS), stability = Stability.BODY_SUPPORTED),
        lift("bench-dumbbell", "Flat Dumbbell Bench Press", "Dumbbell Bench Press", "chest-press", muscles(Muscle.PECS), Movement.HORIZONTAL_PUSH, gear(Equipment.DUMBBELL_PAIR, Equipment.FLAT_BENCH), secondary = muscles(Muscle.TRICEPS, Muscle.FRONT_DELTS), stability = Stability.BODY_SUPPORTED, load = LoadConvention.PER_HAND),
        lift("fly-dumbbell", "Flat Dumbbell Chest Fly", "Chest Fly", primary = muscles(Muscle.PECS), movement = Movement.SHOULDER_ADDUCTION, equipment = gear(Equipment.DUMBBELL_PAIR, Equipment.FLAT_BENCH), coverage = Coverage.ISOLATION, stability = Stability.BODY_SUPPORTED, load = LoadConvention.PER_HAND),
        lift("push-up", "Floor Push-up", "Push-up", "push-up", muscles(Muscle.PECS), Movement.HORIZONTAL_PUSH, floor, secondary = muscles(Muscle.TRICEPS, Muscle.FRONT_DELTS), stabilizers = muscles(Muscle.ABS), stability = Stability.BODY_SUPPORTED, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY, discipline = Discipline.CALISTHENICS),
        lift("press-standing-barbell", "Standing Strict Barbell Press", "Overhead Press", "overhead-press", muscles(Muscle.FRONT_DELTS, Muscle.SIDE_DELTS), Movement.VERTICAL_PUSH, barFloor, secondary = muscles(Muscle.TRICEPS), stabilizers = muscles(Muscle.ABS), skill = Skill.MODERATE),
        lift("press-seated-dumbbell", "Seated Dumbbell Shoulder Press", "Dumbbell Shoulder Press", "overhead-press", muscles(Muscle.FRONT_DELTS, Muscle.SIDE_DELTS), Movement.VERTICAL_PUSH, gear(Equipment.DUMBBELL_PAIR, Equipment.ADJUSTABLE_BENCH), secondary = muscles(Muscle.TRICEPS), stability = Stability.BODY_SUPPORTED, load = LoadConvention.PER_HAND),
        lift("lateral-raise-db", "Dumbbell Lateral Raise", "Lateral Raise", primary = muscles(Muscle.SIDE_DELTS), movement = Movement.SHOULDER_ABDUCTION, equipment = dumbbells, coverage = Coverage.ISOLATION, load = LoadConvention.PER_HAND),
        lift("pull-up-overhand", "Overhand Pull-up", "Pull-up", "vertical-pull", muscles(Muscle.LATS), Movement.VERTICAL_PULL, gear(Equipment.PULL_UP_BAR), secondary = muscles(Muscle.BICEPS, Muscle.UPPER_BACK), stabilizers = muscles(Muscle.ABS, Muscle.FOREARMS), skill = Skill.MODERATE, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY, discipline = Discipline.CALISTHENICS),
        lift("chin-up", "Underhand Chin-up", "Chin-up", "vertical-pull", muscles(Muscle.LATS, Muscle.BICEPS), Movement.VERTICAL_PULL, gear(Equipment.PULL_UP_BAR), stabilizers = muscles(Muscle.ABS, Muscle.FOREARMS), skill = Skill.MODERATE, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY, discipline = Discipline.CALISTHENICS),
        lift("lat-pulldown", "Seated Cable Lat Pulldown", "Lat Pulldown", "vertical-pull", muscles(Muscle.LATS), Movement.VERTICAL_PULL, gear(Equipment.HIGH_CABLE, Equipment.CABLE_BAR, Equipment.PULLDOWN_SEAT), secondary = muscles(Muscle.BICEPS), stability = Stability.BODY_SUPPORTED, load = LoadConvention.MACHINE_DISPLAY),
        lift("row-barbell", "Bent-over Barbell Row", "Barbell Row", "row", muscles(Muscle.LATS, Muscle.UPPER_BACK), Movement.HORIZONTAL_PULL, barFloor, secondary = muscles(Muscle.BICEPS, Muscle.REAR_DELTS), stabilizers = muscles(Muscle.SPINAL_EXTENSORS, Muscle.ABS), skill = Skill.MODERATE),
        lift("row-supported-db", "Bench-supported Single-arm Dumbbell Row", "Dumbbell Row", "row", muscles(Muscle.LATS, Muscle.UPPER_BACK), Movement.HORIZONTAL_PULL, gear(Equipment.DUMBBELL_SINGLE, Equipment.FLAT_BENCH), secondary = muscles(Muscle.BICEPS, Muscle.REAR_DELTS), stability = Stability.BODY_SUPPORTED, load = LoadConvention.SINGLE_IMPLEMENT, unilateral = true),
        lift("row-cable", "Seated Cable Row", "Seated Cable Row", "row", muscles(Muscle.LATS, Muscle.UPPER_BACK), Movement.HORIZONTAL_PULL, gear(Equipment.LOW_CABLE, Equipment.CABLE_HANDLE, Equipment.ROW_SEAT), secondary = muscles(Muscle.BICEPS, Muscle.REAR_DELTS), stability = Stability.BODY_SUPPORTED, load = LoadConvention.MACHINE_DISPLAY),
        lift("face-pull-cable", "Cable Rope Face Pull", "Face Pull", primary = muscles(Muscle.REAR_DELTS, Muscle.UPPER_BACK), movement = Movement.EXTERNAL_ROTATION, equipment = gear(Equipment.HIGH_CABLE, Equipment.CABLE_ROPE, Equipment.FLOOR_SPACE), secondary = muscles(Muscle.ROTATOR_CUFF), load = LoadConvention.MACHINE_DISPLAY),
        lift("curl-supinated-db", "Supinated Dumbbell Curl", "Biceps Curl", "curl", muscles(Muscle.BICEPS), Movement.ELBOW_FLEXION, dumbbells, coverage = Coverage.ISOLATION, load = LoadConvention.PER_HAND),
        lift("curl-hammer-db", "Dumbbell Hammer Curl", "Hammer Curl", "curl", muscles(Muscle.BICEPS, Muscle.FOREARMS), Movement.ELBOW_FLEXION, dumbbells, coverage = Coverage.ISOLATION, load = LoadConvention.PER_HAND),
        lift("pushdown-rope", "Cable Rope Triceps Pushdown", "Triceps Pushdown", primary = muscles(Muscle.TRICEPS), movement = Movement.ELBOW_EXTENSION, equipment = gear(Equipment.HIGH_CABLE, Equipment.CABLE_ROPE, Equipment.FLOOR_SPACE), coverage = Coverage.ISOLATION, load = LoadConvention.MACHINE_DISPLAY),
        lift("dip-bars", "Parallel-bar Dip", "Dip", primary = muscles(Muscle.TRICEPS, Muscle.PECS), movement = Movement.VERTICAL_PUSH, equipment = gear(Equipment.DIP_BARS), secondary = muscles(Muscle.FRONT_DELTS), skill = Skill.MODERATE, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY, discipline = Discipline.CALISTHENICS),
        lift("squat-bodyweight", "Bodyweight Squat", family = "squat", primary = muscles(Muscle.QUADS, Muscle.GLUTES), movement = Movement.SQUAT, equipment = floor, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY, discipline = Discipline.CALISTHENICS),
        lift("split-squat-bodyweight", "Bodyweight Split Squat", family = "split-squat", primary = muscles(Muscle.QUADS, Muscle.GLUTES), movement = Movement.SPLIT_SQUAT, equipment = floor, stabilizers = muscles(Muscle.HIP_ABDUCTORS), stability = Stability.UNILATERAL_BALANCE, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY, discipline = Discipline.CALISTHENICS, unilateral = true),
        lift("glute-bridge", "Bodyweight Glute Bridge", primary = muscles(Muscle.GLUTES), movement = Movement.HIP_EXTENSION, equipment = floor, secondary = muscles(Muscle.HAMSTRINGS), stability = Stability.BODY_SUPPORTED, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY, discipline = Discipline.CALISTHENICS),
        lift("push-up-kneeling", "Kneeling Push-up", family = "push-up", primary = muscles(Muscle.PECS), movement = Movement.HORIZONTAL_PUSH, equipment = floor, secondary = muscles(Muscle.TRICEPS, Muscle.FRONT_DELTS), stability = Stability.BODY_SUPPORTED, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY, discipline = Discipline.CALISTHENICS),
        lift("dead-bug", "Dead Bug", primary = muscles(Muscle.ABS), movement = Movement.ANTI_EXTENSION, equipment = floor, stability = Stability.BODY_SUPPORTED, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY),
        lift("plank", "Forearm Plank", primary = muscles(Muscle.ABS), movement = Movement.ANTI_EXTENSION, equipment = floor, stability = Stability.BODY_SUPPORTED, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.TIME, range = RangeOfMotion.ISOMETRIC),
        lift("side-plank", "Side Plank", primary = muscles(Muscle.OBLIQUES), movement = Movement.ANTI_LATERAL_FLEXION, equipment = floor, secondary = muscles(Muscle.HIP_ABDUCTORS), stability = Stability.BODY_SUPPORTED, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.TIME, unilateral = true, range = RangeOfMotion.ISOMETRIC),
        lift("pallof-press", "Cable Pallof Press", primary = muscles(Muscle.OBLIQUES, Muscle.ABS), movement = Movement.ANTI_ROTATION, equipment = gear(Equipment.CABLE_HANDLE, Equipment.HIGH_CABLE, Equipment.FLOOR_SPACE), load = LoadConvention.MACHINE_DISPLAY),
        lift("farmer-carry", "Dumbbell Farmer Carry", primary = muscles(Muscle.FOREARMS), movement = Movement.CARRY, equipment = gear(Equipment.DUMBBELL_PAIR, Equipment.CLEAR_PATH), stabilizers = muscles(Muscle.ABS, Muscle.OBLIQUES, Muscle.UPPER_BACK), coverage = Coverage.INTEGRATED, load = LoadConvention.PER_HAND, recording = RecordingMode.LOAD_DISTANCE),
        lift("swing-kettlebell", "Two-hand Kettlebell Swing", family = "hinge", primary = muscles(Muscle.GLUTES, Muscle.HAMSTRINGS), movement = Movement.HINGE, equipment = gear(Equipment.KETTLEBELL, Equipment.FLOOR_SPACE), stabilizers = muscles(Muscle.ABS, Muscle.SPINAL_EXTENSORS, Muscle.FOREARMS), skill = Skill.MODERATE, load = LoadConvention.SINGLE_IMPLEMENT, discipline = Discipline.POWER, intents = setOf(Intent.BALLISTIC)),
        lift("box-jump", "Box Jump", primary = muscles(Muscle.QUADS, Muscle.GLUTES), movement = Movement.JUMP, equipment = gear(Equipment.BOX, Equipment.FLOOR_SPACE), secondary = muscles(Muscle.GASTROCNEMIUS), skill = Skill.MODERATE, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY, discipline = Discipline.POWER, intents = setOf(Intent.BALLISTIC)),
        lift("power-clean", "Barbell Power Clean", primary = muscles(Muscle.GLUTES, Muscle.QUADS, Muscle.HAMSTRINGS), movement = Movement.OLYMPIC, equipment = gear(Equipment.BARBELL, Equipment.PLATES, Equipment.PLATFORM), stabilizers = muscles(Muscle.UPPER_BACK, Muscle.ABS), skill = Skill.ADVANCED, discipline = Discipline.OLYMPIC_DERIVATIVE, intents = setOf(Intent.BALLISTIC)),
        lift("snatch", "Barbell Snatch", primary = muscles(Muscle.GLUTES, Muscle.QUADS, Muscle.HAMSTRINGS), movement = Movement.OLYMPIC, equipment = gear(Equipment.BARBELL, Equipment.PLATES, Equipment.PLATFORM), stabilizers = muscles(Muscle.ROTATOR_CUFF, Muscle.UPPER_BACK, Muscle.ABS), skill = Skill.ADVANCED, discipline = Discipline.OLYMPIC_COMPETITION, intents = setOf(Intent.BALLISTIC), sources = setOf("editorial-v1", "iwf-lifts")),
        lift("clean-jerk", "Barbell Clean and Jerk", primary = muscles(Muscle.GLUTES, Muscle.QUADS, Muscle.HAMSTRINGS, Muscle.FRONT_DELTS), movement = Movement.OLYMPIC, equipment = gear(Equipment.BARBELL, Equipment.PLATES, Equipment.PLATFORM), secondary = muscles(Muscle.TRICEPS), stabilizers = muscles(Muscle.ABS, Muscle.UPPER_BACK), skill = Skill.ADVANCED, discipline = Discipline.OLYMPIC_COMPETITION, intents = setOf(Intent.BALLISTIC), sources = setOf("editorial-v1", "iwf-lifts")),
        lift("deadlift-kettlebell", "Two-hand Kettlebell Deadlift", family = "hinge", primary = muscles(Muscle.GLUTES, Muscle.HAMSTRINGS), movement = Movement.HINGE, equipment = gear(Equipment.KETTLEBELL, Equipment.FLOOR_SPACE), stabilizers = muscles(Muscle.ABS, Muscle.SPINAL_EXTENSORS), load = LoadConvention.SINGLE_IMPLEMENT),
        lift("row-kettlebell", "Single-arm Kettlebell Row", family = "row", primary = muscles(Muscle.LATS, Muscle.UPPER_BACK), movement = Movement.HORIZONTAL_PULL, equipment = gear(Equipment.KETTLEBELL, Equipment.FLOOR_SPACE), secondary = muscles(Muscle.BICEPS), stabilizers = muscles(Muscle.ABS, Muscle.SPINAL_EXTENSORS), load = LoadConvention.SINGLE_IMPLEMENT, unilateral = true),
        lift("floor-press-kettlebell", "Single-arm Kettlebell Floor Press", family = "chest-press", primary = muscles(Muscle.PECS), movement = Movement.HORIZONTAL_PUSH, equipment = gear(Equipment.KETTLEBELL, Equipment.FLOOR_SPACE), secondary = muscles(Muscle.TRICEPS, Muscle.FRONT_DELTS), stability = Stability.BODY_SUPPORTED, load = LoadConvention.SINGLE_IMPLEMENT, unilateral = true),
        lift("press-kettlebell", "Single-arm Strict Kettlebell Press", family = "overhead-press", primary = muscles(Muscle.FRONT_DELTS, Muscle.SIDE_DELTS), movement = Movement.VERTICAL_PUSH, equipment = gear(Equipment.KETTLEBELL, Equipment.FLOOR_SPACE), secondary = muscles(Muscle.TRICEPS), stabilizers = muscles(Muscle.ABS, Muscle.OBLIQUES), skill = Skill.MODERATE, load = LoadConvention.SINGLE_IMPLEMENT, unilateral = true),
        lift("split-squat-kettlebell", "Goblet Kettlebell Split Squat", family = "split-squat", primary = muscles(Muscle.QUADS, Muscle.GLUTES), movement = Movement.SPLIT_SQUAT, equipment = gear(Equipment.KETTLEBELL, Equipment.FLOOR_SPACE), stabilizers = muscles(Muscle.HIP_ABDUCTORS), stability = Stability.UNILATERAL_BALANCE, load = LoadConvention.SINGLE_IMPLEMENT, unilateral = true),
        lift("rdl-dumbbell", "Dumbbell Romanian Deadlift", family = "hinge", primary = muscles(Muscle.HAMSTRINGS, Muscle.GLUTES), movement = Movement.HINGE, equipment = dumbbells, stabilizers = muscles(Muscle.SPINAL_EXTENSORS, Muscle.ABS), skill = Skill.MODERATE, load = LoadConvention.PER_HAND),
        lift("floor-press-dumbbell", "Dumbbell Floor Press", family = "chest-press", primary = muscles(Muscle.PECS), movement = Movement.HORIZONTAL_PUSH, equipment = dumbbells, secondary = muscles(Muscle.TRICEPS, Muscle.FRONT_DELTS), stability = Stability.BODY_SUPPORTED, load = LoadConvention.PER_HAND),
        lift("row-dumbbell", "Single-arm Dumbbell Row (Unsupported)", family = "row", primary = muscles(Muscle.LATS, Muscle.UPPER_BACK), movement = Movement.HORIZONTAL_PULL, equipment = gear(Equipment.DUMBBELL_SINGLE, Equipment.FLOOR_SPACE), secondary = muscles(Muscle.BICEPS), stabilizers = muscles(Muscle.SPINAL_EXTENSORS, Muscle.ABS), load = LoadConvention.SINGLE_IMPLEMENT, unilateral = true),
        lift("row-inverted", "Fixed-bar Inverted Row", family = "row", primary = muscles(Muscle.LATS, Muscle.UPPER_BACK), movement = Movement.HORIZONTAL_PULL, equipment = gear(Equipment.LOW_FIXED_BAR, Equipment.FLOOR_SPACE), secondary = muscles(Muscle.BICEPS, Muscle.REAR_DELTS), stabilizers = muscles(Muscle.ABS), load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY, discipline = Discipline.CALISTHENICS),
        lift("prone-w", "Prone W Raise", family = "scapular-w", primary = muscles(Muscle.UPPER_BACK, Muscle.REAR_DELTS), movement = Movement.SCAPULAR_RETRACTION, equipment = floor, secondary = muscles(Muscle.ROTATOR_CUFF), coverage = Coverage.ISOLATION, stability = Stability.BODY_SUPPORTED, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY, sources = setOf("editorial-v1", "ace-scapular")),
        lift("prone-t", "Prone T Raise", family = "scapular-t", primary = muscles(Muscle.UPPER_BACK, Muscle.REAR_DELTS), movement = Movement.SCAPULAR_RETRACTION, equipment = floor, coverage = Coverage.ISOLATION, stability = Stability.BODY_SUPPORTED, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY, sources = setOf("editorial-v1", "ace-scapular")),
        lift("reverse-crunch", "Reverse Crunch", primary = muscles(Muscle.ABS), movement = Movement.TRUNK_FLEXION, equipment = floor, coverage = Coverage.ISOLATION, stability = Stability.BODY_SUPPORTED, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY),
        lift("bicycle-crunch", "Bicycle Crunch", primary = muscles(Muscle.ABS, Muscle.OBLIQUES), movement = Movement.TRUNK_FLEXION, equipment = floor, coverage = Coverage.ISOLATION, stability = Stability.BODY_SUPPORTED, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY, unilateral = true),
        lift("bird-dog", "Bird Dog", primary = muscles(Muscle.ABS, Muscle.OBLIQUES), movement = Movement.ANTI_ROTATION, equipment = floor, secondary = muscles(Muscle.GLUTES), stabilizers = muscles(Muscle.SPINAL_EXTENSORS), stability = Stability.BODY_SUPPORTED, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY, unilateral = true),
        lift("pike-push-up", "Pike Push-up", family = "overhead-press", primary = muscles(Muscle.FRONT_DELTS, Muscle.TRICEPS), movement = Movement.VERTICAL_PUSH, equipment = floor, stabilizers = muscles(Muscle.ABS), skill = Skill.MODERATE, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY, discipline = Discipline.CALISTHENICS),
        lift("calf-raise-bodyweight", "Standing Bodyweight Calf Raise", family = "calf-raise", primary = muscles(Muscle.GASTROCNEMIUS, Muscle.SOLEUS), movement = Movement.CALF_RAISE, equipment = floor, coverage = Coverage.ISOLATION, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY),
        lift("hip-abduction-side", "Side-lying Hip Abduction", primary = muscles(Muscle.HIP_ABDUCTORS), movement = Movement.HIP_ABDUCTION, equipment = floor, coverage = Coverage.ISOLATION, stability = Stability.BODY_SUPPORTED, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY, unilateral = true),
        lift("hip-adduction-side", "Side-lying Hip Adduction", primary = muscles(Muscle.ADDUCTORS), movement = Movement.HIP_ADDUCTION, equipment = floor, coverage = Coverage.ISOLATION, stability = Stability.BODY_SUPPORTED, load = LoadConvention.BODYWEIGHT_ONLY, recording = RecordingMode.REPS_ONLY, unilateral = true),
        lift("pullover-dumbbell", "Dumbbell Pullover", primary = muscles(Muscle.LATS, Muscle.PECS), movement = Movement.VERTICAL_PULL, equipment = gear(Equipment.DUMBBELL_SINGLE, Equipment.FLAT_BENCH), coverage = Coverage.ISOLATION, stability = Stability.BODY_SUPPORTED, load = LoadConvention.SINGLE_IMPLEMENT),
        lift("rear-fly-dumbbell", "Dumbbell Rear Delt Fly", primary = muscles(Muscle.REAR_DELTS, Muscle.UPPER_BACK), movement = Movement.SHOULDER_ABDUCTION, equipment = dumbbells, stabilizers = muscles(Muscle.SPINAL_EXTENSORS), coverage = Coverage.ISOLATION, load = LoadConvention.PER_HAND),
        lift("triceps-extension-db", "Single-dumbbell Overhead Triceps Extension", family = "triceps-extension", primary = muscles(Muscle.TRICEPS), movement = Movement.ELBOW_EXTENSION, equipment = gear(Equipment.DUMBBELL_SINGLE, Equipment.FLOOR_SPACE), coverage = Coverage.ISOLATION, load = LoadConvention.SINGLE_IMPLEMENT),
        lift("wrist-curl-db", "Seated Dumbbell Wrist Curl", primary = muscles(Muscle.FOREARMS), movement = Movement.WRIST_FLEXION, equipment = gear(Equipment.DUMBBELL_SINGLE, Equipment.FLAT_BENCH), coverage = Coverage.ISOLATION, stability = Stability.BODY_SUPPORTED, load = LoadConvention.SINGLE_IMPLEMENT, unilateral = true),
        lift("curl-kettlebell", "Two-hand Kettlebell Curl", family = "curl", primary = muscles(Muscle.BICEPS), movement = Movement.ELBOW_FLEXION, equipment = gear(Equipment.KETTLEBELL, Equipment.FLOOR_SPACE), coverage = Coverage.ISOLATION, load = LoadConvention.SINGLE_IMPLEMENT),
        lift("halo-kettlebell", "Kettlebell Halo", primary = muscles(Muscle.ROTATOR_CUFF), movement = Movement.EXTERNAL_ROTATION, equipment = gear(Equipment.KETTLEBELL, Equipment.FLOOR_SPACE), stabilizers = muscles(Muscle.ABS), coverage = Coverage.INTEGRATED, skill = Skill.MODERATE, load = LoadConvention.SINGLE_IMPLEMENT)
    )
    private val byKey = exercises.associateBy { it.key }
    fun find(key: String): CatalogExercise? = byKey[key]
    // Suggestions only: callers must confirm association; never reinterpret legacy history by name.
    fun legacyCandidates(name: String): List<CatalogExercise> = exercises.filter { entry ->
        entry.legacyNames.any { ExerciseNames.key(it) == ExerciseNames.key(name) }
    }
    fun eligible(available: Set<Equipment>, modes: Set<RecordingMode>, maximumSkill: Skill = Skill.BASIC,
        allowExplosive: Boolean = false): List<CatalogExercise> = exercises.filter {
        it.availableWith(available) && it.recording in modes && it.skill.ordinal <= maximumSkill.ordinal &&
            (allowExplosive || Intent.BALLISTIC !in it.allowedIntents)
    }.sortedBy { it.key }
    // Primary coverage only. Secondary/stabilizer involvement is not equivalent to direct work.
    fun missingPrimaryTargets(keys: Set<String>, desired: Set<Muscle>): Set<Muscle> {
        require(keys.all { it in byKey })
        return desired - keys.flatMap { byKey.getValue(it).primary }.toSet()
    }
    fun comparisonKey(key: String, modifiers: ExerciseModifiers): String {
        val entry = requireNotNull(find(key))
        require(entry.supports(modifiers))
        return "$key|${modifiers.range}|${modifiers.intent}|${modifiers.pauseSeconds}|${modifiers.eccentricSeconds}"
    }
}
