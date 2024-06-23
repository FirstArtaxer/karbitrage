package com.artaxer.service

import kotlinx.serialization.json.Json

abstract class PriceExtractor {
    val jsonParser = Json { ignoreUnknownKeys = true }
    abstract fun getExtractor(): (String) -> Map<String, Double>
}

enum class CryptoCode {
    BTC, ETH, SOL, XRP, DOGE, TON, PEPE, TRX
}