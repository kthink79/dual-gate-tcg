package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.AffinityRules
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.VisualState

/**
 * 메인 로비 — 캐릭터 중심 화면.
 *
 * 2026-11-16 사용자 요청(실기기 스크린샷) 반영:
 *  - 상단은 **1줄**만 — 차원 에너지 · 재화 3종 · 음소거 아이콘 · 「📊 정보」 버튼.
 *    (스테이지·덱 전투력·보유 인원·정신 상태·자동 저장·인트로 다시보기·새 게임은
 *     [LobbyInfoOverlay] 패널로 옮겨 캐릭터 얼굴/상반신을 가리지 않게 했다.)
 *  - 하단 캐릭터 정보도 이름·칭호·호감도·4종 비주얼(전투·일상·데이트·미니어처)·캐릭터 전환만 남겨 컴팩트하게 정돈.
 *  - 좌·우 측면 레일의 서브메뉴 6종은 그대로.
 *
 * 2026-11-20 사용자 요청(태블릿 가로 실기 스크린샷) 반영:
 *  - 대표 캐릭터 원화(2:3)가 가로 화면에서 Crop 으로 채워지며 **머리가 잘리던 문제**를
 *    [AmbientCharacterBackdrop] 으로 교체 — 좌우는 같은 원화의 블러 앰비언트,
 *    중앙은 2:3 그대로 잘림 없이.
 *  - 태블릿 대화면에서 좌우 레일·상단 재화 바·하단 캐릭터 패널이 구석에 작게 몰리지 않도록
 *    [ResponsiveLayout.uiScale] 배율을 글자·여백·버튼 크기에 곱한다(휴대폰은 1.0 그대로).
 */
