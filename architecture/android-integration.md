# Android integration

## Components declared in the manifest

| Component | Type | Exported | Purpose |
|-----------|------|----------|---------|
| `ui.MainTabsActivity` | activity | yes (launcher) | Main UI |
| `ui.LeanbackActivity` | activity | yes (TV launcher) | Main UI on Android TV |
| `share.ReceiveSaveAsActivity` | activity | yes (`SEND`, `SEND_MULTIPLE`) | "Save as": store shared data in a directory |
| `share.ReceiveQuickShareActivity` | activity | yes (`SEND`, `SEND_MULTIPLE`) | "Quick share": serve shared files |
| `remotecontrol.TaskerEditActionActivity`, `TaskerEditConditionActivity` | activity | yes | Configuration screens of the Tasker plugin |
| `ui.StartServerAndExitActivity`, `StopServerAndExitActivity` | activity | no | App shortcuts, fallback to start a service |
| `filepicker.ResettingFilePickerActivity` | activity | no | Directory picker |
| `services.FtpServerService`, `SshServerService` | foreground service (`specialUse`) | no | The servers |
| `services.DownloadsService` | foreground service (`specialUse`) | no | Download of a shared URL |
| `services.QuickSettingsService` | tile service | yes, needs `BIND_QUICK_SETTINGS_TILE` | Quick settings tile |
| `BootUpReceiver` | receiver | yes (`BOOT_COMPLETED`) | Start on boot, start/stop notification |
| `StartStopWidgetProvider` | receiver | no | Widget, target of notification actions |
| `remotecontrol.TaskerReceiverFire`, `TaskerReceiverQuery` | receiver | yes, no permission | Tasker / Locale plugin (acts only if enabled in preferences) |
| `remotecontrol.PftpdPowerTogglesPlugin` | receiver | yes, needs permission of PowerToggles | PowerToggles plugin |
| `androidx.core.content.FileProvider` | provider | no | Used by the directory picker |

## Permissions

| Permission | Why |
|------------|-----|
| `INTERNET` | Listening sockets, download of URLs |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Servers must keep running in the background |
| `POST_NOTIFICATIONS` (runtime, Android 13+) | Notification of the running server with stop action |
| `WAKE_LOCK` | Keep the CPU awake while the server runs (optional preference) |
| `RECEIVE_BOOT_COMPLETED` | Start on boot (optional preference) |
| `MANAGE_EXTERNAL_STORAGE` (special access, Android 11+) | Plain storage: access to all files of shared storage |
| `WRITE_EXTERNAL_STORAGE` (up to Android 10) | Plain storage on old versions, together with `requestLegacyExternalStorage` |
| `ACCESS_MEDIA_LOCATION` | Location data in media files stays intact |

The main screen shows the state of the storage, media location and
notification permissions and offers a link to request each of them
(`PftpdFragment.displayFullStorageAccess()` and neighbours).

## Using the app without root

Root is optional. On a device which is not rooted:

| Feature | Works without root | Remark |
|---------|--------------------|--------|
| FTP and SFTP server | yes | Ports must be above 1024, the preferences enforce that (defaults 12345 and 1234). |
| Storage type plain | yes | Needs "All files access" (Android 11+). Without it listings are empty and uploads fail. |
| Storage type SAF / read-only SAF | yes | Pick a directory, required for SD cards. |
| Quick share, save as, download | yes | |
| Widget, tile, start on boot, Tasker | yes | |
| Storage type virtual | partly | `/superuser` stays empty, the other branches work. |
| Storage type root ("super user") | no | The server does not start and says that root access is not available. |
| Preference "root: copy files" | no effect | Only used by storage type root. |

What decides whether root is used at all is the storage type: a root shell is
only opened for the types root and virtual (`ServerServiceHandler`). With the
default (plain) the app never calls `su`.

Recommended setup without root:

1. Grant "All files access" from the main screen (or use SAF and pick a
   directory if you do not want to grant it).
2. Allow notifications.
3. Exclude the app from battery optimisation if long transfers are
   interrupted with the screen off (see https://dontkillmyapp.com/), keep
   the wake lock preference enabled.
4. Set a password or enable public key authentication, generate the host key
   for SFTP once in the app.

## Authentication

| Protocol | Mechanisms |
|----------|------------|
| FTP | User name and password of the preferences (`AndroidPrefsUserManager`), optional anonymous login. At most 5 failed logins per connection with a delay. |
| SFTP | Password (same user manager), public key (`PubKeyAuthenticator`, keys from `authorized_keys` in the app's external files dir, user name is ignored), optional anonymous login. |

The password is stored as salted SHA-512 hash in the preferences
(`EncryptionUtil`, `EncryptingEditTextPreference`). Connections can be
limited to an IP pattern (`RemoteIpChecker`).

## Data the app keeps

| What | Where |
|------|-------|
| Preferences incl. password hash | default `SharedPreferences` |
| SSH host keys | private files of the app (`openFileInput` / `openFileOutput`) |
| `authorized_keys` | external files dir of the app, `.ssh/authorized_keys` (two old locations on shared storage are still read) |
| Temporary files of quick share and of root copy mode | external files dir of the app, `quick-share/`, `root-copy/` |
| Log files (if enabled) | see `assets/logback.xml` |

## Remote control

* **Tasker / Locale plugin:** exported receivers start and stop the servers
  and report the state. They only act if the user enabled the plugin in the
  preferences (see the security advisory linked there).
* **PowerToggles plugin**, **widget**, **quick settings tile**, **app
  shortcuts**: all use `ServicesStartStopUtil`.
