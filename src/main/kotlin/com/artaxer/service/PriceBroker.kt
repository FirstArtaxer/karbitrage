package com.artaxer.service

import com.artaxer.ReflectionHelper
import com.artaxer.context
import io.ktor.util.logging.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

object PriceBroker {
    private val logger = KtorSimpleLogger("com.artaxer.service.PriceBroker")
    // replay size is equal to size of all prices that extracted each minute
    private val replaySize = CryptoCode.entries.size * ReflectionHelper.exchanges.size
    private val _priceFlow = MutableSharedFlow<CryptoDto>(replay = replaySize)
    val priceFlow: SharedFlow<CryptoDto> get() = _priceFlow.asSharedFlow()
    fun produce(message: CryptoDto) {
        CoroutineScope(context = context).launch {
            try {
                _priceFlow.emit(message)
            } catch (e: Exception) {
                logger.error("Error emitting price: ${e.message}")
            }
        }
    }
}