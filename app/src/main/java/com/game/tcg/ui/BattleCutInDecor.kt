package com.game.tcg.ui

import com.game.tcg.core.model.Element
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * ⚔️ 전투 연출 데이터 — 자동 전투 페이스 · 공격 컷인 문구 · 이펙트 곡선.
 *
 * 2026-11-16 사용자 요청:
 *  - 「기본 자동 전투」: 전투에 들어가면 자동으로 턴이 돌고, 🤖 토글로 언제든 수동 전환
 *  - 「공격 카드 중앙 확대 컷인」: 공격할 때마다 공격자 카드가 화면 중앙으로 확대되며
 *    속성 빛 테두리 + `⚔️ 이름 · 공격!` 배너, 피격 카드에 흔들림·플래시·참격·데미지 숫자
 *
 * Compose 에 의존하지 않는 순수 계산만 담는다(단위 테스트로 고정).
 * 화면(BattleScreen · BattleCutInOverlay)은 여기 값만 받아 그린다.
 */

/** 공격 연출 종류 — 배너 문구와 색조를 가른다 */
enum class BattleAttackKind(val defaultLabel: String) {
    /** 전열 일반 공격 */
    FRONT("공격"),

    /** 후열 지원 사격 */
    BACK("후열 지원 사격"),

    /** 페어 얼티밋 오의 */
    PAIR_ULTIMATE("페어 얼티밋 오의"),

    /** 적의 반격 (턴 진행) */
    ENEMY_COUNTER("반격")
}

/** 전투 배속 — 컷인·이펙트·자동 스텝 시간을 함께 나눈다 */
enum class BattleSpeed(val multiplier: Float, val label: String) {
    X1(1f, "1x"),
    X2(2f, "2x");

    fun toggled(): BattleSpeed = if (this == X1) X2 else X1

    companion object {
        /** 저장·복원용 문자열 라벨 해석 (모르는 값은 1x) */
        fun ofLabel(label: String?): BattleSpeed =
            entries.firstOrNull { it.label.equals(label?.trim(), ignoreCase = true) } ?: X1
    }
}

/**
 * 공격 1회의 연출 스냅샷 — ViewModel 이 공격을 확정할 때마다 새로 만들어 상태에 넣는다.
 * `tick` 이 바뀌면 화면이 컷인을 다시 재생한다.
 */
data class BattleCutIn(
    /** 컷인 재생 트리거 (공격마다 +1) */
    val tick: Int = 0,
    /** 적의 반격인지 (true = 아군 카드가 피격) */
    val fromEnemy: Boolean = false,
    val kind: BattleAttackKind = BattleAttackKind.FRONT,
    /** 공격자 카드 번호 (아트 표시용) */
    val attackerCardId: Int = 0,
    /** 배너에 쓰는 짧은 이름 (예: 세라피엘) */
    val attackerName: String = "",
    /** 공격자 속성 — 빛 테두리 색 */
    val element: Element = Element.LIGHT,
    /** 공격명·스킬명 배너 문구 (예: 공격, 페어 얼티밋 오의) */
    val attackName: String = "",
    /** 아군 공격자 슬롯 (전열 강조) */
    val attackerSlot: Int = -1,
    /** 피격 대상 이름 (적 이름 또는 아군 카드 이름) */
    val targetName: String = "",
    /** 아군 피격 시 피해 슬롯 */
    val targetSlot: Int = -1,
    val damage: Int = 0,
    /** 상성 우위(급소) */
    val critical: Boolean = false,
    /** 상성 배율 (1.3x · 0.8x …) */
    val multiplier: Double = 1.0,
    /** 페어 오의 발동 */
    val ultimate: Boolean = false,
    /** 이 일격으로 적이 쓰러졌는지 */
    val lethal: Boolean = false
) {
    /** 배너 아래 붙는 대상 표시 (예: → 차원 틈새의 마수) */
    val subtitle: String
        get() = if (targetName.isNotBlank()) "→ $targetName" else ""
}

/**
 * 자동 전투·컷인 연출의 수치와 문구 규칙.
 * 배속([BattleSpeed])이 오르면 모든 시간이 배율만큼 짧아진다(1.0~1.3초 템포 유지).
 */
object BattleAutoDecor {

    /** 컷인 1회 길이 (1x) — 카드 확대 → 타격 → 잔상 */
    const val CUT_IN_MILLIS: Long = 620L

    /** 타격 뒤 데미지 숫자를 유지하는 시간 (1x) */
    const val HIT_HOLD_MILLIS: Long = 460L

