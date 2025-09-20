// Build configuration for Parts Inventory Android App
// This file defines the app's build settings, dependencies, and compilation options

plugins {
    alias(libs.plugins.android.application) // Android application plugin
    alias(libs.plugins.kotlin.android)      // Kotlin Android plugin
    alias(libs.plugins.kotlin.compose)      // Kotlin Compose plugin for UI
    alias(libs.plugins.google.services)     // Google Services plugin for Firebase
}

android {
    namespace = "com.example.shan_inventory"
    compileSdk = 36 // Latest Android API level for compilation

    defaultConfig {
        applicationId = "com.example.shan_inventory"
        minSdk = 31        // Minimum Android 12 (API 31) required
        targetSdk = 36     // Target latest Android API level
        versionCode = 1    // Internal version number
        versionName = "1.0" // User-visible version string

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Disable code shrinking for debugging
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    // Java compatibility settings
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    // Kotlin compiler options
    kotlinOptions {
        jvmTarget = "11"
    }
    
    // Build features
    buildFeatures {
        compose = true // Enable Jetpack Compose
    }
}

dependencies {
    // Core Android libraries
    implementation(libs.androidx.core.ktx)                    // Kotlin extensions for Android
    implementation(libs.androidx.lifecycle.runtime.ktx)       // Lifecycle-aware components
    implementation(libs.androidx.activity.compose)            // Compose activity integration
    implementation(libs.androidx.activity.ktx)                // Activity Kotlin extensions
    
    // Jetpack Compose UI framework
    implementation(platform(libs.androidx.compose.bom))       // Compose BOM for version management
    implementation(libs.androidx.ui)                          // Core Compose UI
    implementation(libs.androidx.ui.graphics)                 // Compose graphics
    implementation(libs.androidx.ui.tooling.preview)          // Compose preview tools
    implementation(libs.androidx.material3)                   // Material Design 3 components
    implementation(libs.androidx.navigation.compose)          // Compose navigation
    
    // Firebase backend services
    implementation(platform(libs.firebase.bom))               // Firebase BOM for version management
    implementation(libs.firebase.database)                    // Firebase Realtime Database
    implementation(libs.firebase.storage)                     // Firebase Storage for images
    
    // Camera functionality
    implementation(libs.camerax.core)                         // CameraX core library
    implementation(libs.camerax.camera2)                      // Camera2 integration
    implementation(libs.camerax.lifecycle)                    // CameraX lifecycle management
    implementation(libs.camerax.view)                         // CameraX view components
    
    // Image loading and processing
    implementation(libs.coil.compose)                         // Coil image loading for Compose
    
    // Speech recognition and additional features
    implementation("androidx.activity:activity-compose:1.8.2")           // Activity Compose integration
    implementation("androidx.compose.runtime:runtime-livedata:1.5.4")    // LiveData integration
    implementation("androidx.compose.material:material-icons-extended:1.5.4") // Extended Material icons
    
    // Testing dependencies
    testImplementation(libs.junit)                            // JUnit testing framework
    androidTestImplementation(libs.androidx.junit)            // Android JUnit testing
    androidTestImplementation(libs.androidx.espresso.core)    // Espresso UI testing
    androidTestImplementation(platform(libs.androidx.compose.bom))       // Compose testing BOM
    androidTestImplementation(libs.androidx.ui.test.junit4)   // Compose UI testing
    debugImplementation(libs.androidx.ui.tooling)             // Compose debug tools
    debugImplementation(libs.androidx.ui.test.manifest)       // Compose test manifest
}