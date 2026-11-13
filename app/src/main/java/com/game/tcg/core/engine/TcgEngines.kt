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
        }

        // 시선 유혹 스택 = 핀치 줌 확대 스택 + 10초당 응시 스택 (합산 후 최대 5스택)
        // (v0.3 수정: 기존 max() 방식은 줌 1회 + 12초 응시 시 1스택으로 과소 계산되어
        //  화면 줌 즉시 1스택 + 10초당 1스택 누적 규칙과 어긋났음)
        val zoomStacks = updated.gazeZoomCount.coerceAtMost(5)
        val timeStacks = (updated.gazeDurationSeconds / 10).toInt().coerceAtMost(5)
        updated.gazeTemptationStacks = (zoomStacks + timeStacks).coerceAtMost(5)

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

// =============================================================================
// v0.3 로그라이크 영구 상실·기억 초기화 환생 엔진
// =============================================================================

/**
 * 사망 상실·환생 엔진 인터페이스
 * - 사망한 캐릭터 카드는 컬렉션과 덱에서 즉시 제거되고, 성장·기억이 0으로 초기화된 채
 *   환생 풀(reincarnationPool)에 등록된다.
 * - 1캐릭터 = 유일한 1장 원칙: 보유 중인 캐릭터는 중복 획득할 수 없다.
 */
interface IRoguelikeLifecycleEngine {
    val collection: RoguelikeCollection

    /** 카드 원본 템플릿 등록 (도감 번호 기준) */
    fun registerTemplate(template: TcgCard)

    /** 이미 보유 중인 카드를 컬렉션에 등록 (보유 카드 시딩용, 중복은 무시) */
    fun registerOwnedCard(card: TcgCard)

    /**
     * 전투 중 HP 0 사망 처리
     * - 덱/보유 목록에서 제거, 강화·교감 기억 초기화, 환생 풀 등록
     */
    fun onCardDied(
        cardId: Int,
        causeOfDeath: String = "전장에서 HP가 0이 되었다",
        diedAtTurn: Int = 0
    ): CardDeathResult

    /**
     * 카드 획득 (가챠·던전 드랍)
     * - 이미 보유 중이면 중복 획득 차단 결과 반환
     * - 환생 풀에 있던 캐릭터면 기억·강화가 리셋된 초기 상태로 재획득
     */
    fun acquireCard(templateId: Int): AcquisitionResult

    /** 템플릿 객체로 직접 획득 (템플릿 자동 등록) */
    fun acquireCard(template: TcgCard): AcquisitionResult
}

/**
 * 사망 상실·환생 엔진 기본 구현체
 */
