package com.game.tcg.core.model

/**
 * 장비 3슬롯 — 무기 · 방어구 · 장신구.
 * 캐릭터 1명당 슬롯별 1개씩, 최대 3개를 장착한다.
 */
enum class EquipmentSlot(val displayName: String, val icon: String) {
    WEAPON("무기", "🗡"),
    ARMOR("방어구", "🛡"),
    ACCESSORY("장신구", "💍")
}

/**
 * 장비 아이템 1개.
 *
 * 장착하면 대상 카드의 ATK/DEF에 보너스가 더해지고 HP 보너스는 전장 최대 HP에 합산된다.
 * ([com.game.tcg.core.engine.EquipmentEngine] 이 장착/해제 시 스탯을 가감한다.)
 *
 * @param equippedTo 장착된 카드 번호 (null = 미장착 · 소지품)
 */
data class EquipmentItem(
    val id: String,
    val name: String,
    val slot: EquipmentSlot,
    val rarity: Int,            // 1 ~ 3 (일반·희귀·전설)
    val atk: Int,
    val def: Int,
    val hp: Int,
    val description: String,
    val goldPrice: Int,         // 상점 구매가 (0 = 상점 판매 없음 · 드롭 전용)
    var equippedTo: Int? = null,
    /** 소지품 개별 식별자 — 같은 장비를 여러 개 가져도 각각 따로 장착·해제한다 */
    val uid: String = id
) {
    val rarityStars: String get() = "★".repeat(rarity) + "☆".repeat((3 - rarity).coerceAtLeast(0))

    /** 상점·드롭 표시용 짧은 스탯 요약 */
    val statSummary: String
        get() = buildList {
            if (atk > 0) add("ATK +$atk")
            if (def > 0) add("DEF +$def")
            if (hp > 0) add("HP +$hp")
        }.joinToString(" · ")
}

/**
 * 장비 도감 — 무기·방어구·장신구 12종.
 * 앞쪽 6종은 상점에서 골드로 구매할 수 있고, 뒤쪽 6종은 스테이지 드롭 전용이다.
 */
object EquipmentCatalog {

    val TRAINING_DAGGER = EquipmentItem(
        id = "weapon_training_dagger",
        name = "훈련용 단검",
        slot = EquipmentSlot.WEAPON,
        rarity = 1,
        atk = 180, def = 0, hp = 0,
        description = "계약자가 처음 받는 짧은 강철 단검.",
        goldPrice = 120
    )

    val SILVER_BLADE = EquipmentItem(
        id = "weapon_silver_blade",
        name = "은빛 장검",
        slot = EquipmentSlot.WEAPON,
        rarity = 2,
        atk = 420, def = 90, hp = 120,
        description = "달빛에 담금질한 성기사단의 장검.",
        goldPrice = 420
    )

    val ABYSS_GREATSWORD = EquipmentItem(
        id = "weapon_abyss_greatsword",
        name = "심연의 대검",
        slot = EquipmentSlot.WEAPON,
        rarity = 3,
        atk = 900, def = 220, hp = 380,
        description = "심연의 문장이 새겨진 대검. 벨 때마다 검은 불꽃이 인다.",
        goldPrice = 0
    )

    val LEATHER_VEST = EquipmentItem(
        id = "armor_leather_vest",
        name = "가죽 조끼",
        slot = EquipmentSlot.ARMOR,
        rarity = 1,
        atk = 0, def = 220, hp = 420,
        description = "가볍고 튼튼한 모험자용 조끼.",
        goldPrice = 150
    )

    val MYTHRIL_MAIL = EquipmentItem(
        id = "armor_mythril_mail",
        name = "미스릴 갑주",
        slot = EquipmentSlot.ARMOR,
        rarity = 2,
        atk = 120, def = 540, hp = 1100,
        description = "미스릴을 엮어 만든 가벼운 판금 갑주.",
        goldPrice = 520
    )

