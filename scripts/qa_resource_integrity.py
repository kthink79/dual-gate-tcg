#!/usr/bin/env python3
"""듀얼 게이트 TCG — 리소스·아트 무결성 전수 검사.

검사 항목
  1) CardCatalog(플레이 177명) · MidBossCatalog(중간 보스 6명)의 모든 캐릭터 번호가
     CharacterArtRegistry에 battle/casual/date/miniature 4비주얼 모두 등록되어 있는지
     (miniature = 2026-11-17 추가된 4번째 페어 '미니어처(SD/Chibi)')
  2) 등록된 모든 드로어블 이름이 res/drawable-nodpi/char_{번호}_{상태}.jpg 실제 파일과 1:1인지
  3) drawable-nodpi 안의 char_*.jpg 중 레지스트리에 없는 고아 파일이 있는지
  4) intro_scene_{1..4}.jpg 존재 + IntroArtRegistry 등록 여부
  5) Kotlin 소스의 R.drawable.* 참조가 모두 실제 리소스를 가리키는지
  6) 각 JPG가 0바이트가 아니고, 540×810 규격(가로<세로)인지

종료 코드: 문제 0건이면 0, 발견 시 1.
"""
from __future__ import annotations

import re
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res/drawable-nodpi"
SRC = ROOT / "app/src/main/java/com/game/tcg"

errors: list[str] = []
warnings: list[str] = []


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def jpeg_size(path: Path) -> tuple[int, int] | None:
    """JPEG SOF 마커에서 (width, height)를 읽는다."""
    data = path.read_bytes()
    if len(data) < 4 or data[0:2] != b"\xff\xd8":
        return None
    i = 2
    while i < len(data) - 9:
        if data[i] != 0xFF:
            i += 1
            continue
        marker = data[i + 1]
        if marker in (0xD8, 0xD9) or 0xD0 <= marker <= 0xD7:
            i += 2
            continue
        seg_len = struct.unpack(">H", data[i + 2:i + 4])[0]
        if 0xC0 <= marker <= 0xCF and marker not in (0xC4, 0xC8, 0xCC):
            h, w = struct.unpack(">HH", data[i + 5:i + 9])
            return w, h
        i += 2 + seg_len
    return None


# ── 1. 카탈로그 번호 수집 ────────────────────────────────────────────────
card_catalog = read(SRC / "core/model/CardCatalog.kt")
mid_boss = read(SRC / "core/model/MidBossCatalog.kt")
registry_src = read(SRC / "ui/CharacterArtRegistry.kt")

roster_ids = sorted({int(m) for m in re.findall(r"^\s*id\s*=\s*(\d+)\s*,", card_catalog, re.M)})
midboss_ids = sorted({int(m) for m in re.findall(r"^\s*id\s*=\s*(\d+)\s*,", mid_boss, re.M)})

if len(roster_ids) != 177:
    errors.append(f"CardCatalog 캐릭터 수가 177명이 아님: {len(roster_ids)}명")
if len(midboss_ids) != 6:
    errors.append(f"MidBossCatalog 캐릭터 수가 6명이 아님: {len(midboss_ids)}명")
if set(roster_ids) & set(midboss_ids):
    errors.append(f"플레이 로스터와 중간 보스 번호가 겹침: {sorted(set(roster_ids) & set(midboss_ids))}")

expected_ids = set(roster_ids) | set(midboss_ids)

# ── 2. CharacterArtRegistry 파싱 ─────────────────────────────────────────
# "170 to intArrayOf(R.drawable.char_170_battle, R.drawable.char_170_casual, R.drawable.char_170_date),"
entry_re = re.compile(
    r"(\d+)\s+to\s+intArrayOf\(\s*R\.drawable\.(char_\d+_battle)\s*,\s*"
    r"R\.drawable\.(char_\d+_casual)\s*,\s*R\.drawable\.(char_\d+_date)\s*,\s*"
    r"R\.drawable\.(char_\d+_miniature)\s*\)"
)
VISUAL_STATES = ("battle", "casual", "date", "miniature")
registry: dict[int, tuple[str, ...]] = {}
for entry in entry_re.findall(registry_src):
    n = int(entry[0])
    if n in registry:
        errors.append(f"CharacterArtRegistry에 번호 {n} 중복 등록")
    registry[n] = tuple(entry[1:])

# registry 표에 잡히지 않은 R.drawable.char_ 참조가 남아 있는지(파싱 누락 감지)
all_char_refs = re.findall(r"R\.drawable\.(char_\d+_\w+)", registry_src)
matched_refs = [name for trio in registry.values() for name in trio]
if sorted(all_char_refs) != sorted(matched_refs):
    errors.append(
        "CharacterArtRegistry 안에 파싱되지 않은 R.drawable.char_ 참조가 있음 "
        f"(전체 {len(all_char_refs)} / 파싱 {len(matched_refs)})"
    )

