package com.game.tcg.core.model

/** 스테이지 보스 종류 */
enum class BossKind(val displayName: String) {
    NONE("일반"),
    MID_BOSS("중간 보스"),
    DEMON_LORD("대마왕")
}

/**
 * 스테이지 클리어 보상.
 *
 * @param shardCardId 조각 보상 대상 카드 번호 (null = 조각 없음)
 * @param equipmentId 확률 드롭 장비 id (null = 드롭 없음)
 * @param equipmentChance 드롭 확률 (0.0 ~ 1.0). 1.0 = 확정
 */
data class StageReward(
    val gold: Int,
    val exp: Int,
    val packTickets: Int = 0,
    val gems: Int = 0,
    val shardCardId: Int? = null,
    val shardCount: Int = 0,
    val equipmentId: String? = null,
    val equipmentChance: Double = 0.0
) {
    /** 보상 미리보기 한 줄 */
    val preview: String
        get() = buildList {
            add("🪙 ${gold}G")
            add("EXP $exp")
            if (packTickets > 0) add("🎟 $packTickets")
            if (gems > 0) add("💎 $gems")
            if (shardCardId != null && shardCount > 0) add("조각 +$shardCount")
            if (equipmentId != null) {
                val name = EquipmentCatalog.find(equipmentId)?.name ?: equipmentId
                add(if (equipmentChance >= 1.0) "장비 $name" else "장비 $name ${(equipmentChance * 100).toInt()}%")
            }
        }.joinToString(" · ")
}

/**
 * 스테이지 1개 정의.
 *
 * @param recommendedPower 권장 덱 전투력 (덱 카드 ATK+DEF 합계)
 * @param bossId 보스 카드 번호 (중간 보스 170~175 · 마왕 112~114)
 */
data class StageDefinition(
    val number: Int,
    val chapter: Int,
    val chapterName: String,
    val name: String,
    val story: String,
    val recommendedPower: Int,
    val enemyName: String,
    val enemyElement: Element,
    val enemyHp: Int,
    val enemyAtk: Int,
    val reward: StageReward,
    val bossId: Int? = null,
    val bossKind: BossKind = BossKind.NONE
) {
    val isBoss: Boolean get() = bossKind != BossKind.NONE

    /** 챕터 안에서의 순번 (1~5) */
    val indexInChapter: Int get() = (number - 1) % StageCatalog.STAGES_PER_CHAPTER + 1

    /** 보스 카드 기록 (보스 스테이지가 아니면 null) */
    val bossRecord: CharacterRecord? get() = bossId?.let { CardCatalog.find(it) }

    /** 목록에 쓰는 짧은 제목 */
    val label: String get() = "${number}. $name"
}

/**
 * 캠페인 스테이지 30개 — 6챕터 × 5스테이지.
 *
 * ```
 * 1장 균열의 시작   1  2  3  4  [5  중간보스 로잘린드]
 * 2장 잿빛 성벽     6  7  8  9  [10 중간보스 시그룬]
 * 3장 빙결의 회랑  11 12 13 14  [15 중간보스 파필리아]
 * 4장 폭풍의 정원  16 17 18 19  [20 중간보스 나이아드라]
 * 5장 황혼의 성    21 22 23 24  [25 중간보스 솔라리아]
 * 6장 심연의 옥좌  26 [마왕 릴리스]  27  28 [중간보스 닉사리아]  29 [마왕 카밀라]  30 [최종 마왕 모르가나]
 * ```
 *
 * 권장 전투력은 34,000(1스테이지)에서 230,000(최종)까지 오르고,
 * 2026-11-17 난이도 개편으로 **5스테이지부터 스테이지당 +2.5%** 적 스탯이 추가로 올라간다.
 * 2026-11-18 다중 적 개편으로 적은 9~12장의 개별 HP·공격력을 가진 카드가 되었고,
 * 1~5스테이지 권장 전투력도 34,000~64,000 으로 올려 **기본 1성/무강화 3장 덱은 3스테이지가 한계**,
 * 5스테이지 중간 보스부터는 덱을 더 채우고 강화해야 뚫린다.
 */
