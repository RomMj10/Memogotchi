package com.example.memogotchi.api

import retrofit2.http.Body
import retrofit2.http.POST

interface GoalBuddyApiService {

    // --- Nearby / presence ---

    @POST("nearby/token")
    suspend fun requestNearbyToken(): NearbyTokenResponse

    @POST("nearby/resolve")
    suspend fun resolveNearbyToken(@Body body: ResolveTokenRequest): ResolveTokenResponse

    @POST("nearby/standby")
    suspend fun setStandbyStatus(): OkResponse

    // --- Goal buddies ---

    @POST("buddies/request")
    suspend fun createGoalBuddyRequest(@Body body: BuddyRequestRequest): BuddyRequestResponse

    @POST("buddies/accept")
    suspend fun acceptGoalBuddyRequest(@Body body: BuddyIdRequest): OkResponse

    @POST("buddies/decline")
    suspend fun declineGoalBuddyRequest(@Body body: BuddyIdRequest): OkResponse

    @POST("buddies/end")
    suspend fun endGoalBuddyConnection(@Body body: BuddyIdRequest): OkResponse

    // --- Shared goals / events ---

    @POST("events/create")
    suspend fun createBuddyEvent(@Body body: CreateEventRequest): CreateEventResponse

    @POST("events/join")
    suspend fun joinBuddyEvent(@Body body: JoinEventRequest): OkResponse

    @POST("events/consent")
    suspend fun setEventConsent(@Body body: SetConsentRequest): OkResponse

    @POST("events/insight")
    suspend fun updateInsightShare(@Body body: UpdateInsightRequest): UpdateInsightResponse

    @POST("nearby/checkReady")
    suspend fun checkNearbyReady(@Body request: CheckNearbyReadyRequest): CheckNearbyReadyResponse
}