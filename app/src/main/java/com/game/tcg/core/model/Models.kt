package com.game.tcg.core.model

/**
 * 속성 및 상성 시스템
 * - 3원소 삼각 순환: 화염(FIRE) -> 바람(WIND) -> 빙결(ICE) -> 화염 (유리 시 데미지 +30%, 피격 시 -20%)
 * - 2극원소 상호 카운터: 빛(LIGHT) <-> 어둠(DARK) (서로에게 +50% 극딜)
 */
enum class Element(val displayName: String) {
    FIRE("화염"),
    WIND("바람"),
    ICE("빙결"),
    LIGHT("빛"),
    DARK("어둠");

    fun getDamageMultiplier(target: Element): Double = when (this) {
        FIRE -> when (target) {
            WIND -> 1.30
            ICE -> 0.80
            else -> 1.00
        }
        WIND -> when (target) {
            ICE -> 1.30
            FIRE -> 0.80
            else -> 1.00
        }
        ICE -> when (target) {
            FIRE -> 1.30
            WIND -> 0.80
            else -> 1.00
        }
        LIGHT -> when (target) {
            DARK -> 1.50
            else -> 1.00
        }
        DARK -> when (target) {
            LIGHT -> 1.50
            else -> 1.00
        }
    }
}

/**
 * 3단 비주얼 상태 (전투 / 사복 큐트 / 로맨틱 데이트 룩)
 */
enum class VisualState(val displayName: String) {
    BATTLE("전투 TCG 아머"),
    CASUAL_CUTE("일상 사복 큐트"),
    ROMANTIC_DATE("매혹적인 데이트 룩")
}

/**
 * TCG 캐릭터 카드
 */
data class TcgCard(
    val id: Int,                     // 도감 번호 (1~70)
    val name: String,                // 캐릭터명 (예: "태양의 대천사 세라피엘", "백호령 신녀 단아")
    val element: Element,            // 5대 속성
    val baseCost: Int,               // 소환 마나 코스트
    var atk: Int,                    // 공격력
    var def: Int,                    // 방어력/생명력
    var starLevel: Int = 1,          // 성급 (1성 ~ 5성 한계 돌파)
    var currentExp: Int = 0,         // 이세계 원정 획득 누적 EXP
    var bondLevel: Int = 0,          // 현실 일상 교감 호감도 (0~100)
    var currentVisual: VisualState = VisualState.BATTLE,
    val skillText: String
) {
    /**
     * 호감도 MAX(100) 달성 시 개방되는 '페어 얼티밋 오의' 활성화 여부
     */
    val isUltimateUnlocked: Boolean
        get() = bondLevel >= 100
}

/**
 * 배틀 필드 슬롯 구조 (전열 3슬롯, 후열 2슬롯)
 */
data class BattleSlot(
    val slotIndex: Int,
    val isFrontRow: Boolean, // true = 전열 (적 공격 1차 방어선), false = 후열 (지원/원거리)
    var placedCard: TcgCard? = null
)

/**
 * 플레이어 및 덱 상태
 */
data class PlayerDeck(
    val deckName: String,
    val cards: MutableList<TcgCard> = mutableListOf()
)
