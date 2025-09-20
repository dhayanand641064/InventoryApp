package com.example.shan_inventory.utils

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraManager class handles file creation for camera captures
 * Provides methods to create image files with different resolutions
 * for the Parts Inventory application
 */
class CameraManager {
    /**
     * Creates a standard image file for camera capture
     * Uses the app's external Pictures directory for storage
     * 
     * @param context Android context for accessing file system
     * @return File object representing the image file to be created
     */
    fun createImageFile(context: Context): File {
        // Generate timestamp for unique file naming
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        
        // Get the app's external Pictures directory
        val storageDir = context.getExternalFilesDir("Pictures")
        
        // Ensure the directory exists before creating files
        storageDir?.mkdirs()
        
        // Create temporary file with timestamp prefix
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }
    
    /**
     * Creates an image file specifically for 1080p camera capture
     * Uses the app's external Pictures directory for storage
     * File naming includes "1080p" prefix for identification
     * 
     * @param context Android context for accessing file system
     * @return File object representing the 1080p image file to be created
     */
    fun createImageFileWith1080p(context: Context): File {
        // Generate timestamp for unique file naming
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        
        // Get the app's external Pictures directory
        val storageDir = context.getExternalFilesDir("Pictures")
        
        // Ensure the directory exists before creating files
        storageDir?.mkdirs()
        
        // Create temporary file with 1080p prefix and timestamp
        return File.createTempFile(
            "JPEG_1080p_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }
}

/**
 * Composable function that provides camera capture functionality
 * Handles permission requests and launches camera intent for image capture
 * Uses 1080p resolution for high-quality part images
 * 
 * @param onImageCaptured Callback function called when image is successfully captured
 * @param onError Callback function called when an error occurs during capture
 * @return Function that can be called to trigger camera capture
 */
@Composable
fun useCameraCapture(
    onImageCaptured: (Uri) -> Unit,
    onError: (Exception) -> Unit
): () -> Unit {
    // Get current Android context
    val context = LocalContext.current
    
    // Create CameraManager instance (remembered to avoid recreation)
    val cameraManager = remember { CameraManager() }
    
    // State to hold the URI of the captured image
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Camera launcher for 1080p image capture
    // Uses ActivityResultContracts.TakePicture() for camera intent
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        // Check if capture was successful and URI exists
        if (success && imageUri != null) {
            onImageCaptured(imageUri!!)
        } else {
            onError(Exception("Failed to capture image"))
        }
    }
    
    // Permission launcher for camera permission request
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, proceed with camera launch
            try {
                // Create 1080p image file for capture
                val imageFile = cameraManager.createImageFileWith1080p(context)
                
                // Create URI using FileProvider for secure file sharing
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                
                // Store URI and launch camera
                imageUri = uri
                cameraLauncher.launch(uri)
            } catch (e: SecurityException) {
                onError(Exception("Camera permission not granted: ${e.message}"))
            } catch (e: Exception) {
                onError(Exception("Failed to launch camera: ${e.message}"))
            }
        } else {
            // Permission denied by user
            onError(Exception("Camera permission denied"))
        }
    }
    
    // Return function that triggers camera capture
    return {
        // Request camera permission first, then launch camera if granted
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }
}

/**
 * Alternative CameraX-based capture function for 1080p images
 * This is a placeholder for future CameraX integration
 * Currently uses the same implementation as useCameraCapture
 * 
 * @param onImageCaptured Callback function called when image is successfully captured
 * @param onError Callback function called when an error occurs during capture
 * @return Function that can be called to trigger camera capture
 */
@Composable
fun useCameraXCapture1080p(
    onImageCaptured: (Uri) -> Unit,
    onError: (Exception) -> Unit
): () -> Unit {
    // Get current Android context
    val context = LocalContext.current
    
    // Create CameraManager instance (remembered to avoid recreation)
    val cameraManager = remember { CameraManager() }
    
    // State to hold the URI of the captured image
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Camera launcher for 1080p image capture using CameraX
    // Note: Currently uses standard camera intent, CameraX integration pending
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        // Check if capture was successful and URI exists
        if (success && imageUri != null) {
            onImageCaptured(imageUri!!)
        } else {
            onError(Exception("Failed to capture image"))
        }
    }
    
    // Permission launcher for camera permission request
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, proceed with camera launch
            try {
                // Create 1080p image file for capture
                val imageFile = cameraManager.createImageFileWith1080p(context)
                
                // Create URI using FileProvider for secure file sharing
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                
                // Store URI and launch camera
                imageUri = uri
                cameraLauncher.launch(uri)
            } catch (e: SecurityException) {
                onError(Exception("Camera permission not granted: ${e.message}"))
            } catch (e: Exception) {
                onError(Exception("Failed to launch camera: ${e.message}"))
            }
        } else {
            // Permission denied by user
            onError(Exception("Camera permission denied"))
        }
    }
    
    // Return function that triggers camera capture
    return {
        // Request camera permission first, then launch camera if granted
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }
}