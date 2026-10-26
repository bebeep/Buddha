# -*- coding: utf-8 -*-
"""
佛像视频简介合成脚本（可复用给其他佛像）

1. 旁白按分镜拆段，用 edge-tts 逐段生成，方便和画面精确对位
2. 镜头素材：delogo 抹掉角落的「AI生成」水印（比裁边干净、不掉画质），统一 1280x720/30fps
3. 静图：竖图用「模糊放大背景 + 等比缩放主体」合成 16:9 底图，避免被裁成只剩躯干；
   再用 zoompan 做缓慢推拉（x/y 取整，否则亚像素抖动会让码率爆炸）
4. 按旁白时长决定每幕时长，逐幕编码后 concat
5. 旁白按幕起点延迟混音，片头淡入、片尾淡出 + 结束字幕

用法：改 SUBTITLES / SCENES 后 python 合成脚本.py
"""
import json
import os
import re
import shutil
import subprocess
import sys

import imageio_ffmpeg
from PIL import Image, ImageFilter

FF = imageio_ffmpeg.get_ffmpeg_exe()
HERE = os.path.dirname(os.path.abspath(__file__))      # 佛像/_build
BASE = os.path.dirname(HERE)                            # 佛像/
SRC = os.path.join(BASE, "释迦牟尼")                     # 佛像/释迦牟尼
WORK = os.path.join(HERE, "build")
W, H, FPS = 1280, 720, 30

SUBTITLES = [
    "两千五百多年前，一位太子走出了王宫。",
    "他在菩提树下静坐七日，夜睹明星，豁然大悟。",
    "此后四十五年，他不曾停下脚步，只为把一条离苦的路，讲给众生听。",
    "他就是本师，释迦牟尼佛。",
    "今天，我们合掌的时候，念的不只是他的名字，也是他心里那份不忍众生受苦的愿。",
]

# type=video 用镜头素材（water=True 表示左上角有水印要抹）；type=still 用静图做缓推
SCENES = [
    {"type": "video", "src": "视频/镜头2_太子出宫.mp4", "water": True},
    {"type": "video", "src": "视频/镜头1_菩提树下.mp4", "water": True},
    {"type": "video", "src": "视频/镜头3_香炉青烟.mp4", "water": True},
    {"type": "still", "src": "图片/释迦牟尼_立像主图.png", "zoom_in": True},
    {"type": "still", "src": "图片/释迦牟尼_详情封面.png", "zoom_in": False},
]

END_TITLE = "本师释迦牟尼佛"
VOICE = "zh-CN-YunyangNeural"
RATE = "-12%"
GAP_BEFORE = 0.45


def run(args, cwd=None):
    r = subprocess.run(args, capture_output=True, text=True, encoding="utf-8", errors="ignore", cwd=cwd)
    if r.returncode != 0:
        print("FFMPEG FAILED:", " ".join(args)[:400])
        print((r.stderr or "")[-1500:])
        sys.exit(1)
    return r


def duration(path):
    r = subprocess.run([FF, "-hide_banner", "-i", path], capture_output=True, text=True,
                       encoding="utf-8", errors="ignore")
    m = re.search(r"Duration:\s*(\d+):(\d+):([\d.]+)", r.stderr or "")
    if not m:
        raise RuntimeError("读不到时长: " + path)
    return int(m.group(1)) * 3600 + int(m.group(2)) * 60 + float(m.group(3))


def tts():
    import asyncio
    import edge_tts
    outs = []
    for i, text in enumerate(SUBTITLES, 1):
        out = os.path.join(WORK, "vo_%d.mp3" % i)
        outs.append(out)
        if os.path.exists(out) and os.path.getsize(out) > 0:
            continue

        async def gen(t=text, o=out):
            await edge_tts.Communicate(t, VOICE, rate=RATE).save(o)

        asyncio.run(gen())
    return outs


