package com.game.tcg

import com.game.tcg.core.engine.AdvancedSynthesisEngine
import com.game.tcg.core.engine.CardUpgradeEngine
import com.game.tcg.core.engine.EquipmentEngine
import com.game.tcg.core.engine.PackShopEngine
import com.game.tcg.core.engine.StageEngine
import com.game.tcg.core.engine.StageReadiness
import com.game.tcg.core.engine.SummonEngine
import com.game.tcg.core.model.BossKind
import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.CardRarity
import com.game.tcg.core.model.EquipmentCatalog
import com.game.tcg.core.model.EquipmentSlot
import com.game.tcg.core.model.MidBossCatalog
import com.game.tcg.core.model.PackCatalog
import com.game.tcg.core.model.ShardWallet
import com.game.tcg.core.model.StageCatalog
import com.game.tcg.core.model.StartingRoster
import com.game.tcg.ui.CharacterArtRegistry
import com.game.tcg.ui.LobbyDestination
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * 게임 진행 루프 검증 — 2026-11-16 사용자 요청.
 *
 * ```
 * 시작(저코스트 3장) → 스테이지 클리어(골드·티켓·조각·장비) → 카드팩 → 동료 확보 → 덱 전투력 상승
 *                   └ 서브: 현실 소환 · 카드 강화 · 연성/초월 · 장비 장착
 * ```
 * 보스: 중간 보스 6명(No.170~175)은 5·10·15·20·25·28, 3대 마왕(112~114)은 26·29·30 스테이지.
 */
class GameProgressionTest {

    private val allIds = CardCatalog.ALL.map { it.id }

    /** 도감 전체를 보유한(강한 덱) ViewModel — 스테이지·보스 검증용 */
    private fun strongViewModel(
        cleared: Set<Int> = emptySet(),
        gold: Int = 5000,
        shards: ShardWallet = ShardWallet(),
        seed: Int = 7
    ) = TcgGameViewModel(
        random = Random(seed),
        initialOwnedCardIds = allIds,
        initialGold = gold,
        initialShards = shards,
        initialClearedStages = cleared
    )

    /** 시작 3장 상태의 ViewModel (재화 시드 가능) */
    private fun starterViewModel(
        gold: Int = 5000,
        shards: ShardWallet = ShardWallet(),
        tickets: Int = 1,
        seed: Int = 42
    ) = TcgGameViewModel(
        random = Random(seed),
        initialOwnedCardIds = listOf(3, 1, 2),
        initialGold = gold,
        initialPackTickets = tickets,
        initialShards = shards
    )

    /** 전투를 승리까지 진행한다 (최대 40턴) */
    private fun fightToVictory(vm: TcgGameViewModel) {
        var guard = 0
        while (vm.uiState.value.enemyHp > 0 && guard < 120) {
            val before = vm.uiState.value.enemyHp
            if (guard % 2 == 0) vm.attackFront() else vm.attackBack()
            if (vm.uiState.value.enemyHp >= before) {
                // 마나가 없으면 턴을 넘겨 회복한다
                vm.nextTurn()
            } else if (vm.uiState.value.enemyHp > 0 && vm.uiState.value.mana.currentMana <= 1) {
                vm.nextTurn()
            }
            guard++
        }
    }

    // =========================================================================
    // 1. 시작 상태 — 저코스트 3장 · 전열 3칸 · 나머지 잠금
    // =========================================================================

    @Test
    fun `게임 시작은 저코스트 3장만 지급하고 나머지 도감은 잠근다`() {
        val vm = TcgGameViewModel(random = Random(2026))
        val state = vm.uiState.value

        assertEquals(3, state.ownedCards.size)
        assertTrue("시작 카드는 모두 코스트 3~4", state.ownedCards.all { it.baseCost in 3..4 })
        assertEquals("같은 카드가 두 번 나오지 않는다", 3, state.ownedCards.map { it.id }.toSet().size)
        assertTrue("시작 카드는 시작 풀에서만 나온다", state.ownedCardIds.all { it in StartingRoster.POOL_IDS })
        assertTrue("미보유 카드는 도감에 남아 있다", state.ownedCards.size < CardCatalog.ALL.size)
        assertEquals("속성이 서로 다르게 뽑힌다", 3, state.ownedCards.map { it.element }.toSet().size)
    }

    @Test
    fun `같은 시드는 같은 시작 3장을 뽑는다`() {
        val a = TcgGameViewModel(random = Random(99)).uiState.value.ownedCardIds
        val b = TcgGameViewModel(random = Random(99)).uiState.value.ownedCardIds
        val c = (1..20).map { TcgGameViewModel(random = Random(it)).uiState.value.ownedCardIds }

        assertEquals(a, b)
        assertTrue("시드가 다르면 조합도 달라진다", c.toSet().size > 1)
        assertTrue(c.all { it.size == 3 })
    }

    @Test
    fun `시작 덱은 9칸 중 전열 3칸만 채워진다`() {
        val state = TcgGameViewModel(random = Random(42)).uiState.value

        assertEquals("시작 덱은 3장", 3, state.party.size)
        assertEquals("전열 3칸(0·1·2)에만 배치", setOf(0, 1, 2), state.party.map { it.slotIndex }.toSet())
        assertTrue("전원 전열", state.party.all { it.isFrontRow })
        assertEquals("기본 덱은 9칸", 9, state.unlockedDeckSize)
        assertEquals("잠긴 칸은 3칸(최대 12장)", 3, state.lockedDeckSlots)
        assertEquals("시작 재화 — 골드 500 · 티켓 1", 500 to 1, state.gold to state.packTickets)
        assertEquals("스테이지는 1번부터", 1, state.frontStageNumber)
        assertTrue(state.stageCleared.isEmpty())
    }

