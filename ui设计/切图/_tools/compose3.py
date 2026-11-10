from PIL import Image
import numpy as np
import os

B = r'E:/Android/Buddha/ui设计/切图'
bg = Image.open(os.path.join(B, '佛光背景.png')).convert('RGBA')
tree = Image.open(os.path.join(B, '许愿树.png')).convert('RGBA')

W, H = bg.size
arr = np.array(bg).astype(np.float32)
Y, X = np.mgrid[0:H, 0:W]
cx, cy = W * 0.50, H * 0.36          # 光心：树冠背后偏上
maxd = max(W, H)
dist = np.sqrt((X - cx) ** 2 + (Y - cy) ** 2)

# 中央暖金光晕（强）
glow = np.clip(1 - dist / (maxd * 0.58), 0, 1) ** 1.4
ring = np.exp(-((dist - maxd * 0.30) ** 2) / (2 * (maxd * 0.06) ** 2)) * 0.7
warm = np.array([255.0, 236.0, 190.0])
light = (glow * 0.5 + ring * 0.4)[..., np.newaxis]
arr[..., :3] += light * warm
# 四角压暗，做纵深、突出中央佛光
vig = np.clip(dist / (maxd * 0.60), 0, 1) ** 1.5
arr[..., :3] -= vig[..., np.newaxis] * 60
arr = np.clip(arr, 0, 255)

bg_glow = Image.fromarray(arr.astype(np.uint8), 'RGBA')

# 叠树：缩到 85%，居中略偏下，让树冠背后佛光从四周天空透出
scale = 0.85
tw, th = int(W * scale), int(H * scale)
tree_r = tree.resize((tw, th), Image.LANCZOS)
off_x = (W - tw) // 2
off_y = (H - th) // 2 + int(H * 0.05)
canvas = Image.new('RGBA', (W, H), (0, 0, 0, 0))
canvas.paste(bg_glow, (0, 0))
canvas.alpha_composite(tree_r, (off_x, off_y))
canvas = canvas.convert('RGB')

# 树根落地薄雾 + 接触阴影感
arr2 = np.array(canvas).astype(np.float32)
fog = np.clip((Y - H * 0.78) / (H * 0.22), 0, 1) ** 1.2
arr2[..., :3] += fog[..., np.newaxis] * 58                      # 底部淡白雾
# 树根两侧微微压暗，制造落地接触阴影
contact = (fog ** 1.5) * (1 - np.clip(np.abs(X - cx) / (W * 0.30), 0, 1))
arr2[..., :3] -= contact[..., np.newaxis] * 30
arr2 = np.clip(arr2, 0, 255)

out = os.path.join(B, '树+佛光_合成整图.png')
Image.fromarray(arr2.astype(np.uint8), 'RGB').save(out, 'PNG')
print('saved', out, os.path.getsize(out), 'bytes')
