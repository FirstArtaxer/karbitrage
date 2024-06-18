package com.artaxer.service

interface PriceExtractor {
    fun getExtractor(): (String) -> Map<String, Double>
}

enum class CryptoCode {
    BTC, ETH, SOL, XRP, DOGE, TON
}