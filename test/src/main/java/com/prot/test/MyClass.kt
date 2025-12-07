package com.prot.test

import java.net.HttpURLConnection
import java.net.URI

fun main() {
    var name: String = "Yurii" // mutable
    name += " OP"

    val b1: Byte = 256.toByte()
    val b2: Byte = 128.toByte()
    val b3: Byte = 255.toByte()


    println("Hello, world! \n $b1 \n$b2 \n$b3")

    simpleGet()
}

fun simpleGet() {
    val url = URI("https://github.com/Pomog").toURL()
    val conn = (url.openConnection() as HttpURLConnection)
    conn.requestMethod = "GET"

    conn.getInputStream()
        .use { it.reader().readText() }
        .also { println(it) }
}