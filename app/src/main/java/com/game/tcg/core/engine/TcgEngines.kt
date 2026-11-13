package com.game.tcg.core.engine

import com.game.tcg.core.model.*

/**
 * 턴제 TCG 룰 및 상성 데미지 계산 배틀 엔진 인터페이스
 */
interface IBattleEngine {
    fun calculateDamage(attacker: TcgCard, defender: TcgCard): BattleDamageResult
    fun calculateDamageWithTemptation(
        attacker: TcgCard,
        defender: TcgCard,
        penalty: CombatPenalty
    ): BattleDamageResult = calculateDamage(attacker, defender)
}

/**
 * 카드 합성 및 한계 돌파(5성) 엔진 인터페이스
 */
interface ISynthesisEngine {
    fun synthesizeCard(baseCard: TcgCard, materialCard: TcgCard): Boolean
}

/**
 * 현실–이세계 크로스 듀얼 월드 순환 루프 엔진 인터페이스
 */
interface IDualWorldEngine {
    fun interactInRealWorld(card: TcgCard, activity: RealWorldActivity): Int
    fun completeIsekaiBattle(card: TcgCard, earnedExp: Int): Boolean
}

/**
 * 턴제 TCG 룰 및 상성 데미지 계산 배틀 엔진
 */
class BattleEngine : IBattleEngine {

    /**
     * 카드 간 단일 공격 판정 (상성 배율 + 페어 얼티밋 오의 보너스 적용)
     */
    override fun calculateDamage(attacker: TcgCard, defender: TcgCard): BattleDamageResult {
        val baseDamage = attacker.atk.toDouble()
        val multiplier = attacker.element.getDamageMultiplier(defender.element)

        // 호감도 MAX 달성으로 페어 오의가 각성된 경우 추가 데미지 +25%
        val ultimateBonus = if (attacker.isUltimateUnlocked) 1.25 else 1.00

        val finalDamage = (baseDamage * multiplier * ultimateBonus).toInt()
        val isCriticalElement = multiplier >= 1.30

        return BattleDamageResult(
            rawDamage = attacker.atk,
            multiplier = multiplier,
            finalDamage = finalDamage,
            isCriticalElement = isCriticalElement,
            isUltimateTriggered = attacker.isUltimateUnlocked,
            summaryMessage = buildString {
                append("${attacker.name} [${attacker.element.displayName}] → ")
                append("${defender.name} [${defender.element.displayName}] 타격: ")
                append("${finalDamage} 데미지! (상성 ${multiplier}x")
                if (attacker.isUltimateUnlocked) append(", 오의 발동 +25%")
                append(")")
            }
        )
    }

    /**
     * 유혹 페널티(마왕의 매혹)가 적용된 공격 판정
     */
    override fun calculateDamageWithTemptation(
        attacker: TcgCard,
        defender: TcgCard,
        penalty: CombatPenalty
    ): BattleDamageResult {
        val penaltyMultiplier = (100 - penalty.attackReductionPercent).coerceAtLeast(10) / 100.0
        val effectiveAtk = (attacker.atk * penaltyMultiplier).toInt()

        val multiplier = attacker.element.getDamageMultiplier(defender.element)
        val ultimateBonus = if (attacker.isUltimateUnlocked) 1.25 else 1.00

        val finalDamage = (effectiveAtk * multiplier * ultimateBonus).toInt()
        val isCriticalElement = multiplier >= 1.30

        return BattleDamageResult(
            rawDamage = attacker.atk,
            multiplier = multiplier,
            finalDamage = finalDamage,
            isCriticalElement = isCriticalElement,
            isUltimateTriggered = attacker.isUltimateUnlocked,
            summaryMessage = buildString {
                append("${attacker.name} [${attacker.element.displayName}] → ")
                append("${defender.name} [${defender.element.displayName}] 타격: ")
                append("${finalDamage} 데미지! (상성 ${multiplier}x")
                if (attacker.isUltimateUnlocked) append(", 오의 발동 +25%")
                if (penalty.isCharmed) append(", [마왕의 매혹] 공격력 -${penalty.attackReductionPercent}%")
                append(")")
            }
        )
    }
}

data class BattleDamageResult(
    val rawDamage: Int,
    val multiplier: Double,
    val finalDamage: Int,
    val isCriticalElement: Boolean,
    val isUltimateTriggered: Boolean,
    val summaryMessage: String
)

