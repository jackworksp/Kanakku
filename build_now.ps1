$env:JAVA_HOME = "C:\Program Files\Android\Android Studio1\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Set-Location "C:\Users\ADMIN\AndroidStudioProjects\Kanakku"
Write-Host "===== Starting Build ====="
& .\gradlew.bat assembleDebug --no-daemon
Write-Host "===== Build Exit Code: $LASTEXITCODE ====="