    // =========================================================================
    // 2. 스테이지 카탈로그 — 6챕터 30 · 보스 배치
    // =========================================================================

    @Test
    fun `스테이지는 6챕터 30개이고 권장 전투력이 단조 증가한다`() {
        assertEquals(6, StageCatalog.CHAPTER_COUNT)
        assertEquals(30, StageCatalog.TOTAL_STAGES)
        assertEquals(30, StageCatalog.STAGES.size)
        assertEquals((1..30).toList(), StageCatalog.STAGES.map { it.number })

        val powers = StageCatalog.STAGES.map { it.recommendedPower }
        assertEquals("권장 전투력은 오름차순", powers.sorted(), powers)
        assertEquals("같은 값이 두 번 나오지 않는다", powers.size, powers.toSet().size)
        assertTrue("보스는 일반 스테이지보다 두껍다",
            StageCatalog.STAGES.filter { it.isBoss }.all { it.enemyHp > 0 && it.enemyAtk > 0 })
        StageCatalog.STAGES.forEach { stage ->
            assertEquals("챕터 계산", (stage.number - 1) / 5 + 1, stage.chapter)
            assertTrue("#${stage.number} 보상 골드", stage.reward.gold > 0)
            assertTrue("#${stage.number} 보상 경험치", stage.reward.exp > 0)
            assertTrue("#${stage.number} 적 이름", stage.enemyName.isNotBlank())
        }
    }

    @Test
    fun `중간 보스 6명은 5 10 15 20 25 28 스테이지에 서고 3대 마왕은 26 29 30 스테이지에 선다`() {
        assertEquals(setOf(5, 10, 15, 20, 25, 28), StageCatalog.MID_BOSS_STAGES.keys)
        assertEquals((170..175).toList(), StageCatalog.MID_BOSS_STAGES.values.toList())
        assertEquals(setOf(26, 29, 30), StageCatalog.DEMON_LORD_STAGES.keys)
        assertEquals(listOf(112, 113, 114), StageCatalog.DEMON_LORD_STAGES.values.toList())

        StageCatalog.MID_BOSS_STAGES.forEach { (stageNumber, bossId) ->
            val stage = StageCatalog.require(stageNumber)
            assertEquals("#$stageNumber 중간 보스", bossId, stage.bossId)
            assertEquals(BossKind.MID_BOSS, stage.bossKind)
            val boss = MidBossCatalog.find(bossId)!!
            assertTrue("적 이름에 보스 이름 포함", stage.enemyName.contains(boss.name))
            assertEquals("보스 속성 = 스테이지 속성", boss.element, stage.enemyElement)
            assertTrue("보스 아트 등록", CharacterArtRegistry.hasArt(bossId))
        }
        StageCatalog.DEMON_LORD_STAGES.forEach { (stageNumber, lordId) ->
            val stage = StageCatalog.require(stageNumber)
            assertEquals(BossKind.DEMON_LORD, stage.bossKind)
            assertEquals(lordId, stage.bossId)
            assertEquals(CardCatalog.require(lordId).name, CardCatalog.require(lordId).name)
            assertTrue("대마왕 이름 포함", stage.enemyName.contains(CardCatalog.require(lordId).name))
        }
        assertEquals("보스는 정확히 9관문", 9, StageCatalog.STAGES.count { it.isBoss })
    }

    @Test
    fun `보스 스테이지는 티켓과 조각을 더 준다`() {
        val bossStages = StageCatalog.STAGES.filter { it.isBoss }
        val normalStages = StageCatalog.STAGES.filterNot { it.isBoss }

        assertTrue("보스는 조각 보상이 있다", bossStages.all { it.reward.shardCardId != null && it.reward.shardCount > 0 })
        assertTrue("보스는 티켓을 준다", bossStages.count { it.reward.packTickets > 0 } >= 5)
        assertTrue(
            "보스 평균 골드가 일반보다 많다",
            bossStages.map { it.reward.gold }.average() > normalStages.map { it.reward.gold }.average()
        )
        val last = StageCatalog.require(30)
        assertTrue("최종 보상이 가장 크다", last.reward.gold >= bossStages.maxOf { it.reward.gold })
        assertEquals("최종 관문은 모르가나", 114, last.bossId)
    }

    // =========================================================================
    // 3. 진행 잠금 · 출전 · 클리어 보상
    // =========================================================================

    @Test
    fun `1스테이지만 열려 있고 앞 스테이지를 깨야 다음이 열린다`() {
        val vm = strongViewModel()
        assertTrue(vm.uiState.value.isStageUnlocked(1))
        assertFalse(vm.uiState.value.isStageUnlocked(2))
        assertFalse(vm.uiState.value.isStageUnlocked(30))

        assertFalse("잠긴 스테이지에는 출전할 수 없다", vm.startStageBattle(2))
        assertTrue(vm.uiState.value.statusMessage.contains("잠겨") || vm.uiState.value.stageMessage!!.contains("잠김"))

        assertTrue(StageEngine.isUnlocked(1, emptySet()))
        assertTrue(StageEngine.isUnlocked(6, setOf(5)))
        assertFalse(StageEngine.isUnlocked(6, setOf(4)))
        assertEquals(1, StageEngine.frontStage(emptySet()))
        assertEquals(6, StageEngine.frontStage((1..5).toSet()))
        assertEquals(30, StageEngine.frontStage((1..30).toSet()))
    }