/**
 * 카드 합성 및 한계 돌파(5성) 엔진
 */
class SynthesisEngine : ISynthesisEngine {

    /**
     * 동일 카드 합성을 통한 성급 상승 (최대 5성 한계 돌파)
     * 성급당 공격력/방어력 +20% 영구 스케일링
     */
    override fun synthesizeCard(baseCard: TcgCard, materialCard: TcgCard): Boolean {
        if (baseCard.id != materialCard.id) return false
        if (baseCard.starLevel >= 5) return false

        baseCard.starLevel += 1
        baseCard.atk = (baseCard.atk * 1.20).toInt()
        baseCard.def = (baseCard.def * 1.20).toInt()
        return true
    }
}

/**
 * 현실–이세계 크로스 듀얼 월드 순환 루프 엔진
 * - 현실 소환 모드: 일상 평상복/데이트 룩 교감, 호감도 상승 -> 페어 얼티밋 오의 개방
 * - 이세계 원정 모드: 턴제 배틀 클리어, EXP 획득 -> 레벨업 및 영구 스탯 성장
 */
class DualWorldEngine : IDualWorldEngine {

    /**
     * 현실 소환 모드: 일상 교감으로 호감도 육성
     */
    override fun interactInRealWorld(card: TcgCard, activity: RealWorldActivity): Int {
        val bondIncrease = when (activity) {
            RealWorldActivity.DAILY_GREETING -> 5
            RealWorldActivity.CASUAL_CAFE_WALK -> 15
            RealWorldActivity.ROMANTIC_EVENING_DATE -> 30
        }
        card.bondLevel = (card.bondLevel + bondIncrease).coerceAtMost(100)

        // 비주얼 스킨 자동 전환
        card.currentVisual = when {
            activity == RealWorldActivity.ROMANTIC_EVENING_DATE -> VisualState.ROMANTIC_DATE
            card.bondLevel >= 50 -> VisualState.CASUAL_CUTE
            else -> VisualState.BATTLE
        }

        return card.bondLevel
    }

    /**
     * 이세계 원정 모드: 배틀 승리를 통해 EXP 획득 및 영구 능력치 성장
     */
    override fun completeIsekaiBattle(card: TcgCard, earnedExp: Int): Boolean {
        card.currentExp += earnedExp
        val expThreshold = card.starLevel * 1000

        if (card.currentExp >= expThreshold) {
            card.currentExp -= expThreshold
            card.atk += 150
            card.def += 100
            return true // 레벨업 성공
        }
        return false
    }
}

enum class RealWorldActivity {
    DAILY_GREETING,
    CASUAL_CAFE_WALK,
    ROMANTIC_EVENING_DATE
}

/**
 * 덱 및 핸드 관리 엔진 인터페이스
 */
interface IDeckEngine {
    val maxDeckSize: Int get() = 30
    val maxCardCopies: Int get() = 3
    val initialHandSize: Int get() = 5
    val maxHandSize: Int get() = 10

    /**
     * 30장 덱 규칙 및 동일 카드 최대 3장 제한 검증, 코스트 커브 분석
     */
    fun validateDeck(deck: List<TcgCard>): DeckValidationResult

    /**
     * 초기 5장 오프닝 핸드 드로우
     */
    fun drawInitialHand(deck: MutableList<TcgCard>): List<TcgCard>

    /**
     * 1회 한정 멀리건: 교체할 카드들을 덱에 다시 섞고 동일한 매수만큼 재드로우
     */
    fun performMulligan(
        deck: MutableList<TcgCard>,
        currentHand: MutableList<TcgCard>,
        cardsToReplace: List<TcgCard>
    ): List<TcgCard>

    /**
     * 턴 시작 드로우 (1장), 최대 핸드(10장) 초과 시 소멸(Burn), 덱 소진 시 탈진 데미지(1씩 누적 증가)
     */
    fun drawTurnCard(
        deck: MutableList<TcgCard>,
        hand: MutableList<TcgCard>,
        fatigueCount: Int
    ): DrawResult
}

/**
 * 마나 자원 관리 엔진 인터페이스
 */
interface IManaEngine {
    val maxManaCap: Int get() = 10

