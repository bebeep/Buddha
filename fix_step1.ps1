$filepath = 'E:\Android\SceneformEQR\Sceneform-EQR-main\Sceneform-EQR-main\Eq-Renderer\Android\eq-renderer\src\main\java\com\eqgis\eqr\layout\SceneLayout.java'
$enc = New-Object System.Text.UTF8Encoding($false)
$c = [System.IO.File]::ReadAllText($filepath, $enc)
# Chinese strings using char codes
$t_cn = '触摸旋转'
$a_cn = '自动旋转'
$d_cn = '每秒旋转角度'
$c_cn = '触摸旋转控制'
$c = $c.Replace('// touch rotation fields', '// ' + $t_cn + [char]0x7684 + [char]0x76F8 + [char]0x5173 + [char]0x5B57 + [char]0x6BB5)
$c = $c.Replace('// auto rotation fields', '// ' + $a_cn + [char]0x7684 + [char]0x76F8 + [char]0x5173 + [char]0x5B57 + [char]0x6BB5)
$c = $c.Replace('45f; // degrees per second', '45f; // ' + $d_cn + [char]0x89D2 + [char]0x5EA6)
$c = $c.Replace('// ==================== Touch Rotation Control ====================', '// ==================== ' + $c_cn + ' ====================')
[System.IO.File]::WriteAllText($filepath, $c, $enc)
Write-Host "STEP1 DONE"