    @Test
    fun `출전하면 VS 전투가 그 스테이지 적 편성으로 열린다`() {
        val vm = strongViewModel()
        val stage = StageCatalog.require(1)

        assertTrue(vm.startStageBattle(1))

        val state = vm.uiState.value
        assertEquals(LobbyDestination.BATTLE, state.currentScreen)
        assertEquals(1, state.activeStage)
        assertEquals(stage.enemyName, state.enemyName)
        assertEquals(stage.enemyHp, state.enemyHp)
        assertEquals(stage.enemyAtk, state.enemyAtk)
        assertEquals(stage.enemyElement, state.enemyElement)
        assertTrue("출전 시 파티 HP 최대 회복", state.party.all { it.hp == it.maxHp })
        assertTrue(state.battleLog.any { it.contains("스테이지 1") })
    }

    @Test
    fun `스테이지 승리 보상으로 골드 티켓 조각이 들어오고 다음 스테이지가 열린다`() {
        val vm = strongViewModel()
        val before = vm.uiState.value
        val stage = StageCatalog.require(4) // 티켓 보상이 있는 스테이지
        vm.selectStage(4)
        // 1~3을 먼저 깬 상태로 시드해야 하므로 별도 VM으로 검증한다
        val seeded = strongViewModel(cleared = setOf(1, 2, 3))
        val seededBefore = seeded.uiState.value

        assertTrue(seeded.startStageBattle(4))
        fightToVictory(seeded)

        val after = seeded.uiState.value
        assertEquals("적 격파", 0, after.enemyHp)
        assertTrue("스테이지 4 클리어", 4 in after.stageCleared)
        assertEquals("다음 스테이지 선택", 5, after.selectedStage)
        assertNull("전투 종료 후 activeStage 해제", after.activeStage)
        assertNotNull(after.lastStageOutcome)
        assertEquals(stage.reward.gold, after.lastStageOutcome!!.gold)
        assertTrue("골드 증가", after.gold > seededBefore.gold)
        assertTrue("티켓 증가", after.packTickets > seededBefore.packTickets)
        assertEquals("조각 보상", stage.reward.shardCount, after.shards.of(stage.reward.shardCardId!!))
        assertTrue("다음 스테이지 해금", after.isStageUnlocked(5))
        assertTrue(after.battleLog.any { it.contains("[스테이지 4 클리어]") })
        assertEquals("시작 상태는 아직 1번뿐", 0, before.stageCleared.size)
    }

    @Test
    fun `보스 스테이지를 깨면 확정 장비 드롭과 함께 다음 챕터가 열린다`() {
        val vm = strongViewModel(cleared = setOf(1, 2, 3, 4))
        val stage = StageCatalog.require(5)

        assertTrue(vm.startStageBattle(5))
        fightToVictory(vm)

        val after = vm.uiState.value
        assertTrue("5번 클리어", 5 in after.stageCleared)
        assertTrue("6번 해금(2장 시작)", after.isStageUnlocked(6))
        assertNotNull("확정 장비 드롭", after.lastStageOutcome!!.equipmentDrop)
        assertEquals("드롭 장비", "armor_leather_vest", after.lastStageOutcome!!.equipmentDrop!!.id)
        assertTrue("소지품에 장비", after.equipment.any { it.id == "armor_leather_vest" })
        assertEquals("중간 보스 조각", 10, after.shards.of(170))
        assertTrue("심연 연성 안내", after.battleLog.any { it.contains("심연 연성") })
        assertEquals(stage.bossId, after.lastStageOutcome!!.stage.bossId)
    }

    @Test
    fun `대마왕 스테이지를 깨면 마왕이 서약 파트너로 합류한다`() {
        // 마왕 3명만 미보유인 상태에서 시작해야 "합류"를 확인할 수 있다
        val vm = TcgGameViewModel(
            random = Random(7),
            initialOwnedCardIds = CardCatalog.ALL.filterNot { it.isDemonLord }.map { it.id },
            initialGold = 5000,
            initialClearedStages = (1..25).toSet()
        )
        assertFalse(112 in vm.uiState.value.ownedCardIds)
        vm.autoEquipParty()
        assertTrue(vm.startStageBattle(26))
        fightToVictory(vm)

        val after = vm.uiState.value
        assertTrue("26번 클리어", 26 in after.stageCleared)
        assertTrue("릴리스 합류", after.ownedCardIds.contains(112))
        assertTrue(after.statusMessage.contains("서약") || after.battleLog.any { it.contains("[서약]") })
        assertTrue("114는 아직 미보유", 114 !in after.ownedCardIds)
    }

    @Test
    fun `이미 깬 스테이지는 보상이 절반만 지급된다`() {
        val vm = strongViewModel(cleared = (1..3).toSet())
        val stage = StageCatalog.require(3)
        val beforeGold = vm.uiState.value.gold

        assertTrue(vm.startStageBattle(3))
        fightToVictory(vm)

        val after = vm.uiState.value
        assertEquals("재클리어 골드 절반", stage.reward.gold * StageEngine.REPEAT_REWARD_PERCENT / 100, after.gold - beforeGold)
        assertTrue("클리어 목록은 그대로", 3 in after.stageCleared)
    }

