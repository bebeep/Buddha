from PIL import Image
import numpy as np
import os

B = r'E:/Android/Buddha/ui设计/切图'
src = os.path.join(B, 'Semi_realistic_3D_illustration_2026-11-10T03-38-35.png')
im = Image.open(src).convert('RGBA')
W, H = im.size
px = np.array(im).astype(np.float32)
rgb = px[..., :3]
a = px[..., 3]
print('alpha extrema:', a.min(), a.max())

# 背景色：边缘一圈最亮像素均值
edge = []
for x in range(0, W, 8):
    edge.append(rgb[2, x]); edge.append(rgb[H - 3, x])
for y in range(0, H, 8):
    edge.append(rgb[y, 2]); edge.append(rgb[y, W - 3])
bg = np.array(edge).mean(axis=0)
print('bg color:', bg)

if a.max() > 250 and a.min() > 250:
    print('pseudo-transparent, chroma-keying...')
    d = np.sqrt(((rgb - bg) ** 2).sum(axis=2))
    th, thu = 42, 105
    na = np.where(d < th, 0.0,
                  np.where(d < thu, ((d - th) / (thu - th)) * 255, 255.0))
    bright = rgb.mean(axis=2)
    soft = (na > 0) & (na < 255) & (bright > 220)
    na[soft] = 0
    out = np.dstack([rgb.astype(np.uint8), na.astype(np.uint8)])
else:
    out = px.astype(np.uint8)

# 去水印：右下角浅色半透明像素置透明
x0, y0 = int(W * 0.62), int(H * 0.70)
region = out[y0:H, x0:W]
ra = region[..., 3]
rr = region[..., :3].mean(axis=2)
mask = (rr > 200) & (ra < 255)
region[..., 3][mask] = 0
out[y0:H, x0:W] = region

# tight crop 到牌实际轮廓（含红绳流苏），极薄 padding 防硬切
alpha = out[..., 3]
ys, xs = np.where(alpha > 12)
if len(xs) > 0:
    x0n, x1n = xs.min(), xs.max()
    y0n, y1n = ys.min(), ys.max()
    pad = int(max(x1n - x0n, y1n - y0n) * 0.02) + 2
    x0n, x1n = max(0, x0n - pad), min(W - 1, x1n + pad)
    y0n, y1n = max(0, y0n - pad), min(H - 1, y1n + pad)
    out = out[y0n:y1n + 1, x0n:x1n + 1]
    print('cropped to', out.shape[1], 'x', out.shape[0])
else:
    print('no opaque pixels?')

dst = os.path.join(B, '许愿牌.png')
Image.fromarray(out, 'RGBA').save(dst)
print('saved', dst, os.path.getsize(dst), 'bytes')