class RoguelikeLifecycleEngine(
    initialTemplates: List<TcgCard> = emptyList()
) : IRoguelikeLifecycleEngine {

    private val _collection = RoguelikeCollection()
    private val templates: MutableMap<Int, TcgCard> = linkedMapOf()

    init {
        initialTemplates.forEach { registerTemplate(it) }
    }

    override val collection: RoguelikeCollection
        get() = _collection

    override fun registerTemplate(template: TcgCard) {
        templates[template.id] = template.copy()
    }

    override fun registerOwnedCard(card: TcgCard) {
        if (!templates.containsKey(card.id)) {
            templates[card.id] = card.copy()
        }
        if (_collection.isOwned(card.id)) return
        // 재획득 대기 중이던 카드가 실제로 보유 목록에 들어오면 풀에서 제거
        _collection.reincarnationPool.remove(card.id)
        _collection.cards[card.id] = card
    }

    override fun onCardDied(
        cardId: Int,
        causeOfDeath: String,
        diedAtTurn: Int
    ): CardDeathResult {
        val dying = _collection.cards[cardId]
            ?: return CardDeathResult(
                isSuccess = false,
                failureReason = "보유하지 않은 카드입니다. (ID: $cardId)",
                message = "[상실 판정 실패] 컬렉션에 보유 중이지 않은 카드입니다. (ID: $cardId)"
            )

        val nextReincarnationCount = dying.reincarnationCount + 1
        val seed = ReincarnatedCard(
            templateId = dying.id,
            name = dying.name,
            element = dying.element,
            baseCost = dying.baseCost,
            baseAtk = dying.baseAtk,
            baseDef = dying.baseDef,
            skillText = dying.skillText,
            reincarnationCount = nextReincarnationCount,
            lostUpgradeLevel = dying.upgradeLevel,
            lostMemoryAffinity = dying.memoryAffinity,
            lostBondLevel = dying.bondLevel,
            lostStarLevel = dying.starLevel,
            lostRelicCount = dying.equippedRelicCount,
            causeOfDeath = causeOfDeath.ifBlank { "전장에서 HP가 0이 되었다" },
            diedAtTurn = diedAtTurn.coerceAtLeast(0)
        )

        // 영구 상실: 보유 목록에서 제거하고 환생 풀에 등록
        _collection.cards.remove(cardId)
        _collection.reincarnationPool[cardId] = seed

        val message = buildString {
            append("[영구 상실] '${dying.name}'이(가) 쓰러졌습니다. 덱과 보유 컬렉션에서 즉시 제거됩니다.\n")
            append("잃은 것: 성급 ${dying.starLevel}★ · 강화 Lv.${dying.upgradeLevel} · 장비 ${dying.equippedRelicCount}개 · ")
            append("교감 ${dying.bondLevel} · 기억 ${dying.memoryAffinity} — 모두 0으로 초기화.\n")
            append("영혼은 세계 어딘가로 환생했습니다. (${nextReincarnationCount}회차) 재획득해야 다시 계약할 수 있습니다.")
        }

        return CardDeathResult(
            isSuccess = true,
            lostCard = dying,
            reincarnation = seed,
            lostUpgradeLevel = dying.upgradeLevel,
            lostMemoryAffinity = dying.memoryAffinity,
            message = message
        )
    }

    override fun acquireCard(templateId: Int): AcquisitionResult {
        if (_collection.isOwned(templateId)) {
            val owned = _collection.cards[templateId]
            return AcquisitionResult(
                status = AcquisitionStatus.REJECTED_DUPLICATE,
                card = owned,
                message = "[획득 차단] '${owned?.name ?: "ID:$templateId"}'은(는) 이미 보유 중인 유일 카드입니다. " +
                        "1캐릭터 = 1장 원칙에 따라 중복 획득할 수 없습니다."
            )
        }

        val poolEntry = _collection.reincarnationPool.remove(templateId)
        if (poolEntry != null) {
            val reborn = buildReacquiredCard(poolEntry)
            _collection.cards[reborn.id] = reborn
            return AcquisitionResult(
                status = AcquisitionStatus.REACQUIRED_FROM_REINCARNATION,
                card = reborn,
                message = "[환생 카드 재획득] '${reborn.name}'이(가) ${poolEntry.reincarnationCount}회차 윤회를 안고 돌아왔습니다.\n" +
                        "강화 Lv.0 · 성급 1★ · 기억 0 · 교감 0 — 모든 기억이 초기화된 첫 만남입니다."
            )
        }

        val template = templates[templateId]
            ?: return AcquisitionResult(
                status = AcquisitionStatus.REJECTED_UNKNOWN_TEMPLATE,
                message = "[획득 실패] 등록되지 않은 카드 템플릿입니다. (ID: $templateId)"
            )

        val acquired = template.copy(
            starLevel = 1,
            currentExp = 0,
            bondLevel = 0,
            currentVisual = VisualState.BATTLE,
            upgradeLevel = 0,
            memoryAffinity = 0,
            reincarnationCount = 0,
            equippedRelicCount = 0
        )
        _collection.cards[acquired.id] = acquired

        return AcquisitionResult(
            status = AcquisitionStatus.ACQUIRED_NEW,
            card = acquired,
            message = "[신규 획득] '${acquired.name}' [${acquired.element.displayName}] 카드를 컬렉션에 등록했습니다. (1캐릭터 1장)"
        )
    }

    override fun acquireCard(template: TcgCard): AcquisitionResult {
        registerTemplate(template)
        return acquireCard(template.id)
    }

    /**
     * 환생 풀의 원본 스탯 시드로 초기 상태 카드 복원 (기억·강화 0, 환생 횟수만 계승)
     */
    private fun buildReacquiredCard(seed: ReincarnatedCard): TcgCard = TcgCard(
        id = seed.templateId,
        name = seed.name,
        element = seed.element,
        baseCost = seed.baseCost,
        atk = seed.baseAtk,
        def = seed.baseDef,
        starLevel = 1,
        currentExp = 0,
        bondLevel = 0,
        currentVisual = VisualState.BATTLE,
        skillText = seed.skillText,
        upgradeLevel = 0,
        memoryAffinity = 0,
        reincarnationCount = seed.reincarnationCount,
        equippedRelicCount = 0
    )
}

// =============================================================================
// v0.3 전투 중 전략적 후퇴 & 파티 긴급 철수 엔진
// =============================================================================

