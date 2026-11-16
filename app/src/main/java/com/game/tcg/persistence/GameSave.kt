package com.game.tcg.persistence

/**
 * 《듀얼 게이트》 진행 상태 저장 모델.
 *
 * 화면 연출용 임시 값(데미지 팝업 틱·전투 로그·결과 배너 등)은 담지 않고,
 * **앱을 껐다 켜도 이어져야 하는 플레이어 진행 상태 전체**만 담는다.
 * - 보유 카드의 레벨·성급·스탯·호감도·기억 상태
 * - 전열/후열 편성과 예비대, 환생(재획득 대기) 풀
 * - 골드·보석·성수·티켓·조각 지갑·보유/장착 장비
 * - 클리어한 스테이지·선택 스테이지·스토리 진행도
 * - 긴급 보급 사용 횟수, 인트로 시청 여부, BGM·SFX 음소거 설정
 *
 * 이 모델은 Android에 의존하지 않으므로 JVM 단위 테스트에서 그대로 왕복 검증할 수 있다.
 * (직렬화는 [SaveCodec], 파일 입출력은 [SaveStore] 담당)
 */
data class GameSave(
    // ── 재화 ──
    val gold: Int = 0,
    val gems: Int = 0,
    val holyWater: Int = 0,
    val junkCards: Int = 0,
    val packTickets: Int = 0,
    /** 카드별 기억 조각 지갑 (카드 번호 → 보유 조각 수) */
    val shards: Map<Int, Int> = emptyMap(),
    val playerExp: Int = 0,
    // ── 스토리 ──
    val storyStepIndex: Int = 0,
    val storyReadLog: List<String> = emptyList(),
    val eventAChoice: String? = null,
    val eventAResult: String? = null,
    val eventBChoice: String? = null,
    val eventBResult: String? = null,
    val lilyFamiliarity: Int = 0,
    val defenseBonusPercent: Int = 0,
    val intelBonusPercent: Int = 0,
    val chapter2PreludeOpened: Boolean = false,
    // ── 유혹(홀림) ──
    val temptation: SavedTemptation = SavedTemptation(),
    // ── 현실 교감 ──
    val partnerBond: Int = 20,
    val partnerVisual: String = "BATTLE",
    val dateLookUnlocked: Boolean = false,
    val ultimateUnlocked: Boolean = false,
    val frontRowDefensePenaltyPercent: Int = 0,
    val trustRiftChoice: String? = null,
    // ── 진행 카운터 ──
    val battleVictories: Int = 0,
    val pairUltimateCount: Int = 0,
    val summonCount: Int = 0,
    val summonBuffPercent: Int = 0,
    val summonBuffBattles: Int = 0,
    val explorationCount: Int = 0,
    val explorationLog: List<String> = emptyList(),
    val gateEnergy: Int = 10,
    // ── 보유 카드 · 편성 · 환생 풀 ──
    val cards: List<SavedCard> = emptyList(),
    val party: List<SavedPartyMember> = emptyList(),
    val reserveIds: List<Int> = emptyList(),
    val reincarnations: List<SavedReincarnation> = emptyList(),
    // ── 장비 ──
    val equipment: List<SavedEquipment> = emptyList(),
    // ── 스테이지 ──
    val stageCleared: List<Int> = emptyList(),
    val selectedStage: Int = 1,
    // ── 마왕 토벌 ──
    val defeatedDemonLords: List<Int> = emptyList(),
    val vowedDemonLords: List<Int> = emptyList(),
    // ── 화면 선택 상태 ──
    val lobbyHeroId: Int = 3,
    val lobbyVisual: String = "BATTLE",
    val growthCardId: Int = 0,
    val summonCardId: Int = 0,
    val selectedCardId: Int = 3,
    val selectedVisual: String = "BATTLE",
    // ── 인트로 ──
    val introSeen: Boolean = false,
    val introReplayCount: Int = 0,
    // ── 오디오 ──
    val bgmEnabled: Boolean = true,
    val sfxEnabled: Boolean = true,
    val bgmVolume: Float = 0.65f,
    val sfxVolume: Float = 0.90f
) {
    companion object {
        /** 저장 포맷 버전 — 필드 구성이 바뀌면 올린다 */
        const val VERSION: Int = 1

        /** 이 앱이 읽을 수 있는 가장 오래된 저장 포맷 버전 */
        const val MIN_SUPPORTED_VERSION: Int = 1
    }
}

/** 보유 카드 1장의 진행 수치 (이름·속성·스킬 등 고정 정의는 도감에서 다시 만든다) */
data class SavedCard(
    val cardId: Int,
    val atk: Int,
    val def: Int,
    val starLevel: Int = 1,
    val currentExp: Int = 0,
    val bondLevel: Int = 0,
    val upgradeLevel: Int = 0,
    val memoryAffinity: Int = 0,
    val reincarnationCount: Int = 0,
    val equippedRelicCount: Int = 0,
    val isMemoryReset: Boolean = false,
    val memoryFragments: Int = 0,
    val visual: String = "BATTLE"
)

/** 편성 슬롯 1칸 (전열 0~2 · 후열/대기 3~11) */
data class SavedPartyMember(
    val cardId: Int,
    val slotIndex: Int,
    val hp: Int,
    val maxHp: Int,
    val memoryAffinity: Int = 0
)

/** 환생 대기 카드 1장 */
data class SavedReincarnation(
    val templateId: Int,
    val reincarnationCount: Int,
    val lostUpgradeLevel: Int,
    val lostMemoryAffinity: Int,
    val lostBondLevel: Int,
    val lostStarLevel: Int,
    val lostRelicCount: Int,
    val causeOfDeath: String,
    val diedAtTurn: Int
)

/** 소지/장착 장비 1개 */
data class SavedEquipment(
    val uid: String,
    val itemId: String,
    val equippedTo: Int? = null
)

/** 유혹(홀림) 스택·변화율 묶음 */
data class SavedTemptation(
    val gazeDurationSeconds: Long = 0L,
    val gazeZoomCount: Int = 0,
    val gazeTemptationStacks: Int = 0,
    val undercoverEncounterCount: Int = 0,
    val undercoverFlirtCount: Int = 0,
    val curseOfArchdemonStacks: Int = 0,
    val stolenGold: Int = 0
)
