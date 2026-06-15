package com.gayathrini.chatapp.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class PresenceTest {

    private val now = Instant.parse("2026-06-13T12:00:00Z")

    @Test
    fun online_showsOnline() {
        assertEquals("online", presenceLabel(online = true, lastSeenAt = now, now = now))
    }

    @Test
    fun offline_withNoLastSeen_isNull() {
        assertNull(presenceLabel(online = false, lastSeenAt = null, now = now))
    }

    @Test
    fun offline_recent_showsMinutesAgo() {
        val tenMinAgo = now.minus(10, ChronoUnit.MINUTES)
        assertEquals("last seen 10 min ago", presenceLabel(online = false, lastSeenAt = tenMinAgo, now = now))
    }

    @Test
    fun offline_underAMinute_showsJustNow() {
        val seconds = now.minus(30, ChronoUnit.SECONDS)
        assertEquals("last seen just now", presenceLabel(online = false, lastSeenAt = seconds, now = now))
    }
}
