package com.game.tcg.ui

/**
 * 메인 로비 「📊 정보」 패널의 데이터 모델.
 *
 * 사용자 요청(2026-11-16): 로비 상단에 4줄로 겹쳐 있던 정보(장·차원·재화·티켓·스테이지·덱 전투력·
 * 보유 인원·정신 상태·자동 저장·인트로 다시보기·새 게임)가 캐릭터 얼굴을 가린다.
 * → 상단은 "재화/차원 요약 1줄 + 📊 정보"만 남기고, 나머지 정보는 이 패널로 모아 옮긴다.
 *
 * 이 파일은 Compose 에 의존하지 않는 순수 모델이라(JVM 단위 테스트 가능) 로비 상태가
 * "무엇을 어디에 보여 주는가"를 테스트로 고정할 수 있다.
 */
enum class InfoTone {
    NEUTRAL,
    GOLD,
    VIOLET,
    HEAL,
    DANGER,
    MANA,
    ABYSS
}

/** 정보 패널의 한 줄 — 아이콘 · 항목명 · 값 */
data class LobbyInfoEntry(
    val icon: String,
    val label: String,
    val value: String,
    val tone: InfoTone = InfoTone.NEUTRAL
) {
    /** 값이 비어 있으면 항목명만 (상단 요약의 재화 칩처럼) */
    val line: String get() = if (label.isBlank()) "$icon $value".trim() else "$icon $label $value".trim()
}

/** 정보 패널의 한 묶음 */
data class LobbyInfoSection(
    val title: String,
    val entries: List<LobbyInfoEntry>
)

object LobbyInfoPanel {

    /** 로비 상단의 패널 열기 버튼 라벨 */
    const val BUTTON_LABEL = "📊 정보"

    /** 패널 제목 */
    const val TITLE = "정보"

    /** 자동 저장 안내 (설정 묶음의 마지막 줄) */
    const val AUTOSAVE_LABEL = "진행 자동 저장"
    const val AUTOSAVE_VALUE = "켜짐 · 앱을 닫아도 이어서"

    /**
     * 로비 최상단 1줄 요약 — 차원 에너지와 3대 재화만 남긴다(캐릭터를 가리지 않는 최소 정보).
     * 경보·진행도·설정은 모두 [sections]로 옮겼다.
     */
    fun topSummary(state: TcgGameState): List<LobbyInfoEntry> = listOf(
        LobbyInfoEntry("⚡", "", "${state.gateEnergy}/${state.gateEnergyMax}", InfoTone.MANA),
        LobbyInfoEntry("🪙", "", "${state.gold}G", InfoTone.GOLD),
        LobbyInfoEntry("💎", "", "${state.gems}", InfoTone.VIOLET),
        LobbyInfoEntry("🎟", "", "${state.packTickets}", InfoTone.HEAL)
    )

    /** 상단 요약 한 줄을 텍스트로 (접근성 설명·테스트용) */
    fun topSummaryLine(state: TcgGameState): String =
        topSummary(state).joinToString(" · ") { it.line }

    /**
     * 정보 패널의 묶음들 — 진행도 · 정신 상태 · 설정(오디오·자동 저장).
     * 「🎬 인트로 다시보기」·「🆕 새 게임」은 값이 아니라 동작이라 화면 버튼으로 둔다.
     */
    fun sections(state: TcgGameState): List<LobbyInfoSection> = listOf(
        LobbyInfoSection(
            title = "진행도",
            entries = listOf(
                LobbyInfoEntry("📖", "스토리", state.storyProgressLabel, InfoTone.VIOLET),
                LobbyInfoEntry(
                    "🗺", "스테이지",
                    "${state.stageCleared.size}/${com.game.tcg.core.model.StageCatalog.TOTAL_STAGES} · " +
                        state.stageProgressLabel,
                    InfoTone.VIOLET
                ),
                LobbyInfoEntry("⚔", "덱 전투력", "${state.deckTotalPower}", InfoTone.HEAL),
                LobbyInfoEntry("🎴", "보유 인원", "${state.ownedCards.size}명", InfoTone.GOLD),
                LobbyInfoEntry("🧩", "조각", "${state.shardTotal}", InfoTone.ABYSS)
            )
        ),
        LobbyInfoSection(
            title = "정신 상태",
            entries = listOf(
                LobbyInfoEntry(
                    if (state.combatPenalty.totalStacks > 0) "🌫" else "✨",
                    if (state.combatPenalty.totalStacks > 0) "[홀림]" else "정신 맑음",
                    if (state.combatPenalty.totalStacks > 0) {
                        "${state.combatPenalty.totalStacks}스택 · ${state.combatPenalty.debuffDescription}"
                    } else {
                        "유혹에 흔들리지 않아 전투 페널티가 없습니다."
                    },
                    if (state.combatPenalty.totalStacks > 0) InfoTone.DANGER else InfoTone.HEAL
                )
            )
        ),
        LobbyInfoSection(
            title = "설정",
            entries = listOf(
                LobbyInfoEntry(
                    if (state.audioSettings.bgmEnabled) "🔊" else "🔇", "BGM",
                    if (state.audioSettings.bgmEnabled) "켜짐" else "꺼짐",
                    if (state.audioSettings.bgmEnabled) InfoTone.VIOLET else InfoTone.NEUTRAL
                ),
                LobbyInfoEntry(
                    if (state.audioSettings.sfxEnabled) "🔊" else "🔇", "SFX",
                    if (state.audioSettings.sfxEnabled) "켜짐" else "꺼짐",
                    if (state.audioSettings.sfxEnabled) InfoTone.VIOLET else InfoTone.NEUTRAL
                ),
                LobbyInfoEntry("💾", AUTOSAVE_LABEL, AUTOSAVE_VALUE, InfoTone.NEUTRAL)
            )
        )
    )

    /** 정보 패널 안에 함께 보여 줄 경보(누르면 해당 화면으로 이동) */
    fun alerts(state: TcgGameState): List<LobbyAlert> = state.lobbyAlerts

    /** 정보 버튼에 붙는 경보 개수 (0이면 배지 없음) */
    fun alertCount(state: TcgGameState): Int = state.lobbyAlerts.size

    /** 긴급 경보가 있는지 — 정보 버튼을 위험 색으로 칠한다 */
    fun hasUrgentAlert(state: TcgGameState): Boolean = state.lobbyAlerts.any { it.isUrgent }
}
