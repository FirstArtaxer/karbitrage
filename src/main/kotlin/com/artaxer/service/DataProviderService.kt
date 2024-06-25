package com.artaxer.service

import com.artaxer.ReflectionHelper
import com.artaxer.configureDatabases
import io.ktor.client.request.*
import io.ktor.util.logging.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime
import java.util.concurrent.Executors
import kotlin.reflect.full.createInstance

class DataProviderService {
    private val LOGGER = KtorSimpleLogger("com.artaxer.service.DataProviderService")

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
        val context = Executors.newFixedThreadPool(50).asCoroutineDispatcher()
        val exchangesWithFunctions = ReflectionHelper.exchanges.map {
            val exchangeClass = Class.forName(it.name)
            val request = exchangeClass.getMethod("getRequest").invoke(exchangeClass.kotlin.createInstance())
            val extractor = exchangeClass.getMethod("getExtractor").invoke(exchangeClass.kotlin.createInstance())
            Triple(it.simpleName, request as HttpRequestBuilder, extractor as ((String) -> Map<String, Double>))
        }
        val cryptoService = CryptoService(database = configureDatabases())
        val priceService = PriceService()

        CoroutineScope(context = context).launch {
            while (true) {
                delay(60000)
                val dateTime = LocalDateTime.now().toKotlinLocalDateTime()
                exchangesWithFunctions.forEach { exchange ->
                    launch(context) {
                        runCatching {
                            LOGGER.info("${exchange.first} fetched!")
                            val exchangePrices =
                                priceService.getPrices(httpRequest = exchange.second, extractor = exchange.third)
                            cryptoService.save(
                                exchangePrices = Triple(
                                    exchange.first,
                                    exchangePrices.parseToString(),
                                    dateTime
                                )
                            )
                            LOGGER.info("${exchange.first} saved!")
                        }.onFailure {
                            LOGGER.error("${exchange.first} cannot saved! - ${it.stackTraceToString()}")
                        }
                    }
                }
            }
        }
    }
}
fun Map<String, Double>.parseToString() = this.entries.joinToString(",")