package com.game.tcg.ui

import com.game.tcg.R
import com.game.tcg.core.engine.ExplorationMode
import com.game.tcg.core.engine.ExplorationResult
import com.game.tcg.core.engine.RealWorldActivity
import com.game.tcg.core.engine.SummonEngine
import com.game.tcg.core.model.VisualState

/**
 * 「이세계 접속 · 현실 소환 · 탐색」 세 화면의 연출 데이터.
 *
 * 2026-11-16 사용자 요청("텍스트 위주라 게임성이 부족하다")에 맞춰 세 화면을
 * 실제 일러스트 중심으로 다시 구성하며, 화면이 쓸 배너·초상화·존·보상 배지의
 * 선택 규칙을 이 파일(Compose 무관)에 모아 단위 테스트로 고정한다.
 */

// ============================================================================
// 📖 이세계 접속 (ScenarioScreen)
// ============================================================================

/** 현실·이세계 교차 이벤트에 동행하는 파트너 (성기사 아우렐리아 = 도감 No.3) */
object ScenarioDecor {

    /** 상단 배너 — 차원의 문이 열리는 인트로 장면(3장면)을 그대로 쓴다 */
    val bannerResId: Int get() = R.drawable.intro_scene_3

    /** 동행 캐릭터 도감 번호 */
    const val COMPANION_CARD_ID: Int = 3

    /** 동행 캐릭터 표시 이름 (아트가 없을 때 대체 문구) */
    const val COMPANION_NAME: String = "아우렐리아"

    /** 이세계 접속 화면의 동행 캐릭터는 일상 모습으로 서 있다 */
    val COMPANION_VISUAL: VisualState get() = VisualState.CASUAL_CUTE

    /** 선택지 카드의 성격 */
    enum class ChoiceTone {
        /** 이득이 큰 선택 (보상 배지 초록) */
        REWARD,

        /** 대가·위험이 있는 선택 (경고 배지 빨강) */
        RISK,

        /** 이득과 대가가 함께 있는 선택 (혼합 배지 금색) */
        MIXED
    }

    /**
     * 선택지 1개의 미리보기 — 아이콘 · 성격 · 보상/리스크 문구.
     * 힌트 문구를 '·'로 나눠 위험 신호가 있는 조각은 리스크로 분류한다.
     */
    data class ChoicePreview(
        val icon: String,
        val tone: ChoiceTone,
        val reward: String?,
        val risk: String?
    )

    private val RISK_MARKERS = listOf("경고", "디버프", "유혹 스택", "피해", "유출", "소모", "-")

    /** 위험 신호가 섞인 조각인지 */
    fun isRiskFragment(fragment: String): Boolean = RISK_MARKERS.any { fragment.contains(it) }

    /** 선택지 힌트 → 보상/리스크 미리보기 */
    fun choicePreview(label: String, hint: String, isTempted: Boolean): ChoicePreview {
        val fragments = hint.split("·").map { it.trim() }.filter { it.isNotEmpty() }
        val riskParts = fragments.filter { isRiskFragment(it) }
        val rewardParts = fragments.filterNot { isRiskFragment(it) }
        val tone = when {
            isTempted -> ChoiceTone.RISK
            riskParts.isNotEmpty() && rewardParts.isNotEmpty() -> ChoiceTone.MIXED
            riskParts.isNotEmpty() -> ChoiceTone.RISK
            else -> ChoiceTone.REWARD
        }
        val icon = when (tone) {
            ChoiceTone.REWARD -> "✦"
            ChoiceTone.RISK -> "⚠"
            ChoiceTone.MIXED -> "⚖"
        }
        return ChoicePreview(
            icon = icon,
            tone = tone,
            reward = rewardParts.takeIf { it.isNotEmpty() }?.joinToString(" · "),
            risk = riskParts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
        )
    }

