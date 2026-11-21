# Verification

First review: this page up to "Recommended tests before release".
Second review: section "Second review" at the end.

## Environment

No JDK and no Android SDK were installed on the machine used for the review,
so the app could not be built with Gradle and nothing was run on a device or
emulator.

## What has been done

- **Type check of all sources.** All Java sources of `primitiveFTPd`,
  `pftpd-pojo-lib` and `sshd-core-0.14.0` were compiled with the `javac` of the
  JDK bundled with IntelliJ (21) against the dependency jars downloaded from
  Maven Central / Google Maven (Android framework classes from robolectric's
  `android-all`), with a generated stub of class `R`. Result: no errors.
- **Existing unit tests.** `KeyParserTests` and `LsOutputParserTests`
  (13 tests) pass.
- **FTP transfer loop.** `AndroidIoDataConnection` was run against real
  sockets in both directions with sizes 0, 1, 4095, 65536, 65537 and
  5,000,003 bytes, with short reads and small writes on the other side.
  Content and returned sizes match.
- **Helpers.** Ad-hoc checks of `Utils.parent()`, `Utils.absolute()`,
  `Utils.absoluteOrHome()`, `Utils.openAtOffset()` (with a stream skipping
  less than requested), `IpAddressProvider.extractIp()`,
  `FilenameUnique.sanitize()` and `FileSizeUtils`.

## What has NOT been verified

- Gradle build, lint, desugaring and packaging of the app.
- Everything depending on Android at runtime: SAF, root shell, media scanner,
  services, notifications, wake locks, widget, quick settings tile, share
  activities, download service.
- Device tests in `tests/tests.py`.
- Real transfers of files larger than 2 GB.
- Interoperability with FTP / SFTP clients after the hardening
  (ip check of data connections, removed SSH algorithms).

## Recommended tests before release

1. `./gradlew assembleDebug test`
2. `tests/tests.py` for all storage types.
3. Upload and download of a file > 4 GB by FTP and SFTP for plain storage and
   SAF, including an interrupted and resumed transfer.
4. A transfer taking longer than the idle timeout, longer than the server
   stop timeout and longer than 60 minutes with screen off.
5. Quick share and "save as" of a large file and of several files.
6. Download of a large file by URL with the app in background.
7. FTP in active and passive mode, also through `adb forward`.
8. SFTP with an old and a current client (OpenSSH, FileZilla, WinSCP).
9. Widget: start and stop the server several times.

---

# Second review

## Environment

Still no Android SDK, device or emulator. But the servers themselves are
plain Java, so this time **the real server code of the app was run on a
desktop JVM** and tested with real clients.

Test harness (kept outside of the repository):

* `FtpServerService.launchServer()` and `SshServerService.launchServer()`
  are called unchanged, with a `PrefsBean` like the app builds it: user and
  password, storage type plain, passive ports, IP checks, ed25519 host key
  generated and read by `HostKeyAlgorithm`.
* Everything below is the code of the app and its libraries:
  `PrimNioListener`, `AndroidIoDataConnectionFactory`,
  `AndroidIoDataConnection`, `AndroidPrefsUserManager`, `FsFtpFile`,
  `FsSshFile`, the patched `SftpSubsystem`, `ScpHelper`, `SignatureEd25519`.
* Replaced by test doubles: `MediaScannerClient` (no media store on a JVM)
  and `android.os.Environment`.
* Clients: `curl` 8.5 (FTP), OpenSSH 9.6 `sftp` and `scp`, a Python script
  with raw sockets for the passive connection tests.
* Test file: 5 GiB (5,368,709,120 bytes) of pseudo random data, compared by
  SHA-256 after every transfer.
* JDK 21, BouncyCastle 1.83, Linux, loopback network.

## Results

All tests ran against the final code of this review.

| Test | Result |
|------|--------|
| FTP download 5 GiB, passive | pass |
| FTP upload 5 GiB, passive | pass |
| FTP download 5 GiB, active (`PORT`) | pass |
| FTP upload 5 GiB, active | pass |
| FTP resume download at 3 GiB + 123 bytes (`REST`) | pass |
| FTP resume upload at 4 GiB + 100 bytes | pass |
| FTP `APPE` of the second half of the file | pass |
| FTP `SIZE` / listing of the 5 GiB file | pass, 5368709120 |
| FTP download and upload which take longer than the idle timeout (5 s), next transfer on the same control connection | pass, connection is kept |
| FTP control connection idle for longer than the idle timeout | pass, closed by server |
| FTP passive: other host (127.0.0.2) connects to the passive port first | pass, client gets the file, other host gets nothing (failed transfer before R2-S1) |
| FTP passive: client never opens the data connection | pass, `425` after the timeout (waited forever before R2-L7) |
| SFTP download 5 GiB (5 re-keyings) | pass, about 65 MB/s |
| SFTP upload 5 GiB | pass |
| SFTP resume download (`reget`) at 4 GiB + 100 bytes | pass |
| SFTP resume upload (`reput`) at 4 GiB + 100 bytes | pass |
| SFTP `put -p`, files of 5,000 / 40,000 / 100,000 / 1,048,576 bytes: modification time kept | pass (3 of 4 failed before R2-L1) |
| SCP legacy protocol (`scp -O`) upload 5 GiB | pass |
| SCP legacy protocol download 5 GiB with relative path | pass (failed before R2-L6) |
| SSH login with ed25519 host key and password | pass (failed on the JVM before R2-B9) |
| Unit tests `KeyParserTests`, `LsOutputParserTests` | 13 of 13 pass |
| Type check of all sources (`javac` against dependency jars) | no errors |
| Normalisation of virtual paths (7 cases) | pass |

## What these tests do not cover

* Everything Android specific: services and their life cycle (R2-B1, R2-B2,
  R2-B3, R2-B8, R2-B10), wake lock (R2-B7), download service (R2-B6), copy of
  shared files (R2-L5), permissions, notifications. These fixes are
  **checked by compiler and by reading only**.
* Storage types SAF, read-only SAF, root, virtual and quick share. Root
  fixes (R2-L3, R2-L4) are not tested, there is no rooted device.
* Android's I/O stack, flash storage, WiFi. Transfer rates above say nothing
  about a phone.
* Gradle build, lint, desugaring, packaging.
* Clients other than `curl` and OpenSSH.

## Recommended tests on a device

In addition to the list of the first review:

1. Start by widget before an SFTP host key exists (both servers enabled):
   no crash, FTP runs.
2. Start with a port which is in use: error message, no crash.
3. `sftp put -p` and check the modification time on the phone.
4. FileZilla in passive mode while another device probes the passive port.
5. "Save as" of a file larger than the free space: message that the copy
   failed.
6. Virtual folders with FileZilla: enter `/fs`, go up, the four storage
   types must be listed again.
