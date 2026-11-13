#!/usr/bin/env python3
"""Capture real phone/watch emulator screens and compose a README GIF.

uv run --with pillow python scripts/capture-demo.py capture --seconds 180
# In another terminal, write one stage name into build/demo/stage.txt while operating the apps.
uv run --with pillow python scripts/capture-demo.py render
Only emulator-5554 and emulator-5556 are addressed; no physical device is touched.
"""
from __future__ import annotations

import argparse
import io
import json
import os
from pathlib import Path
import subprocess
import time
from concurrent.futures import ThreadPoolExecutor
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'build/demo'
ADB = Path(os.environ.get('ANDROID_HOME', str(Path.home() / 'Library/Android/sdk'))) / 'platform-tools/adb'
STAGES = {
    'idle': ('A thought starts on your wrist', 2, 'last'),
    'record': ('Tap to record. Tap again to stop.', 4, 'last'),
    'queue': ('Phone away? Your recordings wait on the watch.', 3, 'last'),
    'transfer': ('Reconnect: transfer and transcribe on device.', 6, 'first'),
    'actions': ('Copy the text, ask in chat, or save a note.', 4, 'last'),
    'saved': ('Audio + transcript, saved together in Jogakbo.', 4, 'last'),
}


def font(size: int):
    for path in ['/System/Library/Fonts/Supplemental/Arial.ttf', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf']:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default(size=size)


def screenshot(serial: str):
    return Image.open(io.BytesIO(subprocess.check_output([str(ADB), '-s', serial, 'exec-out', 'screencap', '-p']))).convert('RGB')


def centered(draw, x, y, text, face, fill):
    box = draw.textbbox((0, 0), text, font=face)
    draw.text((x - (box[2] - box[0]) / 2, y), text, font=face, fill=fill)


def compose(phone, watch, stage):
    image = Image.new('RGB', (840, 900), '#F3F6F7')
    draw = ImageDraw.Draw(image)
    logo = Image.open(ROOT / 'docs/images/dorangori-icon.png').convert('RGBA').resize((64, 64), Image.Resampling.LANCZOS)
    image.paste(logo, (32, 25), logo)
    draw.text((112, 26), 'Dorangori', font=font(34), fill='#172F38')
    draw.text((113, 68), 'From your wrist to on-device AI', font=font(17), fill='#4D676E')
    centered(draw, 210, 168, 'WEAR OS WATCH', font(17), '#365962')
    centered(draw, 619, 112, 'ANDROID PHONE', font(17), '#365962')
    draw.ellipse((65, 270, 355, 560), fill='#172F38')
    watch = watch.resize((266, 266), Image.Resampling.LANCZOS)
    mask = Image.new('L', (266, 266)); ImageDraw.Draw(mask).ellipse((0, 0, 265, 265), fill=255)
    image.paste(watch, (77, 282), mask)
    draw.rounded_rectangle((463, 143, 775, 807), radius=25, fill='#172F38')
    phone = phone.resize((288, 640), Image.Resampling.LANCZOS)
    mask = Image.new('L', (288, 640)); ImageDraw.Draw(mask).rounded_rectangle((0, 0, 287, 639), radius=14, fill=255)
    image.paste(phone, (475, 155), mask)
    draw.line((378, 415, 440, 415), fill='#329E90', width=5)
    draw.polygon([(441, 415), (426, 405), (426, 425)], fill='#329E90')
    centered(draw, 210, 600, 'Record. Queue. Send.', font(19), '#365962')
    centered(draw, 210, 632, 'Your phone does the AI.', font(17), '#61777E')
    caption = STAGES.get(stage, STAGES['idle'])[0]
    centered(draw, 420, 828, caption, font(24), '#172F38')
    centered(draw, 420, 875, 'Emulator screens  /  prerecorded speech sample  /  on-device ASR', font(13), '#61777E')
    return image


def capture(seconds: int):
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / 'frames').mkdir(exist_ok=True)
    for serial in ['emulator-5554', 'emulator-5556']:
        qemu = subprocess.check_output([str(ADB), '-s', serial, 'shell', 'getprop', 'ro.kernel.qemu'], text=True).strip()
        if qemu != '1':
            raise SystemExit(f'Refusing non-emulator target: {serial}')
    deadline = time.monotonic() + seconds
    with ThreadPoolExecutor(max_workers=2) as pool, (OUT / 'frames.jsonl').open('w') as log:
        i = 0
        while time.monotonic() < deadline and not (OUT / 'stop').exists():
            start = time.monotonic()
            stage = (OUT / 'stage.txt').read_text().strip() if (OUT / 'stage.txt').exists() else 'idle'
            p = pool.submit(screenshot, 'emulator-5554'); w = pool.submit(screenshot, 'emulator-5556')
            frame = compose(p.result(), w.result(), stage)
            path = OUT / f'frames/{i:04d}.png'; frame.save(path)
            log.write(json.dumps({'file': str(path), 'stage': stage, 't': time.time()}) + '\n'); log.flush()
            i += 1
            time.sleep(max(0, .5 - (time.monotonic() - start)))
    print(f'Captured {i} simultaneous screen pairs in {OUT}')


def render():
    rows = [json.loads(s) for s in (OUT / 'frames.jsonl').read_text().splitlines()]
    selected = []
    for stage, (_, seconds, direction) in STAGES.items():
        group = [r for r in rows if r['stage'] == stage]
        if not group:
            continue
        # Keep real consecutive frames. Trim time spent waiting between manual actions.
        window = group[:int(seconds * 2)] if direction == 'first' else group[-int(seconds * 2):]
        selected += window
    if not selected:
        raise SystemExit('No captured stages')
    frames = [Image.open(r['file']).convert('RGB') for r in selected]
    # A shared palette preserves text without per-frame colour flicker.
    sheet = Image.new('RGB', (840, 900 * len(frames)))
    for i, frame in enumerate(frames): sheet.paste(frame, (0, i * 900))
    palette = sheet.quantize(colors=160, method=Image.Quantize.MEDIANCUT)
    frames = [f.quantize(palette=palette, dither=Image.Dither.NONE) for f in frames]
    target = ROOT / 'docs/images/demo.gif'
    frames[0].save(target, save_all=True, append_images=frames[1:], duration=500, loop=0, optimize=True, disposal=1)
    # A small contact sheet is useful for checking each part of the story.
    thumbs = [Image.open(r['file']).resize((280, 300)) for r in selected[::max(1, len(selected) // 6)]]
    contact = Image.new('RGB', (280 * len(thumbs), 300), 'white')
    for i, f in enumerate(thumbs): contact.paste(f, (280 * i, 0))
    contact.save(OUT / 'contact-sheet.png')
    print(f'{target}: {len(frames)} frames, {len(frames) / 2:.1f}s, {target.stat().st_size / 1024**2:.2f} MiB')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=['capture', 'render'])
    parser.add_argument('--seconds', type=int, default=180)
    args = parser.parse_args()
    if args.mode == 'capture': capture(args.seconds)
    else: render()
