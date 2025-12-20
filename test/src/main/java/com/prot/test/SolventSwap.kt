package com.prot.test

import com.prot.test.data.AntoineParams
import com.prot.test.data.BubblePointResult
import com.prot.test.data.Component
import kotlin.math.pow

class SolventSwap {
    /* Raoult's law
     * yi - vapor content
     * xi - liquid content
     *
     * K=x/y at γ=1 K=Psat(T)/P
     * yi - vapor content
     * partial pressure - yiP
     * yiP = xiPisat(T)
     *
     * bubble point equation: P=∑xiPisat(T)
     * Pisat(T) - pSatBar(tk: Double, p: AntoineParams): Double
     *
     * f(T) = x1 * Psat1(T) + x2 * Psat2(T) - P
     */

    fun bubblePointIdealBinary(
        comp1: Component,
        comp2: Component,
        x1: Double,
        pBar: Double
    ): BubblePointResult {
        require(x1 in 0.0..1.0)
        require(pBar > 0.0)

        val x2 = 1.0 - x1

        val tMin1 = comp1.antoineRows.minOf { it.tMinK }
        val tMax1 = comp1.antoineRows.maxOf { it.tMaxK }
        val tMin2 = comp2.antoineRows.minOf { it.tMinK }
        val tMax2 = comp2.antoineRows.maxOf { it.tMaxK }

        val tLow = maxOf(tMin1, tMin2)
        val tHigh = minOf(tMax1, tMax2)

        require(tLow < tHigh)

        // f(T) = x1 * Psat1(T) + x2 * Psat2(T) - P
        fun f(Tk: Double): Double {
            val p1 = pSatBarWithComponent(Tk, comp1)
            val p2 = pSatBarWithComponent(Tk, comp2)
            return x1 * p1 + x2 * p2 - pBar
        }

        val fa = f(tLow)
        val fb = f(tHigh)

        require(fa * fb <= 0.0) {
            "Bubble point not found in [$tLow, $tHigh] K: f(a)=$fa, f(b)=$fb"
        }

        // TODO: Bisection


        return BubblePointResult(
            tK = 0.0,
            tC = 0.0,
            y1 = 0.0,
            y2 = 0.0,
            k1 = 0.0,
            k2 = 0.0,
        )
    }

    fun pSatBarWithComponent(tk: Double, component: Component): Double {
        val chosen = chooseAntoineParams(component.antoineRows, tk)
        val log10P = chosen.a - chosen.b / (tk + chosen.c)

        return 10.0.pow(log10P) // bar
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
