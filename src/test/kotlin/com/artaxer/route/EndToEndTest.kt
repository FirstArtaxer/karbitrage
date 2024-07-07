package com.artaxer.route

import com.artaxer.koinModule
import com.artaxer.module
import com.artaxer.service.CryptoCode
import com.artaxer.service.event.ExchangePriceEvent
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.koin.core.context.GlobalContext
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class EndToEndTest {
    @Before
    fun before() {
        // Start Koin with the module(s) needed for your tests
        GlobalContext.startKoin {
            modules(koinModule)
        }
    }

    @After
    fun after() {
        // Stop Koin after each test
        GlobalContext.stopKoin()
    }

    @Test
    fun `symbols test`() = testApplication {
        application {
            module()
        }
        client.get("symbols").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(Json.encodeToString(CryptoCode.entries.map { it.name }), bodyAsText())
        }
    }

    @Test
    fun `last prices test`() = testApplication {
        application {
            module()
        }
        val priceEvent = ExchangePriceEvent(
            name = "ex1",
            prices = mapOf("BTC" to 50000.0),
            dateTime = LocalDateTime.of(2024, 7, 7, 12, 12).toKotlinLocalDateTime()
        )
        priceEvent.publish()
        delay(500)
        client.get("cryptos/last-prices").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(
                """[{"symbol":"BTC","price":50000.0,"exchange":"ex1","dateTime":"2024-07-07T12:12"}]""",
                bodyAsText()
            )
        }
    }
}