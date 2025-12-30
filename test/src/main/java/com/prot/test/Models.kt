package com.prot.test

data class Component(
    val name: String,
    val cas: String,
    val mw: Double,
    val density: Double,
    val antoineRows: List<AntoineParams>,
)

data class VaporLiquidResult(
    val y1: Double,
    val y2: Double,
    val k1: Double,
    val k2: Double,
)

data class BubblePointResult(
    val tK: Double?,
    val tC: Double,
    val y1: Double,
    val y2: Double,
    val k1: Double,
    val k2: Double,
)

data class AntoineParams(
    val a: Double,
    val b: Double,
    val c: Double,
    val tMinK: Double,
    val tMaxK: Double,
)

data class HttpTimeouts(
    val connectMs: Int = 7000,
    val readMs: Int = 7000
)

data class HttpGetRequest(
    val baseUrl: String,
    val path: String,
    val query: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = mapOf(
        "User-Agent" to "Mozilla/5.0"
    ),
    val timeouts: HttpTimeouts = HttpTimeouts()
)