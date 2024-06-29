package com.artaxer.service.exchange

import com.artaxer.service.CryptoCode
import com.artaxer.service.PriceExtractor
import com.artaxer.service.RequestMaker
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * This class belongs to coinex exchange
 */
class CoinexExchange : RequestMaker, PriceExtractor() {
    override fun getExtractor(): (String) -> Map<String, Double> {
        return {
            val jsonObject = jsonParser.parseToJsonElement(it)
            val items = jsonParser.decodeFromJsonElement<CoinexDto>(jsonObject).data?.data
            items?.mapNotNull { item ->
                val crypto =
                    CryptoCode.entries.find { cryptoCode -> item.asset == cryptoCode.name } ?: return@mapNotNull null
                crypto.name to (item.price_usd?.toDouble() ?: 0.toDouble())
            }?.toMap()?: mapOf()
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
            url("https://www.coinex.com/res/quotes/rank/assets?sort_type=volume_usd&offset=0&limit=25&is_kline=true")
        }
    }

}

@Serializable
data class CoinexDto(
    val code: Int? = null,
    val `data`: Data? = null,
    val message: String? = null
)

@Serializable
data class Data(
    val count: Int? = null,
    val `data`: List<DataX> = emptyList(),
    val total: Int? = null,
)

@Serializable
data class DataX(
    val all_change_rate: String? = null,
    val asset: String? = null,
    val change_rate: String? = null,
    val change_rate_30d: String? = null,
    val circulation: String? = null,
    val circulation_usd: String? = null,
    val circulation_usd_rank: Int? = null,
    val full_name: String? = null,
    val is_st: Boolean? = null,
    val online_time: Int? = null,
    val price_usd: String? = null,
    val ts: Int? = null,
    val volume_usd: String? = null
)