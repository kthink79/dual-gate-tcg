package com.game.tcg.core.model

/**
 * 중간 보스 6명 (No.170~175) — 2026-11-16 제작 배치.
 *
 * 챕터 마일스톤(5·10·15·20·25·28 스테이지)에 배치되는 **적 전용 카드**다.
 * 그래서 [CardCatalog]의 플레이 가능 로스터(170명)와 분리해 두고, 이름·칭호·속성·스탯만
 * 같은 [CharacterRecord] 형식으로 등록한다.
 *
 * - 아트: `res/drawable-nodpi/char_{170~175}_{battle|casual|date}.jpg` (3페어 18장)
 *   → [com.game.tcg.ui.CharacterArtRegistry]가 같은 번호 규칙으로 읽는다.
 * - 해당 챕터를 클리어하면 「심연 연성」으로 계약(획득)할 수 있다 ([AdvancedSynthesisEngine]).
 *
 * 이름·수치는 제작 스크립트(tcg_gen/gen_midboss_170_175.py)와 카드 프레임(ATK/DEF)을 그대로 옮겼다.
 * WATER는 이 게임의 5속성 체계에 맞춰 ICE(빙결)로 매핑했다.
 */
object MidBossCatalog {

    /** 중간 보스 도감 번호 구간 (170~175) */
    val IDS: IntRange = 170..175

    val ROSALIND = CharacterRecord(
        id = 170,
        name = "로잘린드",
        title = "심연의 진홍 장미",
        element = Element.DARK,
        atk = 5600,
        def = 4400,
        baseCost = 7,
        skill = "흑장미의 채찍 · 전열 관통, 출혈 3턴",
        isMidBoss = true
    )

    val SIGRUN = CharacterRecord(
        id = 171,
        name = "시그룬",
        title = "천공의 백은 발키리",
        element = Element.LIGHT,
        atk = 4200,
        def = 3600,
        baseCost = 7,
        skill = "백금의 창 · 빛 속성 아군 공격력 +25%",
        isMidBoss = true
    )

    val PAPILIA = CharacterRecord(
        id = 172,
        name = "파필리아",
        title = "영원의 나비 여왕",
        element = Element.WIND,
        atk = 5000,
        def = 4600,
        baseCost = 7,
        skill = "나비의 환영 · 회피 2회, 아군 마나 +2",
        isMidBoss = true
    )

    val NAIADRA = CharacterRecord(
        id = 173,
        name = "나이아드라",
        title = "심해의 창해 마도사",
        element = Element.ICE,
        atk = 5200,
        def = 4800,
        baseCost = 7,
        skill = "심연의 진혼가 · 빙결 광역, 적 마나 -1",
        isMidBoss = true
    )

    val SOLARIA = CharacterRecord(
        id = 174,
        name = "솔라리아",
        title = "성휘의 태양 여사제",
        element = Element.LIGHT,
        atk = 5300,
        def = 4700,
        baseCost = 7,
        skill = "태양의 성광 · 아군 전체 회복 + 방어막",
        isMidBoss = true
    )

    val NYXARIA = CharacterRecord(
        id = 175,
        name = "닉사리아",
        title = "칠흑 염검의 마룡 기사",
        element = Element.FIRE,
        atk = 5800,
        def = 4300,
        baseCost = 7,
        skill = "흑요룡검 · 화염 참격, 방어 무시 20%",
        isMidBoss = true
    )

    val ALL: List<CharacterRecord> = listOf(ROSALIND, SIGRUN, PAPILIA, NAIADRA, SOLARIA, NYXARIA)

    private val byId: Map<Int, CharacterRecord> = ALL.associateBy { it.id }

    fun find(id: Int): CharacterRecord? = byId[id]

    fun isMidBoss(id: Int): Boolean = byId.containsKey(id)

    /** 아트(3페어)가 등록된 번호 — [com.game.tcg.ui.CharacterArtRegistry] 조회에 위임 */
    fun hasArt(cardId: Int): Boolean = byId.containsKey(cardId)
}
