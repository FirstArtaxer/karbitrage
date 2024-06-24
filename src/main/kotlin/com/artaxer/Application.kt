package com.artaxer

import com.artaxer.route.configureExceptions
import com.artaxer.route.configureRouting
import com.artaxer.service.DataProviderService
import com.artaxer.service.PriceExtractor
import com.typesafe.config.ConfigFactory
import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfoList
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import org.jetbrains.exposed.sql.Database


fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureRouting()
    configureExceptions()
    initDataProvider()
}

fun Application.initDataProvider() {
    DataProviderService().fetchAndSaveData()
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
    install(CORS) {
        anyHost()
    }
}

object ReflectionHelper {
    private val graphData =
        ClassGraph().enableAllInfo().acceptPackages("com.artaxer.service.exchange").scan()
    val exchanges: ClassInfoList =
        graphData.allClasses.filter { it.superclass?.name?.endsWith(PriceExtractor::class.java.simpleName) ?: false }
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