# SmartJoy — Android App

Kotlin + Jetpack Compose Android app for the SmartJoy FYP. Implements:
- **Module 7.1: User Authentication** (Firebase Authentication + Firestore user profiles)
- **Module 7.2: Smart Navigation** (Google Maps + device location, attraction markers)
- **Module 7.3: Wearable Child Tracking** (BLE proximity scanning + pairing + out-of-range alerts)
- **Module 7.4: Danger Zone Alert** (geofenced animal enclosures, live proximity + entry alerts)

## Project structure

```
app/src/main/java/com/smartjoy/app/
  MainActivity.kt          # Nav host: login -> register -> verify_email -> profile -> navigation
  auth/
    AuthViewModel.kt        # Firebase Auth + Firestore logic (login, register, verify, reset, profile)
    AuthUiState.kt           # UI state + UserProfile model
    LoginScreen.kt
    RegisterScreen.kt
    ForgotPasswordScreen.kt
    VerifyEmailScreen.kt
    ProfileScreen.kt
  navigation/
    LocationViewModel.kt     # Fused location provider + permission state
    NavigationScreen.kt      # GoogleMap with current location + attraction markers
    Attraction.kt            # Sample attraction/POI data (placeholder coordinates)
    AttractionInfoCard.kt
  tracking/
    BleScanner.kt            # Wraps BluetoothLeScanner as a Kotlin Flow
    BleDevice.kt              # Device model + RSSI -> proximity level mapping
    ChildTrackingViewModel.kt # Scan/pair/monitor state, persists paired device locally
    ChildTrackingScreen.kt    # Pairing list + live proximity monitoring UI
    NotificationHelper.kt     # Out-of-range alert notification
  danger/
    DangerZone.kt              # Zone model + sample geofences (matches Attraction.kt coordinates)
    DangerZoneViewModel.kt      # Continuous location updates, distance-to-zone, enter/exit tracking
    DangerZoneScreen.kt         # Live map with zone circles + per-zone distance list
    DangerZoneNotificationHelper.kt # Entry alert notification
  ui/theme/                  # Compose color scheme + theme
```

The Gradle project (wrapper, build scripts) was verified to compile and package
(`./gradlew :app:assembleDebug` succeeds) against Android SDK 35 / minSdk 26.

## Required setup before first run

`app/google-services.json` is already committed to this repo and points at the shared project
Firebase project (`smartjoy-ac297`), with Email/Password auth and Firestore already enabled — you
don't need to create your own Firebase project or download anything from the console. Just clone and
add a Maps API key (below).

### 1. Add a Google Maps API key
1. Go to [Google Cloud Console](https://console.cloud.google.com), use the same project Firebase created
   (or any project), and enable the **Maps SDK for Android** API.
2. Create an API key under **APIs & Services → Credentials**. Restrict it to Android apps with
   package `com.smartjoy.app` (and your debug SHA-1, found via `./gradlew signingReport`).
3. Add it to `local.properties` (already gitignored, never committed) at the project root:
   ```
   MAPS_API_KEY=your_key_here
   ```
   The build reads this and injects it into the manifest automatically — no code changes needed.

### 2. Open in Android Studio
- Open the `SmartJoy` folder as a project (not the `app` subfolder).
- Let Gradle sync (it will use the wrapper already configured, Gradle 8.7 / AGP 8.5.2).
- Run on an emulator or device (minSdk 26 / Android 8.0+). Grant location permission when prompted
  to see your position on the map.

## What's implemented
**Authentication**
- Register (name, email, password) → creates Firebase Auth user + Firestore `users/{uid}` doc, sends verification email
- Email verification required before reaching the app (dedicated screen, resend + recheck)
- Login, with friendly error messages for common failure cases
- Forgot-password flow (real reset email via Firebase)
- Profile screen: view/edit name, verified-status indicator, logout
- Session persists across app restarts via `FirebaseAuth.currentUser`
- Firestore rules (`firestore.rules`) restrict each user to reading/writing only their own document

**Smart Navigation**
- Google Map centered on the park, with sample attraction/animal-enclosure markers (tap for info card)
- Requests location permission at runtime, shows current position ("my location" blue dot)
- Recenter-on-me floating action button
- Attraction coordinates are placeholder data — swap `sampleAttractions` in `Attraction.kt` for real park coordinates when available

**Wearable Child Tracking**
- Scans for nearby BLE devices and lets a parent pair one as the "child's wearable" (persisted locally via SharedPreferences, survives app restart)
- Live proximity monitoring using signal strength (RSSI), bucketed into Near / Safe Distance / Getting Far / Out of Range
- Sends a high-priority local notification when the paired device moves from a safe state into Far/Out of Range
- Handles Android 12+ runtime Bluetooth permissions (`BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT`) and Android 13+ notification permission
- **Caveat**: no real wearable hardware exists yet, so this was built and compiled against the BLE APIs but not tested against a physical BLE peripheral. RSSI-based distance is inherently approximate (affected by obstacles/orientation) — treat the proximity thresholds in `BleDevice.kt` as a starting point to tune once real hardware is available. To test with what you have, any BLE-advertising device (a phone with a BLE beacon app, wireless earbuds, a fitness band) will show up in the scan list and can be "paired" to exercise the flow.

**Danger Zone Alert**
- Live map showing each restricted zone as a colored circle (green = safe, amber = approaching, red = inside), matching the animal enclosures from Smart Navigation
- Continuous GPS monitoring (not just a one-shot fetch) while the screen is open, updating every ~3 seconds
- Distance-to-zone list with a Safe / Approaching / Inside status per zone
- Full-width warning banner + high-priority notification the moment you cross into a zone — the notification only fires once per entry (not spammed every GPS update), and again if you leave and re-enter
- Zone coordinates/radii are placeholder data in `DangerZone.kt` — swap for real park coordinates alongside `Attraction.kt` when available
- **Same hardware caveat as Child Tracking**: verified by compiling against the location APIs and reasoning through the entry/exit logic, but not field-tested by physically walking into a geofenced radius. To sanity-check it, you can temporarily set a zone's `center` to your current real-world coordinates (e.g. from Google Maps) and radius to something like 50m, then walk toward that spot.

## Not yet implemented (other modules from the proposal)
AI Animal Recognition (TensorFlow Lite), Interactive Quiz.
