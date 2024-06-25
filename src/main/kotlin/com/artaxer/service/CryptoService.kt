package com.artaxer.service

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.serializers.LocalDateTimeIso8601Serializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.abs
import kotlin.math.roundToInt

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
    suspend fun getLatestPrices(): List<CryptoDto> {
        return dbQuery {
            val subQuery = CryptoEntity
                .slice(CryptoEntity.exchange, CryptoEntity.dateTime.max().alias("maxDateTime"))
                .selectAll()
                .groupBy(CryptoEntity.exchange)
                .alias("latestPrices")

            // Alias fields for easier reference
            val subQueryExchange = subQuery[CryptoEntity.exchange]
            val subQueryMaxDateTime = subQuery[CryptoEntity.dateTime.max().alias("maxDateTime")]

            // Perform a join between the original table and the subquery to get the latest prices
            CryptoEntity
                .join(
                    subQuery, JoinType.INNER,
                    onColumn = CryptoEntity.exchange,
                    otherColumn = subQueryExchange
                )
                .select { CryptoEntity.exchange eq subQueryExchange and (CryptoEntity.dateTime eq subQueryMaxDateTime) }
                .flatMap {
                    toCryptoDtoList(
                        exchangeName = it[CryptoEntity.exchange],
                        rawPrices = it[CryptoEntity.prices],
                        dateTime = it[CryptoEntity.dateTime]
                    )
                }
        }
    }
    private fun toCryptoDtoList(
        exchangeName: String,
        rawPrices: String,
        dateTime: LocalDateTime,
        symbol: String? = null
    ): List<CryptoDto> {
        return rawPrices.split(",")
            .filter { pricePerSymbol ->
                symbol?.let {
                    pricePerSymbol.lowercase().startsWith(it.lowercase())
                } ?: true
            }
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

/**
 * Extension function to calculate the margins between different exchanges.
 *
 * @receiver List<CryptoDto> A list of CryptoDto objects representing the cryptocurrency data.
 * @return List<CryptoMarginDto> A list of CryptoMarginDto objects representing the calculated margins.
 *
 * @throws IllegalArgumentException if the list contains multiple symbols.
 *
 * Time Complexity: O(n log n + k^2), where n is the number of CryptoDto objects and
 * k is the maximum size of any group with the same symbol and dateTime.
 */
fun List<CryptoDto>.buildMargins(): List<CryptoMarginDto> {
    if (this.isEmpty()) return emptyList()

    val symbol = this.first().symbol
    if (this.any { it.symbol != symbol }) {
        throw IllegalArgumentException("All CryptoDto objects must have the same symbol")
    }

    val margins = mutableListOf<CryptoMarginDto>()

    // Sort by dateTime
    val sortedDtos = this.sortedWith(compareBy { it.dateTime })

    var start = 0
    while (start < sortedDtos.size) {
        val dateTime = sortedDtos[start].dateTime
        var end = start

        // Find the range of elements with the same dateTime
        while (end < sortedDtos.size && sortedDtos[end].dateTime == dateTime) {
            end++
        }

        // Calculate margins for the current range
        for (i in start until end) {
            for (j in i + 1 until end) {
                val dto1 = sortedDtos[i]
                val dto2 = sortedDtos[j]

                val margin = (abs(dto1.price - dto2.price) * 100).roundToInt() / 100.0
                val percentMargin = ((margin / ((dto1.price + dto2.price) / 2)) * 100 * 100).roundToInt() / 100.0

                margins.add(
                    CryptoMarginDto(
                        exchange1 = dto1.exchange,
                        exchange2 = dto2.exchange,
                        margin = margin,
                        percentMargin = percentMargin,
                        dateTime = dateTime
                    )
                )
            }
        }

        start = end
    }

    return margins
}
@Serializable
data class CryptoMarginDto(
    val exchange1: String,
    val exchange2: String,
    val margin: Double,
    val percentMargin: Double,
    val dateTime: LocalDateTime
)
@Serializable
data class CryptoDtoWithCryptoMarginDto(val prices: List<CryptoDto>) {
    val margins = this.prices.buildMargins()
}