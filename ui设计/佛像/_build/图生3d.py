# -*- coding: utf-8 -*-
"""
图生 3D 的调用包装。

skill 自带的命令行只接受 --image-url（公网 URL），本地图片要转 base64；
Windows 命令行参数有 32K 上限，240KB 的 base64 塞不进 argv。
所以这里直接 import 那个脚本，把 base64 注入请求体后调用它的 main()。
"""
import base64
import importlib.util
import sys

SKILL = ("D:/WorkBuddy/resources/app.asar.unpacked/resources/plugins/"
         "workbuddy-builtin/skills/buddy-multimodal-generation/scripts/"
         "buddy-multimodal-generation.py")

spec = importlib.util.spec_from_file_location("mmgen", SKILL)
mm = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mm)

image_path, token = sys.argv[1], sys.argv[2]
with open(image_path, "rb") as f:
    b64 = base64.b64encode(f.read()).decode()
print("[INFO] base64 长度 %d" % len(b64), file=sys.stderr)

_orig = mm._build_3d_body


def patched(**kw):
    kw["image_base64"] = b64
    kw["image_url"] = None
    return _orig(**kw)


mm._build_3d_body = patched

sys.argv = [
    "buddy-multimodal-generation.py", "3d",
    "--image-base64", "injected",
    "--enable-pbr",
    "--face-count", "60000",
    "--token", token,
]
mm.main()
