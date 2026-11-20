package dev.clean.lift.widgets

import org.junit.Assert.*
import org.junit.Test

class WidgetEquipmentTest {
    @Test fun legacyInventoryIsPreservedOnlyAtHome() {
        val old = mapOf("plates-LB" to "45:4|25:2", "bar-LB" to "35")
        assertEquals(WidgetEquipment("35", listOf("45:4", "25:2")), readWidgetEquipment(old, WidgetLocation.HOME, LoadUnit.LB))
        val gym = readWidgetEquipment(old, WidgetLocation.GYM, LoadUnit.LB)
        assertEquals("45", gym.bar)
        assertTrue(parseStock(gym.rows)!!.all { it.count == 0 })
    }
    @Test fun locationAndUnitProfilesAreIndependent() {
        val values = mapOf("bar-HOME-LB" to "35", "plates-HOME-LB" to "10:2", "bar-GYM-LB" to "45", "plates-GYM-LB" to "45:8",
            "bar-HOME-KG" to "15", "plates-HOME-KG" to "5:4", "bar-GYM-KG" to "20", "plates-GYM-KG" to "20:6")
        assertEquals(listOf("10:2"), readWidgetEquipment(values, WidgetLocation.HOME, LoadUnit.LB).rows)
        assertEquals(listOf("45:8"), readWidgetEquipment(values, WidgetLocation.GYM, LoadUnit.LB).rows)
        assertEquals("15", readWidgetEquipment(values, WidgetLocation.HOME, LoadUnit.KG).bar)
        assertEquals("20", readWidgetEquipment(values, WidgetLocation.GYM, LoadUnit.KG).bar)
    }
    @Test fun newHomeKeysOverrideLegacyWithoutChangingIt() {
        val values = mapOf("plates-LB" to "45:4", "bar-LB" to "45", "plates-HOME-LB" to "10:2", "bar-HOME-LB" to "35")
        assertEquals(WidgetEquipment("35", listOf("10:2")), readWidgetEquipment(values, WidgetLocation.HOME, LoadUnit.LB))
        assertEquals("45:4", values["plates-LB"])
    }
    @Test fun savedEmptyInventoryNeverResurrectsLegacyPlates() {
        val values = mapOf("plates-LB" to "45:4", "plates-HOME-LB" to "", "bar-HOME-LB" to "0")
        assertEquals(WidgetEquipment("0", emptyList()), readWidgetEquipment(values, WidgetLocation.HOME, LoadUnit.LB))
    }
    @Test fun malformedProfileFallsBackWithoutChangingOtherProfiles() {
        val values = mapOf("plates-HOME-LB" to "NaN:2", "bar-HOME-LB" to "-10", "plates-GYM-LB" to "45:4", "bar-GYM-LB" to "45")
        assertEquals(defaultStock(LoadUnit.LB), readWidgetEquipment(values, WidgetLocation.HOME, LoadUnit.LB).rows)
        assertEquals(listOf("45:4"), readWidgetEquipment(values, WidgetLocation.GYM, LoadUnit.LB).rows)
    }
}
