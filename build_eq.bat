@echo off
set JAVA_HOME=C:\Users\Administrator\.jdks\openjdk-21
cd /d E:\Android\Buddha
call gradlew.bat :eq-renderer:compileDebugJavaWithJavac --console=plain
