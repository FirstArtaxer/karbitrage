package com.artaxer.influxDB

import com.influxdb.client.kotlin.InfluxDBClientKotlin
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.influxdb.client.kotlin.QueryKotlinApi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import java.math.BigDecimal
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis
import kotlin.test.Test

/**
 * start influx docker :
 * docker run -p 8086:8086 -v "$PWD/data:/var/lib/influxdb2" -v "$PWD/config:/etc/influxdb2" influxdb
 *
 *
 * */

val token = "ieIHiM3g2zQ433GHcHfPa8j-CCD36LGWm4J2G34Q8qYjFEAcVJVSYUje0ycI1jebjfoLCy31kt_3maRGerv1ag=="
val org = "org"
val bucket = "buk2"

object Pool {
    val channel = Channel<QueryKotlinApi>(1001)
    private val clients = mutableListOf<InfluxDBClientKotlin>()
    private const val clientSize = 10

    init {
        repeat(clientSize) {
            val client = InfluxDBClientKotlinFactory.create("http://localhost:8086", token.toCharArray(), org, bucket)
            clients.add(client)
        }
        repeat(100) {
            channel.trySend(clients[it % clientSize].getQueryKotlinApi())
        }
    }

    suspend inline fun queryApi(handler: (QueryKotlinApi) -> Unit) {
        val borrowed = channel.receive()
        try {
            handler(borrowed)
        } finally {
            channel.trySend(borrowed)
        }
    }

}

class InfluxReadTest {

    @Test
    fun `read `() {
        val context = Executors.newFixedThreadPool(200).asCoroutineDispatcher()
        runBlocking(context) {
            repeat(1) { x ->
                (1..1000).map {
                    async {
                        //delay(10L*it)
                        readData(it)
                    }
                }.awaitAll()
            }
        }
    }

    private suspend fun readData(i: Int) {
        println("start-$i:${Thread.currentThread()}")
        var sum = BigDecimal.ZERO
        var count = 1
        val millis = measureTimeMillis {
            Pool.queryApi { api ->
                api.query(
                    """from(bucket: "buk2")
                      |> range(start: -111d)
                      |> filter(fn: (r) => r.coin == "some-coin-name-3" )"""
                ).consumeAsFlow()
                    .collect {
                        sum = sum.plus((it.value as Double).toBigDecimal())
                        count++
                    }
            }
        }
        println("${Thread.currentThread()} | $millis ms | count: $count | sum: $sum")
    }

}