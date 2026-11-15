package com.game.tcg

import com.game.tcg.core.engine.ExplorationEngine
import com.game.tcg.core.engine.ExplorationMode
import com.game.tcg.core.engine.ExplorationRewardType
import com.game.tcg.core.model.CardCatalog
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
 * 개성파 신규 30인(No.155~184) 탑재 검증.
 *
 * - 도감 카탈로그 30명 데이터(이름·칭호·ATK·DEF·속성) 완비
 * - 3단 비주얼 JPG 90장 리소스 매핑 누락 0건
 * - 로비 대표 캐릭터 선택 · 3단 비주얼 전환
 * - 도감 목록 · 서약 덱 · 전투 편성 · 탐색 조우 반영
 */
class NewWaveRosterTest {

    // ──────────────────────────────────────────────────────────────
    // 1. 30명 데이터
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `개성파 30인이 155번부터 184번까지 빈틈없이 등록되어 있다`() {
        val wave = CardCatalog.newWave()

        assertEquals(30, wave.size)
        assertEquals((155..184).toList(), wave.map { it.id })
        assertEquals((155..184).toList(), CardCatalog.NEW_WAVE_IDS)
    }

    @Test
    fun `30명 전원이 이름 칭호 스탯 속성을 갖추고 서로 겹치지 않는다`() {
        val wave = CardCatalog.newWave()

        wave.forEach { record ->
            assertTrue("#${record.id} 이름 누락", record.name.isNotBlank())
            assertTrue("#${record.id} 칭호 누락", record.title.isNotBlank())
            assertTrue("#${record.id} 스킬 누락", record.skill.isNotBlank())
            assertTrue("#${record.id} ATK ${record.atk}", record.atk in 2000..5000)
            assertTrue("#${record.id} DEF ${record.def}", record.def in 2000..5000)
            assertTrue("#${record.id} 코스트 ${record.baseCost}", record.baseCost in 3..6)
            assertTrue("#${record.id} 최대 HP", record.maxHp > 0)
            assertTrue("#${record.id} 호감도 시드", record.bondSeed in 0..100)
        }

        assertEquals("이름이 중복되면 도감에서 구분되지 않는다", 30, wave.map { it.name }.toSet().size)
        assertEquals("칭호도 중복 금지", 30, wave.map { it.title }.toSet().size)
        assertEquals(
            "5속성 전부에 분포해야 상성 시스템이 의미를 갖는다",
            setOf(
                com.game.tcg.core.model.Element.FIRE,
                com.game.tcg.core.model.Element.WIND,
                com.game.tcg.core.model.Element.ICE,
                com.game.tcg.core.model.Element.LIGHT,
                com.game.tcg.core.model.Element.DARK
            ),
            wave.map { it.element }.toSet()
        )
    }

    @Test
    fun `카탈로그 전체는 기존 4인과 개성파 30인을 합친 34인이다`() {
        assertEquals(34, CardCatalog.ALL.size)
        assertEquals(34, TcgGameViewModel.profiles().size)
        assertEquals(
            listOf(1, 2, 3, 112),
            CardCatalog.ALL.filter { !CardCatalog.isNewWave(it.id) }.map { it.id }
        )
        // 등록되지 않은 번호는 게임을 깨뜨리지 않는 대체 카드로 복원된다
        assertEquals("미등록 카드 #999", CardCatalog.require(999).name)
    }

    // ──────────────────────────────────────────────────────────────
    // 2. JPG 90장 리소스 매핑 누락 0건
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `개성파 30명의 3단 비주얼 90장이 모두 리소스로 연결되어 있다`() {
        var mapped = 0
        CardCatalog.newWave().forEach { record ->
            assertTrue("#${record.id} 아트 미등록", CharacterArtRegistry.hasArt(record.id))
            VisualState.values().forEach { visual ->
                val resId = CharacterArtRegistry.resId(record.id, visual)
                assertNotNull("#${record.id} ${visual.name} 리소스 없음", resId)
                assertTrue("#${record.id} ${visual.name} 리소스 id 0", (resId ?: 0) != 0)
                mapped++
            }
        }
        assertEquals("30명 × 3비주얼 = 90장", 90, mapped)
    }

