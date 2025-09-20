package com.example.shan_inventory.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.shan_inventory.data.Part
import com.example.shan_inventory.service.FirebaseService
import com.example.shan_inventory.utils.useCameraCapture
import com.example.shan_inventory.utils.AudioManager
import com.example.shan_inventory.utils.rememberAudioManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

@Composable
fun RemarksFieldWithMicrophone(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 3,
    isListening: Boolean = false,
    onMicrophoneClick: () -> Unit
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                modifier = Modifier.weight(1f),
                maxLines = maxLines,
                trailingIcon = {
                    IconButton(
                        onClick = onMicrophoneClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isListening) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else Color.Transparent,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Close else Icons.Default.Mic,
                            contentDescription = if (isListening) "Stop Recording" else "Start Recording",
                            tint = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
        if (isListening) {
            Text(
                text = "Listening... Speak now",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * PartMainScreen is the main interface for the Parts Inventory application
 * 
 * This screen provides comprehensive parts management functionality:
 * - Display list of all parts with search and filter capabilities
 * - Add new parts with form validation and image capture
 * - Edit existing parts with pre-populated form data
 * - Delete parts with confirmation dialog
 * - Speech recognition for remarks field input
 * - Multiple image capture and management (up to 5 images per part)
 * - Real-time Firebase synchronization
 * - Error handling and loading states
 * 
 * The screen uses Jetpack Compose with Material Design 3 components
 * and follows MVVM architecture patterns with reactive state management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartMainScreen() {
    // Get Android context for various operations
    val context = LocalContext.current
    
    // Initialize services and managers
    val firebaseService = remember { FirebaseService() }
    val coroutineScope = rememberCoroutineScope()
    val audioManager = rememberAudioManager(context)
    
    // State management for parts data
    var parts by remember { mutableStateOf<List<Part>>(emptyList()) }
    var filteredParts by remember { mutableStateOf<List<Part>>(emptyList()) }
    
    // Dialog and UI state management
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var partToEdit by remember { mutableStateOf<Part?>(null) }
    
    // Loading and error state management
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Search functionality state
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    
    // Form field state management
    var partName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var cabinetName by remember { mutableStateOf("") }
    var shelfRow by remember { mutableStateOf("") }
    var shelfColumn by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    
    // Image capture state management
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) } // Single image (legacy)
    var capturedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) } // Multiple images (current)
    
    // Audio manager state collection for speech recognition
    val isListening by audioManager.isListening.collectAsStateWithLifecycle()
    val recognizedText by audioManager.recognizedText.collectAsStateWithLifecycle()
    val audioErrorMessage by audioManager.errorMessage.collectAsStateWithLifecycle()
    
    // Update remarks field when speech recognition completes
    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotEmpty()) {
            remarks = recognizedText
            audioManager.clearRecognizedText()
        }
    }
    
    // Display audio error messages to user
    LaunchedEffect(audioErrorMessage) {
        audioErrorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            audioManager.clearError()
        }
    }
    
    /**
     * Filter parts list based on search query
     * Performs case-insensitive search on part names
     * 
     * @param partsList The complete list of parts to filter
     * @param query The search query string
     */
    fun filterParts(partsList: List<Part>, query: String) {
        if (query.isBlank()) {
            filteredParts = partsList
        } else {
            filteredParts = partsList.filter { part ->
                part.partName.contains(query, ignoreCase = true)
            }
        }
    }
    
    // Load parts from Firebase Realtime Database with real-time updates
    LaunchedEffect(Unit) {
        firebaseService.getAllParts().addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Convert Firebase snapshot to list of Part objects
                val partsList = mutableListOf<Part>()
                for (child in snapshot.children) {
                    child.getValue(Part::class.java)?.let { part ->
                        partsList.add(part)
                    }
                }
                parts = partsList
                // Update filtered parts when parts list changes
                filterParts(parts, searchQuery)
            }
            
            override fun onCancelled(error: DatabaseError) {
                errorMessage = "Failed to load parts: ${error.message}"
            }
        })
    }
    
    // Update filtered parts when search query changes
    LaunchedEffect(searchQuery) {
        filterParts(parts, searchQuery)
    }
    
    // Camera capture function with multiple image support
    val captureImage = useCameraCapture(
        onImageCaptured = { uri ->
            // Check if we haven't reached the maximum of 5 images
            if (capturedImageUris.size < 5) {
                capturedImageUris = capturedImageUris + uri
                capturedImageUri = uri // Keep for backward compatibility
                errorMessage = null // Clear any previous errors
                Toast.makeText(context, "Photo captured successfully! (${capturedImageUris.size}/5)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Maximum 5 images allowed!", Toast.LENGTH_SHORT).show()
            }
        },
        onError = { error ->
            errorMessage = "Camera error: ${error.message}"
        }
    )

    
    /**
     * Clear all form fields and reset to initial state
     * Used after successful part addition or when canceling dialogs
     */
    fun clearForm() {
        partName = ""
        quantity = ""
        cabinetName = ""
        shelfRow = ""
        shelfColumn = ""
        remarks = ""
        capturedImageUri = null
        capturedImageUris = emptyList()
    }
    
    /**
     * Add a new part to the inventory
     * 
     * This function handles the complete part addition process:
     * 1. Validates required fields (part name)
     * 2. Uploads all captured images to Firebase Storage
     * 3. Creates Part object with all form data
     * 4. Saves part to Firebase Realtime Database
     * 5. Resets form and shows success message
     * 
     * Supports up to 5 images per part with individual upload progress
     * Includes timeout handling for network operations
     */
    fun addPart() {
        // Validate required fields
        if (partName.isBlank()) {
            errorMessage = "Part name is required"
            return
        }
        
        isLoading = true
        errorMessage = null
        
        coroutineScope.launch {
            try {
                withTimeout(60000) { // 60 second timeout for multiple images
                    val imageUrls = mutableListOf<String>()
                    
                    // Upload all captured images to Firebase Storage
                    if (capturedImageUris.isNotEmpty()) {
                        errorMessage = "Uploading ${capturedImageUris.size} image(s)..."
                        for ((index, uri) in capturedImageUris.withIndex()) {
                            val uploadResult = firebaseService.uploadImage(context, uri, "${partName}_${index + 1}")
                            if (uploadResult.isSuccess) {
                                val imageUrl = uploadResult.getOrNull() ?: ""
                                imageUrls.add(imageUrl)
                                errorMessage = "Uploaded ${index + 1}/${capturedImageUris.size} images..."
                            } else {
                                errorMessage = "Failed to upload image ${index + 1}: ${uploadResult.exceptionOrNull()?.message}"
                                isLoading = false
                                return@withTimeout
                            }
                        }
                    }
                    
                    // Create Part object with all form data and image URLs
                    val newPart = Part(
                        partName = partName,
                        quantity = quantity.toIntOrNull() ?: 0,
                        cabinetName = cabinetName,
                        shelfRow = shelfRow,
                        shelfColumn = shelfColumn,
                        remarks = remarks,
                        imageUrl = imageUrls.firstOrNull() ?: "", // First image for backward compatibility
                        imageUrls = imageUrls
                    )
                    
                    // Save part to Firebase Realtime Database
                    errorMessage = "Adding part to database..."
                    val addResult = firebaseService.addPart(newPart)
                    if (addResult.isSuccess) {
                        // Reset form and close dialog on success
                        clearForm()
                        showAddDialog = false
                        isLoading = false
                        errorMessage = null
                        val imageCount = imageUrls.size
                        Toast.makeText(context, "Part Added with $imageCount image(s)!", Toast.LENGTH_SHORT).show()
                    } else {
                        errorMessage = "Failed to add part: ${addResult.exceptionOrNull()?.message}"
                        isLoading = false
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                errorMessage = "Operation timed out. Please check your internet connection."
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Unexpected error: ${e.message}"
                isLoading = false
            }
        }
    }
    
    /**
     * Delete a part from the inventory
     * 
     * This function handles the complete part deletion process:
     * 1. Deletes the part from Firebase Realtime Database
     * 2. Deletes all associated images from Firebase Storage
     * 3. Shows success/error messages to the user
     * 
     * @param partId The unique ID of the part to be deleted
     */
    fun deletePart(partId: String) {
        coroutineScope.launch {
            try {
                errorMessage = "Deleting part and associated images..."
                val result = firebaseService.deletePart(partId)
                if (result.isSuccess) {
                    errorMessage = null
                    Toast.makeText(context, "Part and images deleted successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    errorMessage = "Failed to delete part: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                errorMessage = "Error deleting part: ${e.message}"
            }
        }
    }
    
    /**
     * Prepare part for editing by populating form fields
     * 
     * This function:
     * 1. Sets the part to edit in state
     * 2. Populates all form fields with existing part data
     * 3. Opens the edit dialog
     * 
     * Note: Images are not editable in the current implementation
     * 
     * @param part The Part object to be edited
     */
    fun editPart(part: Part) {
        partToEdit = part
        partName = part.partName
        quantity = part.quantity.toString()
        cabinetName = part.cabinetName
        shelfRow = part.shelfRow
        shelfColumn = part.shelfColumn
        remarks = part.remarks
        showEditDialog = true
    }
    
    /**
     * Update an existing part in the inventory
     * 
     * This function handles the part update process:
     * 1. Validates all required fields are filled
     * 2. Creates updated Part object with new data
     * 3. Saves changes to Firebase Realtime Database
     * 4. Resets form and closes dialog on success
     * 
     * Note: Images are not editable in the current implementation
     * The existing images are preserved during updates
     */
    fun updatePart() {
        // Validate all required fields
        if (partName.isBlank() || quantity.isBlank() || cabinetName.isBlank() || 
            shelfRow.isBlank() || shelfColumn.isBlank()) {
            errorMessage = "Please fill in all required fields"
            return
        }
        
        val partToUpdate = partToEdit ?: return
        val quantityValue = quantity.toIntOrNull() ?: 0
        
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                withTimeout(15000) {
                    // Create updated part object with new form data
                    val updatedPart = partToUpdate.copy(
                        partName = partName,
                        quantity = quantityValue,
                        cabinetName = cabinetName,
                        shelfRow = shelfRow,
                        shelfColumn = shelfColumn,
                        remarks = remarks
                        // Keep existing imageUrl and imageUrls - images are not editable
                    )
                    
                    // Save updated part to Firebase
                    val result = firebaseService.updatePart(updatedPart)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Part Updated", Toast.LENGTH_SHORT).show()
                        clearForm()
                        showEditDialog = false
                        partToEdit = null
                    } else {
                        errorMessage = "Failed to update part: ${result.exceptionOrNull()?.message}"
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to update part: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Parts Inventory Title
        Text(
            text = "Parts Inventory",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search button that transforms into text field
        if (!showSearchBar) {
            Button(
                onClick = { showSearchBar = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50) // Green color
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search Parts")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search Parts")
            }
        } else {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by part name") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    IconButton(
                        onClick = { 
                            searchQuery = ""
                            showSearchBar = false
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Add Part button
        Button(
            onClick = { showAddDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50) // Green color
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
             Icon(Icons.Default.Add, contentDescription = "Add Part")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Part")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Error message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        
        // Parts list
        if (filteredParts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) {
                        "No parts found matching '$searchQuery'"
                    } else {
                        "No parts found. Add your first part!"
                    },
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredParts) { part ->
                    PartItem(
                        part = part,
                        onEdit = { editPart(part) },
                        onDelete = { deletePart(part.id) }
                    )
                }
            }
        }
    }
    
    // Add Part Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Part") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = partName,
                        onValueChange = { 
                            partName = it
                            errorMessage = null
                        },
                        label = { Text("Part Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { 
                            quantity = it.filter { char -> char.isDigit() }
                            errorMessage = null
                        },
                        label = { Text("Qty *") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    OutlinedTextField(
                        value = cabinetName,
                        onValueChange = { 
                            cabinetName = it.filter { char -> char.isDigit() }
                            errorMessage = null
                        },
                        label = { Text("Cab *") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = shelfRow,
                            onValueChange = { 
                                shelfRow = it.filter { char -> char.isDigit() }
                                errorMessage = null
                            },
                            label = { Text("Row *") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        
                        OutlinedTextField(
                            value = shelfColumn,
                            onValueChange = { 
                                shelfColumn = it.filter { char -> char.isDigit() }
                                errorMessage = null
                            },
                            label = { Text("Column *") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    
                    RemarksFieldWithMicrophone(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = "Remarks",
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        isListening = isListening,
                        onMicrophoneClick = {
                            if (isListening) {
                                audioManager.stopListening()
                            } else {
                                audioManager.startListening()
                            }
                        }
                    )
                    
                    // Image capture section
                    Column {
                        Text(
                            text = "Part Image",
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = captureImage,
                                enabled = capturedImageUris.size < 5,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50) // Green color
                                )
                            ) {
                                 Icon(Icons.Default.CameraAlt, contentDescription = "Take Photo")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Take Photo (${capturedImageUris.size}/5)")
                            }
                            
                            // Display captured images
                            if (capturedImageUris.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(capturedImageUris) { uri ->
                                        val index = capturedImageUris.indexOf(uri)
                                        Box {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(uri)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Captured Image ${index + 1}",
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            
                                            // Remove photo button
                                            IconButton(
                                                onClick = { 
                                                    capturedImageUris = capturedImageUris.filter { it != uri }
                                                },
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .align(Alignment.TopEnd)
                                                    .background(
                                                        MaterialTheme.colorScheme.error,
                                                        RoundedCornerShape(10.dp)
                                                    )
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove Photo",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { addPart() },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50) // Green color
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Add Part")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Edit Part Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { 
                showEditDialog = false
                partToEdit = null
                clearForm()
            },
            title = { Text("Edit Part") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = partName,
                        onValueChange = { 
                            partName = it
                            errorMessage = null
                        },
                        label = { Text("Part Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { 
                            quantity = it.filter { char -> char.isDigit() }
                            errorMessage = null
                        },
                        label = { Text("Qty *") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    OutlinedTextField(
                        value = cabinetName,
                        onValueChange = { 
                            cabinetName = it.filter { char -> char.isDigit() }
                            errorMessage = null
                        },
                        label = { Text("Cab *") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    OutlinedTextField(
                        value = shelfRow,
                        onValueChange = { 
                            shelfRow = it.filter { char -> char.isDigit() }
                            errorMessage = null
                        },
                        label = { Text("Row *") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    OutlinedTextField(
                        value = shelfColumn,
                        onValueChange = { 
                            shelfColumn = it.filter { char -> char.isDigit() }
                            errorMessage = null
                        },
                        label = { Text("Column *") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    RemarksFieldWithMicrophone(
                        value = remarks,
                        onValueChange = { 
                            remarks = it
                            errorMessage = null
                        },
                        label = "Remarks",
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        isListening = isListening,
                        onMicrophoneClick = {
                            if (isListening) {
                                audioManager.stopListening()
                            } else {
                                audioManager.startListening()
                            }
                        }
                    )
                    
                }
            },
            confirmButton = {
                Button(
                    onClick = { updatePart() },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50) // Green color
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Update Part")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showEditDialog = false
                        partToEdit = null
                        clearForm()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ZoomableImage(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3f)
        offset += offsetChange
    }
    
    Box(
        modifier = modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
            .transformable(state = transformableState)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Zoomable Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun PartItem(
    part: Part,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImageZoom by remember { mutableStateOf(false) }
    var showExpandedText by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)) // Very light green
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Edit and Delete buttons on the left
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Part name field - expandable
                    if (!showExpandedText) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(40.dp)
                                .background(
                                    color = Color(0xFFE3F2FD),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF1976D2),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { showExpandedText = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${part.partName} (${part.quantity})",
                                fontSize = 14.sp,
                                color = Color(0xFF1976D2),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        // Expanded text box
                        Card(
                            modifier = Modifier
                                .width(200.dp)
                                .clickable { showExpandedText = false },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1976D2))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = "Part Name:",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1976D2),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = part.partName.take(200), // Limit to 200 characters
                                    fontSize = 14.sp,
                                    color = Color(0xFF1976D2),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Quantity: ${part.quantity}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1976D2),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = "Tap to close",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    
                    // Cabinet field
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(40.dp)
                            .background(
                                color = Color(0xFFE3F2FD),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFF1976D2),
                                shape = RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cab : ${part.cabinetName}",
                            fontSize = 14.sp,
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Row and Column field
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(40.dp)
                            .background(
                                color = Color(0xFFE3F2FD),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFF1976D2),
                                shape = RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "R:${part.shelfRow} C:${part.shelfColumn}",
                            fontSize = 14.sp,
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Edit and Delete buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    color = Color(0xFFE3F2FD),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF1976D2),
                                    shape = RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(60.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    color = Color(0xFFE3F2FD),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF1976D2),
                                    shape = RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.size(60.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                
                // Spacer to push image to the right
                Spacer(modifier = Modifier.weight(1f))
                
                // Large image on the right - always show placeholder
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    val displayImageUrl = if (part.imageUrls.isNotEmpty()) part.imageUrls.first() else part.imageUrl
                    if (displayImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(displayImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Part Image",
                            modifier = Modifier
                                .size(240.dp) // 4 times larger (60dp * 4 = 240dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showImageZoom = true },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Image placeholder when no image is available
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .background(
                                    color = Color(0xFFE3F2FD), // Light blue background
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF1976D2), // Blue border
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Image",
                                fontSize = 16.sp,
                                color = Color(0xFF1976D2),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Part") },
            text = { Text("Are you sure you want to delete \"${part.partName}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Image zoom dialog
    val allImages = if (part.imageUrls.isNotEmpty()) part.imageUrls else listOf(part.imageUrl).filter { it.isNotBlank() }
    if (showImageZoom && allImages.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showImageZoom = false },
            title = { 
                Text(
                    text = "Part Images: ${part.partName} (${allImages.size} image${if (allImages.size > 1) "s" else ""})",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (allImages.size == 1) {
                    // Single zoomable image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        ZoomableImage(
                            imageUrl = allImages[0],
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    // Multiple images with pager and zoom
                    val pagerState = rememberPagerState(pageCount = { allImages.size })
                    Column {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth()
                        ) { page ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(500.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                ZoomableImage(
                                    imageUrl = allImages[page],
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        
                        // Page indicator
                        if (allImages.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(allImages.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) 
                                        Color(0xFF4CAF50) else Color.Gray
                                    Box(
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showImageZoom = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50) // Green color
                    )
                ) {
                    Text("Close")
                }
            }
        )
    }
}
