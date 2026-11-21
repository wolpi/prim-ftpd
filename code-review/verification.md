# Verification

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
