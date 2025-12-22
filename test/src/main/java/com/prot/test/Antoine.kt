package com.prot.test

fun main() {
    val scraper = Scraper()
    val swap = SolventSwap()

    val x1 = 0.5
    val pBar = 0.266

    val acetone = scraper.getComponent("67-64-1") // MW = 58 g/mol
    val acetoneMW = 58

    val methanol = scraper.getComponent("67-56-1") // MW = 32 g/mol
    val methanolMW = 32

    val liquidRelativeMass = x1 * acetoneMW + (1 - x1) * methanolMW
    val massRatioInLiquidX1 = x1 * acetoneMW / liquidRelativeMass
    val massRatioInLiquidX2 = (1 - x1) * methanolMW / liquidRelativeMass

    val bubble = swap.bubblePointIdealBinary(acetone, methanol, x1, pBar)

    val vapRelativeMass = bubble.y1 * acetoneMW + bubble.y2 * methanolMW
    val massRatioInVaporY1 = bubble.y1 * acetoneMW / vapRelativeMass
    val massRatioInVaporY2 = bubble.y2 * methanolMW / vapRelativeMass




    println(
        "Solvent Mixture, mass Ratio: Acetone %.2f, Methanol %.2f"
            .format(massRatioInLiquidX1, massRatioInLiquidX2)
    )

    println("Bubble point T = %.2f °C at P=%.2f bar".format(bubble.tC, pBar))
    //println("Initial mole, in the liquid phase, fractions: x1=$x1, x2=${1 - x1}")

    println(
        "Liquid phase (mass fractions):  Acetone w1 = %.2f, Methanol w2 = %.2f"
            .format(massRatioInLiquidX1, massRatioInLiquidX2)
    )
    //println("To vapor by mole: y1=${bubble.y1}, y2=${bubble.y2}")
    println(
        "Vapor phase  (mass fractions):  Acetone w1 = %.2f, Methanol w2 = %.2f"
            .format(massRatioInVaporY1, massRatioInVaporY2)
    )
}
