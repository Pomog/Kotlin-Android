package com.prot.test

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs
import kotlin.math.pow

class Scraper() : ComponentScraper {

    private val nistChemistryWebBookBaseUrl = "https://webbook.nist.gov"
    private val nistChemistryWebBookPath = "/cgi/cbook.cgi"

    private val pubChemBaseUrl = "https://pubchem.ncbi.nlm.nih.gov"

    private val pubChemPugViewPath = "/rest/pug_view/data/compound"

    private val pubChemCompoundApiPath = "/rest/pug/compound"

    private fun encodePathSegment(s: String): String =
        URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private fun encode(s: String): String = URLEncoder.encode(s, "UTF-8")

    override fun fetchComponent(cas: String): Component? = runCatching {
        val key = cas.trim()

        val html = getPhaseChangeData(key)
        val allAntoine = parseAntoineAllSimple(html)
        val name = getMaterialName(html).trim()
        val mw = getMW(html)

        val relDensity = runCatching {
            getRelativeDensityWater1ByName(name, mw)
        }.getOrElse {
            Double.NaN
        }

        Component(
            name = name,
            cas = key,
            mw = mw,
            density = relDensity,
            antoineRows = allAntoine
        )
    }.getOrNull()

    /**
     * PUG-REST: CID list -> MW values (to disambiguate)
     * GET /rest/pug/compound/cid/{cid1,cid2,...}/property/MolecularWeight/JSON
     */
    private fun fetchMwForCids(cids: List<Long>): Map<Long, Double> {
        val cidList = cids.joinToString(",")

        val req = HttpGetRequest(
            baseUrl = pubChemBaseUrl,
            path = "$pubChemCompoundApiPath/cid/$cidList/property/MolecularWeight/JSON"
        )

        val body = httpGet(req)

        // {"CID":180,"MolecularWeight":58.08,...}
        val rx = Regex(
            """\{[^{}]*"CID"\s*:\s*(\d+)[^{}]*"MolecularWeight"\s*:\s*([0-9]+(?:\.[0-9]+)?)""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )

        val map = mutableMapOf<Long, Double>()
        for (m in rx.findAll(body)) {
            val cid = m.groupValues[1].toLong()
            val mw = m.groupValues[2].toDouble()
            map[cid] = mw
        }

        if (map.isEmpty()) error("Could not parse MW table. Response: $body")
        return map
    }

    /**
     * Choose best CID using expected MW (from NIST).
     */
    private fun nameToBestCidByMw(name: String, expectedMw: Double): Long {
        val cids = nameToCids(name)
        if (cids.size == 1) return cids.first()

        val mwMap = fetchMwForCids(cids)

        return mwMap.entries
            .minByOrNull { (_, mw) -> abs(mw - expectedMw) / expectedMw }
            ?.key
            ?: cids.first()
    }

    /**
     * PUG-View: CID -> Density section strings -> extract "Relative density (water = 1): X"
     * GET /rest/pug_view/data/compound/{cid}/JSON?heading=Density
     */
    private fun getRelativeDensityWater1FromCid(cid: Long): Double {
        val req = HttpGetRequest(
            baseUrl = pubChemBaseUrl,
            path = "$pubChemPugViewPath/$cid/JSON",
            query = mapOf("heading" to "Density")
        )

        val body = httpGet(req)

        val strings = Regex(
            """"String"\s*:\s*"((?:\\.|[^"\\])*)"""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        ).findAll(body).map { unescapeJsonString(it.groupValues[1]) }.toList()

        val line = strings.firstOrNull {
            it.contains("Relative density", ignoreCase = true) &&
                    it.contains("water", ignoreCase = true)
        } ?: error("Relative density (water=1) not found for CID=$cid")

        val m = Regex(
            """Relative\s+density\s*\(water\s*=\s*1\)\s*:\s*([0-9]+(?:\.[0-9]+)?)""",
            setOf(RegexOption.IGNORE_CASE)
        ).find(line) ?: error("Could not parse relative density from: '$line'")

        return m.groupValues[1].toDouble()
    }

    /**
     * Minimal JSON string unescape for values coming from "String": "..."
     */
    private fun unescapeJsonString(s: String): String {

        return s.replace("""\n""", "\n")
            .replace("""\t""", "\t")
            .replace("""\"""", "\"")
            .replace("""\\//""", "//")
            .replace("""\\\\""", "\\")
    }


    /**
     * PUG-REST: name -> list of CIDs
     * GET /rest/pug/compound/name/{name}/cids/JSON
     */
    private fun nameToCids(name: String): List<Long> {
        val nm = encodePathSegment(name.trim())

        val req = HttpGetRequest(
            baseUrl = pubChemBaseUrl,
            path = "$pubChemCompoundApiPath/name/$nm/cids/JSON"
        )

        val body = httpGet(req)

        // {"IdentifierList":{"CID":[180,86235482]}}
        val listBlock = Regex(
            """"CID"\s*:\s*\[(.*?)]""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        ).find(body)?.groupValues?.get(1)
            ?: error("CID list not found for name='$name'. Response: $body")

        val cids = Regex("""\d+""").findAll(listBlock).map { it.value.toLong() }.toList()
        if (cids.isEmpty()) error("No CIDs parsed for name='$name'. Response: $body")
        return cids
    }

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
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.use { it.reader().readText() } ?: ""
            if (code !in 200..299) error("HTTP $code for $url\n$body")
            body
        } finally {
            conn.disconnect()
        }
    }


    private fun getPhaseChangeData(cas: String): String {
        val casTrim = cas.trim()

        val req = HttpGetRequest(
            baseUrl = nistChemistryWebBookBaseUrl,
            path = nistChemistryWebBookPath,
            query = mapOf(
                "ID" to casTrim,
                "Units" to "SI",
                "Mask" to "4",
                "Type" to "ANTOINE"
            )
        )

        return httpGet(req)
    }


    private fun getMaterialName(html: String): String {
        // <title>Methane</title>
        val regex = Regex("""<title>([^0-9]+?)</title>""")
        val match = regex.find(html)

        val raw = match?.groupValues?.get(1)?.trim() ?: "Name Not Found"
        return raw.replace(Regex("""\s*-\s*NIST.*$"""), "").trim()
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

    private fun parseAntoineAllSimple(html: String): List<AntoineParams> {
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

    /**
     * Convenience: name -> best CID (by MW) -> relative density (water=1)
     */
    private fun getRelativeDensityWater1ByName(name: String, expectedMw: Double): Double {
        val cid = nameToBestCidByMw(name, expectedMw)
        return getRelativeDensityWater1FromCid(cid)
    }

    /**
     * K=x/y at γ=1 K=Psat(T)/P
     */
    private fun kIdeal(component: Component, tK: Double, pBar: Double): Double {
        val params = chooseAntoineParams(component.antoineRows, tK)
        val pSatBar = pSatBar(tK, params)
        return pSatBar / pBar
    }

    /**
     * Ideal binary solution: γ=1.
     * Input: T (K), P (bar), x1, x2.
     * Output: vapor composition (y1,y2) and K-values.
     */
    private fun vaporCompositionIdeal(
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


    private fun pSatBar(tk: Double, p: AntoineParams): Double {
        val log10P = p.a - p.b / (tk + p.c)
        return 10.0.pow(log10P)
    }

    private fun kIdeal(cas: String, tK: Double, pBar: Double): Double {
        val html = getPhaseChangeData(cas)
        val paramsList = parseAntoineAllSimple(html)

        val params = chooseAntoineParams(paramsList, tK)

        val pSat = pSatBar(tK, params)

        return pSat / pBar
    }

    private fun chooseAntoineParams(
        pList: List<AntoineParams>,
        tk: Double
    ): AntoineParams {
        val chosen = pList
            .filter { tk in it.tMinK..it.tMaxK }
            .maxByOrNull { it.tMaxK - it.tMinK } ?: pList.first()
        return chosen
    }
}


