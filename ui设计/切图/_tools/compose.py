from PIL import Image
import os

B = r'E:/Android/Buddha/ui设计/切图'

bg = Image.open(os.path.join(B, '佛光背景.png')).convert('RGBA')
tree = Image.open(os.path.join(B, '许愿树.png')).convert('RGBA')

W, H = bg.size
print('bg size', bg.size, 'tree size', tree.size)

# 把树缩小到 93%，居中（略偏下 2%），让四周佛光天空透出
scale = 0.93
tw, th = int(W * scale), int(H * scale)
tree_r = tree.resize((tw, th), Image.LANCZOS)

# 居中，树根稍沉底（向下偏移 2% 高度）
off_x = (W - tw) // 2
off_y = (H - th) // 2 + int(H * 0.02)

canvas = Image.new('RGBA', (W, H), (0, 0, 0, 0))
canvas.paste(bg, (0, 0))
canvas.alpha_composite(tree_r, (off_x, off_y))

out = os.path.join(B, '树+佛光_合成整图.png')
canvas.convert('RGB').save(out, 'PNG')
print('saved', out, os.path.getsize(out), 'bytes')
