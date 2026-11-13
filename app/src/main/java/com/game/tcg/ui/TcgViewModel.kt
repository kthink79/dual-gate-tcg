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
    val currentTurn: Int = 1,
    val manaState: ManaState = ManaState(1, 1),
    val playerDeck: List<TcgCard> = emptyList(),
    val playerHand: List<TcgCard> = emptyList(),
    val fatigueCount: Int = 0,
    val mulliganUsed: Boolean = false,
    val playerFieldSlots: List<BattleSlot> = FieldSlotFactory.createStandardSlots(),
    val enemyFieldSlots: List<BattleSlot> = FieldSlotFactory.createStandardSlots(),
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
    ),
    val demonLords: List<DemonLord> = DemonLordRegistry.ALL,
    val temptationState: TemptationState = TemptationState(),
    val combatPenalty: CombatPenalty = CombatPenalty(
        totalStacks = 0,
        attackReductionPercent = 0,
        damageTakenIncreasePercent = 0,
        criticalReductionPercent = 0,
        extraManaCost = 0,
        isCharmed = false,
        debuffDescription = "[정신 맑음] 유혹에 흔들리지 않아 전투 페널티가 없습니다."
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

    // 신규 추가된 덱/마나/AI 턴제 인터랙션 계약
    fun startNewGame(deckCards: List<TcgCard>? = null)
    fun executeMulligan(cardIndicesToReplace: List<Int>)
    fun executeTurnDraw()
    fun executePlayCard(cardIndex: Int, slotIndex: Int): Boolean
    fun executeAiAction(): String?

    // 마왕 도감 시선/확대 응시 및 일상 잠입 유혹 인터랙션 계약
    fun recordDemonLordGaze(demonLordId: Int, durationSeconds: Long, isZoomed: Boolean)
    fun chooseUndercoverOption(option: UndercoverDialogueOption)
    fun defeatDemonLord(demonLordId: Int)
    fun purifyTemptation(amount: Int = 2)
}

/**
 * Jetpack Compose UI용 ViewModel
 */
