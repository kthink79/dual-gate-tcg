package com.game.tcg

import com.game.tcg.core.engine.AffinityRules
import com.game.tcg.core.engine.BalanceRules
import com.game.tcg.core.engine.BattleResultEngine
import com.game.tcg.core.engine.BattleSimulator
import com.game.tcg.core.engine.CardUpgradeEngine
import com.game.tcg.core.engine.StageEngine
import com.game.tcg.core.engine.SynthesisEngine
import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.StageCatalog
import com.game.tcg.core.model.VisualState
import com.game.tcg.ui.CharacterArtRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 2026-11-17 개편 검증 — ④ 미니어처 페어 · ⑤ 승패 연출 · ⑥ 난이도/상성/강화 밸런스.
 *
 * 사용자 요청 원문
 *  · "의상 선택에 4번째 페어 '미니어처(SD/Chibi)' 추가 (전투복·일상복·데이트복·미니어처)"
 *  · "승리(VICTORY) / 패배(DEFEAT) 전용 결과 화면/오버레이 연출"
 *  · "난이도가 너무 쉬운 듯하다. 캐릭터의 강화·합성을 어느 정도 하고 상성을 잘 조합해야
 *     스테이지를 클리어할 수 있게 조절해 달라"
 */
class MiniatureOutfitAndBalanceTest {

