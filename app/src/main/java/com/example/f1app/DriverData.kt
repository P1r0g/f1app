package com.example.f1app

// null means the value is unknown; zero points is a valid championship score.
data class Driver(
    val driverId: String,
    val name: String,
    val surname: String,
    val number: Int,
    val nationality: String,
    val shortName: String,
    val teamId: String,
    val position: Int? = null,
    val points: Double? = null,
    val teamName: String = ""
)

data class StoredChampionship(
    val drivers: List<Driver> = emptyList(),
    val season: Int? = null
)

// Local persistence boundary; also allows deterministic JVM tests.
interface ChampionshipStorage {
    fun read(): StoredChampionship
    fun write(snapshot: StoredChampionship)
    fun createBackup(): Boolean
    fun restoreBackup(): StoredChampionship?
    fun deleteBackup(): Boolean
    fun backupExists(): Boolean
}