class TcgViewModel(
    private val battleEngine: IBattleEngine = BattleEngine(),
    private val synthesisEngine: ISynthesisEngine = SynthesisEngine(),
    private val dualWorldEngine: IDualWorldEngine = DualWorldEngine(),
    private val deckEngine: IDeckEngine = DeckEngine(),
    private val manaEngine: IManaEngine = ManaEngine(),
    private val battleAiEngine: IBattleAiEngine = BattleAiEngine(),
    private val temptationEngine: ITemptationEngine = TemptationEngine()
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

        val penalty = _uiState.value.combatPenalty
        val result = if (penalty.isCharmed) {
            battleEngine.calculateDamageWithTemptation(seraphiel, carmilla, penalty)
        } else {
            battleEngine.calculateDamage(seraphiel, carmilla)
        }
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

    override fun startNewGame(deckCards: List<TcgCard>?) {
        val cards = deckCards?.toMutableList() ?: createSample30Deck()
        val hand = deckEngine.drawInitialHand(cards).toMutableList()
        val initialMana = manaEngine.replenishTurnMana(manaEngine.calculateMaxManaForTurn(1))

        _uiState.update { current ->
            current.copy(
                currentTurn = 1,
                manaState = initialMana,
                playerDeck = cards.toList(),
                playerHand = hand.toList(),
                fatigueCount = 0,
                mulliganUsed = false,
                playerFieldSlots = FieldSlotFactory.createStandardSlots(),
                enemyFieldSlots = FieldSlotFactory.createStandardSlots(),
                statusMessage = "[게임 시작] 턴 1 시작! 오프닝 핸드 5장 드로우 완료 (잔여 덱: ${cards.size}장, 마나: ${initialMana.currentMana}/${initialMana.maxMana})"
            )
        }
    }

    override fun executeMulligan(cardIndicesToReplace: List<Int>) {
        val current = _uiState.value
        if (current.mulliganUsed) {
            _uiState.update { it.copy(statusMessage = "[멀리건 실패] 멀리건은 게임당 1회만 가능합니다.") }
            return
        }

        val deck = current.playerDeck.toMutableList()
        val hand = current.playerHand.toMutableList()
        val cardsToReplace = cardIndicesToReplace.mapNotNull { hand.getOrNull(it) }

        val newHand = deckEngine.performMulligan(deck, hand, cardsToReplace)

        _uiState.update {
            it.copy(
                playerDeck = deck.toList(),
                playerHand = newHand.toList(),
                mulliganUsed = true,
                statusMessage = "[멀리건 완료] ${cardsToReplace.size}장 교체 완료 (현재 핸드: ${newHand.size}장)"
            )
        }
    }

    override fun executeTurnDraw() {
        val current = _uiState.value
        val nextTurn = current.currentTurn + 1
        val maxMana = manaEngine.calculateMaxManaForTurn(nextTurn)
        val replenishedMana = manaEngine.replenishTurnMana(maxMana)

        val deck = current.playerDeck.toMutableList()
        val hand = current.playerHand.toMutableList()

        val drawResult = deckEngine.drawTurnCard(deck, hand, current.fatigueCount)

        val statusMsg = when {
            drawResult.fatigueDamage > 0 -> {
                "[턴 $nextTurn 드로우] 덱이 소진되었습니다! 탈진 데미지 ${drawResult.fatigueDamage} 발생! (누적 탈진: ${drawResult.newFatigueCount})"
            }
            drawResult.isBurned -> {
                "[턴 $nextTurn 드로우] 핸드가 가득 찼습니다(10장)! 카드 '${drawResult.drawnCard?.name}'가 소멸(Burn)되었습니다."
            }
            else -> {
                "[턴 $nextTurn 드로우] 카드 '${drawResult.drawnCard?.name}'(코스트 ${drawResult.drawnCard?.baseCost}) 획득! (핸드: ${hand.size}장, 마나: ${replenishedMana.currentMana}/${replenishedMana.maxMana})"
            }
        }

        _uiState.update {
            it.copy(
                currentTurn = nextTurn,
                manaState = replenishedMana,
                playerDeck = deck.toList(),
                playerHand = hand.toList(),
                fatigueCount = drawResult.newFatigueCount,
                statusMessage = statusMsg
            )
        }
    }

    override fun executePlayCard(cardIndex: Int, slotIndex: Int): Boolean {
        val current = _uiState.value
        val hand = current.playerHand.toMutableList()
        val cardToPlay = hand.getOrNull(cardIndex) ?: run {
            _uiState.update { it.copy(statusMessage = "[소환 실패] 지정된 핸드 카드가 유효하지 않습니다.") }
            return false
        }

        val slots = current.playerFieldSlots.map { it.copy() }
        val targetSlot = slots.getOrNull(slotIndex) ?: run {
            _uiState.update { it.copy(statusMessage = "[소환 실패] 지정된 슬롯이 존재하지 않습니다.") }
            return false
        }

        if (targetSlot.placedCard != null) {
            _uiState.update { it.copy(statusMessage = "[소환 실패] 슬롯 ${slotIndex}에 이미 유닛이 배치되어 있습니다.") }
            return false
        }

        if (!manaEngine.canAfford(current.manaState, cardToPlay.baseCost)) {
            _uiState.update {
                it.copy(
                    statusMessage = "[소환 실패] 마나가 부족합니다! (필요: ${cardToPlay.baseCost}, 보유: ${current.manaState.currentMana})"
                )
            }
            return false
        }

        val updatedMana = manaEngine.deductCost(current.manaState, cardToPlay.baseCost)
        hand.removeAt(cardIndex)
        targetSlot.placedCard = cardToPlay

        val rowName = if (targetSlot.isFrontRow) "전열" else "후열"
        _uiState.update {
            it.copy(
                manaState = updatedMana,
                playerHand = hand.toList(),
                playerFieldSlots = slots,
                statusMessage = "[유닛 소환 성공] '${cardToPlay.name}' [${cardToPlay.element.displayName}] 카드를 $rowName 슬롯 $slotIndex 에 소환! (잔여 마나: ${updatedMana.currentMana}/${updatedMana.maxMana})"
            )
        }
        return true
    }

    override fun executeAiAction(): String? {
        val current = _uiState.value
        val enemyOccupied = current.enemyFieldSlots.filter { it.placedCard != null }
        if (enemyOccupied.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "[AI 행동 불가] 필드에 적 유닛이 없습니다.") }
            return null
        }

        val attacker = enemyOccupied.first().placedCard!!
        val targetSlot = battleAiEngine.selectOptimalTarget(attacker, current.playerFieldSlots)

        return if (targetSlot != null) {
            val defender = targetSlot.placedCard!!
            val result = battleEngine.calculateDamage(attacker, defender)
            val rowName = if (targetSlot.isFrontRow) "전열" else "후열"
            val message = "[AI 공격 감행] 적 '${attacker.name}'이 아군 $rowName 슬롯 ${targetSlot.slotIndex}의 '${defender.name}'을 최우선 타겟 지정!\n${result.summaryMessage}"

            _uiState.update {
                it.copy(
                    statusMessage = message,
                    lastBattleResult = result
                )
            }
            message
        } else {
            val message = "[AI 턴 종료] 아군 진영에 타겟팅 가능한 유닛이 없습니다."
            _uiState.update { it.copy(statusMessage = message) }
            message
        }
    }

    private fun createSample30Deck(): MutableList<TcgCard> {
        val elements = Element.entries.toTypedArray()
        val cards = mutableListOf<TcgCard>()
        for (i in 1..30) {
            val elem = elements[i % elements.size]
            val cost = ((i % 8) + 1)
            cards.add(
                TcgCard(
                    id = (i % 10) + 1, // 1~10번 카드 각 3장씩 구성 (최대 3장 규칙 준수)
                    name = "수호자 #$i",
                    element = elem,
                    baseCost = cost,
                    atk = 1000 + (cost * 200),
                    def = 1000 + (cost * 150),
                    skillText = "필드 전개 스킬"
                )
            )
        }
        return cards
    }

    override fun recordDemonLordGaze(demonLordId: Int, durationSeconds: Long, isZoomed: Boolean) {
        val current = _uiState.value
        val updatedState = temptationEngine.recordGaze(demonLordId, durationSeconds, isZoomed, current.temptationState)
        val penalty = temptationEngine.calculateCombatPenalty(updatedState)

        val targetLord = current.demonLords.find { it.id == demonLordId }
        val lordName = targetLord?.name ?: "마왕"

        val actionDesc = if (isZoomed) "상세 사진 핀치 줌 확대 감지" else "${durationSeconds}초간 응시"
        val message = "[마왕의 시선 유혹] $lordName ($actionDesc)\n${penalty.debuffDescription}"

        _uiState.update {
            it.copy(
                temptationState = updatedState,
                combatPenalty = penalty,
                statusMessage = message
            )
        }
    }

    override fun chooseUndercoverOption(option: UndercoverDialogueOption) {
        val current = _uiState.value
        val updatedState = temptationEngine.processUndercoverChoice(option, current.temptationState)
        val penalty = temptationEngine.calculateCombatPenalty(updatedState)

        val message = if (option.isTemptedChoice) {
            "[일상 잠입 유혹에 흔들림] '${option.text}' 선택!\n마왕의 저주 +${option.curseAdded}스택, 금화 -${option.goldStolen}G 유출!\n${penalty.debuffDescription}"
        } else {
            "[유혹 거부] '${option.text}' 선택! 단호한 태도로 마왕의 유혹을 뿌리쳤습니다."
        }

        _uiState.update {
            it.copy(
                temptationState = updatedState,
                combatPenalty = penalty,
                statusMessage = message
            )
        }
    }

    override fun defeatDemonLord(demonLordId: Int) {
        val current = _uiState.value
        val targetLord = current.demonLords.find { it.id == demonLordId }
        if (targetLord == null) return

        val updatedState = temptationEngine.onDemonLordDefeated(targetLord, current.temptationState)
        val penalty = temptationEngine.calculateCombatPenalty(updatedState)
        val updatedLords = current.demonLords.map {
            if (it.id == demonLordId) it.copy(isDefeated = true, isVowedCompanion = true) else it
        }

        val message = "[마왕 토벌 완료] '${targetLord.name} (${targetLord.title})'을 정벌하여 세계를 구원했습니다!\n" +
                "일상 위장 신분('${targetLord.disguiseHeroineName}')의 가면을 벗고 '진정한 동료'로 합류!\n" +
                "마왕의 저주가 정화되었습니다. (현재 페널티: ${penalty.debuffDescription})"

        _uiState.update {
            it.copy(
                demonLords = updatedLords,
                temptationState = updatedState,
                combatPenalty = penalty,
                statusMessage = message
            )
        }
    }

    override fun purifyTemptation(amount: Int) {
        val current = _uiState.value
        val updatedState = temptationEngine.purifyTemptationWithHolyWater(current.temptationState, amount)
        val penalty = temptationEngine.calculateCombatPenalty(updatedState)

        val message = "[성수 정화] 성수를 사용하여 마왕의 매혹 및 저주 스택을 ${amount}만큼 정화했습니다.\n${penalty.debuffDescription}"

        _uiState.update {
            it.copy(
                temptationState = updatedState,
                combatPenalty = penalty,
                statusMessage = message
            )
        }
    }
}
