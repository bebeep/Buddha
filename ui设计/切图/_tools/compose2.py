from PIL import Image
import numpy as np
import os

B = r'E:/Android/Buddha/ui设计/切图'
bg = Image.open(os.path.join(B, '佛光背景.png')).convert('RGBA')
tree = Image.open(os.path.join(B, '许愿树.png')).convert('RGBA')

W, H = bg.size
arr = np.array(bg).astype(np.float32)
Y, X = np.mgrid[0:H, 0:W]

# ---- 1) 佛光光晕：从树后中央偏上透出的暖金光 ----
cx, cy = W * 0.50, H * 0.40          # 光心：画面中上部
maxd = max(W, H)
dist = np.sqrt((X - cx) ** 2 + (Y - cy) ** 2)
glow = np.clip(1 - dist / (maxd * 0.72), 0, 1) ** 1.7   # 柔和外溢
# 光环（一圈更亮的环）
ring = np.exp(-((dist - maxd * 0.34) ** 2) / (2 * (maxd * 0.07) ** 2)) * 0.6
light = np.array([255.0, 238.0, 198.0])               # 暖金色
for c in range(3):
    arr[..., c] = np.clip(arr[..., c] + (glow + ring) * light[c] * 0.5, 0, 255)

# 把佛光背景转回图，准备叠树
bg_glow = Image.fromarray(arr.astype(np.uint8), 'RGBA')

# ---- 2) 叠树（缩小 93% 居中略偏下）----
scale = 0.93
tw, th = int(W * scale), int(H * scale)
tree_r = tree.resize((tw, th), Image.LANCZOS)
off_x = (W - tw) // 2
off_y = (H - th) // 2 + int(H * 0.02)

canvas = Image.new('RGBA', (W, H), (0, 0, 0, 0))
canvas.paste(bg_glow, (0, 0))
canvas.alpha_composite(tree_r, (off_x, off_y))

# ---- 3) 树根落地薄雾：底部加淡白雾，让树根没入、消除悬浮 ----
canvas = canvas.convert('RGB')
arr2 = np.array(canvas).astype(np.float32)
fog = np.clip((Y - H * 0.80) / (H * 0.20), 0, 1) ** 1.3   # 底部最强
for c in range(3):
    arr2[..., c] = np.clip(arr2[..., c] + fog * 46, 0, 255)
# 底部 alpha 保持不透明（已是不透明背景）
out_arr = arr2.astype(np.uint8)

out = os.path.join(B, '树+佛光_合成整图.png')
Image.fromarray(out_arr, 'RGB').save(out, 'PNG')
print('saved', out, os.path.getsize(out), 'bytes')
