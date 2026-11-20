package dev.clean.lift.widgets

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.clean.lift.data.*
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AirForcePage(modifier: Modifier, model: WidgetAttemptModel) {
    var mode by rememberSaveable { mutableStateOf("Quick score") }
    var age by rememberSaveable { mutableStateOf("") }
    var sex by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var waists by rememberSaveable { mutableStateOf(listOf("", "", "")) }
    var strengthName by rememberSaveable { mutableStateOf(AfEvent.PUSH_UP.name) }
    var coreName by rememberSaveable { mutableStateOf(AfEvent.SIT_UP.name) }
    var cardioName by rememberSaveable { mutableStateOf(AfEvent.RUN.name) }
    var strengthText by rememberSaveable { mutableStateOf("") }
    var coreText by rememberSaveable { mutableStateOf("") }
    var cardioText by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(MeasurementInput.today()) }
    var notes by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("90") }
    var variable by rememberSaveable { mutableStateOf("Cardio") }
    var metric by rememberSaveable { mutableStateOf(true) }
    var token by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    // 0 setup; 1 strength; 2 rest; 3 core; 4 rest; 5 cardio; 6 completed; 7 incomplete.
    var stage by rememberSaveable { mutableIntStateOf(0) }
    var firstStart by rememberSaveable { mutableLongStateOf(0L) }
    var eventStart by rememberSaveable { mutableLongStateOf(0L) }
    var eventFinish by rememberSaveable { mutableLongStateOf(0L) }
    var finishedAt by rememberSaveable { mutableLongStateOf(0L) }
    var elapsedEvents by rememberSaveable { mutableStateOf(listOf(0L,0L,0L)) }
    var deviations by rememberSaveable { mutableStateOf("") }
    var shuttle by rememberSaveable { mutableStateOf(listOf(0,0,0,0,0,0)) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val strength = AfEvent.valueOf(strengthName)
    val core = AfEvent.valueOf(coreName)
    val cardio = AfEvent.valueOf(cardioName)
    val clock: WidgetClockModel = viewModel(key = "widgets.airforce.clock")
    val state by clock.state.collectAsStateWithLifecycle()
    val audio: ShuttleAudioModel = viewModel(key = "widgets.airforce.audio")
    val audioState by audio.state.collectAsStateWithLifecycle()
    val operation by model.operation.collectAsStateWithLifecycle()
    fun flag(text: String) { if (!deviations.contains(text)) deviations = (deviations + "\n" + text).trim() }
    val body = remember(age,sex,height,waists) { runCatching {
        val a = requireNotNull(age.toIntOrNull()) { "Enter age (17–120)." }
        require(a in 17..120) { "Age must be 17–120." }
        require(sex in listOf("M","F")) { "Choose M or F for the official scoring table." }
        AirForceScoring.column(a,sex)
        val h = height.toBigDecimalOrNull() ?: error("Enter measured height in inches.")
        require(h in BigDecimal("36")..BigDecimal("100") && h.multiply(BigDecimal(2)).stripTrailingZeros().scale() <= 0) { "Height must be 36–100 inches, recorded to the nearest half inch." }
        val w = AirForceScoring.roundedWaist(waists.map { it.toBigDecimalOrNull() ?: error("Enter all three waist readings (inches).") })
        Triple(a,h,w)
    } }
    fun parse(event: AfEvent, text: String): Int? = if (event.unit == "seconds") {
        if (text.trim() == "0:00") 0 else WidgetCalculations.duration(text)?.toInt()
    } else text.toIntOrNull()?.takeIf { it in 0..1000 }
    val outcome = remember(body,sex,strength,core,cardio,strengthText,coreText,cardioText,mode,variable) { runCatching {
        val (a,h,w) = body.getOrThrow()
        fun result(event: AfEvent, text: String, category: String) = parse(event,text)
            ?: if (mode == "Target explorer" && variable == category) 0 else error("Enter ${event.title} result (${if (event.unit=="seconds") "m:ss" else event.unit}).")
        AfInputs(a,sex,h,w,strength,result(strength,strengthText,"Strength"),core,result(core,coreText,"Core"),cardio,result(cardio,cardioText,"Cardio"))
            .also { AirForceScoring.score(it) }
    } }
    val input = outcome.getOrNull()
    val score = input?.let(AirForceScoring::score)
    val event = when(stage) { 1 -> strength; 3 -> core; else -> cardio }
    val cap = if (event == AfEvent.PLANK && body.isSuccess) AirForceScoring.thresholds(event,body.getOrThrow().first,sex).first().first
        else event.seconds ?: 10800
    val plan = remember(event,cap,stage) { TimerPlan(listOf(TimerPhase("${event.title} • event $stage",cap*1000L)),1) }
    if (mode == "Run practice test") VisibleWidgetClock(clock)
    LaunchedEffect(stage,event,cap) {
        if (stage in listOf(1,3,5) && event != AfEvent.HAMR) clock.configure(plan)
        if (stage == 5 && event == AfEvent.HAMR) audio.load("HAMR")
    }
    LaunchedEffect(mode,stage) { if (mode == "Run practice test" && stage in 1..5) while (true) { now = SystemClock.elapsedRealtime(); delay(200) } }
    LaunchedEffect(state.interrupted,audioState.interrupted) {
        if (mode == "Run practice test" && stage > 0 && (clock.state.value.interrupted || stage==5 && audio.state.value.interrupted)) flag("Timing/audio interrupted.")
    }
    fun currentShuttle() = ShuttleProgress(shuttle[0],shuttle[1],shuttle[2],shuttle[3],shuttle[4],shuttle[5]==1)
    fun endEvent() {
        clock.pause(); audio.stop()
        val stopped = clock.state.value
        if (stopped.interrupted || event==AfEvent.HAMR && audio.state.value.interrupted) flag("Timing/audio interrupted.")
        val elapsed = if (event == AfEvent.HAMR) audio.state.value.position.toLong() else stopped.elapsed
        val actualFinish = if (elapsed >= plan.totalMillis && event != AfEvent.HAMR && deviations.isBlank() && !stopped.interrupted)
            eventStart + plan.totalMillis else SystemClock.elapsedRealtime()
        if (event == AfEvent.HAMR) cardioText = currentShuttle().lastSuccess.toString()
        else {
            val text = if (event.unit == "seconds") HealthCalculations.timeText((elapsed/1000.0).roundToInt()) else stopped.tally.toString()
            when(stage) { 1 -> strengthText=text; 3 -> coreText=text; 5 -> cardioText=text }
        }
        elapsedEvents = elapsedEvents.toMutableList().also { it[(stage-1)/2] = elapsed }
        eventFinish = actualFinish; eventStart = 0; now = SystemClock.elapsedRealtime()
        if (stage == 5) {
            finishedAt = actualFinish
            if (!AirForcePracticeRules.withinWindow(firstStart,actualFinish)) flag("Physical components exceeded the three-hour window, or the device clock restarted.")
            stage = 6
        } else stage++
    }
    LaunchedEffect(state.elapsed,stage) { if (mode == "Run practice test" && stage in listOf(1,3,5) && event != AfEvent.HAMR && clock.state.value.elapsed >= plan.totalMillis) endEvent() }
    LaunchedEffect(audioState.playing,stage) { if(stage==5 && cardio==AfEvent.HAMR && audioState.playing && eventStart==0L) eventStart=SystemClock.elapsedRealtime() }
    LaunchedEffect(audioState.ended,stage) { if(stage==5 && cardio==AfEvent.HAMR && audioState.ended && eventStart>0) endEvent() }
    val configEnabled = stage == 0 && !operation.busy
    val day = MeasurementInput.parseDay(date)
    val finalDeviations = listOf(deviations,if (stage==7) "Incomplete practice test." else "").filter { it.isNotBlank() }.joinToString("\n")
    fun newAttempt() {
        clock.pause(); clock.reset(); audio.stop(); stage=0; firstStart=0; eventStart=0; eventFinish=0; finishedAt=0
        elapsedEvents=listOf(0L,0L,0L); deviations=""; shuttle=listOf(0,0,0,0,0,0)
        strengthText=""; coreText=""; cardioText=""; token=UUID.randomUUID().toString(); model.clear()
    }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("2026 standard PFRA • practice only", style = MaterialTheme.typography.titleMedium)
            Text(WidgetGuides.forProtocol(WidgetProtocol.AIR_FORCE).protocol) }
        item { WidgetChoices(listOf("Quick score","Run practice test","Target explorer"),mode,enabled=configEnabled) { newAttempt(); mode=it } }
        if(stage==0) {
        item { WidgetNumberField(age,{age=it},"Age on test date (17–120)",whole=true,enabled=configEnabled) }
        item { Text("Choose the official scoring table; profile settings are not applied automatically."); WidgetChoices(listOf("M","F"),sex,enabled=configEnabled) {sex=it} }
        item { Text("Body composition first",style=MaterialTheme.typography.titleMedium)
            Text("Height without shoes, nearest half inch. Waist: bare skin, right side, midpoint between lowest rib and top of hip bone, normal exhale, non-stretch tape without compression. Enter three closest readings within 1 inch. Each is rounded down to half an inch, then the average is rounded down again.") }
        item { WidgetNumberField(height,{height=it},"Height (inches; nearest ½ inch)",enabled=configEnabled) }
        waists.forEachIndexed { i,v -> item { WidgetNumberField(v,{waists=waists.toMutableList().also { values -> values[i]=it }},"Waist reading ${i+1} (inches)",enabled=configEnabled) } }
        item { if (body.isSuccess) {
            val (_,h,w)=body.getOrThrow()
            val ratio=AirForceScoring.ratioHundredths(w,h)
            Text("Recorded waist: $w in • raw WHtR: %.4f • scoring WHtR: %.2f • %s/20 points".format(java.util.Locale.US,w.divide(h,8,java.math.RoundingMode.HALF_UP).toDouble(),ratio/100.0,AirForceScoring.bodyPoints(ratio)))
        } else Text(body.exceptionOrNull()?.message ?: "Check body measurements.",color=MaterialTheme.colorScheme.error) }
        item { AfEventChoices(listOf(AfEvent.PUSH_UP,AfEvent.HAND_RELEASE),strength,configEnabled) { strengthName=it.name; strengthText="" } }
        item { AfEventChoices(listOf(AfEvent.SIT_UP,AfEvent.CROSS_LEG,AfEvent.PLANK),core,configEnabled) { coreName=it.name; coreText="" } }
        item { AfEventChoices(listOf(AfEvent.RUN,AfEvent.HAMR),cardio,configEnabled) { cardioName=it.name; cardioText="" } }
        } else item {
            Text("Age $age • $sex table • height $height in • recorded waist ${body.getOrNull()?.third} in")
            Text("${strength.title} → ${core.title} → ${cardio.title}")
        }
        if(mode=="Run practice test") {
            item { Text("Physical components • order and recovery",style=MaterialTheme.typography.titleMedium) }
            if(stage==0) item { Button(onClick={newAttempt();stage=1},enabled=body.isSuccess && !operation.busy) {Text("Body recorded • prepare strength event")} }
            if(stage in listOf(2,4)) item {
                val remaining=(AirForcePracticeRules.MIN_REST_MILLIS-(now-eventFinish)).coerceAtLeast(0)
                Text("Minimum rest: ${WidgetTimerPlans.clockText(remaining+999)} remaining",style=MaterialTheme.typography.headlineSmall)
                Text("Recovery starts at the actual event finish, including while you enter results. Begin the next component when ready after five minutes.")
                Button(onClick={if(stage==4 && cardio==AfEvent.HAMR) audio.restart("HAMR");stage++;eventStart=0},enabled=AirForcePracticeRules.canStartNext(eventFinish,now)) {Text(if(stage==2) "Prepare core event" else "Prepare cardio event")}
            }
            if(stage in listOf(1,3,5)) {
                item { Text(event.title,style=MaterialTheme.typography.titleLarge); Text(afEventGuide(event)) }
                if(event==AfEvent.HAMR) item {
                    LocalShuttleAudio(audio,"HAMR",keepAwake=false,onRecordingChanged={
                        if(eventStart>0) flag("Recording changed during cardio.")
                        shuttle=listOf(0,0,0,0,0,0)
                    })
                    Text("Attempted: ${currentShuttle().attempts}; last successful shuttle: ${currentShuttle().lastSuccess}; consecutive warnings: ${currentShuttle().consecutive}/3")
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf(true,false).forEach { reached ->
                        OutlinedButton(onClick={
                            val next=currentShuttle().judge(true,reached)
                            shuttle=listOf(next.attempts,next.successful,next.lastSuccess,next.misses,next.consecutive,if(next.ended) 1 else 0)
                            if(next.ended) endEvent()
                        },enabled=audioState.playing && !currentShuttle().ended && currentShuttle().attempts<1000) {Text(if(reached) "Reached line" else "Warning")}
                    } }
                } else item {
                    Text(WidgetTimerPlans.clockText(if(event==AfEvent.RUN) state.elapsed else plan.at(state.elapsed).remaining+999),style=MaterialTheme.typography.displayMedium)
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Button(onClick={
                            if(state.running) {clock.pause();flag("Paused a physical component.")}
                            else { if(eventStart==0L) {eventStart=SystemClock.elapsedRealtime();if(firstStart==0L) firstStart=eventStart};clock.start() }
                        },enabled=state.elapsed<plan.totalMillis) {Text(if(state.running) "Pause" else if(state.elapsed==0L) "Start event" else "Resume")}
                        OutlinedButton(onClick=::endEvent,enabled=eventStart>0 && state.elapsed>0) {Text("End event")}
                    }
                    if(event.unit!="seconds") Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("Valid reps: ${state.tally}")
                        TextButton(onClick={clock.tally(1)},enabled=state.running) {Text("+1")}
                        TextButton(onClick={clock.tally(-1)},enabled=state.running && state.tally>0) {Text("−1")}
                    }
                    Text("Observer announces elapsed 15-second tones; count correct repetitions/form only. End early on a terminating form violation.",style=MaterialTheme.typography.bodySmall)
                }
            }
            if(stage in 1..5) item { TextButton(onClick={clock.pause();audio.stop();flag("Practice ended before all components completed.");stage=7}) {Text("End incomplete practice")} }
            if(stage>=6) item { Text(if(stage==6) "Practice completed" else "Incomplete practice",style=MaterialTheme.typography.titleLarge)
                if(stage==6 && cardio==AfEvent.RUN) Text("Complete approximately five minutes of cooldown after the run.") }
            if(firstStart>0) item { Text("Physical window: ${WidgetTimerPlans.clockText((if(finishedAt>0) finishedAt else now)-firstStart)} / 3:00:00") }
        }
        if(mode!="Run practice test" || stage in listOf(2,4,6,7)) {
            item { AfResultField(strength,strengthText) {strengthText=it} }
            item { AfResultField(core,coreText) {coreText=it} }
            item { AfResultField(cardio,cardioText) {cardioText=it} }
        }
        if(mode=="Target explorer") {
            item { Text("Hypothetical • other components stay fixed. Choose one component to vary."); WidgetChoices(listOf("Strength","Core","Cardio"),variable) {variable=it} }
            item { WidgetNumberField(target,{target=it},"Target composite (75–100)") }
            item {
                val result=runCatching { AirForceScoring.explore(input ?: error("Enter the fixed components."),variable,target.toDoubleOrNull() ?: error("Enter target score.")) }
                val proposed=result.getOrNull()
                if(result.isFailure) Text(result.exceptionOrNull()?.message.orEmpty(),color=MaterialTheme.colorScheme.error)
                else if(proposed==null) Text("No result for this component reaches the target with the other results locked and all minima met.")
                else {
                    val choice=when(variable){"Strength"->strength;"Core"->core;else->cardio}
                    Text("$variable: ${if(choice.unit=="seconds") HealthCalculations.timeText(proposed.first) else proposed.first} ${if(choice.unit=="seconds") "" else choice.unit} → ${proposed.second.total}/100. Least demanding qualifying table threshold; not a training prediction.")
                }
            }
        } else if(mode=="Quick score" || stage>=6) {
            item { if(score!=null && stage!=7) {
                Text("${score.total}/100",style=MaterialTheme.typography.headlineLarge)
                Text("Body ${score.body}/20 • strength ${score.strength}/15 • core ${score.core}/15 • cardio ${score.cardio}/50")
                Text(if(score.passes) "Meets 75-point and physical-component minimums in this practice calculation." else "Does not meet 75 points and/or a physical-component minimum in this calculation.")
                if(!score.minimaMet) Text("Failed minima: ${listOfNotNull(if(score.strength==0.0) "strength" else null,if(score.core==0.0) "core" else null,if(score.cardio==0.0) "cardio" else null).joinToString()}")
                if(score.ratioHundredths>55 && !score.passes) Text("A secondary body-fat assessment may affect an official result; this tool scores primary WHtR only.")
            } else Text(if(stage==7) "Incomplete: no composite score." else outcome.exceptionOrNull()?.message.orEmpty(),color=MaterialTheme.colorScheme.error) }
            item { AttemptDateField(date) {date=it} }
            item { OutlinedTextField(notes,{notes=it.take(1000)},label={Text("Course / conditions / deviations / notes")},modifier=Modifier.fillMaxWidth()) }
            item { Row {Checkbox(metric,{metric=it});Text("Also save raw WHtR snapshot to Progress")} }
            item {
                val complete=mode=="Quick score" || stage==6
                val maySave=day!=null && body.isSuccess && (stage==7 || score!=null)
                Button(onClick={
                    val (a,h,w)=body.getOrThrow()
                    val details="${WidgetProtocol.AIR_FORCE.version}\nAge $a; scoring sex $sex; height $h in; waist readings ${waists.joinToString()} in; recorded waist $w in; raw WHtR ${w.divide(h,8,java.math.RoundingMode.HALF_UP)}; scored ratio ${AirForceScoring.ratioHundredths(w,h)/100.0}.\n${strength.title}: $strengthText; ${core.title}: $coreText; ${cardio.title}: $cardioText.\nComponent points: ${score?.body}, ${score?.strength}, ${score?.core}, ${score?.cardio}; minima met ${score?.minimaMet}.\nGuided event elapsed ms: ${elapsedEvents.joinToString()}; physical window ms: ${if(finishedAt>0) finishedAt-firstStart else "incomplete/entered"}.\n$notes"
                    val record=WidgetAttempt(token=token,protocol=WidgetProtocol.AIR_FORCE.name,version=WidgetProtocol.AIR_FORCE.version,epochDay=day!!,result=if(complete) score!!.total else null,
                        complete=complete,mode=if(mode=="Quick score") "Entered" else "Guided",details=details,deviations=finalDeviations)
                    val health=if(metric) HealthRecord(kind=HealthKind.WAIST_HEIGHT,epochDay=day,result=0.0,
                        heightCm=h.toDouble()*2.54,waistCm=w.toDouble()*2.54,age=a,gender=sex,notes="Widgets • Air Force 2026 primary midpoint WHtR • $token\nScoring truncates separately; raw ratio stored.\n$notes".take(1000)) else null
                    model.save(record,health)
                },enabled=maySave && !operation.busy && operation.savedToken!=token) {Text(if(stage==7) "Save incomplete practice" else "Save practice result to Progress")}
                Text(operation.message)
            }
        }
        if(finalDeviations.isNotBlank()) item {Text(finalDeviations,color=MaterialTheme.colorScheme.error)}
        if(stage>=6 || operation.savedToken==token) item {TextButton(onClick=::newAttempt,enabled=!operation.busy) {Text("New attempt")}}
        item { WidgetSourceLink("Official Air Force manual • 24 March 2026 • §§3.7, 3.15, Attachment 2",AirForceScoring.MANUAL_URL)
            WidgetSourceLink("Official AFPC scoring charts • pages 1–8",AirForceScoring.CHART_URL)
            WidgetSourceLink("AFPC • current fitness resources",AirForceScoring.RESOURCES_URL) }
    }
}