object StageCatalog {

    const val CHAPTER_COUNT: Int = 6
    const val STAGES_PER_CHAPTER: Int = 5
    const val TOTAL_STAGES: Int = CHAPTER_COUNT * STAGES_PER_CHAPTER

    /**
     * 5스테이지 이후 스테이지마다 적 스탯이 추가로 오르는 비율.
     * 6스테이지 +2.5% … 30스테이지 +62.5% (권장 전투력 곡선 위에 한 번 더 얹힌다)
     */
    const val DIFFICULTY_RAMP_PER_STAGE: Double = 0.025

    /** 스테이지 번호 → 추가 난이도 배율 (1~5스테이지 = 1.0) */
    fun difficultyScale(stageNumber: Int): Double =
        1.0 + (stageNumber - STAGES_PER_CHAPTER).coerceAtLeast(0) * DIFFICULTY_RAMP_PER_STAGE

    /** 챕터 이름 (1~6) */
    val CHAPTER_NAMES: List<String> = listOf(
        "균열의 시작",
        "잿빛 성벽",
        "빙결의 회랑",
        "폭풍의 정원",
        "황혼의 성",
        "심연의 옥좌"
    )

    /** 중간 보스가 서는 스테이지 (5·10·15·20·25·28) → No.170~175 */
    val MID_BOSS_STAGES: Map<Int, Int> = linkedMapOf(
        5 to 170,
        10 to 171,
        15 to 172,
        20 to 173,
        25 to 174,
        28 to 175
    )

    /** 3대 마왕이 서는 스테이지 (26·29·30) → No.112~114 */
    val DEMON_LORD_STAGES: Map<Int, Int> = linkedMapOf(
        26 to 112,
        29 to 113,
        30 to 114
    )

    private fun chapterName(chapter: Int): String = CHAPTER_NAMES.getOrElse(chapter - 1) { "챕터 $chapter" }

    /**
     * 스테이지 1개를 만든다. 적 HP·공격력은 권장 전투력에서 파생해 밸런스를 한 곳에서 관리한다.
     * (보스는 HP 1.35배 · 공격력 1.2배)
     */
    private fun stage(
        number: Int,
        name: String,
        story: String,
        power: Int,
        enemyName: String,
        element: Element,
        gold: Int,
        exp: Int,
        tickets: Int = 0,
        gems: Int = 0,
        shardCardId: Int? = null,
        shardCount: Int = 0,
        equipmentId: String? = null,
        equipmentChance: Double = 0.0,
        bossId: Int? = null,
        bossKind: BossKind = BossKind.NONE
    ): StageDefinition {
        // ── 적 스탯 (2026-11-18 다중 적 개편) ─────────────────────────
        // 사용자: "적 진영 카드들이 개별 HP·공격력을 가진 다수 카드로 순차/동시 교전하고,
        //          1~4스테이지부터 적 스탯·권장 전투력을 상향해 달라".
        //
        // 권장 전투력 R 을 기준으로
        //   적 진영 **총** HP = R × [hpFactor]   → 카드 9~12장에 나눠 담는다([com.game.tcg.core.model.EnemySquad])
        //   적 **카드 1장** 공격력 = R ÷ [attackDivisor]  (한 턴에 전열 최대 3장이 각자 반격)
        // 를 잡고, 5스테이지부터 [DIFFICULTY_RAMP_PER_STAGE] 씩 추가로 올린다.
        // (2026-11-17 까지는 단일 HP 풀 + 한 턴 1회 반격이었기 때문에 분모가 72/62/55 로 작았다.
        //  다중 적으로 바꾸면서 반격이 최대 3회가 되었으므로 분모를 160/150/140 로 키워
        //  한 턴 총 위협은 이전과 비슷한 수준으로 유지한다 — 대신 1~4스테이지 권장 전투력을 올렸다.)
        val hpFactor = when (bossKind) {
            BossKind.DEMON_LORD -> 0.78
            BossKind.MID_BOSS -> 0.62
            else -> 0.55
        }
        val attackDivisor = when (bossKind) {
            BossKind.DEMON_LORD -> 140.0
            BossKind.MID_BOSS -> 150.0
            else -> 160.0
        }
        val scale = difficultyScale(number)
        return StageDefinition(
            number = number,
            chapter = (number - 1) / STAGES_PER_CHAPTER + 1,
            chapterName = chapterName((number - 1) / STAGES_PER_CHAPTER + 1),
            name = name,
            story = story,
            recommendedPower = power,
            enemyName = enemyName,
            enemyElement = element,
            enemyHp = (power * hpFactor * scale).toInt(),
            enemyAtk = (power / attackDivisor * scale).toInt(),
            reward = StageReward(
                gold = gold,
                exp = exp,
                packTickets = tickets,
                gems = gems,
                shardCardId = shardCardId,
                shardCount = shardCount,
                equipmentId = equipmentId,
                equipmentChance = equipmentChance
            ),
            bossId = bossId,
            bossKind = bossKind
        )
    }

