package com.artaxer.influxDB

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.influxdb.client.write.Point
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.time.Instant
import kotlin.system.measureTimeMillis
import kotlin.test.Test


class InfluxWriteTest {

    @Test
    fun `write 100_000 record on influxDB by 1000 async-block`() {
        val client = InfluxDBClientKotlinFactory.create("http://localhost:8086", token.toCharArray(), org, bucket)
        var sec = 0L
        measureTimeMillis {
            runBlocking {
                (1..1000)
                    .map {
                        async {
                            repeat(100) {
                                val writeApi = client.getWriteKotlinApi()
                                writeApi.writePoint(
                                    Point
                                        .measurement("msr")
                                        .addTag("coin", "some-coin-name-${it % 7}")
                                        .addField("price", Math.random())
                                        .time(Instant.now().minusSeconds(sec++), WritePrecision.S)
                                )
                            }
                        }
                    }.awaitAll()
            }
        }.also {
            println("100_000 records inserted in $it ms")
        }
    }

}