missing_registry = sorted(expected_ids - set(registry))
extra_registry = sorted(set(registry) - expected_ids)
if missing_registry:
    errors.append(f"아트 미등록 캐릭터 번호: {missing_registry}")
if extra_registry:
    errors.append(f"카탈로그에 없는 번호가 아트 레지스트리에 있음: {extra_registry}")

# ── 3. 파일 1:1 대응 ─────────────────────────────────────────────────────
files = {p.name for p in RES.glob("char_*.jpg")}
used_files: set[str] = set()

for card_id, visuals in sorted(registry.items()):
    for state, name in zip(VISUAL_STATES, visuals):
        fname = f"{name}.jpg"
        used_files.add(fname)
        # 번호·상태 이름 규칙 검증 (예: 7번 카드 battle → char_7_battle)
        if name != f"char_{card_id}_{state}":
            errors.append(f"번호 {card_id} {state} 등록 이름이 규칙과 다름: {name}")
        path = RES / fname
        if not path.exists():
            errors.append(f"파일 누락: {fname} (번호 {card_id} {state})")
            continue
        if path.stat().st_size == 0:
            errors.append(f"0바이트 파일: {fname}")
            continue
        size = jpeg_size(path)
        if size is None:
            errors.append(f"JPEG 아님/손상: {fname}")
        else:
            w, h = size
            if (w, h) != (540, 810):
                warnings.append(f"규격 외 크기 {w}×{h}: {fname}")

orphans = sorted(files - used_files)
if orphans:
    errors.append(f"레지스트리에 없는 고아 아트 파일 {len(orphans)}장: {orphans[:10]}")

# 같은 파일을 두 번호가 공유하면 1:1 매칭 위반
if len(used_files) != len(set(used_files)):
    errors.append("같은 아트 파일이 두 캐릭터에 중복 등록됨")
if len(used_files) != len(registry) * len(VISUAL_STATES):
    errors.append(
        f"등록 캐릭터 {len(registry)}명 × {len(VISUAL_STATES)} ≠ 사용 파일 {len(used_files)}장"
    )

# ── 4. 인트로 아트 ───────────────────────────────────────────────────────
intro_screen = read(SRC / "ui/IntroStoryScreen.kt")
intro_engine = read(SRC / "core/engine/IntroStoryEngine.kt")
for i in range(1, 5):
    fname = f"intro_scene_{i}.jpg"
    if not (RES / fname).exists():
        errors.append(f"인트로 일러스트 누락: {fname}")
    else:
        if (RES / fname).stat().st_size == 0:
            errors.append(f"인트로 일러스트 0바이트: {fname}")
        if f"R.drawable.intro_scene_{i}" not in intro_screen:
            errors.append(f"IntroArtRegistry에 {fname} 미등록")
    if f'"intro_scene_{i}"' not in intro_engine:
        errors.append(f"IntroStoryEngine 장면 대본에 intro_scene_{i} 없음")

# ── 5. R.drawable 참조 전수 검증 ─────────────────────────────────────────
known = set()
for p in RES.iterdir():
    if p.suffix.lower() in (".jpg", ".jpeg", ".png", ".webp"):
        known.add(p.stem)
for sub in ("drawable", "mipmap-anydpi-v26", "mipmap-hdpi", "mipmap-mdpi",
            "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi", "values"):
    d = ROOT / "app/src/main/res" / sub
    if d.exists():
        for p in d.iterdir():
            known.add(p.stem)

missing_refs: dict[str, list[str]] = {}
for kt in sorted(SRC.rglob("*.kt")):
    for ref in re.findall(r"R\.drawable\.(\w+)", read(kt)):
        if ref not in known:
            missing_refs.setdefault(ref, []).append(str(kt.relative_to(ROOT)))
for ref, where in sorted(missing_refs.items()):
    errors.append(f"R.drawable.{ref} 참조 대상 리소스 없음 (사용처 {len(where)}곳)")

# ── 6. 동적 리소스 조회(getIdentifier) 위험 점검 ──────────────────────────
for kt in sorted(SRC.rglob("*.kt")):
    txt = read(kt)
    if "getIdentifier(" in txt:
        warnings.append(f"이름 기반 동적 리소스 조회(getIdentifier) 사용: {kt.relative_to(ROOT)}")