def prepare_still(src, tag):
    """把任意比例的原图铺成 16:9 底图：竖图用模糊背景 + 等比主体，横图直接居中裁切。"""
    im = Image.open(src).convert("RGB")
    iw, ih = im.size
    canvas_w, canvas_h = W * 2, H * 2          # 先做 2 倍图，缩放时更干净
    target_ratio = canvas_w / canvas_h
    ratio = iw / ih

    if abs(ratio - target_ratio) < 0.01:
        base = im.resize((canvas_w, canvas_h), Image.LANCZOS)
    elif ratio > target_ratio:                  # 太宽：上下裁
        new_h = int(iw / target_ratio)
        top = (ih - new_h) // 2
        base = im.crop((0, top, iw, top + new_h)).resize((canvas_w, canvas_h), Image.LANCZOS)
    else:                                       # 太窄（竖图）：模糊背景 + 主体等比放进画面
        bg = im.resize((canvas_w, canvas_h), Image.LANCZOS).filter(ImageFilter.GaussianBlur(48))
        bg = Image.blend(bg, Image.new("RGB", bg.size, (10, 12, 16)), 0.45)
        fg = im.resize((int(canvas_h * ratio), canvas_h), Image.LANCZOS)
        bg.paste(fg, ((canvas_w - fg.width) // 2, 0))
        base = bg

    out = os.path.join(WORK, "still_%s.png" % tag)
    base.save(out)
    return out


def build_scene(idx, scene, dur):
    out = os.path.join(WORK, "scene_%d.mp4" % idx)

    if scene["type"] == "video":
        src = os.path.join(SRC, scene["src"])
        vf = []
        if scene.get("water"):
            # 水印固定在左上角，delogo 用周边像素插值补掉，比裁边少损失画质
            vf.append("delogo=x=2:y=2:w=150:h=60")
        vf.append("scale=%d:%d:force_original_aspect_ratio=increase" % (W, H))
        vf.append("crop=%d:%d" % (W, H))
        vf.append("fps=%d" % FPS)
        raw = duration(src)
        if raw < dur:
            vf.append("setpts=(PTS-STARTPTS)*%.4f" % min(dur / raw, 1.45))
        args = [FF, "-y", "-i", src, "-vf", ",".join(vf), "-an", "-t", "%.2f" % dur]
        crf = "20"
    else:
        src = prepare_still(os.path.join(SRC, scene["src"]), idx)
        frames = int(dur * FPS)
        z_from, z_to = (1.0, 1.06) if scene.get("zoom_in") else (1.06, 1.0)
        step = (z_to - z_from) / max(frames, 1)
        vf = ("zoompan=z='%.6f+%.8f*on':"
              "x='trunc(iw/2-(iw/zoom/2))':y='trunc(ih/2-(ih/zoom/2))':"
              "d=%d:s=%dx%d:fps=%d" % (z_from, step, frames, W, H, FPS))
        args = [FF, "-y", "-loop", "1", "-t", "%.2f" % dur, "-i", src, "-vf", vf, "-an"]
        crf = "22"

    args += ["-c:v", "libx264", "-crf", crf, "-preset", "medium",
             "-pix_fmt", "yuv420p", "-r", str(FPS), out]
    run(args)
    return out


def pick_font():
    for name in ("simhei.ttf", "msyhbd.ttc", "msyh.ttc"):
        src = "C:/Windows/Fonts/" + name
        if os.path.exists(src):
            dst = os.path.join(WORK, name)
            if not os.path.exists(dst):
                shutil.copy(src, dst)
            return name
    return None


def main():
    os.makedirs(WORK, exist_ok=True)

    vo = tts()
    vo_dur = [duration(p) for p in vo]
    print("旁白时长:", ["%.2f" % d for d in vo_dur])

    durs = []
    for d, scene in zip(vo_dur, SCENES):
        extra = 1.9 if scene["type"] == "still" else 1.5
        durs.append(max(d + GAP_BEFORE + extra, 5.0))
    print("分幕时长:", ["%.2f" % d for d in durs])

    scenes = [build_scene(i + 1, s, d) for i, (s, d) in enumerate(zip(SCENES, durs))]

    list_file = os.path.join(WORK, "concat.txt")
    with open(list_file, "w", encoding="utf-8") as f:
        for p in scenes:
            f.write("file '%s'\n" % p.replace("\\", "/"))
    merged = os.path.join(WORK, "merged.mp4")
    run([FF, "-y", "-f", "concat", "-safe", "0", "-i", list_file, "-c", "copy", merged])

    total = sum(durs)
    inputs, filters, labels = [], [], []
    start = 0.0
    for i, (p, d) in enumerate(zip(vo, durs)):
        inputs += ["-i", p]
        delay = int(round((start + GAP_BEFORE) * 1000))
        filters.append("[%d:a]adelay=%d|%d,volume=1.15[a%d]" % (i + 1, delay, delay, i))
        labels.append("[a%d]" % i)
        start += d
    mix = "".join(labels) + "amix=inputs=%d:normalize=0:dropout_transition=0," % len(vo)
    mix += "apad=whole_dur=%.2f,atrim=0:%.2f[aout]" % (total, total)

    vf_chain = ["fade=t=in:st=0:d=0.9", "fade=t=out:st=%.2f:d=1.2" % (total - 1.2)]
    font = pick_font()
    if font:
        # 中文不能直接写进 filter 字符串：Windows 下 argv 会被转码，drawtext 收到的
        # 是一堆「?」，渲染出来就是方框。必须走 textfile 读 UTF-8 文件。
        title_file = os.path.join(WORK, "title.txt")
        with open(title_file, "w", encoding="utf-8") as f:
            f.write(END_TITLE)
        end_start = total - 4.0
        vf_chain.append(
            "drawtext=fontfile=%s:textfile=title.txt:fontcolor=white:fontsize=50:"
            "x=(w-text_w)/2:y=h-140:alpha='if(lt(t,%.2f),0,min((t-%.2f)/1.2,1))'"
            % (font, end_start, end_start))

    out = os.path.join(SRC, "视频", "释迦牟尼佛_视频简介.mp4")
    run([FF, "-y", "-i", merged] + inputs + [
        "-filter_complex",
        ";".join(filters) + ";[0:v]" + ",".join(vf_chain) + "[vout];" + mix,
        "-map", "[vout]", "-map", "[aout]",
        "-c:v", "libx264", "-crf", "21", "-preset", "medium", "-pix_fmt", "yuv420p",
        "-c:a", "aac", "-b:a", "128k", "-movflags", "+faststart",
        "-t", "%.2f" % total, out], cwd=WORK)

    # 中间产物清掉，只留配音和脚本
    for p in scenes + [merged]:
        try:
            os.remove(p)
        except OSError:
            pass

    print(json.dumps({"output": out, "duration": round(duration(out), 2),
                      "scenes": [round(d, 2) for d in durs],
                      "size_mb": round(os.path.getsize(out) / 1048576, 2)},
                     ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
