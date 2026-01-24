package com.prot.tipappdemo.util

import android.util.Log

fun calculateTotalTip(totalBill: Double, tipPercentage: Int): Double {
    val bill = totalBill * tipPercentage / 100
    Log.d("TAG", "calculateTotalTip: $bill")

    return if (totalBill > 1 && totalBill.toString().isNotEmpty()) {
        bill
    } else {
        return 0.0
    }
}

fun calculateTotalPerPerson(
    totalBill: Double,
    splitBy: Int,
    tipPercentage: Int
): Double {
    val bill = calculateTotalTip(
        totalBill = totalBill,
        tipPercentage = tipPercentage
    ) + totalBill

    return (bill / splitBy)
}