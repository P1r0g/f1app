package com.example.f1app

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** Shared encoding for the current cache and manually created backup files. */
object DriverJsonCodec {
    fun encodeDrivers(drivers: List<Driver>): String = driversToJson(drivers).toString()

    fun decodeDrivers(json: String): List<Driver> =
        readDrivers(JsonParser.parseString(json).asJsonArray)

    fun encodeBackup(snapshot: StoredChampionship, createdAt: Long): String {
        return JsonObject().apply {
            addProperty("version", 2)
            addProperty("domain", "formula1")
            addProperty("season", snapshot.season)
            addProperty("created_at", createdAt)
            addProperty("source", "https://f1api.dev/api/current/drivers-championship")
            add("drivers", driversToJson(snapshot.drivers))
        }.toString()
    }

    fun decodeBackup(json: String): StoredChampionship {
        val root = JsonParser.parseString(json).asJsonObject
        require(root.get("version")?.asInt in listOf(1, 2))
        require(root.text("domain") == "formula1")
        val season = root.number("season")?.toInt()
        require(season == null || season >= 1950)
        return StoredChampionship(
            drivers = readDrivers(root.getAsJsonArray("drivers")),
            season = season
        )
    }

    private fun driversToJson(drivers: List<Driver>): JsonArray = JsonArray().apply {
        drivers.forEach { driver ->
            add(JsonObject().apply {
                addProperty("driverId", driver.driverId)
                addProperty("name", driver.name)
                addProperty("surname", driver.surname)
                addProperty("number", driver.number)
                addProperty("nationality", driver.nationality)
                addProperty("shortName", driver.shortName)
                addProperty("teamId", driver.teamId)
                addProperty("teamName", driver.teamName)
                add("position", driver.position?.let { com.google.gson.JsonPrimitive(it) } ?: JsonNull.INSTANCE)
                add("points", driver.points?.let { com.google.gson.JsonPrimitive(it) } ?: JsonNull.INSTANCE)
            })
        }
    }

    private fun readDrivers(array: JsonArray): List<Driver> {
        val drivers = array.map { element ->
            val json = element.asJsonObject
            val id = json.text("driverId")
            val name = json.text("name")
            val surname = json.text("surname")
            require(id.isNotBlank() && name.isNotBlank() && surname.isNotBlank())

            val rawPosition = json.number("position")?.toInt()
            // Old labs wrote zero for missing fields. Do not present that as real data.
            val legacy = rawPosition == 0
            val points = if (legacy) null else json.number("points")
            require(points == null || points.isFinite() && points >= 0.0)

            Driver(
                driverId = id,
                name = name,
                surname = surname,
                number = json.number("number")?.toInt() ?: 0,
                nationality = json.text("nationality"),
                shortName = json.text("shortName"),
                teamId = json.text("teamId"),
                position = rawPosition?.takeIf { it > 0 },
                points = points,
                teamName = json.text("teamName")
            )
        }
        require(drivers.map { it.driverId }.distinct().size == drivers.size)
        return drivers
    }

    private fun JsonObject.text(key: String): String =
        get(key)?.takeUnless { it.isJsonNull }?.asString.orEmpty()

    private fun JsonObject.number(key: String): Double? =
        get(key)?.takeUnless { it.isJsonNull }?.asDouble
}
