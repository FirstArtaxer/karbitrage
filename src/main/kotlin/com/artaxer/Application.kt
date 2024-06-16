package com.artaxer

import com.artaxer.plugins.CryptoDto
import com.artaxer.plugins.CryptoService
import com.typesafe.config.ConfigFactory
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.exposed.sql.Database
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main() {
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

@Serializer(forClass = LocalDateTime::class)
class LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), formatter)
    }
}

fun Application.configureDatabases() {

}

val db = Database.connect(
    url = AppConfig.postgresUrl,
    user = AppConfig.postgresUserName,
    driver = "org.postgresql.Driver",
    password = AppConfig.postgresPassword
)

fun Application.configureRouting() {
    val cryptoService = CryptoService(database = db)
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        get("/crypto/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val crypto = cryptoService.read(id)
            if (crypto != null) {
                call.respond(HttpStatusCode.OK, crypto)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        post("/crypto") {
            val cryptoDto = call.receive<CryptoDto>()
            val id = cryptoService.create(cryptoDto)
            call.respond(HttpStatusCode.Created, id)
        }
    }
}

object AppConfig {
    private val config = ConfigFactory.load()
    val postgresUrl: String = config.getString("settings.postgres.url")
    val postgresUserName: String = config.getString("settings.postgres.username")
    val postgresPassword: String = config.getString("settings.postgres.password")
}