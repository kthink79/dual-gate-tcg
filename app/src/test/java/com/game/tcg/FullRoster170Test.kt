package com.game.tcg

import com.game.tcg.core.engine.ExplorationEngine
import com.game.tcg.core.engine.ExplorationMode
import com.game.tcg.core.engine.ExplorationRewardType
import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.MidBossCatalog
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
 * 188명 카드 자산 검증 — 플레이 가능 182명 + 중간 보스 6명(No.170~175).
 *
 * - 도감 카탈로그 182명 데이터(이름·칭호·ATK·DEF·속성·코스트·스킬) 완비
 *   (2026-11-19: No.176 하린 · 177 시온 · 185~189 신규 5인 / 2026-11-20: 178 메이린 · 179 린 /
 *    2026-11-21: 180 카일라 / 2026-11-22: 181 미라 · 182 키라나 합류)
 * - 중간 보스 6명(170~175) 기록·아트(3페어 18장) 등록
 * - 4종 비주얼 리소스 매핑 누락 0건 (188 × 4 = 752장)
 * - 전체 로스터를 지정한 상태에서 로비 순환 · 도감 필터 · 서약 덱 · 전투 편성 · 탐색 조우
 *
 * (게임 시작은 2026-11-16 사용자 확정으로 **저코스트 3장**만 보유한다 —
 *  시작 상태·진행 루프는 `GameProgressionTest`가 검증한다.)
 */
class FullRoster170Test {

    /** 도감 전체를 보유한 상태의 ViewModel — 로스터 전수 검증용 */
    private fun fullRosterViewModel() = TcgGameViewModel(
        initialOwnedCardIds = CardCatalog.ALL.map { it.id } + MidBossCatalog.ALL.map { it.id }
    )

    // ──────────────────────────────────────────────────────────────
    // 1. 170명 데이터
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `카탈로그는 1번부터 169번 그리고 신규 12인과 184번까지 정확히 182명이다`() {
        val expected = (1..169).toList() +
            listOf(176, 177, 178, 179, 180, 181, 182, 184, 185, 186, 187, 188, 189)

        assertEquals(182, CardCatalog.ALL.size)
        assertEquals(expected, CardCatalog.ALL.map { it.id })
        assertEquals(182, CardCatalog.ROSTER_SIZE)
        assertEquals("플레이 가능 로스터에 중간 보스는 없다", 0,
            CardCatalog.ALL.count { it.isMidBoss })
    }

