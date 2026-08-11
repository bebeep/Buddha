import os

file_path = r'E:\Android\SceneformEQR\Sceneform-EQR-main\Sceneform-EQR-main\Eq-Renderer\Android\eq-renderer\src\main\java\com\eqgis\eqr\layout\SceneLayout.java'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix 1: Remove broken javadoc before touch rotation section
broken = '    /**\n     * \u622a\u5c4f\n\n    // ==================== Touch Rotation Control ===================='
fixed = '    // ==================== Touch Rotation Control ===================='
if broken in content:
    content = content.replace(broken, fixed)
    print("Fix 1 applied: removed broken javadoc")
else:
    print("Fix 1: pattern not found")

# Fix 2: Add description to captureScreen javadoc
old = '    /**\n     * @param folderPath'
new = '    /**\n     * \u622a\u5c4f\n     * @param folderPath'
if old in content:
    content = content.replace(old, new)
    print("Fix 2 applied: added description to captureScreen javadoc")
else:
    print("Fix 2: pattern not found")

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Done!")