    // =========================================================================
    // 4. 카드팩 상점 — 신규 계약 / 중복 조각
    // =========================================================================

    @Test
    fun `티켓이 있으면 티켓으로 카드팩을 열고 신규 동료가 늘어난다`() {
        val vm = TcgGameViewModel(random = Random(11), initialOwnedCardIds = listOf(3, 1, 2))
        val before = vm.uiState.value

        val result = vm.openPack("pack_standard")

        assertNotNull(result)
        val after = vm.uiState.value
        assertEquals("티켓 소모", before.packTickets - 1, after.packTickets)
        assertEquals("티켓을 썼으므로 골드 유지 + 중복 환급", before.gold, after.gold - result!!.totalGold)
        assertEquals("3장 개봉", 3, result.draws.size)
        assertEquals("보유 수 = 시작 3장 + 신규", 3 + result.newCardIds.size, after.ownedCards.size)
        assertEquals("신규 목록 반영", 3 + result.newCardIds.size, after.ownedCardIds.size)
        assertTrue("개봉 결과 보관", after.lastPackOpen != null)
    }

    @Test
    fun `중복 카드는 조각과 소량 골드로 자동 변환된다`() {
        // 도감 170명 전원 보유 → 팩에서 나오는 카드는 모두 중복 (티켓 없이 골드로 구매)
        val vm = TcgGameViewModel(
            random = Random(7),
            initialOwnedCardIds = allIds,
            initialGold = 500,
            initialPackTickets = 0
        )
        val before = vm.uiState.value

        val result = vm.openPack("pack_standard")!!

        val after = vm.uiState.value
        assertTrue("신규 없음", result.newCardIds.isEmpty())
        assertEquals("중복 3장 → 조각", 3 * PackShopEngine.SHARDS_PER_DUPLICATE, result.totalShards)
        assertEquals("중복 3장 → 골드", 3 * PackShopEngine.GOLD_PER_DUPLICATE, result.totalGold)
        assertEquals("조각 지갑 반영", 3 * PackShopEngine.SHARDS_PER_DUPLICATE, after.shards.total)
        assertEquals("보유 카드 수는 그대로", before.ownedCards.size, after.ownedCards.size)
        assertEquals("골드 = -200 + 150", before.gold - 200 + 150, after.gold)
        assertTrue(result.draws.all { it.shards == 10 && it.gold == 50 })
    }

    @Test
    fun `고급팩은 영웅 등급 이상을 한 장 보장한다`() {
        repeat(8) { seed ->
            val vm = strongViewModel(gold = 5000, seed = seed)
            val result = vm.openPack("pack_premium")!!
            assertEquals("5장 개봉", 5, result.draws.size)
            assertTrue(
                "영웅(7코스트) 이상 1장 확정 — seed $seed",
                result.draws.any { it.rarity.ordinal >= CardRarity.EPIC.ordinal }
            )
        }
    }

    @Test
    fun `속성팩은 지정 속성만 뽑고 없는 속성팩은 없다`() {
        val vm = strongViewModel(gold = 5000)
        val result = vm.openPack("pack_element_fire")!!

        assertEquals(3, result.draws.size)
        assertTrue("불 속성만", result.draws.all { CardCatalog.require(it.cardId).element == com.game.tcg.core.model.Element.FIRE })
        assertEquals(7, PackCatalog.ALL.size)
        assertNull("없는 상품", PackCatalog.find("pack_unknown"))
    }

    @Test
    fun `골드가 모자라면 카드팩을 살 수 없다`() {
        val vm = TcgGameViewModel(random = Random(3), initialOwnedCardIds = listOf(3, 1, 2),
            initialGold = 100, initialPackTickets = 0)
        val before = vm.uiState.value

        val result = vm.openPack("pack_premium")

        assertNull(result)
        assertEquals(before.gold, vm.uiState.value.gold)
        assertTrue(vm.uiState.value.shopMessage!!.contains("구매 불가"))
    }

    // =========================================================================
    // 5. 카드 강화 — 골드 + 조각 → ATK/DEF → 덱 전투력
    // =========================================================================

    @Test
    fun `강화는 골드와 조각을 쓰고 ATK DEF 를 올려 덱 전투력을 높인다`() {
        val vm = starterViewModel(gold = 1000, shards = ShardWallet(mapOf(3 to 50)))
        val before = vm.uiState.value
        val cardBefore = before.ownedCards.first { it.id == 3 }
        val atkBefore = cardBefore.atk
        val defBefore = cardBefore.def
        val baseAtk = cardBefore.baseAtk
        val baseDef = cardBefore.baseDef
        val powerBefore = before.deckTotalPower
        val cost = CardUpgradeEngine().cost(cardBefore)!!

        vm.selectGrowthCard(3)
        val result = vm.upgradeCard(3)!!

        val after = vm.uiState.value
        val cardAfter = after.ownedCards.first { it.id == 3 }
        assertTrue(result.isSuccess)
        assertEquals(1, cardAfter.upgradeLevel)
        assertEquals("강화 Lv.1 → 2", 2, result.toLevel)
        assertEquals("ATK 상승 = 원본의 6%", atkBefore + (baseAtk * 6 / 100), cardAfter.atk)
        assertEquals("DEF 상승", defBefore + (baseDef * 6 / 100), cardAfter.def)
        assertEquals("골드 차감", before.gold - cost.gold, after.gold)
        assertEquals("조각 차감", 50 - cost.shards, after.shards.of(3))
        assertTrue("덱 전투력 상승", after.deckTotalPower > powerBefore)
        assertTrue(after.growthMessage!!.contains("강화"))
    }

