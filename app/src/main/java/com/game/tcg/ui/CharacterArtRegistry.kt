package com.game.tcg.ui

import com.game.tcg.R
import com.game.tcg.core.model.VisualState

/**
 * 카드 번호 + 4종 비주얼 상태 → 드로어블 리소스 매핑 테이블.
 *
 * 2026-11-24 기준 **198명 × 4비주얼 = JPG 792장**
 * (`res/drawable-nodpi/char_{번호}_{battle|casual|date|miniature}.jpg`)을 정식 등록했다.
 * 도감·로비·전투·현실 교감·의상 선택의 모든 이미지는 이 표를 통해서만 해석된다.
 * (표에 없는 번호는 null → 호출부가 플레이스홀더로 대체한다.)
 *
 * 배열 순서는 [VisualState] ordinal 과 1:1 이다:
 * `[0] 전투 · [1] 일상 · [2] 데이트 · [3] 미니어처(SD)`.
 */
object CharacterArtRegistry {

    private val ART: Map<Int, IntArray> = mapOf(
        1 to intArrayOf(
            R.drawable.char_1_battle,
            R.drawable.char_1_casual,
            R.drawable.char_1_date,
            R.drawable.char_1_miniature
        ),
        2 to intArrayOf(
            R.drawable.char_2_battle,
            R.drawable.char_2_casual,
            R.drawable.char_2_date,
            R.drawable.char_2_miniature
        ),
        3 to intArrayOf(
            R.drawable.char_3_battle,
            R.drawable.char_3_casual,
            R.drawable.char_3_date,
            R.drawable.char_3_miniature
        ),
        4 to intArrayOf(
            R.drawable.char_4_battle,
            R.drawable.char_4_casual,
            R.drawable.char_4_date,
            R.drawable.char_4_miniature
        ),
        5 to intArrayOf(
            R.drawable.char_5_battle,
            R.drawable.char_5_casual,
            R.drawable.char_5_date,
            R.drawable.char_5_miniature
        ),
        6 to intArrayOf(
            R.drawable.char_6_battle,
            R.drawable.char_6_casual,
            R.drawable.char_6_date,
            R.drawable.char_6_miniature
        ),
        7 to intArrayOf(
            R.drawable.char_7_battle,
            R.drawable.char_7_casual,
            R.drawable.char_7_date,
            R.drawable.char_7_miniature
        ),
        8 to intArrayOf(
            R.drawable.char_8_battle,
            R.drawable.char_8_casual,
            R.drawable.char_8_date,
            R.drawable.char_8_miniature
        ),
        9 to intArrayOf(
            R.drawable.char_9_battle,
            R.drawable.char_9_casual,
            R.drawable.char_9_date,
            R.drawable.char_9_miniature
        ),
        10 to intArrayOf(
            R.drawable.char_10_battle,
            R.drawable.char_10_casual,
            R.drawable.char_10_date,
            R.drawable.char_10_miniature
        ),
        11 to intArrayOf(
            R.drawable.char_11_battle,
            R.drawable.char_11_casual,
            R.drawable.char_11_date,
            R.drawable.char_11_miniature
        ),
        12 to intArrayOf(
            R.drawable.char_12_battle,
            R.drawable.char_12_casual,
            R.drawable.char_12_date,
            R.drawable.char_12_miniature
        ),
        13 to intArrayOf(
            R.drawable.char_13_battle,
            R.drawable.char_13_casual,
            R.drawable.char_13_date,
            R.drawable.char_13_miniature
        ),
        14 to intArrayOf(
            R.drawable.char_14_battle,
            R.drawable.char_14_casual,
            R.drawable.char_14_date,
            R.drawable.char_14_miniature
        ),
        15 to intArrayOf(
            R.drawable.char_15_battle,
            R.drawable.char_15_casual,
            R.drawable.char_15_date,
            R.drawable.char_15_miniature
        ),
        16 to intArrayOf(
            R.drawable.char_16_battle,
            R.drawable.char_16_casual,
            R.drawable.char_16_date,
            R.drawable.char_16_miniature
        ),
        17 to intArrayOf(
            R.drawable.char_17_battle,
            R.drawable.char_17_casual,
            R.drawable.char_17_date,
            R.drawable.char_17_miniature
        ),
        18 to intArrayOf(
            R.drawable.char_18_battle,
            R.drawable.char_18_casual,
            R.drawable.char_18_date,
            R.drawable.char_18_miniature
        ),
        19 to intArrayOf(
            R.drawable.char_19_battle,
            R.drawable.char_19_casual,
            R.drawable.char_19_date,
            R.drawable.char_19_miniature
        ),
        20 to intArrayOf(
            R.drawable.char_20_battle,
            R.drawable.char_20_casual,
            R.drawable.char_20_date,
            R.drawable.char_20_miniature
        ),
        21 to intArrayOf(
            R.drawable.char_21_battle,
            R.drawable.char_21_casual,
            R.drawable.char_21_date,
            R.drawable.char_21_miniature
        ),
        22 to intArrayOf(
            R.drawable.char_22_battle,
            R.drawable.char_22_casual,
            R.drawable.char_22_date,
            R.drawable.char_22_miniature
        ),
        23 to intArrayOf(
            R.drawable.char_23_battle,
            R.drawable.char_23_casual,
            R.drawable.char_23_date,
            R.drawable.char_23_miniature
        ),
        24 to intArrayOf(
            R.drawable.char_24_battle,
            R.drawable.char_24_casual,
            R.drawable.char_24_date,
            R.drawable.char_24_miniature
        ),
        25 to intArrayOf(
            R.drawable.char_25_battle,
            R.drawable.char_25_casual,
            R.drawable.char_25_date,
            R.drawable.char_25_miniature
        ),
        26 to intArrayOf(
            R.drawable.char_26_battle,
            R.drawable.char_26_casual,
            R.drawable.char_26_date,
            R.drawable.char_26_miniature
        ),
        27 to intArrayOf(
            R.drawable.char_27_battle,
            R.drawable.char_27_casual,
            R.drawable.char_27_date,
            R.drawable.char_27_miniature
        ),
        28 to intArrayOf(
            R.drawable.char_28_battle,
            R.drawable.char_28_casual,
            R.drawable.char_28_date,
            R.drawable.char_28_miniature
        ),
        29 to intArrayOf(
            R.drawable.char_29_battle,
            R.drawable.char_29_casual,
            R.drawable.char_29_date,
            R.drawable.char_29_miniature
        ),
        30 to intArrayOf(
            R.drawable.char_30_battle,
            R.drawable.char_30_casual,
            R.drawable.char_30_date,
            R.drawable.char_30_miniature
        ),
        31 to intArrayOf(
            R.drawable.char_31_battle,
            R.drawable.char_31_casual,
            R.drawable.char_31_date,
            R.drawable.char_31_miniature
        ),
        32 to intArrayOf(
            R.drawable.char_32_battle,
            R.drawable.char_32_casual,
            R.drawable.char_32_date,
            R.drawable.char_32_miniature
        ),
        33 to intArrayOf(
            R.drawable.char_33_battle,
            R.drawable.char_33_casual,
            R.drawable.char_33_date,
            R.drawable.char_33_miniature
        ),
        34 to intArrayOf(
            R.drawable.char_34_battle,
            R.drawable.char_34_casual,
            R.drawable.char_34_date,
            R.drawable.char_34_miniature
        ),
        35 to intArrayOf(
            R.drawable.char_35_battle,
            R.drawable.char_35_casual,
            R.drawable.char_35_date,
            R.drawable.char_35_miniature
        ),
        36 to intArrayOf(
            R.drawable.char_36_battle,
            R.drawable.char_36_casual,
            R.drawable.char_36_date,
            R.drawable.char_36_miniature
        ),
        37 to intArrayOf(
            R.drawable.char_37_battle,
            R.drawable.char_37_casual,
            R.drawable.char_37_date,
            R.drawable.char_37_miniature
        ),
        38 to intArrayOf(
            R.drawable.char_38_battle,
            R.drawable.char_38_casual,
            R.drawable.char_38_date,
            R.drawable.char_38_miniature
        ),
        39 to intArrayOf(
            R.drawable.char_39_battle,
            R.drawable.char_39_casual,
            R.drawable.char_39_date,
            R.drawable.char_39_miniature
        ),
        40 to intArrayOf(
            R.drawable.char_40_battle,
            R.drawable.char_40_casual,
            R.drawable.char_40_date,
            R.drawable.char_40_miniature
        ),
        41 to intArrayOf(
            R.drawable.char_41_battle,
            R.drawable.char_41_casual,
            R.drawable.char_41_date,
            R.drawable.char_41_miniature
        ),
        42 to intArrayOf(
            R.drawable.char_42_battle,
            R.drawable.char_42_casual,
            R.drawable.char_42_date,
            R.drawable.char_42_miniature
        ),
        43 to intArrayOf(
            R.drawable.char_43_battle,
            R.drawable.char_43_casual,
            R.drawable.char_43_date,
            R.drawable.char_43_miniature
        ),
        44 to intArrayOf(
            R.drawable.char_44_battle,
            R.drawable.char_44_casual,
            R.drawable.char_44_date,
            R.drawable.char_44_miniature
        ),
        45 to intArrayOf(
            R.drawable.char_45_battle,
            R.drawable.char_45_casual,
            R.drawable.char_45_date,
            R.drawable.char_45_miniature
        ),
        46 to intArrayOf(
            R.drawable.char_46_battle,
            R.drawable.char_46_casual,
            R.drawable.char_46_date,
            R.drawable.char_46_miniature
        ),
        47 to intArrayOf(
            R.drawable.char_47_battle,
            R.drawable.char_47_casual,
            R.drawable.char_47_date,
            R.drawable.char_47_miniature
        ),
        48 to intArrayOf(
            R.drawable.char_48_battle,
            R.drawable.char_48_casual,
            R.drawable.char_48_date,
            R.drawable.char_48_miniature
        ),
        49 to intArrayOf(
            R.drawable.char_49_battle,
            R.drawable.char_49_casual,
            R.drawable.char_49_date,
            R.drawable.char_49_miniature
        ),
        50 to intArrayOf(
            R.drawable.char_50_battle,
            R.drawable.char_50_casual,
            R.drawable.char_50_date,
            R.drawable.char_50_miniature
        ),
        51 to intArrayOf(
            R.drawable.char_51_battle,
            R.drawable.char_51_casual,
            R.drawable.char_51_date,
            R.drawable.char_51_miniature
        ),
        52 to intArrayOf(
            R.drawable.char_52_battle,
            R.drawable.char_52_casual,
            R.drawable.char_52_date,
            R.drawable.char_52_miniature
        ),
        53 to intArrayOf(
            R.drawable.char_53_battle,
            R.drawable.char_53_casual,
            R.drawable.char_53_date,
            R.drawable.char_53_miniature
        ),
        54 to intArrayOf(
            R.drawable.char_54_battle,
            R.drawable.char_54_casual,
            R.drawable.char_54_date,
            R.drawable.char_54_miniature
        ),
        55 to intArrayOf(
            R.drawable.char_55_battle,
            R.drawable.char_55_casual,
            R.drawable.char_55_date,
            R.drawable.char_55_miniature
        ),
        56 to intArrayOf(
            R.drawable.char_56_battle,
            R.drawable.char_56_casual,
            R.drawable.char_56_date,
            R.drawable.char_56_miniature
        ),
        57 to intArrayOf(
            R.drawable.char_57_battle,
            R.drawable.char_57_casual,
            R.drawable.char_57_date,
            R.drawable.char_57_miniature
        ),
        58 to intArrayOf(
            R.drawable.char_58_battle,
            R.drawable.char_58_casual,
            R.drawable.char_58_date,
            R.drawable.char_58_miniature
        ),
        59 to intArrayOf(
            R.drawable.char_59_battle,
            R.drawable.char_59_casual,
            R.drawable.char_59_date,
            R.drawable.char_59_miniature
        ),
        60 to intArrayOf(
            R.drawable.char_60_battle,
            R.drawable.char_60_casual,
            R.drawable.char_60_date,
            R.drawable.char_60_miniature
        ),
        61 to intArrayOf(
            R.drawable.char_61_battle,
            R.drawable.char_61_casual,
            R.drawable.char_61_date,
            R.drawable.char_61_miniature
        ),
        62 to intArrayOf(
            R.drawable.char_62_battle,
            R.drawable.char_62_casual,
            R.drawable.char_62_date,
            R.drawable.char_62_miniature
        ),
        63 to intArrayOf(
            R.drawable.char_63_battle,
            R.drawable.char_63_casual,
            R.drawable.char_63_date,
            R.drawable.char_63_miniature
        ),
        64 to intArrayOf(
            R.drawable.char_64_battle,
            R.drawable.char_64_casual,
            R.drawable.char_64_date,
            R.drawable.char_64_miniature
        ),
        65 to intArrayOf(
            R.drawable.char_65_battle,
            R.drawable.char_65_casual,
            R.drawable.char_65_date,
            R.drawable.char_65_miniature
        ),
        66 to intArrayOf(
            R.drawable.char_66_battle,
            R.drawable.char_66_casual,
            R.drawable.char_66_date,
            R.drawable.char_66_miniature
        ),
        67 to intArrayOf(
            R.drawable.char_67_battle,
            R.drawable.char_67_casual,
            R.drawable.char_67_date,
            R.drawable.char_67_miniature
        ),
        68 to intArrayOf(
            R.drawable.char_68_battle,
            R.drawable.char_68_casual,
            R.drawable.char_68_date,
            R.drawable.char_68_miniature
        ),
        69 to intArrayOf(
            R.drawable.char_69_battle,
            R.drawable.char_69_casual,
            R.drawable.char_69_date,
            R.drawable.char_69_miniature
        ),
        70 to intArrayOf(
            R.drawable.char_70_battle,
            R.drawable.char_70_casual,
            R.drawable.char_70_date,
            R.drawable.char_70_miniature
        ),
        71 to intArrayOf(
            R.drawable.char_71_battle,
            R.drawable.char_71_casual,
            R.drawable.char_71_date,
            R.drawable.char_71_miniature
        ),
        72 to intArrayOf(
            R.drawable.char_72_battle,
            R.drawable.char_72_casual,
            R.drawable.char_72_date,
            R.drawable.char_72_miniature
        ),
        73 to intArrayOf(
            R.drawable.char_73_battle,
            R.drawable.char_73_casual,
            R.drawable.char_73_date,
            R.drawable.char_73_miniature
        ),
        74 to intArrayOf(
            R.drawable.char_74_battle,
            R.drawable.char_74_casual,
            R.drawable.char_74_date,
            R.drawable.char_74_miniature
        ),
        75 to intArrayOf(
            R.drawable.char_75_battle,
            R.drawable.char_75_casual,
            R.drawable.char_75_date,
            R.drawable.char_75_miniature
        ),
        76 to intArrayOf(
            R.drawable.char_76_battle,
            R.drawable.char_76_casual,
            R.drawable.char_76_date,
            R.drawable.char_76_miniature
        ),
        77 to intArrayOf(
            R.drawable.char_77_battle,
            R.drawable.char_77_casual,
            R.drawable.char_77_date,
            R.drawable.char_77_miniature
        ),
        78 to intArrayOf(
            R.drawable.char_78_battle,
            R.drawable.char_78_casual,
            R.drawable.char_78_date,
            R.drawable.char_78_miniature
        ),
        79 to intArrayOf(
            R.drawable.char_79_battle,
            R.drawable.char_79_casual,
            R.drawable.char_79_date,
            R.drawable.char_79_miniature
        ),
        80 to intArrayOf(
            R.drawable.char_80_battle,
            R.drawable.char_80_casual,
            R.drawable.char_80_date,
            R.drawable.char_80_miniature
        ),
        81 to intArrayOf(
            R.drawable.char_81_battle,
            R.drawable.char_81_casual,
            R.drawable.char_81_date,
            R.drawable.char_81_miniature
        ),
        82 to intArrayOf(
            R.drawable.char_82_battle,
            R.drawable.char_82_casual,
            R.drawable.char_82_date,
            R.drawable.char_82_miniature
        ),
        83 to intArrayOf(
            R.drawable.char_83_battle,
            R.drawable.char_83_casual,
            R.drawable.char_83_date,
            R.drawable.char_83_miniature
        ),
        84 to intArrayOf(
            R.drawable.char_84_battle,
            R.drawable.char_84_casual,
            R.drawable.char_84_date,
            R.drawable.char_84_miniature
        ),
        85 to intArrayOf(
            R.drawable.char_85_battle,
            R.drawable.char_85_casual,
            R.drawable.char_85_date,
            R.drawable.char_85_miniature
        ),
        86 to intArrayOf(
            R.drawable.char_86_battle,
            R.drawable.char_86_casual,
            R.drawable.char_86_date,
            R.drawable.char_86_miniature
        ),
        87 to intArrayOf(
            R.drawable.char_87_battle,
            R.drawable.char_87_casual,
            R.drawable.char_87_date,
            R.drawable.char_87_miniature
        ),
        88 to intArrayOf(
            R.drawable.char_88_battle,
            R.drawable.char_88_casual,
            R.drawable.char_88_date,
            R.drawable.char_88_miniature
        ),
        89 to intArrayOf(
            R.drawable.char_89_battle,
            R.drawable.char_89_casual,
            R.drawable.char_89_date,
            R.drawable.char_89_miniature
        ),
        90 to intArrayOf(
            R.drawable.char_90_battle,
            R.drawable.char_90_casual,
            R.drawable.char_90_date,
            R.drawable.char_90_miniature
        ),
        91 to intArrayOf(
            R.drawable.char_91_battle,
            R.drawable.char_91_casual,
            R.drawable.char_91_date,
            R.drawable.char_91_miniature
        ),
        92 to intArrayOf(
            R.drawable.char_92_battle,
            R.drawable.char_92_casual,
            R.drawable.char_92_date,
            R.drawable.char_92_miniature
        ),
        93 to intArrayOf(
            R.drawable.char_93_battle,
            R.drawable.char_93_casual,
            R.drawable.char_93_date,
            R.drawable.char_93_miniature
        ),
        94 to intArrayOf(
            R.drawable.char_94_battle,
            R.drawable.char_94_casual,
            R.drawable.char_94_date,
            R.drawable.char_94_miniature
        ),
        95 to intArrayOf(
            R.drawable.char_95_battle,
            R.drawable.char_95_casual,
            R.drawable.char_95_date,
            R.drawable.char_95_miniature
        ),
        96 to intArrayOf(
            R.drawable.char_96_battle,
            R.drawable.char_96_casual,
            R.drawable.char_96_date,
            R.drawable.char_96_miniature
        ),
        97 to intArrayOf(
            R.drawable.char_97_battle,
            R.drawable.char_97_casual,
            R.drawable.char_97_date,
            R.drawable.char_97_miniature
        ),
        98 to intArrayOf(
            R.drawable.char_98_battle,
            R.drawable.char_98_casual,
            R.drawable.char_98_date,
            R.drawable.char_98_miniature
        ),
        99 to intArrayOf(
            R.drawable.char_99_battle,
            R.drawable.char_99_casual,
            R.drawable.char_99_date,
            R.drawable.char_99_miniature
        ),
        100 to intArrayOf(
            R.drawable.char_100_battle,
            R.drawable.char_100_casual,
            R.drawable.char_100_date,
            R.drawable.char_100_miniature
        ),
        101 to intArrayOf(
            R.drawable.char_101_battle,
            R.drawable.char_101_casual,
            R.drawable.char_101_date,
            R.drawable.char_101_miniature
        ),
        102 to intArrayOf(
            R.drawable.char_102_battle,
            R.drawable.char_102_casual,
            R.drawable.char_102_date,
            R.drawable.char_102_miniature
        ),
        103 to intArrayOf(
            R.drawable.char_103_battle,
            R.drawable.char_103_casual,
            R.drawable.char_103_date,
            R.drawable.char_103_miniature
        ),
        104 to intArrayOf(
            R.drawable.char_104_battle,
            R.drawable.char_104_casual,
            R.drawable.char_104_date,
            R.drawable.char_104_miniature
        ),
        105 to intArrayOf(
            R.drawable.char_105_battle,
            R.drawable.char_105_casual,
            R.drawable.char_105_date,
            R.drawable.char_105_miniature
        ),
        106 to intArrayOf(
            R.drawable.char_106_battle,
            R.drawable.char_106_casual,
            R.drawable.char_106_date,
            R.drawable.char_106_miniature
        ),
        107 to intArrayOf(
            R.drawable.char_107_battle,
            R.drawable.char_107_casual,
            R.drawable.char_107_date,
            R.drawable.char_107_miniature
        ),
        108 to intArrayOf(
            R.drawable.char_108_battle,
            R.drawable.char_108_casual,
            R.drawable.char_108_date,
            R.drawable.char_108_miniature
        ),
        109 to intArrayOf(
            R.drawable.char_109_battle,
            R.drawable.char_109_casual,
            R.drawable.char_109_date,
            R.drawable.char_109_miniature
        ),
        110 to intArrayOf(
            R.drawable.char_110_battle,
            R.drawable.char_110_casual,
            R.drawable.char_110_date,
            R.drawable.char_110_miniature
        ),
        111 to intArrayOf(
            R.drawable.char_111_battle,
            R.drawable.char_111_casual,
            R.drawable.char_111_date,
            R.drawable.char_111_miniature
        ),
        112 to intArrayOf(
            R.drawable.char_112_battle,
            R.drawable.char_112_casual,
            R.drawable.char_112_date,
            R.drawable.char_112_miniature
        ),
        113 to intArrayOf(
            R.drawable.char_113_battle,
            R.drawable.char_113_casual,
            R.drawable.char_113_date,
            R.drawable.char_113_miniature
        ),
        114 to intArrayOf(
            R.drawable.char_114_battle,
            R.drawable.char_114_casual,
            R.drawable.char_114_date,
            R.drawable.char_114_miniature
        ),
        115 to intArrayOf(
            R.drawable.char_115_battle,
            R.drawable.char_115_casual,
            R.drawable.char_115_date,
            R.drawable.char_115_miniature
        ),
        116 to intArrayOf(
            R.drawable.char_116_battle,
            R.drawable.char_116_casual,
            R.drawable.char_116_date,
            R.drawable.char_116_miniature
        ),
        117 to intArrayOf(
            R.drawable.char_117_battle,
            R.drawable.char_117_casual,
            R.drawable.char_117_date,
            R.drawable.char_117_miniature
        ),
        118 to intArrayOf(
            R.drawable.char_118_battle,
            R.drawable.char_118_casual,
            R.drawable.char_118_date,
            R.drawable.char_118_miniature
        ),
        119 to intArrayOf(
            R.drawable.char_119_battle,
            R.drawable.char_119_casual,
            R.drawable.char_119_date,
            R.drawable.char_119_miniature
        ),
        120 to intArrayOf(
            R.drawable.char_120_battle,
            R.drawable.char_120_casual,
            R.drawable.char_120_date,
            R.drawable.char_120_miniature
        ),
        121 to intArrayOf(
            R.drawable.char_121_battle,
            R.drawable.char_121_casual,
            R.drawable.char_121_date,
            R.drawable.char_121_miniature
        ),
        122 to intArrayOf(
            R.drawable.char_122_battle,
            R.drawable.char_122_casual,
            R.drawable.char_122_date,
            R.drawable.char_122_miniature
        ),
        123 to intArrayOf(
            R.drawable.char_123_battle,
            R.drawable.char_123_casual,
            R.drawable.char_123_date,
            R.drawable.char_123_miniature
        ),
        124 to intArrayOf(
            R.drawable.char_124_battle,
            R.drawable.char_124_casual,
            R.drawable.char_124_date,
            R.drawable.char_124_miniature
        ),
        125 to intArrayOf(
            R.drawable.char_125_battle,
            R.drawable.char_125_casual,
            R.drawable.char_125_date,
            R.drawable.char_125_miniature
        ),
        126 to intArrayOf(
            R.drawable.char_126_battle,
            R.drawable.char_126_casual,
            R.drawable.char_126_date,
            R.drawable.char_126_miniature
        ),
        127 to intArrayOf(
            R.drawable.char_127_battle,
            R.drawable.char_127_casual,
            R.drawable.char_127_date,
            R.drawable.char_127_miniature
        ),
        128 to intArrayOf(
            R.drawable.char_128_battle,
            R.drawable.char_128_casual,
            R.drawable.char_128_date,
            R.drawable.char_128_miniature
        ),
        129 to intArrayOf(
            R.drawable.char_129_battle,
            R.drawable.char_129_casual,
            R.drawable.char_129_date,
            R.drawable.char_129_miniature
        ),
        130 to intArrayOf(
            R.drawable.char_130_battle,
            R.drawable.char_130_casual,
            R.drawable.char_130_date,
            R.drawable.char_130_miniature
        ),
        131 to intArrayOf(
            R.drawable.char_131_battle,
            R.drawable.char_131_casual,
            R.drawable.char_131_date,
            R.drawable.char_131_miniature
        ),
        132 to intArrayOf(
            R.drawable.char_132_battle,
            R.drawable.char_132_casual,
            R.drawable.char_132_date,
            R.drawable.char_132_miniature
        ),
        133 to intArrayOf(
            R.drawable.char_133_battle,
            R.drawable.char_133_casual,
            R.drawable.char_133_date,
            R.drawable.char_133_miniature
        ),
        134 to intArrayOf(
            R.drawable.char_134_battle,
            R.drawable.char_134_casual,
            R.drawable.char_134_date,
            R.drawable.char_134_miniature
        ),
        135 to intArrayOf(
            R.drawable.char_135_battle,
            R.drawable.char_135_casual,
            R.drawable.char_135_date,
            R.drawable.char_135_miniature
        ),
        136 to intArrayOf(
            R.drawable.char_136_battle,
            R.drawable.char_136_casual,
            R.drawable.char_136_date,
            R.drawable.char_136_miniature
        ),
        137 to intArrayOf(
            R.drawable.char_137_battle,
            R.drawable.char_137_casual,
            R.drawable.char_137_date,
            R.drawable.char_137_miniature
        ),
        138 to intArrayOf(
            R.drawable.char_138_battle,
            R.drawable.char_138_casual,
            R.drawable.char_138_date,
            R.drawable.char_138_miniature
        ),
        139 to intArrayOf(
            R.drawable.char_139_battle,
            R.drawable.char_139_casual,
            R.drawable.char_139_date,
            R.drawable.char_139_miniature
        ),
        140 to intArrayOf(
            R.drawable.char_140_battle,
            R.drawable.char_140_casual,
            R.drawable.char_140_date,
            R.drawable.char_140_miniature
        ),
        141 to intArrayOf(
            R.drawable.char_141_battle,
            R.drawable.char_141_casual,
            R.drawable.char_141_date,
            R.drawable.char_141_miniature
        ),
        142 to intArrayOf(
            R.drawable.char_142_battle,
            R.drawable.char_142_casual,
            R.drawable.char_142_date,
            R.drawable.char_142_miniature
        ),
        143 to intArrayOf(
            R.drawable.char_143_battle,
            R.drawable.char_143_casual,
            R.drawable.char_143_date,
            R.drawable.char_143_miniature
        ),
        144 to intArrayOf(
            R.drawable.char_144_battle,
            R.drawable.char_144_casual,
            R.drawable.char_144_date,
            R.drawable.char_144_miniature
        ),
        145 to intArrayOf(
            R.drawable.char_145_battle,
            R.drawable.char_145_casual,
            R.drawable.char_145_date,
            R.drawable.char_145_miniature
        ),
        146 to intArrayOf(
            R.drawable.char_146_battle,
            R.drawable.char_146_casual,
            R.drawable.char_146_date,
            R.drawable.char_146_miniature
        ),
        147 to intArrayOf(
            R.drawable.char_147_battle,
            R.drawable.char_147_casual,
            R.drawable.char_147_date,
            R.drawable.char_147_miniature
        ),
        148 to intArrayOf(
            R.drawable.char_148_battle,
            R.drawable.char_148_casual,
            R.drawable.char_148_date,
            R.drawable.char_148_miniature
        ),
        149 to intArrayOf(
            R.drawable.char_149_battle,
            R.drawable.char_149_casual,
            R.drawable.char_149_date,
            R.drawable.char_149_miniature
        ),
        150 to intArrayOf(
            R.drawable.char_150_battle,
            R.drawable.char_150_casual,
            R.drawable.char_150_date,
            R.drawable.char_150_miniature
        ),
        151 to intArrayOf(
            R.drawable.char_151_battle,
            R.drawable.char_151_casual,
            R.drawable.char_151_date,
            R.drawable.char_151_miniature
        ),
        152 to intArrayOf(
            R.drawable.char_152_battle,
            R.drawable.char_152_casual,
            R.drawable.char_152_date,
            R.drawable.char_152_miniature
        ),
        153 to intArrayOf(
            R.drawable.char_153_battle,
            R.drawable.char_153_casual,
            R.drawable.char_153_date,
            R.drawable.char_153_miniature
        ),
        154 to intArrayOf(
            R.drawable.char_154_battle,
            R.drawable.char_154_casual,
            R.drawable.char_154_date,
            R.drawable.char_154_miniature
        ),
        155 to intArrayOf(
            R.drawable.char_155_battle,
            R.drawable.char_155_casual,
            R.drawable.char_155_date,
            R.drawable.char_155_miniature
        ),
        156 to intArrayOf(
            R.drawable.char_156_battle,
            R.drawable.char_156_casual,
            R.drawable.char_156_date,
            R.drawable.char_156_miniature
        ),
        157 to intArrayOf(
            R.drawable.char_157_battle,
            R.drawable.char_157_casual,
            R.drawable.char_157_date,
            R.drawable.char_157_miniature
        ),
        158 to intArrayOf(
            R.drawable.char_158_battle,
            R.drawable.char_158_casual,
            R.drawable.char_158_date,
            R.drawable.char_158_miniature
        ),
        159 to intArrayOf(
            R.drawable.char_159_battle,
            R.drawable.char_159_casual,
            R.drawable.char_159_date,
            R.drawable.char_159_miniature
        ),
        160 to intArrayOf(
            R.drawable.char_160_battle,
            R.drawable.char_160_casual,
            R.drawable.char_160_date,
            R.drawable.char_160_miniature
        ),
        161 to intArrayOf(
            R.drawable.char_161_battle,
            R.drawable.char_161_casual,
            R.drawable.char_161_date,
            R.drawable.char_161_miniature
        ),
        162 to intArrayOf(
            R.drawable.char_162_battle,
            R.drawable.char_162_casual,
            R.drawable.char_162_date,
            R.drawable.char_162_miniature
        ),
        163 to intArrayOf(
            R.drawable.char_163_battle,
            R.drawable.char_163_casual,
            R.drawable.char_163_date,
            R.drawable.char_163_miniature
        ),
        164 to intArrayOf(
            R.drawable.char_164_battle,
            R.drawable.char_164_casual,
            R.drawable.char_164_date,
            R.drawable.char_164_miniature
        ),
        165 to intArrayOf(
            R.drawable.char_165_battle,
            R.drawable.char_165_casual,
            R.drawable.char_165_date,
            R.drawable.char_165_miniature
        ),
        166 to intArrayOf(
            R.drawable.char_166_battle,
            R.drawable.char_166_casual,
            R.drawable.char_166_date,
            R.drawable.char_166_miniature
        ),
        167 to intArrayOf(
            R.drawable.char_167_battle,
            R.drawable.char_167_casual,
            R.drawable.char_167_date,
            R.drawable.char_167_miniature
        ),
        168 to intArrayOf(
            R.drawable.char_168_battle,
            R.drawable.char_168_casual,
            R.drawable.char_168_date,
            R.drawable.char_168_miniature
        ),
        169 to intArrayOf(
            R.drawable.char_169_battle,
            R.drawable.char_169_casual,
            R.drawable.char_169_date,
            R.drawable.char_169_miniature
        ),
        184 to intArrayOf(
            R.drawable.char_184_battle,
            R.drawable.char_184_casual,
            R.drawable.char_184_date,
            R.drawable.char_184_miniature
        ),
        // ── 2026-11-19 신규 7인 (No.176 · 177 · 185~189) ──
        176 to intArrayOf(
            R.drawable.char_176_battle,
            R.drawable.char_176_casual,
            R.drawable.char_176_date,
            R.drawable.char_176_miniature
        ),
        177 to intArrayOf(
            R.drawable.char_177_battle,
            R.drawable.char_177_casual,
            R.drawable.char_177_date,
            R.drawable.char_177_miniature
        ),
        // ── 2026-11-20 신규 2인 (No.178 메이린 · No.179 린) ──
        178 to intArrayOf(
            R.drawable.char_178_battle,
            R.drawable.char_178_casual,
            R.drawable.char_178_date,
            R.drawable.char_178_miniature
        ),
        179 to intArrayOf(
            R.drawable.char_179_battle,
            R.drawable.char_179_casual,
            R.drawable.char_179_date,
            R.drawable.char_179_miniature
        ),
        // ── 2026-11-21 신규 1인 (No.180 카일라) ──
        180 to intArrayOf(
            R.drawable.char_180_battle,
            R.drawable.char_180_casual,
            R.drawable.char_180_date,
            R.drawable.char_180_miniature
        ),
        // ── 2026-11-22 신규 2인 (No.181 미라 · No.182 키라나) ──
        181 to intArrayOf(
            R.drawable.char_181_battle,
            R.drawable.char_181_casual,
            R.drawable.char_181_date,
            R.drawable.char_181_miniature
        ),
        182 to intArrayOf(
            R.drawable.char_182_battle,
            R.drawable.char_182_casual,
            R.drawable.char_182_date,
            R.drawable.char_182_miniature
        ),
        185 to intArrayOf(
            R.drawable.char_185_battle,
            R.drawable.char_185_casual,
            R.drawable.char_185_date,
            R.drawable.char_185_miniature
        ),
        186 to intArrayOf(
            R.drawable.char_186_battle,
            R.drawable.char_186_casual,
            R.drawable.char_186_date,
            R.drawable.char_186_miniature
        ),
        187 to intArrayOf(
            R.drawable.char_187_battle,
            R.drawable.char_187_casual,
            R.drawable.char_187_date,
            R.drawable.char_187_miniature
        ),
        188 to intArrayOf(
            R.drawable.char_188_battle,
            R.drawable.char_188_casual,
            R.drawable.char_188_date,
            R.drawable.char_188_miniature
        ),
        189 to intArrayOf(
            R.drawable.char_189_battle,
            R.drawable.char_189_casual,
            R.drawable.char_189_date,
            R.drawable.char_189_miniature
        ),
        190 to intArrayOf(
            R.drawable.char_190_battle,
            R.drawable.char_190_casual,
            R.drawable.char_190_date,
            R.drawable.char_190_miniature
        ),
        191 to intArrayOf(
            R.drawable.char_191_battle,
            R.drawable.char_191_casual,
            R.drawable.char_191_date,
            R.drawable.char_191_miniature
        ),
        192 to intArrayOf(
            R.drawable.char_192_battle,
            R.drawable.char_192_casual,
            R.drawable.char_192_date,
            R.drawable.char_192_miniature
        ),
        193 to intArrayOf(
            R.drawable.char_193_battle,
            R.drawable.char_193_casual,
            R.drawable.char_193_date,
            R.drawable.char_193_miniature
        ),
        194 to intArrayOf(
            R.drawable.char_194_battle,
            R.drawable.char_194_casual,
            R.drawable.char_194_date,
            R.drawable.char_194_miniature
        ),
        195 to intArrayOf(
            R.drawable.char_195_battle,
            R.drawable.char_195_casual,
            R.drawable.char_195_date,
            R.drawable.char_195_miniature
        ),
        196 to intArrayOf(
            R.drawable.char_196_battle,
            R.drawable.char_196_casual,
            R.drawable.char_196_date,
            R.drawable.char_196_miniature
        ),
        197 to intArrayOf(
            R.drawable.char_197_battle,
            R.drawable.char_197_casual,
            R.drawable.char_197_date,
            R.drawable.char_197_miniature
        ),
        198 to intArrayOf(
            R.drawable.char_198_battle,
            R.drawable.char_198_casual,
            R.drawable.char_198_date,
            R.drawable.char_198_miniature
        ),
        199 to intArrayOf(
            R.drawable.char_199_battle,
            R.drawable.char_199_casual,
            R.drawable.char_199_date,
            R.drawable.char_199_miniature
        ),
        170 to intArrayOf(
            R.drawable.char_170_battle,
            R.drawable.char_170_casual,
            R.drawable.char_170_date,
            R.drawable.char_170_miniature
        ),
        171 to intArrayOf(
            R.drawable.char_171_battle,
            R.drawable.char_171_casual,
            R.drawable.char_171_date,
            R.drawable.char_171_miniature
        ),
        172 to intArrayOf(
            R.drawable.char_172_battle,
            R.drawable.char_172_casual,
            R.drawable.char_172_date,
            R.drawable.char_172_miniature
        ),
        173 to intArrayOf(
            R.drawable.char_173_battle,
            R.drawable.char_173_casual,
            R.drawable.char_173_date,
            R.drawable.char_173_miniature
        ),
        174 to intArrayOf(
            R.drawable.char_174_battle,
            R.drawable.char_174_casual,
            R.drawable.char_174_date,
            R.drawable.char_174_miniature
        ),
        175 to intArrayOf(
            R.drawable.char_175_battle,
            R.drawable.char_175_casual,
            R.drawable.char_175_date,
            R.drawable.char_175_miniature
        ),

    )

    /** 이 카드의 4종 비주얼(전투·일상·데이트·미니어처)이 모두 등록되어 있는지 */
    fun hasArt(cardId: Int): Boolean = ART[cardId]?.all { it != 0 } == true

    /** 카드 번호 + 비주얼 상태의 드로어블 리소스 (미등록 시 null) */
    fun resId(cardId: Int, visual: VisualState): Int? =
        ART[cardId]?.getOrNull(visual.ordinal)?.takeIf { it != 0 }

    /** 등록된 카드 번호 전체 */
    val registeredCardIds: Set<Int> get() = ART.keys

    /** 등록 카드 수 */
    val registeredCardCount: Int get() = ART.size

    /** 한 캐릭터가 가진 비주얼 수 (= [VisualState] 개수) */
    val visualCount: Int get() = VisualState.entries.size
}
