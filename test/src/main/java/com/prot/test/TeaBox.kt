package com.prot.test

class TeaBox(var teaMass: Int) :
    Box(
        length = 1, breadth = 1, height = 1
    ) {
    fun getTea(): String {
        val pack = pack()
        return "TeaBox: $pack \nTeaMass: $teaMass"
    }
}