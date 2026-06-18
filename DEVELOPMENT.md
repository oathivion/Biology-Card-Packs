# WildDeck development guide

WildDeck is a native Android app written in Kotlin with Jetpack Compose.

## Requirements

- Android Studio with JDK 17
- Android SDK Platform 35
- An Android 8.0 (API 26) or newer emulator/device

Android Studio normally supplies the JDK and can install the required SDK from
**Tools → SDK Manager**.

## Run the app

1. Open this repository in Android Studio.
2. Allow the Gradle sync to finish.
3. Select an emulator or connected Android device.
4. Run the `app` configuration.

The first launch starts with an empty collection. Play Food Match and choose
the correct food three times to earn the displayed card.

## Tests

From Android Studio, run the tests in `app/src/test` and `app/src/androidTest`.
From a terminal with JDK 17 and the Android SDK configured:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

The unit suite covers inventory, deck limits, ownership checks, scoring,
symbiosis, mini-game rewards, and frame rules. Instrumented tests cover local
save/load and the required card layout.

## Structure

- `model` contains reusable data classes.
- `domain` contains inventory, deck, scoring, frame, and mini-game rules.
- `data` contains the sample biology catalog and local persistence.
- `ui/components` contains the reusable card UI.
- `ui/screens` contains the six required screens.
- `WildDeckViewModel` connects UI state to the domain and persistence layers.
