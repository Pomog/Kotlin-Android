package com.prot.test

/**
 * Converts between mole fraction (x), mass fraction (w), and volume fraction (phi)
 * for a BINARY mixture.
 */
object FractionConverter {

    /** Mole fraction -> mass fraction. */
    fun xToW(x1: Double, mw1: Double, mw2: Double): Double {
        require(x1 in 0.0..1.0)
        val x2 = 1.0 - x1
        val denom = x1 * mw1 + x2 * mw2
        return (x1 * mw1) / denom
    }

    /** Mass fraction -> mole fraction. */
    fun wToX(w1: Double, mw1: Double, mw2: Double): Double {
        require(w1 in 0.0..1.0)
        val a = w1 / mw1
        val b = (1.0 - w1) / mw2
        return a / (a + b)
    }

    /**
     * Mole fraction -> volume fraction, assuming additive volumes.
     * rho can be absolute density (g/mL) or specific gravity (water=1); ratios are what matter.
     */
    fun xToPhi(x1: Double, mw1: Double, mw2: Double, rho1: Double, rho2: Double): Double {
        require(x1 in 0.0..1.0)
        require(rho1 > 0.0 && rho2 > 0.0)
        val x2 = 1.0 - x1
        val v1 = x1 * (mw1 / rho1)
        val v2 = x2 * (mw2 / rho2)
        return v1 / (v1 + v2)
    }

    /**
     * Volume fraction -> mole fraction, assuming additive volumes.
     * rho can be absolute density (g/mL) or specific gravity (water=1).
     */
    fun phiToX(phi1: Double, mw1: Double, mw2: Double, rho1: Double, rho2: Double): Double {
        require(phi1 in 0.0..1.0)
        require(rho1 > 0.0 && rho2 > 0.0)
        val a = phi1 / (mw1 / rho1)
        val b = (1.0 - phi1) / (mw2 / rho2)
        return a / (a + b)
    }
}