    /** 30스테이지 전체 */
    val STAGES: List<StageDefinition> = listOf(
        // ── 1장 · 균열의 시작 ──
        stage(1, "젖은 골목의 첫 균열", "카드팩을 주운 골목에 손톱만 한 균열이 벌어졌다.", 34000,
            "균열의 슬라임", Element.WIND, gold = 85, exp = 44, shardCardId = 24, shardCount = 5),
        stage(2, "편의점 앞 그림자", "가로등 밑 그림자가 주인을 잃고 움직인다.", 42000,
            "그림자 들개", Element.DARK, gold = 110, exp = 48, shardCardId = 91, shardCount = 5),
        stage(3, "옥상의 비늘", "아파트 옥상, 비늘 달린 앞발이 난간을 쥐었다.", 50000,
            "비늘 마수", Element.ICE, gold = 135, exp = 52, equipmentId = "weapon_training_dagger", equipmentChance = 0.6),
        stage(4, "지하철 4번 출구", "유리벽에 비친 문 너머에서 무언가 손을 내민다.", 56000,
            "차원 잠복자", Element.LIGHT, gold = 160, exp = 56, tickets = 1, shardCardId = 164, shardCount = 5),
        stage(5, "홍장미의 계단", "계단을 오르자 검붉은 장미 향이 목을 조인다.", 64000,
            "심연의 홍장미 여제 로잘린드", Element.DARK, gold = 260, exp = 90, tickets = 2, gems = 5,
            shardCardId = 170, shardCount = 10, equipmentId = "armor_leather_vest", equipmentChance = 1.0,
            bossId = 170, bossKind = BossKind.MID_BOSS),

        // ── 2장 · 잿빛 성벽 ──
        stage(6, "잿빛 성벽 아래", "무너진 성벽에서 갑주 소리가 들린다.", 102000,
            "성벽의 갑주병", Element.FIRE, gold = 185, exp = 64, shardCardId = 12, shardCount = 6),
        stage(7, "백금의 깃털", "성벽 위로 백금빛 깃털이 흩날린다.", 108000,
            "백금의 수호자", Element.LIGHT, gold = 210, exp = 68, tickets = 1),
        stage(8, "부서진 예배당", "예배당 제단에 검은 성수가 고여 있다.", 114000,
            "타락한 사제", Element.DARK, gold = 235, exp = 72, shardCardId = 4, shardCount = 6),
        stage(9, "성벽의 회랑", "회랑을 지키는 창들이 일제히 겨눠진다.", 120000,
            "회랑의 창병", Element.WIND, gold = 260, exp = 76, equipmentId = "accessory_lucky_charm", equipmentChance = 0.5),
        stage(10, "백금의 창공", "하늘에서 거대한 날개가 그림자를 드리운다.", 128000,
            "창공의 백금 발키리 시그룬", Element.LIGHT, gold = 420, exp = 110, tickets = 2, gems = 8,
            shardCardId = 171, shardCount = 10, equipmentId = "weapon_silver_blade", equipmentChance = 1.0,
            bossId = 171, bossKind = BossKind.MID_BOSS),

        // ── 3장 · 빙결의 회랑 ──
        stage(11, "얼어붙은 계단", "숨을 내쉴 때마다 얼음 결정이 부서진다.", 132000,
            "서리 파수꾼", Element.ICE, gold = 285, exp = 84, shardCardId = 128, shardCount = 6),
        stage(12, "결빙된 도서관", "책장 사이로 파란 불빛이 떠다닌다.", 136000,
            "빙결의 서고령", Element.ICE, gold = 310, exp = 88, tickets = 1),
        stage(13, "나비의 온실", "얼음 온실 안에서 나비들이 살아 움직인다.", 140000,
            "서리 나비", Element.WIND, gold = 335, exp = 92, shardCardId = 172, shardCount = 4),
        stage(14, "심해의 거울", "회랑 끝 거울에 심해가 비친다.", 144000,
            "거울 속 심해어", Element.ICE, gold = 360, exp = 96, equipmentId = "accessory_frost_pendant", equipmentChance = 0.6),
        stage(15, "영원의 나비 정원", "정원 전체가 나비 날개로 뒤덮인다.", 152000,
            "영원의 나비 여왕 파필리아", Element.WIND, gold = 620, exp = 130, tickets = 2, gems = 10,
            shardCardId = 172, shardCount = 12, equipmentId = "weapon_storm_gauntlet", equipmentChance = 1.0,
            bossId = 172, bossKind = BossKind.MID_BOSS),

        // ── 4장 · 폭풍의 정원 ──
        stage(16, "폭풍의 울타리", "바람이 울타리를 뿌리째 뽑아 던진다.", 156000,
            "폭풍의 정령", Element.WIND, gold = 385, exp = 104, shardCardId = 25, shardCount = 7),
        stage(17, "천 개의 화살", "정원수 사이에서 시위 소리가 동시에 울린다.", 160000,
            "정원의 궁수", Element.WIND, gold = 410, exp = 108, tickets = 1),
        stage(18, "메마른 샘", "물이 마른 샘에서 모래가 솟구친다.", 164000,
            "모래의 마도사", Element.FIRE, gold = 435, exp = 112, shardCardId = 102, shardCount = 7),
        stage(19, "잿불의 문", "정원 끝, 잿불이 타는 성문이 보인다.", 168000,
            "잿불의 수문장", Element.FIRE, gold = 460, exp = 116, equipmentId = "armor_ember_shield", equipmentChance = 0.5),
        stage(20, "심해의 대현자", "정원 아래로 심해가 열리고 진혼가가 들린다.", 176000,
            "심해의 대현자 나이아드라", Element.ICE, gold = 820, exp = 150, tickets = 3, gems = 12,
            shardCardId = 173, shardCount = 12, equipmentId = "armor_mythril_mail", equipmentChance = 1.0,
            bossId = 173, bossKind = BossKind.MID_BOSS),

        // ── 5장 · 황혼의 성 ──
        stage(21, "황혼의 성문", "성문이 스스로 열리며 황혼빛이 흘러나온다.", 180000,
            "황혼의 문지기", Element.DARK, gold = 485, exp = 124, shardCardId = 112, shardCount = 4),
        stage(22, "붉은 연회장", "긴 식탁 위에 식지 않은 잔이 놓여 있다.", 184000,
            "연회장의 시종", Element.DARK, gold = 510, exp = 128, tickets = 1),
        stage(23, "가시 덩굴의 복도", "복도 벽을 가시 덩굴이 덮고 있다.", 188000,
            "가시 덩굴 마수", Element.WIND, gold = 535, exp = 132, shardCardId = 175, shardCount = 4),
        stage(24, "백금의 예배당", "빛이 새어드는 예배당에 성가가 울린다.", 192000,
            "광휘의 성가대", Element.LIGHT, gold = 560, exp = 136, equipmentId = "accessory_mana_circlet", equipmentChance = 0.6),
        stage(25, "태양의 성광", "성당 천장이 열리고 태양이 내려앉는다.", 200000,
            "광휘의 태양 사제 솔라리아", Element.LIGHT, gold = 1050, exp = 175, tickets = 3, gems = 15,
            shardCardId = 174, shardCount = 14, equipmentId = "weapon_abyss_greatsword", equipmentChance = 1.0,
            bossId = 174, bossKind = BossKind.MID_BOSS),

        // ── 6장 · 심연의 옥좌 ──
        stage(26, "황혼과 욕망의 옥좌", "옥좌에 기대 앉은 그림자가 손가락을 튕긴다.", 206000,
            "황혼과 욕망의 마왕 릴리스", Element.DARK, gold = 1200, exp = 200, tickets = 3, gems = 20,
            shardCardId = 112, shardCount = 15, equipmentId = "accessory_demon_seal", equipmentChance = 0.8,
            bossId = 112, bossKind = BossKind.DEMON_LORD),
        stage(27, "피의 회랑", "회랑 바닥의 핏자국이 아직 따뜻하다.", 210000,
            "선혈의 기사", Element.FIRE, gold = 700, exp = 160, shardCardId = 113, shardCount = 5),
        stage(28, "흑요룡의 검", "검은 비늘이 검집에서 미끄러진다.", 216000,
            "흑요룡검 기사 닉사리아", Element.FIRE, gold = 1500, exp = 220, tickets = 4, gems = 20,
            shardCardId = 175, shardCount = 15, equipmentId = "armor_twilight_plate", equipmentChance = 1.0,
            bossId = 175, bossKind = BossKind.MID_BOSS),
        stage(29, "선혈과 파멸의 왕좌", "왕좌가 피로 물들고 폭군이 일어선다.", 222000,
            "선혈과 파멸의 폭군 카밀라", Element.FIRE, gold = 1700, exp = 240, tickets = 4, gems = 25,
            shardCardId = 113, shardCount = 15, bossId = 113, bossKind = BossKind.DEMON_LORD),
        stage(30, "심연의 마지막 문", "심연의 문이 열리고 모든 것이 멈춘다.", 230000,
            "환영과 심연의 요녀 모르가나", Element.DARK, gold = 3000, exp = 400, tickets = 5, gems = 40,
            shardCardId = 114, shardCount = 20, bossId = 114, bossKind = BossKind.DEMON_LORD)
    )

    private val byNumber: Map<Int, StageDefinition> = STAGES.associateBy { it.number }

    fun find(number: Int): StageDefinition? = byNumber[number]

    fun require(number: Int): StageDefinition = byNumber[number] ?: STAGES.first()

    fun chapter(chapter: Int): List<StageDefinition> = STAGES.filter { it.chapter == chapter }

    /** 챕터별 마지막 스테이지 (보스 관문) */
    fun chapterBoss(chapter: Int): StageDefinition? = chapter(chapter).lastOrNull()

    /** 중간 보스 스테이지 목록 (5·10·15·20·25·28) */
    fun midBossStages(): List<StageDefinition> = MID_BOSS_STAGES.keys.map { require(it) }

    /** 3대 마왕 스테이지 (26·29·30) */
    fun demonLordStages(): List<StageDefinition> = DEMON_LORD_STAGES.keys.map { require(it) }

    /** 해당 스테이지가 보스 관문인지 */
    fun isBossStage(number: Int): Boolean = find(number)?.isBoss == true

    /** 마지막 스테이지 번호 */
    val LAST_STAGE: Int = STAGES.size
}
