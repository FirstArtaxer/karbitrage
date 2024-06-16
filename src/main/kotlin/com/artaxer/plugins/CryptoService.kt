package com.artaxer.plugins

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
    val code: String,
    val price: Double,
    @Serializable(with = LocalDateTimeIso8601Serializer::class)
    val dateTime: LocalDateTime
)

class CryptoService(private val database: Database) {

    object CryptoEntity : Table() {
        val id = integer("id").autoIncrement()
        val code = varchar("code", length = 10)
        val price = double("price")
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

    suspend fun create(cryptoDto: CryptoDto): Int = dbQuery {
        CryptoEntity.insert {
            it[code] = cryptoDto.code
            it[price] = cryptoDto.price
            it[dateTime] = cryptoDto.dateTime
        }[CryptoEntity.id]
    }

    suspend fun read(id: Int): CryptoDto? {
        return dbQuery {
            CryptoEntity.select { CryptoEntity.id eq id }
                .map { CryptoDto(code = it[CryptoEntity.code], price = it[CryptoEntity.price], dateTime = it[CryptoEntity.dateTime]) }
                .singleOrNull()
        }
    }
}