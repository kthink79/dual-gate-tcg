package com.game.tcg.ui

import com.game.tcg.audio.AudioScene
import com.game.tcg.audio.AudioSettings
import com.game.tcg.core.engine.IntroStoryState
import com.game.tcg.core.engine.TemptationEngine
import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.DeckRules
import com.game.tcg.core.model.DemonLordRegistry
import com.game.tcg.core.model.EquipmentCatalog
import com.game.tcg.core.model.ReincarnatedCard
import com.game.tcg.core.model.ShardWallet
import com.game.tcg.core.model.StageCatalog
import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.VisualState
import com.game.tcg.persistence.GameSave
import com.game.tcg.persistence.SavedCard
import com.game.tcg.persistence.SavedEquipment
import com.game.tcg.persistence.SavedPartyMember
import com.game.tcg.persistence.SavedReincarnation
import com.game.tcg.persistence.SavedTemptation

// ============================================================================
// 저장 모델 ↔ 화면 상태 변환
//
// TcgGameState(화면 상태) → GameSave(저장 모델) → 텍스트 → 앱 재실행 → TcgGameState 복원.
// 카드 이름·속성·스킬 같은 고정 정의는 도감(CardCatalog)에서 다시 만들고,
// 저장 파일에는 플레이어가 바꾼 수치(레벨·스탯·호감도·편성·재화)만 담는다.
// ============================================================================

/** 화면 상태에서 저장 모델을 만든다 (매 상태 변경 시 자동 저장에 쓰인다) */
fun TcgGameState.toGameSave(): GameSave = GameSave(
    gold = gold,
    gems = gems,
    holyWater = holyWater,
    junkCards = junkCards,
    packTickets = packTickets,
    shards = shards.byCard,
    playerExp = playerExp,
    storyStepIndex = storyStepIndex,
    storyReadLog = storyReadLog,
    eventAChoice = eventAChoice,
    eventAResult = eventAResult,
    eventBChoice = eventBChoice,
    eventBResult = eventBResult,
    lilyFamiliarity = lilyFamiliarity,
    defenseBonusPercent = defenseBonusPercent,
    intelBonusPercent = intelBonusPercent,
    chapter2PreludeOpened = chapter2PreludeOpened,
    temptation = SavedTemptation(
        gazeDurationSeconds = temptationState.gazeDurationSeconds,
        gazeZoomCount = temptationState.gazeZoomCount,
        gazeTemptationStacks = temptationState.gazeTemptationStacks,
        undercoverEncounterCount = temptationState.undercoverEncounterCount,
        undercoverFlirtCount = temptationState.undercoverFlirtCount,
        curseOfArchdemonStacks = temptationState.curseOfArchdemonStacks,
        stolenGold = temptationState.stolenGold
    ),
    partnerBond = partnerBond,
    partnerVisual = partnerVisual.name,
    dateLookUnlocked = dateLookUnlocked,
    ultimateUnlocked = ultimateUnlocked,
    frontRowDefensePenaltyPercent = frontRowDefensePenaltyPercent,
    trustRiftChoice = trustRiftChoice,
    battleVictories = battleVictories,
    pairUltimateCount = pairUltimateCount,
    summonCount = summonCount,
    summonBuffPercent = summonBuffPercent,
    summonBuffBattles = summonBuffBattles,
    explorationCount = explorationCount,
    explorationLog = explorationLog,
    gateEnergy = gateEnergy,
    cards = ownedCards.map { it.toSavedCard() },
    party = party.map {
        SavedPartyMember(
            cardId = it.card.id,
            slotIndex = it.slotIndex,
            hp = it.hp,
            maxHp = it.maxHp,
            memoryAffinity = it.memoryAffinity
        )
    },
    reserveIds = battleReserve.map { it.id },
    reincarnations = reincarnationPool.map {
        SavedReincarnation(
            templateId = it.templateId,
            reincarnationCount = it.reincarnationCount,
            lostUpgradeLevel = it.lostUpgradeLevel,
            lostMemoryAffinity = it.lostMemoryAffinity,
            lostBondLevel = it.lostBondLevel,
            lostStarLevel = it.lostStarLevel,
            lostRelicCount = it.lostRelicCount,
            causeOfDeath = it.causeOfDeath,
            diedAtTurn = it.diedAtTurn
        )
    },
    equipment = equipment.map {
        SavedEquipment(uid = it.uid, itemId = it.id, equippedTo = it.equippedTo)
    },
    stageCleared = stageCleared.sorted(),
    selectedStage = selectedStage,
    defeatedDemonLords = DemonLordRegistry.ALL.filter { it.isDefeated }.map { it.id },
    vowedDemonLords = DemonLordRegistry.ALL.filter { it.isVowedCompanion }.map { it.id },
    lobbyHeroId = lobbyHeroId,
    lobbyVisual = lobbyVisual.name,
    growthCardId = growthCardId,
    summonCardId = summonCardId,
    selectedCardId = selectedCardId,
    selectedVisual = selectedVisual.name,
    introSeen = introStory.hasSeenIntro,
    introReplayCount = introStory.replayCount,
    bgmEnabled = audioSettings.bgmEnabled,
    sfxEnabled = audioSettings.sfxEnabled,
    bgmVolume = audioSettings.bgmVolume,
    sfxVolume = audioSettings.sfxVolume
)

