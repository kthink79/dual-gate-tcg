package com.game.tcg

import com.game.tcg.core.engine.ExplorationEngine
import com.game.tcg.core.engine.ExplorationMode
import com.game.tcg.core.engine.ExplorationRewardType
import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.VisualState
import com.game.tcg.ui.CharacterArtRegistry
import com.game.tcg.ui.TcgGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 170명 전원 탑재 검증 (No.1~169 + No.184 = 170명 × 3단 비주얼 = JPG 510장).
 *
 * - 도감 카탈로그 170명 데이터(이름·칭호·ATK·DEF·속성·코스트·스킬) 완비
 * - 510장 리소스 매핑 누락 0건
 * - 로비 대표 캐릭터 순환 · 3단 비주얼 · 도감 필터 · 서약 덱 · 전투 편성 · 탐색 조우
 */
class FullRoster170Test {

    // ──────────────────────────────────────────────────────────────
    // 1. 170명 데이터
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `카탈로그는 1번부터 169번 그리고 184번까지 정확히 170명이다`() {
        val expected = (1..169).toList() + 184

        assertEquals(170, CardCatalog.ALL.size)
        assertEquals(expected, CardCatalog.ALL.map { it.id })
        assertEquals(170, CardCatalog.ROSTER_SIZE)
        assertEquals("삭제된 170~183번은 카탈로그에 남지 않는다", 0,
            CardCatalog.ALL.count { it.id in 170..183 })
    }

    @Test
    fun `170명 전원이 이름 칭호 스탯 속성 스킬을 갖추고 전체 이름이 겹치지 않는다`() {
        val all = CardCatalog.ALL

        all.forEach { record ->
            assertTrue("#${record.id} 이름 누락", record.name.isNotBlank())
            assertTrue("#${record.id} 칭호 누락", record.title.isNotBlank())
            assertTrue("#${record.id} 스킬 누락", record.skill.isNotBlank())
            assertTrue("#${record.id} ATK ${record.atk}", record.atk in 2000..11000)
            assertTrue("#${record.id} DEF ${record.def}", record.def in 2000..11000)
            assertTrue("#${record.id} 코스트 ${record.baseCost}", record.baseCost in 3..10)
            assertTrue("#${record.id} 최대 HP", record.maxHp > 0)
            assertTrue("#${record.id} 호감도 시드", record.bondSeed in 0..100)
        }

        assertEquals(
            "카드에 표시되는 '칭호 + 이름'은 170명 모두 달라야 구분된다",
            170,
            all.map { it.fullName }.toSet().size
        )
        assertEquals(
            "5속성 전부에 분포해야 상성 시스템이 의미를 갖는다",
            Element.values().toSet(),
            all.map { it.element }.toSet()
        )
        assertEquals("170명 전원 호감도 50으로 합류한다", 50, all.map { it.bondSeed }.toSet().single())
    }

    @Test
    fun `신규 15인은 155번부터 169번이고 기존 155인과 합쳐 170명이다`() {
        assertEquals(listOf(155, 169), listOf(CardCatalog.NEW_WAVE_RANGE.first, CardCatalog.NEW_WAVE_RANGE.last))
        assertEquals(15, CardCatalog.NEW_WAVE_IDS.size)
        assertEquals(155, CardCatalog.LEGACY_IDS.size)
        assertEquals(170, CardCatalog.NEW_WAVE_IDS.size + CardCatalog.LEGACY_IDS.size)
        assertEquals(155, CardCatalog.newWave().first().id)
        assertEquals(169, CardCatalog.newWave().last().id)
    }

