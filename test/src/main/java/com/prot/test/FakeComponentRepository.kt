package com.prot.test

class FakeComponentRepository : ComponentRepository {
    override fun findByCas(cas: String): Component? {
        return null
    }

    override fun saveComponent(component: Component) {
    }
}