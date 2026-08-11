$file = 'E:\Android\SceneformEQR\Sceneform-EQR-main\Sceneform-EQR-main\Eq-Renderer\Android\eq-renderer\src\main\java\com\eqgis\eqr\layout\SceneLayout.java'
$lines = [System.IO.File]::ReadAllLines($file)
$output = New-Object System.Collections.Generic.List[string]
$fieldsAdded = $false
$methodsAdded = $false

for ($i = 0; $i -lt $lines.Count; $i++) {
    $line = $lines[$i]
    
    # Add touch rotation fields after gestureDetector field (only once)
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
    
    # Add touch rotation methods before captureScreen javadoc
    if (-not $methodsAdded -and $line.Contains('* @param folderPath') -and ($i + 1) -lt $lines.Count -and $lines[$i + 1].Contains('* @param crop')) {
        # Remove the last added line (the "/**" line for captureScreen)
        $lastIdx = $output.Count - 1
        if ($lastIdx -ge 0 -and $output[$lastIdx].Contains('/**')) {
            $output.RemoveAt($lastIdx)
        }
        
        $output.Add('    // ==================== Touch Rotation Control ====================')
        $output.Add('')
        $output.Add('    /**')
        $output.Add('     * Enable touch rotation control')
        $output.Add('     * @param target target node to rotate')
        $output.Add('     * @param sensitivity sensitivity per pixel, recommended 0.3f-1.0f')
        $output.Add('     */')
        $output.Add('    public void enableTouchRotation(Node target, float sensitivity) {')
        $output.Add('        this.touchRotationTarget = target;')
        $output.Add('        this.touchRotationSensitivity = sensitivity;')
        $output.Add('        this.touchRotationEnabled = true;')
        $output.Add('        this.currentRotationX = 0f;')
        $output.Add('        this.currentRotationY = 0f;')
        $output.Add('    }')
        $output.Add('')
        $output.Add('    /**')
        $output.Add('     * Enable touch rotation control with default sensitivity')
        $output.Add('     * @param target target node to rotate')
        $output.Add('     */')
        $output.Add('    public void enableTouchRotation(Node target) {')
        $output.Add('        enableTouchRotation(target, 0.5f);')
        $output.Add('    }')
        $output.Add('')
        $output.Add('    /**')
        $output.Add('     * Disable touch rotation')
        $output.Add('     */')
        $output.Add('    public void disableTouchRotation() {')
        $output.Add('        this.touchRotationEnabled = false;')
        $output.Add('        this.touchRotationTarget = null;')
        $output.Add('    }')
        $output.Add('')
        $output.Add('    /**')
        $output.Add('     * Whether user is currently interacting')
        $output.Add('     */')
        $output.Add('    public boolean isUserInteracting() {')
        $output.Add('        return isUserInteracting;')
        $output.Add('    }')
        $output.Add('')
        $output.Add('    /**')
        $output.Add('     * Apply current rotation to target node')
        $output.Add('     */')
        $output.Add('    private void applyTouchRotation() {')
        $output.Add('        if (touchRotationTarget == null) return;')
        $output.Add('        Quaternion rotationY = new Quaternion(Vector3.up(), currentRotationY);')
        $output.Add('        Quaternion rotationX = new Quaternion(Vector3.right(), currentRotationX);')
        $output.Add('        touchRotationTarget.setLocalRotation(Quaternion.multiply(rotationY, rotationX));')
        $output.Add('    }')
        $output.Add('')
        $output.Add('    @Override')
        $output.Add('    public boolean onInterceptTouchEvent(MotionEvent ev) {')
        $output.Add('        if (touchRotationEnabled && touchRotationTarget != null) {')
        $output.Add('            if (ev.getAction() == MotionEvent.ACTION_DOWN) {')
        $output.Add('                lastTouchX = ev.getX();')
        $output.Add('                lastTouchY = ev.getY();')
        $output.Add('                isUserInteracting = true;')
        $output.Add('            }')
        $output.Add('        }')
        $output.Add('        return super.onInterceptTouchEvent(ev);')
        $output.Add('    }')
        $output.Add('')
        $output.Add('    @Override')
        $output.Add('    public boolean onTouchEvent(MotionEvent event) {')
        $output.Add('        if (touchRotationEnabled && touchRotationTarget != null) {')
        $output.Add('            switch (event.getAction()) {')
        $output.Add('                case MotionEvent.ACTION_DOWN:')
        $output.Add('                    lastTouchX = event.getX();')
        $output.Add('                    lastTouchY = event.getY();')
        $output.Add('                    isUserInteracting = true;')
        $output.Add('                    return true;')
        $output.Add('                case MotionEvent.ACTION_MOVE:')
        $output.Add('                    float deltaX = event.getX() - lastTouchX;')
        $output.Add('                    float deltaY = event.getY() - lastTouchY;')
        $output.Add('                    currentRotationY += deltaX * touchRotationSensitivity;')
        $output.Add('                    currentRotationX -= deltaY * touchRotationSensitivity;')
        $output.Add('                    currentRotationX = Math.max(-90f, Math.min(90f, currentRotationX));')
        $output.Add('                    applyTouchRotation();')
        $output.Add('                    lastTouchX = event.getX();')
        $output.Add('                    lastTouchY = event.getY();')
        $output.Add('                    return true;')
        $output.Add('                case MotionEvent.ACTION_UP:')
        $output.Add('                case MotionEvent.ACTION_CANCEL:')
        $output.Add('                    isUserInteracting = false;')
        $output.Add('                    return true;')
        $output.Add('            }')
        $output.Add('        }')
        $output.Add('        return super.onTouchEvent(event);')
        $output.Add('    }')
        $output.Add('')
        $output.Add('    /**')
        $output.Add($line)
        $methodsAdded = $true
        continue
    }
    
    $output.Add($line)
}

[System.IO.File]::WriteAllLines($file, $output.ToArray())
Write-Host "Done. Lines: $($output.Count), fieldsAdded=$fieldsAdded, methodsAdded=$methodsAdded"
