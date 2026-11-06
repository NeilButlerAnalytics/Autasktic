# Autasktic

Autasktic is a local-first personal task organiser for Android, designed for a
Nothing CMF Phone 1. It uses Kotlin, Jetpack Compose, Room, and Android's
on-device speech recognition.

## Requirements

- Android Studio
- Android SDK platform 35
- JDK 17 or newer
- A Nothing CMF Phone 1 for device testing
- USB debugging enabled on the phone

## Build

From the project directory:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

The debug APK is written to:

`app\build\outputs\apk\debug\app-debug.apk`

Run the unit tests with:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest
```

## Install on the CMF Phone 1

1. Enable Developer options and USB debugging on the phone.
2. Connect the phone with a USB cable.
3. Accept the USB debugging authorization prompt.
4. Confirm the device is visible:

   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
   ```

5. Install and launch the debug build:

   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.autasktic/.MainActivity
   ```

## Current functionality

- Local Room task storage
- Text and on-device voice task creation
- UK-style date and time phrase parsing
- Editing, completion, and postponement
- Swipe-to-complete task cards with directional exit animation
- Colour-matched auto-saving task editing panel
- Icon-based date/time editing with bounded date navigation
- Completed-task visibility
- Backward and forward navigation between dates
- Two-hour timed-task pastel fills
- Local notifications for timed incomplete tasks, including when the app is closed
- Exact timed notifications when Android's Alarms & reminders access is enabled
- Long-press task actions for editing, quick rescheduling, routines, breakdowns,
  reminders, focus, and task status
- Optional task steps, recurring routines, pre-deadline reminders, and a manual focus task
- USB deployment to the CMF Phone 1

## Exact-time notifications

On Android 12 and newer, open Autasktic once and allow its **Alarms & reminders**
access when Android prompts for it. This special access allows timed tasks to use
`setExactAndAllowWhileIdle`. If access is not granted, Android uses its inexact
alarm fallback and delivery may be delayed. Notification permission must also be
allowed on Android 13 and newer.

After enabling the access, create a timed incomplete task a minute or two ahead,
then close the app. The notification should be delivered at the scheduled time,
subject to the phone's battery and notification policies.

See [PROGRESS.md](./PROGRESS.md) for the implementation checklist.
