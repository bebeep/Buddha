import os
from PIL import Image

BASE = r"E:\Android\Buddha\ui设计\切图"
renames = {
    "Semi_realistic_3D_illustration_2026-11-10T03-10-32.png": "许愿树.png",
    "Mobile_app_background__soft_ve_2026-11-10T03-08-06.png": "佛光背景.png",
    "Single_wooden_wish_tag_prayer__2026-11-10T03-08-18.png": "许愿牌.png",
}
for old, new in renames.items():
    src = os.path.join(BASE, old)
    dst = os.path.join(BASE, new)
    if os.path.exists(src) and not os.path.exists(dst):
        os.rename(src, dst)
        print("rename", old, "->", new)

# 合成预览：佛光背景 + 许愿树
bg = Image.open(os.path.join(BASE, "佛光背景.png")).convert("RGBA")
tree = Image.open(os.path.join(BASE, "许愿树.png")).convert("RGBA")
comp = Image.new("RGBA", bg.size)
comp = Image.alpha_composite(comp, bg)
comp = Image.alpha_composite(comp, tree)
comp.convert("RGB").save(os.path.join(BASE, "预览_合成.png"))
print("composite saved", comp.size)