    /**
     * 턴 수(1~10+)에 따른 마나 상태 초기화/업그레이드
     * 턴 1=1마나, 턴 2=2마나 ... 최대 10마나
     */
    fun calculateMaxManaForTurn(turnNumber: Int): Int

    /**
     * 턴 시작 시 최대 마나로 완전 충전
     */
    fun replenishTurnMana(currentMaxMana: Int): ManaState

    /**
     * 소환 마나 코스트 차감 가능 여부 검증
     */
    fun canAfford(manaState: ManaState, cost: Int): Boolean

    /**
     * 소환 마나 코스트 차감 (엄격한 한계 검증: 잔여 마나 미만 시 예외 또는 실패 처리)
     */
    fun deductCost(manaState: ManaState, cost: Int): ManaState
}

/**
 * 전열/후열 슬롯 및 배틀 AI 엔진 인터페이스
 */
interface IBattleAiEngine {
    /**
     * 전열 3슬롯 / 후열 2슬롯 방어 규칙:
     * 전열에 생존해 있는 아군 카드가 하나라도 있다면 후열 카드는 지정(타겟) 불가능
     */
    fun getValidTargetSlots(enemySlots: List<BattleSlot>): List<BattleSlot>

    /**
     * 배틀 AI 타겟팅 전략:
     * 타겟 가능한 적 슬롯 중 공격자의 속성에 취약한(카운터) 적을 최우선 타겟팅
     * 우선순위:
     * 1. 2극원소 상호 카운터 (빛 <-> 어둠 : 1.5x)
     * 2. 3원소 삼각 순환 우위 (화염 -> 바람 -> 빙결 -> 화염 : 1.3x)
     * 3. 중립 (1.0x)
     * 4. 역상성 (0.8x)
     * 동률일 경우 공격력이 가장 높은 적 우선 타겟
     */
    fun selectOptimalTarget(attacker: TcgCard, enemySlots: List<BattleSlot>): BattleSlot?
}

/**
 * 덱 및 핸드 관리 엔진 기본 구현체
 */
class DeckEngine : IDeckEngine {
    override fun validateDeck(deck: List<TcgCard>): DeckValidationResult {
        val errors = mutableListOf<String>()

        if (deck.size != maxDeckSize) {
            errors.add("덱은 정확히 ${maxDeckSize}장이어야 합니다. (현재: ${deck.size}장)")
        }

        val cardCounts = deck.groupingBy { it.id }.eachCount()
        for ((cardId, count) in cardCounts) {
            if (count > maxCardCopies) {
                val cardName = deck.firstOrNull { it.id == cardId }?.name ?: "ID:$cardId"
                errors.add("카드 '${cardName}'(ID: $cardId)의 매수가 초과되었습니다: ${count}장 (최대 ${maxCardCopies}장)")
            }
        }

        val costCurve = deck.groupingBy { it.baseCost }.eachCount().toSortedMap()
        val averageCost = if (deck.isNotEmpty()) deck.map { it.baseCost }.average() else 0.0

        return DeckValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            costCurve = costCurve,
            averageCost = (averageCost * 10.0).toInt() / 10.0
        )
    }

    override fun drawInitialHand(deck: MutableList<TcgCard>): List<TcgCard> {
        val initialCards = mutableListOf<TcgCard>()
        val drawCount = initialHandSize.coerceAtMost(deck.size)
        repeat(drawCount) {
            if (deck.isNotEmpty()) {
                initialCards.add(deck.removeAt(0))
            }
        }
        return initialCards
    }

    override fun performMulligan(
        deck: MutableList<TcgCard>,
        currentHand: MutableList<TcgCard>,
        cardsToReplace: List<TcgCard>
    ): List<TcgCard> {
        val countToReplace = cardsToReplace.size
        // 핸드에서 교체할 카드 제거
        for (card in cardsToReplace) {
            val removedIndex = currentHand.indexOfFirst { it.id == card.id }
            if (removedIndex != -1) {
                currentHand.removeAt(removedIndex)
            }
        }

        // 교체 카드 덱으로 복귀 및 셔플 (테스트 결정론 유지를 위해 순서대로 삽입)
        deck.addAll(cardsToReplace)

        // 동일한 장수만큼 덱 맨 위에서 새로 드로우
        val newDrawn = mutableListOf<TcgCard>()
        repeat(countToReplace) {
            if (deck.isNotEmpty()) {
                val card = deck.removeAt(0)
                newDrawn.add(card)
                currentHand.add(card)
            }
        }
        return currentHand
    }

    override fun drawTurnCard(
        deck: MutableList<TcgCard>,
        hand: MutableList<TcgCard>,
        fatigueCount: Int
    ): DrawResult {
        if (deck.isEmpty()) {
            val nextFatigue = fatigueCount + 1
            return DrawResult(
                drawnCard = null,
                isBurned = false,
                fatigueDamage = nextFatigue,
                newFatigueCount = nextFatigue
            )
        }

        val card = deck.removeAt(0)
        return if (hand.size >= maxHandSize) {
            // 핸드가 가득 찬 상태: 카드가 버려짐 (Burned)
            DrawResult(
                drawnCard = card,
                isBurned = true,
                fatigueDamage = 0,
                newFatigueCount = fatigueCount
            )
        } else {
            hand.add(card)
            DrawResult(
                drawnCard = card,
                isBurned = false,
                fatigueDamage = 0,
                newFatigueCount = fatigueCount
            )
        }
    }
}

