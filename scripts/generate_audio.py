#!/usr/bin/env python3
"""
듀얼 게이트 — 이계의 문 : BGM · SFX 프로시저럴 생성기
=====================================================
외부 음원 없이 numpy 수식 합성만으로 게임 음악을 만든다.

  BGM 4종 (모두 앞뒤가 이어지는 완전 루프)
    bgm_intro   몽환 · 신비 — 카드팩 발견 / 게이트 개방 오프닝
    bgm_lobby   밝고 세련된 판타지+현대 — 로비 · 스테이지 · 상점 · 도감
    bgm_battle  박진감 — 일반 전투
    bgm_boss    웅장 · 긴박 — 중간 보스(170~175) · 3대 마왕(112~114)

  SFX 9종
    sfx_click / attack / skill / hit / summon / pack_open / upgrade / victory / defeat

루프 방식
  트랙 길이를 정확히 정수 마디로 잡고, 마디 끝을 넘어가는 잔향은 버퍼 선두로
  되접는다(circular fold). 그래서 마지막 샘플 → 첫 샘플 이음매가 끊기지 않는다.

인코딩
  1순위 ffmpeg(libvorbis, 가변 비트레이트 q5) → 2순위 libsndfile Vorbis → 3순위 WAV.
  결과물은 app/src/main/res/raw/ 아래에 .ogg 로 저장된다.

사용법
  python3 scripts/generate_audio.py                # 전체 생성
  python3 scripts/generate_audio.py --only bgm     # BGM만
  python3 scripts/generate_audio.py --check        # 생성 없이 기존 파일 검증
"""

from __future__ import annotations

import argparse
import json
import math
import os
import shutil
import subprocess
import sys
import wave

import numpy as np

# ──────────────────────────────────────────────────────────────
# 경로 · 상수
# ──────────────────────────────────────────────────────────────
HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(HERE)
RAW_DIR = os.path.join(REPO, "app", "src", "main", "res", "raw")
TMP_DIR = os.path.join(REPO, "app", "build", "audio-tmp")

SR = 44100          # 샘플레이트 (모노 합성 → 스테레오 배치)
TAIL = 4.0          # 루프 되접기용 꼬리 여유 (초)

rng_global = np.random.default_rng(20261116)


# ──────────────────────────────────────────────────────────────
# 음이름 · 스케일 유틸
# ──────────────────────────────────────────────────────────────
_PITCH = {
    "C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5,
    "F#": 6, "Gb": 6, "G": 7, "G#": 8, "Ab": 8, "A": 9, "A#": 10, "Bb": 10, "B": 11,
}


def midi_of(name: str) -> int:
    """'A3' · 'C#4' · 'Bb2' → MIDI 번호"""
    name = name.strip()
    octave = int(name[-1])
    return (octave + 1) * 12 + _PITCH[name[:-1]]


def hz(name: str) -> float:
    return 440.0 * (2.0 ** ((midi_of(name) - 69) / 12.0))


def transpose(name: str, semitones: int) -> str:
    target = midi_of(name) + semitones
    names = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]
    return f"{names[target % 12]}{target // 12 - 1}"


# ──────────────────────────────────────────────────────────────
# 기본 DSP
# ──────────────────────────────────────────────────────────────
def t_axis(n: int) -> np.ndarray:
    return np.arange(n, dtype=np.float64) / SR


def env_ad(n: int, attack: float, decay: float, curve: float = 2.0) -> np.ndarray:
    """어택 + 지수 감쇠 엔벨로프 (초 단위)"""
    t = t_axis(n)
    a = max(attack, 1e-4)
    rise = np.clip(t / a, 0.0, 1.0)
    rise = rise * rise * (3 - 2 * rise)            # smoothstep
    fall = np.exp(-np.clip(t - a, 0.0, None) * curve / max(decay, 1e-3))
    return rise * fall


def env_adsr(n: int, attack: float, decay: float, sustain: float, release: float) -> np.ndarray:
    t = t_axis(n)
    total = n / SR
    release = min(release, max(total - attack - decay, 0.01))
    env = np.zeros(n)
    a_end = min(attack, total)
    d_end = min(attack + decay, total)
    r_start = max(total - release, a_end)
    m = t < a_end
    env[m] = (t[m] / max(a_end, 1e-4)) ** 0.85
    m = (t >= a_end) & (t < d_end)
    if d_end > a_end:
        k = (t[m] - a_end) / max(d_end - a_end, 1e-9)
        env[m] = 1.0 - (1.0 - sustain) * k
    m = (t >= d_end) & (t < r_start)
    env[m] = sustain
    m = t >= r_start
    if np.any(m):
        span = max(total - r_start, 1e-6)
        env[m] = sustain * np.clip(1.0 - (t[m] - r_start) / span, 0.0, 1.0) ** 1.6
    # 끝단 클릭 방지
    edge = min(int(0.004 * SR), n // 4)
    if edge > 0:
        env[:edge] *= np.linspace(0, 1, edge)
        env[-edge:] *= np.linspace(1, 0, edge)
    return env


def fft_filter(x: np.ndarray, kind: str, cutoff: float, order: int = 2,
               cutoff2: float | None = None) -> np.ndarray:
    """
    주파수 영역 버터워스 응답 필터 (scipy 불필요).
    kind: 'low' | 'high' | 'band'
    """
    if x.size == 0:
        return x
    n = x.size
    pad = 1 << int(math.ceil(math.log2(max(n * 2, 1024))))
    spec = np.fft.rfft(x, pad)
    freqs = np.fft.rfftfreq(pad, 1.0 / SR)
    fc = max(cutoff, 1.0)
    if kind == "low":
        resp = 1.0 / np.sqrt(1.0 + (freqs / fc) ** (2 * order))
    elif kind == "high":
        resp = 1.0 / np.sqrt(1.0 + (fc / np.maximum(freqs, 1e-6)) ** (2 * order))
    else:
        f2 = max(cutoff2 or fc * 2.0, fc * 1.01)
        lo = 1.0 / np.sqrt(1.0 + (fc / np.maximum(freqs, 1e-6)) ** (2 * order))
        hi = 1.0 / np.sqrt(1.0 + (freqs / f2) ** (2 * order))
        resp = lo * hi
    out = np.fft.irfft(spec * resp, pad)
    return out[:n]


def soft_clip(x: np.ndarray, drive: float = 1.0) -> np.ndarray:
    return np.tanh(x * drive) / np.tanh(drive)


def high_shelf(x: np.ndarray, freq: float = 4200.0, gain_db: float = -3.0,
               span: float = 4000.0) -> np.ndarray:
    """
    고음역 소프트 셀프 — 2026-11-19 사용자 요청.

    > "BGM 이 전체적으로 거슬리지 않고 차분하게, 특히 메뉴 클릭음이 너무 하이톤"

    `freq` 아래는 그대로 두고, `freq + span` 위로 갈수록 `gain_db` 만큼 부드럽게 깎는다
    (smoothstep). 위상 이동이 없는 제로페이즈라 루프 이음매가 흐트러지지 않는다.
    """
    if x.size == 0 or gain_db == 0.0:
        return x
    n = x.size
    pad = 1 << int(math.ceil(math.log2(max(n * 2, 1024))))
    spec = np.fft.rfft(x, pad)
    freqs = np.fft.rfftfreq(pad, 1.0 / SR)
    w = np.clip((freqs - freq) / max(span, 1.0), 0.0, 1.0)
    w = w * w * (3.0 - 2.0 * w)
    g = 10.0 ** (gain_db / 20.0)
    resp = 1.0 + (g - 1.0) * w
    out = np.fft.irfft(spec * resp, pad)
    return out[:n]


def reverb(x: np.ndarray, decay: float = 1.1, mix: float = 0.25, seed: int = 7,
           circular: bool = False) -> np.ndarray:
    """
    지수 감쇠 노이즈 IR 컨볼루션 리버브.
    circular=True 면 FFT 길이를 신호 길이와 같게 잡아 잔향 꼬리가 선두로 되접힌다
    (루프 트랙에서 이음매가 끊기지 않는 핵심).
    """
    if x.size == 0 or mix <= 0:
        return x
    n = x.size
    n_ir = min(int(decay * SR), n)
    rng = np.random.default_rng(seed)
    ir = rng.uniform(-1.0, 1.0, n_ir) * np.exp(-np.linspace(0, 6.0, n_ir))
    ir = fft_filter(ir, "low", 5200.0, order=1)
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    if circular:
        wet = np.fft.irfft(np.fft.rfft(x, n) * np.fft.rfft(ir, n), n)
    else:
        size = 1 << int(math.ceil(math.log2(n + n_ir)))
        wet = np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)[:n]
    return (1.0 - mix) * x + mix * wet * 1.6


