package com.example.f1app

import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

class F1RepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: F1Repository

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        repository = F1Repository(F1ApiClient.create(server.url("/api/").toString()))
    }

    @After fun tearDown() { server.shutdown() }

    private fun fixture(offset: Int): String =
        javaClass.getResource("/f1api/page-$offset.json")!!.readText()

    private fun enqueue(body: String, code: Int = 200) {
        server.enqueue(MockResponse().setResponseCode(code)
            .setHeader("Content-Type", "application/json").setBody(body))
    }

    @Test fun `requests actual pages and does not confuse total with dataset size`() = runBlocking {
        listOf(0, 10, 20).forEach { enqueue(fixture(it)) }
        val first = repository.loadPage(10, 0)
        assertEquals(10, first.drivers.size)
        assertEquals(365.0, first.drivers.first().points!!, 0.0)
        assertEquals(1, first.drivers.first().position)
        assertEquals("Mercedes Formula 1 Team", first.drivers.first().teamName)
        assertTrue(first.hasMore) // API total is 10, but another page exists.
        assertEquals(10, first.nextOffset)

        val second = repository.loadPage(10, first.nextOffset)
        assertEquals(10, second.drivers.size)
        assertTrue(second.hasMore)
        assertEquals(20, second.nextOffset)
        assertEquals(21, second.drivers.last().position) // Preserve the API's gap.

        val last = repository.loadPage(10, second.nextOffset)
        assertEquals(2, last.drivers.size)
        assertFalse(last.hasMore)
        assertEquals(0.0, last.drivers.last().points!!, 0.0)
        assertEquals(23, last.drivers.last().position)
        assertEquals(22, (first.drivers + second.drivers + last.drivers)
            .map { it.driverId }.distinct().size)

        for (offset in listOf(0, 10, 20)) {
            assertEquals("/api/current/drivers-championship?limit=10&offset=$offset",
                server.takeRequest().path)
        }
    }

    @Test fun `missing points fails validation rather than silently displaying zero`() {
        val json = JsonParser.parseString(fixture(0)).asJsonObject
        json.getAsJsonArray("drivers_championship")[0].asJsonObject.remove("points")
        enqueue(json.toString())
        assertThrows(InvalidApiResponseException::class.java) {
            runBlocking { repository.loadPage(10, 0) }
        }
    }

    @Test fun `null position is supported for an unclassified driver`() = runBlocking {
        val json = JsonParser.parseString(fixture(0)).asJsonObject
        json.getAsJsonArray("drivers_championship")[0].asJsonObject
            .add("position", com.google.gson.JsonNull.INSTANCE)
        enqueue(json.toString())
        val page = repository.loadPage(10, 0)
        assertNull(page.drivers.first().position)
        assertEquals(365.0, page.drivers.first().points!!, 0.0)
    }

    @Test fun `empty successful response ends pagination`() = runBlocking {
        enqueue("""{"season":2026,"limit":10,"offset":0,"total":0,"drivers_championship":[]}""")
        val page = repository.loadPage(10, 0)
        assertTrue(page.drivers.isEmpty())
        assertFalse(page.hasMore)
    }

    @Test fun `404 beyond final page ends pagination`() = runBlocking {
        enqueue(fixture(30), 404)
        val page = repository.loadPage(10, 30)
        assertTrue(page.drivers.isEmpty())
        assertFalse(page.hasMore)
    }

    @Test fun `first page 404 remains a visible error`() {
        enqueue(fixture(30), 404)
        val error = assertThrows(HttpException::class.java) {
            runBlocking { repository.loadPage(10, 0) }
        }
        assertEquals(404, error.code())
    }

    @Test fun `server errors propagate`() {
        enqueue("{}", 500)
        val error = assertThrows(HttpException::class.java) {
            runBlocking { repository.loadPage(10, 0) }
        }
        assertEquals(500, error.code())
    }

    @Test fun `invalid JSON and missing standings are rejected`() {
        enqueue("{broken")
        assertThrows(Exception::class.java) {
            runBlocking { repository.loadPage(10, 0) }
        }
        enqueue("""{"season":2026}""")
        assertThrows(InvalidApiResponseException::class.java) {
            runBlocking { repository.loadPage(10, 0) }
        }
    }

    @Test fun `server ignoring offset is rejected`() {
        enqueue(fixture(0))
        assertThrows(InvalidApiResponseException::class.java) {
            runBlocking { repository.loadPage(10, 10) }
        }
    }
}
