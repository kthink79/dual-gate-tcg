package com.game.tcg.ui

import androidx.lifecycle.ViewModel
import com.game.tcg.core.engine.AffinityRules
import com.game.tcg.core.engine.BattleEngine
import com.game.tcg.core.engine.ChapterTwoScript
import com.game.tcg.core.engine.DualWorldEngine
import com.game.tcg.core.engine.ExplorationContext
import com.game.tcg.core.engine.ExplorationEngine
import com.game.tcg.core.engine.ExplorationMode
import com.game.tcg.core.engine.ExplorationResult
import com.game.tcg.core.engine.ExplorationRewardType
import com.game.tcg.core.engine.IBattleEngine
import com.game.tcg.core.engine.IExplorationEngine
import com.game.tcg.core.engine.IDualWorldEngine
import com.game.tcg.core.engine.IManaEngine
import com.game.tcg.core.engine.IntroScript
import com.game.tcg.core.engine.IntroStoryEngine
import com.game.tcg.core.engine.IntroStoryState
import com.game.tcg.core.engine.IMemoryRecallEngine
import com.game.tcg.core.engine.IPairUltimateEngine
import com.game.tcg.core.engine.IRoguelikeLifecycleEngine
import com.game.tcg.core.engine.ISynthesisEngine
import com.game.tcg.core.engine.ITacticalRetreatEngine
import com.game.tcg.core.engine.ITemptationEngine
import com.game.tcg.core.engine.ITrustRiftEngine
import com.game.tcg.core.engine.ManaEngine
import com.game.tcg.core.engine.MemoryRecallEngine
import com.game.tcg.core.engine.PairUltimateEngine
import com.game.tcg.core.engine.RealWorldActivity
import com.game.tcg.core.engine.RoguelikeLifecycleEngine
import com.game.tcg.core.engine.SynthesisEngine
import com.game.tcg.core.engine.StoryArcNode
import com.game.tcg.core.engine.TacticalRetreatEngine
import com.game.tcg.core.engine.TemptationEngine
import com.game.tcg.core.engine.TrustRiftEngine
import com.game.tcg.core.engine.DeckAutoBuilder
import com.game.tcg.core.engine.AdvancedSynthesisEngine
import com.game.tcg.core.engine.EmergencySupplyEngine
import com.game.tcg.core.engine.SupplyOffer
import com.game.tcg.core.engine.CardUpgradeEngine
import com.game.tcg.core.engine.CraftKind
import com.game.tcg.core.engine.CraftResult
import com.game.tcg.core.engine.CraftTarget
import com.game.tcg.core.engine.EquipResult
import com.game.tcg.core.engine.EquipmentEngine
import com.game.tcg.core.engine.ICardUpgradeEngine
import com.game.tcg.core.engine.IPackOpeningEngine
import com.game.tcg.core.engine.IPackShopEngine
import com.game.tcg.core.engine.PackOpeningEngine
import com.game.tcg.core.engine.PackOpeningPhase
import com.game.tcg.core.engine.PackOpeningState
import com.game.tcg.core.engine.PackShopEngine
import com.game.tcg.core.engine.StageClearOutcome
import com.game.tcg.core.engine.StageEngine
import com.game.tcg.core.engine.SummonEngine
import com.game.tcg.core.engine.SummonResult
import com.game.tcg.core.engine.TranscendResult
import com.game.tcg.core.engine.UpgradeResult
import com.game.tcg.core.model.CardRarity
import com.game.tcg.core.model.EquipmentCatalog
import com.game.tcg.core.model.EquipmentItem
import com.game.tcg.core.model.PackCatalog
import com.game.tcg.core.model.PackDefinition
import com.game.tcg.core.model.PackOpenResult
import com.game.tcg.core.model.ShardWallet
import com.game.tcg.core.model.StageCatalog
import com.game.tcg.core.model.StageDefinition
import com.game.tcg.core.model.StartingRoster
import kotlin.random.Random
import com.game.tcg.core.model.AcquisitionResult
import com.game.tcg.audio.AudioScene
import com.game.tcg.audio.AudioSceneMapper
import com.game.tcg.audio.AudioSettings
import com.game.tcg.audio.SfxEffect
import com.game.tcg.core.model.BattleCardHp
import com.game.tcg.core.model.BattleLineup
import com.game.tcg.core.model.BattleRetreatInfo
import com.game.tcg.core.model.BattleSlot
import com.game.tcg.core.model.BattleState
import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.CombatPenalty
import com.game.tcg.core.model.DeckRules
import com.game.tcg.core.model.DemonLordRegistry
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.EmergencyRetreatResult
import com.game.tcg.core.model.ManaState
import com.game.tcg.core.model.MemoryFragmentSource
import com.game.tcg.core.model.MemoryRecallDialogue
import com.game.tcg.core.model.MemoryRecallResult
import com.game.tcg.core.model.PairUltimateReady
import com.game.tcg.core.model.PairUltimateResult
import com.game.tcg.core.model.PlayerProgress
import com.game.tcg.core.model.ReincarnatedCard
import com.game.tcg.core.model.RetreatResult
import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.TemptationState
import com.game.tcg.core.model.TrustRiftResolution
import com.game.tcg.core.model.UndercoverDialogueOption
import com.game.tcg.core.model.VisualState
import com.game.tcg.persistence.SaveCodec
import com.game.tcg.persistence.SaveStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ============================================================================
// 샘플 시나리오·이벤트 데이터 (프롤로그 ~ 챕터 1)
// ============================================================================

/**
 * 스토리 카드 한 장 (진행 바의 단계)
 */
data class StoryStep(
    val chapter: String,
    val title: String,
    val body: String,
    val objective: String
)

object StoryScript {
    val STEPS: List<StoryStep> = listOf(
        StoryStep(
            chapter = "프롤로그",
            title = "길에서 주운 봉인 카드팩",
            body = "퇴근길 골목, 비에 젖은 봉인 카드팩 하나가 발치에 떨어져 있었다. " +
                    "봉인을 뜯자 카드에서 흘러나온 빛이 손등의 문양을 태워 새겼다. " +
                    "\"계약자가 확인되었습니다. 이계의 문이 열립니다.\"",
            objective = "봉인 카드팩 개봉 · 계약자 등록"
        ),
        StoryStep(
            chapter = "제1장 · 1화",
            title = "스타팅 아우렐리아 계약",
            body = "성기사 아우렐리아가 카드에서 걸어 나와 무릎을 꿇었다. " +
                    "\"당신의 그림자에 제 검을 맡기겠습니다. 대신, 이 세계의 황혼을 막아 주십시오.\" " +
                    "전열 방벽의 서약이 체결되었다.",
            objective = "아우렐리아 계약 · 전열 방벽 해금"
        ),
        StoryStep(
            chapter = "제1장 · 2화",
            title = "이계의 문 개방",
            body = "지하철 4번 출구 유리벽에 이계의 문이 반사되어 열렸다. " +
                    "문 너머에서 차원의 틈새로 흘러든 마수들이 도시의 그림자를 갉아먹고 있었다. " +
                    "첫 원정이 시작된다.",
            objective = "이계 전투 탭에서 첫 원정 전투 수행"
        ),
        StoryStep(
            chapter = "제1장 · 3화",
            title = "비 오는 밤 카페의 낯선 소녀 '릴리'",
            body = "비를 피해 들어간 카페에서 은발의 소녀가 마주 앉았다. " +
                    "\"이런 비 오는 밤엔 혼자 차를 마시기엔 너무 쓸쓸하지 않아?\" " +
                    "그녀의 이름은 릴리. 어딘가 심연의 향이 났다.",
            objective = "일상 잠입 이벤트 A 수행 · 선택에 따라 [홀림] 스택 발생"
        ),
        StoryStep(
            chapter = "제1장 · 4화",
            title = "이계 균열 · 차원 틈새의 마수",
            body = "매탄동 아파트 단지 상공에 검은 균열이 갈라졌다. " +
                    "틈새에서 흘러나온 마수가 옥상 발전기를 집어삼켰다. " +
                    "교전과 정찰 중 하나를 택해야 한다.",
            objective = "이벤트 B 수행 · 마수 토벌 또는 정찰"
        ),
        StoryStep(
            chapter = "제1장 · 5화",
            title = "가면이 벗겨진 밤 — 마왕 릴리스",
            body = "마수를 쓰러뜨린 자리에 서 있던 것은 릴리였다. " +
                    "\"차는 잘 마셨지? 다음엔 심연에서 보자.\" " +
                    "그녀의 실루엣이 황혼과 욕망의 마왕 릴리스로 바뀌며 사라졌다. " +
                    "도감의 No.112가 희미하게 빛난다.",
            objective = "도감에서 No.112를 응시 / 마왕전 준비"
        )
    )

    val EVENTS: List<StoryEvent> = listOf(
        StoryEvent(
            id = "EVENT_A",
            badge = "[일상 잠입]",
            title = "비 오는 밤 카페의 낯선 소녀 '릴리'",
            scene = "창밖으로 빗줄기가 흐른다. 릴리는 두 잔째 밀크티를 비우고, 손끝으로 " +
                    "당신의 카드 봉인 문양을 살며시 짚는다. \"이 문양… 나도 아는데.\" " +
                    "심연의 향이 아주 옅게, 그러나 분명하게 피어올랐다.",
            options = listOf(
                StoryChoice(
                    id = "A_TALK",
                    label = "차를 마시며 대화를 나눈다",
                    hint = "유혹 스택 +1 · 금화 유출 · 마왕전 디버프 경고",
                    isTempted = true
                ),
                StoryChoice(
                    id = "A_PURGE",
                    label = "경계하고 성수로 정화한다",
                    hint = "성수 1 소모 · 방어 보너스 +10%",
                    isTempted = false
                )
            )
        ),
        StoryEvent(
            id = "EVENT_B",
            badge = "[이계 균열]",
            title = "차원 틈새의 마수 출현",
            scene = "균열에서 비늘 달린 앞발이 튀어나온다. 마수는 옥상의 변압기를 삼키며 " +
                    "몸집을 두 배로 불렸다. 도시 전력망이 붉게 깜빡인다. " +
                    "정면 교전인가, 배후 정찰인가.",
            options = listOf(
                StoryChoice(
                    id = "B_FIGHT",
                    label = "즉시 전투 진입",
                    hint = "이계 전투 탭에서 마수와 교전 · 승리 시 금화·보석",
                    isTempted = false
                ),
                StoryChoice(
                    id = "B_SCOUT",
                    label = "정찰 선택",
                    hint = "금화 -200 · 다음 전투 아군 피해 +15% (급소 파악)",
                    isTempted = false
                )
            )
        )
    )

    fun eventById(id: String): StoryEvent = EVENTS.first { it.id == id }
}

data class StoryEvent(
    val id: String,
    val badge: String,
    val title: String,
    val scene: String,
    val options: List<StoryChoice>
)

data class StoryChoice(
    val id: String,
    val label: String,
    val hint: String,
    val isTempted: Boolean
)

// ============================================================================
// 전장 파티 모델
// ============================================================================

/**
 * 출전 덱 슬롯(전열 3 + 후열·대기 최대 9 = 12칸)에 배치된 파티원 (전장 HP 별도 추적)
 * slotIndex 0~2 = 전열, 3 이상 = 후열·대기.
 */
data class PartyMember(
    val card: TcgCard,
    val slotIndex: Int,
    val isFrontRow: Boolean,
    val hp: Int,
    val maxHp: Int,
    val memoryAffinity: Int = 0
) {
    val isAlive: Boolean get() = hp > 0
    val hpRatio: Float get() = if (maxHp <= 0) 0f else (hp.toFloat() / maxHp).coerceIn(0f, 1f)
    val isNearDeath: Boolean get() = isAlive && hpRatio <= 0.15f
}

/**
 * 도감 카드 1장의 표시용 프로필
 */
data class CharacterProfile(
    val cardId: Int,
    val displayName: String,
    val title: String,
    val element: Element,
    val isDemonLord: Boolean = false,
    val disguiseName: String? = null
)

// ============================================================================
// 메인 로비 내비게이션 — 캐릭터 중심 로비 + 좌·우측 측면 서브메뉴
// ============================================================================

/**
 * 메인 화면의 좌·우측 측면 레일 구분
 */
enum class SideRail {
    NONE,   // 로비 본체
    LEFT,   // 이세계 · 모험
    RIGHT   // 현실 · 관리
}

/**
 * 메인 화면에서 열리는 기능 화면 — 좌·우측 측면 서브메뉴 항목과 1:1 대응
 */
enum class LobbyDestination(
    val label: String,
    val icon: String,
    val tagline: String,
    val rail: SideRail
) {
    LOBBY("메인 로비", "🏛", "대표 캐릭터가 당신을 맞이합니다.", SideRail.NONE),

    // ── 좌측 · 이세계 · 모험 ──
    SCENARIO("이세계 접속", "📖", "차원의 문이 열리고 이야기가 이어집니다.", SideRail.LEFT),
    STAGE("스테이지", "🗺", "6챕터 30스테이지 캠페인 — 보상과 보스 관문.", SideRail.LEFT),
    BATTLE("이계 전투", "⚔", "출전 덱 9~12장 · 전열 3 전술 결전.", SideRail.LEFT),
    EXPLORATION("탐색", "🧭", "차원 틈새 탐색 · 기억 파편 수색 · 랜덤 조우.", SideRail.LEFT),

    // ── 우측 · 현실 · 관리 ──
    REAL_WORLD("현실 소환", "🏙", "현실에서 파트너를 부르고 교감합니다.", SideRail.RIGHT),
    SHOP("카드팩", "🛒", "카드팩 구매·개봉 → 신규 동료와 조각.", SideRail.RIGHT),
    GROWTH("성장", "⚒", "카드 강화 · 조각 연성 · 장비 장착.", SideRail.RIGHT),
    COLLECTION("캐릭터 관리", "🃏", "도감 · 합성 · 환생 재획득 · 대표 캐릭터 설정.", SideRail.RIGHT),
    DECK("서약·덱", "📜", "9~12장 출전 덱 편성 · 자동 편성 · 마왕 정화.", SideRail.RIGHT);

    companion object {
        /** 좌측 레일에 도킹되는 서브메뉴 (이세계·모험) */
        val LEFT_ITEMS: List<LobbyDestination> = listOf(SCENARIO, STAGE, BATTLE, EXPLORATION)

        /** 우측 레일에 도킹되는 서브메뉴 (현실·관리) */
        val RIGHT_ITEMS: List<LobbyDestination> = listOf(REAL_WORLD, SHOP, GROWTH, COLLECTION, DECK)
    }
}

/**
 * 메인 로비 상태 스트립의 경보 — 누르면 해당 화면으로 바로 이동
 */
data class LobbyAlert(
    val label: String,
    val destination: LobbyDestination,
    val isUrgent: Boolean = false
)

// ============================================================================
// UI 상태
// ============================================================================

