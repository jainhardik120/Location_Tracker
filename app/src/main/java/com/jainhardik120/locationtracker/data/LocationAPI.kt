package com.jainhardik120.locationtracker.data

import com.jainhardik120.locationtracker.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType

class LocationAPI(
    private val client: HttpClient
) {
    private suspend inline fun <T, reified R> performApiRequest(
        call: () -> T
    ): Result<T, R> {
        return try {
            val response = call.invoke()
            Result.Success(response)
        } catch (e: ClientRequestException) {
            Result.ClientException(e.response.body<R>(), e.response.status)
        } catch (e: Exception) {
            Result.Exception(e.message)
        }
    }


    suspend fun createNewLocation(data: LocationData): Result<NewLocationResponse, ErrorResponse> {
        return performApiRequest {
            client.request(Routes.NEW_LOCATION) {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(data)
            }.body()
        }
    }

    suspend fun updateLocation(
        token: String,
        data: LatestLocationResponse
    ): Result<MessageResponse, ErrorResponse> {
        return performApiRequest {
            client.request(Routes.UPDATE_LOCATION) {
                method = HttpMethod.Put
                contentType(ContentType.Application.Json)
                setBody(data)
                headers {
                    bearerAuth(token)
                }
            }.body()
        }
    }

    suspend fun endLocation(
        token: String
    ): Result<MessageResponse, ErrorResponse> {
        return performApiRequest {
            client.request(Routes.END_LOCATION) {
                method = HttpMethod.Put
                contentType(ContentType.Application.Json)
                headers {
                    bearerAuth(token)
                }
            }.body()
        }
    }

    suspend fun getLatestLocation(id: String): Result<LatestLocationResponse, ErrorResponse> {
        return performApiRequest {
            client.request(Routes.dynamicLatestLocation(id)) {
                method = HttpMethod.Get
            }.body()
        }
    }
}

object Routes {
    private const val BASE_HOST = "https://location-server-ppxr.onrender.com"

    const val NEW_LOCATION = "$BASE_HOST/newLocation"
    const val UPDATE_LOCATION = "$BASE_HOST/updateLocation"
    const val END_LOCATION = "$BASE_HOST/endLocation"
    private const val LATEST_LOCATION = "$BASE_HOST/latestLocation"

    fun dynamicLatestLocation(locationId: String): String {
        return "$LATEST_LOCATION/$locationId"
    }
}
