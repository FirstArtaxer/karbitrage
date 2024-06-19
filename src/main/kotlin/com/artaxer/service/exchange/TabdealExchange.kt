package com.artaxer.service.exchange

import com.artaxer.service.CryptoCode
import com.artaxer.service.PriceExtractor
import com.artaxer.service.RequestMaker
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * This class belongs to tabdeal exchange
 */
class TabdealExchange : RequestMaker, PriceExtractor() {
    override fun getExtractor(): (String) -> Map<String, Double> {
        return {
            val jsonObject = jsonParser.parseToJsonElement(it)
            val items = jsonParser.decodeFromJsonElement<List<TabdealDtoItem>>(jsonObject)
            items.flatMap { item -> item.markets }
                .mapNotNull { market ->
                    val crypto =
                        CryptoCode.entries.find { cryptoCode -> market.symbol.lowercase() == "${cryptoCode.name.lowercase()}_usdt" }
                            ?: return@mapNotNull null
                    crypto.name to market.last_trade_price.toDouble()
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
            url("https://api-web.tabdeal.org/r/plots/currency_prices")
        }
    }
}


@Serializable
data class TabdealDtoItem(
    val change_percent: String,
    val created: String,
    val markets: List<Market>,
    val name: String,
    val name_fa: String,
    val price_in_usdt: String,
    val symbol: String,
    val usdt_volume: String,
    val volume: String
)

@Serializable
data class Market(
    val change_percent: String,
    val high: String,
    val last_trade_price: String,
    val low: String,
    val name_fa: String,
    val price: String,
    val symbol: String,
    val usdt_volume: String,
    val volume: String
)