data class TcgGameState(
    // ── 공통 자원 ──
    val gold: Int = 1200,
    val gems: Int = 30,
    val holyWater: Int = 3,
    val junkCards: Int = 6,
    val statusMessage: String = "듀얼 게이트 — 프롤로그가 시작됩니다.",
    // ── 시나리오 ──
    val storyStepIndex: Int = 0,
    val storyReadLog: List<String> = listOf("[프롤로그] 봉인 카드팩을 주웠다. 계약자 등록 완료."),
    val eventAChoice: String? = null,
    val eventAResult: String? = null,
    val eventBChoice: String? = null,
    val eventBResult: String? = null,
    val lilyFamiliarity: Int = 0,
    val defenseBonusPercent: Int = 0,
    val intelBonusPercent: Int = 0,
    // ── 유혹(홀림) ──
    val temptationState: TemptationState = TemptationState(),
    val combatPenalty: CombatPenalty = CombatPenalty(
        totalStacks = 0,
        attackReductionPercent = 0,
        damageTakenIncreasePercent = 0,
        criticalReductionPercent = 0,
        extraManaCost = 0,
        isCharmed = false,
        debuffDescription = "[정신 맑음] 유혹에 흔들리지 않아 전투 페널티가 없습니다."
    ),
    val gazeMessage: String? = null,
    // ── 전투 ──
    val party: List<PartyMember> = emptyList(),
    val battleReserve: List<TcgCard> = emptyList(),
    val enemyName: String = "차원 틈새의 마수",
    val enemyElement: Element = Element.DARK,
    val enemyHp: Int = 12000,
    val enemyMaxHp: Int = 12000,
    val enemyAtk: Int = 900,
    val isDemonLordBattle: Boolean = false,
    val turn: Int = 1,
    val mana: ManaState = ManaState(1, 1),
    val battleLog: List<String> = listOf("[원정 준비] 출전 덱 편성 완료. 턴 1 시작."),
    val retreatInfo: BattleRetreatInfo = BattleRetreatInfo(),
    val lastRetreatMessage: String? = null,
    val reincarnationPool: List<ReincarnatedCard> = emptyList(),
    val battleVictories: Int = 0,
    val lootGold: Int = 0,
    val lootGems: Int = 0,
    // ── 비주얼 전투 연출 (데미지 팝업 · 공격 포커스) ──
    val lastEnemyHitDamage: Int = 0,
    val lastEnemyHitTick: Int = 0,
    val lastAttackerSlot: Int = -1,
    val lastAllyHitDamage: Int = 0,
    val lastAllyHitSlot: Int = -1,
    val lastAllyHitTick: Int = 0,
    /**
     * 🤖 자동 전투 — 전투에 들어가면 기본 ON (2026-11-16 사용자 요청).
     * 화면의 `🤖 자동 전투 ON/OFF` 토글이 언제든 끄고 수동 조작으로 돌린다.
     */
    val isAutoBattle: Boolean = true,
    /** ⏩ 전투 배속 — 컷인·타격 이펙트·자동 스텝 시간을 함께 나눈다 */
    val battleSpeed: BattleSpeed = BattleSpeed.X1,
    /** ⚔️ 공격 1회의 컷인 스냅샷 (tick 이 바뀌면 화면이 컷인을 다시 재생) */
    val lastCutIn: BattleCutIn? = null,
    val pendingLootGold: Int = 300,
    val pendingLootGems: Int = 5,
    val lastAcquisition: String? = null,
    // ── 도감·서약 ──
    val ownedCards: List<TcgCard> = emptyList(),
    val selectedCardId: Int = 3,
    val selectedVisual: VisualState = VisualState.BATTLE,
    val synthesisMessage: String? = null,
    /** 도감에서 잠긴 비주얼(일상 30 · 데이트 70)을 누를 때 띄우는 안내 */
    val visualLockMessage: String? = null,
    // ── 현실 교감 ──
    val partnerBond: Int = 0,
    val partnerVisual: VisualState = VisualState.BATTLE,
    val realWorldLog: List<String> = listOf("[현실 교감] 파트너 대기 중. 친밀도는 전투 출전으로 올라갑니다."),
    val dateLookUnlocked: Boolean = false,
    val ultimateUnlocked: Boolean = false,
    // ── 「기억의 잔향」: 환생 재획득 후 기억 초기화·파편 회상 ──
    val memoryRecallMessage: String? = null,
    val memoryRecallDialogue: MemoryRecallDialogue? = null,
    // ── 「신뢰의 균열」: 현실·이세계 교차 이벤트 ──
    val trustRiftChoice: String? = null,
    val trustRiftMessage: String? = null,
    val frontRowDefensePenaltyPercent: Int = 0,
    // ── 「페어 얼티밋 오의」 컷인 ──
    val pairUltimateUsedThisBattle: Boolean = false,
    val pairUltimateBanner: String? = null,
    val pairUltimateBannerSubtitle: String? = null,
    val pairUltimateCount: Int = 0,
    // ── 제2장 프롤로그: 제1군단장 「그림자 기사」 전초전 ──
    val chapter2PreludeOpened: Boolean = false,
    val chapter2Message: String? = null,
    val isChapter2BossBattle: Boolean = false,
    // ── 플레이어 레벨 · 덱 해금 (2026-11-16 사용자 확정: 기본 9장 → 최대 12장) ──
    val playerExp: Int = 0,
    // ── 메인 루프: 스테이지 캠페인 (6챕터 × 5스테이지) ──
    val stageCleared: Set<Int> = emptySet(),
    val selectedStage: Int = 1,
    /** 지금 VS 전투 화면에서 싸우고 있는 스테이지 번호 (null = 자유 원정·마왕전) */
    val activeStage: Int? = null,
    val lastStageOutcome: StageClearOutcome? = null,
    val stageMessage: String? = null,
    // ── 소프트락 안전장치: 무료 긴급 보급 (2026-11-16 QA) ──
    val supplyClaims: Int = 0,
    val supplyMessage: String? = null,
    // ── 재화: 조각 · 카드팩 티켓 · 장비 소지품 ──
    val shards: ShardWallet = ShardWallet(),
    val packTickets: Int = 0,
    val equipment: List<EquipmentItem> = emptyList(),
    val lastPackOpen: PackOpenResult? = null,
    /** 카드팩 개봉 연출 단계 (봉인 카드팩 → 카드 1장씩 → 전체 그리드) */
    val packOpening: PackOpeningState = PackOpeningState(),
    val shopMessage: String? = null,
    // ── 성장: 강화(업그레이드) · 연성(합성) ──
    val growthCardId: Int = 0,
    val growthMessage: String? = null,
    val lastCraft: CraftResult? = null,
    // ── 현실 소환 ──
    val summonCardId: Int = 0,
    val summonBuffPercent: Int = 0,
    val summonBuffBattles: Int = 0,
    val lastSummon: SummonResult? = null,
    val summonCount: Int = 0,
    val summonMessage: String? = null,
    // ── 도입부 스토리(4장면 컷신) ──
    val introStory: IntroStoryState = IntroStoryState(),
    // ── 메인 로비 (캐릭터 중심) · 탐색 ──
    /** 로비 「📊 정보」 패널이 열렸는지 — 상단 1줄 요약만 남기고 나머지 정보를 여기에 모은다 */
    val isInfoPanelOpen: Boolean = false,
    val currentScreen: LobbyDestination = LobbyDestination.LOBBY,
    val lobbyHeroId: Int = TcgGameViewModel.DEFAULT_LOBBY_HERO_ID,
    val lobbyVisual: VisualState = VisualState.BATTLE,
    /**
     * 캐릭터별로 마지막에 고른 복장 — 2026-11-16 사용자 요청
     * 「메인 화면에서 일상이나 데이트 복장을 선택한 캐릭터는 유지가 되게 해줘」.
     *
     * 대표 캐릭터를 바꿔도 각 캐릭터가 골라 둔 복장이 그대로 돌아오고,
     * 이 값은 [com.game.tcg.persistence.GameSave]에 함께 저장되어 앱을 껐다 켜도 유지된다.
     * (해금되지 않은 복장은 기존 규칙대로 친밀도 단계까지만 허용)
     */
    val lobbyOutfits: Map<Int, VisualState> = emptyMap(),
    val gateEnergy: Int = TcgGameViewModel.GATE_ENERGY_START,
    val explorationCount: Int = 0,
    val lastExploration: ExplorationResult? = null,
    val explorationLog: List<String> = listOf("[탐색] 차원 균열 지도가 펼쳐졌습니다. 세 갈래 길이 빛납니다."),
    // ── 오디오 (BGM·SFX 설정 — 화면의 🔊/🔇 토글이 바꾼다) ──
    val audioSettings: AudioSettings = AudioSettings(),
    /** 이 상태에서 흘러야 할 BGM 장면 (전투 개시·정산 때 지정, 기본은 로비) */
    val audioScene: AudioScene = AudioScene.LOBBY
) {
    /** 생존 아군 수 (오의 조건 판정용) */
    val aliveSurvivorCount: Int get() = party.count { it.isAlive }

    // ── 플레이어 레벨 · 덱 해금 파생 상태 ──

    /** 경험치로 계산한 플레이어 레벨 */
    val playerLevel: Int get() = PlayerProgress.levelForExp(playerExp)

    /** 현재 레벨에서 편성 가능한 덱 크기 (9 ~ 12) */
    val unlockedDeckSize: Int get() = DeckRules.deckSizeForLevel(playerLevel)

    /** 아직 잠긴 슬롯 수 (최대 12칸 기준) */
    val lockedDeckSlots: Int get() = DeckRules.lockedSlotCount(playerLevel)

    /** 현재 레벨에서 다음 해금 단계 (최대치면 null) */
    val nextDeckUnlock: DeckRules.Unlock? get() = DeckRules.nextUnlock(playerLevel)

    /** 다음 레벨까지 진행률 (0~1) */
    val levelProgress: Float get() = PlayerProgress.levelProgress(playerExp)

    /** 덱에 장착된 카드 id 집합 (컬렉션 그리드의 장착중 표시용) */
    val equippedCardIds: Set<Int> get() = party.map { it.card.id }.toSet()

    /** 장착된 카드 목록 */
    val equippedCards: List<TcgCard> get() = party.sortedBy { it.slotIndex }.map { it.card }

    /** 덱 총 전투력 표기 합계 (카드 전투력/100 합) — 상단 DECK 배지 */
    val deckDisplayPower: Int get() = DeckAutoBuilder.displayTotalPower(equippedCards)

    /** 슬롯 0~11 중 배치된 카드 (없으면 null) */
    fun memberAt(slotIndex: Int): PartyMember? = party.firstOrNull { it.slotIndex == slotIndex }

    /**
     * 적 컷인에 세울 보스 카드 번호 — VS 화면 적 진영 첫 카드와 같은 규칙
     * (스테이지 보스 → 마왕 → 속성 최고 DEF 순).
     */
    val enemyCutInCardId: Int
        get() = BattleLineup.enemyLineup(
            enemyElement = enemyElement,
            enemyName = enemyName,
            size = 3,
            isDemonLordBattle = isDemonLordBattle,
            bossCardId = activeStage?.let { StageCatalog.find(it)?.bossId }
        ).first().cardId

    // ── 진행 루프 파생 상태 ──

    /** 보유 카드 번호 집합 */
    val ownedCardIds: Set<Int> get() = ownedCards.map { it.id }.toSet()

    /** 영구 상실 후 환생 풀에 대기 중인 카드 번호 (무료 재계약 대상) */
    val lostCardIds: Set<Int> get() = reincarnationPool.map { it.templateId }.toSet()

    /**
     * 무료 긴급 보급 판정 — 보유 3장 미만 + 환생 풀 0장 + 팩/티켓/연성 불가일 때만 지급 대상.
     * (자세한 조건은 [EmergencySupplyEngine])
     */
    val supplyOffer: SupplyOffer
        get() = EmergencySupplyEngine.evaluate(
            ownedCardIds = ownedCardIds,
            lostCardIds = lostCardIds,
            gold = gold,
            packTickets = packTickets,
            shardTotal = shardTotal,
            claimsUsed = supplyClaims
        )

    /** 로비·스테이지 화면에서 구제 패널을 띄울지 (잃은 카드가 있거나 무료 보급이 필요) */
    val needsRescue: Boolean
        get() = ownedCards.size < EmergencySupplyEngine.MIN_OWNED_CARDS &&
            (reincarnationPool.isNotEmpty() || supplyOffer.isOffered)

    /** 덱 총 전투력 (ATK + DEF 합계) — 스테이지 권장 전투력과 비교하는 값 */
    val deckTotalPower: Int get() = equippedCards.sumOf { it.atk + it.def }

    /** 지금 도전할 앞 스테이지 번호 */
    val frontStageNumber: Int get() = StageEngine.frontStage(stageCleared)

    /** 선택한 스테이지 정의 (기본은 앞 스테이지) */
    val selectedStageDefinition: StageDefinition
        get() = StageCatalog.find(selectedStage) ?: StageCatalog.require(frontStageNumber)

    /** 스테이지 진행도 라벨 (예: "12/30 스테이지 · 3장 빙결의 회랑") */
    val stageProgressLabel: String
        get() = "${stageCleared.size}/${StageCatalog.TOTAL_STAGES} 스테이지 · ${selectedStageDefinition.chapter}장 ${selectedStageDefinition.chapterName}"

    /** 스테이지 목록에 쓰는 잠금 여부 */
    fun isStageUnlocked(stageNumber: Int): Boolean = StageEngine.isUnlocked(stageNumber, stageCleared)

    /** 선택한 스테이지의 난이도 판정 */
    val selectedStageReadiness get() = StageEngine.readiness(deckTotalPower, selectedStageDefinition)

    /** 보유 조각 총합 */
    val shardTotal: Int get() = shards.total

    /** 성장 화면에서 다루는 카드 (미지정이면 덱 1번 슬롯 → 첫 보유 카드) */
    val growthCard: TcgCard?
        get() = ownedCards.firstOrNull { it.id == growthCardId }
            ?: equippedCards.firstOrNull()
            ?: ownedCards.firstOrNull()

    /** 현실 소환 대상 (미지정이면 대표 캐릭터 → 첫 보유 카드) */
    val summonCard: TcgCard?
        get() = ownedCards.firstOrNull { it.id == summonCardId }
            ?: ownedCards.firstOrNull { it.id == lobbyHeroId }
            ?: ownedCards.firstOrNull()

    /** 연성 후보 전체 (미보유 신규 + 해금된 중간 보스) */
    val craftTargets: List<CraftTarget>
        get() = AdvancedSynthesisEngine.targets(ownedCardIds, stageCleared, shards, gold)

    /** 지금 바로 연성할 수 있는 후보 */
    val availableCrafts: List<CraftTarget> get() = craftTargets.filter { it.isAvailable }

    /** 소환 버프가 걸려 있는지 */
    val hasSummonBuff: Boolean get() = summonBuffBattles > 0 && summonBuffPercent > 0

    /** 페어 얼티밋 오의 발동 조건 (아군 2명 이상 생존 + 마나 3 이상 + 전투당 1회) */
    val canActivatePairUltimate: Boolean
        get() = !pairUltimateUsedThisBattle && aliveSurvivorCount >= PAIR_ULTIMATE_SURVIVORS &&
            mana.currentMana >= PAIR_ULTIMATE_MANA

    /** [홀림] 스택 2 이상이면 열리는 긴급 교차 이벤트 활성 여부 */
    val isTrustRiftAvailable: Boolean
        get() = trustRiftChoice == null && combatPenalty.totalStacks >= 2

    /** 기억이 지워진 동료 카드 목록 */
    val memoryResetCards: List<TcgCard> get() = ownedCards.filter { it.isMemoryReset }

    /** 제1장 5화까지 읽어 제2장 프롤로그 노드가 열린 상태 */
    val isChapter2Unlocked: Boolean
        get() = ChapterTwoScript.SHADOW_KNIGHT_PRELUDE.isUnlocked(storyStepIndex)

    // ── 메인 로비 파생 상태 ──

    /** 메인 화면 중앙에 크게 세워진 대표 캐릭터 카드 */
    val lobbyHero: TcgCard
        get() = ownedCards.firstOrNull { it.id == lobbyHeroId } ?: TcgGameViewModel.characterCard(lobbyHeroId)

    /** 대표 캐릭터의 표시용 프로필 (이름 · 칭호) */
    val lobbyHeroProfile: CharacterProfile?
        get() = TcgGameViewModel.profileOf(lobbyHeroId)

    /** 메인 화면에서 ◀ ▶ 로 넘길 수 있는 보유 캐릭터 순서 */
    val lobbyRoster: List<Int>
        get() = TcgGameViewModel.LOBBY_HERO_ORDER
            .filter { id -> ownedCards.any { it.id == id } }
            .ifEmpty { listOf(lobbyHeroId) }

    /** 대표 캐릭터의 현재 호감도 */
    val lobbyHeroBond: Int
        get() = if (lobbyHeroId == 3) maxOf(partnerBond, lobbyHero.bondLevel) else lobbyHero.bondLevel

    /** 데이트 룩(③) 해금 여부 — 친밀도 70 이상 */
    val isLobbyDateLookUnlocked: Boolean
        get() = lobbyHeroBond >= TcgGameViewModel.DATE_LOOK_UNLOCK_BOND

    /** 일상 사복(②) 해금 여부 — 친밀도 30 이상 (현실 소환 조건과 같은 값) */
    val isLobbyCasualLookUnlocked: Boolean
        get() = lobbyHeroBond >= TcgGameViewModel.CASUAL_LOOK_UNLOCK_BOND

    /** 로비 대표 캐릭터의 친밀도 해금 진행도 (0~1) */
    val lobbyHeroCasualProgress: Float
        get() = AffinityRules.casualProgress(lobbyHeroBond)

    /** 지금 로비에서 고를 수 있는 가장 높은 비주얼 (친밀도에 따른 상한) */
    val lobbyVisualCap: VisualState
        get() = AffinityRules.visualFor(lobbyHeroBond)

    /** 도감에서 지금 보고 있는 카드의 친밀도 */
    val selectedCardBond: Int
        get() = if (selectedCardId == 3) {
            maxOf(partnerBond, ownedCards.firstOrNull { it.id == 3 }?.bondLevel ?: 0)
        } else {
            ownedCards.firstOrNull { it.id == selectedCardId }?.bondLevel ?: 0
        }

    /** 차원 균열 에너지 최대치 */
    val gateEnergyMax: Int get() = TcgGameViewModel.GATE_ENERGY_MAX

    /** 상단 상태 스트립의 진행도 라벨 */
    val storyProgressLabel: String get() = "제1장 ${storyStepIndex + 1}/${StoryScript.STEPS.size}"

    /** 도입부 인트로가 지금 화면을 덮고 있는지 */
    val isIntroPlaying: Boolean get() = introStory.isPlaying

    /** 카드팩 개봉 연출이 지금 화면을 덮고 있는지 */
    val isPackOpeningActive: Boolean get() = packOpening.isActive

    /** 로비 정보 패널의 최상단 1줄 요약 (차원·재화 3종) */
    val lobbyTopSummaryLine: String get() = LobbyInfoPanel.topSummaryLine(this)

    /** 메인 로비 경보 — 누르면 해당 화면으로 이동 */
    val lobbyAlerts: List<LobbyAlert>
        get() = buildList {
            add(
                LobbyAlert(
                    "다음 스테이지 ${frontStageNumber} · 덱 ${deckTotalPower}",
                    LobbyDestination.STAGE
                )
            )
            if (supplyOffer.isOffered) {
                add(LobbyAlert("긴급 보급 · 동료 ${supplyOffer.cardIds.size}명 무료", LobbyDestination.STAGE, true))
            } else if (needsRescue) {
                add(LobbyAlert("재계약 대기 ${reincarnationPool.size}장 (무료)", LobbyDestination.STAGE, true))
            }
            if (isTrustRiftAvailable) add(LobbyAlert("신뢰의 균열", LobbyDestination.SCENARIO, true))
            if (memoryResetCards.isNotEmpty()) add(LobbyAlert("기억의 잔향", LobbyDestination.REAL_WORLD))
            if (combatPenalty.totalStacks > 0) {
                add(LobbyAlert("[홀림] ${combatPenalty.totalStacks}", LobbyDestination.COLLECTION, true))
            }
            if (isChapter2Unlocked) add(LobbyAlert("제2장 프롤로그", LobbyDestination.SCENARIO))
        }

    companion object {
        /** 페어 얼티밋 오의 최소 생존 인원 */
        const val PAIR_ULTIMATE_SURVIVORS: Int = 2

        /** 페어 얼티밋 오의 소모 마나 */
        const val PAIR_ULTIMATE_MANA: Int = 3
    }
}

// ============================================================================
// ViewModel — 메인 로비(캐릭터 중심) · 기능 화면(시나리오 · 전투 · 탐색 · 현실 · 도감 · 덱)
// ============================================================================

