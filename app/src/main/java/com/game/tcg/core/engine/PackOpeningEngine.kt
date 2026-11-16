package com.game.tcg.core.engine

import com.game.tcg.core.model.PackDraw
import com.game.tcg.core.model.PackOpenResult

/**
 * 카드팩 개봉 연출의 단계.
 *
 * 사용자 요청(2026-11-16): "카드팩 개봉이 텍스트 목록만 나와 밋밋하다 —
 * 빛나는 봉인 카드팩 → 터치 개봉 플래시 → 실제 카드 일러스트가 카드 형태로 등장".
 *
 *  - [SEALED]  : 봉인된 카드팩이 빛나며 기다린다. 화면을 터치하면 봉인이 풀린다.
 *  - [REVEAL]  : 개봉 플래시와 함께 카드가 1장씩 등장한다([PackOpeningState.revealIndex]).
 *  - [SUMMARY] : 획득한 카드를 전부 그리드로 확인하고 닫는다.
 */
enum class PackOpeningPhase {
    /** 연출 없음 (상점 화면의 진열 상태) */
    IDLE,

    /** 봉인된 카드팩 — 터치 대기 */
    SEALED,

    /** 카드 1장씩 등장 */
    REVEAL,

    /** 전체 카드 그리드 */
    SUMMARY
}

/**
 * 카드팩 개봉 연출 상태 — 순수 데이터(Compose·Android 무관)라 단위 테스트로 단계 전이를 검증한다.
 *
 * @param phase       현재 단계
 * @param result      이번 개봉 결과 (IDLE 이면 null)
 * @param revealIndex [REVEAL] 단계에서 화면에 세워진 카드의 순번 (0-based)
 */
data class PackOpeningState(
    val phase: PackOpeningPhase = PackOpeningPhase.IDLE,
    val result: PackOpenResult? = null,
    val revealIndex: Int = 0
) {
    /** 연출이 화면을 덮고 있는지 */
    val isActive: Boolean get() = phase != PackOpeningPhase.IDLE

    /** 이번 개봉에서 나온 카드들 */
    val draws: List<PackDraw> get() = result?.draws ?: emptyList()

    /** 전체 카드 수 */
    val total: Int get() = draws.size

    /** 지금 화면에 세워진 카드 — 봉인 상태에서는 아직 공개되지 않았다 */
    val currentDraw: PackDraw?
        get() = if (phase == PackOpeningPhase.REVEAL || phase == PackOpeningPhase.SUMMARY) {
            draws.getOrNull(revealIndex)
        } else {
            null
        }

    /** 지금까지 공개한 카드 수 — [SEALED] 는 0, [SUMMARY] 는 전체 */
    val revealedCount: Int
        get() = when (phase) {
            PackOpeningPhase.IDLE, PackOpeningPhase.SEALED -> 0
            PackOpeningPhase.REVEAL -> (revealIndex + 1).coerceIn(0, total)
            PackOpeningPhase.SUMMARY -> total
        }

    /** "2 / 5" 형태의 카드 진행 표기 (SEALED 에서는 0 / n) */
    val stepLabel: String get() = "${revealedCount} / $total"

    /** 현재 카드 뒤에 더 남아 있는지 */
    val hasNextCard: Boolean get() = revealIndex < total - 1

    /** 개봉 결과의 신규 계약 수 */
    val newCount: Int get() = result?.newCardCount ?: 0

    /** 개봉 결과의 중복(조각 전환) 수 */
    val duplicateCount: Int get() = total - newCount

    /** 봉인 카드팩 위에 표시할 팩 이름 */
    val packName: String get() = result?.packName ?: "카드팩"

    /** 지금 단계에서 다음 카드로 넘어갈 수 있는지 */
    val canRevealNext: Boolean
        get() = phase == PackOpeningPhase.REVEAL && revealIndex < total
}

/**
 * 카드팩 개봉 연출 상태 전이 엔진 — 순수 함수만 가진다.
 *
 * 화면(`ui/PackShopScreen.kt`)은 이 엔진을 통해서만 단계를 바꾸므로
 * "봉인 해제 → 카드 1장씩 → 전체 보기 → 닫기" 흐름을 JVM 테스트로 그대로 검증할 수 있다.
 */
interface IPackOpeningEngine {

    /** 개봉 결과로 연출 시작 — 봉인 카드팩([PackOpeningPhase.SEALED]) 부터 */
    fun start(result: PackOpenResult): PackOpeningState

    /** 터치로 봉인 해제 — 첫 카드([PackOpeningPhase.REVEAL], index 0)가 등장한다 */
    fun tearSeal(state: PackOpeningState): PackOpeningState

    /** 다음 카드 1장 공개 — 마지막 카드 뒤에는 전체 그리드([PackOpeningPhase.SUMMARY])로 */
    fun revealNext(state: PackOpeningState): PackOpeningState

    /** 전체 보기 — 남은 카드를 한 번에 펼친다 */
    fun revealAll(state: PackOpeningState): PackOpeningState

    /** 연출 닫기 — 상점 진열 상태로 돌아간다 */
    fun close(): PackOpeningState
}

class PackOpeningEngine : IPackOpeningEngine {

    override fun start(result: PackOpenResult): PackOpeningState {
        // 카드가 0장인 결과(비정상)로는 연출을 열지 않는다 — 빈 화면이 덮이지 않게.
        if (result.draws.isEmpty()) return PackOpeningState()
        return PackOpeningState(
            phase = PackOpeningPhase.SEALED,
            result = result,
            revealIndex = 0
        )
    }

    override fun tearSeal(state: PackOpeningState): PackOpeningState {
        if (state.phase != PackOpeningPhase.SEALED) return state
        if (state.total == 0) return PackOpeningState()
        return state.copy(phase = PackOpeningPhase.REVEAL, revealIndex = 0)
    }

    override fun revealNext(state: PackOpeningState): PackOpeningState {
        if (state.phase != PackOpeningPhase.REVEAL) return state
        return if (state.hasNextCard) {
            state.copy(revealIndex = state.revealIndex + 1)
        } else {
            state.copy(phase = PackOpeningPhase.SUMMARY, revealIndex = state.total - 1)
        }
    }

    override fun revealAll(state: PackOpeningState): PackOpeningState {
        if (!state.isActive || state.total == 0) return state
        return state.copy(phase = PackOpeningPhase.SUMMARY, revealIndex = state.total - 1)
    }

    override fun close(): PackOpeningState = PackOpeningState()

    companion object {
        /** 봉인 카드팩 연출에서 「탭하여 봉인 해제」 안내 */
        const val SEALED_PROMPT = "탭하여 봉인 해제"

        /** 카드 공개 단계의 안내 */
        const val REVEAL_PROMPT = "탭하여 다음 카드 ▶"
    }
}
