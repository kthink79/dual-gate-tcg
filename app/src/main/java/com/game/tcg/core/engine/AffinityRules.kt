package com.game.tcg.core.engine

import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.VisualState

/**
 * 친밀도(호감도) 규칙 — 2026-11-16 개편.
 *
 * 사용자 요청: *"친밀도 상승은 전투 출전 중심, 선물·보유 기간은 작게"* · *"일상복 30 / 데이트복 70"*
 *
 * ── 해금 단계 ──────────────────────────────────────────────
 * | 친밀도 | 열리는 것 |
 * | --- | --- |
 * | **0** | 시작값 — 새로 뽑거나 계약한 캐릭터는 모두 0에서 시작한다 |
 * | **30** | ② 일상 사복(CASUAL_CUTE) · **현실(일상) 소환 가능** |
 * | **70** | ③ 매혹적인 데이트 룩(ROMANTIC_DATE) |
 * | **100** | 페어 얼티밋 오의 (+25%) |
 *
 * **미니어처(SD)** 는 친밀도와 무관하게 **처음부터 열려 있다** (수집·감상용 SD 버전).
 *
 * ── 상승 폭 ───────────────────────────────────────────────
 * - **전투 출전 = 주 경로**: 출전 1회당 +[BATTLE_DEPLOY_GAIN](4).
 *   이겨서 정산하면 승리 보너스 +[BATTLE_VICTORY_BONUS](2)가 더 붙어 **한 전투 +6**.
 * - **선물·대화·산책·보유 기간 = 보조**: +[DAILY_TALK_GAIN]~+[DATE_GAIN] 수준으로만 오른다.
 *   (전투 1회(+6) > 하루 보유(+1) — 전투가 친밀도의 중심이라는 규칙을 코드로 고정한다.)
 *
 * 이 객체는 Android에 의존하지 않으므로 JVM 단위 테스트에서 그대로 검증한다.
 */
object AffinityRules {

    // ── 친밀도 범위 ──────────────────────────────────────────────
    const val MIN_BOND: Int = 0
    const val MAX_BOND: Int = 100

    // ── 해금 기준 (일상 30 · 데이트 70 · 오의 100) ────────────────
    /** ② 일상 사복 해금 — 이 단계에서 현실(일상) 소환도 열린다 */
    const val CASUAL_UNLOCK_BOND: Int = 30

    /** ③ 데이트 룩 해금 */
    const val DATE_UNLOCK_BOND: Int = 70

    /** 페어 얼티밋 오의 해금 */
    const val ULTIMATE_UNLOCK_BOND: Int = 100

    // ── 전투 출전 (주 경로) ──────────────────────────────────────
    /** 전투 1회 출전 — 승패와 무관하게 지급 */
    const val BATTLE_DEPLOY_GAIN: Int = 4

    /** 승리 정산 시 추가 지급 (출전 4 + 승리 2 = 한 전투 6) */
    const val BATTLE_VICTORY_BONUS: Int = 2

    /** 승리한 전투 1회의 총 상승분 */
    const val BATTLE_WIN_GAIN: Int = BATTLE_DEPLOY_GAIN + BATTLE_VICTORY_BONUS

    // ── 선물 · 대화 · 보유 기간 (보조 경로, 작은 폭) ───────────────
    /** 짧은 일상 대화 */
    const val DAILY_TALK_GAIN: Int = 1

    /** 저녁 산책 · 카페 */
    const val WALK_GAIN: Int = 1

    /** 카페 데이트 · 선물 */
    const val DATE_GAIN: Int = 2

    /** 탐색에서 동료를 만났을 때 (선물을 건네는 정도) */
    const val COMPANION_ENCOUNTER_GAIN: Int = 2

    /** 현실 소환 1회 (직접 만나는 것 자체) */
    const val SUMMON_GAIN: Int = 2

    /** 보유 기간 — 하루당 */
    const val HOLDING_GAIN_PER_DAY: Int = 1

    /** 보유 기간 — 한 번에 정산되는 최대치 (오래 쉬었다고 한꺼번에 오르지 않게) */
    const val HOLDING_GAIN_MAX_PER_CLAIM: Int = 10

    // ── 판정 ────────────────────────────────────────────────────

    /** ② 일상 사복이 열렸는가 */
    fun isCasualUnlocked(bond: Int): Boolean = bond >= CASUAL_UNLOCK_BOND

    /** ③ 데이트 룩이 열렸는가 */
    fun isDateUnlocked(bond: Int): Boolean = bond >= DATE_UNLOCK_BOND

    /** 페어 얼티밋 오의가 열렸는가 */
    fun isUltimateUnlocked(bond: Int): Boolean = bond >= ULTIMATE_UNLOCK_BOND

    /**
     * 현실(일상) 소환 가능 조건 — **일상 사복이 해금된 캐릭터만** 현실로 부를 수 있다.
     * (사용자 요청: "일상복 해금된 경우에만 현실 소환")
     */
    fun canRealitySummon(bond: Int): Boolean = isCasualUnlocked(bond)

    /** 이 친밀도에서 고를 수 있는 가장 높은 비주얼 */
    fun visualFor(bond: Int): VisualState = when {
        isDateUnlocked(bond) -> VisualState.ROMANTIC_DATE
        isCasualUnlocked(bond) -> VisualState.CASUAL_CUTE
        else -> VisualState.BATTLE
    }