def normalize(x: np.ndarray, peak: float = 0.92) -> np.ndarray:
    m = np.max(np.abs(x)) if x.size else 0.0
    if m < 1e-9:
        return x
    return x * (peak / m)


# ──────────────────────────────────────────────────────────────
# 악기 (모두 모노 시그널 반환)
# ──────────────────────────────────────────────────────────────
def pad_note(freq: float, dur: float, amp: float = 0.2, detune: float = 0.0016,
             attack: float = 0.9, release: float = 1.4) -> np.ndarray:
    """따뜻한 신스 패드 — 배음 + 미세 디튠 3성부"""
    n = int(dur * SR)
    if n <= 0:
        return np.zeros(0)
    t = t_axis(n)
    out = np.zeros(n)
    for dt in (-detune, 0.0, detune):
        f = freq * (1.0 + dt)
        vib = 1.0 + 0.0012 * np.sin(2 * np.pi * (0.17 + dt * 20) * t)
        for k, g in ((1, 1.0), (2, 0.34), (3, 0.16), (4, 0.07), (5, 0.035)):
            out += g * np.sin(2 * np.pi * f * k * t * vib + k * 0.7)
    out /= 3.0
    out *= env_adsr(n, attack, 0.35, 0.82, release)
    return amp * out


def ks_pluck(freq: float, dur: float, amp: float = 0.3, damp: float = 0.997,
             bright: float = 0.55) -> np.ndarray:
    """카르플러스-스트롱 하프/기타 플럭"""
    n = int(dur * SR)
    if n <= 4:
        return np.zeros(max(n, 0))
    period = max(2, int(round(SR / freq)))
    rng = np.random.default_rng(int(freq * 131) % (2 ** 31 - 1))
    exc = rng.uniform(-1.0, 1.0, period)
    exc = np.convolve(exc, np.array([bright, 1.0 - bright]), mode="same")
    out = np.zeros(n + period)
    out[:period] = exc
    prev = exc.copy()
    pos = period
    while pos < n:
        blk = 0.5 * (prev + np.roll(prev, 1)) * damp
        end = min(pos + period, len(out))
        out[pos:end] = blk[: end - pos]
        prev = blk
        pos += period
    out = out[:n]
    out *= env_ad(n, 0.002, max(dur * 0.55, 0.05), curve=5.0)
    return amp * out


def fm_bell(freq: float, dur: float, amp: float = 0.3, ratio: float = 3.01,
            index: float = 5.5, decay: float = 3.2) -> np.ndarray:
    """FM 벨 — 신비로운 카드팩 · 게이트 사운드"""
    n = int(dur * SR)
    if n <= 0:
        return np.zeros(0)
    t = t_axis(n)
    mod = index * np.exp(-t * decay * 0.55) * np.sin(2 * np.pi * freq * ratio * t)
    sig = np.sin(2 * np.pi * freq * t + mod)
    sig += 0.25 * np.sin(2 * np.pi * freq * 2.0 * t) * np.exp(-t * decay)
    sig *= np.exp(-t * decay * 0.75) * (1.0 - np.exp(-t * 900.0))
    return amp * sig


def glock(freq: float, dur: float, amp: float = 0.22) -> np.ndarray:
    """글로켄슈필 — 밝은 금속 타격 (비화성 배음)"""
    n = int(dur * SR)
    if n <= 0:
        return np.zeros(0)
    t = t_axis(n)
    out = np.zeros(n)
    for ratio, g, dec in ((1.0, 1.0, 3.4), (2.76, 0.45, 4.6), (5.40, 0.22, 6.2), (8.93, 0.1, 8.0)):
        out += g * np.sin(2 * np.pi * freq * ratio * t) * np.exp(-t * dec)
    out *= (1.0 - np.exp(-t * 1400.0))
    return amp * out


def saw_lead(freq: float, dur: float, amp: float = 0.25, cutoff: float = 3200.0,
             vibrato: float = 0.006, attack: float = 0.012) -> np.ndarray:
    """리드 — 배음 합성 톱니 + 비브라토"""
    n = int(dur * SR)
    if n <= 0:
        return np.zeros(0)
    t = t_axis(n)
    vib = 1.0 + vibrato * np.sin(2 * np.pi * 5.2 * t) * np.clip(t / 0.25, 0, 1)
    out = np.zeros(n)
    kmax = int(cutoff / freq)
    for k in range(1, max(kmax, 1) + 1):
        out += (1.0 / k) * np.sin(2 * np.pi * freq * k * t * vib)
    out *= env_adsr(n, attack, 0.12, 0.72, 0.18)
    return amp * out


def brass(freqs, dur: float, amp: float = 0.22, attack: float = 0.05) -> np.ndarray:
    """브라스 스택 — 여러 음을 동시에, 살짝 밀려 들어오는 어택"""
    n = int(dur * SR)
    if n <= 0 or not freqs:
        return np.zeros(0)
    t = t_axis(n)
    out = np.zeros(n)
    for i, f in enumerate(freqs):
        bend = 1.0 + 0.004 * np.exp(-t * 18.0)
        for k in range(1, 9):
            if f * k > 9000:
                break
            out += (1.0 / (k ** 1.25)) * np.sin(2 * np.pi * f * k * t * bend + i * 0.5)
    out /= max(len(freqs), 1)
    out *= env_adsr(n, attack, 0.18, 0.75, 0.22)
    return amp * out


def choir(freqs, dur: float, amp: float = 0.2) -> np.ndarray:
    """합창 패드 — 비브라토 + 포먼트 근사"""
    n = int(dur * SR)
    if n <= 0 or not freqs:
        return np.zeros(0)
    t = t_axis(n)
    out = np.zeros(n)
    for i, f in enumerate(freqs):
        vib = 1.0 + 0.008 * np.sin(2 * np.pi * (4.3 + 0.4 * i) * t)
        for k in range(1, 13):
            if f * k > 8000:
                break
            g = 1.0 / k
            # 모음 '아' 근사 포먼트 강조
            for fc, bw, boost in ((730.0, 200.0, 2.4), (1090.0, 260.0, 1.7), (2440.0, 420.0, 0.7)):
                g += boost / k * math.exp(-((f * k - fc) ** 2) / (2 * bw ** 2))
            out += g * np.sin(2 * np.pi * f * k * t * vib + i)
    out /= max(len(freqs), 1)
    out *= env_adsr(n, 0.45, 0.4, 0.8, 0.7)
    return amp * out


