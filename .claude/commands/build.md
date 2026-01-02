---
allowed-tools: Bash(./gradlew:*), Bash(gradlew.bat:*), Bash(cmd /c:*), Bash(dir:*)
description: Build Android app in debug or release mode
argument-hint: debug | release
---

# Build Android App

Build the Android application with the specified variant.

## Build Configuration
- Build type: $1 (default: debug)

## Tasks

If argument is **release**:
```bash
./gradlew clean assembleRelease --build-cache
```

If argument is **debug** or no argument:
```bash
./gradlew assembleDebug --build-cache
```

## After Build
- Report build success/failure
- Show APK location and size
- List any warnings or errors
