package com.artaxer

import com.artaxer.service.CryptoService
import com.artaxer.service.PriceExtractor
import com.artaxer.service.PriceService

import com.typesafe.config.ConfigFactory
import io.github.classgraph.ClassGraph
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*

import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Database
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
}
fun Application.configureRouting() {
    val cryptoService = CryptoService(database = configureDatabases())
    routing {
        get("crypto/prices") {
            val fromDateTime = call.parameters["fromDateTime"]?.toLocalDateTime() ?: LocalDateTime.now().minusDays(7)
                .toKotlinLocalDateTime()
            val crypto = cryptoService.getHistoryPrices(fromDateTime = fromDateTime)
            call.respond(HttpStatusCode.OK, crypto)
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