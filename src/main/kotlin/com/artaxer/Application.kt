package com.artaxer

import com.artaxer.route.configureExceptions
import com.artaxer.route.configureRouting
import com.artaxer.service.CryptoService
import com.artaxer.service.DataProviderService
import com.artaxer.service.PriceExtractor
import com.artaxer.service.PriceService
import com.artaxer.service.event.EventDispatcher
import com.artaxer.service.exchange.NobitexExchange
import com.typesafe.config.ConfigFactory
import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfoList
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.ratelimit.*
import kotlinx.coroutines.asCoroutineDispatcher
import org.jetbrains.exposed.sql.Database
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds


fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureKoin()
    configureSerialization()
    configureRouting()
    configureExceptions()
    initDataProvider()
    rateLimit()
}

fun Application.rateLimit() {
    install(RateLimit) {
        global {
            rateLimiter(limit = 50, refillPeriod = 60.seconds)
        }
    }
}
val koinModule = module {
    single { CryptoService() }
    single { PriceService() }
    single { EventDispatcher() }
    single { DataProviderService(get()) }
    single { NobitexExchange() }
}

fun Application.initDataProvider() {
    val dataProviderService: DataProviderService by inject()
    dataProviderService.fetchAndSaveData()
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
    install(CORS) {
        anyHost()
    }
}
fun Application.configureKoin() {
    install(Koin) {
        modules(koinModule)
    }
}

val context = Executors.newFixedThreadPool(50).asCoroutineDispatcher()
object ReflectionHelper {
    private val graphData =
        ClassGraph().enableAllInfo().acceptPackages("com.artaxer.service.exchange").scan()
    val exchanges: ClassInfoList =
        graphData.allClasses.filter { it.superclass?.name?.endsWith(PriceExtractor::class.java.simpleName) ?: false }
}

object AppConfig {
    private fun getConfig(): ApplicationConfig {
        val environment = System.getProperty("env") ?: "test"
        val configFileName = when (environment) {
            "test" -> "application-test.conf"
            "develop" -> "application-dev.conf"
            "prod" -> "application.conf"
            else -> error("config file not defined!")
        }

        return HoconApplicationConfig(ConfigFactory.load(configFileName))
    }
    val databaseUrl: String = getConfig().tryGetString("ktor.database.url")!!
    val databaseUserName: String = getConfig().tryGetString("ktor.database.user")!!
    val databasePassword: String = getConfig().tryGetString("ktor.database.password")!!
    val databaseDriver: String = getConfig().tryGetString("ktor.database.driver")!!
}

fun getDatabase() = Database.connect(
    url = AppConfig.databaseUrl,
    user = AppConfig.databaseUserName,
    driver = AppConfig.databaseDriver,
    password = AppConfig.databasePassword
)