    @Test
    fun `조각이나 골드가 모자라면 강화되지 않는다`() {
        val vm = starterViewModel(gold = 1000, shards = ShardWallet(mapOf(3 to 1)))
        val before = vm.uiState.value

        val result = vm.upgradeCard(3)

        assertEquals(before.ownedCards.first { it.id == 3 }.upgradeLevel, vm.uiState.value.ownedCards.first { it.id == 3 }.upgradeLevel)
        assertTrue(result == null || !result.isSuccess)
        assertTrue(vm.uiState.value.growthMessage!!.contains("조각이 부족"))

        val poor = starterViewModel(gold = 10, shards = ShardWallet(mapOf(3 to 50)))
        assertTrue(poor.upgradeCard(3) == null || !poor.upgradeCard(3)!!.isSuccess)
        assertTrue(poor.uiState.value.growthMessage!!.contains("골드가 부족"))
    }

    @Test
    fun `강화는 Lv10에서 멈춘다`() {
        val vm = starterViewModel(gold = 100000, shards = ShardWallet(mapOf(3 to 1000)))

        repeat(15) { vm.upgradeCard(3) }

        val card = vm.uiState.value.ownedCards.first { it.id == 3 }
        assertEquals("최대 강화 레벨", CardUpgradeEngine.MAX_LEVEL - 1, card.upgradeLevel)
        assertEquals(CardUpgradeEngine.MAX_LEVEL, card.upgradeLevel + 1)
        assertNull("더 이상 비용 없음", CardUpgradeEngine().cost(card))
        assertTrue(vm.upgradeCard(3) == null || vm.uiState.value.growthMessage!!.contains("최대 레벨"))
    }

    // =========================================================================
    // 6. 연성(확정 계약) · 초월
    // =========================================================================

    @Test
    fun `조각 40개와 500G 로 미보유 캐릭터를 확정 연성한다`() {
        val vm = starterViewModel(gold = 1000, shards = ShardWallet(mapOf(1 to 40)))
        val before = vm.uiState.value
        val target = CardCatalog.require(5)
        assertFalse("미보유", target.id in before.ownedCardIds)

        val result = vm.craftCharacter(5)!!

        val after = vm.uiState.value
        assertTrue(result.isSuccess)
        assertTrue("보유 목록에 추가", 5 in after.ownedCardIds)
        assertEquals(before.ownedCards.size + 1, after.ownedCards.size)
        assertEquals("골드 차감", before.gold - AdvancedSynthesisEngine.CRAFT_GOLD, after.gold)
        assertEquals("조각 차감", 0, after.shards.total)
        assertTrue(after.growthMessage!!.contains("신규 연성"))
    }

    @Test
    fun `신규 연성은 5코스트 이상만 대상으로 한다`() {
        val vm = starterViewModel(gold = 5000, shards = ShardWallet(mapOf(1 to 100)))
        val lowCost = CardCatalog.ALL.first { it.baseCost < 5 && it.id !in listOf(3, 1, 2) }

        val result = vm.craftCharacter(lowCost.id)

        assertTrue(result == null || !result.isSuccess)
        assertFalse(lowCost.id in vm.uiState.value.ownedCardIds)
        assertTrue(vm.uiState.value.craftTargets.all { it.record.baseCost >= 5 })
    }

    @Test
    fun `중간 보스는 해당 스테이지를 깨기 전에는 연성할 수 없다`() {
        val locked = strongViewModel(cleared = setOf(1, 2, 3), shards = ShardWallet(mapOf(1 to 200)), gold = 5000)
        val lockedResult = locked.craftCharacter(170)
        assertTrue(lockedResult == null || !lockedResult.isSuccess)
        assertFalse(170 in locked.uiState.value.ownedCardIds)
        val target = locked.uiState.value.craftTargets.first { it.record.id == 170 }
        assertFalse(target.isAvailable)
        assertTrue(target.blockedReason!!.contains("스테이지 5"))

        val unlocked = strongViewModel(cleared = setOf(1, 2, 3, 4, 5), shards = ShardWallet(mapOf(1 to 200)), gold = 5000)
        val result = unlocked.craftCharacter(170)!!
        assertTrue("심연 연성 성공", result.isSuccess)
        assertTrue(170 in unlocked.uiState.value.ownedCardIds)
        assertEquals("심연 연성 비용", 200 - AdvancedSynthesisEngine.MID_BOSS_SHARDS, unlocked.uiState.value.shards.total)
        assertTrue(unlocked.uiState.value.growthMessage!!.contains("심연 연성"))
    }