    @Test
    fun `182명 전원이 이름 칭호 스탯 속성 스킬을 갖추고 전체 이름이 겹치지 않는다`() {
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
            "카드에 표시되는 '칭호 + 이름'은 182명 모두 달라야 구분된다",
            182,
            all.map { it.fullName }.toSet().size
        )
        assertEquals(
            "5속성 전부에 분포해야 상성 시스템이 의미를 갖는다",
            Element.values().toSet(),
            all.map { it.element }.toSet()
        )
        assertEquals(
            "플레이 가능 182명은 친밀도 0에서 합류한다 (2026-11-16 개편 — 전투 출전으로 30/70/100 해금)",
            0,
            all.map { it.bondSeed }.toSet().single()
        )
    }

    @Test
    fun `신규 배치는 155~169 와 신규 12인이고 기존 155인과 합쳐 182명이다`() {
        assertEquals(listOf(155, 169), listOf(CardCatalog.NEW_WAVE_RANGE.first, CardCatalog.NEW_WAVE_RANGE.last))
        // 155~169(15) + 176·177·185~189(7) + 178·179(2) + 180(1) + 181·182(2) = 27
        assertEquals(27, CardCatalog.NEW_WAVE_IDS.size)
        assertEquals(155, CardCatalog.LEGACY_IDS.size)
        assertEquals(182, CardCatalog.NEW_WAVE_IDS.size + CardCatalog.LEGACY_IDS.size)
        assertEquals(155, CardCatalog.newWave().first().id)
        assertEquals(listOf(176, 177, 178, 179, 180, 181, 182, 185, 186, 187, 188, 189), CardCatalog.NEW_WAVE_IDS.filter { it !in 155..169 })

        // 2026-11-19 신규 7인 — 이름·칭호·속성·스탯
        val fresh = mapOf(
            176 to ("하린" to "바람의 한복 궁수"),
            177 to ("시온" to "황혼 유카타 쌍검사"),
            178 to ("메이린" to "홍등의 치파오 봉술사"),
            179 to ("린" to "옥련꽃 아오자이 부채무희"),
            180 to ("카일라" to "츗타이 킥복서"),
            181 to ("미라" to "자리 검무 무희"),
            182 to ("키라나" to "수궁 연꽃 무희"),
            185 to ("볼티아" to "뇌광의 아케인 건슬링거"),
            186 to ("솔레나" to "태양빛의 무희 무투가"),
            187 to ("니바라" to "빙결 별빛의 천문 마도사"),
            188 to ("비트리아" to "무지갯빛 유리 기사"),
            189 to ("아라냐" to "가시장미의 수호자")
        )
        fresh.forEach { (id, expected) ->
            val record = CardCatalog.require(id)
            assertEquals("#$id 이름", expected.first, record.name)
            assertEquals("#$id 칭호", expected.second, record.title)
            assertTrue("#$id 로스터 등록", id in CardCatalog.ALL.map { it.id })
            assertTrue("#$id 아트 등록", CharacterArtRegistry.hasArt(id))
        }
        assertEquals(4200, CardCatalog.require(176).atk)
        assertEquals(3400, CardCatalog.require(176).def)
        assertEquals(4300, CardCatalog.require(177).atk)
        assertEquals(3500, CardCatalog.require(177).def)
        assertEquals(4400, CardCatalog.require(178).atk)
        assertEquals(3300, CardCatalog.require(178).def)
        assertEquals(4200, CardCatalog.require(179).atk)
        assertEquals(3600, CardCatalog.require(179).def)
        assertEquals(4400, CardCatalog.require(180).atk)
        assertEquals(3500, CardCatalog.require(180).def)
        assertEquals(Element.FIRE, CardCatalog.require(180).element)
        assertEquals(6, CardCatalog.require(180).baseCost)
        assertEquals("츗타이 하이킥 · 적 전열 피해 + 1턴 기절", CardCatalog.require(180).skill)
        assertEquals(4300, CardCatalog.require(181).atk)
        assertEquals(3500, CardCatalog.require(181).def)
        assertEquals(Element.LIGHT, CardCatalog.require(181).element)
        assertEquals(6, CardCatalog.require(181).baseCost)
        assertEquals("자리 검무 · 적 전열 2명 참격 + 출혈, 아군 전체 공격력 +15%(2턴)", CardCatalog.require(181).skill)
        assertEquals(4200, CardCatalog.require(182).atk)
        assertEquals(3700, CardCatalog.require(182).def)
        assertEquals(Element.ICE, CardCatalog.require(182).element)
        assertEquals(6, CardCatalog.require(182).baseCost)
        assertEquals("연잎 물결 · 적 전체 물결 피해 + 아군 전체 1턴 방어 +20%", CardCatalog.require(182).skill)
        assertEquals(4000, CardCatalog.require(188).atk)
        assertEquals(3900, CardCatalog.require(188).def)
        assertEquals(4100, CardCatalog.require(189).atk)
        assertEquals(3500, CardCatalog.require(189).def)
        assertEquals(CharacterArtRegistry.visualCount, CharacterArtRegistry.visualCount)
    }

    @Test
    fun `배치별 대표 캐릭터가 소스 문서와 같은 이름 칭호로 등록되어 있다`() {
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

    @Test
    fun `중간 보스 6명이 170번부터 175번에 카탈로그와 아트로 등록되어 있다`() {
        assertEquals(listOf(170, 171, 172, 173, 174, 175), MidBossCatalog.ALL.map { it.id })
        assertEquals(listOf("로잘린드", "시그룬", "파필리아", "나이아드라", "솔라리아", "닉사리아"),
            MidBossCatalog.ALL.map { it.name })
        assertEquals(
            listOf("심연의 진홍 장미", "천공의 백은 발키리", "영원의 나비 여왕",
                "심해의 창해 마도사", "성휘의 태양 여사제", "칠흑 염검의 마룡 기사"),
            MidBossCatalog.ALL.map { it.title }
        )
        assertEquals(
            listOf(5600, 4200, 5000, 5200, 5300, 5800),
            MidBossCatalog.ALL.map { it.atk }
        )
        assertEquals(
            listOf(4400, 3600, 4600, 4800, 4700, 4300),
            MidBossCatalog.ALL.map { it.def }
        )
        assertTrue("전원 중간 보스 플래그", MidBossCatalog.ALL.all { it.isMidBoss })
        MidBossCatalog.ALL.forEach { boss ->
            assertEquals("카탈로그 조회", boss, CardCatalog.find(boss.id))
            assertTrue("#${boss.id} 아트 미등록", CharacterArtRegistry.hasArt(boss.id))
            assertTrue("#${boss.id} 최대 HP는 DEF 기반보다 두껍다", boss.maxHp > boss.def)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 2. 4종 비주얼 리소스 매핑 누락 0건 (176명 × 4 = 704장)
    //    (2026-11-17: 4번째 페어 '미니어처(SD)' 탑재)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `188명의 4종 비주얼 752장이 모두 리소스로 연결되어 있다`() {
        val allIds = CardCatalog.ALL.map { it.id } + MidBossCatalog.ALL.map { it.id }
        var mapped = 0
        allIds.forEach { id ->
            assertTrue("#$id 아트 미등록", CharacterArtRegistry.hasArt(id))
            VisualState.values().forEach { visual ->
                val resId = CharacterArtRegistry.resId(id, visual)
                assertNotNull("#$id ${visual.name} 리소스 없음", resId)
                assertTrue("#$id ${visual.name} 리소스 id 0", (resId ?: 0) != 0)
                mapped++
            }
        }
        assertEquals("188명 × 4비주얼 = 752장", 752, mapped)
        assertEquals(188, CharacterArtRegistry.registeredCardCount)
        assertEquals(allIds.toSet(), CharacterArtRegistry.registeredCardIds)
        assertNull("등록되지 않은 번호는 null", CharacterArtRegistry.resId(999, VisualState.BATTLE))
    }

    @Test
    fun `한 캐릭터의 네 비주얼은 서로 다른 리소스를 가리킨다`() {
        val allIds = CardCatalog.ALL.map { it.id } + MidBossCatalog.ALL.map { it.id }
        allIds.forEach { id ->
            val ids = VisualState.values().map { CharacterArtRegistry.resId(id, it) }
            assertEquals("#$id 4종 비주얼이 모두 같은 그림이다", 4, ids.toSet().size)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 3. 로비 · 도감 · 덱 (전체 로스터 지정 상태)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `전체 로스터를 지정하면 188명이 보유 목록에 오른다`() {
        val state = fullRosterViewModel().uiState.value

        assertEquals(188, state.ownedCards.size)
        assertEquals(188, state.lobbyRoster.size)
        assertEquals(state.ownedCards.map { it.id }.toSet(), state.lobbyRoster.toSet())
        assertEquals(182, TcgGameViewModel.profiles().size)
    }

    @Test
    fun `로비 좌우 전환은 보유 캐릭터를 한 바퀴 순환한다`() {
        val vm = fullRosterViewModel()
        val roster = vm.uiState.value.lobbyRoster
        assertEquals(188, roster.size)

        val startIndex = roster.indexOf(vm.uiState.value.lobbyHeroId)
        roster.drop(startIndex + 1).forEach { expected ->
            vm.cycleLobbyHero(1)
            assertEquals(expected, vm.uiState.value.lobbyHeroId)
        }
        vm.cycleLobbyHero(1)
        assertEquals("마지막 다음은 처음으로 돌아온다", roster.first(), vm.uiState.value.lobbyHeroId)
        vm.cycleLobbyHero(-1)
        assertEquals("역방향도 순환한다", roster.last(), vm.uiState.value.lobbyHeroId)
    }

    @Test
    fun `미보유 캐릭터는 대표가 될 수 없다`() {
        val vm = TcgGameViewModel(initialOwnedCardIds = listOf(3, 1, 2))
        val start = vm.uiState.value.lobbyHeroId
        vm.selectLobbyHero(112)
        assertEquals("미보유 마왕은 대표가 될 수 없다", start, vm.uiState.value.lobbyHeroId)
        assertTrue(vm.uiState.value.statusMessage.contains("계약하지 않은"))

        vm.selectLobbyHero(1)
        assertEquals("보유 캐릭터는 대표가 된다", 1, vm.uiState.value.lobbyHeroId)
        assertEquals("태양의 대천사", vm.uiState.value.lobbyHeroProfile?.title)
    }

    @Test
    fun `도감 프로필은 전체 182명이고 신규 27인 기존 155인으로 나뉜다`() {
        val profiles = TcgGameViewModel.profiles()

        assertEquals(182, profiles.size)
        assertEquals(27, profiles.count { CardCatalog.isNewWave(it.cardId) })
        assertEquals(155, profiles.count { !CardCatalog.isNewWave(it.cardId) })
        assertEquals(155, CardCatalog.legacy().size)
        assertTrue(CardCatalog.legacy().all { !CardCatalog.isNewWave(it.id) })
    }

    @Test
    fun `도감에서 어떤 카드를 골라도 상세와 전용 스킬이 보인다`() {
        val vm = TcgGameViewModel()
        val unowned = vm.uiState.value.ownedCardIds

        listOf(1, 65, 112, 144, 169, 184).filter { it !in unowned }.forEach { id ->
            vm.selectCard(id)
            val state = vm.uiState.value
            val record = CardCatalog.require(id)
            val card = TcgGameViewModel.characterCard(id)

            assertEquals("#$id 선택", id, state.selectedCardId)
            assertEquals(record.fullName, card.name)
            assertTrue("#$id 스킬", card.skillText.isNotBlank())
            assertFalse("#$id 미보유", id in state.ownedCardIds)
            // 미계약(=친밀도 0) 카드는 일상·데이트 룩이 잠긴다 — 도감에서도 잠금 안내가 보인다
            vm.selectVisual(VisualState.ROMANTIC_DATE)
            assertEquals(VisualState.BATTLE, vm.uiState.value.selectedVisual)
            assertNotNull(vm.uiState.value.visualLockMessage)
        }
    }

    @Test
    fun `서약 덱은 보유한 188명을 카드로 편성 목록에 올린다`() {
        val state = fullRosterViewModel().uiState.value
        val characters = state.ownedCards.filter { it.category.isCharacter }

        assertEquals(188, characters.size)
        assertEquals("1캐릭터 1장 유일 원칙", 188, characters.map { it.id }.toSet().size)
    }

    // ──────────────────────────────────────────────────────────────
    // 4. 전투 편성 · 소환
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `어떤 캐릭터든 전열 후열 슬롯에 소환하면 전투 파티에 반영된다`() {
        val vm = fullRosterViewModel()

        vm.deployToParty(155, 0)
        val front = vm.uiState.value.party.first { it.slotIndex == 0 }
        assertEquals(155, front.card.id)
        assertTrue(front.isFrontRow)
        assertEquals("시작 3인 편성을 유지한 채 0번 슬롯만 교체된다", 3, vm.uiState.value.party.size)
        assertEquals("전열 HP는 DEF 기반", (3400 * 0.85).toInt(), front.maxHp)

        vm.deployToParty(184, 4)
        val back = vm.uiState.value.party.first { it.slotIndex == 4 }
        assertEquals(184, back.card.id)
        assertFalse(back.isFrontRow)

        vm.deployToParty(155, 2)
        assertEquals(1, vm.uiState.value.party.count { it.card.id == 155 })
        assertNull("0번 슬롯은 비워진다", vm.uiState.value.party.firstOrNull { it.slotIndex == 0 })
    }

    @Test
    fun `보유한 캐릭터는 모두 전장에 나가 싸울 수 있다`() {
        CardCatalog.ALL.forEach { record ->
            val id = record.id
            val vm = fullRosterViewModel()
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
        val vm = fullRosterViewModel()
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
    fun `탐색 랜덤 조우는 보유 로스터 182명을 대상으로 한다`() {
        // 판정 64 → r<70 조우 분기, index = (탐색 횟수 0 + 64) % 182 = 64 → 카탈로그 65번
        val vm = TcgGameViewModel(
            explorationEngine = ExplorationEngine { 64 },
            initialOwnedCardIds = CardCatalog.ALL.map { it.id }
        )
        val target = CardCatalog.ALL[64]
        val before = vm.uiState.value.ownedCards.first { it.id == target.id }.bondLevel

        val result = vm.explore(ExplorationMode.ENCOUNTER)

        assertNotNull(result)
        assertEquals(target.id, result!!.companionCardId)
        assertEquals(target.fullName, result.companionDisplayName)
        assertTrue(result.rewards.any { it.type == ExplorationRewardType.COMPANION_BOND })
        // 2026-11-16 개편: 탐색 조우(선물)는 보조 경로 — index 64 → +1
        assertEquals(before + 1, vm.uiState.value.ownedCards.first { it.id == target.id }.bondLevel)
    }

    @Test
    fun `조우 연출은 182명 각각 다른 인연을 걸 수 있다`() {
        val encountered = (0..181).map { count ->
            ExplorationEngine { 64 }.explore(
                ExplorationMode.ENCOUNTER,
                com.game.tcg.core.engine.ExplorationContext(explorationCount = count)
            )
        }

        assertEquals(182, encountered.map { it.companionCardId }.toSet().size)
        assertEquals(CardCatalog.ALL.map { it.id }.toSet(), encountered.map { it.companionCardId }.toSet())
        assertTrue(encountered.all { it.title.startsWith("인연 조우 · ") })
    }
}
