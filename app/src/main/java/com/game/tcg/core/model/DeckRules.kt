package com.game.tcg.core.model

/**
 * 출전 덱(전장에 내보내는 카드 묶음)의 크기 규칙 — 사용자 확정 규칙(2026-11-16).
 *
 * - 기본 편성 가능: **9장** (Lv.1)
 * - 레벨이 오르면 **1장씩 해금**: Lv.5 → 10장, Lv.10 → 11장, Lv.15 → **최대 12장**
 * - 어떤 경우에도 **12장을 넘길 수 없다** ([MAX_DECK_SIZE])
 *
 * 슬롯 배치는 앞의 3칸이 전열, 나머지가 후열·대기열이다(전열은 적의 1차 공격 대상).
 * 레벨은 [PlayerProgress]가 경험치로 계산하며, 전투 승리·탐색으로 경험치가 쌓인다.
 */
object DeckRules {

    /** 기본 덱 크기 (Lv.1 해금) */
    const val BASE_DECK_SIZE: Int = 9

    /** 최소 편성 수 — 기본 9장 */
    const val MIN_DECK_SIZE: Int = BASE_DECK_SIZE

    /** 절대 상한 — 12장 */
    const val MAX_DECK_SIZE: Int = 12

    /** 전열 슬롯 수 (0 ~ 2) */
    const val FRONT_SLOTS: Int = 3

    /** 해금 단계 (레벨 → 덱 크기) */
    data class Unlock(val level: Int, val size: Int)

    /** Lv.1 9장 → Lv.5 10장 → Lv.10 11장 → Lv.15 12장 */
    val UNLOCKS: List<Unlock> = listOf(
        Unlock(level = 1, size = BASE_DECK_SIZE),
        Unlock(level = 5, size = 10),
        Unlock(level = 10, size = 11),
        Unlock(level = 15, size = MAX_DECK_SIZE)
    )

    /** 해당 레벨에서 편성할 수 있는 덱 크기 (9 ~ 12) */
    fun deckSizeForLevel(level: Int): Int = UNLOCKS
        .lastOrNull { level >= it.level }
        ?.size
        ?.coerceIn(MIN_DECK_SIZE, MAX_DECK_SIZE)
        ?: BASE_DECK_SIZE

    /** 잠긴 슬롯에 표시할 다음 해금 정보 — 이미 최대면 null */
    fun nextUnlock(level: Int): Unlock? = UNLOCKS.firstOrNull { level < it.level }

    /** 특정 슬롯(0부터)이 열리는 레벨 */
    fun requiredLevelForSlot(slotIndex: Int): Int = requiredLevelForSize(slotIndex + 1)

    /** 특정 덱 크기를 여는 최소 레벨 */
    fun requiredLevelForSize(size: Int): Int = UNLOCKS
        .firstOrNull { size <= it.size }
        ?.level
        ?: UNLOCKS.last().level

    /** 슬롯이 현재 레벨에서 열려 있는지 */
    fun isSlotUnlocked(slotIndex: Int, level: Int): Boolean =
        slotIndex in 0 until deckSizeForLevel(level)

    /** 잠긴 슬롯 수 (최대 12칸 기준) */
    fun lockedSlotCount(level: Int): Int = (MAX_DECK_SIZE - deckSizeForLevel(level)).coerceAtLeast(0)

    /** 슬롯이 전열인지 (0~2) */
    fun isFrontSlot(slotIndex: Int): Boolean = slotIndex in 0 until FRONT_SLOTS

    /** 슬롯 이름 — 전1~전3 · 후1~후9 */
    fun slotLabel(slotIndex: Int): String =
        if (isFrontSlot(slotIndex)) "전${slotIndex + 1}" else "후${slotIndex - FRONT_SLOTS + 1}"
}

/**
 * 플레이어 레벨 · 경험치 — 덱 해금과 연동된다.
 *
 * 레벨 1에서 시작해 100 경험치마다 1레벨 오른다.
 * 전투 승리 +120, 탐색 +30, 훈련(연습) +120으로 쌓이며, Lv.15에서 덱 12장이 열린다.
 */
object PlayerProgress {

    /** 시작 레벨 */
    const val START_LEVEL: Int = 1

    /** 레벨 1업에 필요한 경험치 */
    const val EXP_PER_LEVEL: Int = 100

    /** 전투 승리 경험치 */
    const val EXP_BATTLE_VICTORY: Int = 120

    /** 탐색 1회 경험치 */
    const val EXP_EXPLORATION: Int = 30

    /** 훈련(연습) 1회 경험치 — 해금 확인용 */
    const val EXP_TRAINING: Int = 120

    /** 경험치 → 레벨 (1 이상, 상한 없음) */
    fun levelForExp(exp: Int): Int = START_LEVEL + (exp.coerceAtLeast(0) / EXP_PER_LEVEL)

    /** 현재 레벨에서 다음 레벨까지 남은 경험치 */
    fun expToNextLevel(exp: Int): Int {
        val remain = exp.coerceAtLeast(0) % EXP_PER_LEVEL
        return EXP_PER_LEVEL - remain
    }

    /** 현재 레벨 구간 진행률 (0~1) */
    fun levelProgress(exp: Int): Float =
        (exp.coerceAtLeast(0) % EXP_PER_LEVEL).toFloat() / EXP_PER_LEVEL
}
