package com.artaxer.service.exchange

import com.artaxer.service.CryptoCode
import com.artaxer.service.PriceExtractor
import com.artaxer.service.RequestMaker
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * This class belongs to bitpin exchange
 */

class BitpinExchange : RequestMaker, PriceExtractor() {
    override fun getExtractor(): (String) -> Map<String, Double> {
        return {
            val jsonObject = jsonParser.parseToJsonElement(it)
            val items = jsonParser.decodeFromJsonElement<BitpinDto>(jsonObject).results
            items.mapNotNull { item ->
                val crypto =
                    CryptoCode.entries.find { cryptoCode -> item.code.lowercase() == "${cryptoCode.name.lowercase()}_usdt" }
                        ?: return@mapNotNull null
                crypto.name to (item.internal_price_info.price?.toDouble()?:0.toDouble())
            }.toMap()
        }
    }

    override fun getRequest(): HttpRequestBuilder {
        return HttpRequestBuilder().apply {
            method = HttpMethod.Get
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.UserAgent, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
            }
            url("https://api.bitpin.org/v1/mkt/markets/")
        }
    }
}
@Serializable
data class BitpinDto(
    val results: List<Result>
)
@Serializable
data class Result(
    val all_time_high: String,
    val circulating_supply: String,
    val code: String,
    val freshness_weight: Int,
    val internal_price_info: InternalPriceInfo,
    val market_cap: String,
    val otc_buy_percent: String,
    val otc_market: Boolean,
    val otc_max_buy_amount: String,
    val otc_max_sell_amount: String,
    val otc_sell_percent: String,
    val popularity_weight: Int,
    val price: String,
    val text: String,
    val title: String,
    val title_fa: String,
    val tradable: Boolean,
    val trading_view_source: String,
    val trading_view_symbol: String,
    val volume_24h: String
)
@Serializable
data class InternalPriceInfo(
    val change: Double ?,
    val max: String?,
    val min: String?,
    val price: String?,
)