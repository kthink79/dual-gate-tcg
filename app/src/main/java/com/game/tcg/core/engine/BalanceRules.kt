package com.game.tcg.core.engine

import com.game.tcg.core.model.Element

/**
 * 전투 밸런스 공통 규칙 — 2026-11-17 난이도 개편.
 *
 * 사용자 요청:
 * > "난이도가 너무 쉬운 듯하다. 캐릭터의 강화·합성을 어느 정도 하고
 * >  상성을 잘 조합해야 스테이지를 클리어할 수 있게 조절해 달라"
 *
 * 그래서 승패 요인을 세 갈래로 나누고, 각각의 영향력을 코드 한 곳에서 관리한다.
 *
 * 1. **강화·합성** — [com.game.tcg.core.engine.CardUpgradeEngine.GROWTH_PERCENT] (레벨당 +9%),
 *    [SynthesisEngine.STAR_GROWTH_MULTIPLIER] (성급당 ×1.26). 5성·Lv.10 이면 ATK·DEF 가 약 4.5배.
 * 2. **상성(공격)** — [Element.ADVANTAGE_MULTIPLIER] 1.45 / [Element.DISADVANTAGE_MULTIPLIER] 0.72.
 *    유리/불리 피해 차이가 약 2배라 속성 조합이 곧 딜량이다.
 * 3. **상성(방어)** — 이 객체. 전열의 다수 속성이 적 속성에 유리하면 받는 피해가 줄고,
 *    불리하면 늘어난다. "상성을 잘 조합"해야 버틴다는 두 번째 축이다.
 *
 * Android 에 의존하지 않으므로 JVM 단위 테스트에서 그대로 검증한다.
 */
object BalanceRules {

    /** 전열 다수가 적 속성에 **유리**할 때 받는 피해 배율 (-12%) */
    const val ADVANTAGE_DAMAGE_TAKEN_MODIFIER: Double = 0.88

    /** 전열 다수가 적 속성에 **불리**할 때 받는 피해 배율 (+12%) */
    const val DISADVANTAGE_DAMAGE_TAKEN_MODIFIER: Double = 1.12

    /** 상성 우열이 없을 때 */
    const val NEUTRAL_DAMAGE_TAKEN_MODIFIER: Double = 1.0

    /**
     * 전열 속성 구성의 상성 우열.
     * @return +1 = 유리 우세, -1 = 불리 우세, 0 = 동등·혼재
     */
    fun elementEdge(frontElements: List<Element>, enemyElement: Element): Int {
        var score = 0
        frontElements.forEach { element ->
            when {
                element.isAdvantageAgainst(enemyElement) -> score += 1
                element.isDisadvantageAgainst(enemyElement) -> score -= 1
            }
        }
        return when {
            score > 0 -> 1
            score < 0 -> -1
            else -> 0
        }
    }

    /** 적의 반격 피해에 곱하는 배율 (상성 우세면 덜 맞는다) */
    fun damageTakenModifier(frontElements: List<Element>, enemyElement: Element): Double =
        when (elementEdge(frontElements, enemyElement)) {
            1 -> ADVANTAGE_DAMAGE_TAKEN_MODIFIER
            -1 -> DISADVANTAGE_DAMAGE_TAKEN_MODIFIER
            else -> NEUTRAL_DAMAGE_TAKEN_MODIFIER
        }

    /** 전투 로그·결과 화면에 쓰는 한 줄 설명 */
    fun edgeLabel(frontElements: List<Element>, enemyElement: Element): String =
        when (elementEdge(frontElements, enemyElement)) {
            1 -> "상성 우세 — 받는 피해 -12%"
            -1 -> "상성 열세 — 받는 피해 +12%"
            else -> "상성 동등 — 피해 보정 없음"
        }

    /**
     * 이 덱의 "상성 준비도" — 적 속성에 유리한 카드 비율 (0.0~1.0).
     * 스테이지 화면의 상성 배지에 쓴다.
     */
    fun advantageShare(deckElements: List<Element>, enemyElement: Element): Double {
        if (deckElements.isEmpty()) return 0.0
        val favourable = deckElements.count { it.isAdvantageAgainst(enemyElement) }
        return favourable.toDouble() / deckElements.size
    }
}
