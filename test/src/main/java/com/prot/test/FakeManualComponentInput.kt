package com.prot.test

class FakeManualComponentInput : ManualComponentInput {
    override fun requestComponent(cas: String, seed: Component?): Component? {
        return null
    }
}