    @Test
    fun `배치별 대표 캐릭터가 소스 문서와 같은 이름 칭호로 등록되어 있다`() {
        // 1~50 · 51~114 · 115~124 · 125~144 · 145~154 · 155~169 · 184 표본
        val samples = mapOf(
            1 to ("세라피엘" to "태양의 대천사"),
            3 to ("아우렐리아" to "천상의 성기사"),
            56 to ("설유진" to "빙화검후"),
            112 to ("릴리스" to "황혼과 욕망의 마왕"),
            115 to ("서설현" to "달빛 한옥의 검무사"),
            121 to ("발렌티나" to "파펠 광장의 화염 결투가"),
            125 to ("사랑게렐" to "초원의 기마 궁수"),
            144 to ("로자리아" to "향로 퇴마사"),
            145 to ("오렐리" to "꽃잎 왈츠의 마도사"),
            150 to ("코하쿠" to "꽃우산의 아가씨"),
            155 to ("엘로디" to "폭풍검의 결투가"),
            161 to ("린" to "서리꽃의 여검사"),
            169 to ("유메" to "해바라기 질주자"),
            184 to ("아메넷" to "태양 장막의 예언자")
        )

        samples.forEach { (id, expected) ->
            val record = CardCatalog.require(id)
            assertEquals("#$id 이름", expected.first, record.name)
            assertEquals("#$id 칭호", expected.second, record.title)
        }
    }

    @Test
    fun `3대 마왕은 마왕 플래그와 위장 신분을 유지한다`() {
        assertEquals(listOf(112, 113, 114), CardCatalog.DEMON_LORD_IDS)
        assertEquals(listOf(112, 113, 114), CardCatalog.demonLords().map { it.id })

        val disguises = CardCatalog.demonLords().map { it.isDemonLord to it.disguiseName }
        assertEquals(listOf(true to "릴리", true to "밀라", true to "모나"), disguises)
        assertTrue("마왕은 어둠 속성", CardCatalog.demonLords().all { it.element == Element.DARK })
    }

    // ──────────────────────────────────────────────────────────────
    // 2. JPG 510장 리소스 매핑 누락 0건
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `170명의 3단 비주얼 510장이 모두 리소스로 연결되어 있다`() {
        var mapped = 0
        CardCatalog.ALL.forEach { record ->
            assertTrue("#${record.id} 아트 미등록", CharacterArtRegistry.hasArt(record.id))
            VisualState.values().forEach { visual ->
                val resId = CharacterArtRegistry.resId(record.id, visual)
                assertNotNull("#${record.id} ${visual.name} 리소스 없음", resId)
                assertTrue("#${record.id} ${visual.name} 리소스 id 0", (resId ?: 0) != 0)
                mapped++
            }
        }
        assertEquals("170명 × 3비주얼 = 510장", 510, mapped)
        assertEquals(170, CharacterArtRegistry.registeredCardCount)
        assertEquals(CardCatalog.ALL.map { it.id }.toSet(), CharacterArtRegistry.registeredCardIds)
        assertNull("등록되지 않은 번호는 null", CharacterArtRegistry.resId(999, VisualState.BATTLE))
    }

