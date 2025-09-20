package com.example.shan_inventory.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.shan_inventory.data.Part
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*

/**
 * FirebaseService class handles all Firebase operations for the Parts Inventory app
 * 
 * This service class provides a centralized interface for all Firebase-related operations:
 * - Firebase Realtime Database CRUD operations for parts management
 * - Firebase Storage operations for image upload/download/delete
 * - Connection testing and error handling
 * - Image processing and optimization
 * 
 * The service is designed to work with the specific Firebase project:
 * - Database: shan001-5b11c (Asia Southeast 1 region)
 * - Storage: gs://shan001-5b11c.firebasestorage.app
 * 
 * All operations are implemented as suspend functions for coroutine compatibility
 * and return Result<T> for proper error handling
 */
class FirebaseService {
    /**
     * Firebase Realtime Database reference for the specific project
     * Points to the root of the database for the shan001-5b11c project
     * Located in Asia Southeast 1 region for optimal performance
     */
    private val database: DatabaseReference = FirebaseDatabase.getInstance("https://shan001-5b11c-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
    
    /**
     * Firebase Storage reference for the specific project bucket
     * Points to the root of the storage bucket for the shan001-5b11c project
     * Used for storing part images and other media files
     */
    private val storage: StorageReference = FirebaseStorage.getInstance("gs://shan001-5b11c.firebasestorage.app").reference
    
    /**
     * Reference to the "parts" node in the database for easier access
     * All part-related operations use this reference for consistency
     * Structure: /parts/{partId}/partData
     */
    private val partsRef: DatabaseReference = database.child("parts")
    
    /**
     * Adds a new part to the Firebase Realtime Database
     * Generates a unique ID for the part and stores it in the "parts" node
     * 
     * @param part The Part object to be added to the database
     * @return Result containing the generated part ID on success, or error on failure
     */
    suspend fun addPart(part: Part): Result<String> {
        return try {
            // Generate unique ID using Firebase push key or UUID fallback
            val partId = partsRef.push().key ?: UUID.randomUUID().toString()
            
            // Create part object with the generated ID
            val partWithId = part.copy(id = partId)
            
            // Store the part in Firebase Realtime Database
            partsRef.child(partId).setValue(partWithId).await()
            
            // Return success with the generated ID
            Result.success(partId)
        } catch (e: Exception) {
            // Return failure with error message
            Result.failure(Exception("Failed to add part to database: ${e.message}"))
        }
    }
    
    /**
     * Updates an existing part in the Firebase Realtime Database
     * Replaces the entire part object with the updated version
     * 
     * @param part The Part object with updated information
     * @return Result indicating success or failure of the update operation
     */
    suspend fun updatePart(part: Part): Result<Unit> {
        return try {
            // Update the part in Firebase Realtime Database using its ID
            partsRef.child(part.id).setValue(part).await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Return the exception as failure
            Result.failure(e)
        }
    }
    
    /**
     * Deletes a part from the Firebase Realtime Database and its associated images from Storage
     * First retrieves the part to get image URLs, then deletes images from Storage,
     * and finally removes the part from the Database
     * 
     * @param partId The unique ID of the part to be deleted
     * @return Result indicating success or failure of the delete operation
     */
    suspend fun deletePart(partId: String): Result<Unit> {
        return try {
            // First get the part to access its associated images
            val partSnapshot = partsRef.child(partId).get().await()
            if (partSnapshot.exists()) {
                val part = partSnapshot.getValue(Part::class.java)
                if (part != null) {
                    // Collect all image URLs to delete from storage
                    val imagesToDelete = mutableListOf<String>()
                    
                    // Add single imageUrl if it exists (backward compatibility)
                    if (part.imageUrl.isNotBlank()) {
                        imagesToDelete.add(part.imageUrl)
                    }
                    
                    // Add multiple imageUrls if they exist (current implementation)
                    if (part.imageUrls.isNotEmpty()) {
                        imagesToDelete.addAll(part.imageUrls)
                    }
                    
                    // Delete all associated images from Firebase Storage
                    for (imageUrl in imagesToDelete) {
                        try {
                            deleteImage(imageUrl)
                            println("FirebaseService: Successfully deleted image: $imageUrl")
                        } catch (e: Exception) {
                            println("FirebaseService: Failed to delete image $imageUrl: ${e.message}")
                            // Continue with other images even if one fails
                        }
                    }
                }
            }
            
            // Delete the part from Firebase Realtime Database
            partsRef.child(partId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Returns a reference to the "parts" node in Firebase Realtime Database
     * This reference can be used to set up real-time listeners for parts data
     * 
     * @return DatabaseReference pointing to the "parts" node
     */
    fun getAllParts(): DatabaseReference {
        return partsRef
    }
    
    /**
     * Resizes an image to 360p resolution while maintaining aspect ratio
     * This is a legacy function that's no longer used since we now capture at 1080p
     * Kept for potential future use or reference
     * 
     * @param context Android context for accessing content resolver
     * @param imageUri URI of the image to be resized
     * @return ByteArray containing the resized image data, or null if failed
     */
    private fun resizeImageTo360p(context: Context, imageUri: Uri): ByteArray? {
        return try {
            // Open input stream from the image URI
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (originalBitmap == null) {
                println("FirebaseService: Failed to decode image")
                return null
            }
            
            // Get original image dimensions
            val originalWidth = originalBitmap.width
            val originalHeight = originalBitmap.height
            
            // Calculate new dimensions for 360p while maintaining aspect ratio
            val newHeight = 360
            val newWidth = (originalWidth * newHeight) / originalHeight
            
            println("FirebaseService: Resizing image from ${originalWidth}x${originalHeight} to ${newWidth}x${newHeight}")
            
            // Create scaled bitmap
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            originalBitmap.recycle() // Free memory
            
            // Convert to JPEG with 85% quality for compression
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            resizedBitmap.recycle() // Free memory
            
            val compressedImage = outputStream.toByteArray()
            outputStream.close()
            
            println("FirebaseService: Image resized successfully, size: ${compressedImage.size} bytes")
            compressedImage
        } catch (e: Exception) {
            println("FirebaseService: Image resize failed: ${e.message}")
            null
        }
    }
    
    /**
     * Uploads a 1080p image to Firebase Storage in the parts_inventory_01 folder
     * 
     * This method handles the complete image upload process:
     * - Reads image data from the provided URI
     * - Maintains original 1080p resolution without compression
     * - Creates a standardized filename based on part name
     * - Uploads to organized folder structure in Firebase Storage
     * - Returns the public download URL for the uploaded image
     * 
     * Storage structure: parts_inventory_01/{partName}.jpg
     * 
     * @param context Android context for accessing content resolver
     * @param imageUri URI of the image to be uploaded (from camera capture)
     * @param partName Name of the part (used for file naming, spaces replaced with underscores)
     * @return Result containing the download URL on success, or error on failure
     */
    suspend fun uploadImage(context: Context, imageUri: Uri, partName: String): Result<String> {
        return try {
            // Create standardized filename by replacing spaces with underscores and adding .jpg extension
            // This ensures consistent naming and prevents issues with Firebase Storage
            val fileName = "${partName.replace(" ", "_")}.jpg"
            
            // Create reference to the storage location in the parts_inventory_01 folder
            // This organizes all part images in a dedicated folder
            val imageRef = storage.child("parts_inventory_01/$fileName")
            
            println("FirebaseService: Starting upload to parts_inventory_01/$fileName")
            println("FirebaseService: Image URI: $imageUri")
            
            // Read image data from URI while maintaining 1080p resolution
            // No compression or resizing is applied to preserve image quality
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val imageData = inputStream?.readBytes()
            inputStream?.close()
            
            if (imageData == null) {
                return Result.failure(Exception("Failed to read image data"))
            }
            
            println("FirebaseService: Uploading 1080p image in same resolution, size: ${imageData.size} bytes")
            
            // Upload image data to Firebase Storage using the putBytes method
            // This is more efficient than uploading from file URI
            val uploadTask = imageRef.putBytes(imageData).await()
            println("FirebaseService: Upload completed, getting download URL...")
            
            // Get the public download URL for the uploaded image
            // This URL can be used to display the image in the app
            val downloadUrl = imageRef.downloadUrl.await()
            println("FirebaseService: Download URL: $downloadUrl")
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            println("FirebaseService: Upload failed: ${e.message}")
            Result.failure(Exception("Storage upload failed: ${e.message}"))
        }
    }
    
    /**
     * Deletes an image from Firebase Storage using its download URL
     * 
     * @param imageUrl The download URL of the image to be deleted
     * @return Result indicating success or failure of the delete operation
     */
    suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return try {
            // Get storage reference from the download URL
            val imageRef = FirebaseStorage.getInstance("gs://shan001-5b11c.firebasestorage.app").getReferenceFromUrl(imageUrl)
            
            // Delete the image from storage
            imageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Tests Firebase Realtime Database connectivity
     * Writes and deletes a test value to verify database connection
     * 
     * @return Result containing success message or error details
     */
    suspend fun testConnection(): Result<String> {
        return try {
            // Create a test reference in the database
            val testRef = database.child("test")
            
            // Write a test value
            testRef.setValue("connection_test").await()
            
            // Remove the test value
            testRef.removeValue().await()
            
            Result.success("Firebase connection successful to shan001-5b11c")
        } catch (e: Exception) {
            Result.failure(Exception("Firebase connection failed: ${e.message}"))
        }
    }
    
    /**
     * Tests Firebase Storage connectivity
     * Uploads and deletes a test file to verify storage connection
     * 
     * @return Result containing success message or error details
     */
    suspend fun testStorageConnection(): Result<String> {
        return try {
            // Create a test file reference in the parts_inventory_01 folder
            val testRef = storage.child("parts_inventory_01/test.txt")
            
            // Create test data
            val testData = "test_storage_connection".toByteArray()
            
            // Upload test file
            testRef.putBytes(testData).await()
            
            // Delete test file
            testRef.delete().await()
            
            Result.success("Firebase Storage connection successful to gs://shan001-5b11c.firebasestorage.app/parts_inventory_01")
        } catch (e: Exception) {
            Result.failure(Exception("Firebase Storage connection failed: ${e.message}"))
        }
    }
}
