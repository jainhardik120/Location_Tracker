package com.jainhardik120.locationtracker.data

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class NewLocationResponse(val id: String, val token: String)

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class LatestLocationResponse(val latitude: Double, val longitude: Double)

@Serializable
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val expiryTime: String
)