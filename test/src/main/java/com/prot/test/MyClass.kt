package com.prot.test

import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.URI

fun main(): Unit = runBlocking {
    var name: String = "Yurii" // mutable
    name += " OP"

    val b1: Byte = 256.toByte()
    val b2: Byte = 128.toByte()
    val b3: Byte = 255.toByte()


    println("Hello, world! \n $b1 \n$b2 \n$b3")

    val body = simpleGet()
    println(body)

    val add: (Int, Int) -> Int = { a, b -> a + b }

    message(message = "Hello") {
        add(1, 2)
    }

    val myList = listOf("Yurii", "Anna", "Nelya")
    val mutableList = mutableListOf("Yurii", "Anna", "Nelya")
    mutableList.add(0, "Cat")

    for (item in myList) {
        println(item)
    }

    mutableList.forEach { println(it) }
    println(myList.size)
}

suspend fun simpleGet() {
    val url = URI("https://github.com/Pomog").toURL()
    val conn: HttpURLConnection = (url.openConnection() as HttpURLConnection)
    conn.requestMethod = "GET"
    conn.connectTimeout = 5000

    try {
        conn.getInputStream().use { it.reader().readText() }.also { println(it) }
    } finally {
        conn.disconnect()
    }
}

fun message(message: String, funAsParameter: () -> Int) {
    println("$message ${funAsParameter()}")
}

