package com.artaxer.service.exchange

import com.artaxer.service.CryptoCode
import com.artaxer.service.PriceExtractor
import com.artaxer.service.RequestMaker
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * This class belongs to nobitex exchange
 */

class NobitexExchange : RequestMaker, PriceExtractor() {
    override fun getExtractor(): (String) -> Map<String, Double> {
        return {
            val jsonObject = jsonParser.parseToJsonElement(it).jsonObject
            val stats = jsonObject["stats"]?.jsonObject ?: error("Invalid JSON structure")
            stats.entries
                .mapNotNull { set ->
                    val crypto =
                        CryptoCode.entries.find { cryptoCode ->
                            set.key.lowercase().contains(cryptoCode.name.lowercase())
                        } ?: return@mapNotNull null
                    val price = set.value.jsonObject["latest"]?.jsonPrimitive?.double ?: 0.0
                    crypto.name to price
                }.toMap()
        }
    }

    override fun getRequest(): HttpRequestBuilder {
        return HttpRequestBuilder().apply {
            method = HttpMethod.Get
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(
                    HttpHeaders.UserAgent,
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
                )
            }
            url("https://api.nobitex.ir/market/stats?srcCurrency=btc,eth,sol,xrp,doge,ton&dstCurrency=usdt")
        }
    }
}