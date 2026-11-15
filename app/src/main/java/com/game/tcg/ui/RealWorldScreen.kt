package com.game.tcg.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.RealWorldActivity
import com.game.tcg.core.engine.SummonEngine
import com.game.tcg.core.model.TcgCard
import com.game.tcg.core.model.VisualState

/**
 * 탭 4 — 🏙️ 현실 교감
 */
@Composable
fun RealWorldScreen(
    state: TcgGameState,
    onInteract: (RealWorldActivity) -> Unit,
    onBondMemoryPartner: (Int) -> Unit,
    onSummon: (Int) -> Unit = {},
    onSelectSummonCard: (Int) -> Unit = {}
) {
    val bond = state.partnerBond
    val partner = state.ownedCards.firstOrNull { it.id == 3 }
    val summonTarget = state.summonCard

    ScreenFrame {
        // ── 현실 소환 — 차원 에너지로 보유 캐릭터를 부른다 (2026-11-16 사용자 요청 서브 플로우) ──
        SectionCard(
            title = "현실 소환 · ${summonTarget?.name ?: "대상 없음"}",
            badge = "⚡ ${state.gateEnergy}/${state.gateEnergyMax}",
            accent = TcgColors.Violet
        ) {
            Text(
                "차원 균열 에너지 ${SummonEngine.ENERGY_COST}를 써서 보유 캐릭터를 현실로 부릅니다. " +
                    "호감도 +${SummonEngine.BOND_GAIN} → 50에서 일상·데이트 룩, 100에서 페어 얼티밋 오의가 열리고, " +
                    "소환 직후 ${SummonEngine.BUFF_BATTLES}회 전투 동안 아군 공격력 +${SummonEngine.BUFF_PERCENT}% 버프가 걸립니다.",
                color = TcgColors.Muted,
                fontSize = 9.sp,
                lineHeight = 14.sp
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.ownedCards.sortedByDescending { it.bondLevel }.forEach { owned ->
                    ChipButton(
                        label = "${owned.name.take(6)} ${owned.bondLevel}",
                        selected = owned.id == summonTarget?.id,
                        accent = elementColor(owned.element)
                    ) { onSelectSummonCard(owned.id) }
                }
            }
            if (summonTarget != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Pill("호감도 ${summonTarget.bondLevel}/100", TcgColors.Gold)
                    Pill(summonTarget.element.displayName, elementColor(summonTarget.element))
                    if (summonTarget.bondLevel >= SummonEngine.DATE_UNLOCK_BOND) Pill("데이트 룩 해금", TcgColors.Heal)
                    if (summonTarget.bondLevel >= SummonEngine.ULTIMATE_UNLOCK_BOND) Pill("오의 개방", TcgColors.Violet)
                }
                StatBar(ratio = summonTarget.bondLevel / 100f, color = TcgColors.Gold, height = 8)
                ActionTile(
                    label = "🌙 ${summonTarget.name} 현실 소환 (⚡ ${SummonEngine.ENERGY_COST})",
                    hint = if (state.gateEnergy >= SummonEngine.ENERGY_COST) {
                        "호감도 +${SummonEngine.BOND_GAIN} · 아군 공격력 +${SummonEngine.BUFF_PERCENT}% (${SummonEngine.BUFF_BATTLES}회)"
                    } else {
                        "차원 에너지 부족 — 이계 전투 승리로 +2 회복"
                    },
                    accent = TcgColors.Violet,
                    enabled = state.gateEnergy >= SummonEngine.ENERGY_COST,
                    onClick = { onSummon(summonTarget.id) }
                )
            }
            if (state.hasSummonBuff) {
                Pill(
                    "소환 버프 유지 중 · 공격력 +${state.summonBuffPercent}% (${state.summonBuffBattles}회)",
                    TcgColors.Heal,
                    filled = true
                )
            }
            if (state.summonMessage != null) {
                Text(state.summonMessage, color = TcgColors.Text.copy(alpha = 0.88f), fontSize = 10.sp, lineHeight = 15.sp)
            }
            Text(
                "소환 ${state.summonCount}회 · 교감은 아래 「오늘의 교감」에서 이어집니다.",
                color = TcgColors.Muted,
                fontSize = 9.sp
            )
        }
        val partnerName = summonTarget?.name ?: "파트너"
        val partnerBondNow = summonTarget?.bondLevel ?: bond
        val partnerLook = summonTarget?.currentVisual ?: state.partnerVisual
        SectionCard(
            title = "현실 교감 · $partnerName",
            badge = if (partnerBondNow >= 100) "오의 개방" else "호감도 $partnerBondNow/100",
            accent = TcgColors.Gold
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                CardArtImage(
                    cardId = summonTarget?.id ?: TcgGameViewModel.DEFAULT_LOBBY_HERO_ID,
                    visual = partnerLook,
                    contentDescription = partnerName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    targetWidthPx = 720
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color(0x22000000), Color(0xDD0B0A12)))
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill(partnerLook.displayName, TcgColors.Gold, filled = true)
                        Spacer(Modifier.width(6.dp))
                        if (partnerBondNow >= SummonEngine.DATE_UNLOCK_BOND) Pill("데이트 룩 해금", TcgColors.Heal)
                    }
                    Text(
                        when {
                            partnerBondNow >= 100 -> "「오의가 열렸습니다. 제 모든 것을 당신의 검에 맡기죠.」"
                            partnerBondNow >= 50 -> "「이런 옷… 어울릴까요? 당신 앞에서만 입어 볼게요.」"
                            else -> "「오늘도 부르셨습니까, 계약자.」"
                        },
                        color = TcgColors.Text,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            StatBar(ratio = partnerBondNow / 100f, color = TcgColors.Gold, height = 10)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0", color = TcgColors.Muted, fontSize = 8.sp)
                Text("50 · 일상 사복 진화", color = TcgColors.Heal, fontSize = 8.sp)
                Text("100 · 페어 얼티밋 오의", color = TcgColors.Gold, fontSize = 8.sp)
            }
            Text(
                "현실 교감으로 호감도를 올리면 3단 비주얼이 진화하고, 100 달성 시 이세계 전투에서 페어 얼티밋 오의(+25%)가 발동합니다.",
                color = TcgColors.Muted,
                fontSize = 9.sp,
                lineHeight = 14.sp
            )
        }

        SectionCard(title = "오늘의 교감", badge = "호감도 상승", accent = TcgColors.Violet) {
            ActionTile(
                label = "☕ 카페 데이트",
                hint = "호감도 +30 · 데이트 룩 연출",
                accent = TcgColors.Gold,
                onClick = { onInteract(RealWorldActivity.ROMANTIC_EVENING_DATE) }
            )
            ActionTile(
                label = "🌆 저녁 산책",
                hint = "호감도 +15",
                accent = TcgColors.Heal,
                onClick = { onInteract(RealWorldActivity.CASUAL_CAFE_WALK) }
            )
            ActionTile(
                label = "🗣 전술 훈련 동행 · 짧은 대화",
                hint = "호감도 +5",
                accent = TcgColors.Mana,
                onClick = { onInteract(RealWorldActivity.DAILY_GREETING) }
            )
            if (partner != null) {
                Text(
                    "파트너 스탯 · ATK ${partner.atk} / DEF ${partner.def} · 성급 ${partner.starLevel}★",
                    color = TcgColors.Muted,
                    fontSize = 9.sp
                )
            }
        }

        // ── 「기억의 잔향」: 기억이 지워진 동료와의 재회 ──
        val memoryPartner = state.memoryResetCards.firstOrNull()
        if (memoryPartner != null) {
            val target = TcgCard.MEMORY_FRAGMENT_TARGET
            SectionCard(
                title = "기억의 잔향 — 기억이 지워진 동료",
                badge = "기억 파편 ${memoryPartner.memoryFragments}/$target",
                accent = TcgColors.Abyss
            ) {
                Text(
                    "「저… 계약자님이 맞으시죠? 이상하게, 이 문양을 보면 가슴이 아파요.」\n" +
                            "${memoryPartner.name}은(는) 죽음을 넘어 돌아왔지만, 당신과의 기억을 전부 잃었습니다. " +
                            "함께 걸으며 파편을 모으면 잊힌 이름이 돌아옵니다.",
                    color = TcgColors.Text.copy(alpha = 0.86f),
                    fontSize = 11.sp,
                    lineHeight = 17.sp
                )
                StatBar(
                    ratio = memoryPartner.memoryFragments.toFloat() / target,
                    color = TcgColors.Abyss,
                    height = 9
                )
                Text(
                    "기억 파편 ${memoryPartner.memoryFragments}/$target · 호감도 ${memoryPartner.bondLevel}/100",
                    color = TcgColors.Muted,
                    fontSize = 9.sp
                )
                ActionTile(
                    label = "기억의 잔향 교감 — ${memoryPartner.name}",
                    hint = "기억 파편 +1 · 호감도 +5 (전장 승리로도 파편 획득)",
                    accent = TcgColors.Abyss,
                    enabled = memoryPartner.memoryFragments < target,
                    onClick = { onBondMemoryPartner(memoryPartner.id) }
                )
                if (memoryPartner.memoryFragments >= target) {
                    Text(
                        "기억 파편이 모두 모였습니다 — 도감·서약 탭에서 「기억의 조각 회상」을 열어 기억을 복원하세요.",
                        color = TcgColors.Gold,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }
                if (state.memoryRecallMessage != null && memoryPartner.memoryFragments < target) {
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

        SectionCard(
            title = "데이트 룩 해금 연출 · $partnerName",
            badge = if (partnerBondNow >= SummonEngine.DATE_UNLOCK_BOND) "해금" else "잠금",
            accent = if (partnerBondNow >= SummonEngine.DATE_UNLOCK_BOND) TcgColors.Heal else TcgColors.Muted
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                CardArtImage(
                    cardId = summonTarget?.id ?: TcgGameViewModel.DEFAULT_LOBBY_HERO_ID,
                    visual = if (partnerBondNow >= SummonEngine.DATE_UNLOCK_BOND) {
                        VisualState.ROMANTIC_DATE
                    } else {
                        VisualState.CASUAL_CUTE
                    },
                    contentDescription = "데이트 룩",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    targetWidthPx = 540
                )
                if (partnerBondNow < SummonEngine.DATE_UNLOCK_BOND) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xCC0B0A12)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "호감도 50 달성 시 해금\n(현재 $partnerBondNow/100)",
                            color = TcgColors.Text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            if (partnerBondNow >= SummonEngine.DATE_UNLOCK_BOND) {
                Text(
                    "「이 옷, 당신이 좋아할까 싶어서…」 — 데이트 룩이 개방되었습니다. 도감 ②③ 탭에서도 확인할 수 있습니다.",
                    color = TcgColors.Heal,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
            if (partnerBondNow >= SummonEngine.ULTIMATE_UNLOCK_BOND) {
                Text(
                    "페어 얼티밋 오의 개방 — $partnerName 와(과)의 서약이 완성되었습니다. (전투 데미지 +25%)",
                    color = TcgColors.Gold,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
        }

        SectionCard(title = "교감 기록", accent = TcgColors.Muted) {
            LogBox(state.realWorldLog, maxLines = 6)
        }
    }
}
