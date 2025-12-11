package com.prot.test

fun main() {
    val scraper = Scraper()

    val html = scraper.getPhaseChangeData("74-82-8")
    val paramsList = scraper.parseAntoineAllSimple(html)

    val parms = scraper.chooseAntoineParams(paramsList, 40.0)
    val boilingPoint = scraper.pSatBar(56.toDouble(), parms)

    println(boilingPoint)


}