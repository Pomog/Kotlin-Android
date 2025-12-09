package com.prot.test

class Box {
    var length: Int = 0
    var breadth: Int = 0
    var height: Int = 0
    var mass: Int = 0

    constructor(
        length: Int,
        breadth: Int,
        height: Int,
        mass: Int
    ) {
        this.length = length
        this.breadth = breadth
        this.height = height
        this.mass = mass
    }

    fun pack(): String {
        val density = mass.toDouble() / (length * breadth * height)
        return "The box Density is $density"
    }


}