package com.game.tcg.ui

import androidx.lifecycle.ViewModel
import com.game.tcg.core.engine.BattleEngine
import com.game.tcg.core.engine.ChapterTwoScript
import com.game.tcg.core.engine.DualWorldEngine
import com.game.tcg.core.engine.IBattleEngine
import com.game.tcg.core.engine.IDualWorldEngine
import com.game.tcg.core.engine.IManaEngine
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
import com.game.tcg.core.model.AcquisitionResult
import com.game.tcg.core.model.BattleCardHp
import com.game.tcg.core.model.BattleRetreatInfo
import com.game.tcg.core.model.BattleSlot
import com.game.tcg.core.model.BattleState
import com.game.tcg.core.model.CombatPenalty
import com.game.tcg.core.model.DemonLordRegistry
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.EmergencyRetreatResult
import com.game.tcg.core.model.ManaState
import com.game.tcg.core.model.MemoryFragmentSource
import com.game.tcg.core.model.MemoryRecallDialogue
import com.game.tcg.core.model.MemoryRecallResult
import com.game.tcg.core.model.PairUltimateReady
import com.game.tcg.core.model.PairUltimateResult
import com.game.tcg.core.model.ReincarnatedCard
import com.game.tcg.core.model.RetreatResult
import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.TemptationState
import com.game.tcg.core.model.TrustRiftResolution
import com.game.tcg.core.model.UndercoverDialogueOption
import com.game.tcg.core.model.VisualState
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
 * 전열 3 / 후열 2 슬롯에 배치된 파티원 (전장 HP 별도 추적)
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
    val battleLog: List<String> = listOf("[원정 준비] 전열 3 · 후열 2 슬롯 편성 완료. 턴 1 시작."),
    val retreatInfo: BattleRetreatInfo = BattleRetreatInfo(),
    val lastRetreatMessage: String? = null,
    val reincarnationPool: List<ReincarnatedCard> = emptyList(),
    val battleVictories: Int = 0,
    val lootGold: Int = 0,
    val lootGems: Int = 0,
    val pendingLootGold: Int = 300,
    val pendingLootGems: Int = 5,
    val lastAcquisition: String? = null,
    // ── 도감·서약 ──
    val ownedCards: List<TcgCard> = emptyList(),
    val selectedCardId: Int = 3,
    val selectedVisual: VisualState = VisualState.BATTLE,
    val synthesisMessage: String? = null,
    // ── 현실 교감 ──
    val partnerBond: Int = 20,
    val partnerVisual: VisualState = VisualState.BATTLE,
    val realWorldLog: List<String> = listOf("[현실 교감] 아우렐리아 소환 대기 중. 호감도 20/100"),
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
    val isChapter2BossBattle: Boolean = false
) {
    /** 생존 아군 수 (오의 조건 판정용) */
    val aliveSurvivorCount: Int get() = party.count { it.isAlive }

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

    companion object {
        /** 페어 얼티밋 오의 최소 생존 인원 */
        const val PAIR_ULTIMATE_SURVIVORS: Int = 2

        /** 페어 얼티밋 오의 소모 마나 */
        const val PAIR_ULTIMATE_MANA: Int = 3
    }
}

