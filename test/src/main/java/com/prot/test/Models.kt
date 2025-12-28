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