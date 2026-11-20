package dev.clean.lift.data

enum class GeneratorStyle(val label: String, val sessions: List<String>) {
    FULL_BODY("Full body A/B", listOf("full:a", "full:b")),
    PPL("Pull / Push / Legs", listOf("ppl:pull", "ppl:push", "ppl:legs")),
    BODYWEIGHT("Bodyweight", listOf("body:a", "body:b")),
    FIVE_BY_FIVE("5×5 main lifts", listOf("five:a", "five:b")),
    KETTLEBELL("Kettlebell strength", listOf("kb:a", "kb:b"))
}
data class GeneratorRequest(val style: GeneratorStyle, val session: String, val equipment: Set<Equipment>,
    val exerciseLimit: Int = 5, val effort: Int = 3, val allowSwings: Boolean = false, val allowTechnical: Boolean = false,
    val compoundFirst: Boolean = true, val abCircuit: Boolean = false)
data class GeneratedExercise(val key: String, val sets: Int, val repMin: Int, val repMax: Int, val restSeconds: Int, val reason: String,
    val circuitRound: Int? = null)
data class GeneratedWorkout(val session: String, val rows: List<GeneratedExercise>, val gaps: List<String>)

object WorkoutGenerator {
    const val VERSION = 2
    val sessionKeys = GeneratorStyle.entries.flatMap { it.sessions }.toSet()
    fun nextSession(style: GeneratorStyle, history: List<Workout>): String {
        val latest = history.filter { it.finishedAt != null && it.generatorSession in style.sessions }
            .maxWithOrNull(compareBy<Workout> { it.finishedAt }.thenBy { it.id })
        val previousIndex = style.sessions.indexOf(latest?.generatorSession)
        return style.sessions[(previousIndex + 1) % style.sessions.size]
    }
    fun sessionLabel(key: String): String = when (key.substringAfter(':')) {
        "pull" -> "Pull"; "push" -> "Push"; "legs" -> "Legs"; "b" -> "B"; else -> "A"
    }
    private data class Slot(val label: String, val candidates: List<String>, val required: Boolean = true, val main: Boolean = true)
    private fun slot(label: String, vararg keys: String, required: Boolean = true, main: Boolean = true) = Slot(label, keys.toList(), required, main)
    private val squat = slot("Squat", "back-squat-barbell", "goblet-squat", "front-squat-barbell", "leg-press", "squat-bodyweight")
    private val hinge = slot("Hinge", "deadlift-conventional", "rdl-barbell", "rdl-dumbbell", "deadlift-kettlebell", "glute-bridge")
    private val push = slot("Horizontal push", "bench-barbell", "push-up", "bench-dumbbell", "floor-press-dumbbell", "floor-press-kettlebell", "push-up-kneeling")
    private val pull = slot("Pull", "pull-up-overhand", "row-barbell", "lat-pulldown", "row-supported-db", "row-dumbbell", "row-kettlebell", "row-cable", "row-inverted", "prone-w")
    private val overhead = slot("Vertical push", "press-standing-barbell", "press-seated-dumbbell", "press-kettlebell", "dip-bars", "pike-push-up", "push-up-kneeling")
    private val trunk = slot("Trunk", "dead-bug", "pallof-press", required = false, main = false)
    private val singleLeg = slot("Single-leg", "split-squat-rear-elevated-db", "lunge-dumbbell", "split-squat-kettlebell", "split-squat-bodyweight", required = false, main = false)
    private val calves = slot("Calves", "calf-raise-standing-db", "calf-raise-bodyweight", required = false, main = false)
    private val rear = slot("Rear shoulders", "face-pull-cable", "rear-fly-dumbbell", "prone-t", required = false, main = false)
    private val arms = slot("Elbow flexion", "curl-supinated-db", "curl-hammer-db", "curl-kettlebell", required = false, main = false)
    private val triceps = slot("Elbow extension", "pushdown-rope", "triceps-extension-db", required = false, main = false)
    private val side = slot("Side shoulders", "lateral-raise-db", required = false, main = false)
    private val hips = slot("Hip abduction", "hip-abduction-side", required = false, main = false)
    private val adductors = slot("Hip adduction", "hip-adduction-side", required = false, main = false)
    private val birdDog = slot("Trunk stability", "bird-dog", required = false, main = false)
    private fun slots(request: GeneratorRequest): List<Slot> {
        val b = request.session.endsWith(":b")
        val result = when (request.style) {
            GeneratorStyle.FULL_BODY -> (if (b) listOf(hinge, overhead, pull, squat.copy(main = false)) else listOf(squat, push, pull, hinge.copy(main = false))) +
                listOf(trunk, singleLeg, calves, rear, arms, triceps, side, hips, adductors, birdDog)
            GeneratorStyle.PPL -> when (request.session) {
                "ppl:pull" -> listOf(pull, slot("Complementary pull", "row-supported-db", "row-cable", "row-barbell", "row-dumbbell", "row-kettlebell", "chin-up", "row-inverted", "prone-t"),
                    rear, arms, trunk, slot("Pullover", "pullover-dumbbell", required = false, main = false),
                    slot("Forearms", "wrist-curl-db", required = false, main = false), birdDog)
                "ppl:push" -> listOf(push, overhead, side, triceps, trunk, rear, birdDog)
                else -> listOf(squat, hinge, slot("Single-leg", "split-squat-rear-elevated-db", "lunge-dumbbell", "split-squat-kettlebell", "split-squat-bodyweight", required = false),
                    calves, trunk, hips, adductors, birdDog)
            }
            GeneratorStyle.BODYWEIGHT -> listOf(
                if (b) slot("Legs", "split-squat-bodyweight", "squat-bodyweight") else slot("Legs", "squat-bodyweight", "split-squat-bodyweight"),
                slot("Push", "push-up", "push-up-kneeling"), slot("Pull", "pull-up-overhand", "chin-up", "row-inverted", "prone-w"),
                slot("Hip extension", "glute-bridge"), trunk, singleLeg, calves, rear, hips, adductors, birdDog)
            GeneratorStyle.FIVE_BY_FIVE -> listOf(slot("Squat", "back-squat-barbell", "front-squat-barbell", "squat-bodyweight"),
                if (b) slot("Press", "press-standing-barbell", "pike-push-up", "push-up-kneeling") else slot("Press", "bench-barbell", "push-up", "push-up-kneeling"),
                slot("Row", "row-barbell", "row-inverted", "prone-w"), slot("Hinge support", "rdl-barbell", "deadlift-conventional", "glute-bridge", required = false, main = false),
                trunk, singleLeg, calves, rear, hips, adductors)
            GeneratorStyle.KETTLEBELL -> listOf(
                if (request.allowSwings) slot("Explosive hinge", "swing-kettlebell", "glute-bridge") else slot("Hinge", "deadlift-kettlebell", "glute-bridge"),
                slot("Squat", "goblet-squat", "squat-bodyweight"),
                if (b) slot("Press", "press-kettlebell", "floor-press-kettlebell", "push-up") else slot("Press", "floor-press-kettlebell", "press-kettlebell", "push-up"),
                slot("Row", "row-kettlebell", "prone-w"), singleLeg, trunk, calves, arms, rear, hips, adductors, birdDog)
        }
        return if (request.abCircuit) result.filter { it != trunk && it != birdDog } else result
    }
    fun candidates(request: GeneratorRequest): List<CatalogExercise> {
        val available = if (request.style == GeneratorStyle.KETTLEBELL) request.equipment.intersect(setOf(Equipment.KETTLEBELL, Equipment.FLOOR_SPACE)) else request.equipment
        return ExerciseCatalog.eligible(available, setOf(RecordingMode.LOAD_REPS, RecordingMode.REPS_ONLY), Skill.MODERATE, request.style == GeneratorStyle.KETTLEBELL && request.allowSwings)
            .filter { it.discipline != Discipline.OLYMPIC_COMPETITION && it.discipline != Discipline.OLYMPIC_DERIVATIVE &&
                (it.skill == Skill.BASIC || request.allowTechnical || (it.key == "swing-kettlebell" && request.style == GeneratorStyle.KETTLEBELL && request.allowSwings)) &&
                (request.style != GeneratorStyle.BODYWEIGHT || it.recording == RecordingMode.REPS_ONLY) &&
                (request.style != GeneratorStyle.FIVE_BY_FIVE || it.load in setOf(LoadConvention.TOTAL_EXTERNAL, LoadConvention.BODYWEIGHT_ONLY)) &&
                (Intent.BALLISTIC !in it.allowedIntents || it.key == "swing-kettlebell") }
    }
    fun generate(request: GeneratorRequest): GeneratedWorkout {
        require(request.session in request.style.sessions && request.exerciseLimit in 4..10 && request.effort in 1..5)
        val eligible = candidates(request).associateBy { it.key }
        val selected = mutableListOf<GeneratedExercise>()
        val families = mutableSetOf<String>()
        val gaps = mutableListOf<String>()
        for (slot in slots(request)) {
            if (selected.size >= request.exerciseLimit) break
            val exercise = slot.candidates.mapNotNull(eligible::get).firstOrNull {
                selected.none { row -> row.key == it.key } && it.family !in families
            } ?: if (slot.required) slot.candidates.mapNotNull(eligible::get).firstOrNull {
                it.recording == RecordingMode.REPS_ONLY && selected.none { row -> row.key == it.key }
            } else null
            if (exercise == null) { if (slot.required) gaps.add("${slot.label}: no compatible exercise with this equipment."); continue }
            families.add(exercise.family)
            val five = request.style == GeneratorStyle.FIVE_BY_FIVE && slot.main && exercise.load == LoadConvention.TOTAL_EXTERNAL
            if (exercise.key in setOf("prone-w", "prone-t") && slot.required) gaps.add("${slot.label}: using floor upper-back accessory work. It is not a loaded row or pull-up and does not provide equivalent lat work. A suitable low fixed bar enables inverted rows.")
            if (exercise.key == "push-up-kneeling" && slot.label == "Vertical push") gaps.add("Vertical push: using a horizontal bodyweight push; overhead coverage remains limited.")
            if (request.style == GeneratorStyle.FIVE_BY_FIVE && slot.main && !five) gaps.add("${slot.label}: bodyweight fallback uses ordinary rep targets instead of 5×5.")
            val sets = when { five -> 5; request.effort == 1 -> 1; request.effort == 2 -> 2;
                !slot.main -> if (request.effort == 5) 4 else if (request.effort == 4) 3 else 2; else -> 3 }
            val min = if (five) 5 else if (exercise.key == "swing-kettlebell") 10 else 6
            val max = if (five) 5 else if (exercise.key == "swing-kettlebell") 15 else 10
            selected.add(GeneratedExercise(exercise.key, sets, min, max, if (five) 180 else if (slot.main) 120 else 60,
                "${slot.label} • ${if (exercise.key == slot.candidates.first()) "preferred staple" else "alternative matching equipment and skill choices"}"))
        }
        val ordered = if (request.compoundFirst) selected.sortedBy { row ->
            val exercise = eligible.getValue(row.key)
            when {
                Intent.BALLISTIC in exercise.allowedIntents -> 0
                exercise.coverage != Coverage.ISOLATION && exercise.movements.any { it in setOf(Movement.SQUAT, Movement.HINGE) } -> 1
                exercise.coverage != Coverage.ISOLATION -> 2
                else -> 3
            }
        } else selected
        val circuit = if (request.abCircuit) listOf("dead-bug", "reverse-crunch", "bicycle-crunch").mapNotNull(eligible::get) else emptyList()
        if (request.abCircuit && circuit.isEmpty()) gaps.add("Ab circuit: enable clear floor space to include the finisher.")
        val finisher = (1..2).flatMap { round -> circuit.mapIndexed { index, entry ->
            GeneratedExercise(entry.key, 1, 8, 12, if (index == circuit.lastIndex) 60 else 15, "Ab circuit • round $round of 2", round)
        } }
        return GeneratedWorkout(request.session, ordered + finisher, gaps)
    }
    fun uniqueRows(rows: List<GeneratedExercise>): Boolean = rows.map { it.key to it.circuitRound }.distinct().size == rows.size
    fun validateRow(row: GeneratedExercise) {
        val entry = requireNotNull(ExerciseCatalog.find(row.key))
        require(entry.recording in setOf(RecordingMode.LOAD_REPS, RecordingMode.REPS_ONLY))
        require(row.sets in 1..GuidedPlan.MAX_SETS && row.repMin in 1..SetInput.MAX_REPS && row.repMax in row.repMin..SetInput.MAX_REPS && row.restSeconds in 15..600)
        require(row.circuitRound == null || row.circuitRound in 1..2)
    }
    fun loadHint(entry: CatalogExercise): String = when (entry.load) {
        LoadConvention.PER_HAND -> "Weight per hand"
        LoadConvention.SINGLE_IMPLEMENT -> "Weight of one implement"
        LoadConvention.TOTAL_EXTERNAL -> "Total weight, including bar"
        LoadConvention.MACHINE_DISPLAY -> "Machine display weight; keep the same machine"
        LoadConvention.BODYWEIGHT_ONLY -> "Bodyweight: enter 0 for weight"
    } + if (entry.unilateral) " • reps per side; perform both sides" else ""
}
