---
name: gradle-config
description: Helps with Gradle build configuration for Android projects. Use when configuring dependencies, build variants, signing configs, ProGuard/R8 rules, or troubleshooting build issues.
allowed-tools: Read, Grep, Glob, Bash(./gradlew:*), Bash(gradlew.bat:*)
---

# Gradle Build Configuration for Android

## Instructions

Provide guidance on Android Gradle configuration.

### 1. Build Configuration (Kotlin DSL)

```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

android {
    namespace = "com.example.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
    }
}
```

### 2. Version Catalogs (libs.versions.toml)

```toml
[versions]
kotlin = "1.9.20"
androidx-core = "1.12.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "androidx-core" }

[bundles]
androidx-ui = ["androidx-appcompat", "androidx-core-ktx"]
```

### 3. Common Commands

```bash
./gradlew assembleDebug       # Build debug
./gradlew assembleRelease     # Build release
./gradlew dependencies        # Dependency tree
./gradlew clean              # Clean build
./gradlew --version          # Gradle version
```

### 4. Performance Optimization

```properties
# gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.jvmargs=-Xmx4096m
kotlin.incremental=true
```

### 5. Troubleshooting

- Clean build: `./gradlew clean`
- Refresh dependencies: `./gradlew --refresh-dependencies`
- Verbose output: `./gradlew build --info`
- Stop daemon: `./gradlew --stop`
