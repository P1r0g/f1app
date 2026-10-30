package com.example.f1app

import org.junit.Assert.*
import org.junit.Test

class DriverJsonCodecTest {
    private val oldDriver = """{"driverId":"test","name":"Test","surname":"Driver","number":12,"nationality":"Italy","shortName":"TST","teamId":"mercedes"}"""

    @Test fun `legacy list preserves names but does not invent position or points`() {
        val driver = DriverJsonCodec.decodeDrivers("[$oldDriver]").single()
        assertEquals("Test", driver.name)
        assertNull(driver.position)
        assertNull(driver.points)
    }

    @Test fun `old zero placeholders become unknown values`() {
        val json = oldDriver.dropLast(1) + ",\"position\":0,\"points\":0}"
        val driver = DriverJsonCodec.decodeDrivers("[$json]").single()
        assertNull(driver.position)
        assertNull(driver.points)
    }

    @Test fun `real zero points and fractional points survive round trip`() {
        val drivers = listOf(
            Driver("zero", "Zero", "Points", 1, "", "ZER", "team", 21, 0.0, "Team"),
            Driver("half", "Half", "Points", 2, "", "HAL", "team", 2, 12.5, "Team")
        )
        assertEquals(drivers, DriverJsonCodec.decodeDrivers(DriverJsonCodec.encodeDrivers(drivers)))
    }

    @Test fun `unclassified driver with known zero points remains valid`() {
        val driver = Driver("unknown", "Unknown", "Position", 1, "", "UNK", "team", null, 0.0)
        assertEquals(listOf(driver), DriverJsonCodec.decodeDrivers(DriverJsonCodec.encodeDrivers(listOf(driver))))
    }

    @Test fun `new backup preserves all fields and season`() {
        val snapshot = StoredChampionship(
            listOf(Driver("id", "A", "B", 12, "Italy", "ABC", "mercedes", 1, 365.5, "Mercedes")),
            2026
        )
        assertEquals(snapshot, DriverJsonCodec.decodeBackup(DriverJsonCodec.encodeBackup(snapshot, 123)))
    }

    @Test fun `version one backup can still be restored`() {
        val json = """{"version":1,"domain":"formula1","season":2026,"drivers":[$oldDriver]}"""
        val result = DriverJsonCodec.decodeBackup(json)
        assertEquals(2026, result.season)
        assertEquals("test", result.drivers.single().driverId)
        assertNull(result.drivers.single().points)
    }

    @Test fun `corrupted backup identity and duplicate drivers are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            DriverJsonCodec.decodeBackup("""{"version":9,"domain":"formula1","drivers":[]}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            DriverJsonCodec.decodeBackup("""{"version":2,"domain":"other","drivers":[]}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            DriverJsonCodec.decodeDrivers("[$oldDriver,$oldDriver]")
        }
    }
}
