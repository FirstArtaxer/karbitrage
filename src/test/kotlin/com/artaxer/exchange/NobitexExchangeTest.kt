package com.artaxer.exchange

import com.artaxer.service.exchange.NobitexExchange
import kotlin.test.Test
import kotlin.test.assertEquals

class NobitexExchangeTest {
    private val nobitexExchange = NobitexExchange()
    @Test
    fun `nobitex extractor should return latest price`(){
        val extractor = nobitexExchange.getExtractor()
        val result = extractor(originalNobitexResponse)

        val expected = mapOf(
            "BTC" to 64615.74,
            "ETH" to 3418.0,
            "SOL" to 133.585,
            "XRP" to 0.47805,
            "DOGE" to 0.11985,
            "TON" to 7.027
        )

        assertEquals(expected, result)
    }




    val originalNobitexResponse = """
        {
          "status": "ok",
          "stats": {
            "btc-usdt": {
              "isClosed": false,
              "bestSell": "64615.74",
              "bestBuy": "64570.01",
              "volumeSrc": "5.23348157895",
              "volumeDst": "341318.28130968115",
              "latest": "64615.74",
              "mark": "64774.03",
              "dayLow": "64300",
              "dayHigh": "66860",
              "dayOpen": "66513.92",
              "dayClose": "64615.74",
              "dayChange": "-2.85"
            },
            "eth-usdt": {
              "isClosed": false,
              "bestSell": "3418",
              "bestBuy": "3408.66",
              "volumeSrc": "35.94216396765",
              "volumeDst": "123981.9965911749",
              "latest": "3418",
              "mark": "3418.6",
              "dayLow": "3375",
              "dayHigh": "3640",
              "dayOpen": "3551.08",
              "dayClose": "3418",
              "dayChange": "-3.75"
            },
            "sol-usdt": {
              "isClosed": false,
              "bestSell": "133.933",
              "bestBuy": "133.556",
              "volumeSrc": "1016.227767405275",
              "volumeDst": "139665.76294362125",
              "latest": "133.585",
              "mark": "133.756",
              "dayLow": "131",
              "dayHigh": "146.61",
              "dayOpen": "145.769",
              "dayClose": "133.585",
              "dayChange": "-8.36"
            },
            "xrp-usdt": {
              "isClosed": false,
              "bestSell": "0.48076",
              "bestBuy": "0.47862",
              "volumeSrc": "139462.1075319119",
              "volumeDst": "69160.89695944155",
              "latest": "0.47805",
              "mark": "0.48033",
              "dayLow": "0.4774",
              "dayHigh": "0.51998",
              "dayOpen": "0.51191",
              "dayClose": "0.47805",
              "dayChange": "-6.61"
            },
            "doge-usdt": {
              "isClosed": false,
              "bestSell": "0.1198885",
              "bestBuy": "0.1196601",
              "volumeSrc": "1392118.7586990358",
              "volumeDst": "169015.76861211365",
              "latest": "0.11985",
              "mark": "0.1198649",
              "dayLow": "0.11682",
              "dayHigh": "0.1338",
              "dayOpen": "0.1324669",
              "dayClose": "0.11985",
              "dayChange": "-9.52"
            },
            "ton-usdt": {
              "isClosed": false,
              "bestSell": "7.045",
              "bestBuy": "7.031",
              "volumeSrc": "24423.784835583275",
              "volumeDst": "177869.971452518725",
              "latest": "7.027",
              "mark": "7.043",
              "dayLow": "6.85",
              "dayHigh": "7.9",
              "dayOpen": "7.841",
              "dayClose": "7.027",
              "dayChange": "-10.38"
            }
          },
          "global": {
            "binance": {
              "mkr": 2190.4,
              "srm": 0.287,
              "c98": 0.1686,
              "sfp": 0.7393,
              "1000xec": 0.03043,
              "xem": 0.014,
              "zen": 6.078
            }
          }
        }
    """.trimIndent()

}