/**
 * 마나 자원 관리 엔진 기본 구현체
 */
class ManaEngine : IManaEngine {
    override fun calculateMaxManaForTurn(turnNumber: Int): Int {
        val turn = turnNumber.coerceAtLeast(1)
        return turn.coerceAtMost(maxManaCap)
    }

    override fun replenishTurnMana(currentMaxMana: Int): ManaState {
        val boundedMax = currentMaxMana.coerceIn(1, maxManaCap)
        return ManaState(currentMana = boundedMax, maxMana = boundedMax)
    }

    override fun canAfford(manaState: ManaState, cost: Int): Boolean {
        if (cost < 0) return false
        return manaState.currentMana >= cost
    }

    override fun deductCost(manaState: ManaState, cost: Int): ManaState {
        require(cost >= 0) { "마나 코스트는 음수일 수 없습니다: $cost" }
        require(manaState.currentMana >= cost) {
            "마나가 부족합니다. (현재: ${manaState.currentMana}, 필요: $cost)"
        }
        return manaState.copy(currentMana = manaState.currentMana - cost)
    }
}

/**
 * 전열/후열 방어 및 배틀 AI 타겟팅 엔진 기본 구현체
 */
class BattleAiEngine : IBattleAiEngine {
    override fun getValidTargetSlots(enemySlots: List<BattleSlot>): List<BattleSlot> {
        val occupiedSlots = enemySlots.filter { it.placedCard != null }
        if (occupiedSlots.isEmpty()) return emptyList()

        val activeFrontlineSlots = occupiedSlots.filter { it.isFrontRow }
        return if (activeFrontlineSlots.isNotEmpty()) {
            // 전열에 생존 유닛이 있으면 전열만 공격 가능
            activeFrontlineSlots
        } else {
            // 전열이 전멸한 경우 후열 유닛 공격 가능
            occupiedSlots.filter { !it.isFrontRow }
        }
    }

    override fun selectOptimalTarget(attacker: TcgCard, enemySlots: List<BattleSlot>): BattleSlot? {
        val validSlots = getValidTargetSlots(enemySlots)
        if (validSlots.isEmpty()) return null

        // 상성 배율 높은 순, 같으면 ATK 높은 적 순 정렬
        return validSlots.maxWithOrNull(
            compareBy<BattleSlot> { slot ->
                val targetCard = slot.placedCard!!
                attacker.element.getDamageMultiplier(targetCard.element)
            }.thenBy { slot ->
                slot.placedCard!!.atk
            }
        )
    }
}

/**
 * 마왕 도감 시선/확대 응시 및 일상 잠입 유혹 페널티 엔진 인터페이스
 */
interface ITemptationEngine {
    fun recordGaze(demonLordId: Int, durationSeconds: Long, isZoomed: Boolean, state: TemptationState): TemptationState
    fun processUndercoverChoice(option: UndercoverDialogueOption, state: TemptationState): TemptationState
    fun calculateCombatPenalty(state: TemptationState): CombatPenalty
    fun applyCombatPenaltyToAttacker(baseDamage: Int, penalty: CombatPenalty): Int
    fun applyDamageTakenPenalty(baseIncomingDamage: Int, penalty: CombatPenalty): Int
    fun onDemonLordDefeated(demonLord: DemonLord, state: TemptationState): TemptationState
    fun purifyTemptationWithHolyWater(state: TemptationState, purifyAmount: Int): TemptationState
}

