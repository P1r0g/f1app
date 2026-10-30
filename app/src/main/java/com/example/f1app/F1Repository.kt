package com.example.f1app

import retrofit2.HttpException

data class DriverPage(
    val drivers: List<Driver>,
    val season: Int?,
    val nextOffset: Int,
    val hasMore: Boolean
)

interface DriverPageSource {
    suspend fun loadPage(limit: Int, offset: Int): DriverPage
}

class InvalidApiResponseException(message: String) : IllegalStateException(message)

class F1Repository(private val api: F1Api) : DriverPageSource {
    override suspend fun loadPage(limit: Int, offset: Int): DriverPage {
        require(limit > 0 && offset >= 0)

        val response = try {
            api.getDriverChampionship(limit, offset)
        } catch (exception: HttpException) {
            // This API returns 404 for an offset past the final row.
            // A first-page 404 remains a visible error.
            if (offset > 0 && exception.code() == 404) {
                return DriverPage(emptyList(), null, offset, false)
            }
            throw exception
        }

        val rows = response.standings
            ?: throw InvalidApiResponseException("Missing drivers_championship")
        val season = response.season?.takeIf { it >= 1950 }
            ?: throw InvalidApiResponseException("Missing championship season")

        if (response.offset != null && response.offset != offset) {
            throw InvalidApiResponseException("Server ignored requested offset")
        }
        if (rows.size > limit) {
            throw InvalidApiResponseException("Server ignored requested limit")
        }

        val drivers = rows.map { row ->
            val person = row.driver
                ?: throw InvalidApiResponseException("Missing nested driver")
            val id = row.driverId?.takeIf { it.isNotBlank() }
                ?: throw InvalidApiResponseException("Missing driverId")
            val name = person.name?.takeIf { it.isNotBlank() }
                ?: throw InvalidApiResponseException("Missing driver name")
            val surname = person.surname?.takeIf { it.isNotBlank() }
                ?: throw InvalidApiResponseException("Missing driver surname")
            val points = row.points?.takeIf { it.isFinite() && it >= 0.0 }
                ?: throw InvalidApiResponseException("Missing or invalid points")
            if (row.position != null && row.position <= 0) {
                throw InvalidApiResponseException("Invalid position")
            }

            Driver(
                driverId = id,
                name = name,
                surname = surname,
                number = person.number ?: 0,
                nationality = person.nationality.orEmpty(),
                shortName = person.shortName.orEmpty(),
                teamId = row.teamId ?: row.team?.teamId.orEmpty(),
                // Both fields belong to the standing, not the nested driver object.
                position = row.position,
                points = points,
                teamName = row.team?.teamName.orEmpty()
            )
        }

        if (drivers.map { it.driverId }.distinct().size != drivers.size) {
            throw InvalidApiResponseException("Duplicate drivers within a page")
        }

        return DriverPage(
            drivers = drivers,
            season = season,
            nextOffset = offset + rows.size,
            // total is not a dataset total in this API. A short page ends paging.
            hasMore = rows.size == limit
        )
    }
}