    @Test
    fun `한 캐릭터의 세 비주얼은 서로 다른 리소스를 가리킨다`() {
        CardCatalog.newWave().forEach { record ->
            val ids = VisualState.values().map { CharacterArtRegistry.resId(record.id, it) }
            assertEquals("#${record.id} 3단 비주얼이 모두 같은 그림이다", 3, ids.toSet().size)
        }
    }

    @Test
    fun `기존 4인도 아트 매핑이 유지된다`() {
        listOf(1, 2, 3, 112).forEach { id ->
            assertTrue("#$id 기존 아트 매핑 유지", CharacterArtRegistry.hasArt(id))
        }
        assertEquals(34, CharacterArtRegistry.registeredCardCount)
        assertNull("등록되지 않은 번호는 null", CharacterArtRegistry.resId(999, VisualState.BATTLE))
    }

    // ──────────────────────────────────────────────────────────────
    // 3. 로비 · 도감 · 덱
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `게임을 시작하면 개성파 30인을 모두 보유하고 도감에 노출된다`() {
        val state = TcgGameViewModel().uiState.value

        assertEquals(33, state.ownedCards.size) // 스타팅 3인 + 개성파 30인
        CardCatalog.NEW_WAVE_IDS.forEach { id ->
            assertTrue("#$id 보유 목록에 없음", state.ownedCards.any { it.id == id })
        }
        assertEquals("로비 로스터는 스타팅 3인 + 개성파 30인", 33, state.lobbyRoster.size)
        assertTrue(state.lobbyRoster.containsAll(CardCatalog.NEW_WAVE_IDS))
    }

    @Test
    fun `개성파를 대표 캐릭터로 세우면 로비에 이름 칭호 3단 비주얼이 반영된다`() {
        val vm = TcgGameViewModel()

        vm.selectLobbyHero(160)
        val state = vm.uiState.value
        assertEquals(160, state.lobbyHeroId)
        assertEquals("브리오나", state.lobbyHeroProfile?.displayName)
        assertEquals("맨주먹의 격투가", state.lobbyHeroProfile?.title)
        assertEquals("맨주먹의 격투가 브리오나", state.lobbyHero.name)

        // 개성파는 호감도 시드 50으로 합류하므로 데이트 룩이 처음부터 열려 있다
        assertTrue(state.isLobbyDateLookUnlocked)
        vm.selectLobbyVisual(VisualState.CASUAL_CUTE)
        assertEquals(VisualState.CASUAL_CUTE, vm.uiState.value.lobbyVisual)
        vm.selectLobbyVisual(VisualState.ROMANTIC_DATE)
        assertEquals(
            "개성파 30인은 전투·일상·데이트 세 비주얼을 모두 볼 수 있어야 한다",
            VisualState.ROMANTIC_DATE,
            vm.uiState.value.lobbyVisual
        )
    }

    @Test
    fun `로비 좌우 전환은 개성파 30인까지 순환한다`() {
        val vm = TcgGameViewModel()
        val roster = vm.uiState.value.lobbyRoster

        assertEquals(listOf(3, 1, 2), roster.take(3))
        assertEquals(155, roster[3])

        vm.selectLobbyHero(184)
        vm.cycleLobbyHero(1)
        assertEquals("마지막 개성파 다음은 스타팅 첫 캐릭터", 3, vm.uiState.value.lobbyHeroId)

        vm.cycleLobbyHero(-1)
        assertEquals(184, vm.uiState.value.lobbyHeroId)
    }

    @Test
    fun `도감에서 개성파 카드를 선택하면 상세와 전용 스킬이 보인다`() {
        val vm = TcgGameViewModel()
        vm.selectCard(177)
        val state = vm.uiState.value
        val card = state.ownedCards.first { it.id == 177 }

        assertEquals("은혜", state.ownedCards.first { it.id == 177 }.name.split(" ").last())
        assertEquals("맥을 짚는 약초사 은혜", card.name)
        assertTrue("전용 스킬 문구", card.skillText.contains("재생"))

        vm.selectVisual(VisualState.ROMANTIC_DATE)
        assertEquals(VisualState.ROMANTIC_DATE, vm.uiState.value.selectedVisual)
    }

