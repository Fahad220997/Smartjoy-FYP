# How to Run SmartJoy

This is an Android app (Kotlin + Jetpack Compose) with Firebase Authentication and Firestore.
Follow these steps to get it running on your machine.

## 1. Prerequisites

- **Android Studio** (latest stable) — [download here](https://developer.android.com/studio)
  - This includes the Android SDK and an emulator. During first install, let it install SDK Platform 35 and an emulator image.

## 2. Clone the project

```
git clone https://github.com/hamzaalie/smartjoy.git
```

Open the `smartjoy` folder in Android Studio (File → Open → select the folder, **not** a specific file).
Let Gradle sync automatically — it may take a few minutes the first time (downloads Gradle 8.7 + dependencies).

Firebase is already configured — `app/google-services.json` is committed to the repo and points at the
shared project, with Email/Password auth and Firestore already set up. You don't need to create your own
Firebase project or download anything from the console.

## 3. Add a Google Maps API key (for the Navigation screen)

1. Go to [console.cloud.google.com](https://console.cloud.google.com) — you can use the same project
   Firebase just created for you.
2. Search for and enable the **"Maps SDK for Android"** API.
3. Go to **APIs & Services → Credentials → Create Credentials → API key**.
4. Open a terminal in the project folder and run `./gradlew signingReport` — copy the SHA-1 under the
   `debug` variant.
5. Click into your new key → restrict it to Android apps → add `com.smartjoy.app` with that SHA-1.
6. Open (or create) `local.properties` in the project root and add this line:
   ```
   MAPS_API_KEY=your_key_here
   ```
   (This file is gitignored — your key stays local and never gets committed.)

If you skip this step, the app still builds and runs fine — you just won't see the map tiles load on
the Navigation screen.

## 4. Run the app

- In Android Studio, create or select a device:
  - **Emulator**: Tools → Device Manager → create a virtual device (e.g. Pixel 6, API 34+)
  - **Physical phone**: enable Developer Options + USB debugging, plug in via USB
- Click the green **Run ▶** button (or `Shift+F10`).
- The app should install and launch, showing the SmartJoy login screen.

## 5. Try it out

- Tap **"Don't have an account? Register"**, sign up with a real email you can check.
- You'll be asked to verify your email (check your inbox for the Firebase verification link).
- After clicking the link, go back to the app and tap **"I've Verified, Continue"**.
- You'll land on the Profile screen — try editing your name, logging out, and logging back in.
- "Forgot password?" on the login screen sends a real password-reset email too.
- Tap **"Open Smart Navigation"** on the Profile screen — grant location permission when asked, and
  you should see a map with a few sample attraction markers (tap one for details) and your current
  position if the Maps key is set up.
- Tap **"Open Child Tracking"** on the Profile screen — grant Bluetooth (and notification) permission
  when asked. You won't have an actual SmartJoy wearable, but any nearby BLE-advertising device
  (phone, earbuds, fitness band with Bluetooth on) will show up in the scan list — tap "Pair" on one
  to try the monitoring view, then walk away from it to see the proximity status change and trigger
  the out-of-range notification.
- Tap **"Open Danger Zone Alerts"** on the Profile screen — grant location permission when asked. You'll
  see a map with two red/amber/green circles (sample restricted zones) and a live-updating list showing
  your distance to each. If you're not physically near either sample zone you'll just see "Safe" for
  both, which is expected — the zones use placeholder coordinates, not your real location.

## Troubleshooting

- **Gradle sync fails / can't find SDK** → Android Studio should prompt you to install missing SDK components; accept it. If not, go to File → Settings → Languages & Frameworks → Android SDK and confirm SDK Platform 35 is installed.
- **Verification/reset emails not arriving** → check spam folder; Firebase sends from `noreply@smartjoy-ac297.firebaseapp.com`.
- **Map screen is blank/grey** → your Maps API key is missing, invalid, or not restricted to the right SHA-1/package — double check step 3.
