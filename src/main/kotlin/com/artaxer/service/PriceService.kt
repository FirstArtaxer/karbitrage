package com.artaxer.service

import arrow.resilience.Schedule
import arrow.resilience.retry
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.request.*
import kotlin.time.Duration.Companion.milliseconds

class PriceService {
    private val client = HttpClient(CIO) {
        install(ContentEncoding) {
            gzip()
        }
    }
    private val schedule = Schedule.recurs<Throwable>(3) // Retry 3 times
        .and(Schedule.exponential(100.milliseconds)) // Exponential backoff starting from 100ms

    suspend fun getPrices(
        httpRequest: HttpRequestBuilder,
        extractor: (String) -> Map<String, Double>
    ): Map<String, Double> {
        val response = schedule.retry { client.request(httpRequest) }
        return extractor.invoke(response.body())
    }
}