def sine_bass(freq: float, dur: float, amp: float = 0.4, punch: float = 0.0) -> np.ndarray:
    n = int(dur * SR)
    if n <= 0:
        return np.zeros(0)
    t = t_axis(n)
    sig = np.sin(2 * np.pi * freq * t) + 0.22 * np.sin(2 * np.pi * freq * 2 * t)
    sig += 0.07 * np.sin(2 * np.pi * freq * 3 * t)
    env = env_adsr(n, 0.006 + punch, 0.09, 0.68, 0.1)
    return amp * sig * env


def sub_drone(freq: float, dur: float, amp: float = 0.3) -> np.ndarray:
    n = int(dur * SR)
    if n <= 0:
        return np.zeros(0)
    t = t_axis(n)
    trem = 1.0 + 0.12 * np.sin(2 * np.pi * 0.21 * t)
    sig = np.sin(2 * np.pi * freq * t) * trem
    return amp * sig * env_adsr(n, 1.2, 0.6, 0.85, 1.4)


# ── 타격계 ──
def kick(dur: float = 0.32, amp: float = 0.55, f0: float = 128.0, f1: float = 44.0) -> np.ndarray:
    n = int(dur * SR)
    t = t_axis(n)
    f = f1 + (f0 - f1) * np.exp(-t * 32.0)
    sig = np.sin(2 * np.pi * np.cumsum(f) / SR)
    sig *= np.exp(-t * 7.5)
    sig += 0.25 * np.exp(-t * 260.0) * np.random.default_rng(3).uniform(-1, 1, n)
    return amp * sig


def snare(dur: float = 0.24, amp: float = 0.32, seed: int = 11) -> np.ndarray:
    n = int(dur * SR)
    t = t_axis(n)
    noise = np.random.default_rng(seed).uniform(-1, 1, n)
    noise = fft_filter(noise, "band", 1500.0, order=1, cutoff2=8200.0)
    body = np.sin(2 * np.pi * 178.0 * t) * np.exp(-t * 26.0)
    return amp * (noise * np.exp(-t * 17.0) * 1.2 + 0.35 * body)


def hat(dur: float = 0.07, amp: float = 0.12, open_hat: bool = False, seed: int = 5) -> np.ndarray:
    n = int(dur * SR)
    t = t_axis(n)
    noise = np.random.default_rng(seed).uniform(-1, 1, n)
    noise = fft_filter(noise, "high", 7200.0, order=2)
    decay = 22.0 if open_hat else 68.0
    return amp * noise * np.exp(-t * decay)


def crash(dur: float = 1.6, amp: float = 0.2, seed: int = 19) -> np.ndarray:
    n = int(dur * SR)
    t = t_axis(n)
    noise = np.random.default_rng(seed).uniform(-1, 1, n)
    noise = fft_filter(noise, "high", 4200.0, order=1)
    return amp * noise * np.exp(-t * 3.4)


def timpani(freq: float, dur: float = 0.9, amp: float = 0.4) -> np.ndarray:
    n = int(dur * SR)
    t = t_axis(n)
    sig = np.sin(2 * np.pi * freq * t) * np.exp(-t * 4.2)
    sig += 0.4 * np.sin(2 * np.pi * freq * 1.5 * t) * np.exp(-t * 7.0)
    sig += 0.15 * np.sin(2 * np.pi * freq * 2.02 * t) * np.exp(-t * 9.0)
    sig *= (1.0 - np.exp(-t * 900.0))
    return amp * sig


def noise_swell(dur: float, amp: float = 0.18, seed: int = 23, rising: bool = True) -> np.ndarray:
    n = int(dur * SR)
    t = t_axis(n)
    noise = np.random.default_rng(seed).uniform(-1, 1, n)
    noise = fft_filter(noise, "band", 900.0, order=1, cutoff2=7000.0)
    shape = np.clip(t / max(dur, 1e-6), 0, 1) ** 2.2 if rising else np.exp(-t * 3.5)
    return amp * noise * shape


# ──────────────────────────────────────────────────────────────
# 버스 — 스테레오 트랙 조립 (루프 되접기 지원)
# ──────────────────────────────────────────────────────────────
class Bus:
    """스테레오 버퍼. 루프 트랙이면 길이를 정확히 유지하고 꼬리를 선두로 되접는다."""

    def __init__(self, duration: float, loop: bool = True):
        self.n = int(round(duration * SR))
        self.loop = loop
        self.tail = int(TAIL * SR)
        self.buf = np.zeros((2, self.n + (self.tail if loop else 0)))

    def add(self, sig: np.ndarray, start: float, gain: float = 1.0, pan: float = 0.0):
        """
        sig(모노)를 start(초) 지점에 배치. pan -1(좌) ~ +1(우).
        루프 트랙에서 끝을 넘으면 선두로 되접는다(circular fold).
        """
        if sig.size == 0 or gain == 0.0:
            return
        i0 = int(round(start * SR))
        if i0 >= self.buf.shape[1]:
            return
        i1 = min(i0 + sig.size, self.buf.shape[1])
        seg = sig[: i1 - i0] * gain
        ang = (pan + 1.0) * math.pi / 4.0          # 등전력 패닝
        gl, gr = math.cos(ang), math.sin(ang)
        self.buf[0, i0:i1] += seg * gl
        self.buf[1, i0:i1] += seg * gr

    def finish(self) -> np.ndarray:
        """루프 꼬리 되접기 후 정확한 길이의 스테레오 반환"""
        if self.loop:
            out = self.buf[:, : self.n].copy()
            out[:, : self.tail] += self.buf[:, self.n: self.n + self.tail]
            return out
        return self.buf[:, : self.n].copy()


