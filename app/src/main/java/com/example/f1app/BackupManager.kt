package com.example.f1app

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object BackupManager {
    private const val BACKUP_FILE_NAME = "f1_drivers_2026_backup.json"
    private const val CONTENT_PREFS = "f1_content_2026"
    private const val CONTENT_KEY = "drivers"
    private const val SEASON_KEY = "season"

    fun getCurrentSnapshot(context: Context): StoredChampionship {
        val prefs = context.getSharedPreferences(CONTENT_PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(CONTENT_KEY, null) ?: return StoredChampionship()
        return try {
            val drivers = DriverJsonCodec.decodeDrivers(json)
            val season = if (prefs.contains(SEASON_KEY)) {
                prefs.getInt(SEASON_KEY, 2026)
            } else null
            StoredChampionship(drivers, season)
        } catch (exception: Exception) {
            Log.e("F1Backup", "Cannot read cached standings", exception)
            StoredChampionship()
        }
    }

    fun setCurrentSnapshot(context: Context, snapshot: StoredChampionship) {
        val editor = context.getSharedPreferences(CONTENT_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CONTENT_KEY, DriverJsonCodec.encodeDrivers(snapshot.drivers))
        if (snapshot.season == null) editor.remove(SEASON_KEY)
        else editor.putInt(SEASON_KEY, snapshot.season)
        editor.apply()
    }

    fun getCurrentDrivers(context: Context): List<Driver> = getCurrentSnapshot(context).drivers

    fun setCurrentDrivers(context: Context, drivers: List<Driver>) {
        setCurrentSnapshot(context, getCurrentSnapshot(context).copy(drivers = drivers))
    }

    fun createBackup(context: Context): Boolean {
        val snapshot = getCurrentSnapshot(context)
        if (snapshot.drivers.isEmpty()) return false
        val file = AtomicFile(File(context.filesDir, BACKUP_FILE_NAME))
        var output: FileOutputStream? = null
        return try {
            val bytes = DriverJsonCodec.encodeBackup(snapshot, System.currentTimeMillis())
                .toByteArray(Charsets.UTF_8)
            val stream = file.startWrite()
            output = stream
            stream.write(bytes)
            file.finishWrite(stream)
            true
        } catch (exception: Exception) {
            output?.let { file.failWrite(it) }
            Log.e("F1Backup", "Cannot create backup", exception)
            false
        }
    }

    fun restoreBackup(context: Context): StoredChampionship? {
        return try {
            val file = AtomicFile(File(context.filesDir, BACKUP_FILE_NAME))
            val snapshot = DriverJsonCodec.decodeBackup(
                String(file.readFully(), Charsets.UTF_8)
            )
            require(snapshot.drivers.isNotEmpty())
            setCurrentSnapshot(context, snapshot)
            snapshot
        } catch (exception: Exception) {
            Log.e("F1Backup", "Cannot restore backup", exception)
            null
        }
    }

    fun deleteBackup(context: Context): Boolean {
        return try {
            val file = AtomicFile(File(context.filesDir, BACKUP_FILE_NAME))
            file.delete()
            !file.baseFile.exists()
        } catch (exception: Exception) {
            Log.e("F1Backup", "Cannot delete backup", exception)
            false
        }
    }

    fun backupExists(context: Context): Boolean =
        File(context.filesDir, BACKUP_FILE_NAME).exists()
}

class PreferencesChampionshipStorage(context: Context) : ChampionshipStorage {
    private val appContext = context.applicationContext
    override fun read() = BackupManager.getCurrentSnapshot(appContext)
    override fun write(snapshot: StoredChampionship) =
        BackupManager.setCurrentSnapshot(appContext, snapshot)
    override fun createBackup() = BackupManager.createBackup(appContext)
    override fun restoreBackup() = BackupManager.restoreBackup(appContext)
    override fun deleteBackup() = BackupManager.deleteBackup(appContext)
    override fun backupExists() = BackupManager.backupExists(appContext)
}
