package com.gayathrini.chatapp.ui.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
private val dateFormatter = DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault())

/**
 * Human presence label for the chat top bar / contact profile (TDD §6.5). Returns `null` when
 * there's nothing to show (peer hides last-seen, or has never been seen).
 */
fun presenceLabel(online: Boolean, lastSeenAt: Instant?, now: Instant = Instant.now()): String? {
    if (online) return "online"
    if (lastSeenAt == null) return null

    val minutes = ChronoUnit.MINUTES.between(lastSeenAt, now)
    val time = timeFormatter.format(lastSeenAt)
    val today = LocalDate.now(ZoneId.systemDefault())
    val seenDay = lastSeenAt.atZone(ZoneId.systemDefault()).toLocalDate()
    return when {
        minutes < 1 -> "last seen just now"
        minutes < 60 -> "last seen $minutes min ago"
        seenDay == today -> "last seen today at $time"
        seenDay == today.minusDays(1) -> "last seen yesterday at $time"
        else -> "last seen ${dateFormatter.format(lastSeenAt)}"
    }
}
