package com.artaxer.service

import com.artaxer.ReflectionHelper
import com.artaxer.context
import com.artaxer.service.event.ExchangePriceEvent
import io.ktor.client.request.*
import io.ktor.util.logging.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime
import kotlin.reflect.full.createInstance
import kotlin.time.measureTimedValue

class DataProviderService {
    private val logger = KtorSimpleLogger("com.artaxer.service.DataProviderService")

    /**
     * This function scans for exchange services, extracts their respective request builders and price extractors,
     * and then periodically fetches and saves prices to a database asynchronously.
     *
     * The workflow is as follows:
     * 1. A coroutine context is created using a fixed thread pool.
     * 2. ClassGraph is used to scan and filter classes that extend the PriceExtractor class.
     * 3. Each exchange service is mapped to its corresponding request and extractor functions.
     * 4. A CryptoService and PriceService instance are initialized for database operations and price fetching, respectively.
     * 5. An infinite loop is launched that:
     *    - Waits for 60 seconds.
     *    - Fetches prices from each exchange service asynchronously.
     *    - Saves the fetched prices along with the timestamp to the database.
     */
    fun fetchAndSaveData() {
        val exchangesWithFunctions = ReflectionHelper.exchanges.map {
            val exchangeClass = Class.forName(it.name)
            val request = exchangeClass.getMethod("getRequest").invoke(exchangeClass.kotlin.createInstance())
            val extractor = exchangeClass.getMethod("getExtractor").invoke(exchangeClass.kotlin.createInstance())
            Triple(it.simpleName, request as HttpRequestBuilder, extractor as ((String) -> Map<String, Double>))
        }
        val priceService = PriceService()
        CoroutineScope(context = context).launch {
            while (true) {
                delay(60000)
                val dateTime = LocalDateTime.now().toKotlinLocalDateTime()
                exchangesWithFunctions.forEach { exchange ->
                    launch(context) {
                        runCatching {
                            val (exchangePrices,timeTaken) = measureTimedValue {
                                priceService.getPrices(httpRequest = exchange.second, extractor = exchange.third)
                            }
                            logger.info("${exchange.first} fetched in $timeTaken!")
                            ExchangePriceEvent(
                                name = exchange.first,
                                prices = exchangePrices,
                                dateTime = dateTime
                            ).publish()
                        }.onFailure {
                            logger.error("${exchange.first} cannot fetched! - ${it.stackTraceToString()}")
                        }
                    }
                }
            }
        }
    }
}