/**
 * 마왕 도감 시선/확대 응시 및 일상 잠입 유혹 페널티 엔진 기본 구현체
 */
class TemptationEngine : ITemptationEngine {

    override fun recordGaze(
        demonLordId: Int,
        durationSeconds: Long,
        isZoomed: Boolean,
        state: TemptationState
    ): TemptationState {
        val updated = state.copy()
        updated.gazeDurationSeconds += durationSeconds

        if (isZoomed) {
            updated.gazeZoomCount += 1
            // 핀치 줌 1회당 즉시 1스택 추가 (최대 5스택)
            updated.gazeTemptationStacks = (updated.gazeTemptationStacks + 1).coerceAtMost(5)
        }

        // 10초 이상 응시할 때마다 1스택씩 추가 (기본 스택에 누적 반영, 최대 5스택)
        val timeStacks = (updated.gazeDurationSeconds / 10).toInt().coerceAtMost(5)
        if (timeStacks > updated.gazeTemptationStacks) {
            updated.gazeTemptationStacks = timeStacks
        }

        return updated
    }

    override fun processUndercoverChoice(
        option: UndercoverDialogueOption,
        state: TemptationState
    ): TemptationState {
        val updated = state.copy()
        updated.undercoverEncounterCount += 1

        if (option.isTemptedChoice) {
            updated.undercoverFlirtCount += 1
            updated.curseOfArchdemonStacks = (updated.curseOfArchdemonStacks + option.curseAdded).coerceAtMost(5)
            updated.stolenGold += option.goldStolen
        }

        return updated
    }

    override fun calculateCombatPenalty(state: TemptationState): CombatPenalty {
        val total = state.totalTemptationStacks
        val atkRed = (total * 5).coerceAtMost(35)
        val dmgInc = (total * 5).coerceAtMost(35)
        val critRed = (total * 4).coerceAtMost(25)
        val manaCost = if (total >= 3) 1 else 0
        val isCharmed = total > 0

        val desc = if (isCharmed) {
            "[마왕의 매혹: 총 ${total}스택] 아군 공격력 -${atkRed}%, 받는 피해 +${dmgInc}%, 치명타율 -${critRed}%" +
                    (if (manaCost > 0) ", 스킬 마나 코스트 +$manaCost" else "")
        } else {
            "[정신 맑음] 유혹에 흔들리지 않아 전투 페널티가 없습니다."
        }

        return CombatPenalty(
            totalStacks = total,
            attackReductionPercent = atkRed,
            damageTakenIncreasePercent = dmgInc,
            criticalReductionPercent = critRed,
            extraManaCost = manaCost,
            isCharmed = isCharmed,
            debuffDescription = desc
        )
    }

    override fun applyCombatPenaltyToAttacker(baseDamage: Int, penalty: CombatPenalty): Int {
        if (!penalty.isCharmed) return baseDamage
        val multiplier = (100 - penalty.attackReductionPercent).coerceAtLeast(10) / 100.0
        return (baseDamage * multiplier).toInt()
    }

    override fun applyDamageTakenPenalty(baseIncomingDamage: Int, penalty: CombatPenalty): Int {
        if (!penalty.isCharmed) return baseIncomingDamage
        val multiplier = (100 + penalty.damageTakenIncreasePercent) / 100.0
        return (baseIncomingDamage * multiplier).toInt()
    }

    override fun onDemonLordDefeated(demonLord: DemonLord, state: TemptationState): TemptationState {
        demonLord.isDefeated = true
        demonLord.isVowedCompanion = true

        val updated = state.copy()
        // 토벌 성공 시 저주 스택 3 완화
        updated.curseOfArchdemonStacks = (updated.curseOfArchdemonStacks - 3).coerceAtLeast(0)
        return updated
    }

    override fun purifyTemptationWithHolyWater(
        state: TemptationState,
        purifyAmount: Int
    ): TemptationState {
        val updated = state.copy()
        updated.gazeTemptationStacks = (updated.gazeTemptationStacks - purifyAmount).coerceAtLeast(0)
        updated.curseOfArchdemonStacks = (updated.curseOfArchdemonStacks - purifyAmount).coerceAtLeast(0)
        return updated
    }
}