class TcgGameViewModel(
    private val battleEngine: IBattleEngine = BattleEngine(),
    private val synthesisEngine: ISynthesisEngine = SynthesisEngine(),
    private val dualWorldEngine: IDualWorldEngine = DualWorldEngine(),
    private val manaEngine: IManaEngine = ManaEngine(),
    private val temptationEngine: ITemptationEngine = TemptationEngine(),
    private val lifecycleEngine: IRoguelikeLifecycleEngine = RoguelikeLifecycleEngine(),
    private val retreatEngine: ITacticalRetreatEngine = TacticalRetreatEngine(),
    private val memoryRecallEngine: IMemoryRecallEngine = MemoryRecallEngine(),
    private val trustRiftEngine: ITrustRiftEngine = TrustRiftEngine(),
    private val pairUltimateEngine: IPairUltimateEngine = PairUltimateEngine(),
    private val explorationEngine: IExplorationEngine = ExplorationEngine(),
    // ── 도입부 스토리(4장면 컷신) ──
    private val introEngine: IntroStoryEngine = IntroStoryEngine(),
    // ── 진행 루프(2026-11-16): 카드팩 · 강화 · 연성 · 장비 · 스테이지 ──
    private val packShopEngine: IPackShopEngine = PackShopEngine(),
    /** 카드팩 개봉 연출 상태 전이 (봉인 → 1장씩 → 전체 보기) */
    private val packOpeningEngine: IPackOpeningEngine = PackOpeningEngine(),
    private val upgradeEngine: ICardUpgradeEngine = CardUpgradeEngine(),
    /** 시작 3장 추첨 · 카드팩 개봉 · 장비 드롭에 쓰는 난수 (테스트는 고정 시드) */
    private val random: Random = Random.Default,
    /**
     * 시작 보유 카드 번호를 지정한다 (null = 저코스트 풀에서 무작위 3장).
     * 도감·로스터 전체를 대상으로 하는 검증·데모에서만 쓴다.
     */
    initialOwnedCardIds: List<Int>? = null,
    // ── 시작 재화·진행도 시드 (세이브 복원·검증용) ──
    initialGold: Int = START_GOLD,
    initialGems: Int = START_GEMS,
    initialPackTickets: Int = START_PACK_TICKETS,
    initialShards: ShardWallet = ShardWallet(),
    initialEquipmentIds: List<String> = emptyList(),
    initialClearedStages: Set<Int> = emptySet(),
    /** 앱을 다시 켰을 때 인트로를 이미 본 상태라면 true (MainActivity가 저장값을 넘긴다) */
    introAlreadySeen: Boolean = false,
    /** 시작 카드의 친밀도 — 기본 0(모든 캐릭터는 0에서 시작). 테스트·데모에서만 지정한다 */
    private val initialBondLevel: Int = CardCatalog.ROSTER_BOND_SEED,
    /** 보유 기간 정산에 쓰는 「오늘」(에포크 일) — 단위 테스트는 고정값을 넘긴다 */
    private val todayEpochDay: () -> Int = { AffinityRules.epochDay(System.currentTimeMillis()) },
    /**
     * 진행 상태 자동 저장소 — 넘기면 상태가 바뀔 때마다 저장하고, 생성 시 저장본을 복원한다.
     * null 이면 저장하지 않는다(단위 테스트·데모 기본값).
     */
    private val saveStore: SaveStore? = null
) : ViewModel() {

    /** 시작 보유 카드 — 저코스트 풀에서 무작위 3장 (또는 테스트 지정 목록) */
    private val starterCards: List<TcgCard> =
        (initialOwnedCardIds ?: StartingRoster.draw(random))
            .distinct()
            .map { TcgGameViewModel.characterCard(it, initialBondLevel) }

    /** 시작 덱 — 뽑힌 카드를 전열 1~3칸에 자동 배치 */
    private val starterParty: List<PartyMember> =
        StartingRoster.deploySlots(starterCards).map { (card, slot) ->
            val maxHp = TcgGameViewModel.maxHpOf(card)
            PartyMember(card, slot, DeckRules.isFrontSlot(slot), maxHp, maxHp)
        }

    private val _uiState = MutableStateFlow(
        TcgGameState(
            gold = initialGold,
            gems = initialGems,
            junkCards = 0,
            packTickets = initialPackTickets,
            shards = initialShards,
            equipment = initialEquipmentIds.mapNotNull { EquipmentCatalog.instantiate(it) },
            stageCleared = initialClearedStages,
            selectedStage = StageEngine.frontStage(initialClearedStages),
            party = starterParty,
            ownedCards = starterCards,
            lobbyHeroId = starterParty.firstOrNull()?.card?.id
                ?: starterCards.firstOrNull()?.id
                ?: TcgGameViewModel.DEFAULT_LOBBY_HERO_ID,
            growthCardId = starterParty.firstOrNull()?.card?.id ?: 0,
            summonCardId = starterParty.firstOrNull()?.card?.id ?: 0,
            statusMessage = "[시작] 봉인 카드팩에서 동료 ${starterCards.size}명과 계약했습니다. 스테이지 1에 도전하세요.",
            introStory = introEngine.initialState(alreadySeen = introAlreadySeen)
        )
    )
    val uiState: StateFlow<TcgGameState> = _uiState.asStateFlow()

    /** 마지막으로 저장소에 쓴 내용 (같은 내용을 다시 쓰지 않기 위한 비교용) */
    private var lastPersisted: String? = null

    init {
        starterCards.forEach { lifecycleEngine.registerOwnedCard(it) }
        restoreSavedProgress()
        // 보유 기간 친밀도(하루 +1) 정산 — 앱을 다시 켰을 때 지난 날짜만큼만
        claimHoldingBond()
    }

    /**
     * 보유 기간 친밀도 정산 — 마지막 정산일로부터 지난 날짜만큼 하루 +1
     * (한 번에 최대 [AffinityRules.HOLDING_GAIN_MAX_PER_CLAIM]).
     * 전투 1회 +6 에 비하면 아주 작은 보조 경로다 — 전투가 친밀도의 중심이라는 규칙 유지.
     */
    private fun claimHoldingBond() {
        val today = todayEpochDay()
        updateState { s ->
            if (s.ownedCards.isEmpty()) return@updateState s
            val lines = mutableListOf<String>()
            s.ownedCards.forEach { card ->
                if (card.lastBondDay == 0) {
                    // 처음 보는 날 — 오늘을 기준일로만 기록 (아직 상승 없음)
                    card.lastBondDay = today
                    return@forEach
                }
                val days = today - card.lastBondDay
                if (days <= 0) return@forEach
                val change = AffinityRules.creditHolding(card, days)
                card.lastBondDay = today
                if (change.gained > 0) {
                    lines += "[보유 ${days}일] ${shortName(card.id, card.name)} 친밀도 " +
                        "${change.before}→${change.after} (+${change.gained})"
                }
            }
            if (lines.isEmpty()) return@updateState s
            s.copy(
                ownedCards = s.ownedCards.toList(),
                realWorldLog = (s.realWorldLog + lines).takeLast(6),
                battleLog = s.battleLog + lines
            )
        }
    }

    /**
     * 전투 출전 정산 — 출전한 카드마다 친밀도 +[AffinityRules.BATTLE_DEPLOY_GAIN], 출전 횟수 +1.
     * 승리하면 [applyVictory] 가 승리 보너스(+2)를 더해 **한 전투 +6** 이 된다.
     * (TcgCard 는 편성·보유가 같은 인스턴스를 공유하므로 제자리 변경으로 충분하다)
     */
    private fun creditBattleDeployment(s: TcgGameState): TcgGameState {
        val deployed = s.party.map { it.card.id }.distinct()
        if (deployed.isEmpty()) return s
        val lines = mutableListOf<String>()
        deployed.forEach { id ->
            val card = s.ownedCards.firstOrNull { it.id == id } ?: return@forEach
            val change = AffinityRules.creditBattleDeployment(card)
            lines += "[출전] ${shortName(id, card.name)} 출전 ${card.battleCount}회 · " +
                "친밀도 ${change.before}→${change.after} (+${change.gained})" +
                affinityUnlockSuffix(change)
        }
        return s.copy(ownedCards = s.ownedCards.toList(), battleLog = s.battleLog + lines)
    }

    /** 친밀도 상승으로 새 단계가 열렸을 때 로그에 붙이는 문구 */
    private fun affinityUnlockSuffix(change: AffinityRules.BondChange): String =
        if (change.newlyUnlocked.isEmpty()) {
            ""
        } else {
            " · 🎉 ${change.newlyUnlocked.joinToString("·") { it.displayName }} 개방!"
        }

    // ──────────────────────────────────────────────────────────────
    // 진행 상태 자동 저장 (앱을 꺼도 이어서)
    // ──────────────────────────────────────────────────────────────

    /**
     * 저장본을 시작 상태 위에 덮어쓴다.
     * 저장 파일이 없거나 손상·구버전이면 아무것도 하지 않고 신규 시작 상태를 그대로 쓴다(크래시 없음).
     */
    private fun restoreSavedProgress() {
        val store = saveStore ?: return
        val text = runCatching { store.read() }.getOrNull() ?: return
        val saved = SaveCodec.decode(text) ?: return
        val restored = runCatching { saved.restoreInto(_uiState.value) }.getOrNull() ?: return
        // 보유 카드·환생 풀을 상실 판정 엔진에도 그대로 등록해 이후 전투가 정상 동작하게 한다.
        // (시작 카드는 같은 번호가 복원 카드로 바뀌므로 먼저 비운다)
        lifecycleEngine.collection.cards.clear()
        lifecycleEngine.collection.reincarnationPool.clear()
        restored.ownedCards.forEach { lifecycleEngine.registerOwnedCard(it) }
        restored.reincarnationPool.forEach {
            lifecycleEngine.collection.reincarnationPool[it.templateId] = it
        }
        lastPersisted = text
        _uiState.value = restored
    }

    /**
     * 상태 변경 + 자동 저장. (기존 `updateState { }` 자리를 그대로 대신한다)
     * 진행에 의미 있는 변화가 있을 때만 파일을 쓴다.
     */
    private inline fun updateState(transform: (TcgGameState) -> TcgGameState) {
        _uiState.update(transform)
        persist()
    }

    /** 지금 상태를 저장한다 (변경이 없으면 쓰지 않는다) */
    private fun persist() {
        val store = saveStore ?: return
        val payload = runCatching { SaveCodec.encode(_uiState.value.toGameSave()) }.getOrNull() ?: return
        if (payload == lastPersisted) return
        store.write(payload)
        lastPersisted = payload
    }

    /**
     * 즉시 저장 — 화면이 백그라운드로 갈 때 MainActivity 가 호출한다.
     * (평소에는 상태가 바뀔 때마다 자동 저장되므로 마지막 상태를 확정하는 용도)
     */
    fun saveNow() {
        lastPersisted = null
        persist()
    }

    /**
     * 「새 게임」 — 진행을 모두 지우고 시작 상태로 되돌린다.
     * 저장 파일도 새 게임 상태로 덮어써서, 앱을 다시 켜도 되살아나지 않는다.
     */
    fun resetGame() {
        saveStore?.clear()
        lastPersisted = null

        // 무작위로 새 스타팅 3인방을 다시 뽑아 진짜 처음부터 시작한다.
        val freshCards = TcgGameViewModel.initialOwnedCards(random, initialBondLevel)
        val freshParty = TcgGameViewModel.initialParty(freshCards)

        // 상실 판정 엔진의 보유/환생 목록도 초기화
        lifecycleEngine.collection.cards.clear()
        lifecycleEngine.collection.reincarnationPool.clear()
        freshCards.forEach { lifecycleEngine.registerOwnedCard(it) }

        // 마왕 토벌 기록 초기화
        DemonLordRegistry.ALL.forEach {
            it.isDefeated = false
            it.isVowedCompanion = false
        }

        _uiState.value = TcgGameState(
            gold = START_GOLD,
            gems = START_GEMS,
            junkCards = 0,
            packTickets = START_PACK_TICKETS,
            party = freshParty,
            ownedCards = freshCards,
            lobbyHeroId = freshParty.firstOrNull()?.card?.id ?: DEFAULT_LOBBY_HERO_ID,
            growthCardId = freshParty.firstOrNull()?.card?.id ?: 0,
            summonCardId = freshParty.firstOrNull()?.card?.id ?: 0,
            statusMessage = "[새 게임] 진행을 초기화했습니다. 봉인 카드팩에서 동료 ${freshCards.size}명과 새로 계약했습니다.",
            introStory = introEngine.initialState(alreadySeen = true)
        )
        persist()
    }

    // ──────────────────────────────────────────────────────────────
    // 오디오 — 상태 유도 BGM 장면 · 액션별 SFX 이벤트 (Compose 레이어가 구독)
    // ──────────────────────────────────────────────────────────────

    /** BGM·SFX 토글/볼륨 변경 (화면 상단 오디오 패널에서 호출) */
    fun updateAudioSettings(transform: (AudioSettings) -> AudioSettings) {
        updateState { s ->
            s.copy(audioSettings = transform(s.audioSettings).sanitized())
        }
    }

    /** 현재 상태에 대응하는 BGM 장면 (전투 · 보스 · 인트로 · 로비) */
    fun audioSceneOf(s: TcgGameState = _uiState.value): AudioScene {
        // 이미 저장된 장면(전투 개시 시 지정)을 우선하고, 인트로만 실시간으로 덮어쓴다.
        val stored = s.audioScene
        if (s.isIntroPlaying) return AudioScene.INTRO
        // 전투가 끝나면(승리 정산에서 LOBBY 로 재지정) 저장 장면을 따른다.
        return when (stored) {
            AudioScene.BATTLE, AudioScene.BOSS, AudioScene.INTRO, AudioScene.LOBBY -> stored
            AudioScene.NONE -> AudioSceneMapper.of(
                isIntroPlaying = s.isIntroPlaying,
                screen = s.currentScreen,
                isBossBattle = s.isDemonLordBattle || s.isChapter2BossBattle || isActiveStageBoss(s)
            )
        }
    }

    private fun isActiveStageBoss(s: TcgGameState): Boolean {
        val stage = s.activeStage?.let { com.game.tcg.core.model.StageCatalog.find(it) } ?: return false
        return stage.isBoss
    }

    // ──────────────────────────────────────────────────────────────
    // 메인 로비 — 대표 캐릭터 · 좌·우측 서브메뉴 · 화면 전환
    // ──────────────────────────────────────────────────────────────

    // ──────────────────────────────────────────────────────────────
    // 도입부 스토리 인트로 — 우연한 발견 → 봉인 해제 → 듀얼 게이트 → 소환과 시작
    // ──────────────────────────────────────────────────────────────

    /** 인트로 한 장면 넘기기 (화면 탭 · 자동 진행). 마지막 장면이면 로비로 진입한다. */
    fun advanceIntro() {
        updateState { s -> applyIntroTransition(s, introEngine.advance(s.introStory)) }
    }

    /** SKIP — 인트로를 즉시 닫고 로비로 진입한다. */
    fun skipIntro() {
        updateState { s -> applyIntroTransition(s, introEngine.skip(s.introStory)) }
    }

    /** 로비의 「🎬 인트로 다시보기」 — 1장면부터 다시 재생한다. */
    fun replayIntro() {
        updateState { s ->
            s.copy(
                currentScreen = LobbyDestination.LOBBY,
                isInfoPanelOpen = false,
                introStory = introEngine.replay(s.introStory),
                statusMessage = "[도입부] 다시보기 — ${IntroScript.SCENES.first().title}부터 재생합니다."
            )
        }
    }

    /** 인트로 상태 전이 결과를 상태·안내문에 반영 (끝나면 로비로 복귀) */
    private fun applyIntroTransition(s: TcgGameState, next: IntroStoryState): TcgGameState {
        if (next == s.introStory) return s
        if (next.isPlaying) {
            return s.copy(
                introStory = next,
                audioScene = AudioScene.INTRO,
                statusMessage = "[도입부] ${next.scene.act} · ${next.scene.title}"
            )
        }
        val heroName = s.lobbyHeroProfile?.displayName ?: s.lobbyHero.name
        val message = if (next.skipped) {
            "[메인 로비] 인트로를 건너뛰었습니다. ${heroName}이(가) 기다리고 있었습니다."
        } else {
            "[메인 로비] 계약이 완료되었습니다. ${heroName}이(가) 당신을 맞이합니다."
        }
        return s.copy(
            currentScreen = LobbyDestination.LOBBY,
            introStory = next,
            audioScene = AudioScene.LOBBY,
            statusMessage = message
        )
    }

    /** 좌·우측 측면 서브메뉴에서 기능 화면 열기 */
    // ──────────────────────────────────────────────────────────────
    // 메인 로비 「📊 정보」 패널 — 상단 정보 과다를 한곳으로 모은다
    // ──────────────────────────────────────────────────────────────

    /** 「📊 정보」 — 진행도 · 정신 상태 · 설정 · 경보를 모은 패널 열기 */
    fun openInfoPanel() {
        updateState { s ->
            s.copy(
                isInfoPanelOpen = true,
                statusMessage = "[정보] 스테이지·덱·정신 상태·설정을 한곳에서 확인합니다."
            )
        }
    }

    /** 패널 닫기 */
    fun closeInfoPanel() {
        updateState { s -> if (!s.isInfoPanelOpen) s else s.copy(isInfoPanelOpen = false) }
    }

    /** 패널 열기/닫기 토글 */
    fun toggleInfoPanel() {
        updateState { s ->
            if (s.isInfoPanelOpen) {
                s.copy(isInfoPanelOpen = false)
            } else {
                s.copy(
                    isInfoPanelOpen = true,
                    statusMessage = "[정보] 스테이지·덱·정신 상태·설정을 한곳에서 확인합니다."
                )
            }
        }
    }

    /** 패널 안의 경보 → 해당 화면으로 이동하며 패널을 닫는다 */
    fun openAlertDestination(destination: LobbyDestination) {
        updateState { s ->
            s.copy(
                currentScreen = destination,
                isInfoPanelOpen = false,
                audioScene = audioSceneOf(s.copy(currentScreen = destination)),
                statusMessage = "[${destination.label}] ${destination.tagline}"
            )
        }
    }

    fun openScreen(destination: LobbyDestination) {
        updateState { s ->
            s.copy(
                currentScreen = destination,
                isInfoPanelOpen = false,
                audioScene = audioSceneOf(s.copy(currentScreen = destination)),
                statusMessage = if (destination == LobbyDestination.LOBBY) {
                    "[메인 로비] ${s.lobbyHeroProfile?.displayName ?: s.lobbyHero.name}이(가) 당신을 맞이합니다."
                } else {
                    "[${destination.label}] ${destination.tagline}"
                }
            )
        }
    }

    /** ← 메인 로비 — 어떤 기능 화면에서든 로비로 복귀 (Android BackHandler 연동) */
    fun backToLobby() {
        updateState { s ->
            s.copy(
                currentScreen = LobbyDestination.LOBBY,
                isInfoPanelOpen = false,
                audioScene = AudioScene.LOBBY,
                statusMessage = "[메인 로비] ${s.lobbyHeroProfile?.displayName ?: s.lobbyHero.name}이(가) 기다리고 있었습니다."
            )
        }
    }

    /** 메인 화면 대표 캐릭터 지정 (캐릭터 관리의 '대표 캐릭터 설정'과 동일) */
    fun selectLobbyHero(cardId: Int) {
        updateState { s ->
            if (s.lobbyRoster.none { it == cardId }) {
                return@updateState s.copy(
                    statusMessage = "[대표 캐릭터] 아직 계약하지 않은 캐릭터입니다. 캐릭터 관리의 신규 연성으로 획득할 수 있습니다."
                )
            }
            val heroName = heroDisplayName(cardId)
            s.copy(
                lobbyHeroId = cardId,
                lobbyVisual = outfitFor(s, cardId),
                lobbyOutfits = s.lobbyOutfits + (s.lobbyHeroId to s.lobbyVisual),
                statusMessage = "[대표 캐릭터] $heroName — 메인 화면에 소환되었습니다."
            )
        }
    }

    /** ◀ ▶ 로 메인 화면 대표 캐릭터 전환 */
    fun cycleLobbyHero(direction: Int) {
        updateState { s ->
            val roster = s.lobbyRoster
            if (roster.size <= 1) {
                return@updateState s.copy(statusMessage = "[대표 캐릭터] 전환할 캐릭터가 없습니다. (보유 ${roster.size}명)")
            }
            val currentIndex = roster.indexOf(s.lobbyHeroId).coerceAtLeast(0)
            val nextIndex = ((currentIndex + direction) % roster.size + roster.size) % roster.size
            val nextId = roster[nextIndex]
            val heroName = heroDisplayName(nextId)
            s.copy(
                lobbyHeroId = nextId,
                lobbyVisual = outfitFor(s, nextId),
                lobbyOutfits = s.lobbyOutfits + (s.lobbyHeroId to s.lobbyVisual),
                statusMessage = "[대표 캐릭터] $heroName (${nextIndex + 1}/${roster.size})"
            )
        }
    }

    /**
     * 그 캐릭터가 마지막에 고른 복장 — 없으면 지금 복장에서 친밀도 상한까지만 맞춘다.
     * (해금되지 않은 복장으로는 되돌아가지 않는다 — 기존 잠금 규칙 유지)
     */
    private fun outfitFor(s: TcgGameState, cardId: Int): VisualState {
        val bond = bondOf(s, cardId)
        val remembered = s.lobbyOutfits[cardId] ?: s.lobbyVisual
        return capVisual(remembered, bond)
    }

    /**
     * 메인 화면 3단 비주얼(전투 · 일상 · 데이트) 전환.
     * ② 일상은 친밀도 30, ③ 데이트는 70부터 열린다(2026-11-16 개편).
     *
     * 고른 복장은 **캐릭터별로 기억**해([TcgGameState.lobbyOutfits]) 다른 캐릭터를 보고
     * 돌아오거나 앱을 껐다 켜도 그대로 유지된다.
     */
    fun selectLobbyVisual(visual: VisualState) {
        updateState { s ->
            val heroName = s.lobbyHeroProfile?.displayName ?: s.lobbyHero.name
            val bond = s.lobbyHeroBond
            if (!AffinityRules.isVisualUnlocked(visual, bond)) {
                val need = AffinityRules.requiredBond(visual)
                return@updateState s.copy(
                    statusMessage = "[잠금] ${visual.displayName}은(는) $heroName 친밀도 $need 이상에서 열립니다. " +
                            "(현재 $bond/100 — 이계 전투 출전 1회 +${AffinityRules.BATTLE_WIN_GAIN})",
                    visualLockMessage = AffinityRules.lockLabel(visual, bond)
                )
            }
            s.copy(
                lobbyVisual = visual,
                lobbyOutfits = s.lobbyOutfits + (s.lobbyHeroId to visual),
                visualLockMessage = null,
                statusMessage = "[연출] $heroName — ${visual.displayName}"
            )
        }
    }

    /** 친밀도에 맞는 단계까지만 비주얼을 유지한다 */
    private fun capVisual(visual: VisualState, bond: Int): VisualState {
        val cap = AffinityRules.visualFor(bond)
        return if (visual.ordinal > cap.ordinal) cap else visual
    }

    private fun heroDisplayName(cardId: Int): String =
        TcgGameViewModel.profileOf(cardId)?.displayName
            ?: TcgGameViewModel.characterCard(cardId).name

    /** 경험치 획득으로 레벨·덱 해금이 바뀌었을 때 상태 메시지에 붙이는 문구 */
    private fun levelLineFor(beforeExp: Int, gainedExp: Int): String {
        val beforeLevel = PlayerProgress.levelForExp(beforeExp)
        val afterLevel = PlayerProgress.levelForExp(beforeExp + gainedExp)
        if (afterLevel <= beforeLevel) return ""
        val beforeSize = DeckRules.deckSizeForLevel(beforeLevel)
        val afterSize = DeckRules.deckSizeForLevel(afterLevel)
        return if (afterSize > beforeSize) {
            " · Lv.$afterLevel 달성 — 덱 ${afterSize}장 해금!"
        } else {
            " · Lv.$afterLevel 달성"
        }
    }

    private fun bondOf(s: TcgGameState, cardId: Int): Int =
        if (cardId == 3) {
            maxOf(s.partnerBond, s.ownedCards.firstOrNull { it.id == 3 }?.bondLevel ?: 0)
        } else {
            s.ownedCards.firstOrNull { it.id == cardId }?.bondLevel ?: 0
        }

    // ──────────────────────────────────────────────────────────────
    // 탐색 — 차원 틈새 탐색 · 기억 파편 수색 · 랜덤 조우
    // ──────────────────────────────────────────────────────────────

    /**
     * 탐색 1회 수행 — 차원 균열 에너지를 소모하고 보상을 적용한다.
     * 전투 조우가 나오면 이계 전투 무대를 해당 적으로 세팅한다.
     */
    fun explore(mode: ExplorationMode): ExplorationResult? {
        var resultOut: ExplorationResult? = null
        updateState { s ->
            if (s.ownedCards.isEmpty()) {
                return@updateState s.copy(
                    statusMessage = "[탐색 불가] 보유 동료가 0장입니다. 재계약(무료)이나 긴급 보급으로 덱을 먼저 채우세요."
                )
            }
            if (s.gateEnergy < mode.energyCost) {
                return@updateState s.copy(
                    statusMessage = "[탐색 불가] 차원 균열 에너지가 부족합니다. (보유 ${s.gateEnergy}/${s.gateEnergyMax}) " +
                            "— 이계 전투 승리로 +${GATE_ENERGY_RECOVERY} 회복됩니다."
                )
            }

            val result = explorationEngine.explore(
                mode = mode,
                context = ExplorationContext(
                    gateEnergy = s.gateEnergy,
                    temptationStacks = s.combatPenalty.totalStacks,
                    memoryResetCardIds = s.memoryResetCards.map { it.id },
                    explorationCount = s.explorationCount,
                    partnerBond = s.partnerBond,
                    gold = s.gold
                )
            )
            resultOut = result

            var gold = s.gold
            var gems = s.gems
            var holyWater = s.holyWater
            var junkCards = s.junkCards
            var bond = s.partnerBond
            var energy = (s.gateEnergy - mode.energyCost).coerceAtLeast(0)
            var temptation = s.temptationState
            var cards = s.ownedCards
            var fragmentMessage: String? = null

            result.rewards.forEach { reward ->
                when (reward.type) {
                    ExplorationRewardType.GOLD -> gold += reward.amount
                    ExplorationRewardType.GEMS -> gems += reward.amount
                    ExplorationRewardType.HOLY_WATER -> holyWater += reward.amount
                    ExplorationRewardType.JUNK_CARD -> junkCards += reward.amount
                    ExplorationRewardType.GATE_ENERGY -> energy = (energy + reward.amount).coerceAtMost(s.gateEnergyMax)
                    ExplorationRewardType.BOND -> bond = (bond + reward.amount).coerceAtMost(100)
                    ExplorationRewardType.TEMPTATION -> {
                        temptation = temptationEngine.processUndercoverChoice(
                            UndercoverDialogueOption(
                                optionId = "EXPLORE_${result.mode.name}",
                                text = result.title,
                                isTemptedChoice = true,
                                goldStolen = 0,
                                curseAdded = reward.amount
                            ),
                            temptation
                        )
                    }
                    ExplorationRewardType.COMPANION_BOND -> {
                        val companionId = result.companionCardId
                        if (companionId != null) {
                            cards = cards.map { card ->
                                if (card.id == companionId) {
                                    card.bondLevel = (card.bondLevel + reward.amount).coerceAtMost(100)
                                }
                                card
                            }
                        }
                    }
                    ExplorationRewardType.MEMORY_FRAGMENT -> {
                        val targetId = result.fragmentTargetCardId
                        val card = cards.firstOrNull { it.id == targetId }
                        if (card != null && card.isMemoryReset) {
                            val gain = memoryRecallEngine.grantFragment(
                                card,
                                reward.amount,
                                MemoryFragmentSource.EXPLORATION
                            )
                            fragmentMessage = gain.message
                            cards = cards.map { if (it.id == card.id) card else it }
                        }
                    }
                }
            }

            val penalty = temptationEngine.calculateCombatPenalty(temptation)
            val logLine = "[탐색 · ${mode.displayName}] ${result.title} — ${result.rewardSummary}"
            var next = s.copy(
                gold = gold,
                gems = gems,
                holyWater = holyWater,
                junkCards = junkCards,
                partnerBond = bond,
                gateEnergy = energy,
                temptationState = temptation,
                combatPenalty = penalty,
                ownedCards = cards.map { card ->
                    if (card.id == 3) {
                        card.bondLevel = bond
                    }
                    card
                },
                explorationCount = s.explorationCount + 1,
                lastExploration = result,
                playerExp = s.playerExp + PlayerProgress.EXP_EXPLORATION,
                explorationLog = (s.explorationLog + logLine + listOfNotNull(fragmentMessage)).takeLast(8),
                statusMessage = "[탐색 · ${mode.displayName}] ${result.title} — ${result.rewardSummary}" +
                        levelLineFor(s.playerExp, PlayerProgress.EXP_EXPLORATION)
            )

            if (result.startsBattle) {
                // 탐색에서 마수와 마주친 것도 전투 출전이다 — 친밀도 +4
                val deployBase = creditBattleDeployment(next)
                next = deployBase.copy(
                    enemyName = result.enemyName ?: s.enemyName,
                    enemyElement = result.enemyElement,
                    enemyHp = result.enemyHp,
                    enemyMaxHp = result.enemyMaxHp,
                    enemyAtk = result.enemyAtk,
                    isDemonLordBattle = false,
                    isChapter2BossBattle = false,
                    turn = 1,
                    mana = ManaState(1, 1),
                    party = freshParty(deployBase),
                    retreatInfo = BattleRetreatInfo(turn = 1),
                    pairUltimateUsedThisBattle = false,
                    pairUltimateBanner = null,
                    pairUltimateBannerSubtitle = null,
                    pendingLootGold = 400,
                    pendingLootGems = 8,
                    statusMessage = "[조우] ${result.enemyName} 출현 — 이계 전투에서 교전하세요."
                )
            }
            next
        }
        return resultOut
    }

    // ──────────────────────────────────────────────────────────────
    // 시나리오 진행
    // ──────────────────────────────────────────────────────────────

    /** 다음 스토리 카드로 진행 */
    fun advanceStory() {
        updateState { s ->
            val next = (s.storyStepIndex + 1).coerceAtMost(StoryScript.STEPS.size - 1)
            if (next == s.storyStepIndex) {
                s.copy(statusMessage = "[제1장 완료] 다음 챕터는 업데이트 예정입니다. 마왕전과 도감을 먼저 즐겨 보세요.")
            } else {
                val step = StoryScript.STEPS[next]
                s.copy(
                    storyStepIndex = next,
                    storyReadLog = (s.storyReadLog + "[${step.chapter}] ${step.title} — ${step.objective}").takeLast(8),
                    statusMessage = "[${step.chapter}] ${step.title} — ${step.objective}"
                )
            }
        }
    }

    /** 이전 스토리 카드로 이동 */
    fun previousStory() {
        updateState { s ->
            s.copy(storyStepIndex = (s.storyStepIndex - 1).coerceAtLeast(0))
        }
    }

    /** 샘플 이벤트 A — 비 오는 밤 카페의 '릴리' */
    fun chooseEventA(talkWithLily: Boolean) {
        updateState { s ->
            if (s.eventAChoice != null) {
                return@updateState s.copy(statusMessage = "[이벤트 A] 이미 선택을 마친 이벤트입니다.")
            }
            if (!talkWithLily) {
                if (s.holyWater <= 0) {
                    return@updateState s.copy(statusMessage = "[이벤트 A] 성수가 없습니다. 대화를 피할 수 없어 유혹 스택이 1 쌓였습니다. (도감에서 성수 정화 가능)")
                }
                val purified = temptationEngine.purifyTemptationWithHolyWater(s.temptationState, 1)
                val penalty = temptationEngine.calculateCombatPenalty(purified)
                s.copy(
                    eventAChoice = "A_PURGE",
                    eventAResult = "[성수 정화] 릴리의 눈을 똑바로 보며 성수를 뿌렸다. 그녀가 낮게 웃었다.\n" +
                            "\"역시… 쉽게 넘어오진 않네.\" — 유혹 스택 -1, 성수 -1, 방어 보너스 +10% 확보.",
                    holyWater = s.holyWater - 1,
                    defenseBonusPercent = s.defenseBonusPercent + 10,
                    temptationState = purified,
                    combatPenalty = penalty,
                    statusMessage = "[이벤트 A] 경계하고 성수로 정화한다 — 방어 보너스 +10%"
                )
            } else {
                val option = UndercoverDialogueOption(
                    optionId = "A_TALK",
                    text = "차를 마시며 대화를 나눈다",
                    isTemptedChoice = true,
                    goldStolen = 150,
                    curseAdded = 1
                )
                val tempted = temptationEngine.processUndercoverChoice(option, s.temptationState)
                val penalty = temptationEngine.calculateCombatPenalty(tempted)
                s.copy(
                    eventAChoice = "A_TALK",
                    eventAResult = "[유혹에 흔들림] 릴리와 밤새 이야기를 나눴다. 그녀는 당신의 카드 봉인 문양까지 " +
                            "알고 있었다. 헤어질 때 지갑에서 150G가 사라져 있었다.\n" +
                            penalty.debuffDescription + "\n" +
                            "[경고] 지금 얻는 이득은 없습니다. 이 스택은 마왕전에서 아군 공격력 저하·피격 피해 증가로 돌아옵니다.",
                    gold = (s.gold - 150).coerceAtLeast(0),
                    lilyFamiliarity = s.lilyFamiliarity + 12,
                    temptationState = tempted,
                    combatPenalty = penalty,
                    statusMessage = "[이벤트 A] 차를 마시며 대화 — [홀림] 스택이 쌓였습니다."
                )
            }
        }
    }

    /** 샘플 이벤트 B — 차원 틈새의 마수 */
    fun chooseEventB(engageBattle: Boolean) {
        updateState { s ->
            if (s.eventBChoice != null) {
                return@updateState s.copy(statusMessage = "[이벤트 B] 이미 선택을 마친 이벤트입니다.")
            }
            if (engageBattle) {
                s.copy(
                    eventBChoice = "B_FIGHT",
                    eventBResult = "[결전 선언] 마수를 향해 카드를 뽑았다. 전열 3장이 지면에 내려선다.\n" +
                            "이계 전투 탭에서 '전열 공격'으로 교전을 시작하세요.",
                    enemyName = "차원 틈새의 마수",
                    enemyElement = Element.DARK,
                    enemyHp = 12000,
                    enemyMaxHp = 12000,
                    enemyAtk = 900,
                    isDemonLordBattle = false,
                    turn = 1,
                    mana = ManaState(1, 1),
                    battleLog = s.battleLog + "[교전 개시] 차원 틈새의 마수와 조우! 전열 3장 전개.",
                    statusMessage = "[이벤트 B] 즉시 전투 진입 — 이계 전투 탭으로 이동하세요."
                )
            } else {
                s.copy(
                    eventBChoice = "B_SCOUT",
                    eventBResult = "[정찰 보고] 마수의 왼쪽 눈이 폭풍을 두려워한다. 빙결·폭풍 계열 급소를 확인했다.\n" +
                            "다음 전투에서 아군 피해량 +15% (급소 파악).",
                    gold = (s.gold - 200).coerceAtLeast(0),
                    intelBonusPercent = s.intelBonusPercent + 15,
                    statusMessage = "[이벤트 B] 정찰 선택 — 급소 파악 +15% (금화 -200)"
                )
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 이계 전투
    // ──────────────────────────────────────────────────────────────

    /** 마왕 릴리스와의 결전 준비 */
    fun startDemonLordBattle() {
        updateState { s ->
            if (s.ownedCards.isEmpty()) {
                return@updateState s.copy(
                    battleLog = s.battleLog + "[출전 불가] 보유 동료가 0장입니다. 재계약·보급으로 덱을 먼저 채우세요.",
                    statusMessage = "[출전 불가] 보유 동료 0장 — 재계약(무료)이나 긴급 보급이 필요합니다."
                )
            }
            val penalty = temptationEngine.calculateCombatPenalty(s.temptationState)
            // 친밀도: 출전 카드마다 +4 (승리 시 정산에서 +2)
            val base = creditBattleDeployment(s)
            base.copy(
                enemyName = "마왕 릴리스",
                enemyElement = Element.DARK,
                enemyHp = 25000,
                enemyMaxHp = 25000,
                enemyAtk = 2800,
                isDemonLordBattle = true,
                turn = 1,
                mana = ManaState(1, 1),
                party = freshParty(base),
                retreatInfo = BattleRetreatInfo(turn = 1),
                isAutoBattle = true,
                lastCutIn = null,
                pairUltimateUsedThisBattle = false,
                pairUltimateBanner = null,
                pairUltimateBannerSubtitle = null,
                isChapter2BossBattle = false,
                audioScene = AudioScene.BOSS,
                pendingLootGold = 2500,
                pendingLootGems = 30,
                battleLog = base.battleLog + listOf(
                    "[마왕전 개시] 황혼과 욕망의 마왕 릴리스 — HP 25000 / 공격력 2800",
                    penalty.debuffDescription
                ),
                statusMessage = "[마왕전] 릴리스가 심연의 옥좌에서 일어섰다. (홀림 ${s.combatPenalty.totalStacks}스택 적용)"
            )
        }
    }

    /** 일반 마수 원정 준비 */
    fun startFieldBattle() {
        updateState { s ->
            if (s.ownedCards.isEmpty()) {
                return@updateState s.copy(
                    battleLog = s.battleLog + "[출전 불가] 보유 동료가 0장입니다. 재계약·보급으로 덱을 먼저 채우세요.",
                    statusMessage = "[출전 불가] 보유 동료 0장 — 재계약(무료)이나 긴급 보급이 필요합니다."
                )
            }
            // 친밀도: 출전 카드마다 +4 (승리 시 정산에서 +2)
            val base = creditBattleDeployment(s)
            base.copy(
                enemyName = "차원 틈새의 마수",
                enemyElement = Element.DARK,
                enemyHp = 12000,
                enemyMaxHp = 12000,
                enemyAtk = 900,
                isDemonLordBattle = false,
                turn = 1,
                mana = ManaState(1, 1),
                party = freshParty(base),
                retreatInfo = BattleRetreatInfo(turn = 1),
                isAutoBattle = true,
                lastCutIn = null,
                pairUltimateUsedThisBattle = false,
                pairUltimateBanner = null,
                pairUltimateBannerSubtitle = null,
                isChapter2BossBattle = false,
                audioScene = AudioScene.BATTLE,
                pendingLootGold = 300,
                pendingLootGems = 5,
                battleLog = base.battleLog + "[원정 개시] 차원 틈새의 마수 — HP 12000 / 공격력 900",
                statusMessage = "[원정] 차원 틈새의 마수와 교전을 시작합니다."
            )
        }
    }

    private fun enemyCard(s: TcgGameState): TcgCard = TcgCard(
        id = 900,
        name = s.enemyName,
        element = s.enemyElement,
        baseCost = 10,
        atk = s.enemyAtk,
        def = s.enemyMaxHp,
        starLevel = 5,
        skillText = "심연의 압박"
    )

    /** 전열 공격 */
    fun attackFront() = performAttack(front = true)

    /** 후열 지원 공격 */
    fun attackBack() = performAttack(front = false)

    private fun performAttack(front: Boolean) {
        updateState { s ->
            if (s.enemyHp <= 0) return@updateState s.copy(statusMessage = "[전투 종료] 이미 적을 쓰러뜨렸습니다.")
            val attackers = s.party.filter { it.isAlive && it.isFrontRow == front }
            if (attackers.isEmpty()) {
                return@updateState s.copy(
                    battleLog = s.battleLog + "[공격 불가] ${if (front) "전열" else "후열"}에 생존한 카드가 없습니다. 도감에서 재획득하거나 턴을 넘기세요.",
                    statusMessage = "[공격 불가] ${if (front) "전열" else "후열"} 생존 카드 없음"
                )
            }
            if (s.mana.currentMana < 1) {
                return@updateState s.copy(
                    battleLog = s.battleLog + "[공격 실패] 마나가 부족합니다. (보유 ${s.mana.currentMana}) — '턴 진행'으로 마나를 회복하세요.",
                    statusMessage = "[공격 실패] 마나 부족 (턴 진행 필요)"
                )
            }
            // 턴에 따라 공격자를 순환
            val attacker = attackers[(s.turn - 1) % attackers.size]
            val base = battleEngine.calculateDamageWithTemptation(attacker.card, enemyCard(s), s.combatPenalty)
            val intelMultiplier = 1.0 + (s.intelBonusPercent / 100.0)
            val summonMultiplier = 1.0 + (s.summonBuffPercent / 100.0)
            val finalDamage = (base.finalDamage * intelMultiplier * summonMultiplier).toInt()
            val newEnemyHp = (s.enemyHp - finalDamage).coerceAtLeast(0)
            val newMana = s.mana.copy(currentMana = s.mana.currentMana - 1)

            val logLine = "[턴 ${s.turn}] ${attacker.card.name} → ${s.enemyName} : ${finalDamage} 데미지 " +
                    "(상성 ${base.multiplier}x" +
                    (if (s.intelBonusPercent > 0) ", 급소 +${s.intelBonusPercent}%" else "") +
                    (if (s.summonBuffPercent > 0) ", 현실 소환 +${s.summonBuffPercent}%" else "") +
                    (if (s.combatPenalty.isCharmed) ", 홀림 -${s.combatPenalty.attackReductionPercent}%" else "") +
                    ") · 적 잔여 HP ${newEnemyHp}"

            if (newEnemyHp <= 0) {
                applyVictory(s, newMana, listOf(logLine)).copy(
                    lastEnemyHitDamage = finalDamage,
                    lastEnemyHitTick = s.lastEnemyHitTick + 1,
                    lastAttackerSlot = attacker.slotIndex,
                    lastCutIn = allyCutIn(
                        s = s,
                        attacker = attacker,
                        front = front,
                        damage = finalDamage,
                        multiplier = base.multiplier,
                        critical = base.isCriticalElement,
                        ultimate = base.isUltimateTriggered,
                        lethal = true
                    )
                )
            } else {
                s.copy(
                    enemyHp = newEnemyHp,
                    mana = newMana,
                    lastEnemyHitDamage = finalDamage,
                    lastEnemyHitTick = s.lastEnemyHitTick + 1,
                    lastAttackerSlot = attacker.slotIndex,
                    lastCutIn = allyCutIn(
                        s = s,
                        attacker = attacker,
                        front = front,
                        damage = finalDamage,
                        multiplier = base.multiplier,
                        critical = base.isCriticalElement,
                        ultimate = base.isUltimateTriggered,
                        lethal = false
                    ),
                    battleLog = s.battleLog + logLine,
                    statusMessage = "[공격] ${attacker.card.name} ${finalDamage} 데미지 — 적 HP ${newEnemyHp}/${s.enemyMaxHp}"
                )
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 🤖 자동 전투 · ⚔️ 공격 컷인 (2026-11-16 사용자 요청)
    // ──────────────────────────────────────────────────────────────

    /** 컷인 배너에 쓰는 짧은 이름 (칭호를 뺀 표시 이름) */
    private fun shortName(cardId: Int, fallback: String): String =
        TcgGameViewModel.profileOf(cardId)?.displayName?.takeIf { it.isNotBlank() }
            ?: fallback.substringAfterLast(' ').takeIf { it.isNotBlank() }
            ?: fallback

    /** 다음 컷인 트리거 번호 */
    private fun nextCutInTick(s: TcgGameState): Int = (s.lastCutIn?.tick ?: 0) + 1

    /** 아군 공격 컷인 — 공격자 카드 아트를 중앙에 확대하고 속성 빛 테두리를 두른다 */
    private fun allyCutIn(
        s: TcgGameState,
        attacker: PartyMember,
        front: Boolean,
        damage: Int,
        multiplier: Double,
        critical: Boolean,
        ultimate: Boolean,
        lethal: Boolean
    ): BattleCutIn = BattleCutIn(
        tick = nextCutInTick(s),
        fromEnemy = false,
        kind = if (front) BattleAttackKind.FRONT else BattleAttackKind.BACK,
        attackerCardId = attacker.card.id,
        attackerName = shortName(attacker.card.id, attacker.card.name),
        element = attacker.card.element,
        attackName = if (front) "공격" else "후열 지원 사격",
        attackerSlot = attacker.slotIndex,
        targetName = s.enemyName,
        damage = damage,
        critical = critical,
        multiplier = multiplier,
        ultimate = ultimate,
        lethal = lethal
    )

    /** 적 반격 컷인 — 적 보스 카드 아트가 중앙에 확대되고 아군 피해 슬롯이 흔들린다 */
    private fun enemyCutIn(s: TcgGameState, victim: PartyMember, damage: Int): BattleCutIn = BattleCutIn(
        tick = nextCutInTick(s),
        fromEnemy = true,
        kind = BattleAttackKind.ENEMY_COUNTER,
        attackerCardId = s.enemyCutInCardId,
        attackerName = s.enemyName,
        element = s.enemyElement,
        attackName = "반격",
        attackerSlot = -1,
        targetName = shortName(victim.card.id, victim.card.name),
        targetSlot = victim.slotIndex,
        damage = damage,
        critical = false,
        multiplier = 1.0,
        ultimate = false,
        lethal = victim.hp - damage <= 0
    )

    /** 🤖 자동 전투 ON/OFF */
    fun setAutoBattle(enabled: Boolean) {
        updateState { s ->
            if (s.isAutoBattle == enabled) return@updateState s
            s.copy(
                isAutoBattle = enabled,
                statusMessage = if (enabled) {
                    "🤖 자동 전투 ON — ${BattleAutoDecor.stepIntervalMillis(s.battleSpeed)}ms 간격으로 " +
                            "${BattleAutoPilot.nextAction(s.copy(isAutoBattle = true)).label} 진행"
                } else {
                    "✋ 수동 전투로 전환했습니다. FIGHT · 턴 진행 · 전술로 직접 지휘하세요."
                }
            )
        }
    }

    /** 🤖 자동 전투 토글 (화면 상단 버튼) */
    fun toggleAutoBattle() = setAutoBattle(!_uiState.value.isAutoBattle)

    /** ✋ 자동 전투 종료 (승리·전멸·연출 종료 시) — 전투 상태 문구는 그대로 둔다 */
    fun stopAutoBattle() {
        updateState { s -> if (s.isAutoBattle) s.copy(isAutoBattle = false) else s }
    }

    /** ⏩ 전투 배속 지정 (1x / 2x) — 컷인·타격 이펙트 시간이 함께 줄어든다 */
    fun setBattleSpeed(speed: BattleSpeed) {
        updateState { s ->
            if (s.battleSpeed == speed) return@updateState s
            s.copy(
                battleSpeed = speed,
                statusMessage = "⏩ 전투 배속 ${speed.label} — 컷인 " +
                        "${BattleAutoDecor.cutInMillis(speed)}ms · 자동 스텝 " +
                        "${BattleAutoDecor.stepIntervalMillis(speed)}ms"
            )
        }
    }

    /** ⏩ 배속 토글 (1x ↔ 2x) */
    fun toggleBattleSpeed() = setBattleSpeed(_uiState.value.battleSpeed.toggled())

    /**
     * 자동 전투 1스텝 — [BattleAutoPilot] 이 고른 행동을 실제로 실행한다.
     * 화면의 자동 전투 루프가 일정 템포로 호출하며, 승리·전멸에서는 스스로 멈춘다.
     */
    fun autoBattleStep(): BattleAutoPilot.Action {
        val action = BattleAutoPilot.nextAction(_uiState.value)
        when (action) {
            BattleAutoPilot.Action.PAIR_ULTIMATE -> activatePairUltimate()
            BattleAutoPilot.Action.ATTACK_FRONT -> attackFront()
            BattleAutoPilot.Action.ATTACK_BACK -> attackBack()
            BattleAutoPilot.Action.NEXT_TURN -> nextTurn()
            BattleAutoPilot.Action.STOP -> stopAutoBattle()
        }
        return action
    }

    /**
     * 전투 승리 정산 — 전리품·마왕 토벌·기억 파편·오의 상태 초기화·제2장 전초전 진행
     */
    private fun applyVictory(
        s: TcgGameState,
        newMana: ManaState,
        logLines: List<String>
    ): TcgGameState {
        val rewardGold = s.pendingLootGold
        val rewardGems = s.pendingLootGems

        // ── 친밀도(2026-11-16 개편): 이번 전투에 출전한 카드에 승리 보너스 +2 ──
        // (출전 시 +4는 이미 지급됨 → 승리한 전투 1회 = 총 +6)
        val affinityLogs = mutableListOf<String>()
        s.party.map { it.card.id }.distinct().forEach { id ->
            val card = s.ownedCards.firstOrNull { it.id == id } ?: return@forEach
            val change = AffinityRules.creditBattleVictory(card)
            affinityLogs += "[출전 승리] ${shortName(id, card.name)} 친밀도 ${change.before}→${change.after} " +
                "(+${change.gained} · 누적 출전 ${card.battleCount}회)" +
                (if (change.newlyUnlocked.isNotEmpty()) {
                    " · 🎉 ${change.newlyUnlocked.joinToString("·") { it.displayName }} 개방!"
                } else "")
        }

        // 「기억의 잔향」: 전투 승리로 기억이 지워진 동료에게 기억 파편 +1
        val fragmentLogs = mutableListOf<String>()
        s.ownedCards.forEach { card ->
            if (!card.isMemoryReset) return@forEach
            val gain = memoryRecallEngine.grantFragment(card, 1, MemoryFragmentSource.BATTLE)
            if (gain.isSuccess) fragmentLogs.add(gain.message)
        }

        var state = s.copy(
            enemyHp = 0,
            mana = newMana,
            ownedCards = s.ownedCards.toList(),
            battleLog = s.battleLog + logLines +
                    "[전투 승리] ${s.enemyName} 토벌 완료!" +
                    (if (rewardGold > 0 || rewardGems > 0) " (전리품 금화 ${rewardGold}G · 보석 ${rewardGems})" else "") +
                    fragmentLogs + affinityLogs,
            gold = s.gold + rewardGold,
            gems = s.gems + rewardGems,
            gateEnergy = (s.gateEnergy + GATE_ENERGY_RECOVERY).coerceAtMost(s.gateEnergyMax),
            battleVictories = s.battleVictories + 1,
            playerExp = s.playerExp + PlayerProgress.EXP_BATTLE_VICTORY,
            summonBuffBattles = (s.summonBuffBattles - 1).coerceAtLeast(0),
            summonBuffPercent = if (s.summonBuffBattles - 1 <= 0) 0 else s.summonBuffPercent,
            pairUltimateUsedThisBattle = false,
            frontRowDefensePenaltyPercent = 0,
            audioScene = AudioScene.LOBBY,
            storyStepIndex = if (s.storyStepIndex <= 2) 3 else s.storyStepIndex,
            statusMessage = "[전투 승리] ${s.enemyName}을(를) 쓰러뜨렸습니다. 획득 금화 ${rewardGold}G." +
                    levelLineFor(s.playerExp, PlayerProgress.EXP_BATTLE_VICTORY)
        )

        // ── 메인 루프: 스테이지 클리어 정산 (골드·경험치·티켓·조각·장비 드롭) ──
        val activeStageNumber = s.activeStage
        if (activeStageNumber != null) {
            val stage = StageCatalog.find(activeStageNumber)
            if (stage != null) {
                val alreadyCleared = activeStageNumber in s.stageCleared
                val outcome = StageEngine.settle(stage, alreadyCleared, random)
                var wallet = state.shards
                if (outcome.shardCardId != null && outcome.shardCount > 0) {
                    wallet = wallet.add(outcome.shardCardId, outcome.shardCount)
                }
                var owned = state.ownedCards
                val extraLogs = mutableListOf<String>()
                // 대마왕 스테이지: 토벌하면 서약 파트너로 아군에 합류한다
                val lordId = stage.bossId
                if (stage.bossKind == com.game.tcg.core.model.BossKind.DEMON_LORD &&
                    lordId != null && owned.none { it.id == lordId }
                ) {
                    val lordCard = characterCard(lordId)
                    owned = owned + lordCard
                    // 상실 엔진에도 등록해야 이후 전투에서 쓰러졌을 때 정상적으로 상실 처리된다
                    lifecycleEngine.registerOwnedCard(lordCard)
                    extraLogs += "[서약] ${lordCard.name}이(가) 가면을 벗고 파트너로 합류했습니다. (조각·강화 상태 초기)"
                }
                // 심연 연성 해금 안내
                if (stage.bossKind == com.game.tcg.core.model.BossKind.MID_BOSS && !alreadyCleared) {
                    val bossName = stage.bossRecord?.name ?: stage.enemyName
                    extraLogs += "[심연 연성] '${bossName}'이(가) 성장 화면의 연성 목록에 추가되었습니다."
                }
                val drop = outcome.equipmentDrop
                val dropLogs = if (drop != null) listOf("[장비 드롭] ${drop.name} (${drop.slot.displayName}) ${drop.statSummary}") else emptyList()
                state = state.copy(
                    gold = state.gold + outcome.gold,
                    gems = state.gems + outcome.gems,
                    packTickets = state.packTickets + outcome.tickets,
                    playerExp = state.playerExp + outcome.exp,
                    shards = wallet,
                    equipment = state.equipment + listOfNotNull(drop),
                    ownedCards = owned,
                    stageCleared = if (alreadyCleared) state.stageCleared else state.stageCleared + activeStageNumber,
                    selectedStage = outcome.nextStageNumber ?: activeStageNumber,
                    activeStage = null,
                    lastStageOutcome = outcome,
                    stageMessage = outcome.message,
                    battleLog = state.battleLog + outcome.message + extraLogs + dropLogs,
                    statusMessage = outcome.message + if (extraLogs.isNotEmpty()) " " + extraLogs.first() else ""
                )
            }
        }

        if (s.isDemonLordBattle) {
            val afterDefeat = temptationEngine.onDemonLordDefeated(
                DemonLordRegistry.LILITH.copy(),
                state.temptationState
            )
            state = state.copy(
                temptationState = afterDefeat,
                combatPenalty = temptationEngine.calculateCombatPenalty(afterDefeat),
                battleLog = state.battleLog + "[토벌 완료] 릴리스가 가면을 벗고 서약 파트너로 합류합니다. 저주 스택 -3.",
                statusMessage = "[마왕 토벌] 릴리스 정벌! 황혼이 걷히고 그녀가 서약 파트너로 합류했습니다."
            )
        }

        if (s.isChapter2BossBattle) {
            state = state.copy(
                isChapter2BossBattle = false,
                chapter2Message = "[전초전 승리] 제1군단장 그림자 기사가 갑주를 풀고 무릎을 꿇었다. " +
                        "\"전초전일 뿐이야… 제2장은 아직 열리지 않았다.\" — 제2장 본편 진입 조건이 열렸습니다.",
                battleLog = state.battleLog + "[전초전 승리] 제1군단장 그림자 기사의 가면이 갈라졌다. 제2장 진입 조건 충족.",
                statusMessage = "[전초전 승리] 제1군단장 그림자 기사 격퇴 — 제2장이 곧 열립니다."
            )
        }

        if (state.storyStepIndex >= StoryScript.STEPS.size - 1 && !state.chapter2PreludeOpened) {
            val node = ChapterTwoScript.SHADOW_KNIGHT_PRELUDE
            state = state.copy(
                chapter2Message = "[${node.chapter}] ${node.title} — 제1장 5화 이후 열리는 다음 여정 노드입니다."
            )
        }

        return state
    }

    /** 턴 진행 — 마나 회복, 후퇴 제한 해제, 적 반격 */
    fun nextTurn() {
        updateState { s ->
            if (s.enemyHp <= 0) return@updateState s.copy(statusMessage = "[전투 종료] 적이 없습니다. 새 원정을 시작하세요.")
            val nextTurnNo = s.turn + 1
            val mana = manaEngine.replenishTurnMana(manaEngine.calculateMaxManaForTurn(nextTurnNo))
            val targets = s.party.filter { it.isAlive && it.isFrontRow }.ifEmpty { s.party.filter { it.isAlive } }
            if (targets.isEmpty()) {
                return@updateState s.copy(
                    turn = nextTurnNo,
                    mana = mana,
                    statusMessage = "[전투 불능] 생존 카드가 없습니다. 도감에서 재획득이 필요합니다."
                )
            }
            val victim = targets[(nextTurnNo - 1) % targets.size]
            val raw = s.enemyAtk
            val taken = temptationEngine.applyDamageTakenPenalty(raw, s.combatPenalty)
            val frontPenalty = if (victim.isFrontRow) s.frontRowDefensePenaltyPercent else 0
            val mitigated = (taken * (100 - s.defenseBonusPercent + frontPenalty) / 100.0).toInt()
            val newHp = (victim.hp - mitigated).coerceAtLeast(0)

            var post = s.copy(
                turn = nextTurnNo,
                mana = mana,
                lastAllyHitDamage = mitigated,
                lastAllyHitSlot = victim.slotIndex,
                lastAllyHitTick = s.lastAllyHitTick + 1,
                lastCutIn = enemyCutIn(s, victim, mitigated),
                retreatInfo = s.retreatInfo.copy(turn = nextTurnNo, retreatUsedThisTurn = false),
                lastRetreatMessage = null,
                party = s.party.map { if (it.slotIndex == victim.slotIndex) it.copy(hp = newHp) else it },
                battleLog = s.battleLog + ("[턴 $nextTurnNo] ${s.enemyName}의 반격 → ${victim.card.name} ${mitigated} 피해 " +
                        "(받는 피해 +${s.combatPenalty.damageTakenIncreasePercent}%, 방어 보너스 -${s.defenseBonusPercent}%" +
                        (if (frontPenalty > 0) ", 전열 결손 방어 -$frontPenalty%" else "") + ") · 잔여 HP $newHp/${victim.maxHp}"),
                statusMessage = "[턴 $nextTurnNo] 마나 ${mana.currentMana}/${mana.maxMana} 회복 · ${victim.card.name} 피해 ${mitigated}"
            )

            if (newHp <= 0 && post.reincarnationPool.none { it.templateId == victim.card.id }) {
                post = applyDeath(post, victim)
            }
            post
        }
    }

    /** 카드 사망 → 영구 상실 · 환생 풀 등록 */
    private fun applyDeath(s: TcgGameState, victim: PartyMember): TcgGameState {
        lifecycleEngine.registerOwnedCard(victim.card)
        val result = lifecycleEngine.onCardDied(
            cardId = victim.card.id,
            causeOfDeath = "전투 중 HP 0 (턴 ${s.turn}, ${s.enemyName})",
            diedAtTurn = s.turn
        )
        if (result.isSuccess) {
            // 쓰러진 카드의 장비는 소지품으로 회수한다 (재획득 카드는 초기 상태로 돌아오므로)
            s.equipment.filter { it.equippedTo == victim.card.id }.forEach { it.equippedTo = null }
        }
        return if (result.isSuccess) {
            s.copy(
                reincarnationPool = lifecycleEngine.collection.reincarnationQueue(),
                ownedCards = s.ownedCards.filterNot { it.id == victim.card.id },
                // 영구 상실 — 편성·대기열에서도 즉시 빠진다.
                // (남겨 두면 다음 전투 개시 시 HP 가 되살아나 상실 규칙이 무력화된다)
                party = s.party.filterNot { it.card.id == victim.card.id },
                battleReserve = s.battleReserve.filterNot { it.id == victim.card.id },
                equipment = s.equipment.toList(),
                battleLog = s.battleLog + result.message.split("\n"),
                statusMessage = "[영구 상실] ${victim.card.name}이(가) 쓰러졌습니다. 기억·강화 초기화 — 재획득 필요."
            )
        } else {
            s.copy(
                battleLog = s.battleLog + result.message,
                statusMessage = result.message
            )
        }
    }

    /** 환생 풀의 카드 재획득 → 빈 슬롯에 복귀 */
    fun reacquire(cardId: Int) {
        updateState { s ->
            val result: AcquisitionResult = lifecycleEngine.acquireCard(cardId)
            if (!result.isSuccess || result.card == null) {
                return@updateState s.copy(
                    lastAcquisition = result.message,
                    statusMessage = result.message
                )
            }
            val card = result.card
            val slot = DeckAutoBuilder.firstEmptySlot(s.party.map { it.slotIndex }, s.unlockedDeckSize)
                ?: (0 until s.unlockedDeckSize).firstOrNull { idx -> s.party.none { it.slotIndex == idx } }
                ?: 0
            val maxHp = TcgGameViewModel.maxHpOf(card) + EquipmentEngine.hpBonus(card.id, s.equipment)
            val member = PartyMember(card = card, slotIndex = slot, isFrontRow = slot < 3, hp = maxHp, maxHp = maxHp)
            s.copy(
                party = (s.party.filterNot { it.slotIndex == slot || it.card.id == card.id } + member).sortedBy { it.slotIndex },
                ownedCards = lifecycleEngine.collection.ownedCards(),
                reincarnationPool = lifecycleEngine.collection.reincarnationQueue(),
                lastAcquisition = result.message,
                battleLog = s.battleLog + "[재획득] ${card.name}이(가) 초기 상태로 복귀했습니다. (영혼 ${result.card.reincarnationCount}회차 흔적)",
                statusMessage = "[재획득] ${card.name} 재계약 완료 — ${slot}번 슬롯 복귀"
            )
        }
    }

    /**
     * 환생 풀에 대기 중인 카드를 **전부 무료 재계약**한다.
     * (기본 편성 순서대로 전열 → 후열 빈 슬롯에 들어간다. 비용 없음)
     */
    fun reclaimAllLostCards(): Int {
        val pending = _uiState.value.reincarnationPool.map { it.templateId }
        pending.forEach { reacquire(it) }
        if (pending.isNotEmpty()) {
            updateState { s ->
                s.copy(
                    statusMessage = "[무료 재계약] 잃었던 동료 ${pending.size}명을 되찾았습니다. (보유 ${s.ownedCards.size}장)"
                )
            }
        }
        return pending.size
    }

    /**
     * 무료 긴급 보급 — 보유 카드가 3장 미만이고 환생 풀·카드팩·조각 연성 어느 것도 불가능할 때만
     * 지원 카드를 지급한다([EmergencySupplyEngine]). 지급 카드는 미보유 카드 중에서만 고른다.
     */
    fun claimEmergencySupply(): SupplyOffer? {
        var offered: SupplyOffer? = null
        updateState { s ->
            val offer = s.supplyOffer
            offered = offer
            if (!offer.isOffered || offer.cardIds.isEmpty()) {
                return@updateState s.copy(
                    supplyMessage = offer.message.ifBlank { "[긴급 보급] ${offer.reason}" },
                    statusMessage = "[긴급 보급] ${offer.reason}"
                )
            }
            var party = s.party
            val newCards = offer.cardIds.map { id ->
                val card = characterCard(id)
                lifecycleEngine.registerOwnedCard(card)
                val slot = DeckAutoBuilder.firstEmptySlot(party.map { it.slotIndex }, s.unlockedDeckSize)
                if (slot != null) {
                    val maxHp = TcgGameViewModel.maxHpOf(card) + EquipmentEngine.hpBonus(card.id, s.equipment)
                    party = (party + PartyMember(card, slot, DeckRules.isFrontSlot(slot), maxHp, maxHp))
                        .sortedBy { it.slotIndex }
                }
                card
            }
            val names = newCards.joinToString(", ") { it.name }
            s.copy(
                ownedCards = s.ownedCards + newCards,
                party = party,
                supplyClaims = s.supplyClaims + 1,
                supplyMessage = "[긴급 보급] ${names} — ${newCards.size}명이 합류했습니다. (보유 ${s.ownedCards.size} → ${s.ownedCards.size + newCards.size}장)",
                battleLog = s.battleLog + "[긴급 보급] ${names} 무료 지급 (${newCards.size}명)",
                statusMessage = "[긴급 보급] ${names} 합류 — 이제 다시 출전할 수 있습니다."
            )
        }
        return offered
    }

    /** 전술 후퇴 — 턴당 1회, 마나 1 (마왕전 2), 홀림 3스택 이상이면 명령 불복 */
    fun retreat(slotIndex: Int): RetreatResult? {
        var resultOut: RetreatResult? = null
        updateState { s ->
            val battleState = toBattleState(s)
            val result = retreatEngine.retreatCard(
                battleState = battleState,
                slotIndex = slotIndex,
                temptationStacks = s.combatPenalty.totalStacks,
                isDemonLordBattle = s.isDemonLordBattle
            )
            resultOut = result
            if (!result.isSuccess) {
                return@updateState s.copy(
                    lastRetreatMessage = result.message,
                    battleLog = s.battleLog + result.message,
                    statusMessage = result.message
                )
            }
            val card = result.retreatedCard
            s.copy(
                party = if (card != null) s.party.filterNot { it.slotIndex == slotIndex } else s.party,
                battleReserve = if (card != null) s.battleReserve + card else s.battleReserve,
                mana = battleState.mana,
                retreatInfo = battleState.retreatInfo,
                lastRetreatMessage = result.message,
                battleLog = s.battleLog + result.message.split("\n"),
                statusMessage = result.message
            )
        }
        return resultOut
    }

    /** 파티 긴급 철수 — 전리품 포기, 생존 카드 전원 귀환 */
    fun emergencyRetreat(): EmergencyRetreatResult? {
        var resultOut: EmergencyRetreatResult? = null
        updateState { s ->
            val battleState = toBattleState(s)
            val result = retreatEngine.emergencyPartyRetreat(battleState)
            resultOut = result
            if (!result.isSuccess) {
                return@updateState s.copy(
                    lastRetreatMessage = result.message,
                    statusMessage = result.message
                )
            }
            s.copy(
                party = emptyList(),
                battleReserve = result.rescuedCards,
                mana = battleState.mana,
                retreatInfo = battleState.retreatInfo,
                lootGold = 0,
                lootGems = 0,
                lastRetreatMessage = result.message,
                battleLog = s.battleLog + result.message.split("\n"),
                statusMessage = "[긴급 철수] 생존 ${result.rescuedCardCount}장 귀환 · 전리품 포기"
            )
        }
        return resultOut
    }

    /** 후퇴 대기열 카드를 슬롯에 재투입 */
    fun redeploy(cardId: Int) {
        updateState { s ->
            val card = s.battleReserve.firstOrNull { it.id == cardId } ?: return@updateState s
            val occupied = s.party.map { it.slotIndex }.toSet()
            val slot = DeckAutoBuilder.firstEmptySlot(occupied, s.unlockedDeckSize) ?: return@updateState s.copy(
                statusMessage = "[재투입 불가] 빈 슬롯이 없습니다. (${s.unlockedDeckSize}장 편성)"
            )
            val maxHp = TcgGameViewModel.maxHpOf(card) + EquipmentEngine.hpBonus(card.id, s.equipment)
            s.copy(
                party = (s.party + PartyMember(card, slot, DeckRules.isFrontSlot(slot), maxHp, maxHp)).sortedBy { it.slotIndex },
                battleReserve = s.battleReserve.filterNot { it.id == cardId },
                battleLog = s.battleLog + "[재투입] ${card.name} → ${DeckRules.slotLabel(slot)} 슬롯 (${if (DeckRules.isFrontSlot(slot)) "전열" else "후열"})",
                statusMessage = "[재투입] ${card.name}이(가) ${slot}번 슬롯에 복귀했습니다."
            )
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 덱 편성 (비주얼 DECK) — 9장 기본 · 최대 12장, 레벨 해금
    // ──────────────────────────────────────────────────────────────

    /**
     * 자동 편성 — 보유 카드 중 전투력 상위 N장을 지금 해금된 슬롯(N = 9~12)에 채운다.
     * DEF가 높은 3장은 전열, 나머지는 후열·대기 슬롯으로 들어간다.
     */
    fun autoEquipParty() {
        updateState { s ->
            if (s.ownedCards.isEmpty()) {
                return@updateState s.copy(statusMessage = "[자동 편성] 보유 캐릭터가 없습니다. 신규 연성으로 계약하세요.")
            }
            val slots = DeckAutoBuilder.autoEquip(s.ownedCards, s.unlockedDeckSize)
            val newParty = slots.mapIndexedNotNull { slotIndex, cardId ->
                cardId?.let { id ->
                    s.ownedCards.firstOrNull { it.id == id }?.let { card ->
                        val maxHp = TcgGameViewModel.maxHpOf(card) + EquipmentEngine.hpBonus(card.id, s.equipment)
                        PartyMember(card, slotIndex, DeckRules.isFrontSlot(slotIndex), maxHp, maxHp)
                    }
                }
            }
            val frontCount = newParty.count { it.isFrontRow }
            s.copy(
                party = newParty,
                battleReserve = s.battleReserve.filterNot { r -> newParty.any { it.card.id == r.id } },
                battleLog = s.battleLog + "[자동 편성] 전투력 상위 ${newParty.size}장 장착 (전열 $frontCount · 후열 ${newParty.size - frontCount})",
                statusMessage = "[자동 편성] ${newParty.size}장 장착 — 총 전투력 ${DeckAutoBuilder.displayTotalPower(newParty.map { it.card })}"
            )
        }
    }

    /** 전체 해제 — 출전 덱을 모두 비우고 장착 카드를 대기열로 돌린다. */
    fun unloadAllParty() {
        updateState { s ->
            if (s.party.isEmpty()) {
                return@updateState s.copy(statusMessage = "[전체 해제] 이미 덱이 비어 있습니다.")
            }
            val removed = s.party.map { it.card }
            s.copy(
                party = emptyList(),
                battleReserve = (s.battleReserve + removed).distinctBy { it.id },
                battleLog = s.battleLog + "[전체 해제] ${removed.size}장을 대기열로 돌렸습니다.",
                statusMessage = "[전체 해제] 덱 ${removed.size}장 해제 — 빈 슬롯에 카드를 탭해 다시 채우세요."
            )
        }
    }

    /**
     * 빈 슬롯에 즉시 장착 — 해금된 슬롯(0~8/0~11) 중 가장 앞자리에 넣는다.
     * 이미 들어 있는 카드·가득 참·미보유·잠금 해제 전 슬롯은 상태 메시지로 돌려준다.
     */
    fun equipToFirstEmptySlot(cardId: Int) {
        updateState { s ->
            val card = s.ownedCards.firstOrNull { it.id == cardId }
                ?: return@updateState s.copy(statusMessage = "[장착 실패] 아직 계약하지 않은 캐릭터입니다. (도감 #$cardId)")
            if (s.party.any { it.card.id == cardId }) {
                return@updateState s.copy(statusMessage = "[장착] ${card.name}은(는) 이미 편성되어 있습니다.")
            }
            val slot = DeckAutoBuilder.firstEmptySlot(s.party.map { it.slotIndex }, s.unlockedDeckSize)
                ?: return@updateState s.copy(
                    statusMessage = "[장착 실패] ${s.unlockedDeckSize}칸이 모두 찼습니다. " +
                            "덱에서 카드를 눌러 해제하거나 Lv.${s.nextDeckUnlock?.level ?: s.playerLevel}에서 1칸 더 열립니다."
                )
            val maxHp = TcgGameViewModel.maxHpOf(card) + EquipmentEngine.hpBonus(card.id, s.equipment)
            val member = PartyMember(card, slot, DeckRules.isFrontSlot(slot), maxHp, maxHp)
            s.copy(
                party = (s.party + member).sortedBy { it.slotIndex },
                battleReserve = s.battleReserve.filterNot { it.id == cardId },
                battleLog = s.battleLog + "[편성] ${card.name} → ${DeckRules.slotLabel(slot)} 슬롯",
                statusMessage = "[편성] ${card.name}을(를) ${DeckRules.slotLabel(slot)} 슬롯에 장착했습니다. " +
                        "(${s.party.size + 1}/${s.unlockedDeckSize}장)"
            )
        }
    }

    /** 컬렉션 카드 탭 — 장착되어 있으면 해제, 아니면 빈 슬롯에 장착. */
    fun toggleEquipCard(cardId: Int) {
        val equipped = _uiState.value.party.any { it.card.id == cardId }
        if (equipped) {
            undeployFromParty(cardId)
        } else {
            equipToFirstEmptySlot(cardId)
        }
    }

    /** 슬롯 단위 해제 (덱 상단 슬롯 탭) */
    fun unloadSlot(slotIndex: Int) {
        val member = _uiState.value.party.firstOrNull { it.slotIndex == slotIndex } ?: return
        undeployFromParty(member.card.id)
    }

    /** 훈련(연습) — 경험치 +120. 덱 해금(Lv.5/10/15)을 바로 확인할 수 있게 둔 테스트·확인용 경로. */
    fun trainForExp(amount: Int = PlayerProgress.EXP_TRAINING) {
        grantPlayerExp(amount)
    }

    /** 경험치 획득 — 레벨이 오르면 덱 슬롯 해금을 함께 알린다. */
    fun grantPlayerExp(amount: Int) {
        if (amount <= 0) return
        updateState { s ->
            val beforeLevel = s.playerLevel
            val beforeSize = s.unlockedDeckSize
            val gained = s.playerExp + amount
            val afterLevel = PlayerProgress.levelForExp(gained)
            val afterSize = DeckRules.deckSizeForLevel(afterLevel)
            val levelLine = if (afterLevel > beforeLevel) {
                "[레벨 업] Lv.$beforeLevel → Lv.$afterLevel " +
                        if (afterSize > beforeSize) "— 덱 슬롯 ${afterSize}장 해금!" else ""
            } else {
                "[경험치] +$amount (Lv.$afterLevel · ${gained % PlayerProgress.EXP_PER_LEVEL}/${PlayerProgress.EXP_PER_LEVEL})"
            }
            s.copy(
                playerExp = gained,
                battleLog = if (afterLevel > beforeLevel) s.battleLog + levelLine else s.battleLog,
                statusMessage = levelLine
            )
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 전투 편성 — 보유 캐릭터를 출전 덱 슬롯(전열 3 + 후열·대기)에 직접 소환
    // ──────────────────────────────────────────────────────────────

    /**
     * 보유 캐릭터를 출전 덱 슬롯(0 ~ 해금 슬롯-1)에 배치한다. 같은 캐릭터가 다른 슬롯에 있으면 그 자리에서 빠진다.
     * 0~2 = 전열, 3 이상 = 후열·대기.
     */
    fun deployToParty(cardId: Int, slotIndex: Int) {
        updateState { s ->
            if (slotIndex !in 0 until s.unlockedDeckSize) {
                return@updateState s.copy(
                    statusMessage = if (slotIndex in 0 until DeckRules.MAX_DECK_SIZE) {
                        "[편성 실패] ${DeckRules.slotLabel(slotIndex)} 슬롯은 Lv.${DeckRules.requiredLevelForSlot(slotIndex)}에 열립니다. (현재 Lv.${s.playerLevel} · ${s.unlockedDeckSize}장)"
                    } else {
                        "[편성 실패] 슬롯 번호는 0~${DeckRules.MAX_DECK_SIZE - 1}만 가능합니다."
                    }
                )
            }
            val card = s.ownedCards.firstOrNull { it.id == cardId }
                ?: return@updateState s.copy(statusMessage = "[편성 실패] 아직 계약하지 않은 캐릭터입니다. (도감 #$cardId)")
            val maxHp = TcgGameViewModel.maxHpOf(card) + EquipmentEngine.hpBonus(card.id, s.equipment)
            val others = s.party.filterNot { it.slotIndex == slotIndex || it.card.id == cardId }
            val member = PartyMember(card, slotIndex, DeckRules.isFrontSlot(slotIndex), maxHp, maxHp)
            val newParty = (others + member).sortedBy { it.slotIndex }
            val rowName = if (DeckRules.isFrontSlot(slotIndex)) "전열" else "후열"
            s.copy(
                party = newParty,
                battleLog = s.battleLog + "[편성] ${card.name} → $rowName ${slotIndex}번 슬롯 (HP $maxHp)",
                statusMessage = "[편성] ${card.name}을(를) $rowName ${slotIndex}번 슬롯에 소환했습니다."
            )
        }
    }

    /** 현재 전투 편성에서 카드를 빼고 대기열로 보낸다 (슬롯 비우기) */
    fun undeployFromParty(cardId: Int) {
        updateState { s ->
            val member = s.party.firstOrNull { it.card.id == cardId } ?: return@updateState s
            val rowName = if (member.isFrontRow) "전열" else "후열"
            s.copy(
                party = s.party.filterNot { it.card.id == cardId },
                battleReserve = (s.battleReserve + member.card).distinctBy { it.id },
                battleLog = s.battleLog + "[편성] ${member.card.name} → $rowName ${member.slotIndex}번 슬롯에서 대기열로",
                statusMessage = "[편성] ${member.card.name}을(를) 대기열로 보냈습니다."
            )
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 메인 루프 ①: 스테이지 캠페인 — 클리어 → 보상 → 다음 스테이지
    // ──────────────────────────────────────────────────────────────

    /** 스테이지 목록에서 스테이지 선택 (상세·보상 미리보기) */
    fun selectStage(stageNumber: Int) {
        updateState { s ->
            val stage = StageCatalog.find(stageNumber)
                ?: return@updateState s.copy(stageMessage = "[스테이지] ${stageNumber}번은 없습니다.")
            s.copy(
                selectedStage = stage.number,
                stageMessage = "[스테이지 ${stage.number}] ${stage.name} · ${stage.enemyName} · ${stage.reward.preview}",
                statusMessage = if (isStageUnlockedFor(s, stage.number)) {
                    "[스테이지 ${stage.number}] ${stage.name} — 권장 전투력 ${stage.recommendedPower} (내 덱 ${s.deckTotalPower})"
                } else {
                    "[스테이지 ${stage.number}] 아직 잠겨 있습니다. ${stage.number - 1}번을 먼저 클리어하세요."
                }
            )
        }
    }

    private fun isStageUnlockedFor(s: TcgGameState, stageNumber: Int): Boolean =
        StageEngine.isUnlocked(stageNumber, s.stageCleared)

    /**
     * 스테이지 출전 — 해당 스테이지의 적 편성으로 VS 전투 화면을 연다.
     * @return 출전 성공 여부
     */
    fun startStageBattle(stageNumber: Int = 0): Boolean {
        var started = false
        updateState { s ->
            val number = if (stageNumber <= 0) s.frontStageNumber else stageNumber
            val stage = StageCatalog.find(number)
                ?: return@updateState s.copy(statusMessage = "[스테이지] ${number}번은 없습니다.")
            if (!isStageUnlockedFor(s, number)) {
                return@updateState s.copy(
                    stageMessage = "[잠김] ${number}번 스테이지는 ${number - 1}번 클리어 후 열립니다.",
                    statusMessage = "[스테이지] ${number}번은 아직 잠겨 있습니다."
                )
            }
            if (s.party.isEmpty()) {
                return@updateState s.copy(
                    stageMessage = "[출전 불가] 출전 덱이 비어 있습니다. 잃은 카드는 무료 재계약, 없으면 긴급 보급으로 채울 수 있습니다.",
                    statusMessage = if (s.ownedCards.isEmpty()) {
                        "[스테이지] 보유 동료 0장 — 무료 재계약이나 긴급 보급으로 덱을 채우세요."
                    } else {
                        "[스테이지] 덱 편성에서 카드를 먼저 배치하세요."
                    }
                )
            }
            started = true
            // 친밀도: 이번 출전으로 출전한 카드마다 +4 (승리하면 정산에서 +2 더)
            val base = creditBattleDeployment(s)
            val bossScene = when (stage.bossKind) {
                com.game.tcg.core.model.BossKind.MID_BOSS -> AudioScene.BOSS
                com.game.tcg.core.model.BossKind.DEMON_LORD -> AudioScene.BOSS
                else -> AudioScene.BATTLE
            }
            val bossLine = when (stage.bossKind) {
                com.game.tcg.core.model.BossKind.MID_BOSS -> " · 중간 보스 관문"
                com.game.tcg.core.model.BossKind.DEMON_LORD -> " · 대마왕 관문"
                else -> ""
            }
            base.copy(
                enemyName = stage.enemyName,
                enemyElement = stage.enemyElement,
                enemyHp = stage.enemyHp,
                enemyMaxHp = stage.enemyHp,
                enemyAtk = stage.enemyAtk,
                isDemonLordBattle = false,
                isChapter2BossBattle = false,
                activeStage = number,
                selectedStage = number,
                turn = 1,
                mana = ManaState(1, 1),
                party = freshParty(base),
                retreatInfo = BattleRetreatInfo(turn = 1),
                isAutoBattle = true,
                lastCutIn = null,
                pairUltimateUsedThisBattle = false,
                pairUltimateBanner = null,
                pairUltimateBannerSubtitle = null,
                pendingLootGold = 0,
                pendingLootGems = 0,
                currentScreen = LobbyDestination.BATTLE,
                audioScene = bossScene,
                stageMessage = "[스테이지 $number] ${stage.name} — ${stage.enemyName}",
                battleLog = base.battleLog + listOf(
                    "[스테이지 $number] ${stage.chapter}장 ${stage.chapterName} · ${stage.name}$bossLine",
                    "${stage.enemyName} — HP ${stage.enemyHp} / 공격력 ${stage.enemyAtk} / 속성 ${stage.enemyElement.displayName}",
                    "권장 전투력 ${stage.recommendedPower} · 내 덱 ${s.deckTotalPower} (${StageEngine.readiness(s.deckTotalPower, stage).label})"
                ),
                statusMessage = "[스테이지 $number] ${stage.name} 출전 — ${StageEngine.readiness(s.deckTotalPower, stage).label}"
            )
        }
        return started
    }

    /** 지금 도전할 앞 스테이지로 바로 출전 */
    fun startFrontStageBattle(): Boolean = startStageBattle()

    // ──────────────────────────────────────────────────────────────
    // 메인 루프 ②: 카드팩 상점 — 구매·개봉 → 신규 캐릭터 / 중복 조각
    // ──────────────────────────────────────────────────────────────

    /** 이 팩을 지금 살 수 있는지 (🎟 티켓 우선) */
    fun canBuyPack(packId: String): Boolean {
        val s = _uiState.value
        val pack = PackCatalog.find(packId) ?: return false
        return if (pack.ticketCost > 0 && s.packTickets >= pack.ticketCost) true else s.gold >= pack.goldCost
    }

    /** 카드팩 구매 + 개봉 (티켓이 있으면 티켓을 먼저 쓴다) */
    fun openPack(packId: String): PackOpenResult? {
        var resultOut: PackOpenResult? = null
        updateState { s ->
            val pack = PackCatalog.find(packId)
                ?: return@updateState s.copy(shopMessage = "[상점] 없는 상품입니다. ($packId)")
            val useTicket = pack.ticketCost > 0 && s.packTickets >= pack.ticketCost
            val goldCost = if (useTicket) 0 else pack.goldCost
            if (!useTicket && s.gold < pack.goldCost) {
                return@updateState s.copy(
                    shopMessage = "[구매 불가] ${pack.name} — ${goldCost}G가 필요합니다. (보유 ${s.gold}G · 🎟 ${s.packTickets})",
                    statusMessage = "[상점] 골드가 부족합니다."
                )
            }
            val opened = packShopEngine.open(pack, s.ownedCardIds, random)
            val actualTickets = if (useTicket) pack.ticketCost else 0
            val applied = opened.copy(goldSpent = goldCost, ticketsSpent = actualTickets)
            resultOut = applied

            var wallet = s.shards
            opened.draws.filter { !it.isNew && it.shards > 0 }.forEach { draw ->
                wallet = wallet.add(draw.cardId, draw.shards)
            }
            val newCards = opened.newCardIds.map { TcgGameViewModel.characterCard(it) }
            newCards.forEach { lifecycleEngine.registerOwnedCard(it) }
            val ticketLine = if (useTicket) " (🎟 ${pack.ticketCost} 사용)" else " (${goldCost}G)"
            s.copy(
                gold = s.gold - goldCost + opened.totalGold,
                packTickets = s.packTickets - actualTickets,
                ownedCards = (s.ownedCards + newCards).distinctBy { it.id },
                shards = wallet,
                lastPackOpen = applied,
                packOpening = packOpeningEngine.start(applied),
                shopMessage = opened.message + ticketLine,
                statusMessage = opened.message + ticketLine
            )
        }
        return resultOut
    }

    // ──────────────────────────────────────────────────────────────
    // 카드팩 개봉 연출 — 봉인 카드팩 → 카드 1장씩 → 전체 그리드
    // (구매·개봉 판정은 [openPack]이 끝내고, 이 단계들은 연출만 움직인다)
    // ──────────────────────────────────────────────────────────────

    /** 봉인된 카드팩을 터치로 개봉 — 첫 카드가 등장한다 */
    fun tearPackSeal() {
        updateState { s ->
            val next = packOpeningEngine.tearSeal(s.packOpening)
            if (next == s.packOpening) return@updateState s
            s.copy(
                packOpening = next,
                statusMessage = "[개봉] ${next.packName} — 카드 ${next.total}장이 빛을 뿌립니다."
            )
        }
    }

    /** 다음 카드 1장 공개 (마지막 카드 뒤에는 전체 보기로) */
    fun revealNextPackDraw() {
        updateState { s ->
            val next = packOpeningEngine.revealNext(s.packOpening)
            if (next == s.packOpening) return@updateState s
            s.copy(packOpening = next)
        }
    }

    /** 남은 카드를 한 번에 펼치기 (전체 그리드) */
    fun revealAllPackDraws() {
        updateState { s ->
            val next = packOpeningEngine.revealAll(s.packOpening)
            if (next == s.packOpening) return@updateState s
            s.copy(packOpening = next)
        }
    }

    /**「🎬 개봉 연출 다시 보기」 — 마지막 개봉 결과로 봉인 카드팩부터 다시 연출한다 */
    fun replayPackOpening() {
        val result = _uiState.value.lastPackOpen ?: return
        updateState { s -> s.copy(packOpening = packOpeningEngine.start(result)) }
    }

    /** 개봉 연출 닫기 — 결과는 상점 화면 아래 목록에 그대로 남는다 */
    fun closePackOpening() {
        updateState { s ->
            if (!s.packOpening.isActive) return@updateState s
            s.copy(packOpening = packOpeningEngine.close())
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 서브 플로우 ①: 카드 강화 (골드 + 조각 → Lv.1~10, ATK/DEF 상승)
    // ──────────────────────────────────────────────────────────────

    fun selectGrowthCard(cardId: Int) {
        updateState { s ->
            if (s.ownedCards.none { it.id == cardId }) {
                return@updateState s.copy(growthMessage = "[강화] 보유하지 않은 카드입니다. (도감 #$cardId)")
            }
            s.copy(growthCardId = cardId, growthMessage = null)
        }
    }

    /** 선택한(또는 지정한) 카드를 강화한다 */
    fun upgradeCard(cardId: Int = 0): UpgradeResult? {
        var resultOut: UpgradeResult? = null
        updateState { s ->
            val targetId = if (cardId > 0) cardId else s.growthCard?.id ?: 0
            val card = s.ownedCards.firstOrNull { it.id == targetId }
                ?: return@updateState s.copy(growthMessage = "[강화] 강화할 카드가 없습니다. 보유 카드를 먼저 선택하세요.")
            val result = upgradeEngine.upgrade(card, s.gold, s.shards)
            resultOut = result
            if (!result.isSuccess) {
                return@updateState s.copy(growthMessage = "[강화 불가] ${result.failureReason}")
            }
            val newMaxHp = TcgGameViewModel.maxHpOf(card) + EquipmentEngine.hpBonus(card.id, s.equipment)
            s.copy(
                ownedCards = s.ownedCards.map { if (it.id == card.id) card else it },
                party = s.party.map { member ->
                    if (member.card.id == card.id) member.copy(card = card, hp = newMaxHp, maxHp = newMaxHp) else member
                },
                gold = s.gold - result.goldSpent,
                shards = result.walletAfter,
                growthCardId = card.id,
                growthMessage = result.message,
                statusMessage = result.message
            )
        }
        return resultOut
    }

    // ──────────────────────────────────────────────────────────────
    // 서브 플로우 ②: 연성(확정 계약) · 초월(조각 한계 돌파)
    // ──────────────────────────────────────────────────────────────

    /** 미보유 캐릭터(또는 해금된 중간 보스)를 조각으로 확정 연성한다 */
    fun craftCharacter(cardId: Int): CraftResult? {
        var resultOut: CraftResult? = null
        updateState { s ->
            val record = CardCatalog.find(cardId)
                ?: return@updateState s.copy(growthMessage = "[연성] 등록되지 않은 카드입니다. (#$cardId)")
            if (s.ownedCards.any { it.id == cardId }) {
                return@updateState s.copy(growthMessage = "[연성] 이미 보유한 카드입니다. (${record.fullName})")
            }
            val isMidBoss = com.game.tcg.core.model.MidBossCatalog.isMidBoss(cardId)
            val kind = if (isMidBoss) CraftKind.MID_BOSS else CraftKind.STANDARD
            if (isMidBoss) {
                val stage = AdvancedSynthesisEngine.midBossStageOf(cardId)
                if (stage == null || stage !in s.stageCleared) {
                    return@updateState s.copy(
                        growthMessage = "[심연 연성] ${record.name}은(는) 스테이지 $stage 보스 격파 후 연성할 수 있습니다."
                    )
                }
            } else if (record.baseCost < AdvancedSynthesisEngine.CRAFT_MIN_COST) {
                return@updateState s.copy(
                    growthMessage = "[연성] 신규 연성은 ${AdvancedSynthesisEngine.CRAFT_MIN_COST}코스트 이상 캐릭터부터 가능합니다."
                )
            }
            val result = AdvancedSynthesisEngine.craft(record, kind, s.shards, s.gold)
            resultOut = result
            if (!result.isSuccess || result.card == null) {
                return@updateState s.copy(growthMessage = "[연성 불가] ${result.failureReason}")
            }
            val card = result.card
            lifecycleEngine.registerOwnedCard(card)
            s.copy(
                ownedCards = s.ownedCards + card,
                gold = s.gold - result.goldSpent,
                shards = result.walletAfter,
                growthCardId = card.id,
                growthMessage = result.message,
                lastCraft = result,
                statusMessage = result.message
            )
        }
        return resultOut
    }

    /** 보유 카드를 조각으로 초월(성급 +1)한다 */
    fun transcendCard(cardId: Int = 0): TranscendResult? {
        var resultOut: TranscendResult? = null
        updateState { s ->
            val targetId = if (cardId > 0) cardId else s.growthCard?.id ?: 0
            val card = s.ownedCards.firstOrNull { it.id == targetId }
                ?: return@updateState s.copy(growthMessage = "[초월] 초월할 카드를 먼저 선택하세요.")
            val result = AdvancedSynthesisEngine.transcend(card, s.shards, s.gold)
            resultOut = result
            if (!result.isSuccess) {
                return@updateState s.copy(growthMessage = "[초월 불가] ${result.failureReason}")
            }
            val newMaxHp = TcgGameViewModel.maxHpOf(card) + EquipmentEngine.hpBonus(card.id, s.equipment)
            s.copy(
                ownedCards = s.ownedCards.map { if (it.id == card.id) card else it },
                party = s.party.map { member ->
                    if (member.card.id == card.id) member.copy(card = card, hp = newMaxHp, maxHp = newMaxHp) else member
                },
                gold = s.gold - result.goldSpent,
                shards = result.walletAfter,
                growthMessage = result.message,
                statusMessage = result.message
            )
        }
        return resultOut
    }

    // ──────────────────────────────────────────────────────────────
    // 서브 플로우 ③: 장비 — 구매 · 장착 · 해제
    // ──────────────────────────────────────────────────────────────

    /** 장비 상점에서 골드로 구매 */
    fun buyEquipment(itemId: String): EquipResult? {
        var resultOut: EquipResult? = null
        updateState { s ->
            val catalogItem = EquipmentCatalog.find(itemId)
                ?: return@updateState s.copy(growthMessage = "[장비] 없는 장비입니다. ($itemId)")
            if (catalogItem.goldPrice <= 0) {
                return@updateState s.copy(growthMessage = "[장비] ${catalogItem.name}은(는) 스테이지 드롭 전용입니다.")
            }
            if (s.gold < catalogItem.goldPrice) {
                return@updateState s.copy(growthMessage = "[장비] 골드가 부족합니다. (${catalogItem.name} ${catalogItem.goldPrice}G · 보유 ${s.gold}G)")
            }
            val item = EquipmentCatalog.instantiate(itemId)
                ?: return@updateState s.copy(growthMessage = "[장비] 장비를 만들 수 없습니다.")
            val result = EquipResult(
                isSuccess = true,
                item = item,
                message = "[장비 구매] ${item.name} (${item.slot.displayName}) ${item.statSummary} · ${catalogItem.goldPrice}G"
            )
            resultOut = result
            s.copy(
                gold = s.gold - catalogItem.goldPrice,
                equipment = s.equipment + item,
                growthMessage = result.message,
                statusMessage = result.message
            )
        }
        return resultOut
    }

    /** 장비 장착 — 지정(또는 성장 화면) 카드에 착용 */
    fun equipItem(itemUid: String, cardId: Int = 0): EquipResult? {
        var resultOut: EquipResult? = null
        updateState { s ->
            val item = s.equipment.firstOrNull { it.uid == itemUid && it.equippedTo == null }
                ?: s.equipment.firstOrNull { it.id == itemUid && it.equippedTo == null }
                ?: return@updateState s.copy(growthMessage = "[장비] 소지품에 없는 장비입니다.")
            val targetId = if (cardId > 0) cardId else s.growthCard?.id ?: 0
            val card = s.ownedCards.firstOrNull { it.id == targetId }
                ?: return@updateState s.copy(growthMessage = "[장비] 장착할 카드를 먼저 선택하세요.")
            val result = EquipmentEngine.equip(card, item, s.equipment)
            resultOut = result
            if (!result.isSuccess) {
                return@updateState s.copy(growthMessage = "[장착 불가] ${result.failureReason}")
            }
            val newMaxHp = TcgGameViewModel.maxHpOf(card) + EquipmentEngine.hpBonus(card.id, s.equipment)
            s.copy(
                ownedCards = s.ownedCards.map { if (it.id == card.id) card else it },
                party = s.party.map { member ->
                    if (member.card.id == card.id) member.copy(card = card, hp = newMaxHp, maxHp = newMaxHp) else member
                },
                equipment = s.equipment.toList(),
                growthCardId = card.id,
                growthMessage = result.message,
                statusMessage = result.message
            )
        }
        return resultOut
    }

    /** 장비 해제 — 수치를 정확히 되돌리고 소지품으로 돌린다 */
    fun unequipItem(itemUid: String): EquipResult? {
        var resultOut: EquipResult? = null
        updateState { s ->
            val item = s.equipment.firstOrNull { (it.uid == itemUid || it.id == itemUid) && it.equippedTo != null }
                ?: return@updateState s.copy(growthMessage = "[장비] 장착 중인 장비가 아닙니다.")
            val card = s.ownedCards.firstOrNull { it.id == item.equippedTo }
                ?: return@updateState s.copy(growthMessage = "[장비] 장착 대상 카드를 찾을 수 없습니다.")
            val result = EquipmentEngine.unequip(card, item)
            resultOut = result
            if (!result.isSuccess) {
                return@updateState s.copy(growthMessage = "[해제 불가] ${result.failureReason}")
            }
            val newMaxHp = TcgGameViewModel.maxHpOf(card) + EquipmentEngine.hpBonus(card.id, s.equipment)
            s.copy(
                ownedCards = s.ownedCards.map { if (it.id == card.id) card else it },
                party = s.party.map { member ->
                    if (member.card.id == card.id) {
                        val hp = newMaxHp.coerceAtMost(member.hp)
                        member.copy(card = card, hp = hp, maxHp = newMaxHp)
                    } else {
                        member
                    }
                },
                equipment = s.equipment.toList(),
                growthMessage = result.message,
                statusMessage = result.message
            )
        }
        return resultOut
    }

    // ──────────────────────────────────────────────────────────────
    // 서브 플로우 ④: 현실 소환 — 차원 에너지 → 호감도·룩 해금·전투 버프
    // ──────────────────────────────────────────────────────────────

    fun selectSummonCard(cardId: Int) {
        updateState { s ->
            if (s.ownedCards.none { it.id == cardId }) {
                return@updateState s.copy(statusMessage = "[현실 소환] 보유하지 않은 캐릭터입니다. (도감 #$cardId)")
            }
            s.copy(summonCardId = cardId, summonMessage = null)
        }
    }

    /** 선택한(또는 지정한) 캐릭터를 현실로 소환한다 */
    fun summonToReality(cardId: Int = 0): SummonResult? {
        var resultOut: SummonResult? = null
        updateState { s ->
            val targetId = if (cardId > 0) cardId else s.summonCard?.id ?: 0
            val card = s.ownedCards.firstOrNull { it.id == targetId }
                ?: return@updateState s.copy(summonMessage = "[현실 소환] 소환할 캐릭터가 없습니다.")
            val result = SummonEngine.summon(card, s.gateEnergy)
            resultOut = result
            if (!result.isSuccess) {
                return@updateState s.copy(summonMessage = "[소환 불가] ${result.failureReason}")
            }
            s.copy(
                ownedCards = s.ownedCards.map { if (it.id == card.id) card else it },
                gateEnergy = (s.gateEnergy - result.energySpent).coerceAtLeast(0),
                summonBuffPercent = result.buffPercent,
                summonBuffBattles = result.buffBattles,
                summonCardId = card.id,
                summonCount = s.summonCount + 1,
                lastSummon = result,
                summonMessage = result.message,
                partnerBond = if (card.id == 3) card.bondLevel else s.partnerBond,
                dateLookUnlocked = s.dateLookUnlocked || result.unlockedDateLook,
                ultimateUnlocked = s.ultimateUnlocked || card.bondLevel >= 100,
                realWorldLog = (s.realWorldLog + result.message).takeLast(6),
                statusMessage = result.message
            )
        }
        return resultOut
    }

    // ──────────────────────────────────────────────────────────────
    // 도감·서약
    // ──────────────────────────────────────────────────────────────

    fun selectCard(cardId: Int) {
        updateState { s ->
            s.copy(
                selectedCardId = cardId,
                selectedVisual = VisualState.BATTLE,
                visualLockMessage = null,
                gazeMessage = null,
                synthesisMessage = null
            )
        }
    }

    fun selectVisual(visual: VisualState) {
        updateState { s ->
            val bond = s.selectedCardBond
            if (!AffinityRules.isVisualUnlocked(visual, bond)) {
                val locked = AffinityRules.lockLabel(visual, bond)
                return@updateState s.copy(
                    visualLockMessage = locked,
                    statusMessage = "[잠금] ${locked ?: visual.displayName}"
                )
            }
            s.copy(selectedVisual = visual, visualLockMessage = null)
        }
    }

    /** 마왕 카드 자세히 응시하기 (줌 체험) — 10초 응시 + 핀치 줌 */
    fun gazeAtDemonLord() {
        updateState { s ->
            val updated = temptationEngine.recordGaze(
                demonLordId = 112,
                durationSeconds = 10,
                isZoomed = true,
                state = s.temptationState
            )
            val penalty = temptationEngine.calculateCombatPenalty(updated)
            val gained = updated.gazeTemptationStacks - s.temptationState.gazeTemptationStacks
            s.copy(
                temptationState = updated,
                combatPenalty = penalty,
                gazeMessage = "[자세히 응시하기] 화면을 확대해 10초간 응시했다. [홀림] 스택 +${gained.coerceAtLeast(0)} " +
                        "(시선 ${updated.gazeTemptationStacks}/5) — ${penalty.debuffDescription}",
                statusMessage = "[줌 응시] [홀림] 스택 누적 — 마왕전 페널티 ${penalty.totalStacks}스택"
            )
        }
    }

    /** 성수 정화 */
    fun purify() {
        updateState { s ->
            if (s.holyWater <= 0) {
                return@updateState s.copy(statusMessage = "[정화 실패] 성수가 부족합니다. (보유 0)")
            }
            val updated = temptationEngine.purifyTemptationWithHolyWater(s.temptationState, 2)
            val penalty = temptationEngine.calculateCombatPenalty(updated)
            s.copy(
                temptationState = updated,
                combatPenalty = penalty,
                holyWater = s.holyWater - 1,
                gazeMessage = "[성수 정화] 매혹·저주 스택을 2 정화했습니다. (시선 ${updated.gazeTemptationStacks}/5 · 저주 ${updated.curseOfArchdemonStacks}/5)\n${penalty.debuffDescription}",
                statusMessage = "[성수 정화] 성수 -1 · ${penalty.debuffDescription}"
            )
        }
    }

    /** 2웨이 합성 ① 한계 돌파 — 동일 카드 흡수 */
    fun limitBreakSelected() {
        updateState { s ->
            val original = s.ownedCards.firstOrNull { it.id == s.selectedCardId }
                ?: return@updateState s.copy(synthesisMessage = "[한계 돌파 불가] 보유하지 않은 카드입니다.")
            if (original.starLevel >= 5) {
                return@updateState s.copy(synthesisMessage = "[한계 돌파] 이미 5성 최대치입니다. 3단 비주얼도 모두 개방되었습니다.")
            }
            val base = original.copy()
            val material = original.copy()
            val prevAtk = base.atk
            val prevDef = base.def
            val success = synthesisEngine.synthesizeCard(base, material)
            if (!success) {
                return@updateState s.copy(synthesisMessage = "[한계 돌파 실패] 조건을 만족하지 못했습니다.")
            }
            s.copy(
                ownedCards = s.ownedCards.map { if (it.id == base.id) base else it },
                synthesisMessage = "[한계 돌파] ${base.name} ${base.starLevel}성 달성! ATK $prevAtk → ${base.atk} (+20%), DEF $prevDef → ${base.def} (+20%)\n" +
                        "다음 성급에서 3단 비주얼(일상·데이트 룩)이 진화합니다.",
                statusMessage = "[한계 돌파] ${base.name} ${base.starLevel}성"
            )
        }
    }

    /** 2웨이 합성 ② 신규 연성 — 잡카드 3장 + 300G */
    fun craftNewCharacter() {
        updateState { s ->
            if (s.junkCards < 3) {
                return@updateState s.copy(synthesisMessage = "[신규 연성 불가] 잡카드가 부족합니다. (보유 ${s.junkCards}장 / 3장 필요)")
            }
            if (s.gold < 300) {
                return@updateState s.copy(synthesisMessage = "[신규 연성 불가] 수수료 300G가 부족합니다. (보유 ${s.gold}G)")
            }
            // 아직 계약하지 않은 카드(마왕 등) 중 가장 앞 번호와 계약한다
            val candidate = CardCatalog.unownedCandidates(s.ownedCards.map { it.id }).firstOrNull()
            val nextId = candidate?.id
            val gainedName = candidate?.fullName
            if (nextId == null || gainedName == null) {
                return@updateState s.copy(
                    junkCards = s.junkCards - 3,
                    gold = s.gold - 300,
                    synthesisMessage = "[신규 연성] 잡카드 3장을 연성했지만 이미 보유한 캐릭터의 조각만 나왔습니다. (금화 -300G, 잡카드 -3)",
                    statusMessage = "[신규 연성] 보유 캐릭터 조각 획득"
                )
            }
            val card = characterCard(nextId)
            lifecycleEngine.registerOwnedCard(card)
            s.copy(
                junkCards = s.junkCards - 3,
                gold = s.gold - 300,
                ownedCards = s.ownedCards + card,
                selectedCardId = card.id,
                synthesisMessage = "[신규 연성] 잡카드 3장이 빛으로 엮여 새로운 캐릭터 '${gaindNameOf(card.id)}'와 계약했습니다!\n" +
                        "(금화 -300G, 잡카드 -3 → 3장 보유)",
                statusMessage = "[신규 연성] ${gaindNameOf(card.id)} 획득!"
            )
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 현실 교감
    // ──────────────────────────────────────────────────────────────

    /**
     * 현실 교감 — 현재 소환 대상([summonCard])과의 교감.
     * (2026-11-16 진행 루프 개편: 시작 카드가 무작위 3장이므로 특정 번호가 아니라
     *  화면에서 고른 파트너를 대상으로 한다.)
     */
    fun interactRealWorld(activity: RealWorldActivity) {
        updateState { s ->
            val partner = s.summonCard
                ?: return@updateState s.copy(statusMessage = "[현실 교감] 교감할 파트너가 없습니다. 카드팩으로 동료를 계약하세요.")
            val working = s.ownedCards.firstOrNull { it.id == partner.id } ?: partner
            val partnerName = CardCatalog.find(working.id)?.name ?: working.name
            val newBond = dualWorldEngine.interactInRealWorld(working, activity)
            val visual = working.currentVisual
            // 「기억의 잔향」: 기억이 지워진 동료는 교감으로 기억 파편을 되찾는다.
            val fragmentGain = if (working.isMemoryReset) {
                memoryRecallEngine.grantFragment(working, 1, MemoryFragmentSource.REAL_WORLD_BOND)
            } else {
                null
            }
            val label = when (activity) {
                RealWorldActivity.DAILY_GREETING -> "짧은 일상 대화 (+${AffinityRules.DAILY_TALK_GAIN})"
                RealWorldActivity.CASUAL_CAFE_WALK -> "저녁 산책·카페 (+${AffinityRules.WALK_GAIN})"
                RealWorldActivity.ROMANTIC_EVENING_DATE -> "카페 데이트·선물 (+${AffinityRules.DATE_GAIN})"
            }
            val unlockedDate = visual == VisualState.ROMANTIC_DATE && newBond >= AffinityRules.DATE_UNLOCK_BOND
            val unlockedUltimate = newBond >= AffinityRules.ULTIMATE_UNLOCK_BOND
            val unlockedCasual = newBond >= AffinityRules.CASUAL_UNLOCK_BOND
            val logLine = "[현실 교감] $partnerName · $label → 친밀도 $newBond/100" +
                    (if (unlockedCasual && newBond - AffinityRules.DAILY_TALK_GAIN < AffinityRules.CASUAL_UNLOCK_BOND) {
                        " · 🎉 일상 사복·현실 소환 개방!"
                    } else "") +
                    (if (unlockedDate && newBond - AffinityRules.DATE_GAIN < AffinityRules.DATE_UNLOCK_BOND) {
                        " · 데이트 룩 해금 연출"
                    } else "") +
                    (if (unlockedUltimate) " · 페어 얼티밋 오의 개방!" else "")
            val fragmentLine = if (fragmentGain != null && fragmentGain.isSuccess) fragmentGain.message else null
            s.copy(
                ownedCards = s.ownedCards.map { if (it.id == working.id) working else it },
                summonCardId = working.id,
                partnerBond = if (working.id == 3) newBond else s.partnerBond,
                partnerVisual = if (working.id == 3) visual else s.partnerVisual,
                dateLookUnlocked = s.dateLookUnlocked || unlockedDate,
                ultimateUnlocked = s.ultimateUnlocked || unlockedUltimate,
                memoryRecallMessage = fragmentLine ?: s.memoryRecallMessage,
                realWorldLog = (s.realWorldLog + listOfNotNull(logLine, fragmentLine)).takeLast(6),
                statusMessage = logLine
            )
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 「기억의 잔향」 — 기억이 지워진 동료의 파편 수집 · 회상 이벤트
    // ──────────────────────────────────────────────────────────────

    /**
     * 기억이 지워진 동료와 교감해 기억 파편 +1 (호감도 +5)
     * — 현실 교감 경로. 전장 승리 경로는 전투 승리 정산에서 자동 지급된다.
     */
    fun bondMemoryResetPartner(cardId: Int) {
        updateState { s ->
            val card = s.ownedCards.firstOrNull { it.id == cardId }
                ?: return@updateState s.copy(statusMessage = "[교감 불가] 보유하지 않은 카드입니다. (ID: $cardId)")
            if (!card.isMemoryReset) {
                return@updateState s.copy(
                    statusMessage = "[교감] '${card.name}'은(는) 기억이 온전한 상태라 파편이 필요하지 않습니다."
                )
            }
            card.bondLevel = (card.bondLevel + 5).coerceAtMost(100)
            val gain = memoryRecallEngine.grantFragment(card, 1, MemoryFragmentSource.REAL_WORLD_BOND)
            s.copy(
                ownedCards = s.ownedCards.map { if (it.id == card.id) card else it },
                partnerBond = if (card.id == 3) card.bondLevel else s.partnerBond,
                memoryRecallMessage = gain.message,
                realWorldLog = (s.realWorldLog + gain.message).takeLast(6),
                statusMessage = gain.message
            )
        }
    }

    /** 「기억의 조각 회상」 특별 이벤트 — 파편 3/3 달성 시 스탯 보너스와 기억 복원 */
    fun openMemoryRecall(cardId: Int): MemoryRecallResult? {
        var resultOut: MemoryRecallResult? = null
        updateState { s ->
            val card = s.ownedCards.firstOrNull { it.id == cardId }
                ?: return@updateState s.copy(statusMessage = "[회상 불가] 보유하지 않은 카드입니다. (ID: $cardId)")
            val result = memoryRecallEngine.openRecallEvent(card)
            resultOut = result
            if (!result.isSuccess) {
                return@updateState s.copy(
                    memoryRecallMessage = result.message,
                    statusMessage = result.message
                )
            }
            s.copy(
                ownedCards = s.ownedCards.map { if (it.id == card.id) card else it },
                memoryRecallMessage = result.message,
                memoryRecallDialogue = result.dialogue,
                storyReadLog = (s.storyReadLog + "[기억의 조각 회상] ${card.name} — 기억 복원 완료").takeLast(8),
                statusMessage = result.message
            )
        }
        return resultOut
    }

    // ──────────────────────────────────────────────────────────────
    // 「신뢰의 균열」 — [홀림] 2스택 이상 시 열리는 긴급 교차 이벤트
    // ──────────────────────────────────────────────────────────────

    /** 교차 이벤트 선택지 A/B 처리 */
    fun resolveTrustRift(choiceId: String): TrustRiftResolution? {
        var resultOut: TrustRiftResolution? = null
        updateState { s ->
            if (s.trustRiftChoice != null) {
                return@updateState s.copy(statusMessage = "[교차 이벤트] 이미 선택을 마친 이벤트입니다.")
            }
            val result = trustRiftEngine.resolve(choiceId, s.temptationState, s.partnerBond)
            resultOut = result
            if (!result.isSuccess) {
                return@updateState s.copy(statusMessage = result.message)
            }
            val penalty = temptationEngine.calculateCombatPenalty(result.resolvedState)
            val summary = result.message.lineSequence().first()
            s.copy(
                temptationState = result.resolvedState,
                combatPenalty = penalty,
                partnerBond = result.newBondLevel,
                trustRiftChoice = result.choiceId,
                trustRiftMessage = result.message,
                frontRowDefensePenaltyPercent = result.frontRowDefensePenaltyPercent,
                ownedCards = s.ownedCards.map { card ->
                    if (card.id == 3) {
                        card.bondLevel = result.newBondLevel
                    }
                    card
                },
                storyReadLog = (s.storyReadLog + "[신뢰의 균열] $summary").takeLast(8),
                statusMessage = summary
            )
        }
        return resultOut
    }

    // ──────────────────────────────────────────────────────────────
    // 「페어 얼티밋 오의」 — 아군 2명 이상 + 마나 3 이상 협동 오의
    // ──────────────────────────────────────────────────────────────

    /** 페어 얼티밋 오의 발동 (전투당 1회) */
    fun activatePairUltimate(): PairUltimateResult? {
        var resultOut: PairUltimateResult? = null
        updateState { s ->
            if (s.enemyHp <= 0) {
                return@updateState s.copy(statusMessage = "[전투 종료] 적이 없습니다. 새 원정을 시작하세요.")
            }
            val attacker = s.party.firstOrNull { it.isAlive && it.isFrontRow }
                ?: s.party.firstOrNull { it.isAlive }
                ?: return@updateState s.copy(statusMessage = "[오의 불가] 생존한 아군이 없습니다.")

            val result = pairUltimateEngine.activate(
                attacker = attacker.card,
                defender = enemyCard(s),
                penalty = s.combatPenalty,
                aliveCount = s.aliveSurvivorCount,
                mana = s.mana,
                usedThisBattle = s.pairUltimateUsedThisBattle
            )
            resultOut = result
            if (!result.isSuccess) {
                return@updateState s.copy(
                    battleLog = s.battleLog + result.failureReason.orEmpty(),
                    statusMessage = result.failureReason ?: "[오의 불가] 발동 조건을 만족하지 못했습니다."
                )
            }
            val newEnemyHp = (s.enemyHp - result.damage).coerceAtLeast(0)
            val newMana = s.mana.copy(currentMana = (s.mana.currentMana - result.manaSpent).coerceAtLeast(0))
            val tail = "[턴 ${s.turn}] 적 잔여 HP $newEnemyHp/${s.enemyMaxHp}"

            if (newEnemyHp <= 0) {
                applyVictory(s, newMana, result.logLines + tail).copy(
                    pairUltimateUsedThisBattle = true,
                    pairUltimateCount = s.pairUltimateCount + 1,
                    pairUltimateBanner = result.bannerTitle,
                    pairUltimateBannerSubtitle = result.bannerSubtitle,
                    lastEnemyHitDamage = result.damage,
                    lastEnemyHitTick = s.lastEnemyHitTick + 1,
                    lastAttackerSlot = attacker.slotIndex,
                    lastCutIn = pairUltimateCutIn(s, attacker, result, lethal = true)
                )
            } else {
                s.copy(
                    enemyHp = newEnemyHp,
                    mana = newMana,
                    pairUltimateUsedThisBattle = true,
                    pairUltimateBanner = result.bannerTitle,
                    pairUltimateBannerSubtitle = result.bannerSubtitle,
                    pairUltimateCount = s.pairUltimateCount + 1,
                    lastEnemyHitDamage = result.damage,
                    lastEnemyHitTick = s.lastEnemyHitTick + 1,
                    lastAttackerSlot = attacker.slotIndex,
                    lastCutIn = pairUltimateCutIn(s, attacker, result, lethal = false),
                    battleLog = s.battleLog + result.logLines + tail,
                    statusMessage = "[⚡ 페어 얼티밋 오의] ${attacker.card.name} · ${result.damage} 데미지 — 적 HP $newEnemyHp/${s.enemyMaxHp}"
                )
            }
        }
        return resultOut
    }

    /** ⚡ 페어 얼티밋 오의 컷인 — 금빛 오의 연출(오의 이름을 배너에 싣는다) */
    private fun pairUltimateCutIn(
        s: TcgGameState,
        attacker: PartyMember,
        result: PairUltimateResult,
        lethal: Boolean
    ): BattleCutIn = BattleCutIn(
        tick = nextCutInTick(s),
        fromEnemy = false,
        kind = BattleAttackKind.PAIR_ULTIMATE,
        attackerCardId = attacker.card.id,
        attackerName = shortName(attacker.card.id, attacker.card.name),
        element = attacker.card.element,
        attackName = result.bannerTitle.substringAfter("—", "").trim()
            .ifBlank { BattleAttackKind.PAIR_ULTIMATE.defaultLabel },
        attackerSlot = attacker.slotIndex,
        targetName = s.enemyName,
        damage = result.damage,
        critical = true,
        multiplier = result.survivorBonusMultiplier,
        ultimate = true,
        lethal = lethal
    )

    // ──────────────────────────────────────────────────────────────
    // 제2장 프롤로그 — 제1군단장 「그림자 기사」 전초전
    // ──────────────────────────────────────────────────────────────

    /** 5화 이후 제2장 프롤로그 노드 열기 */
    fun openChapter2Prelude() {
        updateState { s ->
            val node: StoryArcNode = ChapterTwoScript.SHADOW_KNIGHT_PRELUDE
            if (!node.isUnlocked(s.storyStepIndex)) {
                return@updateState s.copy(statusMessage = "[제2장] 제1장 5화를 먼저 읽어야 프롤로그 노드가 열립니다.")
            }
            s.copy(
                chapter2PreludeOpened = true,
                chapter2Message = "[${node.chapter}] ${node.title} — ${node.objective}",
                storyReadLog = (s.storyReadLog + "[${node.chapter}] ${node.title} — ${node.objective}").takeLast(8),
                statusMessage = "[${node.chapter}] ${node.title} — 전초전이 열렸습니다."
            )
        }
    }

    /** 제1군단장 그림자 기사 전초전 개시 */
    fun startChapter2BossBattle() {
        updateState { s ->
            val node = ChapterTwoScript.SHADOW_KNIGHT_PRELUDE
            if (!node.isUnlocked(s.storyStepIndex)) {
                return@updateState s.copy(statusMessage = "[제2장] 5화 이후에만 전초전을 열 수 있습니다.")
            }
            // 친밀도: 출전 카드마다 +4 (승리 시 정산에서 +2)
            val base = creditBattleDeployment(s)
            base.copy(
                chapter2PreludeOpened = true,
                enemyName = node.bossName,
                enemyElement = node.bossElement,
                enemyHp = node.bossHp,
                enemyMaxHp = node.bossHp,
                enemyAtk = node.bossAtk,
                isDemonLordBattle = false,
                isChapter2BossBattle = true,
                turn = 1,
                mana = ManaState(1, 1),
                party = freshParty(base),
                retreatInfo = BattleRetreatInfo(turn = 1),
                isAutoBattle = true,
                lastCutIn = null,
                pairUltimateUsedThisBattle = false,
                pairUltimateBanner = null,
                pairUltimateBannerSubtitle = null,
                pendingLootGold = 1800,
                pendingLootGems = 20,
                audioScene = AudioScene.BOSS,
                chapter2Message = "[${node.chapter}] ${node.title} — ${node.bossSkill}",
                battleLog = base.battleLog + listOf(
                    "[전초전 개시] ${node.bossName} — HP ${node.bossHp} / 공격력 ${node.bossAtk}",
                    node.bossSkill
                ),
                statusMessage = "[전초전] ${node.bossName}와(과) 조우했습니다. 아군 2명 생존 + 마나 3으로 페어 오의를 노리세요."
            )
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────────────────────

    private fun toBattleState(s: TcgGameState): BattleState {
        val slots = (0 until DeckRules.MAX_DECK_SIZE)
            .map { idx -> BattleSlot(slotIndex = idx, isFrontRow = DeckRules.isFrontSlot(idx)) }
            .toMutableList()
        s.party.filter { it.isAlive }.forEach { m ->
            val slot = slots.getOrNull(m.slotIndex)
            if (slot != null) slot.placedCard = m.card
        }
        return BattleState(
            turn = s.turn,
            mana = s.mana,
            playerSlots = slots,
            playerHp = s.party.filter { it.isAlive }
                .map { BattleCardHp(it.slotIndex, it.card.id, it.hp, it.maxHp) }
                .toMutableList(),
            hand = mutableListOf(),
            reserve = s.battleReserve.toMutableList(),
            retreatInfo = s.retreatInfo.copy(turn = s.turn),
            lootGold = s.pendingLootGold,
            lootGems = s.pendingLootGems
        )
    }

    /**
     * 전투 개시 시 파티를 준비한다 — 편성(누가 어느 슬롯에 있는지)은 그대로 두고 HP만 최대치로 회복.
     * 편성이 비어 있으면 스타팅 3인방으로 채운다.
     */
    private fun freshParty(s: TcgGameState): List<PartyMember> {
        // 보유하지 않은(영구 상실된) 카드는 편성에 남아 있어도 다시 세우지 않는다.
        val owned = s.ownedCards.associateBy { it.id }
        val kept = s.party
            .filter { owned.containsKey(it.card.id) }
            .map { member ->
                val card = owned.getValue(member.card.id)
                val maxHp = TcgGameViewModel.maxHpOf(card) + EquipmentEngine.hpBonus(card.id, s.equipment)
                member.copy(card = card, hp = maxHp, maxHp = maxHp, memoryAffinity = 0)
            }
        if (kept.isNotEmpty()) return kept
        // 편성이 비어 있으면 보유 카드로 전열을 채운다 (보유 0장이면 빈 목록 = 출전 불가)
        return initialParty(s.ownedCards.take(DeckRules.FRONT_SLOTS))
    }

    companion object {
        /** 메인 로비 기본 대표 캐릭터 (성기사 아우렐리아 — 시작 카드에 없을 때의 대체값) */
        const val DEFAULT_LOBBY_HERO_ID: Int = 3

        /** 시작 골드 — 일반 카드팩 2번 정도 */
        const val START_GOLD: Int = 500

        /** 시작 보석 */
        const val START_GEMS: Int = 10

        /** 시작 카드팩 티켓 — 첫 개봉을 바로 체험 */
        const val START_PACK_TICKETS: Int = 1

        /** 차원 균열 에너지 시작값 */
        const val GATE_ENERGY_START: Int = 10

        /** 차원 균열 에너지 최대치 */
        const val GATE_ENERGY_MAX: Int = 12

        /** 덱 슬롯 총수 — 기본 9장 · 최대 12장 */
        const val DECK_SLOTS: Int = DeckRules.MAX_DECK_SIZE

        /** 이계 전투 승리 시 회복되는 차원 균열 에너지 */
        const val GATE_ENERGY_RECOVERY: Int = 2

        /** 메인 로비 일상 사복(②) 해금 친밀도 — 현실 소환 조건과 같은 값 */
        const val CASUAL_LOOK_UNLOCK_BOND: Int = AffinityRules.CASUAL_UNLOCK_BOND

        /** 메인 로비 데이트 룩(③) 해금 친밀도 */
        const val DATE_LOOK_UNLOCK_BOND: Int = AffinityRules.DATE_UNLOCK_BOND

        /**
         * 메인 로비 좌우 전환 순서 — 스타팅 3인방 → 나머지 플레이 가능 167인 → 중간 보스 6인.
         * (보유하지 않은 카드는 [TcgGameState.lobbyRoster]에서 걸러진다.)
         */
        val LOBBY_HERO_ORDER: List<Int> =
            listOf(3, 1, 2) +
                CardCatalog.ALL.map { it.id }.filter { it !in listOf(3, 1, 2) } +
                com.game.tcg.core.model.MidBossCatalog.ALL.map { it.id }

        /** 도감 카탈로그에서 카드 1장을 만든다 (미등록 번호는 0으로 안전하게 대체) */
        fun characterCard(id: Int, bondLevel: Int = CardCatalog.ROSTER_BOND_SEED): TcgCard =
            CardCatalog.require(id).toCard(bondLevel)

        /**
         * 카드의 전장 최대 HP.
         * 기본 HP(카탈로그)에 **DEF 성장률**을 곱한다 — 강화·성급·장비로 DEF가 오르면 HP도 함께 오른다.
         * 장비의 HP 보너스는 상태를 가진 쪽(ViewModel)이 더한다.
         */
        fun maxHpOf(card: TcgCard): Int {
            val record = CardCatalog.find(card.id) ?: return 4000
            val baseDef = record.def.coerceAtLeast(1)
            val scaled = record.maxHp.toLong() * card.def / baseDef
            return scaled.toInt().coerceAtLeast(1)
        }

        fun gaindNameOf(id: Int): String = CardCatalog.require(id).fullName

        /** 스타팅 파티 — 지정 카드(기본: 저코스트 무작위 3장)를 전열 1~3칸에 배치 */
        fun initialParty(cards: List<TcgCard> = StartingRoster.starterCards()): List<PartyMember> =
            StartingRoster.deploySlots(cards).map { (card, slot) ->
                val maxHp = maxHpOf(card)
                PartyMember(card, slot, DeckRules.isFrontSlot(slot), maxHp, maxHp)
            }

        /** 게임 시작 시 보유 카드 — 저코스트 풀에서 무작위 3장 */
        fun initialOwnedCards(
            random: Random = Random.Default,
            bondLevel: Int = CardCatalog.ROSTER_BOND_SEED
        ): List<TcgCard> =
            StartingRoster.starterCards(StartingRoster.draw(random)).onEach { it.bondLevel = bondLevel }

        /**
         * 도감 프로필 — 등록된 170인 전체 (No.1~169 + No.184).
         * 170명 로비·도감에서 매 프레임 새 리스트를 만들지 않도록 한 번만 생성해 재사용한다.
         */
        private val PROFILES: List<CharacterProfile> = CardCatalog.ALL.map { record ->
            CharacterProfile(
                cardId = record.id,
                displayName = record.name,
                title = record.title,
                element = record.element,
                isDemonLord = record.isDemonLord,
                disguiseName = record.disguiseName
            )
        }

        private val PROFILE_BY_ID: Map<Int, CharacterProfile> = PROFILES.associateBy { it.cardId }

        fun profiles(): List<CharacterProfile> = PROFILES

        /** 중간 보스 6명(No.170~175)의 프로필 — 도감 '보스 6' 필터용 */
        private val MID_BOSS_PROFILES: List<CharacterProfile> =
            com.game.tcg.core.model.MidBossCatalog.ALL.map { record ->
                CharacterProfile(
                    cardId = record.id,
                    displayName = record.name,
                    title = record.title,
                    element = record.element,
                    isDemonLord = false,
                    disguiseName = null
                )
            }

        fun midBossProfiles(): List<CharacterProfile> = MID_BOSS_PROFILES

        /** 카드 1명의 프로필 (170명 로스터 조회 O(1)) */
        fun profileOf(cardId: Int): CharacterProfile? = PROFILE_BY_ID[cardId]
    }
}
