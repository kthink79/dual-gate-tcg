package com.game.tcg.core.model

/**
 * 카드 도감의 정식 등록 기록 (이름·칭호·속성·ATK·DEF·코스트·스킬).
 *
 * v0.3까지 코드 안에 흩어져 있던 `when (id)` 하드코딩을 한 곳으로 모으고,
 * 2026-11-15에 합류한 「개성파」 신규 30인(No.155~184)을 정식 등록한다.
 * 도감·서약·덱·로비·전투·탐색은 모두 이 카탈로그를 단일 진실 원천으로 쓴다.
 */
data class CharacterRecord(
    val id: Int,                      // 도감 번호 (드로어블 접미사 번호와 동일)
    val name: String,                 // 표시 이름 (예: "그레타")
    val title: String,                // 칭호 (예: "강철턱의 선봉장")
    val element: Element,
    val atk: Int,
    val def: Int,
    val baseCost: Int,                // 소환 마나 코스트
    val skill: String,
    val isDemonLord: Boolean = false,
    val disguiseName: String? = null, // 마왕의 일상 위장 신분 이름
    val bondSeed: Int = 0             // 합류 시점 호감도
) {
    /** 카드 전면에 쓰는 전체 이름 */
    val fullName: String get() = "$title $name"

    /** 전장 최대 HP — DEF 기반 (기존 카드 값 6000/4200/5200/7000과 같은 계수) */
    val maxHp: Int
        get() = when (id) {
            1 -> 6000
            2 -> 4200
            3 -> 5200
            112 -> 7000
            else -> (def * 0.85).toInt()
        }

    /** 이 기록으로 실제 전투 카드를 만든다 */
    fun toCard(bondLevel: Int = bondSeed): TcgCard = TcgCard(
        id = id,
        name = fullName,
        element = element,
        baseCost = baseCost,
        atk = atk,
        def = def,
        bondLevel = bondLevel,
        skillText = skill,
        category = CardCategory.CHARACTER
    )
}

/**
 * 도감 카탈로그 — 기존 계약자 4인 + 개성파 신규 30인.
 */
object CardCatalog {

    /** 스타팅 3인방 (아우렐리아 · 세라피엘 · 엘리리아) */
    val STARTER_IDS: List<Int> = listOf(3, 1, 2)

    /** 3대 마왕 (도감 열람 가능 · 112는 토벌 후 서약 파트너) */
    val DEMON_LORD_IDS: List<Int> = listOf(112)

    /** 개성파 신규 30인 (No.155~184) */
    val NEW_WAVE_RANGE: IntRange = 155..184
    val NEW_WAVE_IDS: List<Int> = NEW_WAVE_RANGE.toList()

    /** 개성파 합류 시점 호감도 — 세 가지 비주얼(전투·일상·데이트)을 처음부터 모두 열람할 수 있는 값 */
    const val NEW_WAVE_BOND_SEED: Int = 50

    // ── 기존 계약자 ──────────────────────────────────────────────
    private val AURELIA = CharacterRecord(
        id = 3,
        name = "아우렐리아",
        title = "새벽의 성기사",
        element = Element.FIRE,
        atk = 5600,
        def = 6200,
        baseCost = 5,
        skill = "전열 방벽 형성 · 아군 피해 15% 분산",
        bondSeed = 20
    )

    private val SERAPHIEL = CharacterRecord(
        id = 1,
        name = "세라피엘",
        title = "태양의 대천사",
        element = Element.LIGHT,
        atk = 8500,
        def = 7500,
        baseCost = 8,
        skill = "필드 전개 시 아군 전체에 성광 실드를 부여"
    )

    private val ELYRIA = CharacterRecord(
        id = 2,
        name = "엘리리아",
        title = "바람의 정령궁",
        element = Element.WIND,
        atk = 6400,
        def = 4800,
        baseCost = 6,
        skill = "후열 연사 지원 · 명중 시 적 회피 감소"
    )

    private val LILITH = CharacterRecord(
        id = 112,
        name = "릴리스",
        title = "황혼과 욕망의 마왕",
        element = Element.DARK,
        atk = 9800,
        def = 8800,
        baseCost = 10,
        skill = "심연의 매혹 · 매 턴 아군 1명에게 [홀림] 부여",
        isDemonLord = true,
        disguiseName = "릴리"
    )