    @Test
    fun `서약 덱은 개성파 30인을 모두 카드로 편성 목록에 올린다`() {
        val state = TcgGameViewModel().uiState.value
        val characters = state.ownedCards.filter { it.category.isCharacter }

        assertEquals(33, characters.size)
        assertEquals("1캐릭터 1장 유일 원칙", 33, characters.map { it.id }.toSet().size)
    }

    // ──────────────────────────────────────────────────────────────
    // 4. 전투 편성 · 소환
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `개성파를 전열 후열 슬롯에 소환하면 전투 파티에 반영된다`() {
        val vm = TcgGameViewModel()

        vm.deployToParty(155, 0)
        val front = vm.uiState.value.party.first { it.slotIndex == 0 }
        assertEquals(155, front.card.id)
        assertTrue(front.isFrontRow)
        assertEquals("3인 편성을 유지한 채 0번 슬롯만 교체된다", 3, vm.uiState.value.party.size)
        assertEquals("전열 HP는 DEF 기반", (3600 * 0.85).toInt(), front.maxHp)

        vm.deployToParty(184, 4)
        val back = vm.uiState.value.party.first { it.slotIndex == 4 }
        assertEquals(184, back.card.id)
        assertFalse(back.isFrontRow)

        // 같은 캐릭터를 다른 슬롯에 다시 소환하면 이전 슬롯에서 빠진다
        vm.deployToParty(155, 2)
        assertEquals(1, vm.uiState.value.party.count { it.card.id == 155 })
        assertNull(
            "0번 슬롯은 비워진다",
            vm.uiState.value.party.firstOrNull { it.slotIndex == 0 }
        )
    }

    @Test
    fun `개성파 30인 전원이 전장에 나가 싸울 수 있다`() {
        CardCatalog.NEW_WAVE_IDS.forEach { id ->
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

    @Test
    fun `미보유 캐릭터는 편성할 수 없다`() {
        val vm = TcgGameViewModel()
        vm.deployToParty(112, 0)

        assertTrue(vm.uiState.value.party.none { it.card.id == 112 })
        assertTrue(vm.uiState.value.statusMessage.contains("계약하지 않은"))
    }

    // ──────────────────────────────────────────────────────────────
    // 5. 탐색 조우
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `탐색 랜덤 조우에서 개성파 캐릭터를 만나면 그 캐릭터의 호감도가 오른다`() {
        // 64 → r<80 분기, 짝수라 개성파 조우 / roster[64 % 30] = 159 로잘린 / 호감도 +15
        val vm = TcgGameViewModel(explorationEngine = ExplorationEngine { 64 })
        val before = vm.uiState.value.ownedCards.first { it.id == 159 }.bondLevel

        val result = vm.explore(ExplorationMode.ENCOUNTER)

        assertNotNull(result)
        assertEquals(159, result!!.companionCardId)
        assertEquals("벨벳 속삭임의 정보상 로잘린", result.companionDisplayName)
        assertTrue(result.rewards.any { it.type == ExplorationRewardType.COMPANION_BOND })
        assertEquals(before + 15, vm.uiState.value.ownedCards.first { it.id == 159 }.bondLevel)
    }

    @Test
    fun `개성파 조우 연출은 캐릭터마다 다른 문구를 쓴다`() {
        // 60~69 판정 = 개성파 조우, 대상은 (탐색 횟수 + 판정값) % 30 → 30회 탐색으로 30명 전원
        val titles = (0..29).map { count ->
            ExplorationEngine { 64 }.explore(
                ExplorationMode.ENCOUNTER,
                com.game.tcg.core.engine.ExplorationContext(explorationCount = count)
            ).title
        }
        assertEquals("30명 각각의 조우 제목", 30, titles.toSet().size)
        assertTrue(titles.all { it.startsWith("개성파 조우 · ") })
    }
}
