package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.Element
import com.game.tcg.core.model.VisualState

/**
 * 메인 로비 — 캐릭터 중심 화면
 * 중앙에 대표 캐릭터를 크게 세우고, 좌측(이세계·모험) / 우측(현실·관리) 측면 레일에
 * 서브메뉴 6종을 도킹한다. 상단은 랭크·차원 에너지·재화·경보 스트립, 하단은
 * 캐릭터 이름·칭호·호감도·3단 비주얼·캐릭터 전환.
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
    onReplayIntro: () -> Unit = {}
) {
    val hero = state.lobbyHero
    val profile = state.lobbyHeroProfile
    val roster = state.lobbyRoster
    val rosterIndex = roster.indexOf(state.lobbyHeroId).coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TcgColors.Background)
    ) {
        // ── 중앙 대형 캐릭터 아트 ──
        CardArtImage(
            cardId = hero.id,
            visual = state.lobbyVisual,
            contentDescription = "${hero.name} 대표 캐릭터",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            targetWidthPx = 1080
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xD90B0A12),
                            Color(0x140B0A12),
                            Color(0x330B0A12),
                            Color(0xF20B0A12)
                        )
                    )
                )
        )

        // ── 상단 상태 스트립 + 경보 ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("🎴 듀얼 게이트", TcgColors.Violet)
                Spacer(Modifier.width(5.dp))
                Pill(state.storyProgressLabel, TcgColors.Muted)
                Spacer(Modifier.weight(1f))
                // 🔊/🔇 BGM · SFX 토글 (로비 상단 우측)
                if (audioManager != null && audioSettings != null) {
                    AudioToggleRowInline(audioManager, audioSettings)
                    Spacer(Modifier.width(6.dp))
                }
                Pill(
                    if (state.combatPenalty.totalStacks > 0) "[홀림] ${state.combatPenalty.totalStacks}" else "정신 맑음",
                    if (state.combatPenalty.totalStacks > 0) TcgColors.Danger else TcgColors.Heal
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Pill("⚡ 차원 ${state.gateEnergy}/${state.gateEnergyMax}", TcgColors.Mana)
                Pill("🪙 ${state.gold}G", TcgColors.Gold)
                Pill("💎 ${state.gems}", TcgColors.Violet)
                Pill("🎟 ${state.packTickets}", TcgColors.Heal)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Pill("🗺 ${state.stageCleared.size}/30 스테이지", TcgColors.Violet)
                Pill("⚔ 덱 ${state.deckTotalPower}", TcgColors.Heal)
                Pill("🎴 보유 ${state.ownedCards.size}명", TcgColors.Gold)
                if (state.hasSummonBuff) {
                    Pill("소환 +${state.summonBuffPercent}% (${state.summonBuffBattles})", TcgColors.Mana)
                }
            }
            if (state.lobbyAlerts.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    state.lobbyAlerts.take(3).forEach { alert ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onOpenScreen(alert.destination) }
                        ) {
                            Pill(
                                "⚠ ${alert.label} ›",
                                if (alert.isUrgent) TcgColors.Danger else TcgColors.Gold
                            )
                        }
                    }
                }
            }
            // 「🎬 인트로 다시보기」 — 도입부 4장면 컷신을 언제든 다시 감상
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onReplayIntro() }
                ) {
                    Pill("🎬 인트로 다시보기", TcgColors.Violet)
                }
            }
        }

        // ── 좌측 레일 · 이세계 모험 ──
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "이세계",
                color = TcgColors.Violet.copy(alpha = 0.9f),
                fontSize = 8.sp,
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
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "현실",
                color = TcgColors.Gold.copy(alpha = 0.9f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(62.dp),
                textAlign = TextAlign.Center
            )
            LobbyDestination.RIGHT_ITEMS.forEach { item ->
                LobbyRailButton(item = item, onClick = { onOpenScreen(item) })
            }
        }

        // ── 하단 캐릭터 패널 ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Pill("★ 대표 캐릭터", TcgColors.Violet, filled = true)
                Pill(hero.element.displayName, elementColor(hero.element))
                if (profile?.isDemonLord == true) Pill("마왕", TcgColors.Danger, filled = true)
                if (hero.isMemoryReset) {
                    Pill("기억 파편 ${hero.memoryFragments}/${TcgCard.MEMORY_FRAGMENT_TARGET}", TcgColors.Abyss)
                }
            }
            Text(
                profile?.displayName ?: hero.name,
                color = TcgColors.Text,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                profile?.title ?: hero.skillText,
                color = TcgColors.Violet,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Pill("호감도 ${state.lobbyHeroBond}/100", TcgColors.Gold)
                Pill("${hero.starLevel}★ · ATK ${hero.atk}", TcgColors.Muted)
                if (state.combatPenalty.totalStacks > 0) {
                    Pill("[홀림] ${state.combatPenalty.totalStacks}스택", TcgColors.Danger)
                }
            }
            Box(modifier = Modifier.width(190.dp)) {
                StatBar(ratio = state.lobbyHeroBond / 100f, color = TcgColors.Gold, height = 6)
            }

            // 3단 비주얼 전환
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ChipButton("① 전투", state.lobbyVisual == VisualState.BATTLE) {
                    onSelectVisual(VisualState.BATTLE)
                }
                ChipButton("② 일상", state.lobbyVisual == VisualState.CASUAL_CUTE, TcgColors.Heal) {
                    onSelectVisual(VisualState.CASUAL_CUTE)
                }
                ChipButton(
                    if (state.isLobbyDateLookUnlocked) "③ 데이트" else "③ 데이트 🔒",
                    state.lobbyVisual == VisualState.ROMANTIC_DATE,
                    TcgColors.Gold
                ) {
                    onSelectVisual(VisualState.ROMANTIC_DATE)
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
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    roster.forEach { id ->
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
            Text(
                "대표 캐릭터 ${rosterIndex + 1}/${roster.size} · 좌우 서브메뉴로 이동",
                color = TcgColors.Muted,
                fontSize = 8.sp
            )
        }
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
            fontSize = 9.sp,
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
        Text(label, color = TcgColors.Violet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * 로비 상단의 🔊/🔇 BGM·SFX 토글 (MainActivity 의 AudioToggleRow 와 같은 동작).
 * MainActivity 패키지 순환 참조를 피하기 위해 여기서 가볍게 다시 그린다.
 */
@Composable
private fun AudioToggleRowInline(
    audio: com.game.tcg.audio.AudioManager,
    settings: com.game.tcg.audio.AudioSettings
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ToggleChip(
            label = if (settings.bgmEnabled) "🔊 BGM" else "🔇 BGM",
            active = settings.bgmEnabled
        ) { audio.toggleBgm() }
        Spacer(Modifier.width(4.dp))
        ToggleChip(
            label = if (settings.sfxEnabled) "🔊 SFX" else "🔇 SFX",
            active = settings.sfxEnabled
        ) {
            val wasOff = !settings.sfxEnabled
            audio.toggleSfx()
            if (wasOff) audio.click()
        }
    }
}

@Composable
private fun ToggleChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) TcgColors.Violet.copy(alpha = 0.18f) else TcgColors.SurfaceAlt.copy(alpha = 0.9f))
            .border(1.dp, if (active) TcgColors.Violet.copy(alpha = 0.6f) else TcgColors.Line, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, color = if (active) TcgColors.Violet else TcgColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