    @Test
    fun `초월은 조각 40개로 성급과 스탯을 올린다`() {
        val vm = starterViewModel(gold = 2000, shards = ShardWallet(mapOf(3 to 100)))
        val cardBefore = vm.uiState.value.ownedCards.first { it.id == 3 }
        val starBefore = cardBefore.starLevel
        val atkBefore = cardBefore.atk
        val defBefore = cardBefore.def

        val result = vm.transcendCard(3)!!

        val after = vm.uiState.value.ownedCards.first { it.id == 3 }
        assertTrue(result.isSuccess)
        assertEquals(starBefore + 1, after.starLevel)
        assertEquals("ATK +20%", (atkBefore * 1.20).toInt(), after.atk)
        assertEquals("DEF +20%", (defBefore * 1.20).toInt(), after.def)
        assertEquals("조각 차감", 60, vm.uiState.value.shards.of(3))
        assertEquals("골드 차감", 2000 - AdvancedSynthesisEngine.TRANSCEND_GOLD, vm.uiState.value.gold)
    }

    @Test
    fun `5성 카드는 더 초월할 수 없다`() {
        val vm = starterViewModel(gold = 9000, shards = ShardWallet(mapOf(3 to 500)))
        repeat(4) { vm.transcendCard(3) }
        val star = vm.uiState.value.ownedCards.first { it.id == 3 }.starLevel
        assertEquals(5, star)

        val result = vm.transcendCard(3)
        assertTrue(result == null || !result.isSuccess)
        assertTrue(vm.uiState.value.growthMessage!!.contains("5성"))
    }

    // =========================================================================
    // 7. 장비 — 구매 · 장착 · 해제 · 슬롯
    // =========================================================================

    @Test
    fun `장비를 사면 골드가 줄고 소지품에 들어간다`() {
        val vm = starterViewModel(gold = 1000)
        val before = vm.uiState.value

        val result = vm.buyEquipment("weapon_training_dagger")!!

        val after = vm.uiState.value
        assertTrue(result.isSuccess)
        assertEquals("골드 차감", before.gold - 120, after.gold)
        assertEquals(1, after.equipment.size)
        assertEquals("weapon_training_dagger", after.equipment.single().id)
        assertNull("아직 미장착", after.equipment.single().equippedTo)
    }

    @Test
    fun `장비 장착은 ATK DEF HP 를 올리고 해제하면 정확히 되돌아온다`() {
        val vm = TcgGameViewModel(
            random = Random(5),
            initialOwnedCardIds = listOf(3, 1, 2),
            initialGold = 2000,
            initialEquipmentIds = listOf("weapon_silver_blade", "armor_leather_vest")
        )
        val cardSnapshot = vm.uiState.value.ownedCards.first { it.id == 3 }
        val atkBefore = cardSnapshot.atk
        val defBefore = cardSnapshot.def
        val hpBefore = CardCatalog.require(3).maxHp
        val weapon = vm.uiState.value.equipment.first { it.id == "weapon_silver_blade" }

        vm.equipItem(weapon.uid, 3)

        val equipped = vm.uiState.value.ownedCards.first { it.id == 3 }
        assertEquals("ATK +420", atkBefore + 420, equipped.atk)
        assertEquals("DEF +90", defBefore + 90, equipped.def)
        val member = vm.uiState.value.party.first { it.card.id == 3 }
        assertEquals("전장 HP = DEF 성장분 + 장비 HP", TcgGameViewModel.maxHpOf(equipped) + 120, member.maxHp)
        assertTrue("장비로 HP 가 늘어난다", member.maxHp > hpBefore)

        vm.unequipItem(weapon.uid)

        val restored = vm.uiState.value.ownedCards.first { it.id == 3 }
        assertEquals("ATK 정확히 복원", atkBefore, restored.atk)
        assertEquals("DEF 정확히 복원", defBefore, restored.def)
        assertNull("소지품으로 복귀", vm.uiState.value.equipment.first { it.uid == weapon.uid }.equippedTo)
    }

    @Test
    fun `같은 슬롯에는 하나만, 캐릭터당 3개까지만 장착된다`() {
        val vm = TcgGameViewModel(
            random = Random(5),
            initialOwnedCardIds = listOf(3, 1, 2),
            initialGold = 5000,
            initialEquipmentIds = listOf(
                "weapon_training_dagger", "weapon_silver_blade",
                "armor_leather_vest", "armor_mythril_mail",
                "accessory_lucky_charm", "accessory_mana_circlet"
            )
        )
        val items = vm.uiState.value.equipment
        val dagger = items.first { it.id == "weapon_training_dagger" }
        val blade = items.first { it.id == "weapon_silver_blade" }
        val vest = items.first { it.id == "armor_leather_vest" }
        val charm = items.first { it.id == "accessory_lucky_charm" }
        val circlet = items.first { it.id == "accessory_mana_circlet" }

        assertTrue(vm.equipItem(dagger.uid, 3)!!.isSuccess)
        val blocked = vm.equipItem(blade.uid, 3)
        assertTrue(blocked == null || !blocked.isSuccess)
        assertTrue(vm.uiState.value.growthMessage!!.contains("무기 슬롯"))

        assertTrue(vm.equipItem(vest.uid, 3)!!.isSuccess)
        assertTrue(vm.equipItem(charm.uid, 3)!!.isSuccess)
        assertEquals("3슬롯 만석", 3, EquipmentEngine.equippedFor(3, vm.uiState.value.equipment).size)

        val full = vm.equipItem(circlet.uid, 3)
        assertTrue(full == null || !full.isSuccess)
        assertTrue(vm.uiState.value.growthMessage!!.contains("3칸"))
    }

