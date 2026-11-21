# Android 10 and higher

Question: does the app work well on Android 10 (API 29) and newer?

The app is built with `minSdk` 26, `targetSdk` 36 and `compileSdk` 36, so it
has to follow the rules of every Android version up to 16. This is a check
of the code against the behaviour changes of each version. **It was done by
reading the code, no device or emulator was available** (see
[verification.md](verification.md)).

Result: no blocker found for Android 10 to 16. One real problem which
affects Android 9 and higher was found and fixed (R2-B1, crash when a server
can not be started). Two cosmetic points are open.

## By version

| Android | Rule which matters for this app | State |
|---------|---------------------------------|-------|
| **10** (API 29) | Scoped storage. | Plain storage keeps working through `requestLegacyExternalStorage="true"` and the storage permission (`WRITE_EXTERNAL_STORAGE`, declared up to API 29). The main screen shows and requests it for Android 10 and lower. |
| | Mode `w` of content providers does not truncate anymore. | SAF uploads use `wt` / `wa` (first review, L13). |
| | Activities can not be started from the background. | Only used as fallback (open the app when no password is set, start by helper activity). The toast is shown in any case. |
| | Location data of media files. | `ACCESS_MEDIA_LOCATION` is declared, shown and requestable from Android 10. |
| **11** (API 30) | `requestLegacyExternalStorage` is ignored, direct file access needs "All files access". | `MANAGE_EXTERNAL_STORAGE` is declared, state is read with `Environment.isExternalStorageManager()` (guarded by API level) and the settings page is opened from the main screen. Alternatives without it: SAF, quick share. |
| **12** (API 31) | `PendingIntent` needs a mutability flag. | All `PendingIntent`s use `FLAG_IMMUTABLE`. |
| | `android:exported` must be explicit. | Set for every component. |
| | Foreground services can not be started from the background. | Allowed cases cover the app: start button, widget, notification action, quick settings tile, boot. A start triggered by Tasker while the app is in the background can be refused by Android: the exception is caught, the fallback activity may be blocked too (limitation of Android, see open points). |
| **13** (API 33) | Notifications need a runtime permission. | `POST_NOTIFICATIONS` is declared, shown (highlighted if missing) and requestable. The server runs without it, only the notification is hidden. |
| **14** (API 34) | Foreground services need a type. | Services declare `foregroundServiceType="specialUse"`, permission `FOREGROUND_SERVICE_SPECIAL_USE` is declared. |
| | Receivers registered at runtime need an export flag, except for system broadcasts. | The only one (`PftpdFragment`) listens to the system broadcast `CONNECTIVITY_ACTION`. |
| | A service started with `startForegroundService()` which stops without `startForeground()` crashes the app (rule exists since Android 8, the app uses this start since Android 9). | **Was violated** when a server could not be started. Fixed, R2-B1. |
| **15** (API 35) | Edge to edge is enforced. | Main activity, share activities and directory picker apply window insets. Not handled in the two configuration screens of the Tasker plugin (open point). |
| | 16 KB page size. | The app has no native libraries. |
| | Restrictions for foreground services started on boot. | Do not apply to type `specialUse`. |
| **16** (API 36) | Orientation and resize restrictions are ignored on large screens. | The share activities lock the orientation while copying so the progress dialog survives. On tablets a rotation can close the dialog, the copy continues (cosmetic). |
| | Predictive back. | No custom back handling in the app. |

## API levels of the code

* Calls which need a newer API than `minSdk` 26 are guarded by
  `Build.VERSION.SDK_INT` checks (`ServicesStartStopUtil`, `MainTabsActivity`,
  `PftpdFragment`, share activities).
* Code added by the reviews uses only APIs available on API 26:
  `Service.stopForeground(int)` (24), `HandlerThread.quitSafely()` (18),
  `java.nio.file` (26), socket adaptors of `java.nio.channels`.
* Deprecated but working APIs which are still used: `AsyncTask`,
  `ProgressDialog`, `Handler()` without looper, `android.preference`,
  `ActivityManager.getRunningServices()` (still returns the app's own
  services), `Display.getMetrics()`. No action needed for Android 16.

## Open points

| # | Point | Effect |
|---|-------|--------|
| A-O1 | `TaskerEditActionActivity` and `TaskerEditConditionActivity` do not apply window insets. | On Android 15+ the first list entry may be drawn below the status bar. Only visible when configuring the Tasker plugin. |
| A-O2 | Start by Tasker while the app is in the background (Android 12+). | Android may refuse to start the service. Works if the app is excluded from battery optimisation. |
| A-O3 | Orientation lock during copy of shared files is ignored on large screens (Android 16). | Progress dialog can disappear on rotation. |

## Recommended check on a device

For each Android version you care about (at least 10, 11 and the newest):

1. Fresh install, grant storage access as offered on the main screen, allow
   notifications.
2. Start both servers from the app, from the widget and from the quick
   settings tile. Stop them from the notification.
3. Start by widget **before** generating an SFTP host key: the app must not
   crash, FTP must run.
4. Transfer a file with screen off.
5. SAF: pick a directory on the SD card, upload over an existing larger
   file, compare the size.
