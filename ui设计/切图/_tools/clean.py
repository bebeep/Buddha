import os
from PIL import Image

BASE = r"E:\Android\Buddha\ui设计\切图"
files = {
    "tree": "Semi_realistic_3D_illustration_2026-11-10T03-10-32.png",
    "bg":   "Mobile_app_background__soft_ve_2026-11-10T03-08-06.png",
    "tag":  "Single_wooden_wish_tag_prayer__2026-11-10T03-08-18.png",
}

def lum(r, g, b):
    return 0.299 * r + 0.587 * g + 0.114 * b

def defringe(img):
    """透明图去白边：极低 alpha 直接清；半透明且偏白(抠图残留)也清。"""
    w, h = img.size
    px = img.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 50:
                px[x, y] = (r, g, b, 0)
            elif a < 180 and lum(r, g, b) > 210:
                px[x, y] = (r, g, b, 0)
    return img

def erase_corner(img, pad_x=470, pad_y=110):
    """擦掉右下角水印区域：透明图置透明；不透明图用上方像素填充。"""
    w, h = img.size
    px = img.load()
    x0 = max(0, w - pad_x)
    y0 = max(0, h - pad_y)
    for y in range(y0, h):
        for x in range(x0, w):
            r, g, b, a = px[x, y]
            if a < 200:
                px[x, y] = (r, g, b, 0)
            else:
                ay = max(0, y - 2)
                px[x, y] = px[x, ay]
    return img

for name, fn in files.items():
    path = os.path.join(BASE, fn)
    im = Image.open(path).convert("RGBA")
    if name in ("tree", "tag"):
        im = defringe(im)
    im = erase_corner(im)
    im.save(path)
    print("processed", name, im.size)
