#!/usr/bin/env python3
"""듀얼 게이트 TCG — 4번째 페어 '미니어처(SD/Chibi)' 아트 변환·탑재.

원본 통합 폴더(`downloads/TCG_통합_90종_3페어/`)에는 캐릭터 1명당 4장
(`{번호}-1.png` 전투 · `-2` 일상 · `-3` 데이트 · `-4` 미니어처)이 들어 있다.
이 스크립트는 그중 **`-4` 미니어처만** 골라 카드 규격 `540×810` JPG 로 변환해
`app/src/main/res/drawable-nodpi/char_{번호}_miniature.jpg` 로 배치한다.

사용법
─────
    python3 scripts/convert_miniature_art.py                       # 기본 폴더 자동 탐색
    python3 scripts/convert_miniature_art.py --src /path/to/dir    # 원본 폴더 직접 지정
    python3 scripts/convert_miniature_art.py --quality 85 --dry-run

원본은 1024×1536(2:3)이지만 규격을 강제하기 위해 `540×810` 로 리사이즈한다
(비율이 2:3 이 아니면 가운데 기준으로 잘라낸 뒤 축소 — 카드가 찌그러지지 않게).
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:  # pragma: no cover
    print("[x] Pillow 가 필요합니다: pip install pillow", file=sys.stderr)
    raise SystemExit(1)

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "app/src/main/res/drawable-nodpi"
TARGET = (540, 810)          # 카드 원본 규격 (2:3)
DEFAULT_SOURCES = [
    Path("/workspace/downloads/TCG_통합_90종_3페어"),
    ROOT.parent / "downloads/TCG_통합_90종_3페어",
]
MINIATURE_SUFFIX = "-4"


def find_source(explicit: str | None) -> Path:
    if explicit:
        p = Path(explicit)
        if not p.is_dir():
            raise SystemExit(f"[x] 원본 폴더가 없습니다: {p}")
        return p
    for candidate in DEFAULT_SOURCES:
        if candidate.is_dir() and any(candidate.glob(f"*{MINIATURE_SUFFIX}.png")):
            return candidate
    raise SystemExit(
        "[x] 미니어처 원본 폴더를 찾지 못했습니다. --src 로 지정하세요 "
        "(예: --src /workspace/downloads/TCG_통합_90종_3페어)"
    )


def fit_to_card(image: Image.Image) -> Image.Image:
    """2:3 비율로 가운데 크롭 후 540×810 리사이즈."""
    image = image.convert("RGB")
    width, height = image.size
    target_ratio = TARGET[0] / TARGET[1]
    ratio = width / height
    if abs(ratio - target_ratio) > 0.001:
        if ratio > target_ratio:      # 너무 넓다 → 좌우 크롭
            new_width = int(round(height * target_ratio))
            left = (width - new_width) // 2
            image = image.crop((left, 0, left + new_width, height))
        else:                          # 너무 길다 → 위아래 크롭
            new_height = int(round(width / target_ratio))
            top = (height - new_height) // 2
            image = image.crop((0, top, width, top + new_height))
    return image.resize(TARGET, Image.LANCZOS)


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="미니어처(-4) PNG → char_{번호}_miniature.jpg")
    ap.add_argument("--src", default=None, help="원본 통합 폴더")
    ap.add_argument("--dest", default=str(DEST), help=f"출력 폴더 (기본 {DEST})")
    ap.add_argument("--quality", type=int, default=85, help="JPEG 품질 (기본 85)")
    ap.add_argument("--dry-run", action="store_true", help="변환만 확인하고 쓰지 않음")
    args = ap.parse_args(argv)

    src = find_source(args.src)
    dest = Path(args.dest)
    files = sorted(src.glob(f"*{MINIATURE_SUFFIX}.png"))
    if not files:
        raise SystemExit(f"[x] {src} 안에 *{MINIATURE_SUFFIX}.png 가 없습니다.")

    name_re = re.compile(r"^(\d+)-4$")
    if not args.dry_run:
        dest.mkdir(parents=True, exist_ok=True)

    done = 0
    total_bytes = 0
    for path in files:
        m = name_re.match(path.stem)
        if not m:
            print(f"[!] 파일명 규칙 밖: {path.name}")
            continue
        card_id = int(m.group(1))
        image = fit_to_card(Image.open(path))
        out = dest / f"char_{card_id}_miniature.jpg"
        if not args.dry_run:
            image.save(out, "JPEG", quality=args.quality, optimize=True, progressive=True)
            total_bytes += out.stat().st_size
        done += 1

    print(f"원본: {src}")
    print(f"변환: {done}장 (540×810 JPG q={args.quality})")
    if not args.dry_run:
        print(f"출력: {dest}  ·  합계 {total_bytes / 1024 / 1024:.2f}MB "
              f"(평균 {total_bytes / max(done, 1) / 1024:.0f}KB)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
