package com.artaxer.service

import io.ktor.client.request.*

/**
 * Interface for creating HTTP requests to a cryptocurrency exchange API.
 */
interface RequestMaker {
    /**
     * Constructs an HTTP request to retrieve data from the exchange.
     *
     * @return A [HttpRequestBuilder] configured to make the request to the exchange's API.
     */
    fun getRequest(): HttpRequestBuilder
}