    // ── 개성파 신규 30인 (No.155~184) ────────────────────────────
    private val NEW_WAVE: List<CharacterRecord> = listOf(
        CharacterRecord(
            id = 155,
            name = "그레타",
            title = "강철턱의 선봉장",
            element = Element.FIRE,
            atk = 4100,
            def = 3600,
            baseCost = 5,
            skill = "철벽 지휘 · 전열 아군 방어 +20%, 첫 공격 피해 무효",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 156,
            name = "페넬",
            title = "주근깨의 연금술사",
            element = Element.WIND,
            atk = 3200,
            def = 3200,
            baseCost = 4,
            skill = "알렘빅 증류 · 아군 1명의 코스트 -1, 회복량 +30%",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 157,
            name = "주리",
            title = "고글 렌치의 사막 기술자",
            element = Element.FIRE,
            atk = 3500,
            def = 3300,
            baseCost = 4,
            skill = "황동 기관 폭주 · 적 후열에 화염 3연격",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 158,
            name = "벡스",
            title = "면도날의 펑크 암살자",
            element = Element.DARK,
            atk = 4400,
            def = 2600,
            baseCost = 4,
            skill = "철조망 습격 · 적 최저 HP 대상 즉시 처형(잔여 20% 이하)",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 159,
            name = "로잘린",
            title = "벨벳 속삭임의 정보상",
            element = Element.DARK,
            atk = 3000,
            def = 4300,
            baseCost = 5,
            skill = "밀회의 값 · 적 1명의 스킬 봉인, 아군 마나 +1",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 160,
            name = "브리오나",
            title = "맨주먹의 격투가",
            element = Element.FIRE,
            atk = 4300,
            def = 3000,
            baseCost = 5,
            skill = "연타 콤보 · 공격마다 2회 타격, 3타째 방어 무시",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 161,
            name = "마틸다",
            title = "화로빵의 치유사",
            element = Element.LIGHT,
            atk = 2600,
            def = 4300,
            baseCost = 4,
            skill = "갓 구운 빵 · 전열 전체 HP 30% 회복, 상태이상 해제",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 162,
            name = "카이",
            title = "얼룩무늬 야수 조련사",
            element = Element.WIND,
            atk = 3800,
            def = 3100,
            baseCost = 4,
            skill = "야수 돌진 · 소환 시 아군 1명에게 추격 공격 부여",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 163,
            name = "이자벨",
            title = "은빛 덩굴의 검술 사부",
            element = Element.ICE,
            atk = 3900,
            def = 3700,
            baseCost = 5,
            skill = "은빛 세검 · 반격 태세, 피격 시 공격자에게 60% 반사",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 164,
            name = "아미나",
            title = "매바람의 유목민",
            element = Element.WIND,
            atk = 3600,
            def = 3200,
            baseCost = 4,
            skill = "사막 횡단 · 아군 전체 이동 선공, 첫 턴 회피 +25%",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 165,
            name = "누아렐",
            title = "도자기 인형의 묘지기",
            element = Element.DARK,
            atk = 3400,
            def = 3900,
            baseCost = 5,
            skill = "무표정의 응시 · 적 1명의 공격력 -15%(전투 종료까지)",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 166,
            name = "카일라니",
            title = "소금빛 조수 창병",
            element = Element.ICE,
            atk = 3700,
            def = 3400,
            baseCost = 4,
            skill = "파도 찌르기 · 후열에서 관통, 빙결 1스택 부여",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 167,
            name = "지우",
            title = "안경 너머의 마법 해커",
            element = Element.LIGHT,
            atk = 3300,
            def = 3500,
            baseCost = 4,
            skill = "결계 크래킹 · 적 방어 버프 전부 해제, 아군 드로우 +1",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 168,
            name = "미령",
            title = "여우그림자 감찰관",
            element = Element.DARK,
            atk = 4000,
            def = 3000,
            baseCost = 4,
            skill = "밀정의 눈 · 적 손패 1장 봉인, 은신 상태 진입",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 169,
            name = "라니",
            title = "흙칠의 꼬마 투사",
            element = Element.FIRE,
            atk = 3500,
            def = 2900,
            baseCost = 4,
            skill = "투기장의 함성 · 아군 전체 사기 +15%, 자신은 도발",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 170,
            name = "카이 베가",
            title = "네온 선봉 용병",
            element = Element.LIGHT,
            atk = 4200,
            def = 3000,
            baseCost = 5,
            skill = "하드라이트 블레이드 · 전열 2명 동시 베기, 감전 부여",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 171,
            name = "솔레다드",
            title = "포격 조수의 해적 선장",
            element = Element.ICE,
            atk = 4100,
            def = 3500,
            baseCost = 5,
            skill = "현측 일제사격 · 적 전체에 40% 피해, 후열 추가 타격",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 172,
            name = "펜나",
            title = "들녘 정령 농부",
            element = Element.WIND,
            atk = 2600,
            def = 4000,
            baseCost = 4,
            skill = "황금 들녘 · 아군 전체 소량 회복, 마나 1 회복",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 173,
            name = "세이블",
            title = "백매의 암살자",
            element = Element.DARK,
            atk = 4500,
            def = 2700,
            baseCost = 5,
            skill = "낙하 투척 · 후열 즉시 습격, 치명타 시 재행동",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 174,
            name = "오데트",
            title = "아리아의 지휘자",
            element = Element.LIGHT,
            atk = 3900,
            def = 3400,
            baseCost = 5,
            skill = "합창 지휘 · 아군 전체 공격력 +25%(2턴), 스킬 코스트 -1",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 175,
            name = "라이커",
            title = "잿불의 락커",
            element = Element.FIRE,
            atk = 4000,
            def = 3100,
            baseCost = 4,
            skill = "방화선 리프 · 적 전체에 화상 2스택, 아군 사기 +10%",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 176,
            name = "나디라",
            title = "수관의 수호자",
            element = Element.WIND,
            atk = 3800,
            def = 3600,
            baseCost = 5,
            skill = "정글 도약 · 후열 관통 습격, 수호 정령 소환",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 177,
            name = "은혜",
            title = "맥을 짚는 약초사",
            element = Element.LIGHT,
            atk = 2800,
            def = 4200,
            baseCost = 4,
            skill = "맥진단 · 아군 1명 완전 회복, 3턴 재생 부여",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 178,
            name = "모르바나",
            title = "밤의 궁정 창백한 귀부인",
            element = Element.DARK,
            atk = 4300,
            def = 3200,
            baseCost = 5,
            skill = "혈월의 잔 · 적 HP 10% 흡수, 아군 1명 [홀림] 부여",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 179,
            name = "잉그리드",
            title = "설원의 덫 사냥꾼",
            element = Element.ICE,
            atk = 3600,
            def = 3300,
            baseCost = 4,
            skill = "올가미 매복 · 적 1명 이동 봉쇄, 빙결 2스택",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 180,
            name = "실베인",
            title = "주술 필사의 저주술사",
            element = Element.DARK,
            atk = 4400,
            def = 3000,
            baseCost = 5,
            skill = "저주 각인 · 적 1명 매 턴 200 고정 피해(4턴), 아군 저주 해제",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 181,
            name = "브란나",
            title = "화로심장 대장장이",
            element = Element.FIRE,
            atk = 3700,
            def = 4000,
            baseCost = 5,
            skill = "천둥 망치 · 전열 광역 강타, 아군 장비 1개 강화",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 182,
            name = "서니",
            title = "부기 비트의 마도사",
            element = Element.LIGHT,
            atk = 3300,
            def = 3600,
            baseCost = 4,
            skill = "거울볼 비트 · 아군 전체 마나 +1, 적 1명 혼란",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 183,
            name = "핼리",
            title = "고글 날개의 에이스",
            element = Element.WIND,
            atk = 3900,
            def = 3200,
            baseCost = 4,
            skill = "급상승 강습 · 첫 공격 선제, 적 후열 1명 기절",
            bondSeed = NEW_WAVE_BOND_SEED
        ),
        CharacterRecord(
            id = 184,
            name = "아메넷",
            title = "태양 장막의 예언자",
            element = Element.LIGHT,
            atk = 4000,
            def = 3800,
            baseCost = 5,
            skill = "태양 원반 · 아군 전체 성광 실드, 다음 턴 공격력 +20%",
            bondSeed = NEW_WAVE_BOND_SEED
        )
    )