def micro_fade(stereo: np.ndarray, ms: float = 3.5) -> np.ndarray:
    """
    루프 이음매 클릭 제거용 초단기 페이드 (기본 3.5ms).
    들리지 않을 만큼 짧지만, 마지막/첫 샘플을 0 에 붙여 이음매 불연속을 없앤다.
    """
    k = min(int(ms * 0.001 * SR), stereo.shape[1] // 2)
    if k <= 1:
        return stereo
    ramp = 0.5 - 0.5 * np.cos(np.linspace(0.0, np.pi, k))     # raised cosine
    out = stereo.copy()
    out[:, :k] *= ramp
    out[:, -k:] *= ramp[::-1]
    return out


def apply_bus_fx(stereo: np.ndarray, reverb_mix: float, low_cut: float = 30.0,
                 high_cut: float = 16000.0, loop: bool = True) -> np.ndarray:
    left = fft_filter(stereo[0], "high", low_cut, order=2)
    right = fft_filter(stereo[1], "high", low_cut, order=2)
    if loop:
        # 루프 양끝을 이어 붙여 필터링하면 이음매가 유지된다
        wl = np.concatenate([left[-SR:], left, left[:SR]])
        wr = np.concatenate([right[-SR:], right, right[:SR]])
        wl = fft_filter(wl, "low", high_cut, order=2)[SR: -SR]
        wr = fft_filter(wr, "low", high_cut, order=2)[SR: -SR]
    else:
        wl = fft_filter(left, "low", high_cut, order=2)
        wr = fft_filter(right, "low", high_cut, order=2)
    if reverb_mix > 0:
        wl = reverb(wl, 1.35, reverb_mix, seed=101, circular=loop)
        wr = reverb(wr, 1.35, reverb_mix * 0.94, seed=202, circular=loop)
    return np.stack([wl, wr])


def finalize_loop(bus: "Bus", reverb_mix: float, low_cut: float, high_cut: float,
                  drive: float, peak: float,
                  air_shelf_db: float = -3.2, air_shelf_hz: float = 4200.0) -> np.ndarray:
    """
    루프 마무리 — 꼬리 되접기 → 원형 필터·리버브 → 고음 셀프 → 정규화 → 초단기 페이드.

    2026-11-19: 고음역 자극(글록·벨·하이햇의 8kHz 위 성분)을 `air_shelf_db` 만큼 깎는다.
    """
    out = bus.finish()
    out = apply_bus_fx(out, reverb_mix=reverb_mix, low_cut=low_cut, high_cut=high_cut, loop=True)
    out = np.stack([
        high_shelf(out[0], freq=air_shelf_hz, gain_db=air_shelf_db),
        high_shelf(out[1], freq=air_shelf_hz, gain_db=air_shelf_db),
    ])
    out = normalize(soft_clip(out, drive), peak)
    return micro_fade(out, ms=3.5)


# ──────────────────────────────────────────────────────────────
# BGM ① 인트로 — 몽환 · 신비 (80 BPM, 4/4, 8마디 = 24초)
# ──────────────────────────────────────────────────────────────
def build_bgm_intro() -> np.ndarray:
    bpm, beats, bars = 80.0, 4, 8
    beat = 60.0 / bpm
    bar = beat * beats
    bus = Bus(bar * bars, loop=True)

    # 2마디씩 4개의 화음 — Am9 · Fmaj7 · Cmaj7 · Em7 (마지막→처음이 v-i 로 부드럽다)
    chords = [
        (["A2", "A3", "C4", "E4", "B4"], "A2"),      # Am9
        (["F2", "A3", "C4", "E4", "G4"], "F2"),      # Fmaj7
        (["C3", "G3", "E4", "B4", "D5"], "C3"),      # Cmaj7
        (["E2", "E3", "B3", "D4", "G4"], "E2"),      # Em7
    ]
    pad_bars = [0, 0, 1, 1, 2, 2, 3, 3]

    for b in range(bars):
        notes, root = chords[pad_bars[b]]
        start = b * bar
        # 패드 — 화음 전체, 2마디 길이로 겹쳐 이어지게
        for i, nm in enumerate(notes[1:]):
            pan = -0.35 + 0.6 * (i / max(len(notes) - 2, 1))
            bus.add(pad_note(hz(nm), bar * 2.15, amp=0.15, attack=0.85, release=1.6),
                    start, gain=1.0, pan=pan)
        # 서브 드론 — 루트
        bus.add(sub_drone(hz(root), bar * 2.0, amp=0.20), start, pan=0.0)

        # 신비 벨 아르페지오 (8분음표, 드문드문)
        arp = [notes[2], notes[3], transpose(notes[1], 12), notes[4]]
        for k in range(4):
            if (b + k) % 3 == 2:      # 가끔 쉼 — 여백이 몽환감을 만든다
                continue
            nm = arp[(b + k) % len(arp)]
            bus.add(fm_bell(hz(nm), 1.9, amp=0.115, ratio=3.01, index=4.2, decay=2.6),
                    start + k * beat, pan=-0.25 if k % 2 == 0 else 0.25)

        # 낮은 심장박동(thud) — 2마디마다 한 번
        if b % 2 == 0:
            bus.add(timpani(hz(transpose(root, -12)), 1.4, amp=0.22), start, pan=0.0)

    # 후반부(5~8마디)에 게이트가 열리는 느낌의 스웰 + 고음 반짝임
    bus.add(noise_swell(bar * 1.6, amp=0.10, seed=31, rising=True), bar * 4, pan=-0.2)
    bus.add(noise_swell(bar * 1.6, amp=0.08, seed=32, rising=True), bar * 4.1, pan=0.3)
    sparkle = ["A5", "E6", "B5", "C6", "E6", "A5"]
    for i, nm in enumerate(sparkle):
        bus.add(glock(hz(nm), 1.4, amp=0.06), bar * 4 + i * beat * 2, pan=0.35 - 0.14 * i)

    # 3·6마디의 벨 모티프 — 기억에 남는 4음
    motif = ["E5", "C5", "A4", "B4"]
    for b in (1, 5):
        for i, nm in enumerate(motif):
            bus.add(fm_bell(hz(nm), 2.2, amp=0.13, ratio=2.0, index=3.0, decay=2.0),
                    b * bar + i * beat, pan=0.1 if i % 2 else -0.1)

    return finalize_loop(bus, reverb_mix=0.42, low_cut=32.0, high_cut=9800.0, drive=1.15, peak=0.78,
                         air_shelf_db=-4.0, air_shelf_hz=3600.0)


# ──────────────────────────────────────────────────────────────
# BGM ② 로비 — 밝고 세련된 판타지+현대 (100 BPM, 4/4, 12마디 = 28.8초)
# ──────────────────────────────────────────────────────────────
def build_bgm_lobby() -> np.ndarray:
    bpm, beats, bars = 100.0, 4, 12
    beat = 60.0 / bpm
    bar = beat * beats
    bus = Bus(bar * bars, loop=True)

    prog = [
        (["C3", "E4", "G4", "B4"], "C2", ["C4", "E4", "G4", "B4", "D5"]),   # Cmaj7
        (["A2", "C4", "E4", "G4"], "A1", ["A3", "C4", "E4", "G4", "B4"]),   # Am7
        (["F2", "A3", "C4", "E4"], "F1", ["F3", "A3", "C4", "E4", "G4"]),   # Fmaj7
        (["G2", "B3", "D4", "E4"], "G1", ["G3", "B3", "D4", "E4", "A4"]),   # G6
    ]

    for b in range(bars):
        notes, root, arp_notes = prog[b % 4]
        start = b * bar

        # 따뜻한 패드 (한 마디)
        for i, nm in enumerate(notes[1:]):
            bus.add(pad_note(hz(nm), bar * 1.05, amp=0.085, attack=0.22, release=0.5),
                    start, pan=-0.3 + 0.2 * i)
        bus.add(sub_drone(hz(root), bar * 1.0, amp=0.13), start)

        # 하프 아르페지오 — 16분음표 패턴 (루트-5-7-3)
        pattern = [0, 2, 4, 3, 1, 4, 2, 3]
        for k in range(8):
            idx = pattern[k]
            nm = arp_notes[idx % len(arp_notes)]
            vel = 0.15 if k % 2 == 0 else 0.10
            bus.add(ks_pluck(hz(nm), beat * 0.72, amp=vel, damp=0.9965, bright=0.6),
                    start + k * beat * 0.5, pan=-0.25 + 0.12 * (idx % 3))

        # 베이스 — 1 · 3박
        bus.add(sine_bass(hz(root), beat * 0.9, amp=0.30, punch=0.004), start, pan=0.0)
        bus.add(sine_bass(hz(root), beat * 0.85, amp=0.24, punch=0.004), start + beat * 2, pan=0.0)

        # 드럼 — 가벼운 그루브
        bus.add(kick(0.3, amp=0.30, f0=110.0, f1=48.0), start, pan=0.0)
        if b % 4 in (1, 3):
            bus.add(kick(0.3, amp=0.24, f0=110.0, f1=48.0), start + beat * 2.5, pan=0.0)
        bus.add(snare(0.2, amp=0.13, seed=40 + b % 4), start + beat, pan=0.06)
        bus.add(snare(0.2, amp=0.15, seed=41 + b % 4), start + beat * 3, pan=0.06)
        for k in range(8):
            bus.add(hat(0.06, amp=0.055 if k % 2 else 0.075, seed=50 + k),
                    start + k * beat * 0.5, pan=0.28)

        # 벨 카운터 멜로디 — 5~8마디, 9~12마디에서 변화
        if 4 <= b < 8:
            mel = [("E5", 0.0), ("G5", 1.0), ("A5", 2.0), ("G5", 3.0)]
            for nm, pos in mel:
                bus.add(fm_bell(hz(nm), 1.3, amp=0.075, ratio=2.0, index=2.4, decay=3.0),
                        start + pos * beat, pan=0.18)
        elif b >= 8:
            mel = [("D5", 0.0), ("E5", 1.5), ("C5", 2.0), ("B4", 3.0)]
            for nm, pos in mel:
                bus.add(glock(hz(nm), 1.1, amp=0.06), start + pos * beat, pan=-0.2)

    # 마지막 마디 프레이즈 마무리 (루프 앞 4마디로 자연스럽게 넘어가도록 상승 음형)
    bus.add(glock(hz("G5"), 1.8, amp=0.07), bar * 11 + beat * 3.0, pan=0.2)
    bus.add(fm_bell(hz("C6"), 2.0, amp=0.06, ratio=2.0, index=2.0, decay=2.4), bar * 11 + beat * 3.5, pan=-0.15)

    return finalize_loop(bus, reverb_mix=0.26, low_cut=34.0, high_cut=10000.0, drive=1.1, peak=0.79,
                         air_shelf_db=-4.2, air_shelf_hz=3600.0)


# ──────────────────────────────────────────────────────────────
# BGM ③ 일반 전투 — 박진감 (140 BPM, 4/4, 16마디 = 27.4초)
# ──────────────────────────────────────────────────────────────
def build_bgm_battle() -> np.ndarray:
    bpm, beats, bars = 140.0, 4, 16
    beat = 60.0 / bpm
    bar = beat * beats
    bus = Bus(bar * bars, loop=True)

    # Em - C - G - D (4마디 × 4)
    prog = [
        (["E3", "G3", "B3"], "E2", ["E3", "G3", "B3", "E4"]),
        (["C3", "E3", "G3"], "C2", ["C3", "E3", "G3", "C4"]),
        (["G2", "B2", "D3"], "G1", ["G2", "B2", "D3", "G3"]),
        (["D3", "F#3", "A3"], "D2", ["D3", "F#3", "A3", "D4"]),
    ]

    for b in range(bars):
        notes, root, arp = prog[b % 4]
        start = b * bar

        # 8분음표 베이스 오스티나토 (루트 · 옥타브)
        for k in range(8):
            nm = root if k % 4 in (0, 3) else transpose(root, 12 if k % 2 else 0)
            amp = 0.30 if k % 4 == 0 else 0.21
            bus.add(sine_bass(hz(nm), beat * 0.46, amp=amp, punch=0.002), start + k * beat * 0.5)

        # 톱니 스타브 (오프비트 짧은 코드)
        for k in (1, 3, 5, 7):
            for i, nm in enumerate(notes):
                bus.add(saw_lead(hz(nm), beat * 0.24, amp=0.055, cutoff=2600.0, attack=0.004),
                        start + k * beat * 0.5, pan=-0.25 + 0.25 * i)

        # 드럼
        bus.add(kick(0.34, amp=0.55, f0=132.0, f1=45.0), start)
        bus.add(kick(0.32, amp=0.46, f0=132.0, f1=45.0), start + beat * 2)
        if b % 2 == 1:
            bus.add(kick(0.3, amp=0.34, f0=132.0, f1=45.0), start + beat * 3.5)
        bus.add(snare(0.24, amp=0.30, seed=60 + b % 5), start + beat)
        bus.add(snare(0.24, amp=0.32, seed=61 + b % 5), start + beat * 3)
        for k in range(8):
            bus.add(hat(0.06, amp=0.10 if k % 2 else 0.13, open_hat=(k == 7 and b % 4 == 3), seed=70 + k),
                    start + k * beat * 0.5, pan=0.3)
        if b % 8 == 0:
            bus.add(crash(1.4, amp=0.14, seed=80 + b), start, pan=-0.1)

        # 패드 (배경 두께)
        for i, nm in enumerate(notes):
            bus.add(pad_note(hz(transpose(nm, 12)), bar * 1.02, amp=0.06, attack=0.12, release=0.25),
                    start, pan=-0.3 + 0.3 * i)

        # 리드 리프 — 5~8마디 / 13~16마디
        if 4 <= b < 8:
            riff = [("E5", 0.0, 0.5), ("G5", 0.5, 0.5), ("B5", 1.0, 0.75),
                    ("A5", 2.0, 0.5), ("G5", 2.5, 0.5), ("E5", 3.0, 0.9)]
            for nm, pos, dur in riff:
                bus.add(saw_lead(hz(nm), beat * dur, amp=0.11, cutoff=4200.0), start + pos * beat, pan=0.12)
        elif 12 <= b < 16:
            riff = [("B4", 0.0, 0.5), ("D5", 0.5, 0.5), ("E5", 1.0, 0.5),
                    ("G5", 1.5, 0.75), ("F#5", 2.5, 0.5), ("E5", 3.0, 0.9)]
            for nm, pos, dur in riff:
                bus.add(saw_lead(hz(nm), beat * dur, amp=0.10, cutoff=4200.0), start + pos * beat, pan=0.12)

    # 마지막 마디 필인 — 다음 루프 첫 마디로 넘기는 드럼 롤
    fill_start = bar * 15 + beat * 3.0
    for i in range(4):
        bus.add(snare(0.16, amp=0.16 + 0.03 * i, seed=90 + i), fill_start + i * beat * 0.25, pan=0.05)

    return finalize_loop(bus, reverb_mix=0.16, low_cut=36.0, high_cut=10500.0, drive=1.35, peak=0.80,
                         air_shelf_db=-3.4, air_shelf_hz=4000.0)


# ──────────────────────────────────────────────────────────────
# BGM ④ 보스 — 웅장 · 긴박 (130 BPM, 4/4, 16마디 = 29.5초)
# ──────────────────────────────────────────────────────────────
def build_bgm_boss() -> np.ndarray:
    bpm, beats, bars = 130.0, 4, 16
    beat = 60.0 / bpm
    bar = beat * beats
    bus = Bus(bar * bars, loop=True)

    # Dm - Bb - Gm - A(하모닉 마이너의 A7) : 긴장 → 해소 → 다시 긴장
    prog = [
        (["D3", "F3", "A3", "D4"], "D2", ["D3", "F3", "A3", "D4"]),
        (["Bb2", "D3", "F3", "Bb3"], "Bb1", ["Bb2", "D3", "F3", "Bb3"]),
        (["G2", "Bb2", "D3", "G3"], "G1", ["G2", "Bb2", "D3", "G3"]),
        (["A2", "C#3", "E3", "A3"], "A1", ["A2", "C#3", "E3", "A3"]),
    ]

    for b in range(bars):
        notes, root, arp = prog[b % 4]
        start = b * bar
        intense = b >= 8                     # 후반 8마디는 더 격렬하게

        # 저음 페달 드론 + 베이스 8분음표
        bus.add(sub_drone(hz(root), bar * 1.02, amp=0.20), start)
        for k in range(8):
            amp = 0.32 if k % 4 == 0 else 0.20
            nm = root if k % 2 == 0 else transpose(root, 12)
            bus.add(sine_bass(hz(nm), beat * 0.44, amp=amp, punch=0.002), start + k * beat * 0.5, pan=0.0)

        # 16분음표 스트링 오스티나토 (긴박감)
        for k in range(16):
            nm = arp[k % len(arp)]
            amp = 0.055 if intense else 0.045
            bus.add(ks_pluck(hz(transpose(nm, 12)), beat * 0.22, amp=amp, damp=0.995, bright=0.75),
                    start + k * beat * 0.25, pan=-0.3 + 0.6 * ((k % 4) / 3.0))

        # 브라스 코드 (1·3박) + 합창 패드
        if b % 2 == 0:
            bus.add(brass([hz(n) for n in notes], bar * 0.95, amp=0.16, attack=0.06), start, pan=-0.05)
            bus.add(choir([hz(transpose(n, 12)) for n in notes[:3]], bar * 1.0, amp=0.10), start, pan=0.1)
        else:
            bus.add(brass([hz(n) for n in notes], bar * 0.5, amp=0.12, attack=0.04), start + beat * 2, pan=0.05)

        # 팀파니 + 드럼
        bus.add(timpani(hz(transpose(root, -12)), 1.0, amp=0.34), start)
        bus.add(kick(0.36, amp=0.52, f0=120.0, f1=42.0), start)
        bus.add(kick(0.34, amp=0.44, f0=120.0, f1=42.0), start + beat * 2)
        if intense:
            bus.add(kick(0.3, amp=0.32, f0=120.0, f1=42.0), start + beat * 3.5)
        bus.add(snare(0.26, amp=0.30, seed=110 + b % 6), start + beat)
        bus.add(snare(0.26, amp=0.33, seed=111 + b % 6), start + beat * 3)
        for k in range(8):
            bus.add(hat(0.05, amp=0.06, seed=120 + k), start + k * beat * 0.5, pan=0.32)
        if b % 4 == 0:
            bus.add(crash(1.8, amp=0.16, seed=130 + b), start, pan=-0.15)

        # 4마디마다 트라이톤 경고음 + 스웰
        if b % 4 == 3:
            bus.add(timpani(hz(transpose(root, 6)), 0.7, amp=0.22), start + beat * 2.5)
            bus.add(noise_swell(bar * 0.6, amp=0.07, seed=140 + b, rising=True), start + beat * 3.0, pan=0.2)
            bus.add(saw_lead(hz(transpose(root, 6)), beat * 1.2, amp=0.07, cutoff=1800.0),
                    start + beat * 2.75, pan=-0.2)

    # 후반부 클라이맥스 — 상승 멜로디 (13~16마디)
    climax = [("D5", 12.0, 1.0), ("E5", 13.0, 1.0), ("F5", 14.0, 1.0), ("A5", 15.0, 2.5)]
    for nm, pos, dur in climax:
        bus.add(brass([hz(nm)], beat * dur, amp=0.10, attack=0.05), pos * beat, pan=0.18)

    return finalize_loop(bus, reverb_mix=0.34, low_cut=30.0, high_cut=10000.0, drive=1.45, peak=0.80,
                         air_shelf_db=-3.8, air_shelf_hz=3800.0)


# ──────────────────────────────────────────────────────────────
# SFX
# ──────────────────────────────────────────────────────────────
def sfx_click() -> np.ndarray:
    """UI 버튼·탭 — 따뜻한 중저음 우드 탭 (2026-11-19 재제작).

    사용자 요청: "메뉴 변경/클릭 시 효과음이 너무 하이톤이라 신경 쓰인다".
    예전 소리는 1.18kHz·2.38kHz 사인에 고역 노이즈를 얹어 귀를 찌르는 유리 클릭이었다
    (5kHz 위 에너지 3.0% · 최고 피크 0.96).

    이제는 **속이 빈 목재를 톡 두드린 소리**다:
      · 195→170Hz 우드 몸통 + 430Hz 2배음(아주 짧게) — 따뜻한 중저음 중심
      · 700~2.6kHz 대역의 부드러운 타격(나무 표면의 "톡") — 11ms 만에 사라짐
      · 5.2kHz 저역통과 + 2.6kHz 위 셀프 -2.5dB — 하이톤 자극 제거
      · 어택 1.5ms · 전체 80ms · 피크 0.55 (다른 SFX 보다 한 단계 작게)

    휴대폰 스피커에서 들리도록 중역 0.5~5kHz 성분은 남겼다(5kHz 위는 1% 미만).
    """
    dur = 0.08
    n = int(dur * SR)
    t = t_axis(n)
    rng = np.random.default_rng(11)

    # ① 우드 몸통 — 살짝 떨어지는 피치(195 → 170Hz)
    pitch = 195.0 - 25.0 * (t / dur)
    body = np.sin(2 * np.pi * np.cumsum(pitch) / SR) * np.exp(-t * 85.0)

    # ② 2배음(430Hz) — 아주 짧게, 몸통에 따뜻한 색을 더한다
    body += 0.22 * np.sin(2 * np.pi * 430.0 * t) * np.exp(-t * 130.0)

    # ③ 나무 표면 타격 — 중역 노이즈 버스트 (11ms)
    tick = fft_filter(rng.uniform(-1.0, 1.0, n), "band", 700.0, order=1, cutoff2=2600.0)
    tick = tick * np.exp(-t * 95.0) * 2.2

    sig = body + tick

    # ④ 고역 정리 — 5.2kHz 위는 저역통과로 걷어내고 2.6kHz 위는 -2.5dB
    sig = fft_filter(sig, "low", 5200.0, order=2)
    sig = high_shelf(sig, freq=2600.0, gain_db=-2.5, span=2000.0)

    # ⑤ 부드러운 어택(1.5ms) · 끝단 클릭 방지 페이드(5ms)
    sig *= (1.0 - np.exp(-t * 620.0))
    k = int(0.005 * SR)
    sig[-k:] *= np.linspace(1, 0, k)

    # ⑥ 왼쪽을 살짝 먼저 — 나무가 손끝에서 울리는 느낌
    right = np.concatenate([np.zeros(12), sig[: -12]]) * 0.94
    return normalize(np.stack([sig, right]), 0.55)


def sfx_attack() -> np.ndarray:
    """일반 공격 타격 — 휘두르는 공기 + 짧은 타격"""
    n = int(0.26 * SR)
    t = t_axis(n)
    noise = np.random.default_rng(2).uniform(-1, 1, n)
    swoosh = fft_filter(noise, "band", 700.0, order=1, cutoff2=5200.0) * np.exp(-t * 16.0)
    thud = np.sin(2 * np.pi * np.cumsum(160 * np.exp(-t * 18.0) + 60) / SR) * np.exp(-t * 26.0)
    sig = 0.85 * swoosh + 0.75 * thud
    sig = soft_clip(sig, 1.4)
    stereo = np.stack([sig, np.roll(sig, 24) * 0.92])
    return normalize(stereo, 0.85)


def sfx_skill() -> np.ndarray:
    """스킬 · 오의 발동 — 상승하는 마법 차임 + 반짝임"""
    dur = 0.95
    n = int(dur * SR)
    out = np.zeros(n)
    notes = ["A4", "C#5", "E5", "A5", "C#6"]
    for i, nm in enumerate(notes):
        start = int(i * 0.075 * SR)
        seg = fm_bell(hz(nm), dur - i * 0.075, amp=0.40, ratio=2.0, index=3.4, decay=2.6)
        out[start:start + min(seg.size, n - start)] += seg[: n - start]
    # 상승 스웰 (벨을 덮지 않을 만큼만)
    t = t_axis(n)
    noise = fft_filter(np.random.default_rng(4).uniform(-1, 1, n), "band", 1400.0, order=1, cutoff2=9000.0)
    out += 0.12 * noise * (t / dur) ** 2.0
    out = soft_clip(out, 1.3)
    left = out
    right = np.concatenate([np.zeros(90), out[: -90]]) * 0.95
    return normalize(np.stack([left, right]), 0.85)


def sfx_hit() -> np.ndarray:
    """피격 — 짧고 묵직한 타격음"""
    n = int(0.2 * SR)
    t = t_axis(n)
    noise = np.random.default_rng(6).uniform(-1, 1, n)
    snap = fft_filter(noise, "band", 300.0, order=1, cutoff2=3800.0) * np.exp(-t * 38.0)
    body = np.sin(2 * np.pi * np.cumsum(220 * np.exp(-t * 26.0) + 70) / SR) * np.exp(-t * 20.0)
    sig = soft_clip(1.1 * snap + 0.8 * body, 1.5)
    return normalize(sig, 0.8)[None, :].repeat(2, axis=0)


def sfx_summon() -> np.ndarray:
    """현실 소환 / 영입 — 신비로운 상승 아르페지오 + 파동"""
    dur = 1.35
    n = int(dur * SR)
    out = np.zeros(n)
    t = t_axis(n)
    notes = ["D4", "F4", "A4", "D5", "F5", "A5"]
    for i, nm in enumerate(notes):
        start = int(i * 0.085 * SR)
        seg = fm_bell(hz(nm), dur - i * 0.085, amp=0.30, ratio=3.0, index=2.6, decay=2.2)
        out[start:start + min(seg.size, n - start)] += seg[: n - start]
    # 게이트가 열리는 파동 (저역 스웰)
    swell = np.sin(2 * np.pi * np.cumsum(90 * np.exp(-t * 1.6) + 55) / SR) * np.exp(-t * 2.2)
    out += 0.30 * swell * (1 - np.exp(-t * 8.0))
    out += 0.07 * fft_filter(np.random.default_rng(8).uniform(-1, 1, n), "high", 6000.0) * np.exp(-t * 4.5)
    out = soft_clip(out, 1.25)
    left = out
    right = np.concatenate([np.zeros(140), out[: -140]]) * 0.94
    return normalize(np.stack([left, right]), 0.86)


def sfx_pack_open() -> np.ndarray:
    """카드팩 개봉 팡파르 — 봉인 뜯는 소리 → 밝은 팡파레"""
    dur = 1.2
    n = int(dur * SR)
    out = np.zeros(n)
    t = t_axis(n)
    # 봉인 뜯기 (짧은 지퍼 노이즈 2연타)
    for i, off in enumerate((0.0, 0.075)):
        m = int(0.09 * SR)
        tn = t_axis(m)
        rip = fft_filter(np.random.default_rng(20 + i).uniform(-1, 1, m), "band", 2200.0, order=1, cutoff2=11000.0)
        rip *= np.sin(np.pi * tn / (0.09)) ** 1.5
        s = int(off * SR)
        out[s:s + m] += 0.45 * rip[: n - s]
    # 팡파르 — C 장조 상행 4음 + 마지막 화음
    fan = [("C5", 0.14, 0.16), ("E5", 0.28, 0.16), ("G5", 0.42, 0.16), ("C6", 0.56, 0.45)]
    for nm, pos, length in fan:
        seg = brass([hz(nm)], length, amp=0.30, attack=0.006)
        s = int(pos * SR)
        out[s:s + min(seg.size, n - s)] += seg[: n - s]
        seg2 = glock(hz(nm), 0.5, amp=0.14)
        out[s:s + min(seg2.size, n - s)] += seg2[: n - s]
    # 반짝임
    for i, nm in enumerate(("C6", "E6", "G6", "C7")):
        s = int((0.6 + i * 0.06) * SR)
        seg = glock(hz(nm), 0.45, amp=0.09)
        out[s:s + min(seg.size, n - s)] += seg[: n - s]
    out = soft_clip(out, 1.2)
    left = out
    right = np.concatenate([np.zeros(60), out[: -60]]) * 0.95
    return normalize(np.stack([left, right]), 0.9)


def sfx_upgrade() -> np.ndarray:
    """강화 · 연성 · 장착 성공 — 금속 '팅' + 상승 2음"""
    dur = 0.7
    n = int(dur * SR)
    out = np.zeros(n)
    t = t_axis(n)
    # 금속 타격 (비화성 배음)
    for ratio, g, dec in ((1.0, 1.0, 6.0), (2.02, 0.55, 8.0), (3.03, 0.3, 11.0), (4.98, 0.18, 14.0)):
        out += g * np.sin(2 * np.pi * 880 * ratio * t) * np.exp(-t * dec)
    out *= (1 - np.exp(-t * 3000.0))
    # 상승 2음 (E5 → B5)
    for i, nm in enumerate(("E5", "B5")):
        s = int((0.10 + i * 0.10) * SR)
        seg = glock(hz(nm), 0.42, amp=0.24)
        out[s:s + min(seg.size, n - s)] += seg[: n - s]
    out = soft_clip(out, 1.25)
    return normalize(out, 0.85)[None, :].repeat(2, axis=0)


def sfx_victory() -> np.ndarray:
    """전투 승리 — 장조 팡파르 (브라스 3화음 + 심벌)"""
    dur = 1.9
    n = int(dur * SR)
    out = np.zeros(n)
    # 상행 모티프 C-E-G-C
    for i, (nm, pos) in enumerate((("C5", 0.0), ("E5", 0.13), ("G5", 0.26), ("C6", 0.40))):
        seg = brass([hz(nm)], 0.30 if i < 3 else 0.9, amp=0.30, attack=0.008)
        s = int(pos * SR)
        out[s:s + min(seg.size, n - s)] += seg[: n - s]
    # 마지막 화음 (C E G C)
    chord = [hz(n) for n in ("C4", "E4", "G4", "C5", "E5")]
    seg = brass(chord, 1.35, amp=0.20, attack=0.02)
    s = int(0.40 * SR)
    out[s:s + min(seg.size, n - s)] += seg[: n - s]
    # 심벌 + 반짝임
    cr = crash(1.6, amp=0.16, seed=77)
    out[0:min(cr.size, n)] += cr[: n]
    for i, nm in enumerate(("G6", "C7", "E7")):
        s = int((0.45 + i * 0.08) * SR)
        g = glock(hz(nm), 0.6, amp=0.08)
        out[s:s + min(g.size, n - s)] += g[: n - s]
    out = soft_clip(out, 1.3)
    left = out
    right = np.concatenate([np.zeros(80), out[: -80]]) * 0.96
    return normalize(np.stack([left, right]), 0.9)


def sfx_defeat() -> np.ndarray:
    """전투 패배 — 하강 단조 모티프 + 무거운 저음"""
    dur = 2.0
    n = int(dur * SR)
    out = np.zeros(n)
    t = t_axis(n)
    # 하강 모티프 A4 - F4 - D4 - D3
    for i, (nm, pos) in enumerate((("A4", 0.0), ("F4", 0.26), ("D4", 0.52), ("D3", 0.80))):
        seg = brass([hz(nm)], 0.55 if i < 3 else 1.2, amp=0.24, attack=0.03)
        s = int(pos * SR)
        out[s:s + min(seg.size, n - s)] += seg[: n - s]
    # 낮은 신음 패드
    groan = np.sin(2 * np.pi * 73.4 * t) * np.exp(-t * 1.1) * 0.5
    groan += np.sin(2 * np.pi * 110.0 * t) * np.exp(-t * 1.6) * 0.25
    out += groan * (1 - np.exp(-t * 3.0))
    # 무너지는 노이즈
    noise = fft_filter(np.random.default_rng(9).uniform(-1, 1, n), "low", 900.0, order=2)
    out += 0.18 * noise * np.exp(-t * 1.4)
    out = soft_clip(out, 1.1)
    return normalize(out, 0.82)[None, :].repeat(2, axis=0)


# ──────────────────────────────────────────────────────────────
# 인코딩
# ──────────────────────────────────────────────────────────────
def _find_ffmpeg() -> str | None:
    exe = shutil.which("ffmpeg")
    if exe:
        return exe
    try:
        import imageio_ffmpeg  # type: ignore

        return imageio_ffmpeg.get_ffmpeg_exe()
    except Exception:
        return None


def write_audio(stereo: np.ndarray, path_noext: str, quality: int = 5) -> tuple[str, int]:
    """
    stereo: (2, n) float -1~1 → OGG Vorbis.
    ffmpeg(libvorbis) → libsndfile Vorbis → WAV 순으로 폴백.
    반환: (실제 경로, 바이트)
    """
    os.makedirs(TMP_DIR, exist_ok=True)
    os.makedirs(os.path.dirname(path_noext), exist_ok=True)
    data = np.ascontiguousarray(stereo.T.astype(np.float32))
    data = np.clip(data, -1.0, 1.0)
    wav_path = os.path.join(TMP_DIR, os.path.basename(path_noext) + ".wav")
    with wave.open(wav_path, "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((data * 32767.0).astype("<i2").tobytes())

    ffmpeg = _find_ffmpeg()
    if ffmpeg:
        out = path_noext + ".ogg"
        cmd = [ffmpeg, "-y", "-hide_banner", "-loglevel", "error", "-i", wav_path,
               "-c:a", "libvorbis", "-q:a", str(quality), "-ar", str(SR), out]
        res = subprocess.run(cmd, capture_output=True)
        if res.returncode == 0 and os.path.exists(out) and os.path.getsize(out) > 0:
            return out, os.path.getsize(out)

    try:
        import soundfile as sf  # type: ignore

        out = path_noext + ".ogg"
        sf.write(out, data, SR, format="OGG", subtype="VORBIS")
        return out, os.path.getsize(out)
    except Exception as exc:  # pragma: no cover
        print(f"  ! OGG 인코딩 실패({exc}) — WAV 로 저장합니다")
        out = path_noext + ".wav"
        shutil.copyfile(wav_path, out)
        return out, os.path.getsize(out)


# ──────────────────────────────────────────────────────────────
# 검증
# ──────────────────────────────────────────────────────────────
def loop_seam_report(stereo: np.ndarray) -> dict:
    """루프 이음매 연속성 — 마지막 샘플과 첫 샘플의 불연속 크기"""
    if stereo.shape[1] < 100:
        return {}
    jump = float(np.max(np.abs(stereo[:, 0] - stereo[:, -1])))
    typical = float(np.mean(np.abs(np.diff(stereo, axis=1))))
    local = float(np.sqrt(np.mean(stereo[:, :2048] ** 2)))
    return {
        "loop_seam_jump": round(jump, 5),
        "mean_step": round(typical, 5),
        "seam_vs_local_rms": round(jump / (local + 1e-9), 4),
        "peak": round(float(np.max(np.abs(stereo))), 4),
        "rms_db": round(20 * math.log10(float(np.sqrt(np.mean(stereo ** 2))) + 1e-12), 2),
    }


def decode_file(path: str) -> np.ndarray | None:
    """저장된 파일을 다시 읽어 검증용 스테레오 배열로 돌려준다"""
    try:
        import soundfile as sf  # type: ignore

        data, _sr = sf.read(path, always_2d=True)
        return data.T
    except Exception:
        return None


BGMS = {
    "bgm_intro": build_bgm_intro,
    "bgm_lobby": build_bgm_lobby,
    "bgm_battle": build_bgm_battle,
    "bgm_boss": build_bgm_boss,
}

SFXS = {
    "sfx_click": sfx_click,
    "sfx_attack": sfx_attack,
    "sfx_skill": sfx_skill,
    "sfx_hit": sfx_hit,
    "sfx_summon": sfx_summon,
    "sfx_pack_open": sfx_pack_open,
    "sfx_upgrade": sfx_upgrade,
    "sfx_victory": sfx_victory,
    "sfx_defeat": sfx_defeat,
}


def main() -> int:
    ap = argparse.ArgumentParser(description="듀얼 게이트 BGM·SFX 생성기")
    ap.add_argument("--only", choices=["bgm", "sfx"], help="BGM 또는 SFX만 생성")
    ap.add_argument("--check", action="store_true", help="생성 없이 기존 파일만 검증")
    ap.add_argument("--out", default=RAW_DIR, help="출력 디렉터리 (기본 res/raw)")
    args = ap.parse_args()

    manifest = []
    if args.check:
        for name in list(BGMS) + list(SFXS):
            p = os.path.join(args.out, name + ".ogg")
            if not os.path.exists(p):
                p = os.path.join(args.out, name + ".wav")
            if not os.path.exists(p):
                print(f"MISSING {name}")
                continue
            manifest.append({"name": name, "file": os.path.basename(p), "bytes": os.path.getsize(p)})
        print(json.dumps(manifest, indent=2, ensure_ascii=False))
        return 0

    todo = []
    if args.only in (None, "bgm"):
        todo += [(n, f) for n, f in BGMS.items()]
    if args.only in (None, "sfx"):
        todo += [(n, f) for n, f in SFXS.items()]

    total_bytes = 0
    for name, builder in todo:
        print(f"[생성] {name} ...", flush=True)
        stereo = builder()
        stereo = np.asarray(stereo, dtype=np.float64)
        if stereo.ndim == 1:
            stereo = np.stack([stereo, stereo])
        path, size = write_audio(stereo, os.path.join(args.out, name), quality=5)
        total_bytes += size
        info = {
            "name": name,
            "file": os.path.basename(path),
            "seconds": round(stereo.shape[1] / SR, 2),
            "kbytes": round(size / 1024.0, 1),
            "kbps": round(size * 8 / (stereo.shape[1] / SR) / 1000.0, 1),
        }
        decoded = decode_file(path)
        probe = decoded if decoded is not None else stereo
        info["verified_from_file"] = decoded is not None
        if name in BGMS:
            info.update(loop_seam_report(probe))
        else:
            info["peak"] = round(float(np.max(np.abs(probe))), 4)
            info["seconds"] = round(probe.shape[1] / SR, 2)
        manifest.append(info)
        print("   " + json.dumps(info, ensure_ascii=False), flush=True)

    print(f"\n합계 {total_bytes / 1024.0 / 1024.0:.2f} MB · 파일 {len(manifest)}개")
    manifest_path = os.path.join(REPO, "scripts", "audio_manifest.json")
    with open(manifest_path, "w", encoding="utf-8") as fh:
        json.dump(manifest, fh, ensure_ascii=False, indent=2)
    print(f"매니페스트: {manifest_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
