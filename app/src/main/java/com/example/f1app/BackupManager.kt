package com.example.f1app

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object BackupManager {

    private const val BACKUP_FILE_NAME = "f1_drivers_2026_backup.json"
    private const val CONTENT_PREFS = "f1_content_2026"
    private const val CONTENT_KEY = "drivers"

    private const val SOURCE_URL =
        "https://f1api.dev/api/current/drivers-championship"

    fun getCurrentDrivers(context: Context): List<Driver> {
        val json = context
            .getSharedPreferences(CONTENT_PREFS, Context.MODE_PRIVATE)
            .getString(CONTENT_KEY, null)
            ?: return demoDrivers

        return try {
            decodeDrivers(JSONArray(json))
        } catch (exception: Exception) {
            Log.e("F1Backup", "Cannot read driver data", exception)
            emptyList()
        }
    }

    fun setCurrentDrivers(
        context: Context,
        drivers: List<Driver>
    ) {
        context
            .getSharedPreferences(CONTENT_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CONTENT_KEY, encodeDrivers(drivers).toString())
            .apply()
    }

    fun createBackup(context: Context): Boolean {
        val drivers = getCurrentDrivers(context)

        if (drivers.isEmpty()) {
            return false
        }

        val file = AtomicFile(
            File(context.filesDir, BACKUP_FILE_NAME)
        )

        var output: FileOutputStream? = null

        return try {
            val root = JSONObject().apply {
                put("version", 1)
                put("domain", "formula1")
                put("season", 2026)
                put("created_at", System.currentTimeMillis())
                put("source", SOURCE_URL)
                put("drivers", encodeDrivers(drivers))
            }

            val stream = file.startWrite()
            output = stream

            stream.write(
                root.toString(2).toByteArray(Charsets.UTF_8)
            )

            file.finishWrite(stream)
            true
        } catch (exception: Exception) {
            output?.let { file.failWrite(it) }
            Log.e("F1Backup", "Cannot create backup", exception)
            false
        }
    }

    fun restoreBackup(context: Context): List<Driver>? {
        return try {
            val file = AtomicFile(
                File(context.filesDir, BACKUP_FILE_NAME)
            )

            val root = JSONObject(
                String(file.readFully(), Charsets.UTF_8)
            )

            require(root.getInt("version") == 1)
            require(root.getString("domain") == "formula1")
            require(root.getInt("season") == 2026)

            val drivers = decodeDrivers(
                root.getJSONArray("drivers")
            )

            require(drivers.isNotEmpty())

            setCurrentDrivers(context, drivers)
            drivers
        } catch (exception: Exception) {
            Log.e("F1Backup", "Cannot restore backup", exception)
            null
        }
    }

    fun deleteBackup(context: Context): Boolean {
        return try {
            val file = AtomicFile(
                File(context.filesDir, BACKUP_FILE_NAME)
            )

            file.delete()
            !file.baseFile.exists()
        } catch (exception: Exception) {
            Log.e("F1Backup", "Cannot delete backup", exception)
            false
        }
    }

    fun backupExists(context: Context): Boolean {
        return File(
            context.filesDir,
            BACKUP_FILE_NAME
        ).exists()
    }

    private fun encodeDrivers(drivers: List<Driver>): JSONArray {
        return JSONArray().apply {
            drivers.forEach { driver ->
                put(
                    JSONObject().apply {
                        put("driverId", driver.driverId)
                        put("name", driver.name)
                        put("surname", driver.surname)
                        put("number", driver.number)
                        put("nationality", driver.nationality)
                        put("shortName", driver.shortName)
                        put("teamId", driver.teamId)
                        put("position", driver.position)
                        put("points", driver.points)
                    }
                )
            }
        }
    }

    private fun decodeDrivers(array: JSONArray): List<Driver> {
        val drivers = (0 until array.length()).map { index ->
            val json = array.getJSONObject(index)

            Driver(
                driverId = json.getString("driverId"),
                name = json.getString("name"),
                surname = json.getString("surname"),
                number = json.getInt("number"),
                nationality = json.getString("nationality"),
                shortName = json.getString("shortName"),
                teamId = json.getString("teamId"),
                position = json.optInt("position", 0),
                points = json.optInt("points", 0)
            ).also { driver ->
                require(driver.driverId.isNotBlank())
                require(driver.name.isNotBlank())
                require(driver.surname.isNotBlank())
                require(driver.number > 0)
                require(driver.position >= 0)
                require(driver.points >= 0)
            }
        }

        require(
            drivers.map { it.driverId }.distinct().size == drivers.size
        )

        return drivers
    }
}