    @Test
    fun `한 캐릭터의 세 비주얼은 서로 다른 리소스를 가리킨다`() {
        CardCatalog.ALL.forEach { record ->
            val ids = VisualState.values().map { CharacterArtRegistry.resId(record.id, it) }
            assertEquals("#${record.id} 3단 비주얼이 모두 같은 그림이다", 3, ids.toSet().size)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 3. 로비 · 도감 · 덱
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `게임을 시작하면 170명 전원을 보유하고 로비 로스터에 모두 오른다`() {
        val state = TcgGameViewModel().uiState.value

        assertEquals(170, state.ownedCards.size)
        assertEquals(170, state.lobbyRoster.size)
        assertEquals(state.ownedCards.map { it.id }.toSet(), state.lobbyRoster.toSet())
        assertEquals("스타팅 3인방이 먼저 온다", listOf(3, 1, 2), state.lobbyRoster.take(3))
        assertEquals(170, TcgGameViewModel.profiles().size)
    }

    @Test
    fun `로비 좌우 전환은 170명을 한 바퀴 순환한다`() {
        val vm = TcgGameViewModel()
        val roster = vm.uiState.value.lobbyRoster

        vm.cycleLobbyHero(1)
        assertEquals(1, vm.uiState.value.lobbyHeroId)
        vm.cycleLobbyHero(1)
        assertEquals(2, vm.uiState.value.lobbyHeroId)
        vm.cycleLobbyHero(1)
        assertEquals("세 번째 다음은 4번", 4, vm.uiState.value.lobbyHeroId)

        vm.selectLobbyHero(184)
        vm.cycleLobbyHero(1)
        assertEquals("마지막(184) 다음은 처음(3)", 3, vm.uiState.value.lobbyHeroId)
        vm.cycleLobbyHero(-1)
        assertEquals("역방향도 순환한다", 184, vm.uiState.value.lobbyHeroId)
        assertEquals(170, roster.size)
    }

    @Test
    fun `등록되지 않은 번호만 대표 캐릭터가 될 수 없다`() {
        val vm = TcgGameViewModel()
        vm.selectLobbyHero(112)
        assertEquals("마왕 릴리스도 170명 로스터에 포함되어 대표가 될 수 있다", 112, vm.uiState.value.lobbyHeroId)

        vm.selectLobbyHero(999)
        assertEquals("미등록 번호는 거부된다", 112, vm.uiState.value.lobbyHeroId)
        assertTrue(vm.uiState.value.statusMessage.contains("계약하지 않은"))
    }

    @Test
    fun `170명 전원이 처음부터 3단 비주얼을 모두 열람할 수 있다`() {
        val vm = TcgGameViewModel()

        listOf(3, 1, 2, 1, 112, 155, 169, 184).forEach { id ->
            vm.selectLobbyHero(id)
            assertTrue("#$id 호감도 50 이상이어야 데이트 룩이 열린다", vm.uiState.value.isLobbyDateLookUnlocked)
            vm.selectLobbyVisual(VisualState.ROMANTIC_DATE)
            assertEquals("#$id 데이트 룩 전환", VisualState.ROMANTIC_DATE, vm.uiState.value.lobbyVisual)
            vm.selectLobbyVisual(VisualState.CASUAL_CUTE)
            assertEquals("#$id 일상 룩 전환", VisualState.CASUAL_CUTE, vm.uiState.value.lobbyVisual)
        }
    }

    @Test
    fun `도감 필터는 전체 170 신규 15 기존 155로 나뉜다`() {
        val profiles = TcgGameViewModel.profiles()

        assertEquals(170, profiles.size)
        assertEquals(15, profiles.count { CardCatalog.isNewWave(it.cardId) })
        assertEquals(155, profiles.count { !CardCatalog.isNewWave(it.cardId) })
        assertEquals(155, CardCatalog.legacy().size)
        assertTrue(CardCatalog.legacy().all { it.id !in 155..169 })
    }

    @Test
    fun `도감에서 어떤 카드를 골라도 상세와 전용 스킬이 보인다`() {
        val vm = TcgGameViewModel()

        listOf(1, 65, 112, 144, 169, 184).forEach { id ->
            vm.selectCard(id)
            val state = vm.uiState.value
            val card = state.ownedCards.first { it.id == id }
            val record = CardCatalog.require(id)

            assertEquals("#$id 선택", id, state.selectedCardId)
            assertEquals(record.fullName, card.name)
            assertTrue("#$id 스킬", card.skillText.isNotBlank())
            vm.selectVisual(VisualState.ROMANTIC_DATE)
            assertEquals(VisualState.ROMANTIC_DATE, vm.uiState.value.selectedVisual)
        }
    }

    @Test
    fun `서약 덱은 170명 전원을 카드로 편성 목록에 올린다`() {
        val state = TcgGameViewModel().uiState.value
        val characters = state.ownedCards.filter { it.category.isCharacter }

        assertEquals(170, characters.size)
        assertEquals("1캐릭터 1장 유일 원칙", 170, characters.map { it.id }.toSet().size)
    }

    // ──────────────────────────────────────────────────────────────
    // 4. 전투 편성 · 소환
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `어떤 캐릭터든 전열 후열 슬롯에 소환하면 전투 파티에 반영된다`() {
        val vm = TcgGameViewModel()

        vm.deployToParty(155, 0)
        val front = vm.uiState.value.party.first { it.slotIndex == 0 }
        assertEquals(155, front.card.id)
        assertTrue(front.isFrontRow)
        assertEquals("3인 편성을 유지한 채 0번 슬롯만 교체된다", 3, vm.uiState.value.party.size)
        assertEquals("전열 HP는 DEF 기반", (3400 * 0.85).toInt(), front.maxHp)

        vm.deployToParty(184, 4)
        val back = vm.uiState.value.party.first { it.slotIndex == 4 }
        assertEquals(184, back.card.id)
        assertFalse(back.isFrontRow)

        // 같은 캐릭터를 다른 슬롯에 다시 소환하면 이전 슬롯에서 빠진다
        vm.deployToParty(155, 2)
        assertEquals(1, vm.uiState.value.party.count { it.card.id == 155 })
        assertNull("0번 슬롯은 비워진다", vm.uiState.value.party.firstOrNull { it.slotIndex == 0 })
    }

    @Test
    fun `170명 전원이 전장에 나가 싸울 수 있다`() {
        CardCatalog.ALL.forEach { record ->
            val id = record.id
            val vm = TcgGameViewModel()
            vm.deployToParty(id, 0)
            vm.startFieldBattle()

            assertEquals("#$id 편성 실패", 1, vm.uiState.value.party.count { it.card.id == id })

            val beforeHp = vm.uiState.value.enemyHp
            vm.attackFront()
            val after = vm.uiState.value
            assertTrue("#$id 공격이 적에게 반영되지 않는다", after.enemyHp < beforeHp)
            assertTrue(
                "#$id 가 실제 공격자로 전투 로그에 남아야 한다",
                after.battleLog.any { it.contains("${CardCatalog.require(id).fullName} → ") }
            )
        }
    }

    @Test
    fun `편성에서 뺀 캐릭터는 대기열로 이동한다`() {
        val vm = TcgGameViewModel()
        vm.deployToParty(161, 3)
        vm.undeployFromParty(161)

        val state = vm.uiState.value
        assertTrue(state.party.none { it.card.id == 161 })
        assertTrue(state.battleReserve.any { it.id == 161 })
    }

    // ──────────────────────────────────────────────────────────────
    // 5. 탐색 조우
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `탐색 랜덤 조우는 170명 전원을 대상으로 한다`() {
        // 판정 64 → r<70 조우 분기, index = (탐색 횟수 0 + 64) % 170 = 64 → 카탈로그 65번
        val vm = TcgGameViewModel(explorationEngine = ExplorationEngine { 64 })
        val target = CardCatalog.ALL[64]
        val before = vm.uiState.value.ownedCards.first { it.id == target.id }.bondLevel

        val result = vm.explore(ExplorationMode.ENCOUNTER)

        assertNotNull(result)
        assertEquals(target.id, result!!.companionCardId)
        assertEquals(target.fullName, result.companionDisplayName)
        assertTrue(result.rewards.any { it.type == ExplorationRewardType.COMPANION_BOND })
        assertEquals(before + 15, vm.uiState.value.ownedCards.first { it.id == target.id }.bondLevel)
    }

    @Test
    fun `조우 연출은 170명 각각 다른 인연을 걸 수 있다`() {
        // 탐색 횟수를 0~169로 돌리면 170명 전원이 한 번씩 조우 대상이 된다
        val encountered = (0..169).map { count ->
            ExplorationEngine { 64 }.explore(
                ExplorationMode.ENCOUNTER,
                com.game.tcg.core.engine.ExplorationContext(explorationCount = count)
            )
        }

        assertEquals(170, encountered.map { it.companionCardId }.toSet().size)
        assertEquals(CardCatalog.ALL.map { it.id }.toSet(), encountered.map { it.companionCardId }.toSet())
        assertTrue(encountered.all { it.title.startsWith("인연 조우 · ") })
    }
}
