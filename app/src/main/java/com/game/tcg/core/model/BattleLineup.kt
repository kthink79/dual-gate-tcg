package com.game.tcg.core.model

/**
 * 비주얼 전투(VS 화면)의 진영 카드 1장 — 표시 전용 정보.
 * 실제 전투 수치는 기존 [com.game.tcg.core.engine.BattleEngine]이 계산하고,
 * 이 모델은 "무슨 일러스트를 어떤 이름·속성·배지로 보여 줄지"만 담는다.
 */
data class LineupUnit(
    val cardId: Int,
    val name: String,
    val element: Element,
    val atk: Int,
    val def: Int,
    val isBoss: Boolean = false
) {
    /** 카드 모서리 전투력 표기 (ATK + DEF)/100 */
    val displayPower: Int get() = (atk + def) / 100
}

/**
 * 적 진영 편성 — 사용자 덱과 같은 9~12장 규모로 보스 + 수하를 만든다.
 *
 * 적은 실제로는 단일 HP 풀([com.game.tcg.ui.TcgGameState.enemyHp])을 쓰지만,
 * 화면에서는 3열 그리드가 대칭으로 보이도록 같은 수의 카드 일러스트를 배치한다.
 * 보스 카드는 실제 적 이름·속성을 그대로 쓰고, 수하는 같은 속성 카탈로그에서 결정적으로 고른다.
 */
object BattleLineup {

    /** 마왕전 보스 카드 (황혼과 욕망의 마왕 릴리스) */
    const val DEMON_LORD_CARD_ID: Int = 112

    /**
     * 적 진영 카드 목록.
     * @param size 진영 카드 수 (9~12 권장, [DeckRules.MAX_DECK_SIZE]로 상한)
     */
    fun enemyLineup(
        enemyElement: Element,
        enemyName: String,
        size: Int,
        isDemonLordBattle: Boolean = false,
        bossCardId: Int? = null
    ): List<LineupUnit> {
        val count = size.coerceIn(1, DeckRules.MAX_DECK_SIZE)
        val pool = CardCatalog.ALL
        val bossRecord = resolveBossRecord(pool, enemyElement, isDemonLordBattle, bossCardId)

        val boss = LineupUnit(
            cardId = bossRecord?.id ?: DEMON_LORD_CARD_ID,
            name = enemyName.ifBlank { bossRecord?.fullName ?: "심연의 군주" },
            element = bossRecord?.element ?: enemyElement,
            atk = bossRecord?.atk ?: 9800,
            def = bossRecord?.def ?: 8800,
            isBoss = true
        )

        val minionPool = pool
            .filter { it.element == enemyElement && it.id != boss.cardId }
            .ifEmpty { pool.filter { it.id != boss.cardId } }
            .sortedBy { it.id }

        val minions = (1 until count).map { index ->
            val record = minionPool[index % minionPool.size]
            LineupUnit(
                cardId = record.id,
                name = record.fullName,
                element = record.element,
                atk = record.atk,
                def = record.def
            )
        }
        return listOf(boss) + minions
    }

    private fun resolveBossRecord(
        pool: List<CharacterRecord>,
        enemyElement: Element,
        isDemonLordBattle: Boolean,
        bossCardId: Int?
    ): CharacterRecord? {
        bossCardId?.let { id -> CardCatalog.find(id)?.let { return it } }
        if (isDemonLordBattle) return CardCatalog.find(DEMON_LORD_CARD_ID)
        // 일반 원정 보스 — 같은 속성에서 가장 단단한 카드를 보스 아트로 쓴다(마왕 제외).
        return pool
            .filter { it.element == enemyElement && !it.isDemonLord }
            .maxWithOrNull(compareBy<CharacterRecord> { it.def }.thenBy { it.atk })
            ?: pool.maxWithOrNull(compareBy<CharacterRecord> { it.def })
    }

    /** 적 진영 총 전투력 표기 합계 */
    fun totalDisplayPower(units: List<LineupUnit>): Int = units.sumOf { it.displayPower }
}
