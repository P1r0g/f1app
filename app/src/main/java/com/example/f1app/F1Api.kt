package com.example.f1app

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface F1Api {
    @GET("current/drivers-championship")
    suspend fun getDriverChampionship(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): DriverChampionshipResponseDto
}

data class DriverChampionshipResponseDto(
    val limit: Int? = null,
    val offset: Int? = null,
    // The live API returns the PAGE size here, so pagination must not rely on total.
    val total: Int? = null,
    val season: Int? = null,
    @SerializedName("drivers_championship")
    val standings: List<DriverStandingDto>? = null
)

data class DriverStandingDto(
    val driverId: String? = null,
    val teamId: String? = null,
    val position: Int? = null,
    val points: Double? = null,
    val driver: DriverDto? = null,
    val team: TeamDto? = null
)

data class DriverDto(
    val name: String? = null,
    val surname: String? = null,
    val number: Int? = null,
    val nationality: String? = null,
    val shortName: String? = null
)

data class TeamDto(
    val teamId: String? = null,
    val teamName: String? = null
)

object F1ApiClient {
    const val BASE_URL = "https://f1api.dev/api/"

    fun create(baseUrl: String = BASE_URL): F1Api {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(F1Api::class.java)
    }
}