    val TWILIGHT_PLATE = EquipmentItem(
        id = "armor_twilight_plate",
        name = "황혼의 성갑",
        slot = EquipmentSlot.ARMOR,
        rarity = 3,
        atk = 220, def = 980, hp = 2200,
        description = "황혼의 성벽에서 벗겨낸 성갑. 마왕의 일격도 버틴다.",
        goldPrice = 0
    )

    val LUCKY_CHARM = EquipmentItem(
        id = "accessory_lucky_charm",
        name = "행운의 부적",
        slot = EquipmentSlot.ACCESSORY,
        rarity = 1,
        atk = 90, def = 90, hp = 260,
        description = "낡은 종이 부적. 이상하게 위기에서 살아남는다.",
        goldPrice = 180
    )

    val MANA_CIRCLET = EquipmentItem(
        id = "accessory_mana_circlet",
        name = "마나 서클릿",
        slot = EquipmentSlot.ACCESSORY,
        rarity = 2,
        atk = 260, def = 120, hp = 180,
        description = "이마에 닿으면 마나가 맑아지는 은빛 고리.",
        goldPrice = 450
    )

    val DEMON_SEAL = EquipmentItem(
        id = "accessory_demon_seal",
        name = "마왕의 인장",
        slot = EquipmentSlot.ACCESSORY,
        rarity = 3,
        atk = 680, def = 420, hp = 900,
        description = "마왕의 옥좌에서 떨어진 인장. 압도적인 힘이 흐른다.",
        goldPrice = 0
    )

    val FROST_PENDANT = EquipmentItem(
        id = "accessory_frost_pendant",
        name = "서리 결정 펜던트",
        slot = EquipmentSlot.ACCESSORY,
        rarity = 2,
        atk = 200, def = 300, hp = 420,
        description = "빙결의 회랑에서 떨어진 결정. 손끝이 시리다.",
        goldPrice = 0
    )

    val STORM_GAUNTLET = EquipmentItem(
        id = "weapon_storm_gauntlet",
        name = "폭풍의 건틀릿",
        slot = EquipmentSlot.WEAPON,
        rarity = 2,
        atk = 520, def = 140, hp = 200,
        description = "바람의 정원에서 얻은 건틀릿. 휘두르면 바람이 갈라진다.",
        goldPrice = 0
    )

    val EMBER_SHIELD = EquipmentItem(
        id = "armor_ember_shield",
        name = "잿불의 방패",
        slot = EquipmentSlot.ARMOR,
        rarity = 2,
        atk = 60, def = 460, hp = 900,
        description = "황혼의 성문을 지키던 방패. 식지 않는 잿불이 남아 있다.",
        goldPrice = 0
    )

    /** 전체 장비 목록 (상점 판매분 먼저) */
    val ALL: List<EquipmentItem> = listOf(
        TRAINING_DAGGER, LEATHER_VEST, LUCKY_CHARM,
        SILVER_BLADE, MYTHRIL_MAIL, MANA_CIRCLET,
        STORM_GAUNTLET, EMBER_SHIELD, FROST_PENDANT,
        ABYSS_GREATSWORD, TWILIGHT_PLATE, DEMON_SEAL
    )

    /** 상점에서 골드로 살 수 있는 장비 */
    val SHOP_ITEMS: List<EquipmentItem> = ALL.filter { it.goldPrice > 0 }

    private val byId: Map<String, EquipmentItem> = ALL.associateBy { it.id }

    fun find(id: String): EquipmentItem? = byId[id]

    private var instanceSeq: Int = 0

    /**
     * 드롭용 새 장비 인스턴스 — 같은 도감 항목이라도 소지품은 별개 개체다.
     * ([EquipmentItem.equippedTo] 를 개별로 추적해야 하므로 복사본을 만든다.)
     */
    fun instantiate(id: String): EquipmentItem? {
        val base = byId[id] ?: return null
        instanceSeq += 1
        return base.copy(equippedTo = null, uid = "${base.id}#$instanceSeq")
    }
}
