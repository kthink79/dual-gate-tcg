#!/usr/bin/env python3
"""듀얼 게이트 TCG — 캐릭터 등장 영상 압축·탑재 스크립트.

원본 동영상(대개 1280x720 · 8~10MB · 10초)을 모바일용 MP4로 압축해
`app/src/main/assets/character_videos/{캐릭터번호}.mp4` 로 배치한다.

앱은 `CharacterVideoRegistry` 가 이 폴더를 런타임에 스캔하므로
**파일만 넣으면 Kotlin 코드 수정 없이** 카드팩 개봉·도감에서 자동 재생된다.

────────────────────────────────────────────────────────────────────
사용법
────────────────────────────────────────────────────────────────────

1) 파일명 앞에 캐릭터 번호가 있으면 그대로 실행 (예: `114-5.mp4` → 114.mp4)

    python3 scripts/compress_character_videos.py ~/Downloads/114-5.mp4

2) 번호를 직접 지정 (파일명에 번호가 없는 경우 — 예: gemini_generated_video_*.mp4)

    python3 scripts/compress_character_videos.py ~/Downloads/hero.mp4:6

3) 폴더를 통째로 (폴더 안 모든 *.mp4 를 파일명 규칙대로 처리)

    python3 scripts/compress_character_videos.py ~/Downloads/tcg_raw/

4) 앱에 넣기 전에 결과만 확인 (assets 에 쓰지 않음)

    python3 scripts/compress_character_videos.py ~/Downloads/114-5.mp4 --out-dir /tmp/out --dry-run

────────────────────────────────────────────────────────────────────
옵션
────────────────────────────────────────────────────────────────────
  --out-dir PATH       출력 폴더 (기본: app/src/main/assets/character_videos)
  --max-width N        가로 최대 픽셀 (기본 720, 원본이 작으면 그대로)
  --crf N              x264 품질 (기본 28, 낮을수록 고화질·대용량)
  --preset NAME        x264 프리셋 (기본 slow)
  --audio-bitrate B    AAC 비트레이트 (기본 64k)
  --no-audio           오디오 트랙 제거 (-an)
  --limit-mb M         편당 목표 상한 (기본 1.5MB, 넘으면 경고만)
  --delogo X:Y:W:H     모서리 워터마크 제거 영역 (ffmpeg delogo 필터). 원본 좌표로 지정
  --dry-run            압축 결과만 표로 보여 주고 파일을 배치하지 않음

ffmpeg 는 시스템에 없어도 된다 — `imageio_ffmpeg` 번들 바이너리를 쓴다.
(설치: `pip install imageio-ffmpeg`)
"""
from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUT = ROOT / "app/src/main/assets/character_videos"

# 캐릭터 번호 범위 (도감 No.1~184 · 중간 보스 170~175 포함)
MIN_CARD_ID = 1
MAX_CARD_ID = 999


def die(msg: str) -> "NoReturn":  # noqa: F821
    print(f"[x] {msg}", file=sys.stderr)
    raise SystemExit(1)


def ffmpeg_exe() -> str:
    """imageio_ffmpeg 번들 ffmpeg 경로 (없으면 시스템 ffmpeg)."""
    try:
        import imageio_ffmpeg  # type: ignore
        return imageio_ffmpeg.get_ffmpeg_exe()
    except Exception:  # pragma: no cover - 패키지 미설치 환경
        found = shutil.which("ffmpeg")
        if found:
            return found
        die("ffmpeg 를 찾을 수 없습니다. `pip install imageio-ffmpeg` 로 설치하세요.")


FFMPEG = ffmpeg_exe()


def probe(path: Path) -> dict:
    """ffmpeg -i 로 해상도·길이·오디오 유무를 읽는다 (ffprobe 없이 동작)."""
    proc = subprocess.run(
        [FFMPEG, "-hide_banner", "-i", str(path)],
        capture_output=True, text=True,
    )
    info = {"width": 0, "height": 0, "duration": 0.0, "has_audio": False}
    for line in proc.stderr.splitlines():
        line = line.strip()
        if line.startswith("Duration:"):
            m = re.search(r"Duration:\s*(\d+):(\d+):(\d+\.\d+)", line)
            if m:
                h, mi, s = int(m.group(1)), int(m.group(2)), float(m.group(3))
                info["duration"] = round(h * 3600 + mi * 60 + s, 2)
        elif line.startswith("Stream") and "Video:" in line:
            m = re.search(r"(\d{2,5})x(\d{2,5})", line)
            if m:
                info["width"], info["height"] = int(m.group(1)), int(m.group(2))
        elif line.startswith("Stream") and "Audio:" in line:
            info["has_audio"] = True
    return info


