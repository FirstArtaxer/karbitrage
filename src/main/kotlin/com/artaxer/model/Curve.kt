package com.artaxer.model

import kotlinx.coroutines.*
import kotlinx.datetime.Instant
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

class Curve {

    private val scalesMap: Map<FrameScale, Frame> = FrameScale.entries.associateWith { Frame() }
    private var firstNode: Node? = null
    private var lastNode: Node? = null

    fun getNodesBetween(from: Instant, to: Instant, scale: FrameScale): List<Node> {
        return scalesMap[scale]?.nodes?.let { scaledCurve ->
            val startIndex =
                scaledCurve.binarySearch { (it.time.toEpochMilliseconds() - from.toEpochMilliseconds()).toInt() }.let {
                    if (it < 0)
                        -(it + 1)
                    else
                        it
                }
            val endIndex =
                scaledCurve.binarySearch { (it.time.toEpochMilliseconds() - to.toEpochMilliseconds()).toInt() }.let {
                    if (it < 0)
                        -(it + 1)
                    else
                        it
                }
            scaledCurve.subList(startIndex, endIndex)
        }?.toList() ?: emptyList()
    }

    fun getAllNodes(scale: FrameScale = FrameScale.SECOND) = scalesMap[scale]!!.nodes

    fun append(node: Node) {
        if (firstNode == null) {
            firstNode = node
        } else {
            FrameScale.SECOND.zoomOut({
                scalesMap[it]!!.lastResetTime != null &&
                        (node.time.minus(scalesMap[it]!!.lastResetTime!!).inWholeSeconds > it.maxSizeInSecond())
            })
            { overFlowedLevel ->
                val overFlowedCurve = scalesMap[overFlowedLevel]!!
                val max = overFlowedCurve.max!!
                val min = overFlowedCurve.min!!
                overFlowedLevel.upper()?.let {
                    if (max.time > min.time) {
                        scalesMap[it]!!.append(min)
                        scalesMap[it]!!.append(max)
                    } else if (max.time < min.time) {
                        scalesMap[it]!!.append(max)
                        scalesMap[it]!!.append(min)
                    } else {
                        scalesMap[it]!!.append(max)
                    }
                }
                overFlowedCurve.reset()
            }
        }
        scalesMap[FrameScale.SECOND]!!.append(node)
        lastNode = node
    }

}

fun main() {
    val c = Curve()
    repeat(33_000_000) {
        c.append(Node(Instant.fromEpochSeconds(it.toLong()), (Math.random() * 1_000_000)))
    }
    println(c.getAllNodes().size)
    c.getNodesBetween(
        Instant.fromEpochSeconds(11_234),
        Instant.fromEpochSeconds(42_000), FrameScale.HOUR
    ).also {
        println(it.size)
        it.forEach {
            println("${it.time}  ${it.value}")
        }
    }
    println("------------------------------")
    val context = Executors.newFixedThreadPool(200)

    measureTimeMillis {
        runBlocking(context.asCoroutineDispatcher()) {
            (1..1000).map {
                async {
                    c.getNodesBetween(
                        Instant.fromEpochSeconds(7_611_234),
                        Instant.fromEpochSeconds(7_643_807), FrameScale.SECOND
                    ).also {
                        println(it.sumOf { it.value })
                        println("size: ${it.size}")
                    }
                }
            }.awaitAll()
        }
    }.also { println("in $it ms") }
    context.shutdown()

}