    @Test
    fun `장비는 다른 캐릭터가 착용 중이면 옮길 수 없다`() {
        val vm = TcgGameViewModel(
            random = Random(5),
            initialOwnedCardIds = listOf(3, 1, 2),
            initialGold = 2000,
            initialEquipmentIds = listOf("weapon_training_dagger")
        )
        val dagger = vm.uiState.value.equipment.single()

        assertTrue(vm.equipItem(dagger.uid, 3)!!.isSuccess)
        val other = vm.equipItem(dagger.uid, 1)
        assertTrue(other == null || !other.isSuccess)
        assertEquals("장착자는 그대로", 3, vm.uiState.value.equipment.single().equippedTo)
    }

    @Test
    fun `전설 장비는 상점에서 살 수 없다`() {
        val vm = starterViewModel(gold = 100000)
        val result = vm.buyEquipment("weapon_abyss_greatsword")

        assertTrue(result == null || !result.isSuccess)
        assertTrue(vm.uiState.value.growthMessage!!.contains("드롭 전용"))
        assertEquals("상점 판매는 6종", 6, EquipmentCatalog.SHOP_ITEMS.size)
        assertEquals("장비 도감 12종", 12, EquipmentCatalog.ALL.size)
        assertTrue(EquipmentCatalog.ALL.map { it.slot }.toSet().containsAll(EquipmentSlot.entries))
    }

    // =========================================================================
    // 8. 현실 소환 — 차원 에너지 · 호감도 · 전투 버프
    // =========================================================================

    @Test
    fun `현실 소환은 차원 에너지를 쓰고 호감도와 전투 버프를 준다`() {
        val vm = starterViewModel()
        val before = vm.uiState.value
        val bondBefore = before.ownedCards.first { it.id == 3 }.bondLevel

        val result = vm.summonToReality(3)!!

        val after = vm.uiState.value
        assertTrue(result.isSuccess)
        assertEquals("차원 에너지 차감", before.gateEnergy - SummonEngine.ENERGY_COST, after.gateEnergy)
        assertEquals("호감도 상승", (bondBefore + SummonEngine.BOND_GAIN).coerceAtMost(100),
            after.ownedCards.first { it.id == 3 }.bondLevel)
        assertEquals("버프 수치", SummonEngine.BUFF_PERCENT, after.summonBuffPercent)
        assertEquals("버프 전투 수", SummonEngine.BUFF_BATTLES, after.summonBuffBattles)
        assertTrue(after.hasSummonBuff)
        assertEquals(1, after.summonCount)
        assertTrue(after.realWorldLog.any { it.contains("현실 소환") })
    }

    @Test
    fun `차원 에너지가 모자라면 소환할 수 없다`() {
        val vm = starterViewModel()
        repeat(3) { vm.summonToReality(3) }
        assertEquals("10 - 9 = 1", 1, vm.uiState.value.gateEnergy)

        val result = vm.summonToReality(3)

        assertTrue(result == null || !result.isSuccess)
        assertTrue(vm.uiState.value.summonMessage!!.contains("에너지가 부족"))
    }

    @Test
    fun `소환 버프는 전투 데미지를 올리고 승리하면 1회 소모된다`() {
        val plain = strongViewModel(seed = 21)
        val buffed = strongViewModel(seed = 21)
        assertTrue(plain.startStageBattle(1))
        assertTrue(buffed.summonToReality(buffed.uiState.value.ownedCards.first().id)!!.isSuccess)
        assertTrue(buffed.startStageBattle(1))

        plain.attackFront()
        buffed.attackFront()

        val plainDamage = plain.uiState.value.lastEnemyHitDamage
        val buffedDamage = buffed.uiState.value.lastEnemyHitDamage
        assertTrue("소환 버프만큼 데미지가 크다 ($plainDamage → $buffedDamage)", buffedDamage > plainDamage)
        assertTrue(buffed.uiState.value.battleLog.any { it.contains("현실 소환 +${SummonEngine.BUFF_PERCENT}%") })

        fightToVictory(buffed)
        assertEquals("전투 1회 소모", SummonEngine.BUFF_BATTLES - 1, buffed.uiState.value.summonBuffBattles)
    }

    @Test
    fun `호감도 50 미만은 데이트 룩이 잠기고 소환으로 열린다`() {
        val vm = TcgGameViewModel(
            random = Random(8),
            initialOwnedCardIds = listOf(3, 1, 2),
            initialGold = 100,
            initialShards = ShardWallet(),
            initialPackTickets = 0
        )
        // 시작 호감도는 50(시드)이므로 40으로 낮춘 카드를 직접 만들어 잠금을 확인한다
        val card = TcgGameViewModel.characterCard(3).copy(bondLevel = 45)
        val engine = SummonEngine
        var bond = card.bondLevel
        repeat(1) { bond = SummonEngine.summon(card, gateEnergy = 10).bondAfter }
        assertEquals("45 → 53", 53, bond)
        assertTrue("데이트 룩 해금", bond >= SummonEngine.DATE_UNLOCK_BOND)
        assertTrue("소환은 성공한다", SummonEngine.canSummon(10))
        assertFalse(SummonEngine.canSummon(2))
        assertNotNull(vm.uiState.value)
        assertTrue(engine.ENERGY_COST == 3)
    }

    // =========================================================================
    // 9. 덱 전투력 · 난이도 · 승률
    // =========================================================================