def card_id_from_name(name: str) -> int | None:
    """파일명 앞자리 숫자 → 캐릭터 번호 (`114-5.mp4` → 114, `006.mp4` → 6)."""
    m = re.match(r"^(\d{1,3})(?:[_\-. ]|$)", Path(name).stem + "-")
    if not m:
        return None
    cid = int(m.group(1))
    return cid if MIN_CARD_ID <= cid <= MAX_CARD_ID else None


def next_slot(out_dir: Path, cid: int, used: list[Path], dry_run: bool = False) -> Path:
    """이 캐릭터의 빈 파일 이름을 고른다 — `{id}.mp4` → `{id}_2.mp4` → `{id}_3.mp4` …"""
    used_names = {p.name for p in used}
    for n in range(1, 100):
        name = f"{cid}.mp4" if n == 1 else f"{cid}_{n}.mp4"
        if name in used_names:
            continue
        if not dry_run and (out_dir / name).exists():
            continue
        return out_dir / name
    die(f"No.{cid} 영상 슬롯이 너무 많습니다.")


def delogo_expr(spec: str | None) -> str | None:
    """`X:Y:W:H` → ffmpeg delogo 필터 문자열. 형식이 틀리면 즉시 실패한다."""
    if not spec:
        return None
    parts = spec.split(":")
    if len(parts) != 4 or not all(p.strip().lstrip("-").isdigit() for p in parts):
        die(f"--delogo 형식이 잘못되었습니다: {spec} (예: 1120:556:72:82)")
    x, y, w, h = (int(p) for p in parts)
    if w <= 0 or h <= 0:
        die(f"--delogo 폭·높이는 1 이상이어야 합니다: {spec}")
    return f"delogo=x={x}:y={y}:w={w}:h={h}"


