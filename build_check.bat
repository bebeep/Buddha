@echo off
cd /d E:\Android\Buddha
call gradlew.bat :uilib:compileDebugKotlin --console=plain 2>&1
