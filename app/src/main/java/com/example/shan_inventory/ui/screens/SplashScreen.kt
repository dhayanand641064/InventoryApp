package com.example.shan_inventory.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.shan_inventory.AboutActivity
import com.example.shan_inventory.R
import kotlinx.coroutines.delay

/**
 * SplashScreen composable displays the app's branding and version information
 * 
 * This screen provides:
 * - Animated app logo with scale and fade effects
 * - App name and description with staggered animations
 * - Version information display (Version 1.0, Sep 16, 2025)
 * - Loading indicator during the splash duration
 * - About button for accessing app information
 * - Automatic navigation to main screen after 3 seconds
 * 
 * Animation sequence:
 * 1. Logo scales up from 0.5x to 1.0x with bounce effect
 * 2. Logo fades in over 800ms
 * 3. Text fades in after 400ms delay
 * 4. About button slides up from bottom after 600ms delay
 * 5. Screen automatically navigates to main after 3 seconds
 * 
 * @param navController Navigation controller for screen routing
 * @param onNavigateToMain Callback function to navigate to main screen
 */
@Composable
fun SplashScreen(
    navController: NavController,
    onNavigateToMain: () -> Unit
) {
    // State to control animation visibility
    var isVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Animation states for different UI elements
    // Logo scale animation with bounce effect (EaseOutBack)
    val logoScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.5f,
        animationSpec = tween(1000, easing = EaseOutBack),
        label = "logoScale"
    )
    
    // Logo alpha animation for fade-in effect
    val logoAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(800),
        label = "logoAlpha"
    )
    
    // Text alpha animation with delay for staggered effect
    val textAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 400),
        label = "textAlpha"
    )
    
    // About button offset animation for slide-up effect
    val aboutButtonOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 100f,
        animationSpec = tween(800, delayMillis = 600),
        label = "aboutButtonOffset"
    )
    
    // LaunchedEffect to handle splash screen timing and navigation
    LaunchedEffect(Unit) {
        isVisible = true // Start animations
        delay(3000) // Show splash for 3 seconds
        onNavigateToMain() // Navigate to main screen
    }
    
    // Main container with gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // Blue gradient background for professional appearance
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1976D2), // Primary blue
                        Color(0xFF1565C0)  // Darker blue
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Main content column with centered alignment
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo with scale and alpha animations
            Card(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .scale(logoScale) // Animated scale from 0.5x to 1.0x
                    .alpha(logoAlpha), // Animated alpha from 0 to 1
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                // White background container for the logo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    // App logo image with 80dp size
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo_n),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Name with fade-in animation
            Text(
                text = "Inventory",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha) // Animated alpha
            )
            
            // App subtitle with fade-in animation
            Text(
                text = "Parts Management",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f), // Slightly transparent white
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha) // Animated alpha
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Version information card with fade-in animation
            Card(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .alpha(textAlpha), // Animated alpha
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Version number display
                    Text(
                        text = "Version 1.0",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1976D2) // Blue color matching theme
                    )
                    // Release date display
                    Text(
                        text = "Sep 16, 2025",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Loading indicator with fade-in animation
            // Only shows when animations are visible
            if (isVisible) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.alpha(textAlpha) // Animated alpha
                )
            }
        }
        
        // About button with slide-up animation from bottom
        Button(
            onClick = {
                // Launch AboutActivity to show app information
                val intent = Intent(context, AboutActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 116.dp) // 16dp original + 100dp lift
                .offset(y = aboutButtonOffset.dp), // Animated offset for slide-up effect
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50) // Green color for contrast
            )
        ) {
            // Info icon
            Icon(
                Icons.Default.Info,
                contentDescription = "About",
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Button text
            Text(
                text = "About",
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

