package com.artaxer.model

import kotlinx.datetime.Instant

enum class FrameScale {
    SECOND, MINUTE, HOUR, DAY, MONTH, YEAR;

    fun upper(): FrameScale? = when (this) {
        SECOND -> MINUTE
        MINUTE -> HOUR
        HOUR -> DAY
        DAY -> MONTH
        MONTH -> YEAR
        YEAR -> null
    }

    fun lower(): FrameScale? = when (this) {
        SECOND -> null
        MINUTE -> SECOND
        HOUR -> MINUTE
        DAY -> HOUR
        MONTH -> DAY
        YEAR -> MONTH
    }

    fun zoomOut(predicate: (FrameScale) -> Boolean, onOverFlow: (FrameScale) -> Unit) {
        if (predicate(this))
            onOverFlow(this)
        this.upper()?.zoomOut(predicate, onOverFlow)
    }

    fun maxSizeInSecond() = when (this) {
        SECOND -> 60
        MINUTE -> 3600    //60 * 60
        HOUR -> 86400     //60 * 60 * 24
        DAY -> 2592000    //60 * 60 * 24 * 30
        MONTH -> 31104000 //60 * 60 * 24 * 30 * 12
        YEAR -> Int.MAX_VALUE
    }
}

data class Node(
    val time: Instant,
    val value: Double
)

class Frame {
    var min: Node? = null
    var max: Node? = null
    var lastResetTime: Instant? = null
    val nodes: MutableList<Node> = mutableListOf()

    fun reset() {
        min = null
        max = null
        lastResetTime = null
    }

    fun append(node: Node): Frame {
        if (min == null) {
            min = node
            max = node
            lastResetTime = node.time
        }
        if (node.value >= max!!.value) {
            max = node
        }
        if (node.value < min!!.value) {
            min = node
        }
        nodes.add(node)
        return this
    }
}