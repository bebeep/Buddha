# -*- coding: utf-8 -*-
"""
把 GLB 里的贴图降到指定尺寸并转 JPEG，重建 GLB。

为什么不用 gltf-transform：
- 它的 --texture-compress 只支持 ktx2 / webp / avif；
- 项目用的 Filament gltfio 只认 PNG/JPEG（webp 不支持，ktx2 还要额外挂 basis 转码器），
  所以只能自己把 PNG 转成 JPEG 再拼回去。

用法：python 压缩glb.py 输入.glb 输出.glb [最大边长] [质量]
"""
import json
import os
import struct
import sys

from PIL import Image

MAX_SIZE = int(sys.argv[3]) if len(sys.argv) > 3 else 1024
QUALITY = int(sys.argv[4]) if len(sys.argv) > 4 else 88


def read_glb(path):
    data = open(path, "rb").read()
    magic, version, _ = struct.unpack("<4sII", data[:12])
    assert magic == b"glTF", "不是 GLB 文件"
    offset, chunks = 12, {}
    while offset < len(data):
        clen, ctype = struct.unpack("<I4s", data[offset:offset + 8])
        chunks[ctype] = data[offset + 8:offset + 8 + clen]
        offset += 8 + clen
    return json.loads(chunks[b"JSON"]), chunks[b"BIN\x00"]


def write_glb(path, gltf, binary):
    j = json.dumps(gltf, separators=(",", ":")).encode("utf-8")
    j += b" " * ((4 - len(j) % 4) % 4)
    b = binary + b"\x00" * ((4 - len(binary) % 4) % 4)
    total = 12 + 8 + len(j) + 8 + len(b)
    with open(path, "wb") as f:
        f.write(struct.pack("<4sII", b"glTF", 2, total))
        f.write(struct.pack("<I4s", len(j), b"JSON") + j)
        f.write(struct.pack("<I4s", len(b), b"BIN\x00") + b)


def main():
    src, dst = sys.argv[1], sys.argv[2]
    gltf, bin_data = read_glb(src)

    views = gltf["bufferViews"]
    image_view = {}
    for i, img in enumerate(gltf.get("images", [])):
        if "bufferView" in img:
            image_view[img["bufferView"]] = i

    new_parts, new_views = [], []
    offset = 0
    for vi, view in enumerate(views):
        start = view.get("byteOffset", 0)
        length = view["byteLength"]
        raw = bin_data[start:start + length]

        if vi in image_view:
            i = image_view[vi]
            im = Image.open(__import__("io").BytesIO(raw))
            w, h = im.size
            if max(w, h) > MAX_SIZE:
                scale = MAX_SIZE / max(w, h)
                im = im.resize((max(1, int(w * scale)), max(1, int(h * scale))), Image.LANCZOS)
            im = im.convert("RGB")
            buf = __import__("io").BytesIO()
            im.save(buf, "JPEG", quality=QUALITY, optimize=True)
            raw = buf.getvalue()
            gltf["images"][i]["mimeType"] = "image/jpeg"
            print("贴图 %d: %dx%d -> %dx%d, %.2fMB -> %.2fMB"
                  % (i, w, h, im.width, im.height, length / 1048576, len(raw) / 1048576))

        pad = (4 - len(raw) % 4) % 4
        new_views.append({"buffer": 0, "byteOffset": offset,
                          "byteLength": len(raw), "target": view["target"]}
                         if "target" in view else
                         {"buffer": 0, "byteOffset": offset, "byteLength": len(raw)})
        new_parts.append(raw + b"\x00" * pad)
        offset += len(raw) + pad

    gltf["bufferViews"] = new_views
    gltf["buffers"] = [{"byteLength": offset}]
    write_glb(dst, gltf, b"".join(new_parts))
    print("%s -> %s : %.2fMB -> %.2fMB"
          % (os.path.basename(src), os.path.basename(dst),
             os.path.getsize(src) / 1048576, os.path.getsize(dst) / 1048576))


if __name__ == "__main__":
    main()
