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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.model.DeckRules

/**
 * 우측 서브메뉴 「서약·덱」의 서약 탭 — 출전 덱 현황(9~12장 · 1인 1장) + 마왕 응시·성수 정화 + 대표 캐릭터 설정
 */
@Composable
fun DeckScreen(
    state: TcgGameState,
    onGaze: () -> Unit,
    onPurify: () -> Unit,
    onSetLobbyHero: (Int) -> Unit,
    onOpenCollection: () -> Unit
) {
    val deckSize = state.unlockedDeckSize

    ScreenFrame {
        SectionCard(
            title = "출전 덱 현황 · 1인 1장",
            badge = "${state.party.size}/${deckSize}장 · 최대 ${DeckRules.MAX_DECK_SIZE}장",
            accent = TcgColors.Gold
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("전열", TcgColors.Hp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "${state.party.count { it.isFrontRow }}/3",
                    color = TcgColors.Text,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(10.dp))
                Pill("후열·대기", TcgColors.Mana)
                Spacer(Modifier.width(6.dp))
                Text(
                    "${state.party.count { !it.isFrontRow }}/${deckSize - 3}",
                    color = TcgColors.Text,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(10.dp))
                Pill("Lv.${state.playerLevel}", TcgColors.Violet)
            }
            StatBar(ratio = state.party.size / deckSize.toFloat(), color = TcgColors.Violet, height = 8)
            Text(
                "기본 9장 편성 · Lv.5에서 10장 · Lv.10에서 11장 · Lv.15에서 12장까지 열립니다. " +
                        "같은 캐릭터는 단 1장만 편성할 수 있습니다(1캐릭터 1장 원칙).",
                color = TcgColors.Muted,
                fontSize = 9.sp,
                lineHeight = 14.sp
            )

            state.equippedCards.forEachIndexed { index, card ->
                val member = state.party.sortedBy { it.slotIndex }.getOrNull(index)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Pill(DeckRules.slotLabel(member?.slotIndex ?: index), elementColor(card.element))
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
            if (state.party.isEmpty()) {
                Text(
                    "아직 편성된 카드가 없습니다 — 「덱 편성」 탭에서 자동 편성 또는 카드 탭으로 채우세요.",
                    color = TcgColors.Muted,
                    fontSize = 9.sp
                )
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
