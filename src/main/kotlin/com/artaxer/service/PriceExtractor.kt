package com.artaxer.service

import kotlinx.serialization.json.Json

/**
 * Abstract class for extracting price data from a cryptocurrency exchange.
 */
abstract class PriceExtractor {
    val jsonParser = Json { ignoreUnknownKeys = true }
    /**
     * Gets a function that processes the API response from the exchange.
     *
     * @return A function that takes a string response from the exchange's API and returns
     *         a map where the key is the cryptocurrency symbol and the value is the price.
     *         This function extracts the relevant data from the API response string.
     */
    abstract fun getExtractor(): (String) -> Map<String, Double>
}

enum class CryptoCode {
    BTC, ETH, SOL, XRP, DOGE, TON, PEPE, TRX
}