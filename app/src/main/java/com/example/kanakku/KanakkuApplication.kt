package com.example.kanakku

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Kanakku.
 *
 * This class is annotated with @HiltAndroidApp to enable Hilt dependency injection
 * across the entire application. The annotation triggers Hilt's code generation to create
 * all necessary components and modules for dependency injection.
 *
 * Hilt will automatically:
 * - Generate the application-level dependency container
 * - Manage the lifecycle of injected dependencies
 * - Provide dependencies to Activities, ViewModels, and other Android components
 *
 * The Application class serves as the entry point for the app's dependency graph.
 * All dependencies provided by Hilt modules will be available throughout the app's lifecycle.
 *
 * Usage:
 * This class must be registered in AndroidManifest.xml:
 * ```xml
 * <application
 *     android:name=".KanakkuApplication"
 *     ...>
 * ```
 *
 * @see dagger.hilt.android.HiltAndroidApp
 */
@HiltAndroidApp
class KanakkuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Hilt will automatically initialize here
        // Any application-level initialization can be added here if needed
    }
}
