@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "C:\Users\ADMIN\AndroidStudioProjects\Kanakku"
call gradlew.bat assembleDebug --no-daemon > build_log.txt 2>&1
echo BUILD_EXIT_CODE=%ERRORLEVEL% >> build_log.txt
