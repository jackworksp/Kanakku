@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "C:\Users\ADMIN\AndroidStudioProjects\Kanakku"
echo ===== Starting Build =====
call gradlew.bat assembleDebug --no-daemon
echo ===== Build Exit Code: %ERRORLEVEL% =====
