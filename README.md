# yokos-android-launcher

A BlackBerry 10–style home launcher for Android 9+ (API 28–34), written in Kotlin and Jetpack Compose.

## What it does

| BB10 feature | How it works on Android |
|---|---|
| **Hub**: one stream for every app's messages | A `NotificationListenerService` records every notification in an on-device history (SQLite), so entries stay after they leave the status bar and across restarts, and each new message in a conversation gets its own entry. The Hub page has an account rail with unread badges, day-grouped entries with category colour stripes, swipe to delete, and a long-press sheet with inline reply, mark read/unread, snooze (15 min / 1 h / 4 h) and delete. History keeps 90 days (up to 5,000 cleared entries); *Clear history* empties it. |
| **Peek** into the Hub from any app | An accessibility service (`BB10 Peek`) draws a thin strip on one screen edge. Drag in from it to slide the Hub over the current app. Let go past 40% (or flick) to open the full Hub. |
| **Active Frames** | The last 8 apps you used, from `UsageStatsManager`, as tinted cards with a close button. You can also pin any app widget as a live frame. |
| **App grid pages** | 4×6 pages from `LauncherApps` (work profile included). Long-press and drag to rearrange, tap in rearrange mode for App info / Uninstall. |
| **Home layout** | Swipe between Hub ← Active Frames ← app pages. Home returns to Active Frames. Bottom bar: Phone · Search · Camera. Search covers apps and Hub entries on the phone, with a Google search on top (the keyboard's search key goes to Google). Swipe down on the app grid to open notifications. |
| **Look** | Black and dark-grey surfaces with the BB10 blue accent. The font is Source Sans 3 (SIL OFL) in place of Slate Pro, which can't be redistributed. |

### Android limits

- The Hub history starts when you grant notification access. Android doesn't give other apps its own notification history, so anything from before then (other than what's still in the status bar) can't be imported.
- Reply and snooze only work while the notification is still in the status bar. Older entries open the app instead.

- Active Frames can't mirror other apps' live screens. Android offers no way to do that without screen capture, so app frames show the icon and last-used time, and widgets serve as live frames.
- Closing a frame only hides it until you use the app again. Launchers can't stop other apps.
- With gesture navigation, the side edges belong to Back. The peek strip asks to be excluded, but Android only grants that for up to 200dp per edge. Keep the strip short (Third or Half), or use 3-button navigation.
- **Android 13+ sideloaded APKs:** notification access and accessibility are "restricted settings". If a switch is greyed out, open *App info → ⋮ → Allow restricted settings* first.

## Set up on a phone

1. Install the debug APK from the **bb10-launcher-debug** artifact of the latest GitHub Actions run.
2. Open the launcher. The Active Frames page shows a *Finish setup* banner. Tap it and grant:
   - **Default home app**
   - **Notification access** (Hub)
   - **Usage access** (Active Frames)
   - **Peek gesture**: turn on *BB10 Peek* under Accessibility, then choose its edge, length and position in the same screen.

Or with adb:

```sh
adb install -r app-debug.apk
adb shell cmd package set-home-activity com.yokos.bb10launcher/.launcher.LauncherActivity
adb shell cmd notification allow_listener com.yokos.bb10launcher/com.yokos.bb10launcher.hub.HubNotificationService
adb shell appops set com.yokos.bb10launcher GET_USAGE_STATS allow
adb shell settings put secure enabled_accessibility_services com.yokos.bb10launcher/com.yokos.bb10launcher.overlay.PeekOverlayService
adb shell settings put secure accessibility_enabled 1

# Post a test notification for the Hub
adb shell cmd notification post -S bigtext -t "Test" hubtest "Hello from adb"
```

## Build

Needs JDK 17 and the Android SDK (platform 34).

```sh
./gradlew assembleDebug lintDebug testDebugUnitTest
```

CI (`.github/workflows/build-bb10.yml`) runs the same tasks on `main`, `develop` and `claude/**` pushes and uploads the APK and reports.

## Code map

```
app/src/main/java/com/yokos/bb10launcher/
  LauncherApp.kt          process-wide repositories (apps, Hub, settings, icons, widgets)
  launcher/               LauncherActivity (HOME), pager root, search, view model
  hub/                    HubRepository (pure), listener service, Hub UI
  frames/                 FrameReducer (pure), usage-stats source, widget host, Frames UI
  apps/                   AppOrdering (pure), LauncherApps repository, grid UI
  overlay/                PeekOverlayService, peek panel, PeekConfig/PeekGesture (pure)
  onboarding/             permission checks and the setup checklist
  settings/               DataStore preferences
  ui/theme/               BB10 colours and typography
```

The `pure` files have no Android dependencies and are covered by JVM unit tests in `app/src/test`.

## Third-party

- Source Sans 3 © Adobe, SIL Open Font License 1.1: see `third_party/source-sans/LICENSE.md`.
