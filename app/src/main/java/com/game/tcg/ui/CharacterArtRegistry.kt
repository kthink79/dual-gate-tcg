package com.game.tcg.ui

import com.game.tcg.R
import com.game.tcg.core.model.VisualState

/**
 * 카드 번호 + 3단 비주얼 상태 → 드로어블 리소스 매핑 테이블.
 *
 * 2026-11-15 개성파 신규 30인(No.155~184)의 JPG 90장(`res/drawable-nodpi/char_{번호}_{상태}.jpg`)을
 * 정식 등록했다. 도감·로비·전투·현실 교감의 모든 이미지는 이 표를 통해서만 해석된다.
 * (표에 없는 번호는 null → 호출부가 플레이스홀더로 대체한다.)
 */
object CharacterArtRegistry {

    private val ART: Map<Int, IntArray> = mapOf(
        1 to intArrayOf(R.drawable.char_seraphiel_battle, R.drawable.char_seraphiel_casual, R.drawable.char_seraphiel_date),
        2 to intArrayOf(R.drawable.char_elyria_battle, R.drawable.char_elyria_casual, R.drawable.char_elyria_date),
        3 to intArrayOf(R.drawable.char_aurelia_battle, R.drawable.char_aurelia_casual, R.drawable.char_aurelia_date),
        112 to intArrayOf(R.drawable.char_lilith_battle, R.drawable.char_lilith_casual, R.drawable.char_lilith_date),
        155 to intArrayOf(R.drawable.char_155_battle, R.drawable.char_155_casual, R.drawable.char_155_date),
        156 to intArrayOf(R.drawable.char_156_battle, R.drawable.char_156_casual, R.drawable.char_156_date),
        157 to intArrayOf(R.drawable.char_157_battle, R.drawable.char_157_casual, R.drawable.char_157_date),
        158 to intArrayOf(R.drawable.char_158_battle, R.drawable.char_158_casual, R.drawable.char_158_date),
        159 to intArrayOf(R.drawable.char_159_battle, R.drawable.char_159_casual, R.drawable.char_159_date),
        160 to intArrayOf(R.drawable.char_160_battle, R.drawable.char_160_casual, R.drawable.char_160_date),
        161 to intArrayOf(R.drawable.char_161_battle, R.drawable.char_161_casual, R.drawable.char_161_date),
        162 to intArrayOf(R.drawable.char_162_battle, R.drawable.char_162_casual, R.drawable.char_162_date),
        163 to intArrayOf(R.drawable.char_163_battle, R.drawable.char_163_casual, R.drawable.char_163_date),
        164 to intArrayOf(R.drawable.char_164_battle, R.drawable.char_164_casual, R.drawable.char_164_date),
        165 to intArrayOf(R.drawable.char_165_battle, R.drawable.char_165_casual, R.drawable.char_165_date),
        166 to intArrayOf(R.drawable.char_166_battle, R.drawable.char_166_casual, R.drawable.char_166_date),
        167 to intArrayOf(R.drawable.char_167_battle, R.drawable.char_167_casual, R.drawable.char_167_date),
        168 to intArrayOf(R.drawable.char_168_battle, R.drawable.char_168_casual, R.drawable.char_168_date),
        169 to intArrayOf(R.drawable.char_169_battle, R.drawable.char_169_casual, R.drawable.char_169_date),
        170 to intArrayOf(R.drawable.char_170_battle, R.drawable.char_170_casual, R.drawable.char_170_date),
        171 to intArrayOf(R.drawable.char_171_battle, R.drawable.char_171_casual, R.drawable.char_171_date),
        172 to intArrayOf(R.drawable.char_172_battle, R.drawable.char_172_casual, R.drawable.char_172_date),
        173 to intArrayOf(R.drawable.char_173_battle, R.drawable.char_173_casual, R.drawable.char_173_date),
        174 to intArrayOf(R.drawable.char_174_battle, R.drawable.char_174_casual, R.drawable.char_174_date),
        175 to intArrayOf(R.drawable.char_175_battle, R.drawable.char_175_casual, R.drawable.char_175_date),
        176 to intArrayOf(R.drawable.char_176_battle, R.drawable.char_176_casual, R.drawable.char_176_date),
        177 to intArrayOf(R.drawable.char_177_battle, R.drawable.char_177_casual, R.drawable.char_177_date),
        178 to intArrayOf(R.drawable.char_178_battle, R.drawable.char_178_casual, R.drawable.char_178_date),
        179 to intArrayOf(R.drawable.char_179_battle, R.drawable.char_179_casual, R.drawable.char_179_date),
        180 to intArrayOf(R.drawable.char_180_battle, R.drawable.char_180_casual, R.drawable.char_180_date),
        181 to intArrayOf(R.drawable.char_181_battle, R.drawable.char_181_casual, R.drawable.char_181_date),
        182 to intArrayOf(R.drawable.char_182_battle, R.drawable.char_182_casual, R.drawable.char_182_date),
        183 to intArrayOf(R.drawable.char_183_battle, R.drawable.char_183_casual, R.drawable.char_183_date),
        184 to intArrayOf(R.drawable.char_184_battle, R.drawable.char_184_casual, R.drawable.char_184_date),
    )

    /** 이 카드의 3단 비주얼이 모두 등록되어 있는지 */
    fun hasArt(cardId: Int): Boolean = ART[cardId]?.all { it != 0 } == true

    /** 카드 번호 + 비주얼 상태의 드로어블 리소스 (미등록 시 null) */
    fun resId(cardId: Int, visual: VisualState): Int? =
        ART[cardId]?.getOrNull(visual.ordinal)?.takeIf { it != 0 }

    /** 등록된 카드 번호 전체 */
    val registeredCardIds: Set<Int> get() = ART.keys

    /** 등록 카드 수 */
    val registeredCardCount: Int get() = ART.size
}
