// CarLink: new file, after open-headunit c3b58e6 (HandsFreeWakeEscalationPolicy); see docs/VENDOR_CHANGES.md.
package com.andrerinas.openheadunit.aap

/**
 * Whether a wake poke that [BluetoothWakePolicy.shouldPoke] stood down (this head unit already
 * holds a hands-free link) may go out anyway.
 *
 * Android Auto starts wireless setup from a Bluetooth *event* (ACL connected, hands-free connection
 * state changed), not from a connected state. A car head unit whose own stack connected hands-free
 * long before CarLink opened its Android Auto listener raises no further event, so the phone never
 * opens the Android Auto channel: seen on a Topway head unit (Android 8.1), where the phone joined
 * the Wi-Fi Direct group again and again but never opened RFCOMM, and CarLink gave up after 30 s.
 *
 * open-headunit waits 90 s and needs a pairing that has run Android Auto before. In CarLink every
 * session is one the driver asked for (Connect, Try again, or CarLink's own reconnect of the last
 * Android Auto phone) and CarLink gives up after 30 s, so the wait is [ESCALATE_AFTER_MS], just
 * before CarLink's first nudge, and the budget is [MAX_WAKES] per handshake-server start. The cost
 * is the one upstream measured: the poke takes the phone's hands-free slot, and the head unit's own
 * link may stay down until the phone or the stereo reconnects it.
 *
 * A poke cannot finish the job where the stereo has a hands-free stack of its own: DiAuto then holds
 * the poke silently (it must not compete with the real stack), and Android Auto 17.8 waits ~4.5 s
 * for a *connected* hands-free profile and gives up (seen on the same Topway: HFP-AG and HSP-AG
 * pokes answered, no Android Auto channel). So below API 33, where `BluetoothAdapter.disable()`
 * still works for an app, the first lever is open-headunit's radio cycle (`BluetoothRadioCyclePolicy`,
 * 506bd59/c3b58e6): the unit's Bluetooth goes off and on, its own stack reconnects hands-free with a
 * real Service Level Connection, and the phone sees the event *and* the connected profile. Once per
 * start; pokes then stay quiet for [POST_CYCLE_QUIET_MS] so they do not take back the link the cycle
 * handed over. The escalated wake remains the fallback.
 */
object HandsFreeWakeEscalation {

    /** Escalated wakes allowed between one handshake-server start and the next. */
    const val MAX_WAKES = 2

    /** How long the stand-down runs before the first escalated wake. */
    const val ESCALATE_AFTER_MS = 6_000L

    /** Minimum gap between two escalated wakes, so a phone that is merely slow is not poked twice over. */
    const val COOLDOWN_MS = 15_000L

    /** Last release where `BluetoothAdapter.disable()` does anything for an app; from 33 it is a no-op. */
    const val LAST_CYCLEABLE_SDK = 32

    /** Radio cycles allowed per handshake-server start. A cycle costs every Bluetooth device a few seconds. */
    const val MAX_CYCLES = 1

    /** Bounds on the two adapter transitions, so a radio that never answers cannot hang the loop. */
    const val ADAPTER_OFF_WAIT_MS = 10_000L
    const val ADAPTER_ON_WAIT_MS = 20_000L

    /** After a cycle, how long no poke goes out: the stereo reconnects hands-free and the phone dials back. */
    const val POST_CYCLE_QUIET_MS = 25_000L

    /** Whether to cycle this unit's own Bluetooth instead of poking over the link it holds. */
    fun shouldCycleRadio(
        sdkInt: Int,
        sessionInProgress: Boolean,
        standDownSinceMs: Long,
        cyclesUsed: Int,
        now: Long,
    ): Boolean {
        if (sdkInt > LAST_CYCLEABLE_SDK) return false
        if (sessionInProgress) return false
        if (cyclesUsed >= MAX_CYCLES) return false
        if (standDownSinceMs <= 0L || now - standDownSinceMs < ESCALATE_AFTER_MS) return false
        return true
    }

    /** Whether pokes stand aside because a cycle is running or has just handed the link back. */
    fun quietAfterCycle(cycleInFlight: Boolean, quietUntilMs: Long, now: Long): Boolean =
        cycleInFlight || (quietUntilMs > 0L && now < quietUntilMs)

    fun shouldEscalate(
        sessionInProgress: Boolean,
        standDownSinceMs: Long,
        lastWakeMs: Long,
        wakesUsed: Int,
        now: Long,
    ): Boolean {
        if (sessionInProgress) return false
        if (wakesUsed >= MAX_WAKES) return false
        if (standDownSinceMs <= 0L || now - standDownSinceMs < ESCALATE_AFTER_MS) return false
        if (lastWakeMs > 0L && now - lastWakeMs < COOLDOWN_MS) return false
        return true
    }
}
