package com.gayathrini.chatapp.ui.conversations

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("d MMM")

/** Short, readable timestamp for the chat list: time if today, otherwise the date. */
fun Instant?.toShortLabel(): String {
    if (this == null) return ""
    val zoned = atZone(ZoneId.systemDefault())
    return if (zoned.toLocalDate() == LocalDate.now()) {
        timeFormatter.format(zoned)
    } else {
        dateFormatter.format(zoned)
    }
}
