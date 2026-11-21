# Improvements of the first review

Commits `3554ec2` and `ec9f15d`. The findings behind each entry are in
[../large-files.md](../large-files.md) (`L`), [../bugs.md](../bugs.md) (`B`)
and [../security.md](../security.md) (`S`).

## Long running transfers are not cut off anymore

| Improvement | Files | Finding |
|-------------|-------|---------|
| FTP sessions use the configured idle timeout instead of a hard coded 60 seconds. | `services/AndroidPrefsUserManager.java` | L1 |
| Idle events of the control connection are ignored while data is transferred. | `io/PrimNioListener.java` | L2 |
| The timer which stops idle servers looks at the last transferred data of FTP and SFTP. | `services/AbstractServerService.java`, `io/AndroidIoDataConnection.java`, `sshd-core` `SftpSubsystem.java` | L3 |
| The wake lock is renewed every 5 minutes while data flows. | `util/WakelockUtil.java`, `services/AbstractServerService.java` | L4 |

## Faster and correct transfer code

| Improvement | Files | Finding |
|-------------|-------|---------|
| FTP copy loop with 64 KiB buffer and standard buffer handling, complete writes of listings. | `io/AndroidIoDataConnection.java` | L5, L6 |
| Byte counters are `long`, statistics count uploads and downloads in the right direction. | `io/Counting*ByteChannel.java`, `io/AndroidIoDataConnection.java` | L7, L8 |
| SFTP reads fill the requested length, no re-opening of streams at end of file. | `sshd-core` `SftpSubsystem.java` | L9 |

## Offsets and resume

| Improvement | Files | Finding |
|-------------|-------|---------|
| Streams are positioned with `skipFully()` / `openAtOffset()`, file based streams seek. | `filesystem/Utils.java`, all `createInputStream()` | L10 |
| Upload at an arbitrary offset works for plain storage (file stays open, bulk writes). | `filesystem/FsFile.java` | L11 |
| SAF: append for resumed uploads, truncation when a file is overwritten. | `filesystem/SafFile.java` | L12, L13 |
| Root copy mode: download with offset on a new file object. | `filesystem/RootFile.java` | L14 |

## Shared files and downloads

| Improvement | Files | Finding |
|-------------|-------|---------|
| Shared files are copied in a background thread with a 64 KiB buffer, once. | `share/AbstractReceiveShareActivity.java`, `share/ReceiveQuickShareActivity.java` | L15 - L18 |
| Download service is a foreground service with its own wake lock, notification updated once per second, progress for sizes above 2 GB, HTTP errors are errors. | `services/DownloadsService.java`, `util/NotificationUtil.java` | L19 - L22 |

## Bugs

| Improvement | Files | Finding |
|-------------|-------|---------|
| Path helpers handle empty paths, root and top level entries. | `filesystem/Utils.java`, `FsSshFile.java` | B1, B2, B4, B11 |
| Media scanner connection is awaited with a limit. | `filesystem/MediaScannerClient.java` | B3 |
| Root storage: user of home dir, null results of shell commands, empty `ls` output, `.` and `..`, double slashes, empty tmp dir, link resolution. | `filesystem/Root*.java` | B5 - B12 |
| SAF / read-only SAF: nonexistent paths, cursor handling, toasts from server threads, needless provider queries, null streams. | `filesystem/Saf*.java`, `RoSaf*.java` | B13 - B19 |
| Virtual storage and quick share: prefix matching, missing delegates, move between storages, parents, empty `CWD`, home dir. | `filesystem/Virtual*.java`, `QuickShare*.java` | B20 - B26 |
| Thread-safety of date formats, user map, time resolution cache, download flags, wake lock. | `pojo/LsOutputParser.java`, `filesystem/Utils.java`, `RootFile.java`, `StorageManagerUtil.java`, `util/FilenameUnique.java`, `services/AndroidPrefsUserManager.java`, `DownloadsService.java`, `util/WakelockUtil.java` | B27 - B30 |
| Services, UI and others: start intent without extras, service announcement, missing password, idle timeout, ed25519 host key lookup, tile, widget click handlers, clean space, client action log, CSV log, `authorized_keys` parser, share intents. | `services/*.java`, `util/ServicesStartStopUtil.java`, `ui/CleanSpaceFragment.java`, `ui/ClientActionFragment.java`, `log/CsvEncoder.java`, `pojo/KeyParser.java`, `share/*.java` | B31 - B44 |

## Security

| Improvement | Files | Finding |
|-------------|-------|---------|
| Allowed IP pattern denies if the address can not be checked, client IPs are extracted correctly. | `util/RemoteIpChecker.java`, `util/IpAddressProvider.java` | S1, S2 |
| Share intents can not make the app copy its own private files, file names of shared data are sanitised. | `share/AbstractReceiveShareActivity.java`, `util/FilenameUnique.java` | S3, S4 |
| FTP data connections must come from / go to the address of the control connection. | `services/FtpServerService.java`, `io/AndroidIoDataConnectionFactory.java` | S5, S6, S9 |
| Weak SSH algorithms are not offered. | `services/SshServerService.java` | S7 |
| Constant time comparison of password hashes. | `services/AndroidPrefsUserManager.java` | S8 |