    /** 화자 이름표 — 대사창 위에 붙는다 */
    fun speakerName(stepIndex: Int): String = if (stepIndex % 2 == 0) COMPANION_NAME else "계약자"
}

// ============================================================================
// 🏙️ 현실 소환 · 교감 (RealWorldScreen)
// ============================================================================

object RealWorldDecor {

    /** 하트 게이지 개수 */
    const val HEART_COUNT: Int = 5

    /** 호감도(0~100) → 하트 5칸 채움 여부 (20마다 1칸) */
    fun hearts(bond: Int): List<Boolean> {
        val filled = (bond.coerceIn(0, 100) * HEART_COUNT) / 100
        return List(HEART_COUNT) { index -> index < filled }
    }

    /** 하트 게이지 텍스트 */
    fun heartLabel(bond: Int): String = "❤ ${bond.coerceIn(0, 100)}/100"

    /** 호감도 단계 라벨 (0~49 첫 만남 · 50~99 일상 · 100 서약) */
    fun bondStageLabel(bond: Int): String = when {
        bond >= SummonEngine.ULTIMATE_UNLOCK_BOND -> "서약 완성 · 페어 얼티밋 오의"
        bond >= SummonEngine.DATE_UNLOCK_BOND -> "일상 사복 진화 · 데이트 룩"
        else -> "첫 만남 · 전투 모습"
    }

    /** 교감 액션 카드 1종 */
    data class ActionCard(
        val activity: RealWorldActivity,
        val icon: String,
        val title: String,
        val hint: String,
        val bondGain: Int,
        val tone: InfoTone
    )

    /** 선물 대화 데이트 — 세 가지 교감 카드 (엔진의 호감도 상승치와 일치) */
    fun actionCards(): List<ActionCard> = listOf(
        ActionCard(
            activity = RealWorldActivity.ROMANTIC_EVENING_DATE,
            icon = "🌹",
            title = "카페 데이트",
            hint = "데이트 룩 연출 · 가장 큰 호감도",
            bondGain = 30,
            tone = InfoTone.GOLD
        ),
        ActionCard(
            activity = RealWorldActivity.CASUAL_CAFE_WALK,
            icon = "☕",
            title = "저녁 산책",
            hint = "일상 사복 · 편안한 대화",
            bondGain = 15,
            tone = InfoTone.HEAL
        ),
        ActionCard(
            activity = RealWorldActivity.DAILY_GREETING,
            icon = "💬",
            title = "짧은 대화",
            hint = "부담 없는 인사 · 소소한 호감도",
            bondGain = 5,
            tone = InfoTone.MANA
        )
    )

    /**
     * 호감도에 따른 파트너 반응 대사 — 교감 카드 아래 말풍선에 그대로 띄운다.
     */
    fun reactionLine(bond: Int): String = when {
        bond >= 100 -> "「오의가 열렸습니다. 제 모든 것을 당신의 검에 맡기죠.」"
        bond >= 70 -> "「오늘은… 조금 더 가까이 있어도 될까요.」"
        bond >= SummonEngine.DATE_UNLOCK_BOND -> "「이런 옷… 어울릴까요? 당신 앞에서만 입어 볼게요.」"
        bond >= 30 -> "「당신과 걷는 밤은, 이세계의 어떤 전장보다 조용합니다.」"
        else -> "「오늘도 부르셨습니까, 계약자.」"
    }

    /** 마지막 교감 결과 한 줄 (반응 연출용) */
    fun lastReaction(log: List<String>): String? = log.lastOrNull()
}

// ============================================================================
// 🧭 탐색 (ExplorationScreen)
// ============================================================================

object ExplorationDecor {

    /** 탐색 구역 1곳 — 배경 일러스트 · 소모 · 예상 보상 */
    data class Zone(
        val mode: ExplorationMode,
        val backdropResId: Int,
        val expectedRewards: List<String>,
        val tone: InfoTone
    )