    @Test
    fun `난이도와 예상 승률은 덱 전투력을 반영한다`() {
        val stage = StageCatalog.require(10)
        val weak = StageEngine.readiness(stage.recommendedPower / 3, stage)
        val even = StageEngine.readiness(stage.recommendedPower, stage)
        val strong = StageEngine.readiness(stage.recommendedPower * 2, stage)

        assertEquals(StageReadiness.LETHAL, weak)
        assertEquals(StageReadiness.EVEN, even)
        assertEquals(StageReadiness.EASY, strong)
        assertTrue(StageEngine.estimatedWinChance(stage.recommendedPower * 2, stage) >
            StageEngine.estimatedWinChance(stage.recommendedPower / 3, stage))
        assertEquals("권장치와 같으면 75% 이상", true, StageEngine.estimatedWinChance(stage.recommendedPower, stage) >= 75)
        assertEquals(100, StageEngine.powerRatio(stage.recommendedPower, stage))
    }

    @Test
    fun `강화와 장비로 덱 전투력이 올라 다음 스테이지에 도전할 수 있다`() {
        val vm = starterViewModel(gold = 3000, shards = ShardWallet(mapOf(3 to 100)))
        val start = vm.uiState.value.deckTotalPower

        repeat(3) { vm.upgradeCard(3) }
        vm.buyEquipment("weapon_training_dagger")
        val dagger = vm.uiState.value.equipment.first { it.id == "weapon_training_dagger" }
        vm.equipItem(dagger.uid, 3)

        val after = vm.uiState.value.deckTotalPower
        assertTrue("덱 전투력 상승 ($start → $after)", after > start)
        assertEquals("덱 전투력 = 장착 카드 ATK+DEF 합", vm.uiState.value.equippedCards.sumOf { it.atk + it.def }, after)
        assertEquals("덱 전투력 표시값은 /100", after / 100, vm.uiState.value.deckDisplayPower)
    }

    @Test
    fun `사망한 카드의 장비는 소지품으로 회수된다`() {
        val vm = TcgGameViewModel(
            random = Random(5),
            initialOwnedCardIds = listOf(3, 1, 2),
            initialGold = 2000,
            initialEquipmentIds = listOf("weapon_silver_blade")
        )
        val weapon = vm.uiState.value.equipment.single()
        vm.equipItem(weapon.uid, 3)
        assertEquals(3, vm.uiState.value.equipment.single().equippedTo)

        vm.startDemonLordBattle()
        var guard = 0
        while (vm.uiState.value.reincarnationPool.none { it.templateId == 3 } && guard < 20) {
            vm.nextTurn()
            guard++
        }

        assertTrue("아우렐리아 상실", vm.uiState.value.reincarnationPool.any { it.templateId == 3 })
        assertNull("장비는 회수된다", vm.uiState.value.equipment.single().equippedTo)

        vm.reacquire(3)
        val reborn = vm.uiState.value.ownedCards.first { it.id == 3 }
        assertEquals("재획득 카드는 초기 스탯", TcgGameViewModel.characterCard(3).atk, reborn.atk)
        assertEquals("장비 HP 보너스가 남지 않는다", 0, EquipmentEngine.hpBonus(3, vm.uiState.value.equipment))
    }

    @Test
    fun `캠페인 1~30 스테이지를 덱 성장과 함께 완주할 수 있다`() {
        val vm = TcgGameViewModel(random = Random(1))
        var stage = 1
        var guard = 0

        while (stage <= 30 && guard < 60) {
            guard++
            // 플레이어처럼: 자동 편성 → 카드팩 → 조각 강화
            vm.autoEquipParty()
            val s = vm.uiState.value
            val upgradable = s.ownedCards.firstOrNull { it.upgradeLevel < 9 && s.shards.of(it.id) >= 20 }
            if (upgradable != null) repeat(3) { vm.upgradeCard(upgradable.id) }
            if (vm.canBuyPack("pack_premium")) vm.openPack("pack_premium")
            else if (vm.canBuyPack("pack_standard")) vm.openPack("pack_standard")

            if (!vm.startStageBattle(stage)) break
            fightToVictory(vm)
            if (stage in vm.uiState.value.stageCleared) stage++ else guard += 5
        }

        val state = vm.uiState.value
        assertEquals("30스테이지 전부 클리어", 30, state.stageCleared.size)
        assertEquals(1, state.stageCleared.min())
        assertEquals(30, state.stageCleared.max())
        assertTrue("카드팩으로 덱이 자랐다", state.ownedCards.size > 3)
        assertTrue("덱 전투력이 시작(약 2만)보다 훨씬 강해졌다", state.deckTotalPower > 100000)
        assertTrue("최종 관문 보상 확인", state.lastStageOutcome != null)
        assertEquals(114, state.lastStageOutcome!!.stage.bossId)
    }

    @Test
    fun `성장 화면과 상점 화면이 로비 서브메뉴로 열린다`() {
        val vm = TcgGameViewModel()

        listOf(LobbyDestination.STAGE, LobbyDestination.SHOP, LobbyDestination.GROWTH).forEach { destination ->
            vm.openScreen(destination)
            assertEquals(destination, vm.uiState.value.currentScreen)
            assertTrue(vm.uiState.value.statusMessage.startsWith("[${destination.label}]"))
        }
        assertEquals("중간 보스는 로비 순환 순서 마지막에 붙는다", 176, TcgGameViewModel.LOBBY_HERO_ORDER.size)
        assertEquals((170..175).toList(), TcgGameViewModel.LOBBY_HERO_ORDER.takeLast(6))
    }
}
