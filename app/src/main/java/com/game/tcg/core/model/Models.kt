package com.game.tcg.core.model

/**
 * 속성 및 상성 시스템
 * - 3원소 삼각 순환: 화염(FIRE) -> 바람(WIND) -> 빙결(ICE) -> 화염 (유리 시 데미지 +30%, 피격 시 -20%)
 * - 2극원소 상호 카운터: 빛(LIGHT) <-> 어둠(DARK) (서로에게 +50% 극딜)
 */
enum class Element(val displayName: String) {
    FIRE("화염"),
    WIND("바람"),
    ICE("빙결"),
    LIGHT("빛"),
    DARK("어둠");

    fun getDamageMultiplier(target: Element): Double = when (this) {
        FIRE -> when (target) {
            WIND -> 1.30
            ICE -> 0.80
            else -> 1.00
        }
        WIND -> when (target) {
            ICE -> 1.30
            FIRE -> 0.80
            else -> 1.00
        }
        ICE -> when (target) {
            FIRE -> 1.30
            WIND -> 0.80
            else -> 1.00
        }
        LIGHT -> when (target) {
            DARK -> 1.50
            else -> 1.00
        }
        DARK -> when (target) {
            LIGHT -> 1.50
            else -> 1.00
        }
    }
}

/**
 * 3단 비주얼 상태 (전투 / 사복 큐트 / 로맨틱 데이트 룩)
 */
enum class VisualState(val displayName: String) {
    BATTLE("전투 TCG 아머"),
    CASUAL_CUTE("일상 사복 큐트"),
    ROMANTIC_DATE("매혹적인 데이트 룩")
}

/**
 * TCG 캐릭터 카드
 */
data class TcgCard(
    val id: Int,                     // 도감 번호 (1~70)
    val name: String,                // 캐릭터명 (예: "태양의 대천사 세라피엘", "백호령 신녀 단아")
    val element: Element,            // 5대 속성
    val baseCost: Int,               // 소환 마나 코스트
    var atk: Int,                    // 공격력
    var def: Int,                    // 방어력/생명력
    var starLevel: Int = 1,          // 성급 (1성 ~ 5성 한계 돌파)
    var currentExp: Int = 0,         // 이세계 원정 획득 누적 EXP
    var bondLevel: Int = 0,          // 현실 일상 교감 호감도 (0~100)
    var currentVisual: VisualState = VisualState.BATTLE,
    val skillText: String
) {
    /**
     * 호감도 MAX(100) 달성 시 개방되는 '페어 얼티밋 오의' 활성화 여부
     */
    val isUltimateUnlocked: Boolean
        get() = bondLevel >= 100
}

/**
 * 배틀 필드 슬롯 구조 (전열 3슬롯, 후열 2슬롯)
 */
data class BattleSlot(
    val slotIndex: Int,
    val isFrontRow: Boolean, // true = 전열 (적 공격 1차 방어선), false = 후열 (지원/원거리)
    var placedCard: TcgCard? = null
)

/**
 * 플레이어 및 덱 상태
 */
data class PlayerDeck(
    val deckName: String,
    val cards: MutableList<TcgCard> = mutableListOf()
)

/**
 * 마나 자원 상태
 */
data class ManaState(
    val currentMana: Int = 1,
    val maxMana: Int = 1
)

/**
 * 덱 유효성 검증 결과 및 코스트 커브 분석
 */
data class DeckValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val costCurve: Map<Int, Int> = emptyMap(), // 코스트별 카드 장수 분포
    val averageCost: Double = 0.0
)

/**
 * 드로우 단계 결과
 */
data class DrawResult(
    val drawnCard: TcgCard? = null,
    val isBurned: Boolean = false, // 최대 핸드 초과로 소멸 여부
    val fatigueDamage: Int = 0,    // 덱 소진 시 누적 탈진 데미지
    val newFatigueCount: Int = 0
)

/**
 * 배틀 필드 슬롯 팩토리 헬퍼 (전열 3 / 후열 2 기본 구성)
 */
object FieldSlotFactory {
    fun createStandardSlots(): List<BattleSlot> = listOf(
        BattleSlot(slotIndex = 0, isFrontRow = true),
        BattleSlot(slotIndex = 1, isFrontRow = true),
        BattleSlot(slotIndex = 2, isFrontRow = true),
        BattleSlot(slotIndex = 3, isFrontRow = false),
        BattleSlot(slotIndex = 4, isFrontRow = false)
    )
}

/**
 * 3대 매혹의 마왕 데이터 모델
 */
