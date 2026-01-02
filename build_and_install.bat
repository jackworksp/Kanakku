@echo off
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d C:\Users\ADMIN\AndroidStudioProjects\Kanakku
echo Building APK...
call gradlew.bat assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    exit /b 1
)
echo Build successful! Installing on device...
"C:\Users\ADMIN\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
echo Done!
