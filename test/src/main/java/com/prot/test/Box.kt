package com.prot.test

open class Box {
    var length: Int = 0
    var breadth: Int = 0
    var height: Int = 0
    val mass: Int = 10

    constructor(
        length: Int,
        breadth: Int,
        height: Int,
    ) {
        this.length = length
        this.breadth = breadth
        this.height = height
    }

    fun pack(): String {
        val density = mass.toDouble() / (length * breadth * height)
        return "The box Density is $density"
    }

}