    /** 자동 전투 1스텝 간격 (1x) — 컷인 + 타격 유지 + 여유 */
    const val STEP_MILLIS: Long = 1180L

    /** 컷인 안에서 타격이 터지는 지점 비율 (0.55 = 확대가 정점에 닿는 순간) */
    const val IMPACT_RATIO: Float = 0.55f

    /** 피격 흔들림 길이 (1x) */
    const val SHAKE_MILLIS: Long = 420L

    /** 배속을 반영한 시간 — 너무 짧아 보이지 않도록 하한을 둔다 */
    fun scaledMillis(base: Long, speed: BattleSpeed): Long =
        (base / speed.multiplier).toLong().coerceAtLeast(100L)

    fun cutInMillis(speed: BattleSpeed): Long = scaledMillis(CUT_IN_MILLIS, speed)

    fun hitHoldMillis(speed: BattleSpeed): Long = scaledMillis(HIT_HOLD_MILLIS, speed)

    fun shakeMillis(speed: BattleSpeed): Long = scaledMillis(SHAKE_MILLIS, speed)

    /** 자동 전투 스텝 간격 — 컷인 연출 시간을 포함한 값 */
    fun stepIntervalMillis(speed: BattleSpeed): Long = scaledMillis(STEP_MILLIS, speed)

    /** 컷인 시작 → 타격까지 (타격 플래시·데미지 숫자는 이 시점에 터진다) */
    fun impactMillis(speed: BattleSpeed): Long =
        scaledMillis((CUT_IN_MILLIS * IMPACT_RATIO).toLong(), speed)

    /** 컷인 시작 → 타격 연출 정리까지 한 번에 (오버레이 타임라인 길이) */
    fun effectMillis(speed: BattleSpeed): Long =
        impactMillis(speed) + hitHoldMillis(speed)

    // ── 문구 ──────────────────────────────────────────────────────────

    /** 🤖 자동 전투 토글 라벨 */
    fun autoLabel(isAuto: Boolean): String = if (isAuto) "🤖 자동 전투 ON" else "✋ 수동 전투 OFF"

    /** 자동 전투 상태 한 줄 (상단 바 힌트) */
    fun autoHint(isAuto: Boolean, speed: BattleSpeed): String = when {
        !isAuto -> "수동 조작 — FIGHT / 턴 진행"
        speed == BattleSpeed.X2 -> "2x 자동 진행 · ${stepIntervalMillis(speed)}ms 템포"
        else -> "자동 진행 · ${stepIntervalMillis(speed)}ms 템포"
    }

    /** 중앙 컷인 배너 — `⚔️ 세라피엘 · 공격!` */
    fun bannerText(cutIn: BattleCutIn): String {
        val name = cutIn.attackerName.ifBlank { "무명의 계약자" }
        val attack = cutIn.attackName.ifBlank { cutIn.kind.defaultLabel }
        val mark = if (cutIn.fromEnemy) "🩸" else "⚔️"
        return "$mark $name · $attack!"
    }

    /** 데미지 숫자 — `-1,240` (적 반격은 아군이 잃은 HP) */
    fun damageText(damage: Int): String = "-" + thousands(damage)

    /** 1,000 단위 구분 */
    fun thousands(value: Int): String {
        val negative = value < 0
        val digits = kotlin.math.abs(value).toString()
        val out = StringBuilder()
        digits.forEachIndexed { index, ch ->
            if (index > 0 && (digits.length - index) % 3 == 0) out.append(',')
            out.append(ch)
        }
        return (if (negative) "-" else "") + out.toString()
    }

    /** 상성 배율 표기 — 1.5x / 1x / 0.8x */
    fun multiplierText(multiplier: Double): String {
        val tenths = (multiplier * 10).roundToInt()
        return if (tenths % 10 == 0) "${tenths / 10}x" else "${tenths / 10}.${kotlin.math.abs(tenths % 10)}x"
    }

    /** 데미지 옆에 붙는 배지 (상성·급소·오의·반격) */
    fun badges(cutIn: BattleCutIn): List<String> = buildList {
        if (cutIn.ultimate || cutIn.kind == BattleAttackKind.PAIR_ULTIMATE) add("오의")
        if (cutIn.critical) add("급소")
        when {
            cutIn.multiplier >= 1.05 -> add("상성 ${multiplierText(cutIn.multiplier)}")
            cutIn.multiplier <= 0.95 -> add("상성 열세")
        }
        if (cutIn.kind == BattleAttackKind.BACK) add("후열")
        if (cutIn.fromEnemy) add("적 반격")
    }

