from PIL import Image
import numpy as np
import os

B = r'E:/Android/Buddha/ui设计/切图'
src = os.path.join(B, 'Hand_drawn_cartoon_illustratio_2026-11-10T03-35-43.png')
im = Image.open(src).convert('RGBA')
W, H = im.size
px = np.array(im).astype(np.float32)
rgb = px[..., :3]
a = px[..., 3]
print('alpha extrema:', a.min(), a.max())

# 背景色：取边缘一圈最亮像素均值
edge = []
for x in range(0, W, 8):
    edge.append(rgb[2, x]); edge.append(rgb[H - 3, x])
for y in range(0, H, 8):
    edge.append(rgb[y, 2]); edge.append(rgb[y, W - 3])
bg = np.array(edge).mean(axis=0)
print('bg color:', bg)

if a.max() > 250 and a.min() > 250:   # 伪透明近白底 -> 颜色抠图
    print('pseudo-transparent, chroma-keying...')
    d = np.sqrt(((rgb - bg) ** 2).sum(axis=2))
    th, thu = 40, 100
    na = np.where(d < th, 0.0,
                  np.where(d < thu, ((d - th) / (thu - th)) * 255, 255.0))
    # 浅色半透明残边清零
    bright = rgb.mean(axis=2)
    soft = (na > 0) & (na < 255) & (bright > 222)
    na[soft] = 0
    out = np.dstack([rgb.astype(np.uint8), na.astype(np.uint8)])
else:
    print('already has real alpha, keep')
    out = px.astype(np.uint8)

# 去水印：右下角浅色半透明像素置透明（红绳/流苏为红色不会被误删）
x0, y0 = int(W * 0.66), int(H * 0.68)
region = out[y0:H, x0:W]
ra = region[..., 3]
rr = region[..., :3].mean(axis=2)
mask = (rr > 200) & (ra < 255)
region[..., 3][mask] = 0
out[y0:H, x0:W] = region

dst = os.path.join(B, '许愿牌.png')
Image.fromarray(out, 'RGBA').save(dst)
print('saved', dst, os.path.getsize(dst), 'bytes')