def compress(src: Path, dst: Path, args: argparse.Namespace) -> dict:
    """1개 파일을 모바일 MP4 로 압축하고 결과 통계를 돌려준다."""
    before = probe(src)
    filters = [f for f in (delogo_expr(args.delogo),) if f]
    filters.append(f"scale='min({args.max_width},iw)':-2")
    vf = ",".join(filters)
    # 홀수 폭/높이를 피해야 libx264(yuv420p)가 실패하지 않는다 — scale 필터가 -2 로 짝수를 보장.
    cmd = [
        FFMPEG, "-y", "-hide_banner", "-loglevel", "error",
        "-i", str(src),
        "-vf", vf,
        "-c:v", "libx264", "-profile:v", "high", "-pix_fmt", "yuv420p",
        "-crf", str(args.crf), "-preset", args.preset,
        "-movflags", "+faststart",
    ]
    if args.no_audio or not before["has_audio"]:
        cmd += ["-an"]
    else:
        cmd += ["-c:a", "aac", "-b:a", args.audio_bitrate]
    cmd.append(str(dst))

    proc = subprocess.run(cmd, capture_output=True, text=True)
    if proc.returncode != 0:
        die(f"{src.name} 압축 실패:\n{proc.stderr.strip()[:600]}")

    after = probe(dst)
    return {
        "card_id": None,
        "src": src,
        "dst": dst,
        "before_bytes": src.stat().st_size,
        "after_bytes": dst.stat().st_size if dst.exists() else 0,
        "before": before,
        "after": after,
    }


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(
        description="캐릭터 등장 영상 압축 → assets/character_videos/{번호}.mp4",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    ap.add_argument("sources", nargs="+", help="원본 mp4 파일 또는 폴더 (file.mp4 또는 file.mp4:114)")
    ap.add_argument("--out-dir", default=str(DEFAULT_OUT), help=f"출력 폴더 (기본 {DEFAULT_OUT})")
    ap.add_argument("--max-width", type=int, default=720, help="가로 최대 픽셀 (기본 720)")
    ap.add_argument("--crf", type=int, default=28, help="x264 CRF (기본 28)")
    ap.add_argument("--preset", default="slow", help="x264 preset (기본 slow)")
    ap.add_argument("--audio-bitrate", default="64k", help="AAC 비트레이트 (기본 64k)")
    ap.add_argument("--no-audio", action="store_true", help="오디오 제거")
    ap.add_argument("--limit-mb", type=float, default=1.5, help="편당 목표 상한 MB (기본 1.5)")
    ap.add_argument("--delogo", default=None, help="워터마크 제거 영역 X:Y:W:H (원본 좌표)")
    ap.add_argument("--dry-run", action="store_true", help="배치하지 않고 결과만 확인")
    args = ap.parse_args(argv)

    # ── 입력 수집 (경로:번호 표기 허용) ───────────────────────────────
    jobs: list[tuple[Path, int]] = []
    explicit: dict[str, int] = {}
    files: list[Path] = []
    for raw in args.sources:
        item, _, num = raw.rpartition(":")
        if item and num.isdigit():
            explicit[item] = int(num)
            p = Path(item)
        else:
            p = Path(raw)
        if p.is_dir():
            files.extend(sorted(p.glob("*.mp4")) + sorted(p.glob("*.MP4")))
        elif p.is_file():
            files.append(p)
        else:
            die(f"입력 파일을 찾을 수 없습니다: {raw}")

    for f in files:
        cid = explicit.get(str(f)) or explicit.get(f.name) or card_id_from_name(f.name)
        if cid is None:
            die(
                f"{f.name}: 캐릭터 번호를 알 수 없습니다. "
                f"`{f.name}:<번호>` 형식으로 지정하세요 (예: {f.name}:114)."
            )
        jobs.append((f, cid))

    if not jobs:
        die("처리할 mp4 가 없습니다.")

    out_dir = Path(args.out_dir)
    if not args.dry_run:
        out_dir.mkdir(parents=True, exist_ok=True)

    print(f"ffmpeg: {FFMPEG}")
    print(f"출력  : {out_dir}{'  (dry-run: 배치하지 않음)' if args.dry_run else ''}")
    print(f"설정  : max-width={args.max_width}px crf={args.crf} preset={args.preset} "
          f"audio={'없음' if args.no_audio else args.audio_bitrate}")
    print()

    rows: list[dict] = []
    taken: dict[int, list[Path]] = {}
    with tempfile.TemporaryDirectory() as tmp:
        for src, cid in jobs:
            # 2026-11-19: 같은 캐릭터에 여러 편을 **모두** 넣는다(1:N).
            # 예전에는 한 캐릭터에 두 번째 영상이 오면 건너뛰었지만, 이제는
            # `{번호}.mp4` 가 이미 있으면 `{번호}_2.mp4`, `{번호}_3.mp4` … 로 나란히 배치한다.
            # (앱은 랜덤 재생으로 목록 중 1개를 고른다 — CharacterVideoIndex 참조)
            target = next_slot(out_dir, cid, taken.get(cid, []), dry_run=args.dry_run)
            taken.setdefault(cid, []).append(target)
            tmp_out = Path(tmp) / target.name
            print(f"· No.{cid:<4} ← {src.name}  → {target.name}")
            row = compress(src, tmp_out, args)
            row["card_id"] = cid
            if not args.dry_run:
                shutil.copyfile(tmp_out, target)
                row["dst"] = target
            rows.append(row)

    # ── 결과 표 ────────────────────────────────────────────────────
    print()
    header = f"{'No.':>5} {'원본':>10} {'압축':>9} {'감소':>7} {'해상도':>11} {'길이':>6}  파일"
    print(header)
    print("-" * len(header))
    total_before = total_after = 0
    warnings: list[str] = []
    for r in rows:
        mb = 1024 * 1024
        b, a = r["before_bytes"], r["after_bytes"]
        total_before += b
        total_after += a
        ratio = (1 - a / b) * 100 if b else 0
        before_res = f"{r['before']['width']}x{r['before']['height']}"
        after_res = f"{r['after']['width']}x{r['after']['height']}"
        print(
            f"{r['card_id']:>5} {b / mb:>9.2f}M {a / mb:>8.2f}M {ratio:>6.0f}% "
            f"{after_res:>11} {r['after']['duration']:>5.1f}s  "
            f"{before_res} → {r['dst'].name}"
        )
        if a > args.limit_mb * mb:
            warnings.append(
                f"No.{r['card_id']} {a / mb:.2f}MB — 목표 {args.limit_mb}MB 초과 "
                f"(CRF 를 올리거나 --max-width 를 낮추세요)"
            )
    print("-" * len(header))
    mb = 1024 * 1024
    print(
        f"{'합계':>5} {total_before / mb:>9.2f}M {total_after / mb:>8.2f}M "
        f"{(1 - total_after / total_before) * 100 if total_before else 0:>6.0f}%"
    )
    print()
    for w in warnings:
        print(f"[!] {w}")
    if not args.dry_run:
        print(f"[ok] {len(rows)}개 영상을 {out_dir} 에 배치했습니다.")
        print("     앱은 이 폴더를 런타임에 스캔하므로 코드 수정 없이 바로 재생됩니다.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
