package com.artaxer.service.event

import kotlinx.datetime.LocalDateTime

open class BaseEvent

interface EventHandler<E : BaseEvent> {
    suspend fun handle(event: E)
}

data class ExchangePriceEvent(
    val name: String,
    val prices: Map<String, Double>,
    val dateTime: LocalDateTime
) : BaseEvent() {
    suspend fun publish() {
        AppEvent.eventDispatcher.dispatch(this)
    }
}