    /** 전체 등록 기록 (도감 번호 오름차순) */
    val ALL: List<CharacterRecord> = (listOf(AURELIA, SERAPHIEL, ELYRIA, LILITH) + NEW_WAVE)
        .sortedBy { it.id }

    private val byId: Map<Int, CharacterRecord> = ALL.associateBy { it.id }

    fun find(id: Int): CharacterRecord? = byId[id]

    /** 미등록 번호용 안전한 대체 기록 (게임이 깨지지 않게) */
    fun require(id: Int): CharacterRecord =
        byId[id] ?: CharacterRecord(
            id = id,
            name = "미등록 카드 #$id",
            title = "미등록",
            element = Element.LIGHT,
            atk = 1000,
            def = 1000,
            baseCost = 1,
            skill = "샘플 카드"
        )

    fun newWave(): List<CharacterRecord> = NEW_WAVE

    fun isNewWave(id: Int): Boolean = id in NEW_WAVE_RANGE

    /** 합류(보유) 상태로 시작하는 카드 — 스타팅 3인 + 개성파 30인 */
    fun initialOwnedRecords(): List<CharacterRecord> =
        STARTER_IDS.map { require(it) } + newWave()

    /** 아직 계약하지 않은 카드 (신규 연성 대상) — 마왕 112번 등 */
    fun unownedCandidates(ownedIds: Collection<Int>): List<CharacterRecord> =
        ALL.filter { it.id !in ownedIds && !isNewWave(it.id) }
}
