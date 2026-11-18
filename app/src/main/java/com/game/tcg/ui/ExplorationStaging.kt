package com.game.tcg.ui

import com.game.tcg.core.engine.ExplorationMode
import com.game.tcg.core.model.VisualState

/**
 * 탐색 진행 연출 — 2026-11-18 사용자 요청.
 *
 * > "탐색 진행 화면에서 텍스트만 표시되지 않도록, 일상복 차림(평상복/캐주얼 페어) 캐릭터
 * >  일러스트가 화면에 함께 나타나면서 탐색이 진행되도록 UI와 연출을 구현한다."
 *
 * 예전에는 구역을 누르면 곧바로 결과 텍스트 카드만 떴다. 이제는
 * ① 파견 파티가 **일상복(CASUAL_CUTE)** 차림으로 장면에 들어서고
 * ② 진행 게이지가 차오르며 단계별 문구가 하나씩 나타나고
 * ③ 마지막에 결과 카드가 열린다.
 *
 * 연출 단계는 화면(Compose)과 분리된 순수 데이터라 JVM 단위 테스트에서 검증한다.
 */
object ExplorationStaging {

    /** 연출 한 단계 */
    data class Step(
        val progress: Float,
        val message: String,
        /** 이 단계에서 캐릭터가 등장(슬라이드 인)하는지 */
        val revealCompanion: Boolean = false
    )

    /** 전체 연출 길이 (ms) — 이 시간이 지나면 결과 카드로 넘어간다 */
    const val DURATION_MILLIS: Int = 2200

    /** 연출에 세우는 최대 캐릭터 수 */
    const val MAX_COMPANIONS: Int = 3

    /** 구역별 단계 문구 */
    fun steps(mode: ExplorationMode): List<Step> = when (mode) {
        ExplorationMode.RIFT -> listOf(
            Step(0.12f, "차원 균열이 벌어진다 — 일상복 차림의 동료들이 먼저 발을 들인다.", revealCompanion = true),
            Step(0.45f, "균열 가장자리를 따라 걷는다. 발밑으로 보랏빛 물결이 흐른다."),
            Step(0.75f, "무언가 빛난다 — 손을 뻗는다."),
            Step(1.00f, "탐색 완료")
        )
        ExplorationMode.FRAGMENT -> listOf(
            Step(0.12f, "기억의 잔향이 흐르는 길 — 평상복 동료들이 조용히 따라선다.", revealCompanion = true),
            Step(0.45f, "이름을 잃은 목소리가 들린다. 조각이 손끝에 닿는다."),
            Step(0.75f, "달빛 파편이 손바닥에 모인다."),
            Step(1.00f, "탐색 완료")
        )
        ExplorationMode.ENCOUNTER -> listOf(
            Step(0.12f, "인적 드문 골목 — 동료들이 일상복 차림으로 좌우를 살핀다.", revealCompanion = true),
            Step(0.45f, "누군가의 기척. 서로의 눈이 마주친다."),
            Step(0.75f, "조우 — 계약의 문양이 빛난다."),
            Step(1.00f, "탐색 완료")
        )
    }

    /** 연출에 쓰는 복장 — 항상 일상복(평상복/캐주얼 페어) */
    val stagingVisual: VisualState = VisualState.CASUAL_CUTE

    /**
     * 연출에 세울 캐릭터 번호 — 전열 → 후열 순으로 최대 3명.
     * 파티가 비어 있으면 대표 캐릭터를 세운다.
     */
    fun companionIds(party: List<Pair<Int, Boolean>>, fallbackCardId: Int): List<Int> {
        val ordered = party.sortedByDescending { it.second }.map { it.first }.distinct()
        return (ordered.take(MAX_COMPANIONS)).ifEmpty { listOf(fallbackCardId) }
    }

    /** 진행률에 해당하는 단계 문구 (게이지 아래 한 줄) */
    fun messageAt(mode: ExplorationMode, progress: Float): String {
        val list = steps(mode)
        val hit = list.lastOrNull { progress >= it.progress } ?: list.first()
        return hit.message
    }
}
