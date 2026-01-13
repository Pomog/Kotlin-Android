package com.prot.test

class ComponentServiceImpl(
    private val repo: ComponentRepository,
    private val scraper: ComponentScraper,
    private val manualInput: ManualComponentInput
) : ComponentService {

    override fun getComponent(cas: String): Component {
        val key = cas.trim()

        // 1) Try DB
        repo.findByCas(key)?.let { return it }

        // 2) Try scraper (catch exceptions)
        val scraped: Component? = runCatching { scraper.fetchComponent(key) }
            .getOrElse { null }
        if (scraped != null && isComplete(scraped, key)) {
            repo.saveComponent(scraped) // upsert
            return scraped
        }

        // 3) Manual input (UI/CLI)
        val manual = manualInput.requestComponent(key, seed = scraped)
            ?: error("Component not available: manual input cancelled for CAS=$key")

        require(isComplete(manual, key)) {
            "Manual input returned incomplete or invalid component for CAS=$key"
        }

        repo.saveComponent(manual)
        return manual
    }

    // TODO: return error message with a list
    private fun isComplete(c: Component, expectedCas: String): Boolean {
        if (c.cas.trim() != expectedCas) return false
        if (c.name.isBlank()) return false
        if (!c.mw.isFinite() || c.mw <= 0.0) return false
        if (!c.density.isFinite() || c.density <= 0.0) return false
        if (c.antoineRows.isEmpty()) return false
        return true
    }

}