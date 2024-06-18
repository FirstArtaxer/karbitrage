package com.artaxer.service

interface PriceExtractor {
    fun getExtractor(): (String) -> Map<String, Double>
}