$file = 'E:\Android\SceneformEQR\Sceneform-EQR-main\Sceneform-EQR-main\Eq-Renderer\Android\eq-renderer\src\main\java\com\eqgis\eqr\layout\SceneLayout.java'
$lines = [System.IO.File]::ReadAllLines($file)
$output = New-Object System.Collections.Generic.List[string]
$fieldsAdded = $false

for ($i = 0; $i -lt $lines.Count; $i++) {
    $line = $lines[$i]
    
    # Fix: add fields after gestureDetector
    if (-not $fieldsAdded -and $line.Contains('private GestureDetector gestureDetector;')) {
        $output.Add($line)
        $output.Add('')
        $output.Add('    // touch rotation fields')
        $output.Add('    private Node touchRotationTarget;')
        $output.Add('    private float lastTouchX = 0f;')
        $output.Add('    private float lastTouchY = 0f;')
        $output.Add('    private boolean isUserInteracting = false;')
        $output.Add('    private float currentRotationX = 0f;')
        $output.Add('    private float currentRotationY = 0f;')
        $output.Add('    private float touchRotationSensitivity = 0.5f;')
        $output.Add('    private boolean touchRotationEnabled = false;')
        $fieldsAdded = $true
        continue
    }
    
    # Fix: repair broken javadoc for captureScreen
    # Replace the broken "/**" + "* 截屏" + "// ===" with proper structure
    if ($line.Contains('* ') -and $line.Contains('截屏') -and ($i + 1) -lt $lines.Count -and $lines[$i + 1].Contains('// ==================== Touch Rotation Control')) {
        # Skip this broken line, the "/**" above already added, methods section follows
        continue
    }
    
    # Fix: ensure captureScreen javadoc is proper
    if ($line.Contains('// ==================== Touch Rotation Control ====================')) {
        # Make sure there's a proper javadoc start before this
        $lastIdx = $output.Count - 1
        if ($lastIdx -ge 0 -and $output[$lastIdx].Contains('/**')) {
            # Remove the dangling /**
            $output.RemoveAt($lastIdx)
        }
        $output.Add('')
        $output.Add($line)
        continue
    }
    
    # Fix: ensure captureScreen has proper javadoc after methods section
    if ($line.Contains('* @param folderPath') -and ($i -gt 0) -and $lines[$i - 1].Contains('截屏')) {
        # Add the javadoc start
        $output.Add('    /**')
        $output.Add($line)
        continue
    }
    
    $output.Add($line)
}

[System.IO.File]::WriteAllLines($file, $output.ToArray())
Write-Host "Fix done. Lines: $($output.Count), fieldsAdded=$fieldsAdded"