@Composable
fun MainLobbyScreen(
    state: TcgGameState,
    audioManager: com.game.tcg.audio.AudioManager? = null,
    audioSettings: com.game.tcg.audio.AudioSettings? = null,
    onOpenScreen: (LobbyDestination) -> Unit,
    onSelectHero: (Int) -> Unit,
    onCycleHero: (Int) -> Unit,
    onSelectVisual: (VisualState) -> Unit,
    onReplayIntro: () -> Unit = {},
    /** 「새 게임」 — 확인 다이얼로그에서 승인했을 때만 불린다 */
    onResetGame: () -> Unit = {},
    /** 「📊 정보」 패널 열기 / 닫기 */
    onOpenInfo: () -> Unit = {},
    onCloseInfo: () -> Unit = {},
    /** 상단 음소거 아이콘 — BGM·SFX 를 함께 끄고 켠다 */
    onToggleBgm: () -> Unit = {},
    onToggleSfx: () -> Unit = {}
) {
    val hero = state.lobbyHero
    val profile = state.lobbyHeroProfile
    val roster = state.lobbyRoster
    val rosterIndex = roster.indexOf(state.lobbyHeroId).coerceAtLeast(0)
    val settings = audioSettings ?: state.audioSettings
    val muted = !settings.bgmEnabled && !settings.sfxEnabled

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(TcgColors.Background)
    ) {
        // 태블릿·대화면 배율 — 휴대폰(짧은 변 ≤420dp)은 1.0 이라 기존 화면과 동일
        val uiScale = ResponsiveLayout.uiScale(maxWidth.value, maxHeight.value)
        // 2026-11-28 v0.6.1 — 높이가 낮은 가로 모드(폰 가로 등)에서는 하단 패널·좌우 레일이 겹친다
        val short = ResponsiveLayout.isShortViewport(maxHeight.value)

        // ── 중앙 대형 캐릭터 아트 (화면의 주인공) — 가로 화면에서도 머리 잘림 없이 ──
        AmbientCharacterBackdrop(
            cardId = hero.id,
            visual = state.lobbyVisual,
            contentDescription = "${hero.name} 대표 캐릭터",
            modifier = Modifier.fillMaxSize(),
            targetWidthPx = 1080
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xB30B0A12),
                            Color(0x0A0B0A12),
                            Color(0x1A0B0A12),
                            Color(0xF20B0A12)
                        )
                    )
                )
        )

        // 좌우 레일·상단 재화 바·하단 캐릭터 패널 — 태블릿 배율을 한 번에 적용한다
        ScaledHud(uiScale) {
        // ── ① 최상단 1줄: 차원·재화 요약 + 음소거 + 📊 정보 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 8.dp, vertical = if (short) 4.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 좁은 화면에서도 🔊·📊 가 밀려나지 않도록 요약 칩 줄만 가로 스크롤에 담는다
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 좁은 화면에서도 재화 4종 + 정보 버튼이 한 줄에 들어가도록 제목은 아이콘만 남긴다
                LobbySummaryChip("🎴", TcgColors.Violet)
                LobbyInfoPanel.topSummary(state).forEach { entry ->
                    LobbySummaryChip(entry.line, entry.tone.color())
                }
            }
            // 작은 음소거 아이콘 — 누르면 BGM·SFX 를 함께 끄고 켠다
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x99120E1C))
                    .border(1.dp, TcgColors.Line, RoundedCornerShape(50))
                    .clickable {
                        if (muted) {
                            if (!settings.bgmEnabled) onToggleBgm()
                            if (!settings.sfxEnabled) onToggleSfx()
                        } else {
                            if (settings.bgmEnabled) onToggleBgm()
                            if (settings.sfxEnabled) onToggleSfx()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(if (muted) "🔇" else "🔊", fontSize = TcgType.LABEL.sp)
            }
            // 📊 정보 — 상세 정보·설정·경보·인트로 다시보기·새 게임이 모두 여기로
            val alertCount = LobbyInfoPanel.alertCount(state)
            val urgent = LobbyInfoPanel.hasUrgentAlert(state)
            val infoColor = if (urgent) TcgColors.Danger else TcgColors.Violet
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(infoColor.copy(alpha = 0.22f))
                    .border(1.dp, infoColor.copy(alpha = 0.8f), RoundedCornerShape(50))
                    .clickable { onOpenInfo() }
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        LobbyInfoPanel.BUTTON_LABEL,
                        color = if (urgent) TcgColors.Danger else TcgColors.Text,
                        fontSize = TcgType.LABEL.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (alertCount > 0) {
                        Spacer(Modifier.width(3.dp))
                        Text("$alertCount", color = infoColor, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // ── 좌측 레일 · 이세계 모험 ──
        // 높이가 낮은 가로 모드에서는 가운데 정렬하면 하단 캐릭터 패널과 겹친다 — 상단에 붙인다
        Column(
            modifier = Modifier
                .align(if (short) Alignment.TopStart else Alignment.CenterStart)
                .padding(start = 6.dp, top = if (short) 34.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(if (short) 6.dp else 8.dp)
        ) {
            Text(
                "이세계",
                color = TcgColors.Violet.copy(alpha = 0.9f),
                fontSize = TcgType.LABEL.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(62.dp),
                textAlign = TextAlign.Center
            )
            LobbyDestination.LEFT_ITEMS.forEach { item ->
                LobbyRailButton(item = item, onClick = { onOpenScreen(item) })
            }
        }

        // ── 우측 레일 · 현실 관리 ──
        Column(
            modifier = Modifier
                .align(if (short) Alignment.TopEnd else Alignment.CenterEnd)
                .padding(end = 6.dp, top = if (short) 34.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(if (short) 6.dp else 8.dp)
        ) {
            Text(
                "현실",
                color = TcgColors.Gold.copy(alpha = 0.9f),
                fontSize = TcgType.LABEL.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(62.dp),
                textAlign = TextAlign.Center
            )
            LobbyDestination.RIGHT_ITEMS.forEach { item ->
                LobbyRailButton(item = item, onClick = { onOpenScreen(item) })
            }
        }

        // ── 하단 캐릭터 패널 (컴팩트 — 캐릭터 비중 최대화) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = if (short) 4.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (short) 2.dp else 4.dp)
        ) {
            // 이름 + 칭호 (한 덩어리로)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Pill(hero.element.displayName, elementColor(hero.element))
                if (profile?.isDemonLord == true) Pill("마왕", TcgColors.Danger, filled = true)
                Text(
                    profile?.displayName ?: hero.name,
                    color = TcgColors.Text,
                    fontSize = if (short) 18.sp else 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!short) {
                Text(
                    profile?.title ?: hero.skillText,
                    color = TcgColors.Violet.copy(alpha = 0.95f),
                    fontSize = TcgType.LABEL.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            // 호감도 · 전투력 · 상태 (한 줄)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Pill("❤ ${state.lobbyHeroBond}/100", TcgColors.Gold)
                Pill("${hero.starLevel}★ ATK ${hero.atk}", TcgColors.Muted)
                if (state.combatPenalty.totalStacks > 0) {
                    Pill("🌫 ${state.combatPenalty.totalStacks}", TcgColors.Danger)
                }
                if (hero.isMemoryReset) {
                    Pill("🧩 ${hero.memoryFragments}/${com.game.tcg.core.model.TcgCard.MEMORY_FRAGMENT_TARGET}", TcgColors.Abyss)
                }
            }
            Box(modifier = Modifier.width(170.dp)) {
                StatBar(ratio = state.lobbyHeroBond / 100f, color = TcgColors.Gold, height = 5)
            }
            // 4종 비주얼 전환 (전투 · 일상 · 데이트 · 미니어처)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChipButton("① 전투", state.lobbyVisual == VisualState.BATTLE) {
                    onSelectVisual(VisualState.BATTLE)
                }
                ChipButton(
                    if (state.isLobbyCasualLookUnlocked) "② 일상" else "② 일상 🔒",
                    state.lobbyVisual == VisualState.CASUAL_CUTE,
                    if (state.isLobbyCasualLookUnlocked) TcgColors.Heal else TcgColors.Muted
                ) {
                    onSelectVisual(VisualState.CASUAL_CUTE)
                }
                ChipButton(
                    if (state.isLobbyDateLookUnlocked) "③ 데이트" else "③ 데이트 🔒",
                    state.lobbyVisual == VisualState.ROMANTIC_DATE,
                    TcgColors.Gold
                ) {
                    onSelectVisual(VisualState.ROMANTIC_DATE)
                }
                ChipButton(
                    "④ 미니어처",
                    state.lobbyVisual == VisualState.MINIATURE,
                    TcgColors.Abyss
                ) {
                    onSelectVisual(VisualState.MINIATURE)
                }
            }

            // 캐릭터 좌우 전환
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                HeroCycleButton("◀") { onCycleHero(-1) }
                Spacer(Modifier.width(8.dp))
                // 170명 이름 칩은 화면에 보이는 만큼만 구성한다(작은 기기 프레임 드롭 방지)
                LazyRow(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(roster, key = { it }) { id ->
                        val chipProfile = TcgGameViewModel.profileOf(id)
                        ChipButton(
                            label = chipProfile?.displayName ?: "#$id",
                            selected = id == state.lobbyHeroId,
                            accent = elementColor(chipProfile?.element ?: Element.LIGHT)
                        ) {
                            onSelectHero(id)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                HeroCycleButton("▶") { onCycleHero(1) }
            }
            if (!short) {
                Text(
                    "대표 캐릭터 ${rosterIndex + 1}/${roster.size} · 일상 ${TcgGameViewModel.CASUAL_LOOK_UNLOCK_BOND} / " +
                        "데이트 ${TcgGameViewModel.DATE_LOOK_UNLOCK_BOND} 해금 · 전투 출전 1회 +${AffinityRules.BATTLE_WIN_GAIN}",
                    color = TcgColors.Muted.copy(alpha = 0.8f),
                    fontSize = TcgType.LABEL.sp
                )
            }
        }

        }

        // ── 📊 정보 패널 (모달 시트) ──
        if (state.isInfoPanelOpen) {
            LobbyInfoOverlay(
                state = state,
                onClose = onCloseInfo,
                onOpenDestination = onOpenScreen,
                onReplayIntro = onReplayIntro,
                onToggleBgm = onToggleBgm,
                onToggleSfx = onToggleSfx,
                onResetGame = onResetGame
            )
        }
    }
}

/**
 * 태블릿·대화면에서 HUD(사이드 레일·재화 바·하단 패널)를 화면 크기에 맞춰 키우는 래퍼.
 *
 * [scale] 이 1.0 이면(휴대폰) 아무 것도 바꾸지 않고, 1.0 보다 크면 이 서브트리의
 * 밀도를 함께 올려 dp·sp 가 모두 같은 비율로 커지게 한다 — 버튼·글자·터치 영역이
 * 구석에 작게 몰리지 않고 화면 비율에 맞게 자리잡는다.
 */
@Composable
private fun ScaledHud(scale: Float, content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    if (scale <= 1.02f) {
        Box(modifier = Modifier.fillMaxSize(), content = content)
    } else {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density = density.density * scale, fontScale = density.fontScale)
        ) {
            Box(modifier = Modifier.fillMaxSize(), content = content)
        }
    }
}

/** 로비 최상단 1줄 요약 칩 — 아주 작고 반투명하게 */
@Composable
private fun LobbySummaryChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x99120E1C))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(label, color = color, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** 좌·우측 레일에 도킹되는 아이콘+라벨 서브메뉴 버튼 */
@Composable
private fun LobbyRailButton(item: LobbyDestination, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(62.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xD917131F))
            .border(1.dp, TcgColors.Line, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(item.icon, fontSize = 19.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            item.label,
            color = TcgColors.Text,
            fontSize = TcgType.LABEL.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

/** 대표 캐릭터 좌우 전환 화살표 */
@Composable
private fun HeroCycleButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(30.dp)
            .height(30.dp)
            .clip(CircleShape)
            .background(Color(0xCC17131F))
            .border(1.dp, TcgColors.Violet.copy(alpha = 0.6f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = TcgColors.Violet, fontSize = TcgType.LABEL.sp, fontWeight = FontWeight.Bold)
    }
}