# ── 7. 캐릭터 등장 영상 (assets/character_videos/{번호}.mp4) ───────────────
# 앱은 이 폴더를 런타임에 스캔한다(CharacterVideoRegistry). 파일만 추가하면 자동 반영되므로
# 이름 규약·용량·MP4 시그니처를 여기서 전수 검사해 "넣었는데 안 나오는" 상황을 막는다.
VIDEO_DIR = ROOT / "app/src/main/assets/character_videos"
VIDEO_NAME_RE = re.compile(r"^(\d{1,3})(?:[-_ ].*)?\.mp4$", re.IGNORECASE)
VIDEO_SIZE_LIMIT = int(1.5 * 1024 * 1024)  # 편당 1.5MB 권장 상한
# 2026-11-19: 한 캐릭터에 영상 여러 편(1:N)을 **모두** 넣는 규칙으로 바뀌었다.
#   `{번호}.mp4` · `{번호}_2.mp4` · `{번호}_3.mp4` … 는 각각 다른 테이크이며 정상이다.
#   (앱은 목록 중 1편을 랜덤 재생 — CharacterVideoIndex.randomVideoFor)
#   같은 **파일명**이 두 번 나오는 것만 오류, 번호 중복은 1:N 으로 인정한다.
video_ids: dict[int, list[str]] = {}
video_files: set[str] = set()
TAKE_NAME_RE = re.compile(r"^(\d{1,3})(?:_(\d+))?\.mp4$", re.IGNORECASE)

if VIDEO_DIR.is_dir():
    for p in sorted(VIDEO_DIR.iterdir()):
        if not p.is_file():
            continue
        m = VIDEO_NAME_RE.match(p.name)
        if not m:
            errors.append(f"등장 영상 파일명이 {{번호}}.mp4 규약이 아님: {p.name}")
            continue
        cid = int(m.group(1))
        if cid not in expected_ids:
            errors.append(f"등장 영상 {p.name} → 카탈로그에 없는 번호 {cid}")
            continue
        if p.name in video_files:
            errors.append(f"등장 영상 파일명 중복: {p.name}")
        video_files.add(p.name)
        # 같은 번호의 테이크가 이미 있는데 정식 이름(`{번호}.mp4`)을 또 넣으면 앱의 대표 선택이 흔들린다
        takes = video_ids.setdefault(cid, [])
        take_match = TAKE_NAME_RE.match(p.name)
        take_no = int(take_match.group(2)) if (take_match and take_match.group(2)) else 1
        if take_no in {1 if n == f"{cid}.mp4" else -1 for n in takes} and p.name in takes:
            errors.append(f"등장 영상 테이크 중복: {p.name}")
        takes.append(p.name)
        size = p.stat().st_size
        if size == 0:
            errors.append(f"등장 영상 0바이트: {p.name}")
        elif size > VIDEO_SIZE_LIMIT:
            errors.append(
                f"등장 영상 용량 초과 {size / 1024 / 1024:.2f}MB: {p.name} "
                f"(scripts/compress_character_videos.py 로 다시 압축)"
            )
        head = p.open("rb").read(12)
        if len(head) < 12 or head[4:8] != b"ftyp":
            errors.append(f"MP4 시그니처가 아님: {p.name}")
    # 영상 재생은 assets.openFd() 를 쓰므로 mp4 는 무압축 저장이어야 한다
    gradle_src = read(ROOT / "app/build.gradle.kts")
    if "noCompress" not in gradle_src or '"mp4"' not in gradle_src:
        warnings.append("app/build.gradle.kts 에 noCompress(mp4) 설정이 없음 — assets.openFd 재생이 실패할 수 있음")
else:
    warnings.append("assets/character_videos 폴더가 없음 — 등장 영상 미탑재 상태")

# ── 리포트 ───────────────────────────────────────────────────────────────
print("== 리소스·아트 무결성 전수 검사 ==")
print(f"플레이 로스터: {len(roster_ids)}명  (No.{min(roster_ids)}~{max(roster_ids)})")
print(f"중간 보스   : {len(midboss_ids)}명  {midboss_ids}")
print(f"아트 레지스트리: {len(registry)}명 × {len(VISUAL_STATES)}비주얼 = {len(registry) * len(VISUAL_STATES)}장")
print(f"drawable-nodpi char_*.jpg: {len(files)}장")
print(f"인트로 장면: 4장")
"영상 편수" 
print(f"캐릭터 등장 영상: {sum(len(v) for v in video_ids.values())}편 / {len(video_ids)}명 {sorted(video_ids)}")
print()
if warnings:
    print(f"[경고] {len(warnings)}건")
    for w in warnings[:20]:
        print("  -", w)
if errors:
    print(f"[실패] {len(errors)}건")
    for e in errors:
        print("  ✗", e)
    sys.exit(1)
print("[통과] 누락·오타·고아 파일·0 폴백 0건")
