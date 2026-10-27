// CarLink: new file (see docs/VENDOR_CHANGES.md).
package com.andrerinas.openheadunit.aap

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandsFreeWakeEscalationTest {
    private val t0 = 100_000L

    @Test fun `never on the first stand-down`() {
        assertFalse(HandsFreeWakeEscalation.shouldEscalate(false, t0, 0L, 0, t0))
        assertFalse(HandsFreeWakeEscalation.shouldEscalate(false, 0L, 0L, 0, t0))
    }

    @Test fun `wakes once the phone has stayed silent long enough`() {
        // Topway log: stand-down 2.3 s into the session, CarLink's first nudge at 10 s.
        assertTrue(HandsFreeWakeEscalation.shouldEscalate(false, t0, 0L, 0, t0 + 7_700L))
    }

    @Test fun `spaced by the cooldown and bounded by the budget`() {
        val first = t0 + 9_000L
        assertFalse(HandsFreeWakeEscalation.shouldEscalate(false, t0, first, 1, first + 5_000L))
        assertTrue(HandsFreeWakeEscalation.shouldEscalate(false, t0, first, 1, first + HandsFreeWakeEscalation.COOLDOWN_MS))
        assertFalse(HandsFreeWakeEscalation.shouldEscalate(false, t0, first, HandsFreeWakeEscalation.MAX_WAKES, first + 60_000L))
    }

    @Test fun `nothing touches the radio during a session`() {
        assertFalse(HandsFreeWakeEscalation.shouldEscalate(true, t0, 0L, 0, t0 + 60_000L))
    }

    @Test fun `cycles the radio once, only below API 33`() {
        assertTrue(HandsFreeWakeEscalation.shouldCycleRadio(27, false, t0, 0, t0 + 7_700L))
        assertFalse(HandsFreeWakeEscalation.shouldCycleRadio(27, false, t0, 0, t0 + 2_000L))
        assertFalse(HandsFreeWakeEscalation.shouldCycleRadio(27, false, t0, 1, t0 + 60_000L))
        assertFalse(HandsFreeWakeEscalation.shouldCycleRadio(33, false, t0, 0, t0 + 60_000L))
        assertFalse(HandsFreeWakeEscalation.shouldCycleRadio(27, true, t0, 0, t0 + 60_000L))
    }

    @Test fun `pokes stay quiet while the cycle hands the link back`() {
        assertTrue(HandsFreeWakeEscalation.quietAfterCycle(true, 0L, t0))
        assertTrue(HandsFreeWakeEscalation.quietAfterCycle(false, t0 + 1_000L, t0))
        assertFalse(HandsFreeWakeEscalation.quietAfterCycle(false, t0, t0 + 1L))
        assertFalse(HandsFreeWakeEscalation.quietAfterCycle(false, 0L, t0))
    }
}