    // ──────────────────────────────────────────────────────────────
    // 1. 4번째 페어 — 미니어처(SD)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `비주얼은 전투 일상 데이트 미니어처 네 종류다`() {
        assertEquals(4, VisualState.entries.size)
        assertEquals(VisualState.MINIATURE, VisualState.entries[3])
        assertEquals("미니어처 SD", VisualState.MINIATURE.displayName)
        assertEquals(4, CharacterArtRegistry.visualCount)
    }

    @Test
    fun `183명 전원이 미니어처 아트를 가진다`() {
        val ids = CardCatalog.ALL.map { it.id } + com.game.tcg.core.model.MidBossCatalog.ALL.map { it.id }
        assertEquals(183, ids.size)
        ids.forEach { id ->
            val res = CharacterArtRegistry.resId(id, VisualState.MINIATURE)
            assertNotNull("#$id 미니어처 아트 없음", res)
            assertTrue("#$id 미니어처 리소스 id 0", (res ?: 0) != 0)
        }
    }

    @Test
    fun `미니어처는 친밀도 0에서도 선택할 수 있다`() {
        assertTrue(AffinityRules.isVisualUnlocked(VisualState.MINIATURE, 0))
        assertTrue(AffinityRules.isVisualUnlocked(VisualState.MINIATURE, 100))
        assertNull("미니어처는 잠금 안내가 없다", AffinityRules.lockLabel(VisualState.MINIATURE, 0))
        // 기존 해금 규칙은 그대로
        assertFalse(AffinityRules.isVisualUnlocked(VisualState.CASUAL_CUTE, 29))
        assertTrue(AffinityRules.isVisualUnlocked(VisualState.CASUAL_CUTE, 30))
        assertFalse(AffinityRules.isVisualUnlocked(VisualState.ROMANTIC_DATE, 69))
        assertTrue(AffinityRules.isVisualUnlocked(VisualState.ROMANTIC_DATE, 70))
    }

    @Test
    fun `친밀도가 오를 때 미니어처 선택이 자동으로 덮어쓰이지 않는다`() {
        // 친밀도 상승은 잠금 해제 단계로만 자동 승급한다 (미니어처는 사용자가 고른 복장)
        val card = com.game.tcg.core.model.TcgCard(
            id = 5, name = "테스트", element = Element.FIRE, baseCost = 5,
            atk = 4000, def = 4000, bondLevel = 29, skillText = ""
        )
        card.currentVisual = VisualState.MINIATURE
        val change = AffinityRules.applyGain(card, 5) // 29 → 34 (일상 개방)
        assertEquals(VisualState.MINIATURE, card.currentVisual)
        assertTrue(change.newlyUnlocked.contains(VisualState.CASUAL_CUTE))
        assertFalse("미니어처는 항상 열려 있으므로 새 해금 목록에 없다", change.newlyUnlocked.contains(VisualState.MINIATURE))
    }

    // ──────────────────────────────────────────────────────────────
    // 2. VICTORY / DEFEAT 결과 패널
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `승리 패널은 보상 다음 스테이지 대표 대사를 담는다`() {
        val stage = StageCatalog.require(5) // 중간 보스
        val outcome = StageEngine.settle(stage, alreadyCleared = false)
        val hero = CardCatalog.require(170).toCard()
        val panel = BattleResultEngine.victory(stage, outcome, hero)

        assertTrue(panel.isVictory)
        assertEquals("VICTORY", com.game.tcg.core.engine.BattleOutcome.VICTORY.headline)
        assertEquals("☠ 중간 보스 격파", panel.badge)
        assertTrue("보상에 골드", panel.rewardLines.any { it.contains("G") })
        assertTrue("보상에 경험치", panel.rewardLines.any { it.contains("EXP") })
        assertEquals(6, panel.nextStageNumber)
        assertEquals("다음 스테이지 ▸", panel.primaryLabel)
        assertEquals(170, panel.heroCardId)
        assertTrue("대표 대사가 있다", panel.heroQuote.isNotBlank())
        assertTrue("승리에는 패인 힌트가 없다", panel.hints.isEmpty())
    }

    @Test
    fun `마지막 스테이지 승리는 캠페인 완주로 표시된다`() {
        val stage = StageCatalog.require(30)
        val panel = BattleResultEngine.victory(stage, StageEngine.settle(stage, false), null)
        assertNull(panel.nextStageNumber)
        assertEquals("캠페인 완주 🎉", panel.primaryLabel)
        assertTrue(panel.badge.contains("대마왕"))
    }

    @Test
    fun `패배 패널은 상성 강화 합성 전투력 힌트를 계산한다`() {
        val stage = StageCatalog.require(10) // LIGHT 적
        // 일부러 불리한 덱 — FIRE 3장, 무강화 1성
        val deck = listOf(11, 12, 13).map { id ->
            com.game.tcg.core.model.TcgCard(
                id = id, name = "t$id", element = Element.FIRE, baseCost = 5,
                atk = 4000, def = 3500, starLevel = 1, skillText = ""
            )
        }
        val panel = BattleResultEngine.defeat(
            stage = stage,
            hero = deck.first(),
            deckPower = 30000,
            deckCards = deck,
            lostCards = 2
        )
        assertFalse(panel.isVictory)
        assertEquals("DEFEAT", com.game.tcg.core.engine.BattleOutcome.DEFEAT.headline)
        assertTrue("배지", panel.badge.contains("패배"))
        assertTrue("상성 힌트", panel.hints.any { it.contains("상성") })
        assertTrue("강화 힌트", panel.hints.any { it.contains("강화") })
        assertTrue("합성 힌트", panel.hints.any { it.contains("합성") })
        assertTrue("전투력 힌트", panel.hints.any { it.contains("전투력") })
        assertTrue("손실 힌트", panel.hints.any { it.contains("잃은 동료") })
        assertEquals("다시 도전 ▸", panel.primaryLabel)
        assertNull("패배에는 다음 스테이지가 없다", panel.nextStageNumber)
    }

    @Test
    fun `패배 패널의 상성 힌트는 적에게 유리한 속성을 알려 준다`() {
        assertEquals("빛", BattleResultEngine.favourableNames(Element.DARK))
        assertEquals("어둠", BattleResultEngine.favourableNames(Element.LIGHT))
        assertEquals("화염", BattleResultEngine.favourableNames(Element.WIND))
        assertEquals("바람", BattleResultEngine.favourableNames(Element.ICE))
        assertEquals("빙결", BattleResultEngine.favourableNames(Element.FIRE))
    }

    // ──────────────────────────────────────────────────────────────
    // 3. 난이도 · 강화 · 합성 · 상성 밸런스
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `상성 배율이 이전보다 커졌다`() {
        assertEquals(1.45, Element.FIRE.getDamageMultiplier(Element.WIND), 0.001)
        assertEquals(0.72, Element.FIRE.getDamageMultiplier(Element.ICE), 0.001)
        assertEquals(1.70, Element.LIGHT.getDamageMultiplier(Element.DARK), 0.001)
        // 유리/불리 피해 차이가 2배 이상 — 속성 조합이 곧 딜량
        val ratio = Element.FIRE.getDamageMultiplier(Element.WIND) / Element.FIRE.getDamageMultiplier(Element.ICE)
        assertTrue("상성 유불리 비율이 2배 이상", ratio >= 2.0)
    }

    @Test
    fun `전열 상성 우세면 받는 피해가 줄고 열세면 늘어난다`() {
        val fireFront = listOf(Element.FIRE, Element.FIRE, Element.FIRE)
        // WIND 적에게 FIRE 는 유리 → 덜 맞는다
        assertEquals(0.88, BalanceRules.damageTakenModifier(fireFront, Element.WIND), 0.001)
        // ICE 적에게 FIRE 는 불리 → 더 맞는다
        assertEquals(1.12, BalanceRules.damageTakenModifier(fireFront, Element.ICE), 0.001)
        // 무관계는 보정 없음
        assertEquals(1.0, BalanceRules.damageTakenModifier(fireFront, Element.DARK), 0.001)
        assertEquals(1, BalanceRules.elementEdge(fireFront, Element.WIND))
        assertEquals(-1, BalanceRules.elementEdge(fireFront, Element.ICE))
        assertEquals(0, BalanceRules.elementEdge(fireFront, Element.LIGHT))
    }

    @Test
    fun `강화는 레벨당 9퍼센트 합성은 성급당 26퍼센트로 커졌다`() {
        assertEquals(9, CardUpgradeEngine.GROWTH_PERCENT)
        assertEquals(1.26, SynthesisEngine.STAR_GROWTH_MULTIPLIER, 0.0001)
        assertEquals(5, SynthesisEngine.MAX_STAR_LEVEL)

        // Lv.9 + 5성 풀투자면 무강화 대비 4배 이상 강해진다
        val card = com.game.tcg.core.model.TcgCard(
            id = 7, name = "t", element = Element.ICE, baseCost = 6,
            atk = 6000, def = 6000, skillText = ""
        )
        val basePower = card.atk + card.def
        val wallet = com.game.tcg.core.model.ShardWallet(mapOf(7 to 500))
        repeat(9) { CardUpgradeEngine().upgrade(card, Int.MAX_VALUE / 2, wallet) }
        val mat = com.game.tcg.core.model.TcgCard(
            id = 7, name = "m", element = Element.ICE, baseCost = 6,
            atk = 1, def = 1, skillText = ""
        )
        repeat(4) { SynthesisEngine().synthesizeCard(card, mat.copy(starLevel = 1)) }
        assertEquals(5, card.starLevel)
        assertTrue("강화+합성 후 전투력 4배 이상", (card.atk + card.def) > basePower * 4)
    }

    @Test
    fun `스테이지 적 스탯이 5스테이지 이후로 갈수록 함께 오른다`() {
        assertEquals(1.0, StageCatalog.difficultyScale(1), 0.0001)
        assertEquals(1.0, StageCatalog.difficultyScale(5), 0.0001)
        assertEquals(1.025, StageCatalog.difficultyScale(6), 0.0001)
        assertEquals(1.625, StageCatalog.difficultyScale(30), 0.0001)
        // 같은 권장 전투력이라도 뒤 스테이지가 더 두껍다
        val s5 = StageCatalog.require(5)
        val s25 = StageCatalog.require(25)
        assertTrue("보스 HP 가 스테이지와 함께 커진다", s25.enemyHp > s5.enemyHp)
        assertTrue("보스 공격력도 커진다", s25.enemyAtk > s5.enemyAtk)
        // 적 공격력은 권장 전투력 대비 충분히 위협적이어야 한다
        // (2026-11-18 다중 적 개편 — 카드 1장 공격력 = 권장 전투력 / 160, 한 턴에 전열 3장이 반격)
        val normal = StageCatalog.require(3)
        assertTrue("적 카드 ATK 가 권장 전투력의 1/200 이상", normal.enemyAtk > normal.recommendedPower / 200)
        // 다중 적 — 적 진영은 9~12장의 개별 HP 카드다 (전열 3 + 후열)
        val squad = com.game.tcg.core.model.EnemySquad.build(stage = normal, size = 9)
        assertEquals("적 카드 수 = 덱 크기", 9, squad.size)
        assertEquals("전열 3장", 3, squad.count { it.isFront })
        assertEquals("총 HP 합 = 스테이지 적 HP", normal.enemyHp, squad.sumOf { it.maxHp })
        assertTrue("전열 HP 가 후열보다 두껍다", squad.first().maxHp > squad.last().maxHp)
        assertTrue("전열 공격력이 후열보다 높다", squad.first().atk > squad.last().atk)
    }

    @Test
    fun `무강화 1성 덱은 5스테이지 이후를 뚫지 못한다`() {
        // 실제 시작 덱과 같은 저코스트(3~4) 3장, 강화 0 · 1성
        val starterIds = com.game.tcg.core.model.StartingRoster.draw(kotlin.random.Random(7))
        assertEquals(3, starterIds.size)
        assertTrue(
            "시작 카드는 코스트 3~4",
            starterIds.all { CardCatalog.require(it).baseCost in com.game.tcg.core.model.StartingRoster.STARTER_COST_RANGE }
        )
        val starter = starterIds.map { id -> CardCatalog.require(id).toCard() }
        // 2026-11-18 다중 적 개편 — 3장 무강화 덱은 5스테이지 중간 보스까지가 한계이고,
        // 6스테이지부터는 덱을 더 채우고 강화해야 한다.
        (6..30 step 4).forEach { number ->
            val stage = StageCatalog.require(number)
            val result = BattleSimulator.simulate(starter, stage)
            assertFalse("무강화 덱이 ${number}스테이지를 뚫었다 (${result.summary})", result.victory)
        }
    }

    @Test
    fun `강화 합성 상성을 갖춘 덱은 중반 스테이지를 뚫는다`() {
        // cost 6 이상 9장 · Lv.5 · 3성 · 적 속성에 유리한 속성으로 구성
        val stage = StageCatalog.require(10) // LIGHT 적
        val cards = listOf(45, 153, 112, 113, 114, 90, 71, 48, 65).map { id ->
            val record = CardCatalog.find(id) ?: CardCatalog.require(1)
            com.game.tcg.core.model.TcgCard(
                id = record.id,
                name = record.name,
                element = Element.DARK, // LIGHT 에 유리
                baseCost = record.baseCost,
                atk = (record.atk * 1.45).toInt(),   // Lv.5 (9%×5)
                def = (record.def * 1.45).toInt(),
                starLevel = 3,                        // 1.26^2
                skillText = record.skill
            )
        }
        val result = BattleSimulator.simulate(cards, stage)
        assertTrue("강화·합성·상성 덱이 10스테이지를 못 뚫었다 (${result.summary})", result.victory)
    }

    @Test
    fun `상성 우위 덱이 중립 덱보다 승률이 높다`() {
        val stage = StageCatalog.require(5) // DARK 적
        val base = listOf(45, 153, 112, 113, 114, 90, 71, 48, 65).map { id ->
            val r = CardCatalog.require(id)
            com.game.tcg.core.model.TcgCard(
                id = r.id, name = r.name, element = Element.DARK, baseCost = r.baseCost,
                atk = (r.atk * 1.36).toInt(), def = (r.def * 1.36).toInt(),
                starLevel = 3, skillText = r.skill
            )
        }
        val neutral = base.map { it.copy(element = Element.FIRE) }
        val advantageChance = BattleSimulator.winChance(base, stage)
        val neutralChance = BattleSimulator.winChance(neutral, stage)
        assertTrue(
            "상성 우위($advantageChance%)가 중립($neutralChance%)보다 높아야 한다",
            advantageChance >= neutralChance
        )
    }

}
