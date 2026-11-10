from PIL import Image
import numpy as np
import os

B = r'E:/Android/Buddha/ui设计/切图'
im = Image.open(os.path.join(B, '许愿牌.png')).convert('RGBA')
a = np.array(im)[..., 3]
print('alpha extrema:', int(a.min()), int(a.max()))
print('transparent%%: %.1f' % ((a == 0).mean() * 100))
print('size:', im.size)

# 深色底校验图：把牌合成到深墨绿底，直观证明透明+贴合
bg = Image.new('RGB', im.size, (24, 36, 30))
comp = Image.composite(im.convert('RGB'), bg, im.split()[3])
out = os.path.join(B, '_tools', 'check_tag.png')
comp.save(out)
print('check image saved', out, os.path.getsize(out), 'bytes')
