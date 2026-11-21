# Use without root

Question: does the app work well on a device which is **not rooted**?

Short answer: **yes.** Root is only used by one optional storage type. With
the default settings the app never calls `su`. Two problems which affect
devices without root were found and fixed (R2-B1, R2-B10 in
[round-2.md](round-2.md)).

## Where root is used

Searched for every use of the root shell and of `su`:

| Place | When it runs |
|-------|--------------|
| `ServerServiceHandler.shellOpen()` opens `su` through libsuperuser | Only if the storage type is "super user" (root) or "virtual folders" |
| `RootFile`, `RootFileSystemView` and subclasses run `ls`, `mv`, `rm`, `mkdir`, `touch` in that shell and start `su -c dd ...` per transfer | Only for files of storage type root, or below `/superuser` of virtual storage |
| Preference "Root: copy files to tmp dir" | Only read by `RootFile` |

Nothing else needs root: servers, authentication, SAF, quick share, save as,
downloads, widget, tile, boot start, Tasker and PowerToggles plugins.

## What happens without root

| Situation | Before | Now |
|-----------|--------|-----|
| Default setup (storage type plain) | works | works |
| Storage type SAF / read-only SAF | works | works |
| Storage type "super user" selected by mistake | Server started and showed "running", clients could log in but saw no files and could not transfer anything. No hint why. | Server does not start, message "root access is not available, choose another storage type". |
| Storage type "virtual folders" | `/fs`, `/saf`, `/rosaf` work, `/superuser` is empty | same (intended: the other branches are usable) |
| Opening the root shell fails (`su` not found) | handled by libsuperuser, no crash | same |

## Things which matter on a device without root

These are not root specific, but they are the usual reasons why "it does not
work" without root:

| Topic | State |
|-------|-------|
| **Ports.** Ports up to 1024 can not be bound without root. | Preferences only accept ports above 1024 (`LoadPrefsUtil.validatePort()`), defaults are 12345 (FTP) and 1234 (SFTP). Passive FTP port 5678 by default. OK. |
| **Access to files.** Without root the app only sees what Android allows. | Plain storage needs "All files access" on Android 11+ (`MANAGE_EXTERNAL_STORAGE`), the storage permission before. The main screen shows whether it is granted with a link to request it. Without it listings are empty and uploads are rejected. Alternative without that permission: SAF with a picked directory, or quick share. |
| **SD card.** | Use storage type SAF and pick a directory (see README). |
| **Running in background.** | Foreground services with notification, optional wake lock which is renewed during transfers. Battery optimisation of some vendors can still stop the app, see https://dontkillmyapp.com/. |
| **Notification permission** (Android 13+). | Shown and requestable on the main screen. Server runs without it, but the notification with the stop action is hidden. |
| **Server start fails** (port in use, no SFTP host key yet). | Crashed the app on Android 9+ (R2-B1). Fixed: the failing service ends cleanly, the other server keeps running. |

## How this was checked

* Code reading of all places listed above.
* The test harness of this review runs `FtpServerService` and
  `SshServerService` as an ordinary process without any privileges (see
  [verification.md](verification.md)). All transfer tests passed with
  storage type plain.
* **Not checked on a real device.** In particular the permission dialogs,
  SAF and the behaviour of vendor specific battery savers can only be
  verified on a phone.

## Recommended check on your phone

1. Install, open the app, tap "Request" next to "Has full access to storage"
   and allow it. Allow notifications.
2. Keep storage type "Plain old filesystem", set a password, generate the
   host key when asked.
3. Start the server, connect with an FTP and an SFTP client to the address
   shown, upload and download a file larger than 4 GB.
4. Switch the screen off during a transfer which takes several minutes.
5. Stop the server from the notification.
