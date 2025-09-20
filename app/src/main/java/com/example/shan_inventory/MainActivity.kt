package com.example.shan_inventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.shan_inventory.ui.screens.PartMainScreen
import com.example.shan_inventory.ui.screens.SplashScreen
import com.example.shan_inventory.ui.theme.Shan_InventoryTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

/**
 * MainActivity is the entry point of the Parts Inventory Android application
 * 
 * This activity handles:
 * - Firebase initialization and configuration
 * - App theme setup
 * - Navigation between splash screen and main parts management screen
 * - Edge-to-edge display configuration for modern Android UI
 * 
 * The app follows a single-activity architecture with Jetpack Compose for UI
 * and Firebase for backend services (Realtime Database and Storage)
 */
class MainActivity : ComponentActivity() {
    
    /**
     * Called when the activity is first created
     * Performs essential app initialization tasks
     * 
     * @param savedInstanceState Previously saved instance state (if any)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display for modern Android UI
        // This allows content to extend behind system bars
        enableEdgeToEdge()
        
        // Initialize Firebase SDK for this application
        // This must be called before using any Firebase services
        FirebaseApp.initializeApp(this)
        
        // Configure Firebase Realtime Database with persistence enabled
        // This allows the app to work offline and sync when connectivity is restored
        // Database URL points to the specific Firebase project: shan001-5b11c
        FirebaseDatabase.getInstance("https://shan001-5b11c-default-rtdb.asia-southeast1.firebasedatabase.app/").setPersistenceEnabled(true)
        
        // Set the main content using Jetpack Compose
        setContent {
            // Apply the custom app theme
            Shan_InventoryTheme {
                // Launch the main app composable
                PartsInventoryApp()
            }
        }
    }
}

/**
 * Main app composable that manages the overall app navigation and screen transitions
 * 
 * This composable handles:
 * - Navigation controller setup for screen routing
 * - Splash screen display with automatic transition to main screen
 * - Animated transitions between splash and main screens
 * - Navigation host configuration for the main parts management screen
 * 
 * The app uses a simple two-screen navigation:
 * 1. SplashScreen - Shows app branding and version info for 3 seconds
 * 2. PartMainScreen - Main parts inventory management interface
 */
@Composable
fun PartsInventoryApp() {
    // Create navigation controller for managing screen navigation
    val navController = rememberNavController()
    
    // State to control splash screen visibility
    // Initially true to show splash screen on app launch
    var showSplash by remember { mutableStateOf(true) }
    
    // Animated splash screen with slide-in from top and fade-in effects
    // Automatically transitions to main screen after 3 seconds
    AnimatedVisibility(
        visible = showSplash,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
    ) {
        SplashScreen(
            navController = navController,
            onNavigateToMain = {
                // Hide splash screen when navigation callback is triggered
                showSplash = false
            }
        )
    }
    
    // Animated main app content with slide-in from bottom and fade-in effects
    // Only visible when splash screen is hidden
    AnimatedVisibility(
        visible = !showSplash,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
    ) {
        // Main app scaffold with navigation host
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            // Navigation host for managing screen routing
            NavHost(
                navController = navController,
                startDestination = "parts_main", // Default screen is parts management
                modifier = Modifier.padding(innerPadding)
            ) {
                // Define the parts main screen route
                composable("parts_main") {
                    PartMainScreen()
                }
            }
        }
    }
}