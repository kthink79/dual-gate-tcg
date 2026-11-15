package com.game.tcg.core.model

/**
 * 카드 등급 — 카드팩 확률·연성 대상 판정에 쓰는 4단계.
 * 코스트(소환 난이도)를 그대로 등급으로 쓴다. (낮은 코스트 = 흔한 카드)
 */
enum class CardRarity(val displayName: String, val color: Long, val costRange: IntRange) {
    COMMON("일반", 0xFF9C90B4, 3..4),
    RARE("희귀", 0xFF64B5F6, 5..6),
    EPIC("영웅", 0xFFB388FF, 7..8),
    LEGENDARY("전설", 0xFFFFD54F, 9..10);

    companion object {
        /** 카드의 코스트로 등급을 정한다 */
        fun of(baseCost: Int): CardRarity = when {
            baseCost <= 4 -> COMMON
            baseCost <= 6 -> RARE
            baseCost <= 8 -> EPIC
            else -> LEGENDARY
        }

        fun of(record: CharacterRecord): CardRarity = of(record.baseCost)
    }
}

/** 카드팩 종류 */
enum class PackKind(val displayName: String, val icon: String) {
    STANDARD("일반팩", "🎴"),
    PREMIUM("고급팩", "💎"),
    ELEMENTAL("속성팩", "🔮")
}

/**
 * 카드팩 정의.
 *
 * @param cardCount 개봉 시 획득하는 카드 수
 * @param element 속성팩이면 지정 속성만 등장 (null = 전체)
 * @param guaranteedRarity 이 등급 이상 1장 확정 (null = 확정 없음)
 * @param ticketCost 카드팩 티켓으로도 살 수 있는 매수 (0 = 골드 전용)
 */
data class PackDefinition(
    val id: String,
    val name: String,
    val kind: PackKind,
    val goldCost: Int,
    val ticketCost: Int,
    val cardCount: Int,
    val element: Element? = null,
    val guaranteedRarity: CardRarity? = null,
    val description: String
) {
    /** 상점 카드에 표시할 가격 문구 */
    val priceLabel: String
        get() = buildList {
            if (goldCost > 0) add("${goldCost}G")
            if (ticketCost > 0) add("🎟 ${ticketCost}")
        }.joinToString(" 또는 ")
}

/** 카드팩 1회 개봉에서 나온 카드 1장 */
data class PackDraw(
    val cardId: Int,
    val name: String,
    val rarity: CardRarity,
    val isNew: Boolean,
    val shards: Int = 0,
    val gold: Int = 0
) {
    val line: String
        get() = if (isNew) {
            "★ 신규 계약 · $name (${rarity.displayName})"
        } else {
            "중복 · $name → 조각 +$shards · ${gold}G"
        }
}

/** 카드팩 개봉 결과 */
data class PackOpenResult(
    val packId: String,
    val packName: String,
    val draws: List<PackDraw>,
    val goldSpent: Int,
    val ticketsSpent: Int,
    val newCardIds: List<Int> = emptyList(),
    val totalShards: Int = 0,
    val totalGold: Int = 0,
    val message: String = ""
) {
    val newCardCount: Int get() = newCards.count { it }
    private val newCards: List<Boolean> get() = draws.map { it.isNew }
    val hasNewCard: Boolean get() = newCardIds.isNotEmpty()
}

/**
 * 카드팩 상점 — 일반팩 · 고급팩 · 속성팩 5종.
 *
 * - **일반팩(200G · 3장)**: 저코스트 위주. 시작 직후 덱을 채우는 기본 루트.
 * - **고급팩(600G 또는 🎟1 · 5장)**: 영웅 등급 이상 1장 확정.
 * - **속성팩(400G · 3장)**: 지정 속성만 등장 — 덱의 속성 상성을 맞출 때.
 */
object PackCatalog {

    val STANDARD = PackDefinition(
        id = "pack_standard",
        name = "일반 카드팩",
        kind = PackKind.STANDARD,
        goldCost = 200,
        ticketCost = 1,
        cardCount = 3,
        description = "봉인 카드팩 3장. 낮은 코스트 동료가 주로 나온다."
    )

    val PREMIUM = PackDefinition(
        id = "pack_premium",
        name = "고급 카드팩",
        kind = PackKind.PREMIUM,
        goldCost = 600,
        ticketCost = 1,
        cardCount = 5,
        guaranteedRarity = CardRarity.EPIC,
        description = "5장 개봉 · 영웅(7코스트) 이상 1장 확정."
    )

    fun elemental(element: Element) = PackDefinition(
        id = "pack_element_${element.name.lowercase()}",
        name = "${element.displayName} 속성팩",
        kind = PackKind.ELEMENTAL,
        goldCost = 400,
        ticketCost = 0,
        cardCount = 3,
        element = element,
        description = "${element.displayName} 속성 동료만 3장 등장."
    )

    /**
     * 상점 진열 순서 — 일반 · 고급 · 화염 · 바람 · 빙결 · 빛 · 어둠.
     * (골드·티켓·개봉 로직은 [com.game.tcg.core.engine.PackShopEngine])
     */
    val ALL: List<PackDefinition> = listOf(
        STANDARD,
        PREMIUM,
        elemental(Element.FIRE),
        elemental(Element.WIND),
        elemental(Element.ICE),
        elemental(Element.LIGHT),
        elemental(Element.DARK)
    )

    private val byId: Map<String, PackDefinition> = ALL.associateBy { it.id }

    fun find(id: String): PackDefinition? = byId[id]
}
