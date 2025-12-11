package com.prot.test

import java.net.HttpURLConnection
import java.net.URI
import kotlin.math.pow

class Scraper() {
    fun getPhaseChangeData(cas: String): String {
        val casTrim = cas.trim()
        val url = URI(
            "https://webbook.nist.gov/cgi/cbook.cgi?ID=${casTrim}&Units=SI&Mask=4&Type=ANTOINE"
        ).toURL()

        val content: String


        val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000

        try {
            content = conn.getInputStream().use { it.reader().readText() }
        } finally {
            conn.disconnect()
        }
        println(content)

        return content
    }

    fun getMaterialName(html: String): String {
        // <title>Methane</title>
        val regex = Regex("""<title>(.+?)</title>""")
        val match = regex.find(html)

        return match?.groupValues?.get(1) ?: "Name Not Found"
    }

    fun parseAntoineAllSimple(html: String): List<AntoineParams> {
        //    <tr class="exp"> ... </tr>
        val rowRegex = Regex(
            """<tr class="exp">(.+?)</tr>""", setOf(RegexOption.DOT_MATCHES_ALL)
        )

        val numberRegex = Regex("""-?\d+(\.\d+)?""")

        val matches = rowRegex.findAll(html)

        return matches.map { match ->
            val rowHtml = match.groupValues[1]

            val nums: List<Double> =
                numberRegex.findAll(rowHtml).map { it.value.toDouble() }.toList()
            require(nums.size >= 5) {
                "Not enough numeric values in Antoine row: $rowHtml"
            }

            val tMin = nums[0]
            val tMax = nums[1]
            val a = nums[2]
            val b = nums[3]
            val c = nums[4]

            AntoineParams(
                a = a, b = b, c = c, tMinK = tMin, tMaxK = tMax
            )
        }.toList()
    }

    fun pSatBar(tk: Double, p: AntoineParams): Double {
        val log10P = p.a - p.b / (tk + p.c)
        return 10.0.pow(log10P)
    }

    fun chooseAntoineParams(pList: List<AntoineParams>, tk: Double): AntoineParams {
        val chosen = pList
            .filter { tk in it.tMinK..it.tMaxK }
            .maxByOrNull { it.tMaxK - it.tMinK } ?: pList.first()
        return chosen
    }

    fun kIdeal(cas: String, tK: Double, pBar: Double): Double {
        val html = getPhaseChangeData(cas)
        val paramsList = parseAntoineAllSimple(html)

        val params = chooseAntoineParams(paramsList, tK)

        val pSat = pSatBar(tK, params)

        return pSat / pBar
    }


}
