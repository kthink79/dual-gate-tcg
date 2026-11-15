package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.DeckEngine
import com.game.tcg.core.model.DeckValidationResult

/**
 * 우측 서브메뉴 「서약·덱」 — 20+10 · 1인 1장 덱 확인 + 마왕 응시·성수 정화 + 대표 캐릭터 설정
 */
@Composable
fun DeckScreen(
    state: TcgGameState,
    onGaze: () -> Unit,
    onPurify: () -> Unit,
    onSetLobbyHero: (Int) -> Unit,
    onOpenCollection: () -> Unit
) {
    val validation: DeckValidationResult = remember(state.ownedCards.size, state.ownedCards.map { it.id }) {
        DeckEngine().validateDeck(state.ownedCards)
    }
    val characterCount = validation.characterCount
    val equipmentCount = validation.equipmentSpellCount

    ScreenFrame {
        SectionCard(
            title = "서약 덱 · 20+10 · 1인 1장",
            badge = "캐릭터 ${characterCount}/20 · 장비 ${equipmentCount}/10",
            accent = TcgColors.Gold
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("캐릭터 카드", TcgColors.Violet)
                Spacer(Modifier.width(6.dp))
                Text("$characterCount/20", color = TcgColors.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Pill("장비·스펠", TcgColors.Mana)
                Spacer(Modifier.width(6.dp))
                Text("$equipmentCount/10", color = TcgColors.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            StatBar(ratio = characterCount / 20f, color = TcgColors.Violet, height = 8)
            StatBar(ratio = equipmentCount / 10f, color = TcgColors.Mana, height = 6)
            Text(
                "1캐릭터 = 1장 유일 원칙. 동일 캐릭터를 두 장 편성할 수 없으며, " +
                        "장비·스펠은 같은 카드 최대 3장까지 허용됩니다.",
                color = TcgColors.Muted,
                fontSize = 9.sp,
                lineHeight = 14.sp
            )

            state.ownedCards.forEach { card ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Pill("#${card.id}", elementColor(card.element))
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(card.name, color = TcgColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${card.element.displayName} · ${card.starLevel}★ · ATK ${card.atk} / DEF ${card.def} · 호감도 ${card.bondLevel}",
                            color = TcgColors.Muted,
                            fontSize = 9.sp
                        )
                    }
                    Pill("1인 1장 ✓", TcgColors.Heal)
                }
            }

            if (validation.errors.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(TcgColors.SurfaceAlt)
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        validation.errors.take(3).forEach { error ->
                            Text("· $error", color = TcgColors.Muted, fontSize = 9.sp, lineHeight = 14.sp)
                        }
                        Text(
                            "· 덱 30장 완성은 캐릭터 연성과 장비 카드 해금으로 채워집니다.",
                            color = TcgColors.Muted,
                            fontSize = 9.sp
                        )
                    }
                }
            }
            ActionTile(
                label = "🃏 캐릭터 관리에서 도감·합성 열기",
                hint = "신규 연성 · 한계 돌파 · 환생 재획득",
                accent = TcgColors.Violet,
                onClick = onOpenCollection
            )
        }

        SectionCard(
            title = "마왕 응시 · 성수 정화",
            badge = "[홀림] ${state.combatPenalty.totalStacks}스택",
            accent = TcgColors.Danger
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(
                    "시선 ${state.temptationState.gazeTemptationStacks}/5 · 저주 ${state.temptationState.curseOfArchdemonStacks}/5",
                    TcgColors.Danger
                )
                Spacer(Modifier.width(6.dp))
                Text("누적 ${state.temptationState.totalTemptationStacks}/10", color = TcgColors.Danger, fontSize = 10.sp)
            }
            StatBar(ratio = state.temptationState.visualMistIntensity, color = TcgColors.Abyss, height = 8)
            Text(state.combatPenalty.debuffDescription, color = TcgColors.Danger, fontSize = 10.sp, lineHeight = 15.sp)
            ActionTile(
                label = "🔍 마왕 카드 자세히 응시하기",
                hint = "[홀림] 스택 +1 · 마왕전 디버프 누적",
                accent = TcgColors.Danger,
                onClick = onGaze
            )
            ActionTile(
                label = "💧 성수로 정화 (보유 ${state.holyWater})",
                hint = "매혹·저주 스택 -2",
                accent = TcgColors.Heal,
                enabled = state.holyWater > 0,
                onClick = onPurify
            )
            if (state.gazeMessage != null) {
                Text(state.gazeMessage, color = TcgColors.Text.copy(alpha = 0.9f), fontSize = 10.sp, lineHeight = 15.sp)
            }
        }

        SectionCard(title = "대표 캐릭터 설정", badge = "메인 로비", accent = TcgColors.Violet) {
            Text(
                "메인 화면 중앙에 크게 세울 캐릭터를 고릅니다. 좌우 레일의 어느 화면에서든 " +
                        "「← 메인 로비」로 돌아오면 이 캐릭터가 맞이합니다.",
                color = TcgColors.Muted,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
            state.lobbyRoster.forEach { id ->
                val card = TcgGameViewModel.characterCard(id)
                val profile = TcgGameViewModel.profileOf(id)
                ActionTile(
                    label = "${if (id == state.lobbyHeroId) "★ " else "☆ "}${profile?.displayName ?: card.name}",
                    hint = "${profile?.title ?: card.skillText} · 호감도 " +
                            (if (id == 3) state.lobbyHeroBond else state.ownedCards.firstOrNull { it.id == id }?.bondLevel ?: 0) +
                            "/100",
                    accent = if (id == state.lobbyHeroId) TcgColors.Gold else elementColor(card.element),
                    onClick = { onSetLobbyHero(id) }
                )
            }
        }
    }
}
