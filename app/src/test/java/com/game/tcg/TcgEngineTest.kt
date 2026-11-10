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

    // =========================================================================
    // 1. BattleEngine & Element Affinity Tests (3-element cycle & Light/Dark)
    // =========================================================================

    @Test
    fun testThreeWayElementAffinityCycleAdvantage() {
        // FIRE -> WIND -> ICE -> FIRE (+30% bonus)
        val fireCard = TcgCard(11, "이그니아", Element.FIRE, 8, 1000, 1000, skillText = "")
        val windCard = TcgCard(21, "실바나", Element.WIND, 8, 1000, 1000, skillText = "")
        val iceCard = TcgCard(16, "엘바라", Element.ICE, 8, 1000, 1000, skillText = "")

        val fireToWind = battleEngine.calculateDamage(fireCard, windCard)
        assertEquals(1.30, fireToWind.multiplier, 0.001)
        assertEquals(1300, fireToWind.finalDamage)
        assertTrue(fireToWind.isCriticalElement)

        val windToIce = battleEngine.calculateDamage(windCard, iceCard)
        assertEquals(1.30, windToIce.multiplier, 0.001)
        assertEquals(1300, windToIce.finalDamage)
        assertTrue(windToIce.isCriticalElement)

        val iceToFire = battleEngine.calculateDamage(iceCard, fireCard)
        assertEquals(1.30, iceToFire.multiplier, 0.001)
        assertEquals(1300, iceToFire.finalDamage)
        assertTrue(iceToFire.isCriticalElement)
    }

    @Test
    fun testThreeWayElementAffinityCycleDisadvantage() {
        // Reverse cycle: FIRE -> ICE (-20%), ICE -> WIND (-20%), WIND -> FIRE (-20%)
        val fireCard = TcgCard(11, "이그니아", Element.FIRE, 8, 1000, 1000, skillText = "")
        val windCard = TcgCard(21, "실바나", Element.WIND, 8, 1000, 1000, skillText = "")
        val iceCard = TcgCard(16, "엘바라", Element.ICE, 8, 1000, 1000, skillText = "")

        val fireToIce = battleEngine.calculateDamage(fireCard, iceCard)
        assertEquals(0.80, fireToIce.multiplier, 0.001)
        assertEquals(800, fireToIce.finalDamage)
        assertFalse(fireToIce.isCriticalElement)

        val iceToWind = battleEngine.calculateDamage(iceCard, windCard)
        assertEquals(0.80, iceToWind.multiplier, 0.001)
        assertEquals(800, iceToWind.finalDamage)
        assertFalse(iceToWind.isCriticalElement)

        val windToFire = battleEngine.calculateDamage(windCard, fireCard)
        assertEquals(0.80, windToFire.multiplier, 0.001)
        assertEquals(800, windToFire.finalDamage)
        assertFalse(windToFire.isCriticalElement)
    }

    @Test
    fun testLightDarkMutualCounter() {
        // LIGHT <-> DARK (both deal +50% counter damage)
        val lightCard = TcgCard(1, "세라피엘", Element.LIGHT, 8, 1000, 1000, bondLevel = 0, skillText = "")
        val darkCard = TcgCard(6, "카밀라", Element.DARK, 8, 1000, 1000, bondLevel = 0, skillText = "")

        val lightToDark = battleEngine.calculateDamage(lightCard, darkCard)
        assertEquals(1.50, lightToDark.multiplier, 0.001)
        assertEquals(1500, lightToDark.finalDamage)
        assertTrue(lightToDark.isCriticalElement)
        assertFalse(lightToDark.isUltimateTriggered)

        val darkToLight = battleEngine.calculateDamage(darkCard, lightCard)
        assertEquals(1.50, darkToLight.multiplier, 0.001)
        assertEquals(1500, darkToLight.finalDamage)
        assertTrue(darkToLight.isCriticalElement)
        assertFalse(darkToLight.isUltimateTriggered)
    }

    @Test
    fun testNeutralAndSameElementAffinity() {
        // Neutral matchups: 1.0x multiplier
        val fireCard = TcgCard(11, "이그니아", Element.FIRE, 8, 1000, 1000, skillText = "")
        val sameFireCard = TcgCard(12, "화염정령", Element.FIRE, 8, 1000, 1000, skillText = "")
        val lightCard = TcgCard(1, "세라피엘", Element.LIGHT, 8, 1000, 1000, skillText = "")
        val darkCard = TcgCard(6, "카밀라", Element.DARK, 8, 1000, 1000, skillText = "")

        val fireToFire = battleEngine.calculateDamage(fireCard, sameFireCard)
        assertEquals(1.00, fireToFire.multiplier, 0.001)
        assertEquals(1000, fireToFire.finalDamage)
        assertFalse(fireToFire.isCriticalElement)

        val fireToLight = battleEngine.calculateDamage(fireCard, lightCard)
        assertEquals(1.00, fireToLight.multiplier, 0.001)
        assertEquals(1000, fireToLight.finalDamage)

        val darkToFire = battleEngine.calculateDamage(darkCard, fireCard)
        assertEquals(1.00, darkToFire.multiplier, 0.001)
        assertEquals(1000, darkToFire.finalDamage)

        val lightToLight = battleEngine.calculateDamage(lightCard, lightCard)
        assertEquals(1.00, lightToLight.multiplier, 0.001)
        assertEquals(1000, lightToLight.finalDamage)

        val darkToDark = battleEngine.calculateDamage(darkCard, darkCard)
        assertEquals(1.00, darkToDark.multiplier, 0.001)
        assertEquals(1000, darkToDark.finalDamage)
    }

    @Test
    fun testPairUltimateCombinations() {
        // Awakened bondLevel >= 100 gives +25% ultimate bonus (x1.25)
        val lightAwakened = TcgCard(1, "세라피엘", Element.LIGHT, 8, 1000, 1000, bondLevel = 100, skillText = "")
        val darkCard = TcgCard(6, "카밀라", Element.DARK, 8, 1000, 1000, bondLevel = 0, skillText = "")
        val fireCard = TcgCard(11, "이그니아", Element.FIRE, 8, 1000, 1000, bondLevel = 0, skillText = "")
        val windCard = TcgCard(21, "실바나", Element.WIND, 8, 1000, 1000, bondLevel = 0, skillText = "")
        val iceCard = TcgCard(16, "엘바라", Element.ICE, 8, 1000, 1000, bondLevel = 0, skillText = "")

        // LIGHT awakened vs DARK: 1000 * 1.50 * 1.25 = 1875
        val lightDarkResult = battleEngine.calculateDamage(lightAwakened, darkCard)
        assertEquals(1875, lightDarkResult.finalDamage)
        assertTrue(lightDarkResult.isUltimateTriggered)

        // LIGHT awakened vs FIRE (neutral): 1000 * 1.00 * 1.25 = 1250
        val neutralAwakenedResult = battleEngine.calculateDamage(lightAwakened, fireCard)
        assertEquals(1250, neutralAwakenedResult.finalDamage)
        assertTrue(neutralAwakenedResult.isUltimateTriggered)

        // FIRE awakened vs WIND (advantage + ultimate): 1000 * 1.30 * 1.25 = 1625
        val fireAwakened = TcgCard(11, "이그니아", Element.FIRE, 8, 1000, 1000, bondLevel = 100, skillText = "")
        val advantageAwakenedResult = battleEngine.calculateDamage(fireAwakened, windCard)
        assertEquals(1625, advantageAwakenedResult.finalDamage)
        assertTrue(advantageAwakenedResult.isUltimateTriggered)

        // FIRE awakened vs ICE (disadvantage + ultimate): 1000 * 0.80 * 1.25 = 1000
        val disadvantageAwakenedResult = battleEngine.calculateDamage(fireAwakened, iceCard)
        assertEquals(1000, disadvantageAwakenedResult.finalDamage)
        assertTrue(disadvantageAwakenedResult.isUltimateTriggered)

        // Unawakened bondLevel = 99 -> no ultimate
        val nearAwakened = TcgCard(11, "이그니아", Element.FIRE, 8, 1000, 1000, bondLevel = 99, skillText = "")
        assertFalse(nearAwakened.isUltimateUnlocked)
        val unawakenedResult = battleEngine.calculateDamage(nearAwakened, windCard)
        assertEquals(1300, unawakenedResult.finalDamage)
        assertFalse(unawakenedResult.isUltimateTriggered)
    }

    // =========================================================================
    // 2. SynthesisEngine Tests (1-star to 5-star ascension, +20% scaling & edge cases)
    // =========================================================================

    @Test
    fun testFullFiveStarSynthesisProgression() {
        val baseCard = TcgCard(
            id = 1,
            name = "태양의 대천사 세라피엘",
            element = Element.LIGHT,
            baseCost = 8,
            atk = 1000,
            def = 1000,
            starLevel = 1,
            skillText = ""
        )

        // 1성 -> 2성: 1000 * 1.20 = 1200
        val mat1 = TcgCard(1, "세라피엘 재료1", Element.LIGHT, 8, 1000, 1000, starLevel = 1, skillText = "")
        assertTrue(synthesisEngine.synthesizeCard(baseCard, mat1))
        assertEquals(2, baseCard.starLevel)
        assertEquals(1200, baseCard.atk)
        assertEquals(1200, baseCard.def)

        // 2성 -> 3성: 1200 * 1.20 = 1440
        val mat2 = TcgCard(1, "세라피엘 재료2", Element.LIGHT, 8, 1000, 1000, starLevel = 1, skillText = "")
        assertTrue(synthesisEngine.synthesizeCard(baseCard, mat2))
        assertEquals(3, baseCard.starLevel)
        assertEquals(1440, baseCard.atk)
        assertEquals(1440, baseCard.def)

        // 3성 -> 4성: 1440 * 1.20 = 1728
        val mat3 = TcgCard(1, "세라피엘 재료3", Element.LIGHT, 8, 1000, 1000, starLevel = 1, skillText = "")
        assertTrue(synthesisEngine.synthesizeCard(baseCard, mat3))
        assertEquals(4, baseCard.starLevel)
        assertEquals(1728, baseCard.atk)
        assertEquals(1728, baseCard.def)

        // 4성 -> 5성 한계 돌파: 1728 * 1.20 = 2073
        val mat4 = TcgCard(1, "세라피엘 재료4", Element.LIGHT, 8, 1000, 1000, starLevel = 1, skillText = "")
        assertTrue(synthesisEngine.synthesizeCard(baseCard, mat4))
        assertEquals(5, baseCard.starLevel)
        assertEquals(2073, baseCard.atk)
        assertEquals(2073, baseCard.def)

        // 5성 도달 후 추가 합성 시도 -> 실패 (한계 돌파 상한 5성 유지)
        val mat5 = TcgCard(1, "세라피엘 재료5", Element.LIGHT, 8, 1000, 1000, starLevel = 1, skillText = "")
        assertFalse(synthesisEngine.synthesizeCard(baseCard, mat5))
        assertEquals(5, baseCard.starLevel)
        assertEquals(2073, baseCard.atk)
        assertEquals(2073, baseCard.def)
    }

    @Test
    fun testSynthesisRejectionDifferentCardId() {
        val baseCard = TcgCard(1, "태양의 대천사 세라피엘", Element.LIGHT, 8, 1000, 1000, starLevel = 1, skillText = "")
        val differentCard = TcgCard(2, "건슬링어 베리티", Element.FIRE, 7, 1000, 1000, starLevel = 1, skillText = "")

        val result = synthesisEngine.synthesizeCard(baseCard, differentCard)
        assertFalse(result)
        assertEquals(1, baseCard.starLevel)
        assertEquals(1000, baseCard.atk)
        assertEquals(1000, baseCard.def)
    }

    @Test
    fun testSynthesisRejectionAlreadyFiveStar() {
        val fiveStarCard = TcgCard(51, "백호령 신녀 단아", Element.WIND, 7, 7400, 6200, starLevel = 5, skillText = "")
        val materialCard = TcgCard(51, "백호령 신녀 단아", Element.WIND, 7, 7400, 6200, starLevel = 1, skillText = "")

        val result = synthesisEngine.synthesizeCard(fiveStarCard, materialCard)
        assertFalse(result)
        assertEquals(5, fiveStarCard.starLevel)
        assertEquals(7400, fiveStarCard.atk)
        assertEquals(6200, fiveStarCard.def)
    }

    // =========================================================================
    // 3. DualWorldEngine Tests (Real-world bond & Another-world expedition)
    // =========================================================================

    @Test
    fun testRealWorldActivityScalingAndVisualTransitions() {
        val card = TcgCard(51, "단아", Element.WIND, 7, 7400, 6200, bondLevel = 0, skillText = "")
        assertEquals(VisualState.BATTLE, card.currentVisual)

        // Daily greeting (+5 bond) -> bond 5, remains BATTLE (< 50)
        val bond1 = dualWorldEngine.interactInRealWorld(card, RealWorldActivity.DAILY_GREETING)
        assertEquals(5, bond1)
        assertEquals(VisualState.BATTLE, card.currentVisual)

        // Casual cafe walk (+15 bond) -> bond 20, remains BATTLE (< 50)
        val bond2 = dualWorldEngine.interactInRealWorld(card, RealWorldActivity.CASUAL_CAFE_WALK)
        assertEquals(20, bond2)
        assertEquals(VisualState.BATTLE, card.currentVisual)

        // Casual cafe walk two more times -> bond 50, switches to CASUAL_CUTE
        dualWorldEngine.interactInRealWorld(card, RealWorldActivity.CASUAL_CAFE_WALK)
        dualWorldEngine.interactInRealWorld(card, RealWorldActivity.CASUAL_CAFE_WALK)
        assertEquals(50, card.bondLevel)
        assertEquals(VisualState.CASUAL_CUTE, card.currentVisual)

        // Romantic evening date (+30 bond) -> switches to ROMANTIC_DATE
        dualWorldEngine.interactInRealWorld(card, RealWorldActivity.ROMANTIC_EVENING_DATE)
        assertEquals(80, card.bondLevel)
        assertEquals(VisualState.ROMANTIC_DATE, card.currentVisual)

        // Romantic evening date (+30 bond) -> capped at 100, unlocks ultimate
        dualWorldEngine.interactInRealWorld(card, RealWorldActivity.ROMANTIC_EVENING_DATE)
        assertEquals(100, card.bondLevel)
        assertTrue(card.isUltimateUnlocked)
        assertEquals(VisualState.ROMANTIC_DATE, card.currentVisual)

        // Further interaction does not exceed 100
        dualWorldEngine.interactInRealWorld(card, RealWorldActivity.DAILY_GREETING)
        assertEquals(100, card.bondLevel)
    }

    @Test
    fun testAnotherWorldExpeditionExpAndStatGrowth() {
        // 1-star card: EXP threshold = 1 * 1000 = 1000
        val card = TcgCard(1, "세라피엘", Element.LIGHT, 8, 1000, 1000, starLevel = 1, skillText = "")

        // Battle 1: Earn 600 EXP (threshold 1000) -> no level up yet
        val levelUp1 = dualWorldEngine.completeIsekaiBattle(card, 600)
        assertFalse(levelUp1)
        assertEquals(600, card.currentExp)
        assertEquals(1000, card.atk)
        assertEquals(1000, card.def)

        // Battle 2: Earn 500 EXP -> total 1100 >= 1000 -> level up (+150 ATK, +100 DEF), remaining EXP 100
        val levelUp2 = dualWorldEngine.completeIsekaiBattle(card, 500)
        assertTrue(levelUp2)
        assertEquals(100, card.currentExp)
        assertEquals(1150, card.atk)
        assertEquals(1100, card.def)

        // Now test 2-star card: threshold = 2 * 1000 = 2000
        card.starLevel = 2
        card.currentExp = 0
        val levelUp3 = dualWorldEngine.completeIsekaiBattle(card, 1500)
        assertFalse(levelUp3)
        assertEquals(1500, card.currentExp)

        val levelUp4 = dualWorldEngine.completeIsekaiBattle(card, 800) // 1500 + 800 = 2300 >= 2000
        assertTrue(levelUp4)
        assertEquals(300, card.currentExp)
        assertEquals(1300, card.atk)
        assertEquals(1200, card.def)
    }
}