// ============================================================================
// ViewModel — 화면 4탭(시나리오 · 이계 전투 · 도감 · 현실 교감)의 상태와 조작
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
    private val pairUltimateEngine: IPairUltimateEngine = PairUltimateEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TcgGameState(
            party = initialParty(),
            ownedCards = initialOwnedCards()
        )
    )
    val uiState: StateFlow<TcgGameState> = _uiState.asStateFlow()

    init {
        initialOwnedCards().forEach { lifecycleEngine.registerOwnedCard(it) }
    }

    // ──────────────────────────────────────────────────────────────
    // 시나리오 진행
    // ──────────────────────────────────────────────────────────────

    /** 다음 스토리 카드로 진행 */
    fun advanceStory() {
        _uiState.update { s ->
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
        _uiState.update { s ->
            s.copy(storyStepIndex = (s.storyStepIndex - 1).coerceAtLeast(0))
        }
    }

    /** 샘플 이벤트 A — 비 오는 밤 카페의 '릴리' */
    fun chooseEventA(talkWithLily: Boolean) {
        _uiState.update { s ->
            if (s.eventAChoice != null) {
                return@update s.copy(statusMessage = "[이벤트 A] 이미 선택을 마친 이벤트입니다.")
            }
            if (!talkWithLily) {
                if (s.holyWater <= 0) {
                    return@update s.copy(statusMessage = "[이벤트 A] 성수가 없습니다. 대화를 피할 수 없어 유혹 스택이 1 쌓였습니다. (도감에서 성수 정화 가능)")
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
        _uiState.update { s ->
            if (s.eventBChoice != null) {
                return@update s.copy(statusMessage = "[이벤트 B] 이미 선택을 마친 이벤트입니다.")
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
        _uiState.update { s ->
            val penalty = temptationEngine.calculateCombatPenalty(s.temptationState)
            s.copy(
                enemyName = "마왕 릴리스",
                enemyElement = Element.DARK,
                enemyHp = 25000,
                enemyMaxHp = 25000,
                enemyAtk = 2800,
                isDemonLordBattle = true,
                turn = 1,
                mana = ManaState(1, 1),
                party = initialParty(),
                retreatInfo = BattleRetreatInfo(turn = 1),
                pairUltimateUsedThisBattle = false,
                pairUltimateBanner = null,
                pairUltimateBannerSubtitle = null,
                isChapter2BossBattle = false,
                pendingLootGold = 2500,
                pendingLootGems = 30,
                battleLog = s.battleLog + listOf(
                    "[마왕전 개시] 황혼과 욕망의 마왕 릴리스 — HP 25000 / 공격력 2800",
                    penalty.debuffDescription
                ),
                statusMessage = "[마왕전] 릴리스가 심연의 옥좌에서 일어섰다. (홀림 ${s.combatPenalty.totalStacks}스택 적용)"
            )
        }
    }

    /** 일반 마수 원정 준비 */
    fun startFieldBattle() {
        _uiState.update { s ->
            s.copy(
                enemyName = "차원 틈새의 마수",
                enemyElement = Element.DARK,
                enemyHp = 12000,
                enemyMaxHp = 12000,
                enemyAtk = 900,
                isDemonLordBattle = false,
                turn = 1,
                mana = ManaState(1, 1),
                party = initialParty(),
                retreatInfo = BattleRetreatInfo(turn = 1),
                pairUltimateUsedThisBattle = false,
                pairUltimateBanner = null,
                pairUltimateBannerSubtitle = null,
                isChapter2BossBattle = false,
                pendingLootGold = 300,
                pendingLootGems = 5,
                battleLog = s.battleLog + "[원정 개시] 차원 틈새의 마수 — HP 12000 / 공격력 900",
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
        _uiState.update { s ->
            if (s.enemyHp <= 0) return@update s.copy(statusMessage = "[전투 종료] 이미 적을 쓰러뜨렸습니다.")
            val attackers = s.party.filter { it.isAlive && it.isFrontRow == front }
            if (attackers.isEmpty()) {
                return@update s.copy(
                    battleLog = s.battleLog + "[공격 불가] ${if (front) "전열" else "후열"}에 생존한 카드가 없습니다. 도감에서 재획득하거나 턴을 넘기세요.",
                    statusMessage = "[공격 불가] ${if (front) "전열" else "후열"} 생존 카드 없음"
                )
            }
            if (s.mana.currentMana < 1) {
                return@update s.copy(
                    battleLog = s.battleLog + "[공격 실패] 마나가 부족합니다. (보유 ${s.mana.currentMana}) — '턴 진행'으로 마나를 회복하세요.",
                    statusMessage = "[공격 실패] 마나 부족 (턴 진행 필요)"
                )
            }
            // 턴에 따라 공격자를 순환
            val attacker = attackers[(s.turn - 1) % attackers.size]
            val base = battleEngine.calculateDamageWithTemptation(attacker.card, enemyCard(s), s.combatPenalty)
            val intelMultiplier = 1.0 + (s.intelBonusPercent / 100.0)
            val finalDamage = (base.finalDamage * intelMultiplier).toInt()
            val newEnemyHp = (s.enemyHp - finalDamage).coerceAtLeast(0)
            val newMana = s.mana.copy(currentMana = s.mana.currentMana - 1)

            val logLine = "[턴 ${s.turn}] ${attacker.card.name} → ${s.enemyName} : ${finalDamage} 데미지 " +
                    "(상성 ${base.multiplier}x" +
                    (if (s.intelBonusPercent > 0) ", 급소 +${s.intelBonusPercent}%" else "") +
                    (if (s.combatPenalty.isCharmed) ", 홀림 -${s.combatPenalty.attackReductionPercent}%" else "") +
                    ") · 적 잔여 HP ${newEnemyHp}"

            if (newEnemyHp <= 0) {
                applyVictory(s, newMana, listOf(logLine))
            } else {
                s.copy(
                    enemyHp = newEnemyHp,
                    mana = newMana,
                    battleLog = s.battleLog + logLine,
                    statusMessage = "[공격] ${attacker.card.name} ${finalDamage} 데미지 — 적 HP ${newEnemyHp}/${s.enemyMaxHp}"
                )
            }
        }
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
                    "[전투 승리] ${s.enemyName} 토벌 완료! (전리품 금화 ${rewardGold}G · 보석 ${rewardGems})" +
                    fragmentLogs,
            gold = s.gold + rewardGold,
            gems = s.gems + rewardGems,
            battleVictories = s.battleVictories + 1,
            pairUltimateUsedThisBattle = false,
            frontRowDefensePenaltyPercent = 0,
            storyStepIndex = if (s.storyStepIndex <= 2) 3 else s.storyStepIndex,
            statusMessage = "[전투 승리] ${s.enemyName}을(를) 쓰러뜨렸습니다. 획득 금화 ${rewardGold}G."
        )

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
        _uiState.update { s ->
            if (s.enemyHp <= 0) return@update s.copy(statusMessage = "[전투 종료] 적이 없습니다. 새 원정을 시작하세요.")
            val nextTurnNo = s.turn + 1
            val mana = manaEngine.replenishTurnMana(manaEngine.calculateMaxManaForTurn(nextTurnNo))
            val targets = s.party.filter { it.isAlive && it.isFrontRow }.ifEmpty { s.party.filter { it.isAlive } }
            if (targets.isEmpty()) {
                return@update s.copy(
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
        return if (result.isSuccess) {
            s.copy(
                reincarnationPool = lifecycleEngine.collection.reincarnationQueue(),
                ownedCards = s.ownedCards.filterNot { it.id == victim.card.id },
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
        _uiState.update { s ->
            val result: AcquisitionResult = lifecycleEngine.acquireCard(cardId)
            if (!result.isSuccess || result.card == null) {
                return@update s.copy(
                    lastAcquisition = result.message,
                    statusMessage = result.message
                )
            }
            val card = result.card
            val slot = when (card.id) {
                1 -> 1
                2 -> 2
                3 -> 0
                else -> 4
            }
            val maxHp = maxHpOf(card)
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

    /** 전술 후퇴 — 턴당 1회, 마나 1 (마왕전 2), 홀림 3스택 이상이면 명령 불복 */
    fun retreat(slotIndex: Int): RetreatResult? {
        var resultOut: RetreatResult? = null
        _uiState.update { s ->
            val battleState = toBattleState(s)
            val result = retreatEngine.retreatCard(
                battleState = battleState,
                slotIndex = slotIndex,
                temptationStacks = s.combatPenalty.totalStacks,
                isDemonLordBattle = s.isDemonLordBattle
            )
            resultOut = result
            if (!result.isSuccess) {
                return@update s.copy(
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
        _uiState.update { s ->
            val battleState = toBattleState(s)
            val result = retreatEngine.emergencyPartyRetreat(battleState)
            resultOut = result
            if (!result.isSuccess) {
                return@update s.copy(
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
        _uiState.update { s ->
            val card = s.battleReserve.firstOrNull { it.id == cardId } ?: return@update s
            val occupied = s.party.map { it.slotIndex }.toSet()
            val slot = (0 until 5).firstOrNull { it !in occupied } ?: return@update s.copy(
                statusMessage = "[재투입 불가] 빈 슬롯이 없습니다."
            )
            val maxHp = maxHpOf(card)
            s.copy(
                party = (s.party + PartyMember(card, slot, slot < 3, maxHp, maxHp)).sortedBy { it.slotIndex },
                battleReserve = s.battleReserve.filterNot { it.id == cardId },
                battleLog = s.battleLog + "[재투입] ${card.name} → ${slot}번 슬롯 (${if (slot < 3) "전열" else "후열"})",
                statusMessage = "[재투입] ${card.name}이(가) ${slot}번 슬롯에 복귀했습니다."
            )
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 도감·서약
    // ──────────────────────────────────────────────────────────────

    fun selectCard(cardId: Int) {
        _uiState.update { s ->
            s.copy(
                selectedCardId = cardId,
                selectedVisual = VisualState.BATTLE,
                gazeMessage = null,
                synthesisMessage = null
            )
        }
    }

    fun selectVisual(visual: VisualState) {
        _uiState.update { s -> s.copy(selectedVisual = visual) }
    }

    /** 마왕 카드 자세히 응시하기 (줌 체험) — 10초 응시 + 핀치 줌 */
    fun gazeAtDemonLord() {
        _uiState.update { s ->
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
        _uiState.update { s ->
            if (s.holyWater <= 0) {
                return@update s.copy(statusMessage = "[정화 실패] 성수가 부족합니다. (보유 0)")
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
        _uiState.update { s ->
            val original = s.ownedCards.firstOrNull { it.id == s.selectedCardId }
                ?: return@update s.copy(synthesisMessage = "[한계 돌파 불가] 보유하지 않은 카드입니다.")
            if (original.starLevel >= 5) {
                return@update s.copy(synthesisMessage = "[한계 돌파] 이미 5성 최대치입니다. 3단 비주얼도 모두 개방되었습니다.")
            }
            val base = original.copy()
            val material = original.copy()
            val prevAtk = base.atk
            val prevDef = base.def
            val success = synthesisEngine.synthesizeCard(base, material)
            if (!success) {
                return@update s.copy(synthesisMessage = "[한계 돌파 실패] 조건을 만족하지 못했습니다.")
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
        _uiState.update { s ->
            if (s.junkCards < 3) {
                return@update s.copy(synthesisMessage = "[신규 연성 불가] 잡카드가 부족합니다. (보유 ${s.junkCards}장 / 3장 필요)")
            }
            if (s.gold < 300) {
                return@update s.copy(synthesisMessage = "[신규 연성 불가] 수수료 300G가 부족합니다. (보유 ${s.gold}G)")
            }
            val nextId = listOf(1, 2, 112).firstOrNull { id -> s.ownedCards.none { it.id == id } }
            val gainedName = when (nextId) {
                1 -> "태양의 대천사 세라피엘"
                2 -> "바람의 정령궁 엘리리아"
                112 -> "황혼과 욕망의 마왕 릴리스"
                else -> null
            }
            if (nextId == null || gainedName == null) {
                return@update s.copy(
                    junkCards = s.junkCards - 3,
                    gold = s.gold - 300,
                    synthesisMessage = "[신규 연성] 잡카드 3장을 연성했지만 이미 보유한 캐릭터의 조각만 나왔습니다. (금화 -300G, 잡카드 -3)",
                    statusMessage = "[신규 연성] 보유 캐릭터 조각 획득"
                )
            }
            val card = characterCard(nextId)
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

    fun interactRealWorld(activity: RealWorldActivity) {
        _uiState.update { s ->
            val partner = s.ownedCards.firstOrNull { it.id == 3 } ?: characterCard(3)
            val working = partner.copy()
            val newBond = dualWorldEngine.interactInRealWorld(working, activity)
            val visual = working.currentVisual
            // 「기억의 잔향」: 기억이 지워진 동료는 교감으로 기억 파편을 되찾는다.
            val fragmentGain = if (working.isMemoryReset) {
                memoryRecallEngine.grantFragment(working, 1, MemoryFragmentSource.REAL_WORLD_BOND)
            } else {
                null
            }
            val label = when (activity) {
                RealWorldActivity.DAILY_GREETING -> "짧은 일상 대화 (+5)"
                RealWorldActivity.CASUAL_CAFE_WALK -> "저녁 산책·카페 (+15)"
                RealWorldActivity.ROMANTIC_EVENING_DATE -> "카페 데이트 (+30)"
            }
            val unlockedDate = newBond >= 50
            val unlockedUltimate = newBond >= 100
            val logLine = "[현실 교감] 아우렐리아 · $label → 호감도 $newBond/100" +
                    (if (unlockedDate) " · 데이트 룩 해금 연출" else "") +
                    (if (unlockedUltimate) " · 페어 얼티밋 오의 개방!" else "")
            val fragmentLine = if (fragmentGain != null && fragmentGain.isSuccess) fragmentGain.message else null
            s.copy(
                ownedCards = s.ownedCards.map { if (it.id == 3) working else it },
                partnerBond = newBond,
                partnerVisual = visual,
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
        _uiState.update { s ->
            val card = s.ownedCards.firstOrNull { it.id == cardId }
                ?: return@update s.copy(statusMessage = "[교감 불가] 보유하지 않은 카드입니다. (ID: $cardId)")
            if (!card.isMemoryReset) {
                return@update s.copy(
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
        _uiState.update { s ->
            val card = s.ownedCards.firstOrNull { it.id == cardId }
                ?: return@update s.copy(statusMessage = "[회상 불가] 보유하지 않은 카드입니다. (ID: $cardId)")
            val result = memoryRecallEngine.openRecallEvent(card)
            resultOut = result
            if (!result.isSuccess) {
                return@update s.copy(
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
        _uiState.update { s ->
            if (s.trustRiftChoice != null) {
                return@update s.copy(statusMessage = "[교차 이벤트] 이미 선택을 마친 이벤트입니다.")
            }
            val result = trustRiftEngine.resolve(choiceId, s.temptationState, s.partnerBond)
            resultOut = result
            if (!result.isSuccess) {
                return@update s.copy(statusMessage = result.message)
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
        _uiState.update { s ->
            if (s.enemyHp <= 0) {
                return@update s.copy(statusMessage = "[전투 종료] 적이 없습니다. 새 원정을 시작하세요.")
            }
            val attacker = s.party.firstOrNull { it.isAlive && it.isFrontRow }
                ?: s.party.firstOrNull { it.isAlive }
                ?: return@update s.copy(statusMessage = "[오의 불가] 생존한 아군이 없습니다.")

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
                return@update s.copy(
                    battleLog = s.battleLog + result.failureReason.orEmpty(),
                    statusMessage = result.failureReason ?: "[오의 불가] 발동 조건을 만족하지 못했습니다."
                )
            }
            val newEnemyHp = (s.enemyHp - result.damage).coerceAtLeast(0)
            val newMana = s.mana.copy(currentMana = (s.mana.currentMana - result.manaSpent).coerceAtLeast(0))
            val tail = "[턴 ${s.turn}] 적 잔여 HP $newEnemyHp/${s.enemyMaxHp}"

            if (newEnemyHp <= 0) {
                applyVictory(s, newMana, result.logLines + tail).copy(
                    pairUltimateCount = s.pairUltimateCount + 1,
                    pairUltimateBanner = result.bannerTitle,
                    pairUltimateBannerSubtitle = result.bannerSubtitle
                )
            } else {
                s.copy(
                    enemyHp = newEnemyHp,
                    mana = newMana,
                    pairUltimateUsedThisBattle = true,
                    pairUltimateBanner = result.bannerTitle,
                    pairUltimateBannerSubtitle = result.bannerSubtitle,
                    pairUltimateCount = s.pairUltimateCount + 1,
                    battleLog = s.battleLog + result.logLines + tail,
                    statusMessage = "[⚡ 페어 얼티밋 오의] ${attacker.card.name} · ${result.damage} 데미지 — 적 HP $newEnemyHp/${s.enemyMaxHp}"
                )
            }
        }
        return resultOut
    }

    // ──────────────────────────────────────────────────────────────
    // 제2장 프롤로그 — 제1군단장 「그림자 기사」 전초전
    // ──────────────────────────────────────────────────────────────

    /** 5화 이후 제2장 프롤로그 노드 열기 */
    fun openChapter2Prelude() {
        _uiState.update { s ->
            val node: StoryArcNode = ChapterTwoScript.SHADOW_KNIGHT_PRELUDE
            if (!node.isUnlocked(s.storyStepIndex)) {
                return@update s.copy(statusMessage = "[제2장] 제1장 5화를 먼저 읽어야 프롤로그 노드가 열립니다.")
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
        _uiState.update { s ->
            val node = ChapterTwoScript.SHADOW_KNIGHT_PRELUDE
            if (!node.isUnlocked(s.storyStepIndex)) {
                return@update s.copy(statusMessage = "[제2장] 5화 이후에만 전초전을 열 수 있습니다.")
            }
            s.copy(
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
                party = initialParty(),
                retreatInfo = BattleRetreatInfo(turn = 1),
                pairUltimateUsedThisBattle = false,
                pairUltimateBanner = null,
                pairUltimateBannerSubtitle = null,
                pendingLootGold = 1800,
                pendingLootGems = 20,
                chapter2Message = "[${node.chapter}] ${node.title} — ${node.bossSkill}",
                battleLog = s.battleLog + listOf(
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
        val slots = (0 until 5).map { idx -> BattleSlot(slotIndex = idx, isFrontRow = idx < 3) }.toMutableList()
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

    companion object {
        fun characterCard(id: Int): TcgCard = when (id) {
            1 -> TcgCard(
                id = 1,
                name = "태양의 대천사 세라피엘",
                element = Element.LIGHT,
                baseCost = 8,
                atk = 8500,
                def = 7500,
                skillText = "필드 전개 시 아군 전체에 성광 실드를 부여"
            )
            2 -> TcgCard(
                id = 2,
                name = "바람의 정령궁 엘리리아",
                element = Element.WIND,
                baseCost = 6,
                atk = 6400,
                def = 4800,
                skillText = "후열 연사 지원 · 명중 시 적 회피 감소"
            )
            3 -> TcgCard(
                id = 3,
                name = "성기사 아우렐리아",
                element = Element.FIRE,
                baseCost = 5,
                atk = 5600,
                def = 6200,
                bondLevel = 20,
                skillText = "전열 방벽 형성 · 아군 피해 15% 분산"
            )
            112 -> TcgCard(
                id = 112,
                name = "황혼과 욕망의 마왕 릴리스",
                element = Element.DARK,
                baseCost = 10,
                atk = 9800,
                def = 8800,
                skillText = "심연의 매혹 · 매 턴 아군 1명에게 [홀림] 부여"
            )
            else -> TcgCard(
                id = id,
                name = "미등록 카드 #$id",
                element = Element.LIGHT,
                baseCost = 1,
                atk = 1000,
                def = 1000,
                skillText = "샘플 카드"
            )
        }

        fun maxHpOf(card: TcgCard): Int = when (card.id) {
            1 -> 6000
            2 -> 4200
            3 -> 5200
            112 -> 7000
            else -> 4000
        }

        fun gaindNameOf(id: Int): String = characterCard(id).name

        fun initialParty(): List<PartyMember> = listOf(
            PartyMember(characterCard(3), slotIndex = 0, isFrontRow = true, hp = 5200, maxHp = 5200),
            PartyMember(characterCard(1), slotIndex = 1, isFrontRow = true, hp = 6000, maxHp = 6000),
            PartyMember(characterCard(2), slotIndex = 2, isFrontRow = true, hp = 4200, maxHp = 4200)
        )

        fun initialOwnedCards(): List<TcgCard> = listOf(
            characterCard(3), characterCard(1), characterCard(2)
        )

        fun profiles(): List<CharacterProfile> = listOf(
            CharacterProfile(3, "아우렐리아", "새벽의 성기사", Element.FIRE),
            CharacterProfile(1, "세라피엘", "태양의 대천사", Element.LIGHT),
            CharacterProfile(2, "엘리리아", "바람의 정령궁", Element.WIND),
            CharacterProfile(112, "릴리스", "황혼과 욕망의 마왕", Element.DARK, isDemonLord = true, disguiseName = "릴리")
        )
    }
}
