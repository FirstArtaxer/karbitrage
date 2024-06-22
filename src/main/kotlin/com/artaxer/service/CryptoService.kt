package com.artaxer.service

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.serializers.LocalDateTimeIso8601Serializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class CryptoDto(
    val symbol: String,
    val price: Double,
    val exchange: String,
    @Serializable(with = LocalDateTimeIso8601Serializer::class)
    val dateTime: LocalDateTime
)

class CryptoService(private val database: Database) {

    object CryptoEntity : Table() {
        val id = integer("id").autoIncrement()
        val exchange = varchar("exchange", length = 100)
        val prices = varchar("price", length = 255)
        val dateTime = datetime("dateTime")
        override var primaryKey = PrimaryKey(id)
    }
    init {
        transaction(database) {
            SchemaUtils.create(CryptoEntity)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun save(exchangePrices: Triple<String, String, LocalDateTime>) = dbQuery {
        CryptoEntity.insert {
            it[exchange] = exchangePrices.first
            it[prices] = exchangePrices.second
            it[dateTime] = exchangePrices.third
        }[CryptoEntity.id]
    }

    suspend fun getPricesHistory(fromDateTime: LocalDateTime,toDateTime:LocalDateTime,symbol: String): List<CryptoDto> {
        return dbQuery {
            CryptoEntity.select { CryptoEntity.dateTime greaterEq fromDateTime and(CryptoEntity.dateTime lessEq toDateTime) }
                .flatMap {
                    toCryptoDtoList(
                        exchangeName = it[CryptoEntity.exchange],
                        rawPrices = it[CryptoEntity.prices],
                        dateTime = it[CryptoEntity.dateTime],
                        symbol = symbol
                    )
                }
        }
    }
    private fun toCryptoDtoList(
        exchangeName: String,
        rawPrices: String,
        dateTime: LocalDateTime,
        symbol: String
    ): List<CryptoDto> {
        return rawPrices.split(",")
            .filter { it.lowercase().startsWith(symbol.lowercase()) }
            .map {
                val codeAndPrice = it.split("=")
                CryptoDto(
                    symbol = codeAndPrice.first(),
                    price = codeAndPrice.last().toDouble(),
                    exchange = exchangeName,
                    dateTime = dateTime.truncateToMinute()
                )
            }
    }
}

/**
 * Extension function for kotlinx.datetime.LocalDateTime which sets seconds and nanoseconds to 00.
 */
fun LocalDateTime.truncateToMinute(): LocalDateTime {
    return LocalDateTime(
        this.year,
        this.month,
        this.dayOfMonth,
        this.hour,
        this.minute,
        0,
        0
    )
}