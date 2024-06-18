package com.artaxer.service

import io.ktor.client.request.*

interface RequestMaker {
    fun getRequest(): HttpRequestBuilder
}