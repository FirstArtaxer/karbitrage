package com.artaxer

import com.artaxer.service.CryptoDto
import com.artaxer.service.CryptoService
import com.artaxer.service.PriceExtractor
import com.artaxer.service.PriceService
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.typesafe.config.ConfigFactory
import io.github.classgraph.ClassGraph
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Database
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.Executors
import kotlin.reflect.full.createInstance


fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}
fun Application.module() {
    configureSerialization()
    configureRouting()
    fetchAndSaveData()
}
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
    install(CORS) {
        anyHost()
    }
}
private val pricesCache: Cache<String, List<CryptoDto>> = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofMinutes(1))
    .build()
fun Application.configureRouting() {
    val cryptoService = CryptoService(database = configureDatabases())
    routing {
        get("crypto/prices") {
            val fromDateTime = call.parameters["from"]?.toLocalDateTime() ?: error("from parameter should be filled")
            val toDateTime = call.parameters["to"]?.toLocalDateTime() ?: error("to parameter should be filled")
            val symbol = call.parameters["symbol"] ?: error("symbol parameter should be filled")
            val cacheKey = "$symbol-$fromDateTime-$toDateTime"
            val cachedPrices = pricesCache.getIfPresent(cacheKey)
            val prices = if (cachedPrices == null) {
                val dbPrices = cryptoService.getPricesHistory(
                    fromDateTime = fromDateTime,
                    toDateTime = toDateTime,
                    symbol = symbol
                )
                pricesCache.put(cacheKey, dbPrices)
                dbPrices
            } else
                cachedPrices
            call.respond(HttpStatusCode.OK, prices)
        }
        get("crypto/last-prices") {
            val cachedLatestPrices = pricesCache.getIfPresent("latestPrices")
            val latestPrices = if (cachedLatestPrices == null) {
                val dbPrices = cryptoService.getLatestPrices()
                pricesCache.put("latestPrices", dbPrices)
                dbPrices
            } else
                cachedLatestPrices
            call.respond(HttpStatusCode.OK, latestPrices)
        }
    }
}

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
fun Application.fetchAndSaveData() {
    val context = Executors.newFixedThreadPool(50).asCoroutineDispatcher()
    val graphData = ClassGraph().enableAllInfo().acceptPackages("com.artaxer.service.exchange").scan()
    val exchanges =
        graphData.allClasses.filter { it.superclass?.name?.endsWith(PriceExtractor::class.java.simpleName) ?: false }
    val exchangesWithFunctions = exchanges.map {
        val exchangeClass = Class.forName(it.name)
        val request = exchangeClass.getMethod("getRequest").invoke(exchangeClass.kotlin.createInstance())
        val extractor = exchangeClass.getMethod("getExtractor").invoke(exchangeClass.kotlin.createInstance())
        Triple(it.simpleName, request as HttpRequestBuilder, extractor as ((String) -> Map<String, Double>))
    }
    val cryptoService = CryptoService(database = configureDatabases())
    val priceService = PriceService()

    launch(context = context) {
        while (true) {
            delay(60000)
            val dateTime = LocalDateTime.now().toKotlinLocalDateTime()
            exchangesWithFunctions.forEach { exchange ->
                launch(context) {
                    runCatching {
                        log.info("${exchange.first} fetched!")
                        val exchangePrices =
                            priceService.getPrices(httpRequest = exchange.second, extractor = exchange.third)
                        cryptoService.save(
                            exchangePrices = Triple(
                                exchange.first,
                                exchangePrices.parseToString(),
                                dateTime
                            )
                        )
                        log.info("${exchange.first} saved!")
                    }.onFailure {
                        log.error("${exchange.first} cannot saved! - ${it.stackTraceToString()}")
                    }
                }
            }
        }
    }
}

object AppConfig {
    private val config = ConfigFactory.load()
    val postgresUrl: String = config.getString("settings.postgres.url")
    val postgresUserName: String = config.getString("settings.postgres.username")
    val postgresPassword: String = config.getString("settings.postgres.password")
}
fun configureDatabases() = Database.connect(
    url = AppConfig.postgresUrl,
    user = AppConfig.postgresUserName,
    driver = "org.postgresql.Driver",
    password = AppConfig.postgresPassword
)
fun Map<String, Double>.parseToString() = this.entries.joinToString(",")