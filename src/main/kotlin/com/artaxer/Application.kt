package com.artaxer

import com.artaxer.plugins.*
import com.typesafe.config.ConfigFactory
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*

fun main(args: Array<String>) {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureDatabases()
    configureRouting()
}
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}
object AppConfig {
    private val config = ConfigFactory.load()
    val postgresUrl: String = config.getString("settings.postgres.url")
    val postgresUserName: String = config.getString("settings.postgres.username")
    val postgresPassword: String = config.getString("settings.postgres.password")
}