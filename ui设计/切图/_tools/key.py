import os
try:
    import numpy as np
    HAVE_NP = True
except Exception:
    HAVE_NP = False

from PIL import Image

BASE = r"E:\Android\Buddha\ui设计\切图"
BGCOL = (240, 240, 238)   # 近白背景色
TH, THU = 52, 72          # 硬透明阈值 / 软边上限

def key_image(path):
    im = Image.open(path).convert("RGBA")
    if HAVE_NP:
        arr = np.array(im).astype(int)
        r, g, b = arr[:, :, 0], arr[:, :, 1], arr[:, :, 2]
        d2 = (r - BGCOL[0]) ** 2 + (g - BGCOL[1]) ** 2 + (b - BGCOL[2]) ** 2
        th2, thu2 = TH * TH, THU * THU
        newa = np.where(d2 < th2, 0,
                np.where(d2 < thu2, ((d2 - th2) / (thu2 - th2) * 255).astype(int), 255))
        arr[:, :, 3] = newa.astype(np.uint8)
        # 透明像素的 RGB 置 0，避免合成时残留灰边
        mask = arr[:, :, 3] == 0
        arr[mask, 0] = arr[mask, 1] = arr[mask, 2] = 0
        out = Image.fromarray(arr.astype(np.uint8), "RGBA")
    else:
        px = im.load(); W, H = im.size
        th2, thu2 = TH * TH, THU * THU
        for y in range(H):
            for x in range(W):
                rr, gg, bb, aa = px[x, y]
                d2 = (rr - BGCOL[0]) ** 2 + (gg - BGCOL[1]) ** 2 + (bb - BGCOL[2]) ** 2
                if d2 < th2:
                    na = 0
                elif d2 < thu2:
                    na = int(255 * (d2 - th2) / (thu2 - th2))
                else:
                    na = 255
                # 吃掉浅色半透明残边（白底抠图灰边）
                if 0 < na < 200:
                    lum = 0.299 * rr + 0.587 * gg + 0.114 * bb
                    if lum > 180:
                        na = 0
                px[x, y] = (rr, gg, bb, na)
        out = im
    out.save(path)
    print("keyed", os.path.basename(path), "numpy=" + str(HAVE_NP))

for f in ["许愿树.png", "许愿牌.png"]:
    key_image(os.path.join(BASE, f))

# 重新合成预览（背景不透明 + 树透明）
bg = Image.open(os.path.join(BASE, "佛光背景.png")).convert("RGBA")
tree = Image.open(os.path.join(BASE, "许愿树.png")).convert("RGBA")
comp = Image.alpha_composite(bg, tree)
comp.convert("RGB").save(os.path.join(BASE, "预览_合成.png"))
print("composite saved")
