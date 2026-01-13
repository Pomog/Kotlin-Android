package com.prot.test

import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

/**
 * Solvent swap by repeated cycles under an ideal VLE model.
 *
 * Model/assumptions:
 * - Binary mixture, ideal solution: Raoult's law (γ = 1), ideal gas vapor (φ = 1), no Poynting correction.
 * - Vapor pressures from Antoine; bubble point T is solved from: P = x1*Psat1(T) + x2*Psat2(T).
 * - At each evaporation step: y_i = K_i x_i / (K1 x1 + K2 x2), where K_i = Psat_i(T)/P.
 * - "Remove 80% by mass" means: distillate mass removed equals 0.80 × (initial liquid mass of the cycle).
 *   Distillate is assumed to condense with the same composition as the calculated vapor (no holdup/entrainment).
 * - After evaporation, methanol is added with mass equal to the removed mass (refill to the same total mass each cycle).
 * - Density is NOT used here (mass↔moles uses MW only). Composition conversions use MW only.
 *
 * Output:
 * - Tracks Solvent1 mass fraction (wAc) and mole fraction (xAc) before evaporation, after evaporation, and after refill.
 */

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
            tC = tK - 273.15,
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
     * @param pBar - pressure in the apparatus (bar)
     * @param removalFraction - total fraction of INITIAL liquid *in moles* that evaporates (0..1)
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
        require(removalFraction > 0 && removalFraction < 1) {
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
            removed += removedByStep
        }

        val nTotFinal = n1 + n2

        if (nTotFinal <= 1e-12) {
            return 0.0
        }

        val x1Final = n1 / nTotFinal
        return x1Final.coerceIn(0.0, 1.0)
    }

    /**
     * Same model as batchSolventSwap(), but the removal target is based on MASS of distillate.
     *
     * @param x1 - starting solvent mole fraction in the liquid (0..1)
     * removalMassFraction = fraction of the INITIAL liquid mass to remove (0..1).
     * discretizationMassFraction = step size as fraction of INITIAL liquid mass.
     * @return - starting solvent mole fraction in the liquid AFTER evaporation
     */
    fun batchSolventSwapByMass(
        component1: Component,
        component2: Component,
        x1: Double,
        pBar: Double,
        removalMassFraction: Double,
        discretizationMassFraction: Double = removalMassFraction / 100.0
    ): Double {

        require(x1 in 0.0..1.0) { "x1 must be in [0,1]" }
        require(pBar > 0.0) { "pBar must be > 0" }
        require(removalMassFraction > 0.0 && removalMassFraction < 1.0) {
            "removalMassFraction must be in (0,1)"
        }
        require(discretizationMassFraction > 0.0 && discretizationMassFraction <= 1.0) {
            "discretizationMassFraction must be in (0,1]"
        }

        val mw1 = component1.mw
        val mw2 = component2.mw
        require(mw1.isFinite() && mw1 > 0.0)
        require(mw2.isFinite() && mw2 > 0.0)

        // Use a 1.0 mol basis for the initial liquid (normalized system).
        // x1 is the liquid-phase mole fraction; n1/n2 are actual moles on this basis.
        val n0 = 1.0
        var n1 = x1 * n0
        var n2 = (1.0 - x1) * n0

        // Initial mass for n0 = 1 mol of mixture
        val m0 = n1 * mw1 + n2 * mw2
        val targetRemovedMass = removalMassFraction * m0
        var removedMass = 0.0

        while (removedMass < targetRemovedMass) {
            val nTot = n1 + n2
            if (nTot <= 1e-12) break

            val x1Curr = n1 / nTot

            val bubble = bubblePointIdealBinary(component1, component2, x1Curr, pBar)
            val y1 = bubble.y1
            val y2 = bubble.y2

            // Step mass to remove (g), clamp last step to hit target
            val dmRaw = discretizationMassFraction * m0
            val dm = minOf(dmRaw, targetRemovedMass - removedMass)

            // Convert dm -> dnTot using vapor-average MW
            val mwVap = y1 * mw1 + y2 * mw2
            if (mwVap <= 0.0) break

            val dnTot = dm / mwVap
            val dn1 = dnTot * y1
            val dn2 = dnTot * y2

            n1 = (n1 - dn1).coerceAtLeast(0.0)
            n2 = (n2 - dn2).coerceAtLeast(0.0)
            removedMass += dm
        }

        val nTotFinal = n1 + n2
        return if (nTotFinal <= 1e-12) 0.0 else (n1 / nTotFinal).coerceIn(0.0, 1.0)
    }

    fun solventSwapCycles(
        acetone: Component,
        methanol: Component,
        pBar: Double,
        removalMassFraction: Double = 0.80,
        targetAcetoneMassFraction: Double = 0.01,
        maxCycles: Int = 20
    ) {
        var wAc = 1.0 // start with pure acetone by mass

        repeat(maxCycles) { cycle ->
            // Convert feed mass fraction -> mole fraction for the model
            val xAc = FractionConverter.wToX(wAc, acetone.mw, methanol.mw)

            // Concentrate (remove 80% of initial mass), result is xAc in residue
            val xAcResid = batchSolventSwapByMass(
                component1 = acetone,
                component2 = methanol,
                x1 = xAc,
                pBar = pBar,
                removalMassFraction = removalMassFraction
            )

            // Convert residue mole fraction -> residue mass fraction
            val wAcResid = FractionConverter.xToW(
                xAcResid, acetone.mw, methanol.mw
            )

            // "Refill": add methanol mass equal to removed mass.
            // With 80% removed, residue mass is 20% of initial, added mass is 80% of initial:
            val wAcAfterRefill = wAcResid * (1.0 - removalMassFraction) // = 0.2 * wAcResid

            println("Cycle %d".format(cycle + 1))

            println(
                "  Before evap : X1(mass%%)=%.2f, X1(mol%%)=%.2f".format(
                    Locale.US,
                    100.0 * wAc,
                    100.0 * xAc
                )
            )

            println(
                "  After refill: X1(mass%%)=%.2f".format(
                    Locale.US,
                    100.0 * wAcAfterRefill
                )
            )

            wAc = wAcAfterRefill

            if (wAc < targetAcetoneMassFraction) {
                println(
                    "Reached target: X1(mass%%) < %.2f after %d cycles".format(
                        Locale.US,
                        100.0 * targetAcetoneMassFraction,
                        cycle + 1
                    )
                )
                return
            }
        }

        println(
            "Did not reach target within %d cycles. Final X1(mass%%)=%.2f".format(
                Locale.US,
                maxCycles,
                100.0 * wAc
            )
        )
    }

}

private const val ITERATIONS = 80