    /** 배지가 없으면 표기하지 않는다 (UI 편의) */
    fun badgeText(cutIn: BattleCutIn): String? =
        badges(cutIn).takeIf { it.isNotEmpty() }?.joinToString(" · ")

    /** 타격 플래시 색조 이름 — 급소/오의는 금빛, 일반은 붉은 섬광 */
    fun isGoldenHit(cutIn: BattleCutIn): Boolean = cutIn.critical || cutIn.ultimate

    // ── 애니메이션 곡선 (p: 컷인 진행 0→1, d: 타격 감쇠 1→0) ───────────

    /** 중앙 확대 스케일 — 0.62 에서 빠르게 1.18 까지 커진 뒤 1.10 로 가라앉는다 */
    fun cutInScale(p: Float): Float {
        val t = p.coerceIn(0f, 1f)
        return if (t <= 0.55f) {
            val r = t / 0.55f
            0.62f + 0.56f * (1f - (1f - r) * (1f - r))
        } else {
            val r = (t - 0.55f) / 0.45f
            1.18f - 0.08f * r
        }
    }

    /** 컷인 투명도 — 0.12 구간에서 나타나 0.85 이후 사라진다 */
    fun cutInAlpha(p: Float): Float {
        val t = p.coerceIn(0f, 1f)
        return when {
            t <= 0.12f -> t / 0.12f
            t >= 0.85f -> (1f - t) / 0.15f
            else -> 1f
        }.coerceIn(0f, 1f)
    }

    /** 컷인 기울기(도) — 살짝 기울며 들어와 0 으로 선다 */
    fun cutInTilt(p: Float): Float = -7f * (1f - p.coerceIn(0f, 1f))

    /** 타격 플래시 세기 (d: 1→0) */
    fun flashAlpha(d: Float): Float = (d.coerceIn(0f, 1f) * 0.55f)

    /** 피격 흔들림 오프셋 (-1..1, d: 1→0) — 3회 진동 후 멎는다 */
    fun shakeOffset(d: Float): Float {
        val v = d.coerceIn(0f, 1f)
        if (v <= 0f) return 0f
        return sin(v * Math.PI.toFloat() * 6f) * v
    }

    /** 참격 진행도 (d: 1→0 → 0→1) */
    fun slashProgress(d: Float): Float = 1f - d.coerceIn(0f, 1f)
}

/**
 * 🤖 자동 전투 파일럿 — 지금 무엇을 할지만 고르는 순수 두뇌.
 *
 * 우선순위: 페어 오의(가능하면) → 전열 공격 → 후열 지원 → 턴 진행.
 * 적을 쓰러뜨렸거나 생존 아군이 없으면 [Action.STOP] 으로 루프를 끝낸다.
 */
object BattleAutoPilot {

    enum class Action(val label: String) {
        PAIR_ULTIMATE("페어 얼티밋 오의"),
        ATTACK_FRONT("전열 공격"),
        ATTACK_BACK("후열 지원"),
        NEXT_TURN("턴 진행"),
        STOP("자동 전투 종료")
    }

    /** 전투가 계속되는 상태인지 (승리·전멸이면 false) */
    fun isBattleLive(state: TcgGameState): Boolean =
        state.enemyHp > 0 && state.party.any { it.isAlive }

    /** 자동 전투가 다음에 할 행동 */
    fun nextAction(state: TcgGameState): Action {
        if (state.enemyHp <= 0) return Action.STOP
        if (state.party.none { it.isAlive }) return Action.STOP
        if (state.canActivatePairUltimate) return Action.PAIR_ULTIMATE
        val frontAlive = state.party.any { it.isAlive && it.isFrontRow }
        val backAlive = state.party.any { it.isAlive && !it.isFrontRow }
        if (state.mana.currentMana >= 1) {
            if (frontAlive) return Action.ATTACK_FRONT
            if (backAlive) return Action.ATTACK_BACK
        }
        return Action.NEXT_TURN
    }

    /** 자동 전투를 이어야 하는지 (토글 ON + 전투 유효) */
    fun shouldContinue(state: TcgGameState): Boolean =
        state.isAutoBattle && isBattleLive(state)

    /** 자동 전투 종료 사유 (상태 문구용) */
    fun stopReason(state: TcgGameState): String = when {
        state.enemyHp <= 0 -> "적을 쓰러뜨려 자동 전투를 마칩니다."
        state.party.none { it.isAlive } -> "생존한 동료가 없어 자동 전투를 멈춥니다."
        else -> "자동 전투를 끕니다."
    }
}