@Composable
private fun AfEventChoices(options: List<AfEvent>,selected: AfEvent,enabled: Boolean,onSelect:(AfEvent)->Unit) {
    WidgetChoices(options.map {it.title},selected.title,enabled) {title->onSelect(options.first {it.title==title})}
}
@Composable
private fun AfResultField(event: AfEvent,value: String,onChange:(String)->Unit) {
    if(event.unit=="seconds") OutlinedTextField(value,{onChange(it.take(12))},label={Text("${event.title} (m:ss; 0:00 if none)")},singleLine=true,modifier=Modifier.fillMaxWidth())
    else WidgetNumberField(value,onChange,"${event.title} (${event.unit}, 0–1000)",whole=true)
}
private fun afEventGuide(event: AfEvent): String = when(event) {
    AfEvent.PUSH_UP -> "One minute. Rigid head-to-heels body, hands slightly wider than shoulders, feet at most 12 inches apart. Lower upper arms at least parallel to floor; return with arms extended. Rest only up; chest may touch but not rest or bounce. Down-position rest ends the event."
    AfEvent.HAND_RELEASE -> "Two minutes. Index fingers inside shoulder edges. Lower chest, hips and thighs together; extend both arms out fully at shoulder level with hands off the floor; bring hands back under shoulders and push the whole body up. Rest only up; down-position rest ends the event."
    AfEvent.SIT_UP -> "One minute. Knees at 90°, heels on floor, hands/fingers on shoulders or upper chest. Elbows touch knees/thighs at top, shoulder blades touch floor at bottom. Keep buttocks/heels grounded. Rest only up without support from knees/objects. An assessor may hold feet; see manual for authorized anchoring."
    AfEvent.CROSS_LEG -> "Two minutes. Lie on back, hands/fingers touching shoulders or upper chest. Keep lower back on floor. Bring legs toward chest and left elbow to right thigh, then fully extend legs; alternate right elbow to left thigh. Each completed side is one rep. Rest in the starting position."
    AfEvent.PLANK -> "Elbows below shoulders at 90°, fists or flat hands (not clasped), feet hip-width. Keep straight head-to-heels alignment. Only hands, forearms and toes contact floor. End on lost form/contact or third correction. Timer stops at this age/sex table's maximum; time is recorded to nearest second."
    AfEvent.RUN -> "Certified 2.0-mile measured course; below 5250 ft for this scoring tool. Up to three-minute warm-up before starting. No physical assistance; stay on course. End at finish line, then cool down approximately five minutes. Leaving the course terminates the assessment."
    AfEvent.HAMR -> "Approved Air Force recording, flat dry certified 20 m course. Up to three-minute warm-up. Reach each far line before its beep; wait if early. Three consecutive warnings end the test; success resets warnings. Cutting the course or skipping/resting ends it. Last successful shuttle number is the result."
}