data class DemonLord(
    val id: Int,                        // No.112~114
    val name: String,                   // 릴리스, 카밀라, 모르가나
    val title: String,                  // 황혼과 욕망의 마왕 등
    val element: Element,               // DARK, FIRE 등
    val baseHp: Int,
    val attack: Int,
    val disguiseHeroineName: String,    // 릴리, 밀라, 모나
    val disguiseJob: String,            // 심야 라운지 소믈리에, 패션 디자이너, 앤틱 타로술사
    var isDefeated: Boolean = false,    // 토벌 여부 (토벌 전까지 일상 잠입)
    var isVowedCompanion: Boolean = false // 토벌 후 서약 파트너 합류 여부
)

/**
 * 3대 마왕 사전 정의 레지스트리
 */
object DemonLordRegistry {
    val LILITH = DemonLord(
        id = 112,
        name = "릴리스",
        title = "황혼과 욕망의 마왕",
        element = Element.DARK,
        baseHp = 25000,
        attack = 2800,
        disguiseHeroineName = "릴리",
        disguiseJob = "심야 프라이빗 라운지 바텐더/소믈리에"
    )

    val CARMILLA = DemonLord(
        id = 113,
        name = "카밀라",
        title = "선혈과 파멸의 폭군",
        element = Element.FIRE,
        baseHp = 28000,
        attack = 3200,
        disguiseHeroineName = "밀라",
        disguiseJob = "청담 하이엔드 부티크 수석 디자이너"
    )

    val MORGANA = DemonLord(
        id = 114,
        name = "모르가나",
        title = "환영과 심연의 요녀",
        element = Element.DARK,
        baseHp = 22000,
        attack = 2600,
        disguiseHeroineName = "모나",
        disguiseJob = "앤틱 타로 숍 점술가"
    )

    val ALL: List<DemonLord>
        get() = listOf(LILITH, CARMILLA, MORGANA)
}

/**
 * 유저의 마왕 도감 시선/확대 응시 및 일상 잠입 유혹 누적 상태
 */
data class TemptationState(
    var gazeDurationSeconds: Long = 0L,         // 도감/상세창 응시 누적 시간(초)
    var gazeZoomCount: Int = 0,                 // 핀치 줌 확대 횟수
    var gazeTemptationStacks: Int = 0,          // 시선 유혹 스택 (최대 5스택)
    var undercoverEncounterCount: Int = 0,      // 일상에서 위장 마왕과 조우한 횟수
    var undercoverFlirtCount: Int = 0,          // 매혹적 선택지에 흔들린 횟수
    var curseOfArchdemonStacks: Int = 0,        // 잠입 마왕의 저주 누적 스택 (최대 5스택)
    var stolenGold: Int = 0                     // 잠입 중 빼앗긴 금화
) {
    /**
     * 총 유혹 스택 (시선 스택 + 저주 스택, 최대 10)
     */
    val totalTemptationStacks: Int
        get() = (gazeTemptationStacks + curseOfArchdemonStacks).coerceAtMost(10)

    /**
     * UI 연출용: 화면 가장자리 매혹 안개 강도 (0.0f ~ 1.0f)
     */
    val visualMistIntensity: Float
        get() = (totalTemptationStacks / 10.0f).coerceIn(0.0f, 1.0f)

    /**
     * UI 연출용: 심장 박동 사운드 BPM 가속치 (기본 60 -> 최대 120)
     */
    val heartbeatBpm: Int
        get() = 60 + (totalTemptationStacks * 6)
}

/**
 * 전투 돌입 시 아군 카드에 은밀히 적용되는 능력 저감 페널티
 */
data class CombatPenalty(
    val totalStacks: Int,
    val attackReductionPercent: Int,       // 공격력 저감율 (스택당 5%, 최대 35%)
    val damageTakenIncreasePercent: Int,  // 피격 데미지 증가율 (스택당 5%, 최대 35%)
    val criticalReductionPercent: Int,    // 치명타율 저감율 (스택당 4%, 최대 25%)
    val extraManaCost: Int,               // 스킬 마나 코스트 추가 (3스택 이상 시 +1)
    val isCharmed: Boolean,               // 홀림 상태 여부 (스택 >= 1)
    val debuffDescription: String
)

/**
 * 일상 위장 마왕과의 조우 선택지
 */
data class UndercoverDialogueOption(
    val optionId: String,
    val text: String,
    val isTemptedChoice: Boolean,        // 매혹/유혹에 넘어가는 선택지 여부
    val goldStolen: Int = 0,             // 유혹에 넘어가 뺏기는 금화
    val curseAdded: Int = 0              // 추가되는 마왕의 저주 스택
)

