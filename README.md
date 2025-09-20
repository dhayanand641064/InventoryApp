# Parts Inventory App

A modern Android inventory management app built with Jetpack Compose and Firebase.

## Features

- **Splash Screen**: Displays app version (1.0) and release date (Sep 16, 2025)
- **Parts Management**: Add, view, and delete parts with detailed information
- **Camera Integration**: Capture 720p images of parts using device camera
- **Firebase Integration**: Real-time database and cloud storage
- **Modern UI**: Built with Material Design 3 and Jetpack Compose

## Part Information Fields

- Part Name (required)
- Quantity
- Cabinet Name
- Shelf Row
- Shelf Column
- Remarks
- Part Image (captured via camera)

## Setup Instructions

### 1. Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project named "shan-inventory-app"
3. Enable Firebase Realtime Database
4. Enable Firebase Storage
5. Add an Android app with package name: `com.example.shan_inventory`
6. Download the `google-services.json` file and replace the placeholder file in `app/` directory
7. Set up Firebase Realtime Database rules:

```json
{
  "rules": {
    "parts": {
      ".read": true,
      ".write": true
    }
  }
}
```

8. Set up Firebase Storage rules:

```json
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /part_images/{allPaths=**} {
      allow read, write: if true;
    }
  }
}
```

### 2. Build and Run

1. Open the project in Android Studio
2. Sync the project with Gradle files
3. Build and run the app on a device or emulator

## Permissions

The app requires the following permissions:
- `CAMERA`: For capturing part images
- `INTERNET`: For Firebase connectivity
- `READ_EXTERNAL_STORAGE`: For accessing captured images
- `WRITE_EXTERNAL_STORAGE`: For saving captured images

## Technical Details

- **Minimum SDK**: 31 (Android 12)
- **Target SDK**: 36
- **Architecture**: MVVM with Jetpack Compose
- **Database**: Firebase Realtime Database
- **Storage**: Firebase Storage
- **Image Resolution**: 720p
- **UI Framework**: Jetpack Compose with Material Design 3

## Project Structure

```
app/src/main/java/com/example/shan_inventory/
├── data/
│   └── Part.kt                    # Data model
├── service/
│   └── FirebaseService.kt         # Firebase operations
├── ui/screens/
│   ├── SplashScreen.kt           # App splash screen
│   └── PartMainScreen.kt         # Main parts management screen
├── utils/
│   └── CameraManager.kt          # Camera functionality
└── MainActivity.kt               # Main activity with navigation
```

## Usage

1. **Launch**: App starts with a splash screen showing version information
2. **Add Parts**: Tap "Add Part" button to open the form dialog
3. **Fill Details**: Enter part information in the form fields
4. **Capture Image**: Use the camera button to take a photo of the part
5. **Save**: Tap "Add Part" to save the part to Firebase
6. **View Parts**: All parts are displayed in a scrollable list
7. **Delete Parts**: Tap the delete icon on any part to remove it

## Firebase Data Structure

```
parts/
  └── {partId}/
      ├── id: string
      ├── partName: string
      ├── quantity: number
      ├── cabinetName: string
      ├── shelfRow: string
      ├── shelfColumn: string
      ├── remarks: string
      ├── imageUrl: string
      └── createdAt: timestamp
```

## Storage Structure

```
part_images/
  └── {partName}.jpg
```

## Notes

- Images are automatically resized to 720p resolution
- Part names are used as image filenames (spaces replaced with underscores)
- All data is stored in Firebase for real-time synchronization
- The app works offline and syncs when connectivity is restored

