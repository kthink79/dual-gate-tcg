package com.game.tcg.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.tcg.core.engine.RealWorldActivity
import com.game.tcg.core.model.VisualState

/**
 * 탭 4 — 🏙️ 현실 교감
 */
@Composable
fun RealWorldScreen(
    state: TcgGameState,
    onInteract: (RealWorldActivity) -> Unit
) {
    val bond = state.partnerBond
    val partner = state.ownedCards.firstOrNull { it.id == 3 }

    ScreenFrame {
        SectionCard(
            title = "현실 소환 · 아우렐리아",
            badge = if (bond >= 100) "오의 개방" else "호감도 $bond/100",
            accent = TcgColors.Gold
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Image(
                    painter = painterResource(characterImage(3, state.partnerVisual)),
                    contentDescription = "아우렐리아",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
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
                        Pill(state.partnerVisual.displayName, TcgColors.Gold, filled = true)
                        Spacer(Modifier.width(6.dp))
                        if (state.dateLookUnlocked) Pill("데이트 룩 해금", TcgColors.Heal)
                    }
                    Text(
                        when {
                            bond >= 100 -> "「오의가 열렸습니다. 제 모든 것을 당신의 검에 맡기죠.」"
                            bond >= 50 -> "「이런 옷… 어울릴까요? 당신 앞에서만 입어 볼게요.」"
                            else -> "「오늘도 부르셨습니까, 계약자.」"
                        },
                        color = TcgColors.Text,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            StatBar(ratio = bond / 100f, color = TcgColors.Gold, height = 10)
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

        SectionCard(
            title = "데이트 룩 해금 연출",
            badge = if (state.dateLookUnlocked) "해금" else "잠금",
            accent = if (state.dateLookUnlocked) TcgColors.Heal else TcgColors.Muted
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Image(
                    painter = painterResource(
                        characterImage(3, if (state.dateLookUnlocked) VisualState.ROMANTIC_DATE else VisualState.CASUAL_CUTE)
                    ),
                    contentDescription = "데이트 룩",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (!state.dateLookUnlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xCC0B0A12)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "호감도 50 달성 시 해금\n(현재 ${state.partnerBond}/100)",
                            color = TcgColors.Text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            if (state.dateLookUnlocked) {
                Text(
                    "「이 옷, 당신이 좋아할까 싶어서…」 — 데이트 룩이 개방되었습니다. 도감 ②③ 탭에서도 확인할 수 있습니다.",
                    color = TcgColors.Heal,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
            if (state.ultimateUnlocked) {
                Text(
                    "페어 얼티밋 오의 개방 — 아우렐리아와의 서약이 완성되었습니다. (전투 데미지 +25%)",
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
