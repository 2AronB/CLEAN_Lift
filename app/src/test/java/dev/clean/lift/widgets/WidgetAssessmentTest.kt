package dev.clean.lift.widgets

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class WidgetAssessmentTest {
    private fun input() = AfInputs(24,"M",BigDecimal("72"),BigDecimal("35"),AfEvent.PUSH_UP,67,AfEvent.PLANK,220,AfEvent.RUN,805)
    @Test fun officialTopAndMinimumExamples() {
        assertEquals(100.0, AirForceScoring.score(input()).total, 0.0)
        assertEquals(50.0, AirForceScoring.points(AfEvent.RUN,24,"M",805), 0.0)
        assertEquals(49.5, AirForceScoring.points(AfEvent.RUN,24,"M",806), 0.0)
        assertEquals(35.0, AirForceScoring.points(AfEvent.RUN,24,"M",1185), 0.0)
        assertEquals(0.0, AirForceScoring.points(AfEvent.RUN,24,"M",1186), 0.0)
        assertEquals(15.0, AirForceScoring.points(AfEvent.PUSH_UP,24,"F",50), 0.0)
        assertEquals(15.0, AirForceScoring.points(AfEvent.PLANK,60,"F",175), 0.0)
        assertEquals(35.0, AirForceScoring.points(AfEvent.HAMR,60,"F",11), 0.0)
        val minimum = input().copy(strengthResult=30,coreResult=95,cardioResult=1185)
        assertEquals(60.0, AirForceScoring.score(minimum).total, 0.0)
        assertFalse(AirForceScoring.score(minimum).passes)
        assertFalse(AirForceScoring.score(input().copy(strengthResult=29)).passes)
    }
    @Test fun allAgeSexColumnTransitionsAndComponentMinima() {
        val ages = listOf(17,25,30,35,40,45,50,55,60)
        ages.forEachIndexed { band, age -> listOf("M","F").forEachIndexed { sexIndex, sex ->
            assertEquals(band*2+sexIndex,AirForceScoring.column(age,sex))
            assertEquals(band*2+sexIndex,AirForceScoring.column(if(band==8) 100 else ages.getOrElse(band+1){120}-1,sex))
            AfEvent.entries.forEach { event ->
                val bounds = AirForceScoring.thresholds(event,age,sex)
                val minimum = bounds.last().first
                assertTrue(AirForceScoring.points(event,age,sex,minimum) > 0)
                assertEquals(0.0,AirForceScoring.points(event,age,sex,minimum + if(event==AfEvent.RUN) 1 else -1),0.0)
                assertEquals(if(event in listOf(AfEvent.RUN,AfEvent.HAMR)) 50.0 else 15.0,AirForceScoring.points(event,age,sex,bounds.first().first),0.0)
            }
        } }
        assertTrue(runCatching { AirForceScoring.column(16,"M") }.isFailure)
        assertTrue(runCatching { AirForceScoring.column(30,"") }.isFailure)
    }
    @Test fun waistUsesProtocolRoundingAndTruncationNotBinaryRounding() {
        assertEquals(BigDecimal("37.5"),AirForceScoring.roundedWaist(listOf("37.9","38.1","38.2").map(::BigDecimal)))
        assertEquals(54,AirForceScoring.ratioHundredths(BigDecimal("39.5"),BigDecimal("72")))
        assertEquals(15.0,AirForceScoring.bodyPoints(54),0.0)
        assertEquals(12.5,AirForceScoring.bodyPoints(55),0.0)
        assertEquals(0.0,AirForceScoring.bodyPoints(60),0.0)
        assertEquals(59,AirForceScoring.ratioHundredths(BigDecimal("42.48"),BigDecimal("72")))
        assertTrue(runCatching { AirForceScoring.roundedWaist(listOf("30","32","32").map(::BigDecimal)) }.isFailure)
    }
    @Test fun targetExplorerFindsRealThresholdAndCannotHideFailedLockedComponent() {
        val result = AirForceScoring.explore(input(),"Strength",90.0)!!
        assertEquals(37,result.first); assertEquals(90.0,result.second.total,0.0)
        assertEquals(52,AirForceScoring.explore(input(),"Strength",95.0)!!.first)
        assertEquals(67,AirForceScoring.explore(input(),"Strength",100.0)!!.first)
        assertNull(AirForceScoring.explore(input().copy(coreResult=0),"Strength",75.0))
    }
    @Test fun prescribedRestAndWindowUseActualFinish() {
        assertFalse(AirForcePracticeRules.canStartNext(1000,300999))
        assertTrue(AirForcePracticeRules.canStartNext(1000,301000))
        assertTrue(AirForcePracticeRules.withinWindow(1000,10801000))
        assertFalse(AirForcePracticeRules.withinWindow(1000,10801001))
    }
    @Test fun hamrWarningsResetButPacerMissesDoNot() {
        var hamr = ShuttleProgress().judge(true,false).judge(true,false).judge(true,true)
        assertEquals(0,hamr.consecutive); assertEquals(3,hamr.lastSuccess)
        hamr = hamr.judge(true,false).judge(true,false).judge(true,false)
        assertTrue(hamr.ended); assertEquals(3,hamr.lastSuccess)
        val pacer = ShuttleProgress().judge(false,false).judge(false,true).judge(false,false)
        assertTrue(pacer.ended); assertEquals(2,pacer.misses)
        assertTrue(runCatching { pacer.judge(false,true) }.isFailure)
    }
    @Test fun metricProgressExcludesPartialInterruptedAndOtherVersions() {
        val p = WidgetProtocol.PACER
        val raw = WidgetAttempt(id=1,token="one",protocol=p.name,version=p.version,epochDay=19000,result=20.0,complete=true,mode="Entered",details="20 m")
        val records = listOf(raw,raw.copy(id=2,complete=false),raw.copy(id=3,deviations="Interrupted"),raw.copy(id=4,version="different"))
        assertEquals(listOf(1L),WidgetAttemptRules.series(records).first { it.widgetProtocol==p.name }.points.map { it.recordId })
        assertTrue(runCatching { WidgetAttemptRules.validate(raw.copy(id=0,result=Double.NaN)) }.isFailure)
        assertTrue(runCatching { WidgetAttemptRules.validate(raw.copy(id=0,result=20.5)) }.isFailure)
    }
    @Test fun cardioRawChartsMergeLegacyAndWidgetResultsWithoutDuplicates() {
        val p=WidgetProtocol.COOPER
        val a=WidgetAttempt(id=7,token="one",protocol=p.name,version=p.version,epochDay=19000,result=2500.0,complete=true,mode="Guided",details="track",healthRecordId=3)
        val b=a.copy(id=8,token="two",result=450.0,healthRecordId=null)
        val legacy=dev.clean.lift.data.MetricSeries("raw.COOPER","12-minute run distance","m","Cardio",
            listOf(dev.clean.lift.data.MetricPoint(18900,2000.0,1),dev.clean.lift.data.MetricPoint(19000,2500.0,3)))
        val combined=WidgetAttemptRules.integrate(listOf(legacy),listOf(a,b))
        assertEquals(listOf(1L,-7L,-8L),combined.first().points.map {it.recordId})
        assertEquals(listOf(2000.0,2500.0,450.0),combined.first().points.map {it.value})
        assertFalse(combined.any {it.key=="widget.COOPER"})
        // Deleting an estimate retains the original measured widget result.
        assertEquals(listOf(1L,-7L,-8L),WidgetAttemptRules.integrate(listOf(legacy.copy(points=legacy.points.take(1))),listOf(a.copy(healthRecordId=null),b)).first().points.map {it.recordId})
    }
    @Test fun rockportRawMergeKeepsExistingMinuteUnits() {
        val p=WidgetProtocol.ROCKPORT
        val a=WidgetAttempt(id=2,token="walk",protocol=p.name,version=p.version,epochDay=19000,result=900.0,complete=true,mode="Entered",details="track")
        val legacy=dev.clean.lift.data.MetricSeries("raw.ROCKPORT","One-mile walk time","minutes","Cardio",emptyList())
        assertEquals(15.0,WidgetAttemptRules.integrate(listOf(legacy),listOf(a)).first().points.single().value,0.0)
    }
}
