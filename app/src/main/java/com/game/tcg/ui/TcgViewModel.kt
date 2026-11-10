package com.game.tcg.ui

import androidx.lifecycle.ViewModel
import com.game.tcg.core.engine.*
import com.game.tcg.core.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Android UI 상태 모델 (Clean Architecture / MVVM)
 */
data class TcgUiState(
    val statusMessage: String = "듀얼 게이트 TCG 엔진 준비 완료",
    val lastBattleResult: BattleDamageResult? = null,
    val partnerCard: TcgCard = TcgCard(
        id = 51,
        name = "백호령 신녀 단아",
        element = Element.WIND,
        baseCost = 7,
        atk = 7400,
        def = 6200,
        bondLevel = 20,
        skillText = "아군 공격 속도 및 회피율 대폭 상승"
    ),
    val synthesisTargetCard: TcgCard = TcgCard(
        id = 1,
        name = "태양의 대천사 세라피엘",
        element = Element.LIGHT,
        baseCost = 8,
        atk = 8500,
        def = 7500,
        starLevel = 1,
        skillText = "필드 전개 시 아군 전체 실드 부여"
    )
)

/**
 * UI 계약 인터페이스
 */
interface ITcgUiContract {
    val uiState: StateFlow<TcgUiState>
    fun executeBattleSimulation()
    fun executeRealWorldInteraction(activity: RealWorldActivity = RealWorldActivity.ROMANTIC_EVENING_DATE)
    fun executeSynthesisAscension()
    fun executeIsekaiExpedition(earnedExp: Int = 1200)
}

/**
 * Jetpack Compose UI용 ViewModel
 */
class TcgViewModel(
    private val battleEngine: IBattleEngine = BattleEngine(),
    private val synthesisEngine: ISynthesisEngine = SynthesisEngine(),
    private val dualWorldEngine: IDualWorldEngine = DualWorldEngine()
) : ViewModel(), ITcgUiContract {

    private val _uiState = MutableStateFlow(TcgUiState())
    override val uiState: StateFlow<TcgUiState> = _uiState.asStateFlow()

    override fun executeBattleSimulation() {
        val seraphiel = TcgCard(
            id = 1,
            name = "태양의 대천사 세라피엘",
            element = Element.LIGHT,
            baseCost = 8,
            atk = 8500,
            def = 7500,
            bondLevel = 100, // 오의 개방
            skillText = "필드 전개 시 아군 전체 실드 부여"
        )
        val carmilla = TcgCard(
            id = 6,
            name = "뱀파이어 퀸 카밀라",
            element = Element.DARK,
            baseCost = 8,
            atk = 8200,
            def = 7100,
            skillText = "피해의 30% 생명력 흡수"
        )

        val result = battleEngine.calculateDamage(seraphiel, carmilla)
        val message = "[배틀 상성 연산 결과]\n${result.summaryMessage}\n원 데미지: ${result.rawDamage} → 최종 데미지: ${result.finalDamage} (빛↔어둠 상극 +50%, 오의 +25%)"

        _uiState.update { current ->
            current.copy(
                statusMessage = message,
                lastBattleResult = result
            )
        }
    }

    override fun executeRealWorldInteraction(activity: RealWorldActivity) {
        _uiState.update { current ->
            val updatedPartner = current.partnerCard.copy()
            val newBond = dualWorldEngine.interactInRealWorld(updatedPartner, activity)
            val message = "[현실 소환 교감 결과]\n캐릭터: ${updatedPartner.name}\n현재 비주얼 상태: ${updatedPartner.currentVisual.displayName}\n호감도: ${newBond}/100"
            current.copy(
                statusMessage = message,
                partnerCard = updatedPartner
            )
        }
    }

    override fun executeSynthesisAscension() {
        _uiState.update { current ->
            val base = current.synthesisTargetCard.copy()
            val material = current.synthesisTargetCard.copy()
            val previousAtk = base.atk
            val previousDef = base.def
            val success = synthesisEngine.synthesizeCard(base, material)

            val message = if (success) {
                "[카드 합성 결과]\n한계 돌파: ${base.starLevel}성 달성!\nATK 스케일링: $previousAtk → ${base.atk} (+20%)\nDEF 스케일링: $previousDef → ${base.def} (+20%)"
            } else {
                "[카드 합성 실패]\n더 이상 합성할 수 없거나 카드가 일치하지 않습니다. (현재: ${base.starLevel}성)"
            }

            current.copy(
                statusMessage = message,
                synthesisTargetCard = if (success) base else current.synthesisTargetCard
            )
        }
    }

    override fun executeIsekaiExpedition(earnedExp: Int) {
        _uiState.update { current ->
            val updatedPartner = current.partnerCard.copy()
            val prevAtk = updatedPartner.atk
            val prevDef = updatedPartner.def
            val leveledUp = dualWorldEngine.completeIsekaiBattle(updatedPartner, earnedExp)

            val message = if (leveledUp) {
                "[이세계 원정 배틀 승리 - 스탯 영구 성장!]\nEXP +$earnedExp 획득! 레벨업 달성!\nATK: $prevAtk → ${updatedPartner.atk} (+150)\nDEF: $prevDef → ${updatedPartner.def} (+100)"
            } else {
                "[이세계 원정 배틀 완료]\nEXP +$earnedExp 획득 (누적: ${updatedPartner.currentExp}/${updatedPartner.starLevel * 1000})"
            }

            current.copy(
                statusMessage = message,
                partnerCard = updatedPartner
            )
        }
    }
}
