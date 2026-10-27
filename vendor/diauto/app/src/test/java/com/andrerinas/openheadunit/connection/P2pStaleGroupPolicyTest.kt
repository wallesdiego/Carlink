// CarLink: new file (see docs/VENDOR_CHANGES.md).
package com.andrerinas.openheadunit.connection

import com.andrerinas.openheadunit.connection.P2pStaleGroupPolicy.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

class P2pStaleGroupPolicyTest {
    @Test fun `our own group is handled as ours whatever its clients`() {
        assertEquals(Verdict.OURS, P2pStaleGroupPolicy.judge(ours = true, isGroupOwner = true, clientCount = 0))
        assertEquals(Verdict.OURS, P2pStaleGroupPolicy.judge(ours = true, isGroupOwner = true, clientCount = 1))
    }

    @Test fun `an idle group left by another app is stale and may be removed`() {
        // Topway head unit: a persistent group from open-headunit or the stereo, nobody on it.
        assertEquals(Verdict.STALE, P2pStaleGroupPolicy.judge(ours = false, isGroupOwner = true, clientCount = 0))
    }

    @Test fun `a group with a phone on it stays another app's`() {
        assertEquals(Verdict.IN_USE, P2pStaleGroupPolicy.judge(ours = false, isGroupOwner = true, clientCount = 1))
    }

    @Test fun `a group this unit joined as a client stays another app's`() {
        assertEquals(Verdict.IN_USE, P2pStaleGroupPolicy.judge(ours = false, isGroupOwner = false, clientCount = 0))
    }
}
