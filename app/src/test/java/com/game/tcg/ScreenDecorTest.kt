package com.game.tcg

import com.game.tcg.core.engine.ExplorationMode
import com.game.tcg.core.engine.ExplorationResult
import com.game.tcg.core.engine.ExplorationReward
import com.game.tcg.core.engine.ExplorationRewardType
import com.game.tcg.core.engine.RealWorldActivity
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.VisualState
import com.game.tcg.ui.CharacterArtRegistry
import com.game.tcg.ui.ExplorationDecor
import com.game.tcg.ui.RealWorldDecor
import com.game.tcg.ui.ScenarioDecor
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 이세계 접속 · 현실 소환 · 탐색 화면 보강 — 2026-11-16 사용자 요청 검증
 *
 * 세 화면이 실제 일러스트를 어디에 세우고, 선택지의 보상/리스크를 어떻게 나누며,
 * 호감도 하트 게이지·탐색 구역·결과 카드가 어떤 규칙으로 그려지는지를 고정한다.
 */
class ScreenDecorTest {

    // ── 📖 이세계 접속 ────────────────────────────────────────────

    @Test
    fun `이세계 접속 배너와 동행 초상화에는 실제 일러스트가 있다`() {
        assertTrue("배너 리소스 없음", ScenarioDecor.bannerResId != 0)
        assertTrue(
            "동행 캐릭터 아트 없음",
            CharacterArtRegistry.hasArt(ScenarioDecor.COMPANION_CARD_ID)
        )
        assertEquals(VisualState.CASUAL_CUTE, ScenarioDecor.COMPANION_VISUAL)
        assertEquals("아우렐리아", ScenarioDecor.COMPANION_NAME)
    }

    @Test
    fun `유혹 선택지는 리스크로만 표시된다`() {
        val preview = ScenarioDecor.choicePreview(
            label = "차를 마시며 대화를 나눈다",
            hint = "유혹 스택 +1 · 금화 유출 · 마왕전 디버프 경고",
            isTempted = true
        )
        assertEquals(ScenarioDecor.ChoiceTone.RISK, preview.tone)
        assertEquals("⚠", preview.icon)
        assertNull(preview.reward)
        assertNotNull(preview.risk)
        assertTrue(preview.risk!!.contains("유혹 스택 +1"))
        assertTrue(preview.risk!!.contains("경고"))
    }

    @Test
    fun `자원을 쓰고 이득을 보는 선택지는 보상과 리스크를 함께 보여 준다`() {
        val preview = ScenarioDecor.choicePreview(
            label = "경계하고 성수로 정화한다",
            hint = "성수 1 소모 · 방어 보너스 +10%",
            isTempted = false
        )
        assertEquals(ScenarioDecor.ChoiceTone.MIXED, preview.tone)
        assertEquals("⚖", preview.icon)
        assertEquals("방어 보너스 +10%", preview.reward)
        assertEquals("성수 1 소모", preview.risk)
    }

    @Test
    fun `순수 이득 선택지는 보상만 보여 준다`() {
        val preview = ScenarioDecor.choicePreview(
            label = "즉시 전투 진입",
            hint = "승리 시 금화 +300 · 보석 +5",
            isTempted = false
        )
        assertEquals(ScenarioDecor.ChoiceTone.REWARD, preview.tone)
        assertEquals("✦", preview.icon)
        assertNull(preview.risk)
        assertTrue(preview.reward!!.contains("금화"))
    }

    @Test
    fun `정찰처럼 손해가 섞인 선택지도 리스크로 잡힌다`() {
        val preview = ScenarioDecor.choicePreview(
            label = "정찰 선택",
            hint = "금화 -200 · 다음 전투 아군 피해 +15% (급소 파악)",
            isTempted = false
        )
        assertEquals(ScenarioDecor.ChoiceTone.RISK, preview.tone)
        assertTrue(preview.risk!!.contains("금화 -200"))
    }

    // ── 🏙 현실 소환 ─────────────────────────────────────────────

    @Test
    fun `하트 게이지는 호감도를 다섯 칸으로 나눠 채운다`() {
        assertEquals(listOf(false, false, false, false, false), RealWorldDecor.hearts(0))
        assertEquals(listOf(true, false, false, false, false), RealWorldDecor.hearts(20))
        assertEquals(listOf(true, true, false, false, false), RealWorldDecor.hearts(50))
        assertEquals(listOf(true, true, true, true, false), RealWorldDecor.hearts(80))
        assertEquals(listOf(true, true, true, true, true), RealWorldDecor.hearts(100))
        assertEquals("❤ 50/100", RealWorldDecor.heartLabel(50))
        assertEquals("❤ 100/100", RealWorldDecor.heartLabel(180))
    }

    @Test
    fun `호감도 단계 라벨은 50과 100에서 바뀐다`() {
        assertTrue(RealWorldDecor.bondStageLabel(10).contains("첫 만남"))
        assertTrue(RealWorldDecor.bondStageLabel(50).contains("일상"))
        assertTrue(RealWorldDecor.bondStageLabel(100).contains("서약"))
    }

    @Test
    fun `교감 액션 카드 3종은 엔진의 호감도 상승치와 같다`() {
        val cards = RealWorldDecor.actionCards()
        assertEquals(3, cards.size)
        assertEquals(
            listOf(
                RealWorldActivity.ROMANTIC_EVENING_DATE,
                RealWorldActivity.CASUAL_CAFE_WALK,
                RealWorldActivity.DAILY_GREETING
            ),
            cards.map { it.activity }
        )
        assertEquals(listOf(30, 15, 5), cards.map { it.bondGain })
        cards.forEach { card ->
            assertTrue(card.icon.isNotBlank())
            assertTrue(card.title.isNotBlank())
            assertTrue(card.hint.isNotBlank())
        }
    }

