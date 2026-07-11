package com.example.memogotchi.api

// --- Nearby / presence ---

data class NearbyTokenResponse(
    val token: String,
    val expiresAtMillis: Long
)

data class ResolveTokenRequest(val token: String)

data class ResolveTokenResponse(
    val matched: Boolean,
    val reason: String? = null,
    val matchId: String? = null
)

data class OkResponse(val ok: Boolean)

// --- Goal buddies ---

data class BuddyRequestRequest(val targetUserId: String)

data class BuddyRequestResponse(
    val id: String,
    val status: String // "pending" | "active"
)

data class BuddyIdRequest(val buddyId: String)

// --- Shared goals / events ---

data class GoalConfig(
    val targetApp: String? = null,
    val limitMinutes: Int? = null,
    val metricType: String,
    val duration: String // "today" | "this_week"
)

data class CreateEventRequest(
    val buddyConnectionId: String,
    val goalConfig: GoalConfig
)

data class CreateEventResponse(val eventId: String)

data class JoinEventRequest(
    val eventId: String,
    val consentedToShare: Boolean
)

data class SetConsentRequest(
    val eventId: String,
    val consentedToShare: Boolean
)

data class UpdateInsightRequest(
    val eventId: String,
    val metric: String,
    val value: Double
)

data class UpdateInsightResponse(
    val written: Boolean,
    val reason: String? = null
)

// --- Error shape returned by the API on non-2xx responses ---

data class ApiErrorBody(
    val error: String,
    val message: String
)