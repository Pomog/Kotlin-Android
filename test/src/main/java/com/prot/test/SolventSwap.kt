package com.prot.test

import kotlin.math.abs
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

    val ITERATIONS = 80

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

        var a = tLow
        var b = tHigh
        var fa = f(tLow)
        var fb = f(tHigh)

        require(fa * fb <= 0.0) {
            "Bubble point not found in [$tLow, $tHigh] K: f(a)=$fa, f(b)=$fb"
        }

        // Bisection
        repeat(ITERATIONS) {
            val c = 0.5 * (a + b)
            val fc = f(c)

            if (abs(fc) < 1e-9 || (b - a) < 1e-6) {
                return buildBubbleResult(comp1, comp2, x1, x2, pBar, c)
            }

            if (fa * fc < 0.0) {
                b = c; fb = fc
            } else {
                a = c; fa = fc
            }
        }

        // return middle after all iterations
        val tMid = 0.5 * (a + b)
        return buildBubbleResult(comp1, comp2, x1, x2, pBar, tMid)
    }

    fun buildBubbleResult(
        comp1: Component,
        comp2: Component,
        x1: Double,
        x2: Double,
        pBar: Double,
        tK: Double
    ): BubblePointResult {
        val p1 = pSatBarWithComponent(tK, comp1)
        val p2 = pSatBarWithComponent(tK, comp2)

        val k1 = p1 / pBar
        val k2 = p2 / pBar

        val denom = k1 * x1 + k2 * x2
        val y1 = k1 * x1 / denom
        val y2 = k2 * x2 / denom

        return BubblePointResult(
            tK = tK,
            tC = tK - 273.1,
            y1 = y1,
            y2 = y2,
            k1 = k1,
            k2 = k2
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

    /**
     * @param component1 - starting solvent
     * @param component2 - target solvent
     * @param x1 - starting solvent mole fraction in the liquid (0..1)
     * @param pBar - pressure in the apparat (bar)
     * @param discretization - the discretization level - changed composition has no effect
     * @param removalFraction - the molar fraction that evaporates
     * @param discretization - step size in terms of fraction of INITIAL liquid (0..1)
     *                         typically removalFraction / 100 for ~100 steps.
     * @return - starting solvent mole fraction in the liquid AFTER evaporation
     */
    fun batchSolventSwap(
        component1: Component,
        component2: Component,
        x1: Double,
        pBar: Double,
        removalFraction: Double,
        discretization: Double = removalFraction / 100
    ): Double {

        require(x1 > 0 && x1 < 1) {
            "x1 must be in (0,1)"
        }
        require(removalFraction in 0.0..1.0) {
            "removalFraction must be in [0,1]"
        }
        require(discretization > 0.0 && discretization <= 1.0) {
            "discretization must be in (0,1]"
        }

        val n0 = 1.0
        var n1 = x1 * n0
        var n2 = (1.0 - x1) * n0

        val totalToRemove = removalFraction * n0
        var removed = 0.0

        removalFraction * n0

        while (removed < totalToRemove) {
            val nTot = n1 + n2
            if (nTot <= 1e-12) break

            val x1Curr = n1 / nTot

            // vapor content
            val bubble = bubblePointIdealBinary(component1, component2, x1Curr, pBar)
            val y1 = bubble.y1
            val y2 = bubble.y2

            // removed in vapor
            val removedByStep = discretization * n0

            val removed1 = removedByStep * y1
            val removed2 = removedByStep * y2

            n1 -= removed1
            n2 -= removed2
            removed += removed1 + removed2
        }

        val nTotFinal = n1 + n2

        if (nTotFinal <= 1e-12) {
            return 0.0
        }

        val x1Final = n1 / nTotFinal
        return x1Final.coerceIn(0.0, 1.0)
    }


}
