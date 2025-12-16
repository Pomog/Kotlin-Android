package com.prot.test

class Finder<T>(private val list: List<T>) {
    fun findItem(elem: T, foundItem: (elem: T?) -> Unit) {
        val item = list.firstOrNull { it == elem }
        foundItem(item)
    }
}