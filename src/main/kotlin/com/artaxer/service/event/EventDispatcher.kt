package com.artaxer.service.event

import com.artaxer.service.CryptoService

class EventDispatcher {
    private val handlers = mutableMapOf<Class<out BaseEvent>, MutableList<EventHandler<out BaseEvent>>>()

    fun <E : BaseEvent> register(eventClass: Class<E>, handler: EventHandler<E>) {
        handlers.computeIfAbsent(eventClass) { mutableListOf() }
            .add(handler)
    }

    suspend fun <E : BaseEvent> dispatch(event: E) {
        handlers[event::class.java]?.forEach { handler ->
            @Suppress("UNCHECKED_CAST")
            (handler as EventHandler<E>).handle(event)
        }
    }
}

object AppEvent {
    val eventDispatcher: EventDispatcher by lazy {
        val eventDispatcher = EventDispatcher()
        eventDispatcher.register(ExchangePriceEvent::class.java, CryptoService())
        eventDispatcher
    }
}