    /** 이 친밀도에서 실제로 개방된 비주얼 목록 (전투는 항상 열려 있다) */
    fun unlockedVisuals(bond: Int): List<VisualState> = VisualState.entries.filter { isVisualUnlocked(it, bond) }

    /** 특정 비주얼이 이 친밀도에서 열리는가 */
    fun isVisualUnlocked(visual: VisualState, bond: Int): Boolean = when (visual) {
        VisualState.BATTLE -> true
        VisualState.CASUAL_CUTE -> isCasualUnlocked(bond)
        VisualState.ROMANTIC_DATE -> isDateUnlocked(bond)
        // 미니어처(SD)는 친밀도 조건 없이 항상 고를 수 있다
        VisualState.MINIATURE -> true
    }

    /** 잠긴 비주얼의 안내 문구 (`🔒 일상 사복 — 친밀도 30 필요 (현재 12)`) */
    fun lockLabel(visual: VisualState, bond: Int): String? {
        if (isVisualUnlocked(visual, bond)) return null
        val need = requiredBond(visual)
        return "🔒 ${visual.displayName} — 친밀도 $need 필요 (현재 ${bond.coerceIn(MIN_BOND, MAX_BOND)})"
    }

    /** 그 비주얼이 열리는 친밀도 */
    fun requiredBond(visual: VisualState): Int = when (visual) {
        VisualState.BATTLE -> MIN_BOND
        VisualState.CASUAL_CUTE -> CASUAL_UNLOCK_BOND
        VisualState.ROMANTIC_DATE -> DATE_UNLOCK_BOND
        VisualState.MINIATURE -> MIN_BOND
    }

    /** 다음 해금까지 남은 친밀도 (모두 열렸으면 null) */
    fun nextUnlockNeed(bond: Int): Int? = when {
        !isCasualUnlocked(bond) -> CASUAL_UNLOCK_BOND - bond
        !isDateUnlocked(bond) -> DATE_UNLOCK_BOND - bond
        !isUltimateUnlocked(bond) -> ULTIMATE_UNLOCK_BOND - bond
        else -> null
    }

    /** 해금 진행도 게이지 (0~1) — 현실 소환 잠금 화면에 쓴다 */
    fun casualProgress(bond: Int): Float =
        (bond.coerceIn(MIN_BOND, CASUAL_UNLOCK_BOND).toFloat() / CASUAL_UNLOCK_BOND)

    // ── 상승 적용 ────────────────────────────────────────────────

    /** 친밀도 변화 1건의 결과 */
    data class BondChange(
        val cardId: Int,
        val before: Int,
        val after: Int,
        /** 실제로 오른 양 (100 상한에서 잘린 뒤) */
        val gained: Int,
        /** 이번 상승으로 새로 열린 비주얼 */
        val newlyUnlocked: List<VisualState>
    ) {
        val isMax: Boolean get() = after >= MAX_BOND
    }

    /**
     * 친밀도를 올리고(0~100 상한) 결과를 돌려준다.
     * 새 단계가 열리면 카드의 현재 비주얼을 그 단계로 자동 승급한다(내려가지는 않는다).
     */
    fun applyGain(card: TcgCard, gain: Int): BondChange {
        val before = card.bondLevel.coerceIn(MIN_BOND, MAX_BOND)
        val after = (before + gain.coerceAtLeast(0)).coerceIn(MIN_BOND, MAX_BOND)
        card.bondLevel = after
        val unlockedBefore = unlockedVisuals(before).toSet()
        val unlockedAfter = unlockedVisuals(after)
        val newly = unlockedAfter.filter { it !in unlockedBefore }
        val natural = visualFor(after)
        if (natural.ordinal > card.currentVisual.ordinal) {
            card.currentVisual = natural
        }
        return BondChange(card.id, before, after, after - before, newly)
    }

    /** 전투 1회 출전 정산 — 출전 횟수 +1, 친밀도 +[BATTLE_DEPLOY_GAIN] */
    fun creditBattleDeployment(card: TcgCard): BondChange {
        card.battleCount += 1
        return applyGain(card, BATTLE_DEPLOY_GAIN)
    }

    /** 전투 승리 정산 — 승리 보너스 +[BATTLE_VICTORY_BONUS] */
    fun creditBattleVictory(card: TcgCard): BondChange = applyGain(card, BATTLE_VICTORY_BONUS)

    /**
     * 보유 기간 정산 — 마지막 정산일부터 [days]일이 지났으면 하루 +[HOLDING_GAIN_PER_DAY].
     * 한 번에 최대 [HOLDING_GAIN_MAX_PER_CLAIM]까지만 오른다.
     */
    fun creditHolding(card: TcgCard, days: Int): BondChange {
        val bounty = (days.coerceAtLeast(0) * HOLDING_GAIN_PER_DAY)
            .coerceAtMost(HOLDING_GAIN_MAX_PER_CLAIM)
        if (bounty <= 0) return BondChange(card.id, card.bondLevel, card.bondLevel, 0, emptyList())
        return applyGain(card, bounty)
    }

    /** 에포크 밀리초 → 일 단위 (보유 기간 계산용) */
    fun epochDay(millis: Long): Int = Math.floorDiv(millis, 86_400_000L).toInt()
}