    @Test
    fun `파트너 반응 대사는 호감도에 따라 달라진다`() {
        val low = RealWorldDecor.reactionLine(10)
        val mid = RealWorldDecor.reactionLine(60)
        val high = RealWorldDecor.reactionLine(100)
        assertTrue(low.startsWith("「"))
        assertTrue(mid.contains("옷"))
        assertTrue(high.contains("오의"))
        assertEquals(3, setOf(low, mid, high).size)
    }

    @Test
    fun `교감 카드의 실제 일러스트는 일상과 데이트 룩 모두 있다`() {
        val vm = TcgGameViewModel(initialOwnedCardIds = listOf(3, 1, 2))
        val partner = vm.uiState.value.summonCard
        assertNotNull(partner)
        assertTrue(CharacterArtRegistry.hasArt(partner!!.id))
    }

    // ── 🧭 탐색 ─────────────────────────────────────────────────

    @Test
    fun `탐색 구역 3곳은 각각 배경 일러스트와 예상 보상을 가진다`() {
        val zones = ExplorationDecor.zones()
        assertEquals(
            listOf(ExplorationMode.RIFT, ExplorationMode.FRAGMENT, ExplorationMode.ENCOUNTER),
            zones.map { it.mode }
        )
        zones.forEach { zone ->
            assertTrue("${zone.mode} 배경 없음", zone.backdropResId != 0)
            assertTrue("${zone.mode} 예상 보상 없음", zone.expectedRewards.isNotEmpty())
        }
        assertEquals(
            zones.map { it.mode }.map { ExplorationDecor.zoneOf(it).mode },
            zones.map { it.mode }
        )
        assertTrue(ExplorationDecor.zoneOf(ExplorationMode.RIFT).expectedRewards.any { it.contains("금화") })
    }

    @Test
    fun `결과 카드는 조우한 캐릭터의 일러스트를 세운다`() {
        val result = ExplorationResult(
            mode = ExplorationMode.ENCOUNTER,
            title = "낯선 인연과 마주쳤다",
            narrative = "경계의 틈에서 낯익은 목소리가 들려왔다.",
            rewards = listOf(ExplorationReward(ExplorationRewardType.COMPANION_BOND, 10, "호감도 +10")),
            companionCardId = 12,
            companionDisplayName = "테스트 동료"
        )
        assertEquals(12, ExplorationDecor.resultArtCardId(result, fallbackCardId = 3))
        assertEquals(VisualState.CASUAL_CUTE, ExplorationDecor.resultVisual(result))
        assertEquals("👤", ExplorationDecor.resultIcon(result))
        assertEquals("테스트 동료", ExplorationDecor.resultCompanionLabel(result))
        assertTrue(CharacterArtRegistry.hasArt(12))
    }

    @Test
    fun `전투 조우 결과는 상대 일러스트가 없으므로 파티 대표를 세우지 않는다`() {
        val battle = ExplorationResult(
            mode = ExplorationMode.RIFT,
            title = "마수와 조우",
            narrative = "균열에서 마수가 튀어나왔다.",
            enemyName = "차원 틈새의 마수",
            enemyElement = Element.DARK,
            enemyHp = 1200,
            enemyMaxHp = 1200,
            enemyAtk = 300
        )
        assertTrue(battle.startsBattle)
        assertNull(ExplorationDecor.resultArtCardId(battle, fallbackCardId = 3))
        assertEquals("⚔", ExplorationDecor.resultIcon(battle))
    }

    @Test
    fun `기억 파편 결과는 파편 대상의 일상 일러스트를 세운다`() {
        val fragment = ExplorationResult(
            mode = ExplorationMode.FRAGMENT,
            title = "기억의 잔향",
            narrative = "잊힌 목소리가 파편으로 맺혔다.",
            rewards = listOf(ExplorationReward(ExplorationRewardType.MEMORY_FRAGMENT, 1, "기억 파편 +1")),
            fragmentTargetCardId = 5
        )
        assertEquals(5, ExplorationDecor.resultArtCardId(fragment, fallbackCardId = 3))
        assertEquals("🌙", ExplorationDecor.resultIcon(fragment))
        assertEquals(VisualState.CASUAL_CUTE, ExplorationDecor.resultVisual(fragment))
    }

    @Test
    fun `보상 배지는 종류별 아이콘을 붙인다`() {
        val result = ExplorationResult(
            mode = ExplorationMode.RIFT,
            title = "보물 발견",
            narrative = "상자에서 금화가 쏟아졌다.",
            rewards = listOf(
                ExplorationReward(ExplorationRewardType.GOLD, 300, "300G"),
                ExplorationReward(ExplorationRewardType.GEMS, 5, "보석 5"),
                ExplorationReward(ExplorationRewardType.BOND, 10, "호감도 +10")
            )
        )
        assertEquals(listOf("🪙 300G", "💎 보석 5", "❤ 호감도 +10"), ExplorationDecor.rewardBadges(result))
    }

    @Test
    fun `파견 파티 스트립은 출전 덱 카드로 구성된다`() {
        val vm = TcgGameViewModel(initialOwnedCardIds = listOf(3, 1, 2))
        val state = vm.uiState.value
        assertEquals(state.equippedCards.map { it.id }.distinct(), ExplorationDecor.partyStrip(state))
        assertTrue(ExplorationDecor.partyStrip(state).isNotEmpty())
    }
}