/**
 * 전투 중 전략적 후퇴 엔진 인터페이스
 * - 개별 전술 후퇴: 턴당 1회, 마나 소모, 빈사(HP 15% 이하) 생존 보너스
 * - 파티 긴급 철수: 전리품을 포기하고 생존 카드 전원을 마을로 귀환
 */
interface ITacticalRetreatEngine {
    /** 일반 전투 후퇴 마나 소모 */
    val manaCost: Int get() = 1

    /** 마왕전 후퇴 마나 소모 */
    val demonLordBattleManaCost: Int get() = 2

    /** 빈사 판정 기준 HP 비율 */
    val nearDeathHpRatio: Double get() = 0.15

    /** 빈사 생존 시 획득하는 기억 스택 */
    val nearDeathMemoryBonus: Int get() = 10

    /** 새 턴 시작: 후퇴 사용 플래그 초기화 */
    fun beginTurn(battleState: BattleState, turn: Int, mana: ManaState? = null): BattleState

    /**
     * 개별 전술 후퇴
     * @param temptationStacks 마왕의 홀림 스택 (3 이상이면 후퇴 명령 불복)
     * @param isDemonLordBattle 마왕전 여부 (마나 소모 2)
     */
    fun retreatCard(
        battleState: BattleState,
        slotIndex: Int,
        temptationStacks: Int = 0,
        isDemonLordBattle: Boolean = false
    ): RetreatResult

    /** 파티 긴급 철수 (전리품 포기 · 생존 카드 전원 귀환) */
    fun emergencyPartyRetreat(battleState: BattleState): EmergencyRetreatResult
}

/**
 * 전투 중 전략적 후퇴 엔진 기본 구현체
 */
class TacticalRetreatEngine : ITacticalRetreatEngine {

    override fun beginTurn(battleState: BattleState, turn: Int, mana: ManaState?): BattleState {
        battleState.turn = turn
        if (mana != null) battleState.mana = mana
        battleState.retreatInfo = battleState.retreatInfo.copy(
            turn = turn,
            retreatUsedThisTurn = false
        )
        return battleState
    }

    override fun retreatCard(
        battleState: BattleState,
        slotIndex: Int,
        temptationStacks: Int,
        isDemonLordBattle: Boolean
    ): RetreatResult {
        val requiredMana = if (isDemonLordBattle) demonLordBattleManaCost else manaCost

        if (battleState.retreatInfo.retreatUsedThisTurn) {
            return failure(
                battleState = battleState,
                reason = "[후퇴 실패] 턴당 1회 제한: 이번 턴(턴 ${battleState.turn})에는 이미 후퇴를 사용했습니다."
            )
        }

        val slot = battleState.playerSlots.getOrNull(slotIndex)
            ?: return failure(
                battleState = battleState,
                reason = "[후퇴 실패] 존재하지 않는 슬롯입니다. (슬롯 $slotIndex)"
            )

        val card = slot.placedCard
            ?: return failure(
                battleState = battleState,
                reason = "[후퇴 실패] 슬롯 $slotIndex 은(는) 비어 있어 후퇴시킬 카드가 없습니다."
            )

        val hp = battleState.hpOf(slotIndex)
        if (hp != null && !hp.isAlive) {
            return failure(
                battleState = battleState,
                reason = "[후퇴 불가] '${card.name}'은(는) 이미 HP 0으로 사망했습니다. 후퇴가 아니라 영구 상실 처리 대상입니다.",
                card = card,
                slotIndex = slotIndex
            )
        }

        if (temptationStacks >= CHARM_RETREAT_BLOCK_STACKS) {
            return failure(
                battleState = battleState,
                reason = "[마왕의 홀림 ${temptationStacks}스택] 카드가 후퇴 명령에 응하지 않습니다! " +
                        "성수로 홀림을 ${CHARM_RETREAT_BLOCK_STACKS}스택 미만으로 정화한 뒤 다시 시도하세요.",
                card = card,
                slotIndex = slotIndex
            )
        }

        if (battleState.mana.currentMana < requiredMana) {
            return failure(
                battleState = battleState,
                reason = "[후퇴 실패] 마나가 부족합니다. (필요: $requiredMana, 보유: ${battleState.mana.currentMana})",
                card = card,
                slotIndex = slotIndex
            )
        }

        battleState.mana = battleState.mana.copy(
            currentMana = battleState.mana.currentMana - requiredMana
        )

        // 손패가 가득 찬 경우 대기열로 철수
        val destination = if (battleState.hand.size >= battleState.maxHandSize) {
            RetreatZone.RESERVE
        } else {
            RetreatZone.HAND
        }
        if (destination == RetreatZone.HAND) {
            battleState.hand.add(card)
        } else {
            battleState.reserve.add(card)
        }

        slot.placedCard = null
        battleState.playerHp.removeAll { it.slotIndex == slotIndex }

        val isNearDeath = hp != null && hp.isNearDeath
        val hpPercent = if (hp != null) (hp.hpRatio * 100).toInt() else 0
        if (isNearDeath) {
            card.memoryAffinity = (card.memoryAffinity + nearDeathMemoryBonus).coerceAtMost(100)
        }

        battleState.retreatInfo = battleState.retreatInfo.copy(
            retreatUsedThisTurn = true,
            totalRetreatCount = battleState.retreatInfo.totalRetreatCount + 1,
            nearDeathSurvivalCount = battleState.retreatInfo.nearDeathSurvivalCount + (if (isNearDeath) 1 else 0)
        )

        val message = buildString {
            append("[전술 후퇴] '${card.name}'을(를) ${slotIndex}번 슬롯에서 ${destination.displayName}(으)로 안전하게 철수시켰습니다. ")
            append("(마나 -$requiredMana → ${battleState.mana.currentMana}/${battleState.mana.maxMana})\n")
            if (isNearDeath) {
                append("생사 고비(HP ${hpPercent}%)에서 살아 돌아왔습니다! 생존 교감 보너스 +$nearDeathMemoryBonus 기억 (현재 ${card.memoryAffinity})\n")
            }
            append("예비 카드를 교대 투입하고, 이번 턴 후퇴는 종료되었습니다.")
        }

        return RetreatResult(
            isSuccess = true,
            retreatedCard = card,
            fromSlotIndex = slotIndex,
            toZone = destination,
            manaSpent = requiredMana,
            remainingMana = battleState.mana.currentMana,
            nearDeathSurvivalBonus = isNearDeath,
            memoryAffinityGained = if (isNearDeath) nearDeathMemoryBonus else 0,
            message = message
        )
    }

