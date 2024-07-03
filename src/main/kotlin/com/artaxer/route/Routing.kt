package com.artaxer.route

import com.artaxer.ReflectionHelper
import com.artaxer.service.*
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import java.time.Duration

fun Application.configureExceptions() {
    install(StatusPages) {
        exception<Throwable> { call, throwable ->
            when (throwable) {
                is BadRequestException -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ExceptionResponse(throwable.message)
                    )
                }

                is NotFoundException -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ExceptionResponse(throwable.message)
                    )
                }

                is RuntimeException -> {
                    call.application.log.error(throwable.stackTraceToString())
                    call.respond(
                        HttpStatusCode.NotAcceptable,
                        ExceptionResponse(throwable.message ?: "unknown situation!")
                    )
                }
            }
        }
    }
}

@Serializable
data class ExceptionResponse(
    val message: String
)

class BadRequestException(override val message: String) : Throwable()
class NotFoundException(override val message: String) : Throwable()

fun throwBadRequestEx(message: String): Nothing = throw BadRequestException(message)
fun throwNotFoundEx(message: String): Nothing = throw BadRequestException(message)
private val pricesCache: Cache<String, List<CryptoDto>> = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofMinutes(1))
    .build()

fun Application.configureRouting() {
    val jsonSerializer = Json
    val cryptoService: CryptoService by inject()
    routing {
        /**
         * this route emit prices via sse method
         */
        get("cryptos/prices/live") {
                call.response.cacheControl(CacheControl.NoCache(null))
                call.respondBytesWriter(contentType = ContentType.Text.EventStream) {
                    PriceBroker.priceFlow.collect { cryptoDto ->
                    writeStringUtf8(jsonSerializer.encodeToString(cryptoDto))
                    writeStringUtf8("\n")
                    flush()
                }
            }
        }
        get("cryptos/prices") {
            val fromDateTime =
                call.parameters["from"]?.toLocalDateTime() ?: throwBadRequestEx("from parameter should be filled")
            val toDateTime =
                call.parameters["to"]?.toLocalDateTime() ?: throwBadRequestEx("to parameter should be filled")
            val symbol = call.parameters["symbol"] ?: throwBadRequestEx("symbol parameter should be filled")
            val withMargins = call.parameters["withMargins"]
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

            val result = withMargins?.let {
                CryptoDtoWithCryptoMarginDto(prices = prices)
            } ?: prices
            call.respond(HttpStatusCode.OK, result)
        }
        get("cryptos/last-prices") {
            val cachedLatestPrices = pricesCache.getIfPresent("latestPrices")
            val latestPrices = if (cachedLatestPrices == null) {
                val dbPrices = cryptoService.getLatestPrices()
                pricesCache.put("latestPrices", dbPrices)
                dbPrices
            } else
                cachedLatestPrices
            call.respond(HttpStatusCode.OK, latestPrices)
        }
        get("symbols") {
            call.respond(HttpStatusCode.OK, CryptoCode.entries.map { it.name })
        }
        get("exchanges") {
            call.respond(HttpStatusCode.OK, ReflectionHelper.exchanges.map { it.simpleName })
        }
    }
}