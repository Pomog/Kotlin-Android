package com.prot.test

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs
import kotlin.math.pow

class Scraper() {

    private val NIST_BASE = "https://webbook.nist.gov"
    private val NIST_CBOOK_PATH = "/cgi/cbook.cgi"

    private fun encode(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun buildUrl(req: HttpGetRequest): URL {
        val base = req.baseUrl.trimEnd('/')
        val path = req.path.trimStart('/')

        val queryStr = if (req.query.isEmpty()) {
            ""
        } else {
            req.query.entries.joinToString("&", prefix = "?") { (k, v) ->
                "${encode(k)}=${encode(v)}"
            }
        }

        val full = "$base/$path$queryStr"
        return URI(full).toURL()
    }

    private fun getConnection(url: URL, req: HttpGetRequest): HttpURLConnection {
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = req.timeouts.connectMs
        conn.readTimeout = req.timeouts.readMs

        req.headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }

        return conn
    }

    private fun httpGet(req: HttpGetRequest): String {
        val url = buildUrl(req)
        val conn = getConnection(url, req)

        return try {
            conn.inputStream.use { it.reader().readText() }
        } finally {
            conn.disconnect()
        }
    }


    fun getPhaseChangeData(cas: String): String {
        val casTrim = cas.trim()

        val req = HttpGetRequest(
            baseUrl = NIST_BASE,
            path = NIST_CBOOK_PATH,
            query = mapOf(
                "ID" to casTrim,
                "Units" to "SI",
                "Mask" to "4",
                "Type" to "ANTOINE"
            )
        )

        return httpGet(req)
    }


    fun getMaterialName(html: String): String {
        // <title>Methane</title>
        val regex = Regex("""<title>([^0-9]+?)</title>""")
        val match = regex.find(html)

        return match?.groupValues?.get(1) ?: "Name Not Found"
    }

    fun getMW(html: String): Double {
        val mwRegex = Regex(
            """Molecular\s+weight</a>:</strong>\s*([0-9]+(?:\.[0-9]+)?)"""
        )

        val match = mwRegex.find(html)
            ?: error("Molecular weight not found in NIST HTML")

        val mwStr = match.groupValues[1]
        return mwStr.toDouble()
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

    fun getComponent(cas: String): Component {
        val html = getPhaseChangeData(cas)
        val allAntoine = parseAntoineAllSimple(html)
        val name = getMaterialName(html)
        val mw = getMW(html)


        val component = Component(
            name = name,
            cas = cas,
            mw = mw,
            density = 0.0, // TODO: add density
            antoineRows = allAntoine
        )
        return component
    }

    /**
     * K=x/y at γ=1 K=Psat(T)/P
     */
    fun kIdeal(component: Component, tK: Double, pBar: Double): Double {
        val params = chooseAntoineParams(component.antoineRows, tK)
        val pSatBar = pSatBar(tK, params)
        return pSatBar / pBar
    }

    /**
     * Ideal binary solution: γ=1.
     * Input: T (K), P (bar), x1, x2.
     * Output: vapor composition (y1,y2) and K-values.
     */
    fun vaporCompositionIdeal(
        comp1: Component,
        comp2: Component,
        tK: Double,
        pBar: Double,
        x1: Double,
        x2: Double
    ): VaporLiquidResult {
        require(abs(x1 + x2 - 1.0) < 1e-6) { "x1 + x2 must be 1" }
        val k1 = kIdeal(comp1, tK, pBar)
        val k2 = kIdeal(comp2, tK, pBar)

        val denom = k1 * x1 + k2 * x2

        val y1 = k1 * x1 / denom
        val y2 = k2 * x2 / denom

        return VaporLiquidResult(y1, y2, k1, k2)
    }


    fun pSatBar(tk: Double, p: AntoineParams): Double {
        val log10P = p.a - p.b / (tk + p.c)
        return 10.0.pow(log10P)
    }

    fun kIdeal(cas: String, tK: Double, pBar: Double): Double {
        val html = getPhaseChangeData(cas)
        val paramsList = parseAntoineAllSimple(html)

        val params = chooseAntoineParams(paramsList, tK)

        val pSat = pSatBar(tK, params)

        return pSat / pBar
    }

    fun chooseAntoineParams(
        pList: List<AntoineParams>,
        tk: Double
    ): AntoineParams {
        val chosen = pList
            .filter { tk in it.tMinK..it.tMaxK }
            .maxByOrNull { it.tMaxK - it.tMinK } ?: pList.first()
        return chosen
    }


}


