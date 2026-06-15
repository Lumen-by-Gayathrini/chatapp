package com.gayathrini.chatapp.data.presence

/** Foreground presence heartbeat (TDD §6.5): stamps the signed-in user's `lastSeenAt`. */
interface PresenceRepository {
    /** Send a heartbeat if signed in; a no-op (and never throws) otherwise. */
    suspend fun heartbeat()
}
