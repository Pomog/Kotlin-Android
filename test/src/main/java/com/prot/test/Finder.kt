package com.prot.test

class Finder(private val list: List<String>) {
    fun findItem(elem: String, foundItem: (elem: String?) -> Unit) {
        val item: List<String> = list.filter { it == elem }

        if (item.isNotEmpty()) {
            foundItem(item[0])
        } else {
            foundItem(null)
        }
    }
}