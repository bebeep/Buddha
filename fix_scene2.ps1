$file = 'E:\Android\SceneformEQR\Sceneform-EQR-main\Sceneform-EQR-main\Eq-Renderer\Android\eq-renderer\src\main\java\com\eqgis\eqr\layout\SceneLayout.java'
$lines = [System.IO.File]::ReadAllLines($file)
$output = New-Object System.Collections.Generic.List[string]

$i = 0
while ($i -lt $lines.Count) {
    $line = $lines[$i]
    
    # Fix 1: Remove broken javadoc before touch rotation section
    # Pattern: "/**" followed by "* 截屏" followed by "" followed by "// ==="
    if ($line.Trim() -eq '/**' -and ($i + 1) -lt $lines.Count -and $lines[$i + 1].Contains('截屏')) {
        # Check if this is the broken javadoc (next non-empty line is the touch rotation section)
        $j = $i + 2
        while ($j -lt $lines.Count -and $lines[$j].Trim() -eq '') { $j++ }
        if ($j -lt $lines.Count -and $lines[$j].Contains('// ==================== Touch Rotation Control')) {
            # This is the broken javadoc, skip "/**" and "* 截屏" and empty lines
            $i = $j  # skip to the "// ===" line
            $output.Add('')
            $output.Add($lines[$i])
            $i++
            continue
        }
    }
    
    # Fix 2: Add description line to captureScreen javadoc
    # Pattern: "/**" followed by "* @param folderPath"
    if ($line.Trim() -eq '/**' -and ($i + 1) -lt $lines.Count -and $lines[$i + 1].Contains('* @param folderPath')) {
        $output.Add($line)
        $output.Add('     * 截屏')
        $i++
        continue
    }
    
    $output.Add($line)
    $i++
}

[System.IO.File]::WriteAllLines($file, $output.ToArray())
Write-Host "Javadoc fix done. Lines: $($output.Count)"
