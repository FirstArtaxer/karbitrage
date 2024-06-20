package com.artaxer.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

class PriceService {
    private val client = HttpClient(CIO) {
        install(ContentEncoding) {
            gzip()
        }
    }
    suspend fun getPrices(
        httpRequest: HttpRequestBuilder,
        extractor: (String) -> Map<String, Double>
    ): Map<String, Double> {
        val response: HttpResponse = client.request(httpRequest)
        return extractor.invoke(response.body())
    }
}

