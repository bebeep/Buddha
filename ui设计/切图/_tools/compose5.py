from PIL import Image, ImageFilter
import numpy as np
import os

B = r'E:/Android/Buddha/ui设计/切图'
bg = Image.open(os.path.join(B, '佛光背景.png')).convert('RGBA')
tree = Image.open(os.path.join(B, '许愿树.png')).convert('RGBA')

W, H = bg.size
arr = np.array(bg).astype(np.float32)
Y, X = np.mgrid[0:H, 0:W]
cx, cy = W * 0.50, H * 0.44          # 光心下移，避免顶边截断
maxd = max(W, H)
dist = np.sqrt((X - cx) ** 2 + (Y - cy) ** 2)

# 中央暖金光晕 + 光环（半径收小，完整在图内）
glow = np.clip(1 - dist / (maxd * 0.50), 0, 1) ** 1.4
ring = np.exp(-((dist - maxd * 0.26) ** 2) / (2 * (maxd * 0.055) ** 2)) * 0.7
light = (glow * 0.5 + ring * 0.4)[..., np.newaxis]
warm = np.array([255.0, 236.0, 190.0])
arr[..., :3] += light * warm

# 四角暗角：聚焦佛光
vig = np.clip((dist - maxd * 0.30) / (maxd * 0.35), 0, 1) ** 1.3
arr[..., :3] -= vig[..., np.newaxis] * 90

# 树根接触阴影（地面，树落下之前）
sx, sy = W * 0.50, H * 0.95
xr, yr = W * 0.24, H * 0.045
sdist = np.sqrt(((X - sx) / xr) ** 2 + ((Y - sy) / yr) ** 2)
shadow = np.exp(-(sdist ** 2) / 2.0)
arr[..., :3] -= shadow[..., np.newaxis] * 85

# 顶部渐隐：最上 14% 压暗，把光晕截断边藏成自然渐隐
topf = np.clip((H * 0.14 - Y) / (H * 0.14), 0, 1)
arr[..., :3] -= topf[..., np.newaxis] * 50

bg_glow = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGBA')

# 叠树：缩 80% 留安全区，alpha 羽化 3px 去硬边
scale = 0.80
tw, th = int(W * scale), int(H * scale)
tree_r = tree.resize((tw, th), Image.LANCZOS)
ta = tree_r.split()[3].filter(ImageFilter.GaussianBlur(3.0))
tree_r.putalpha(ta)
off_x = (W - tw) // 2
off_y = (H - th) // 2 + int(H * 0.06)
canvas = Image.new('RGBA', (W, H), (0, 0, 0, 0))
canvas.paste(bg_glow, (0, 0))
canvas.alpha_composite(tree_r, (off_x, off_y))
canvas = canvas.convert('RGB')

# 底部落地薄雾（起始抬高、不提亮树根正下方）+ 两侧压暗
arr2 = np.array(canvas).astype(np.float32)
fog = np.clip((Y - H * 0.84) / (H * 0.16), 0, 1) ** 1.2
arr2[..., :3] += fog[..., np.newaxis] * 42
contact = (fog ** 1.5) * (1 - np.clip(np.abs(X - cx) / (W * 0.30), 0, 1))
arr2[..., :3] -= contact[..., np.newaxis] * 26
arr2 = np.clip(arr2, 0, 255)

out = os.path.join(B, '树+佛光_合成整图.png')
Image.fromarray(arr2.astype(np.uint8), 'RGB').save(out, 'PNG')
print('saved', out, os.path.getsize(out), 'bytes')
