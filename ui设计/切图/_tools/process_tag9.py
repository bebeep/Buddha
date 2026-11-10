from PIL import Image, ImageFilter
import numpy as np
import os

B = r'E:/Android/Buddha/ui设计/切图'
src = os.path.join(B, '3D_cartoon_game_asset__front_f_2026-11-10T03-52-59.png')
im = Image.open(src).convert('RGBA')
W, H = im.size
px = np.array(im).astype(np.float32)
rgb = px[..., :3]
a = px[..., 3]
print('alpha extrema:', a.min(), a.max())

edge = []
for x in range(0, W, 8):
    edge.append(rgb[2, x]); edge.append(rgb[H - 3, x])
for y in range(0, H, 8):
    edge.append(rgb[y, 2]); edge.append(rgb[y, W - 3])
bg = np.array(edge).mean(axis=0)
print('bg color:', bg)

if a.max() > 250 and a.min() > 250:
    d = np.sqrt(((rgb - bg) ** 2).sum(axis=2))
    th, thu = 40, 95
    na = np.where(d < th, 0.0,
                  np.where(d < thu, ((d - th) / (thu - th)) * 255, 255.0))
    bright = rgb.mean(axis=2)
    soft = (na > 0) & (na < 240) & (bright > 200)   # 浅色软边清零
    na[soft] = 0
    out = np.dstack([rgb.astype(np.uint8), na.astype(np.uint8)])
else:
    out = px.astype(np.uint8)

# 去水印：右下角扩大区域，浅色像素（含半透明字样）置透明
x0, y0 = int(W * 0.55), int(H * 0.62)
region = out[y0:H, x0:W]
ra = region[..., 3]
rr = region[..., :3].mean(axis=2)
mask = (rr > 196) & (ra < 255)
region[..., 3][mask] = 0
mask2 = rr > 220
region[..., 3][mask2] = 0
out[y0:H, x0:W] = region

# 暗金微调（保持已到位的金属感）
c = out[..., :3].astype(np.float32) / 255.0
c[..., 0] = np.clip(c[..., 0] * 1.05, 0, 1)
c[..., 2] = np.clip(c[..., 2] * 0.92, 0, 1)
out[..., :3] = (c * 255).astype(np.uint8)

# alpha 柔化边缘，消除锯齿白边
rgb_pil = Image.fromarray(out[..., :3].astype(np.uint8), 'RGB')
a_pil = Image.fromarray(out[..., 3].astype(np.uint8), 'L').filter(ImageFilter.GaussianBlur(1.2))
r4, g4, b4 = rgb_pil.split()
img = Image.merge('RGBA', (r4, g4, b4, a_pil))
arr = np.array(img)

# tight crop（顶部多留 padding 保绳头环完整）
alpha = arr[..., 3]
ys, xs = np.where(alpha > 12)
if len(xs) > 0:
    x0n, x1n = xs.min(), xs.max()
    y0n, y1n = ys.min(), ys.max()
    pad_x = int((x1n - x0n) * 0.02) + 2
    pad_t = int((y1n - y0n) * 0.04) + 4
    pad_b = int((y1n - y0n) * 0.02) + 2
    x0n, x1n = max(0, x0n - pad_x), min(W - 1, x1n + pad_x)
    y0n, y1n = max(0, y0n - pad_t), min(H - 1, y1n + pad_b)
    out2 = arr[y0n:y1n + 1, x0n:x1n + 1]
    print('cropped to', out2.shape[1], 'x', out2.shape[0])

dst = os.path.join(B, '许愿牌_卡通3D暗金.png')
Image.fromarray(out2, 'RGBA').save(dst)
print('saved', dst, os.path.getsize(dst), 'bytes')
