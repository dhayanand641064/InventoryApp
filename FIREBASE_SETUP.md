# Firebase Setup Instructions

## Your Firebase Database Configuration

**Database URL**: https://shan001-5b11c-default-rtdb.asia-southeast1.firebasedatabase.app/
**Project ID**: shan001-5b11c

## Common Issues and Solutions

### 1. Parts Not Getting Added to Database

**Most Common Causes:**

1. **Missing or Incorrect google-services.json**
   - Download the correct `google-services.json` from Firebase Console
   - Replace the placeholder file in `app/` directory
   - Make sure the package name matches: `com.example.shan_inventory`
   - **Your project ID should be**: `shan001-5b11c`

2. **Firebase Database Rules**
   - Go to Firebase Console → Realtime Database → Rules
   - Set rules to allow read/write for testing:
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

3. **No Internet Connection**
   - Ensure device has internet connectivity
   - Check if Firebase is accessible from your network

4. **Firebase Project Not Created**
   - Create a new Firebase project at https://console.firebase.google.com/
   - Enable Realtime Database
   - Enable Firebase Storage

### 2. Testing Firebase Connection

Use the "Test FB" button in the app to verify:
- Firebase connection is working
- Database read/write permissions
- Check the success/error messages

### 3. Debug Steps

1. **Check Logs**: Look at Android Studio Logcat for Firebase errors
2. **Test Connection**: Use the "Test FB" button
3. **Verify Configuration**: Ensure google-services.json is correct
4. **Check Rules**: Verify Firebase Database rules allow read/write

### 4. Expected Behavior

When working correctly:
- "Test FB" button shows "Firebase connection successful"
- Adding parts shows "Part Added" message
- Parts appear in the list immediately
- No error messages in red

### 5. Troubleshooting

If parts still don't get added:
1. Check Firebase Console for any error messages
2. Verify the google-services.json file is in the correct location
3. Ensure Firebase project has Realtime Database enabled
4. Check if there are any network restrictions
5. Try the "Test FB" button to isolate the issue

## Quick Setup Checklist

- [ ] Firebase project created
- [ ] Realtime Database enabled
- [ ] Firebase Storage enabled
- [ ] google-services.json downloaded and placed in app/ directory
- [ ] Database rules set to allow read/write
- [ ] App has internet connectivity
- [ ] "Test FB" button shows success message