    override fun emergencyPartyRetreat(battleState: BattleState): EmergencyRetreatResult {
        if (battleState.retreatInfo.emergencyRetreatUsed) {
            return EmergencyRetreatResult(
                isSuccess = false,
                failureReason = "이번 전투에서는 이미 긴급 철수를 사용했습니다.",
                message = "[긴급 철수 불가] 이번 전투에서는 이미 긴급 철수를 사용했습니다."
            )
        }

        val survivors = battleState.survivingCards().distinctBy { it.id }
        if (survivors.isEmpty()) {
            return EmergencyRetreatResult(
                isSuccess = false,
                failureReason = "철수할 생존 카드가 없습니다. (파티 전멸)",
                message = "[긴급 철수 불가] 살아남은 카드가 없어 철수할 수 없습니다."
            )
        }

        val abandonedGold = battleState.lootGold
        val abandonedGems = battleState.lootGems
        val abandonedItems = battleState.lootItems.toList()

        battleState.playerSlots.forEach { it.placedCard = null }
        battleState.playerHp.clear()
        battleState.hand.clear()
        battleState.hand.addAll(survivors)
        battleState.reserve.clear()
        battleState.lootGold = 0
        battleState.lootGems = 0
        battleState.lootItems.clear()
        battleState.retreatInfo = battleState.retreatInfo.copy(
            retreatUsedThisTurn = true,
            emergencyRetreatUsed = true
        )

        val message = buildString {
            append("[파티 긴급 철수] 전투를 포기하고 마을로 철수했습니다.\n")
            append("생존 귀환 ${survivors.size}장: ${survivors.joinToString(", ") { it.name }}\n")
            append("포기한 전리품: 금화 ${abandonedGold}G · 보석 ${abandonedGems} · 아이템 ${abandonedItems.size}개\n")
            append("카드 상실은 없습니다. 잃은 것은 전리품뿐입니다.")
        }

        return EmergencyRetreatResult(
            isSuccess = true,
            rescuedCards = survivors,
            lostLootGold = abandonedGold,
            lostLootGems = abandonedGems,
            abandonedLootNames = abandonedItems,
            message = message
        )
    }

    private fun failure(
        battleState: BattleState,
        reason: String,
        card: TcgCard? = null,
        slotIndex: Int = -1
    ): RetreatResult = RetreatResult(
        isSuccess = false,
        retreatedCard = card,
        fromSlotIndex = slotIndex,
        remainingMana = battleState.mana.currentMana,
        failureReason = reason,
        message = reason
    )

    companion object {
        /** 마왕의 홀림 스택이 이 값 이상이면 후퇴 명령에 불복한다. */
        const val CHARM_RETREAT_BLOCK_STACKS: Int = 3
    }
}
