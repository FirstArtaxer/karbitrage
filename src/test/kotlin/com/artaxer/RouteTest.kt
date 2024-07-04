package com.artaxer

import com.artaxer.service.CryptoDto
import com.artaxer.service.CryptoService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.koin.test.KoinTest
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteTest : KoinTest {
    private val cryptoService = mockk<CryptoService>()
    private val myModule = module {
        single<CryptoService> { cryptoService }
    }
    @Before
    fun before() {
        GlobalContext.startKoin {
            modules(koinModule)
        }
    }

    @After
    fun after() {
        GlobalContext.stopKoin()
    }
    @Test
    fun `last prices test`() = testApplication {
        application {
            loadKoinModules(myModule)
        }
        val response = listOf(
            CryptoDto(
                symbol = "BTC",
                price = 5500.0,
                exchange = "binance",
                dateTime = LocalDateTime.now().toKotlinLocalDateTime()
            )
        )
        coEvery { cryptoService.getLatestPrices() } returns response
        client.get("cryptos/last-prices").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(Json.encodeToString(response), bodyAsText())
        }
    }
}