package com.example.f1app

data class Driver(
    val driverId: String,
    val name: String,
    val surname: String,
    val number: Int,
    val nationality: String,
    val shortName: String,
    val teamId: String,
    val position: Int = 0,
    val points: Int = 0
)

// Local snapshot used in labs 1 and 2. This is not a network request.
// Source: https://f1api.dev/api/current/drivers-championship
// Snapshot selected on 2026-10-29; API positions are preserved.
val demoDrivers = listOf(
    Driver("antonelli", "Andrea", "Kimi Antonelli", 12, "Italy", "ANT", "mercedes", 1, 365),
    Driver("russell", "George", "Russell", 63, "Great Britain", "RUS", "mercedes", 2, 242),
    Driver("hamilton", "Lewis", "Hamilton", 44, "Great Britain", "HAM", "ferrari", 3, 241),
    Driver("max_verstappen", "Max", "Verstappen", 33, "Netherlands", "VER", "red_bull", 4, 236),
    Driver("leclerc", "Charles", "Leclerc", 16, "Monaco", "LEC", "ferrari", 5, 233),
    Driver("norris", "Lando", "Norris", 4, "Great Britain", "NOR", "mclaren", 6, 211),
    Driver("piastri", "Oscar", "Piastri", 81, "Australia", "PIA", "mclaren", 7, 144),
    Driver("hadjar", "Isack", "Hadjar", 6, "France", "HAD", "red_bull", 8, 105),
    Driver("lawson", "Liam", "Lawson", 30, "New Zealand", "LAW", "rb", 9, 77),
    Driver("gasly", "Pierre", "Gasly", 10, "France", "GAS", "alpine", 10, 42),
    Driver("lindblad", "Arvid", "Lindblad", 36, "Great Britain", "LIN", "rb", 11, 38),
    Driver("colapinto", "Franco", "Colapinto", 43, "Argentina", "COL", "alpine", 12, 27),
    Driver("bearman", "Oliver", "Bearman", 87, "Great Britain", "BEA", "haas", 13, 23),
    Driver("hulkenberg", "Nico", "Hulkenberg", 27, "Germany", "HUL", "audi", 14, 13),
    Driver("bortoleto", "Gabriel", "Bortoleto", 5, "Brazil", "BOR", "audi", 15, 10),
    Driver("ocon", "Esteban", "Ocon", 31, "France", "OCO", "haas", 16, 7),
    Driver("alonso", "Fernando", "Alonso", 14, "Spain", "ALO", "aston_martin", 17, 7),
    Driver("sainz", "Carlos", "Sainz", 55, "Spain", "SAI", "williams", 18, 7),
    Driver("albon", "Alex", "Albon", 23, "Thailand", "ALB", "williams", 19, 5),
    Driver("stroll", "Lance", "Stroll", 18, "Canada", "STR", "aston_martin", 21, 0),
    Driver("bottas", "Valtteri", "Bottas", 77, "Finland", "BOT", "cadillac", 22, 0),
    Driver("perez", "Sergio", "Pérez", 11, "Mexico", "PER", "cadillac", 23, 0)
)
