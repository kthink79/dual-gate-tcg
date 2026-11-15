package com.game.tcg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.model.CardCatalog
import com.game.tcg.core.model.DeckRules
import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.VisualState

/**
 * 탭 3 — 🃏 도감 · 서약 (3단 비주얼)
 */
@Composable
fun CollectionScreen(
    state: TcgGameState,
    onSelectCard: (Int) -> Unit,
    onSelectVisual: (VisualState) -> Unit,
    onGaze: () -> Unit,
    onPurify: () -> Unit,
    onLimitBreak: () -> Unit,
    onCraftNew: () -> Unit,
    onMemoryRecall: (Int) -> Unit,
    onSetLobbyHero: (Int) -> Unit,
    onDeployToParty: (Int, Int) -> Unit,
    onUndeployFromParty: (Int) -> Unit
) {
    val selected: TcgCard = state.ownedCards.firstOrNull { it.id == state.selectedCardId }
        ?: TcgGameViewModel.characterCard(state.selectedCardId)
    val isOwned = state.ownedCards.any { it.id == state.selectedCardId }
    val profile = TcgGameViewModel.profileOf(state.selectedCardId)
    val isDemonLordCard = state.selectedCardId == 112

    ScreenFrame {
        // ── 카드 선택 (170인 전체 스크롤 목록) ──
        val allProfiles = TcgGameViewModel.profiles()
        val ownedIds = state.ownedCardIds
        var catalogFilter by remember { mutableStateOf(CatalogFilter.ALL) }
        val shownProfiles = when (catalogFilter) {
            CatalogFilter.ALL -> allProfiles
            CatalogFilter.NEW_WAVE -> allProfiles.filter { CardCatalog.isNewWave(it.cardId) }
            CatalogFilter.ORIGINAL -> allProfiles.filter { !CardCatalog.isNewWave(it.cardId) }
            CatalogFilter.BOSS -> TcgGameViewModel.midBossProfiles()
        }
        SectionCard(
            title = "도감 카드",
            badge = "보유 ${state.ownedCards.size}/${allProfiles.size}",
            accent = TcgColors.Violet
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChipButton("전체 ${allProfiles.size}인", catalogFilter == CatalogFilter.ALL) {
                    catalogFilter = CatalogFilter.ALL
                }
                ChipButton("신규 15인", catalogFilter == CatalogFilter.NEW_WAVE, TcgColors.Gold) {
                    catalogFilter = CatalogFilter.NEW_WAVE
                }
                ChipButton("기존 155인", catalogFilter == CatalogFilter.ORIGINAL, TcgColors.Mana) {
                    catalogFilter = CatalogFilter.ORIGINAL
                }
                ChipButton("보스 6", catalogFilter == CatalogFilter.BOSS, TcgColors.Danger) {
                    catalogFilter = CatalogFilter.BOSS
                }
            }
            Text(
                if (catalogFilter == CatalogFilter.NEW_WAVE) {
                    "No.155~169 신규 15인 — 매력 유지 + 얼굴형·포즈 다양화로 다시 그린 세트입니다."
                } else if (catalogFilter == CatalogFilter.ORIGINAL) {
                    "No.1~154 · No.184 아메넷 — 기존 155인. 전원 3단 비주얼(전투·일상·데이트)이 들어 있습니다."
                } else if (catalogFilter == CatalogFilter.BOSS) {
                    "No.170~175 중간 보스 6인 — 챕터 마일스톤 관문. 격파 후 「심연 연성」으로 계약하면 아군이 됩니다."
                } else {
                    "도감 번호를 누르면 카드 상세와 3단 비주얼이 열립니다. (170명 × 3 = JPG 510장, 1캐릭터 = 1장 유일성)"
                },
                color = TcgColors.Muted,
                fontSize = 9.sp,
                lineHeight = 14.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(228.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TcgColors.Background)
                    .verticalScroll(rememberScrollState())
                    .padding(6.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    shownProfiles.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowItems.forEach { profileItem ->
                                Box(modifier = Modifier.weight(1f)) {
                                    ChipButton(
                                        label = "#${profileItem.cardId} ${profileItem.displayName}" +
                                            if (profileItem.cardId in ownedIds) "" else " 🔒",
                                        selected = profileItem.cardId == state.selectedCardId,
                                        accent = if (profileItem.cardId in ownedIds) {
                                            elementColor(profileItem.element)
                                        } else {
                                            TcgColors.Line
                                        },
                                        onClick = { onSelectCard(profileItem.cardId) }
                                    )
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Text(
                "목록 ${shownProfiles.size}명 전원 스크롤 가능 · 자물쇠 표시는 아직 계약하지 않은 카드입니다.",
                color = TcgColors.Muted,
                fontSize = 9.sp
            )
        }

        // ── 3단 비주얼 + 카드 상세 ──
        SectionCard(
            title = "${selected.name}",
            badge = if (isOwned) "${selected.starLevel}★" else "미계약",
            accent = elementColor(selected.element)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                CardArtImage(
                    cardId = selected.id,
                    visual = state.selectedVisual,
                    contentDescription = selected.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    targetWidthPx = 720
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xDD0B0A12))
                            )
                        )
                )
                // 미보유 카드는 잠금(실루엣) 오버레이로 표시 — 도감 열람만 가능
                if (!isOwned) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xD90B0A12)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "🔒 미보유 — 카드팩·연성으로 획득",
                            color = TcgColors.Text.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill(state.selectedVisual.displayName, TcgColors.Violet, filled = true)
                        Spacer(Modifier.width(6.dp))
                        Pill(selected.element.displayName, elementColor(selected.element))
                        if (isDemonLordCard) {
                            Spacer(Modifier.width(6.dp))
                            Pill("마왕", TcgColors.Danger, filled = true)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        profile?.title ?: selected.skillText,
                        color = TcgColors.Text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 3단 비주얼 전환
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChipButton("① 전투", state.selectedVisual == VisualState.BATTLE) {
                    onSelectVisual(VisualState.BATTLE)
                }
                ChipButton("② 일상", state.selectedVisual == VisualState.CASUAL_CUTE, TcgColors.Heal) {
                    onSelectVisual(VisualState.CASUAL_CUTE)
                }
                ChipButton("③ 데이트", state.selectedVisual == VisualState.ROMANTIC_DATE, TcgColors.Gold) {
                    onSelectVisual(VisualState.ROMANTIC_DATE)
                }
            }

            // 전투 편성 — 이 캐릭터를 전열/후열 슬롯에 직접 소환
            val currentSlot = state.party.firstOrNull { it.card.id == selected.id }
            Text(
                if (currentSlot != null) {
                    "전투 편성: ${DeckRules.slotLabel(currentSlot.slotIndex)} 슬롯 (HP ${currentSlot.hp}/${currentSlot.maxHp})"
                } else {
                    "전투 편성: 현재 미배치 — 슬롯을 눌러 소환하세요. (${state.party.size}/${state.unlockedDeckSize}장)"
                },
                color = if (currentSlot != null) TcgColors.Heal else TcgColors.Muted,
                fontSize = 10.sp
            )
            (0 until state.unlockedDeckSize).chunked(4).forEach { rowSlots ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowSlots.forEach { slot ->
                        Box(modifier = Modifier.weight(1f)) {
                            ChipButton(
                                label = DeckRules.slotLabel(slot),
                                selected = currentSlot?.slotIndex == slot,
                                accent = if (DeckRules.isFrontSlot(slot)) TcgColors.Hp else TcgColors.Mana
                            ) {
                                if (isOwned) onDeployToParty(selected.id, slot)
                            }
                        }
                    }
                    repeat(4 - rowSlots.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (currentSlot != null) {
                ActionTile(
                    label = "↩ 전투 편성에서 빼기 (대기열로)",
                    hint = "HP는 전투 시작 시 최대치로 재설정됩니다",
                    accent = TcgColors.Muted,
                    onClick = { onUndeployFromParty(selected.id) }
                )
            }

            ActionTile(
                label = if (state.lobbyHeroId == selected.id) "★ 메인 로비 대표 캐릭터" else "★ 메인 로비 대표 캐릭터로 설정",
                hint = if (state.lobbyHeroId == selected.id) {
                    "지금 메인 화면 중앙에 서 있는 캐릭터입니다"
                } else {
                    "메인 로비 중앙에 크게 소환합니다"
                },
                accent = TcgColors.Gold,
                enabled = isOwned && state.lobbyHeroId != selected.id,
                onClick = { onSetLobbyHero(selected.id) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatColumn("ATK", "${selected.atk}", TcgColors.Hp)
                StatColumn("DEF", "${selected.def}", TcgColors.Mana)
                StatColumn("성급", "${selected.starLevel}★", TcgColors.Gold)
                StatColumn("호감도", "${selected.bondLevel}", TcgColors.Heal)
            }
            Text("스킬 · ${selected.skillText}", color = TcgColors.Text.copy(alpha = 0.85f), fontSize = 11.sp, lineHeight = 16.sp)
            if (!isOwned) {
                Text(
                    "아직 계약하지 않은 카드입니다. 도감 열람과 3단 비주얼 감상은 가능하며, " +
                            "계약은 2웨이 합성의 '신규 연성'에서 할 수 있습니다.",
                    color = TcgColors.Muted,
                    fontSize = 9.sp,
                    lineHeight = 14.sp
                )
            }
            if (isDemonLordCard && state.isDemonLordBattle && state.enemyHp == 0) {
                Pill(
                    if (profile?.disguiseName != null) "서약 파트너 합류 (위장 신분 '${profile.disguiseName}' → 마왕 릴리스)" else "서약 파트너",
                    TcgColors.Heal
                )
            }
        }

        // ── 마왕 응시 기믹 ──
        if (isDemonLordCard) {
            SectionCard(title = "마왕 카드 · 자세히 응시하기", badge = "유혹 기믹", accent = TcgColors.Danger) {
                Text(
                    "마왕의 사진을 확대해 10초 이상 응시하면 [홀림] 스택이 쌓입니다. " +
                            "이 스택은 마왕전에서 아군 공격력 저하·피격 피해 증가로 은밀히 적용됩니다.",
                    color = TcgColors.Muted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(
                        "[홀림] 시선 ${state.temptationState.gazeTemptationStacks}/5 · 저주 ${state.temptationState.curseOfArchdemonStacks}/5",
                        TcgColors.Danger
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("누적 ${state.temptationState.totalTemptationStacks}/10", color = TcgColors.Danger, fontSize = 10.sp)
                }
                StatBar(
                    ratio = state.temptationState.visualMistIntensity,
                    color = TcgColors.Abyss,
                    height = 8
                )
                Text(
                    "심장 박동 ${state.temptationState.heartbeatBpm} BPM · 응시 누적 ${state.temptationState.gazeDurationSeconds}초 · 줌 ${state.temptationState.gazeZoomCount}회",
                    color = TcgColors.Muted,
                    fontSize = 9.sp
                )
                ActionTile(
                    label = "🔍 자세히 응시하기 (핀치 줌 체험)",
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(TcgColors.Danger.copy(alpha = 0.12f))
                            .padding(10.dp)
                    ) {
                        Text(state.gazeMessage, color = TcgColors.Text.copy(alpha = 0.9f), fontSize = 10.sp, lineHeight = 15.sp)
                    }
                }
                Text(state.combatPenalty.debuffDescription, color = TcgColors.Danger, fontSize = 10.sp)
            }
        }

        // ── 「기억의 잔향」: 기억이 지워진 동료 ──
        if (selected.isMemoryReset) {
            val progress = state.memoryResetCards.firstOrNull { it.id == selected.id }
            val fragments = progress?.memoryFragments ?: selected.memoryFragments
            val target = TcgCard.MEMORY_FRAGMENT_TARGET
            SectionCard(
                title = "기억이 지워진 동료",
                badge = "기억 파편 $fragments/$target",
                accent = TcgColors.Abyss
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill("기억이 지워진 동료", TcgColors.Abyss, filled = true)
                    Spacer(Modifier.width(6.dp))
                    Pill("재획득 ${selected.reincarnationCount}회차", TcgColors.Muted)
                }
                Text(
                    "환생 풀에서 돌아온 '${selected.name}'은(는) 이름도 온기도 기억하지 못합니다. " +
                            "현실 교감과 이계 전투 승리로 기억 파편을 모으면 「기억의 조각 회상」 이벤트로 " +
                            "스탯 보너스와 기억을 되찾을 수 있습니다.",
                    color = TcgColors.Muted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                StatBar(ratio = fragments.toFloat() / target, color = TcgColors.Abyss, height = 10)
                Text(
                    "기억 파편 게이지 $fragments/$target" +
                            (if (fragments >= target) " — 회상 이벤트 개방!" else " — ${target - fragments}개 더 필요"),
                    color = if (fragments >= target) TcgColors.Gold else TcgColors.Muted,
                    fontSize = 10.sp
                )
                Text(
                    "파편 수집 경로: 현실 교감 탭의 「기억의 잔향」 교감(파편 +1) · 이계 전투 승리(파편 +1)",
                    color = TcgColors.Muted,
                    fontSize = 9.sp
                )
                ActionTile(
                    label = "🌙 기억의 조각 회상 이벤트 열기",
                    hint = if (fragments >= target) "스탯 보너스 + 기억 복원"
                    else "기억 파편 ${target - fragments}개 부족",
                    accent = TcgColors.Gold,
                    enabled = fragments >= target,
                    onClick = { onMemoryRecall(selected.id) }
                )
                if (state.memoryRecallMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(TcgColors.Abyss.copy(alpha = 0.14f))
                            .padding(10.dp)
                    ) {
                        Text(
                            state.memoryRecallMessage,
                            color = TcgColors.Text.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // ── 「기억의 조각 회상」 대화 연출 ──
        val recallDialogue = state.memoryRecallDialogue
        if (recallDialogue != null) {
            SectionCard(
                title = "「${recallDialogue.eventTitle}」",
                badge = recallDialogue.subtitle,
                accent = TcgColors.Gold
            ) {
                recallDialogue.lines.forEach { line ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(TcgColors.SurfaceAlt)
                            .padding(10.dp)
                    ) {
                        Text(
                            line,
                            color = TcgColors.Text.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
                Text(
                    recallDialogue.closingLine,
                    color = TcgColors.Gold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ── 2웨이 합성 요약 ──
        SectionCard(title = "2웨이 합성", badge = "연성 · 한계 돌파", accent = TcgColors.Gold) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TcgColors.SurfaceAlt)
                            .padding(10.dp)
                    ) {
                        Text("① 신규 연성", color = TcgColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "잡카드 3장 + 수수료 300G\n→ 미보유 캐릭터 1종과 계약",
                            color = TcgColors.Muted,
                            fontSize = 9.sp,
                            lineHeight = 14.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        ActionTile(
                            label = "연성 실행",
                            hint = "보유 잡카드 ${state.junkCards}장 · ${state.gold}G",
                            accent = TcgColors.Gold,
                            enabled = state.junkCards >= 3 && state.gold >= 300,
                            onClick = onCraftNew
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TcgColors.SurfaceAlt)
                            .padding(10.dp)
                    ) {
                        Text("② 한계 돌파", color = TcgColors.Violet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "동일 카드 흡수\n→ 성급 +1 (ATK/DEF +20%, 최대 5★)",
                            color = TcgColors.Muted,
                            fontSize = 9.sp,
                            lineHeight = 14.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        ActionTile(
                            label = "돌파 실행",
                            hint = "대상 ${selected.name.take(6)}",
                            accent = TcgColors.Violet,
                            enabled = isOwned && selected.starLevel < 5,
                            onClick = onLimitBreak
                        )
                    }
                }
            }
            if (state.synthesisMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(TcgColors.Gold.copy(alpha = 0.12f))
                        .padding(10.dp)
                ) {
                    Text(state.synthesisMessage, color = TcgColors.Text.copy(alpha = 0.9f), fontSize = 10.sp, lineHeight = 15.sp)
                }
            }
            Text(
                "한계 돌파는 도감·서약의 카드 상세에서 대상 카드를 먼저 선택하세요.",
                color = TcgColors.Muted,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(TcgColors.SurfaceAlt)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = TcgColors.Muted, fontSize = 8.sp)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** 도감 목록 필터 */
private enum class CatalogFilter { ALL, NEW_WAVE, ORIGINAL, BOSS }
