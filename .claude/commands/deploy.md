---
allowed-tools: Bash(./gradlew:*), Bash(gradlew.bat:*), Bash(adb:*), Bash(cmd /c:*), Bash(dir:*)
description: Build and install app to Android device or emulator
argument-hint: debug | release
---

# Deploy Android App

Build and install the application to a connected device or emulator.

## Build Type
- Variant: $1 (default: debug)

## Deployment Steps

### 1. Check Connected Devices
```bash
adb devices -l
```

### 2. Build APK
For debug:
```bash
./gradlew assembleDebug
```

For release:
```bash
./gradlew assembleRelease
```

### 3. Install to Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 4. Launch App
```bash
adb shell am start -n com.example.kanakku/.MainActivity
```

## After Deploy
- Confirm installation success
- Report any installation errors
- Show logcat for runtime issues if needed