private fun TcgCard.toSavedCard(): SavedCard = SavedCard(
    cardId = id,
    atk = atk,
    def = def,
    starLevel = starLevel,
    currentExp = currentExp,
    bondLevel = bondLevel,
    upgradeLevel = upgradeLevel,
    memoryAffinity = memoryAffinity,
    reincarnationCount = reincarnationCount,
    equippedRelicCount = equippedRelicCount,
    isMemoryReset = isMemoryReset,
    memoryFragments = memoryFragments,
    visual = currentVisual.name
)

/**
 * 저장 데이터를 화면 상태로 복원한다.
 *
 * - 도감에 없는 카드 번호·스테이지 번호·장비 id 는 조용히 건너뛴다(구버전·손상 대비).
 * - 보유 카드가 하나도 복원되지 않으면 `base`(신규 시작 상태)를 그대로 돌려준다.
 */
fun GameSave.restoreInto(base: TcgGameState): TcgGameState {
    val restoredCards: List<TcgCard> = cards.mapNotNull { row ->
        val record = CardCatalog.find(row.cardId) ?: return@mapNotNull null
        record.toCard().copy(
            atk = row.atk.coerceAtLeast(1),
            def = row.def.coerceAtLeast(1),
            starLevel = row.starLevel.coerceIn(1, MAX_STAR_LEVEL),
            currentExp = row.currentExp.coerceAtLeast(0),
            bondLevel = row.bondLevel.coerceIn(0, 100),
            upgradeLevel = row.upgradeLevel.coerceAtLeast(0),
            memoryAffinity = row.memoryAffinity.coerceIn(0, 100),
            reincarnationCount = row.reincarnationCount.coerceAtLeast(0),
            equippedRelicCount = row.equippedRelicCount.coerceIn(0, MAX_EQUIPMENT_SLOTS),
            isMemoryReset = row.isMemoryReset,
            memoryFragments = row.memoryFragments.coerceIn(0, TcgCard.MEMORY_FRAGMENT_TARGET),
            currentVisual = visualOf(row.visual)
        )
    }.distinctBy { it.id }

    // 저장 파일에 유효한 보유 카드가 없으면 게임 시작 상태로 폴백한다.
    if (restoredCards.isEmpty()) return base

    val cardById = restoredCards.associateBy { it.id }

    val restoredParty: List<PartyMember> = party
        .filter { it.slotIndex in 0 until DeckRules.MAX_DECK_SIZE }
        .mapNotNull { row ->
            val card = cardById[row.cardId] ?: return@mapNotNull null
            val maxHp = row.maxHp.coerceAtLeast(1)
            PartyMember(
                card = card,
                slotIndex = row.slotIndex,
                isFrontRow = DeckRules.isFrontSlot(row.slotIndex),
                hp = row.hp.coerceIn(0, maxHp),
                maxHp = maxHp,
                memoryAffinity = row.memoryAffinity.coerceIn(0, 100)
            )
        }
        .distinctBy { it.slotIndex }
        .sortedBy { it.slotIndex }

    val restoredReincarnation: List<ReincarnatedCard> = reincarnations.mapNotNull { row ->
        val record = CardCatalog.find(row.templateId) ?: return@mapNotNull null
        ReincarnatedCard(
            templateId = record.id,
            name = record.fullName,
            element = record.element,
            baseCost = record.baseCost,
            baseAtk = record.atk,
            baseDef = record.def,
            skillText = record.skill,
            reincarnationCount = row.reincarnationCount.coerceAtLeast(1),
            lostUpgradeLevel = row.lostUpgradeLevel.coerceAtLeast(0),
            lostMemoryAffinity = row.lostMemoryAffinity.coerceAtLeast(0),
            lostBondLevel = row.lostBondLevel.coerceAtLeast(0),
            lostStarLevel = row.lostStarLevel.coerceAtLeast(1),
            lostRelicCount = row.lostRelicCount.coerceAtLeast(0),
            causeOfDeath = row.causeOfDeath.ifBlank { "전장에서 HP가 0이 되었다" },
            diedAtTurn = row.diedAtTurn.coerceAtLeast(0)
        )
    }.distinctBy { it.templateId }

    val restoredEquipment = equipment.mapNotNull { row ->
        val item = EquipmentCatalog.instantiate(row.itemId) ?: return@mapNotNull null
        item.copy(
            uid = row.uid.ifBlank { item.uid },
            equippedTo = row.equippedTo?.takeIf { it in cardById }
        )
    }.distinctBy { it.uid }

    val cleared = stageCleared.filter { StageCatalog.find(it) != null }.toSet()
    val stage = StageCatalog.find(selectedStage)?.number
        ?: cleared.maxOrNull()?.plus(1)?.coerceAtMost(StageCatalog.TOTAL_STAGES)
        ?: 1

    val restoredTemptation = temptation.toTemptationState()
    val penalty = TemptationEngine().calculateCombatPenalty(restoredTemptation)

    // 마왕 토벌 상태는 전역 레지스트리에 반영한다(도감·연성·서약 파트너 판정이 이 값을 본다).
    DemonLordRegistry.ALL.forEach { lord ->
        lord.isDefeated = defeatedDemonLords.contains(lord.id)
        lord.isVowedCompanion = vowedDemonLords.contains(lord.id)
    }

    val audio = AudioSettings(
        bgmEnabled = bgmEnabled,
        sfxEnabled = sfxEnabled,
        bgmVolume = bgmVolume,
        sfxVolume = sfxVolume
    ).sanitized()

    return base.copy(
        gold = gold.coerceAtLeast(0),
        gems = gems.coerceAtLeast(0),
        holyWater = holyWater.coerceAtLeast(0),
        junkCards = junkCards.coerceAtLeast(0),
        packTickets = packTickets.coerceAtLeast(0),
        shards = ShardWallet(byCard = shards.filter { (cardId, amount) ->
            cardId > 0 && amount > 0
        }),
        playerExp = playerExp.coerceAtLeast(0),
        storyStepIndex = storyStepIndex.coerceIn(0, (StoryScript.STEPS.size - 1).coerceAtLeast(0)),
        storyReadLog = storyReadLog.ifEmpty { base.storyReadLog },
        eventAChoice = eventAChoice,
        eventAResult = eventAResult,
        eventBChoice = eventBChoice,
        eventBResult = eventBResult,
        lilyFamiliarity = lilyFamiliarity.coerceAtLeast(0),
        defenseBonusPercent = defenseBonusPercent.coerceAtLeast(0),
        intelBonusPercent = intelBonusPercent.coerceAtLeast(0),
        chapter2PreludeOpened = chapter2PreludeOpened,
        temptationState = restoredTemptation,
        combatPenalty = penalty,
        partnerBond = partnerBond.coerceIn(0, 100),
        partnerVisual = visualOf(partnerVisual),
        dateLookUnlocked = dateLookUnlocked,
        ultimateUnlocked = ultimateUnlocked,
        frontRowDefensePenaltyPercent = frontRowDefensePenaltyPercent.coerceAtLeast(0),
        trustRiftChoice = trustRiftChoice,
        battleVictories = battleVictories.coerceAtLeast(0),
        pairUltimateCount = pairUltimateCount.coerceAtLeast(0),
        summonCount = summonCount.coerceAtLeast(0),
        summonBuffPercent = summonBuffPercent.coerceAtLeast(0),
        summonBuffBattles = summonBuffBattles.coerceAtLeast(0),
        explorationCount = explorationCount.coerceAtLeast(0),
        explorationLog = explorationLog.ifEmpty { base.explorationLog },
        gateEnergy = gateEnergy.coerceIn(0, base.gateEnergyMax),
        ownedCards = restoredCards,
        party = if (restoredParty.isNotEmpty()) restoredParty else base.party,
        battleReserve = reserveIds.mapNotNull { cardById[it] },
        reincarnationPool = restoredReincarnation,
        equipment = restoredEquipment,
        stageCleared = cleared,
        selectedStage = stage,
        activeStage = null,
        lobbyHeroId = if (cardById.containsKey(lobbyHeroId)) lobbyHeroId
        else restoredCards.first().id,
        lobbyVisual = visualOf(lobbyVisual),
        growthCardId = if (cardById.containsKey(growthCardId)) growthCardId
        else restoredCards.first().id,
        summonCardId = if (cardById.containsKey(summonCardId)) summonCardId
        else restoredCards.first().id,
        selectedCardId = if (cardById.containsKey(selectedCardId)) selectedCardId
        else restoredCards.first().id,
        selectedVisual = visualOf(selectedVisual),
        introStory = IntroStoryState(
            sceneIndex = 0,
            isPlaying = !introSeen,
            hasSeenIntro = introSeen,
            skipped = false,
            replayCount = introReplayCount.coerceAtLeast(0)
        ),
        audioSettings = audio,
        audioScene = AudioScene.LOBBY,
        currentScreen = LobbyDestination.LOBBY,
        statusMessage = "[불러오기] 이전 진행 상황을 복원했습니다. (보유 ${restoredCards.size}명 · ${cleared.size}/${StageCatalog.TOTAL_STAGES} 스테이지 · ${gold}G)"
    )
}

private fun SavedTemptation.toTemptationState() = com.game.tcg.core.model.TemptationState(
    gazeDurationSeconds = gazeDurationSeconds.coerceAtLeast(0L),
    gazeZoomCount = gazeZoomCount.coerceAtLeast(0),
    gazeTemptationStacks = gazeTemptationStacks.coerceIn(0, 5),
    undercoverEncounterCount = undercoverEncounterCount.coerceAtLeast(0),
    undercoverFlirtCount = undercoverFlirtCount.coerceAtLeast(0),
    curseOfArchdemonStacks = curseOfArchdemonStacks.coerceIn(0, 5),
    stolenGold = stolenGold.coerceAtLeast(0)
)

private fun visualOf(name: String): VisualState =
    runCatching { VisualState.valueOf(name) }.getOrDefault(VisualState.BATTLE)

/** 카드 성급 상한 (1성 ~ 5성 한계 돌파) */
private const val MAX_STAR_LEVEL = 5

/** 카드 1명당 장비 슬롯 수 (무기 · 방어구 · 장신구) */
private const val MAX_EQUIPMENT_SLOTS = 3