    /** 세 갈래 탐색 구역 (배경은 인트로 장면 일러스트를 재사용) */
    fun zones(): List<Zone> = listOf(
        Zone(
            mode = ExplorationMode.RIFT,
            backdropResId = R.drawable.intro_scene_3,
            expectedRewards = listOf("🪙 금화", "💎 보석", "⚔ 조우"),
            tone = InfoTone.VIOLET
        ),
        Zone(
            mode = ExplorationMode.FRAGMENT,
            backdropResId = R.drawable.intro_scene_4,
            expectedRewards = listOf("🌙 기억 파편", "❤ 호감도"),
            tone = InfoTone.ABYSS
        ),
        Zone(
            mode = ExplorationMode.ENCOUNTER,
            backdropResId = R.drawable.intro_scene_1,
            expectedRewards = listOf("👤 동료 조우", "🎁 선물"),
            tone = InfoTone.GOLD
        )
    )

    /** 모드별 구역 조회 */
    fun zoneOf(mode: ExplorationMode): Zone = zones().first { it.mode == mode }

    /**
     * 결과 카드에 세울 캐릭터 — 동료 조우 > 기억 파편 대상 > 대표 캐릭터 순.
     * (전투 조우는 상대 일러스트가 없으므로 파티 대표를 세우지 않고 null 을 돌려준다)
     */
    fun resultArtCardId(result: ExplorationResult, fallbackCardId: Int): Int? = when {
        result.companionCardId != null -> result.companionCardId
        result.fragmentTargetCardId != null -> result.fragmentTargetCardId
        result.startsBattle -> null
        else -> fallbackCardId
    }

    /** 결과 카드의 일러스트 모습 — 조우/파편은 일상 모습, 그 외는 전투 모습 */
    fun resultVisual(result: ExplorationResult): VisualState =
        if (result.companionCardId != null || result.fragmentTargetCardId != null) {
            VisualState.CASUAL_CUTE
        } else {
            VisualState.BATTLE
        }

    /** 결과 카드 제목 앞에 붙는 아이콘 (조우·전투·보물·파편) */
    fun resultIcon(result: ExplorationResult): String = when {
        result.startsBattle -> "⚔"
        result.fragmentTargetCardId != null -> "🌙"
        result.companionCardId != null -> "👤"
        result.temptationGained > 0 -> "🌫"
        else -> "🎁"
    }

    /** 보상 배지 — 종류별 아이콘을 붙인다 */
    fun rewardBadges(result: ExplorationResult): List<String> = result.rewards.map { reward ->
        val icon = when (reward.type) {
            com.game.tcg.core.engine.ExplorationRewardType.GOLD -> "🪙"
            com.game.tcg.core.engine.ExplorationRewardType.GEMS -> "💎"
            com.game.tcg.core.engine.ExplorationRewardType.HOLY_WATER -> "💧"
            com.game.tcg.core.engine.ExplorationRewardType.JUNK_CARD -> "🎴"
            com.game.tcg.core.engine.ExplorationRewardType.GATE_ENERGY -> "⚡"
            com.game.tcg.core.engine.ExplorationRewardType.BOND -> "❤"
            com.game.tcg.core.engine.ExplorationRewardType.MEMORY_FRAGMENT -> "🌙"
            com.game.tcg.core.engine.ExplorationRewardType.TEMPTATION -> "🌫"
            com.game.tcg.core.engine.ExplorationRewardType.COMPANION_BOND -> "👤"
        }
        "$icon ${reward.label}"
    }

    /** 결과 카드에서 캐릭터 이름표로 쓸 문구 */
    fun resultCompanionLabel(result: ExplorationResult): String? =
        result.companionDisplayName?.takeIf { it.isNotBlank() }

    /** 파견 파티 스트립에 세울 카드 순서 (전열 우선) */
    fun partyStrip(state: TcgGameState): List<Int> =
        state.equippedCards.map { it.id }.distinct()
}
