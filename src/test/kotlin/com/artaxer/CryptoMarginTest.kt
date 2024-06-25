package com.artaxer

import com.artaxer.service.CryptoDto
import com.artaxer.service.CryptoMarginDto
import com.artaxer.service.buildMargins
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CryptoMarginTest {
    @Test
    fun `test margins calculation for multiple exchanges`() {
        val dateTime = LocalDateTime(2023, 6, 25, 12, 0)
        val cryptoData = listOf(
            CryptoDto("BTC", 30000.0, "ExchangeA", dateTime),
            CryptoDto("BTC", 30500.0, "ExchangeB", dateTime),
            CryptoDto("BTC", 30200.0, "ExchangeC", dateTime)
        )

        val margins = cryptoData.buildMargins()

        assertEquals(3, margins.size)

        val expectedMargins = listOf(
            CryptoMarginDto("ExchangeA", "ExchangeB", 500.0, 1.65, dateTime),
            CryptoMarginDto("ExchangeA", "ExchangeC", 200.0, 0.66, dateTime),
            CryptoMarginDto("ExchangeB", "ExchangeC", 300.0, 0.99, dateTime)
        )

        assertTrue(margins.containsAll(expectedMargins))
    }

    @Test
    fun `test margins calculation with multiple dateTime entries`() {
        val dateTime1 = LocalDateTime(2023, 6, 25, 12, 0)
        val dateTime2 = LocalDateTime(2023, 6, 25, 13, 0)
        val cryptoData = listOf(
            CryptoDto("BTC", 30000.0, "ExchangeA", dateTime1),
            CryptoDto("BTC", 30500.0, "ExchangeB", dateTime1),
            CryptoDto("BTC", 30200.0, "ExchangeC", dateTime1),
            CryptoDto("BTC", 31000.0, "ExchangeA", dateTime2),
            CryptoDto("BTC", 31500.0, "ExchangeB", dateTime2),
            CryptoDto("BTC", 31200.0, "ExchangeC", dateTime2)
        )

        val margins = cryptoData.buildMargins()

        assertEquals(6, margins.size)

        val expectedMargins = listOf(
            CryptoMarginDto("ExchangeA", "ExchangeB", 500.0, 1.65, dateTime1),
            CryptoMarginDto("ExchangeA", "ExchangeC", 200.0, 0.66, dateTime1),
            CryptoMarginDto("ExchangeB", "ExchangeC", 300.0, 0.99, dateTime1),
            CryptoMarginDto("ExchangeA", "ExchangeB", 500.0, 1.6, dateTime2),
            CryptoMarginDto("ExchangeA", "ExchangeC", 200.0, 0.64, dateTime2),
            CryptoMarginDto("ExchangeB", "ExchangeC", 300.0, 0.96, dateTime2)
        )

        assertTrue(margins.containsAll(expectedMargins))
    }

    @Test
    fun `test margins calculation with different symbols should throw exception`() {
        val cryptoData = listOf(
            CryptoDto("BTC", 30000.0, "ExchangeA", LocalDateTime(2023, 6, 25, 12, 0)),
            CryptoDto("ETH", 2000.0, "ExchangeA", LocalDateTime(2023, 6, 25, 12, 0)),
            CryptoDto("ETH", 2020.0, "ExchangeB", LocalDateTime(2023, 6, 25, 12, 0))
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            cryptoData.buildMargins()
        }

        assertEquals("All CryptoDto objects must have the same symbol", exception.message)
    }

    @Test
    fun `test margins calculation with no margins`() {
        val cryptoData = listOf<CryptoDto>()

        val margins = cryptoData.buildMargins()

        assertTrue(margins.isEmpty())
    }

    @Test
    fun `test margins calculation with single entry`() {
        val cryptoData = listOf(
            CryptoDto("BTC", 30000.0, "ExchangeA", LocalDateTime(2023, 6, 25, 12, 0))
        )

        val margins = cryptoData.buildMargins()

        assertTrue(margins.isEmpty())
    }
}