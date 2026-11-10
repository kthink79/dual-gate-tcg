package com.game.tcg

import com.game.tcg.core.engine.BattleEngine
import com.game.tcg.core.engine.DualWorldEngine
import com.game.tcg.core.engine.RealWorldActivity
import com.game.tcg.core.engine.SynthesisEngine
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.VisualState
import org.junit.Assert.*
import org.junit.Test

class TcgEngineTest {

    private val battleEngine = BattleEngine()
    private val synthesisEngine = SynthesisEngine()
    private val dualWorldEngine = DualWorldEngine()

    @Test
    fun testThreeWayElementAffinity() {
        val fireCard = TcgCard(11, "이그니아", Element.FIRE, 8, 1000, 1000, skillText = "")
        val windCard = TcgCard(21, "실바나", Element.WIND, 8, 1000, 1000, skillText = "")
        val iceCard = TcgCard(16, "엘바라", Element.ICE, 8, 1000, 1000, skillText = "")

        val fireToWind = battleEngine.calculateDamage(fireCard, windCard)
        assertEquals(1300, fireToWind.finalDamage) // +30%

        val windToIce = battleEngine.calculateDamage(windCard, iceCard)
        assertEquals(1300, windToIce.finalDamage) // +30%

        val iceToFire = battleEngine.calculateDamage(iceCard, fireCard)
        assertEquals(1300, iceToFire.finalDamage) // +30%

        val fireToIce = battleEngine.calculateDamage(fireCard, iceCard)
        assertEquals(800, fireToIce.finalDamage) // -20%
    }

    @Test
    fun testLightDarkCounterAndPairUltimate() {
        val lightCard = TcgCard(1, "세라피엘", Element.LIGHT, 8, 1000, 1000, bondLevel = 100, skillText = "")
        val darkCard = TcgCard(6, "카밀라", Element.DARK, 8, 1000, 1000, bondLevel = 0, skillText = "")

        val result = battleEngine.calculateDamage(lightCard, darkCard)
        // 1000 * 1.5 (빛↔어둠 상극) * 1.25 (오의 각성) = 1875
        assertEquals(1875, result.finalDamage)
        assertTrue(result.isUltimateTriggered)
    }

    @Test
    fun testCardSynthesisAndStarAscension() {
        val base = TcgCard(1, "세라피엘", Element.LIGHT, 8, 1000, 1000, starLevel = 1, skillText = "")
        val material = TcgCard(1, "세라피엘", Element.LIGHT, 8, 1000, 1000, starLevel = 1, skillText = "")

        val success = synthesisEngine.synthesizeCard(base, material)
        assertTrue(success)
        assertEquals(2, base.starLevel)
        assertEquals(1200, base.atk)
        assertEquals(1200, base.def)
    }

    @Test
    fun testDualWorldInteractionAndVisualSwitch() {
        val card = TcgCard(51, "단아", Element.WIND, 7, 7400, 6200, bondLevel = 10, skillText = "")

        dualWorldEngine.interactInRealWorld(card, RealWorldActivity.ROMANTIC_EVENING_DATE)
        assertEquals(40, card.bondLevel)
        assertEquals(VisualState.ROMANTIC_DATE, card.currentVisual)

        dualWorldEngine.interactInRealWorld(card, RealWorldActivity.ROMANTIC_EVENING_DATE)
        dualWorldEngine.interactInRealWorld(card, RealWorldActivity.ROMANTIC_EVENING_DATE)
        assertEquals(100, card.bondLevel)
        assertTrue(card.isUltimateUnlocked)
    }
}
