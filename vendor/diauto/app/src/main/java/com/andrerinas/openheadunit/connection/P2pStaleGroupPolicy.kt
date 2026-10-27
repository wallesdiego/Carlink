// CarLink: new file (see docs/VENDOR_CHANGES.md).
package com.andrerinas.openheadunit.connection

/**
 * What to do with a Wi-Fi Direct group that is up on this unit but is not the one this session
 * asked for.
 *
 * DiAuto refused every such group ("Wi-Fi Direct is in use by another app") so that it would never
 * tear down another app's live projection. On head units that is far too strict: a persistent group
 * left by an earlier app (open-headunit, a previous DiAuto install, the stereo's own phone-link app)
 * or re-invoked by wpa_supplicant at boot sits there with nobody on it, and every wireless Android
 * Auto attempt failed before it started (seen on a Topway head unit where open-headunit, which just
 * removes the old group, connected). A group this unit owns with no client on it carries nothing,
 * so removing it costs nobody anything; only a group with a phone on it, or one this unit joined as
 * a client, is someone's live link and stays refused.
 */
internal object P2pStaleGroupPolicy {

    enum class Verdict {
        /** Ours: handle it as before. */
        OURS,

        /** Not ours and idle: remove it (or, before our create, wait for our create to replace it). */
        STALE,

        /** Not ours and in use: leave it alone and report it. */
        IN_USE,
    }

    fun judge(ours: Boolean, isGroupOwner: Boolean, clientCount: Int): Verdict = when {
        ours -> Verdict.OURS
        isGroupOwner && clientCount == 0 -> Verdict.STALE
        else -> Verdict.IN_USE
    }
}
