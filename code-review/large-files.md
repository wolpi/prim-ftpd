# Large files and long running transfers

## Transfers were cut off

| # | Problem | Location | Fix |
|---|---------|----------|-----|
| L1 | Max idle time of FTP users was hard coded to 60 seconds. ftpserver applies it to the control connection after login. During a data transfer the control connection is idle, the idle event is queued behind the running command and the session got closed right after every transfer taking longer than 60 seconds. The idle timeout preference had no effect for FTP. | `services/AndroidPrefsUserManager` | Use configured idle timeout (0 = none). |
| L2 | Same mechanism with a configured idle timeout: stale idle events closed the session after a long transfer. | `io/PrimNioListener` | `sessionIdle()` is ignored if last access time (updated continuously while data is transferred) is younger than the idle time. |
| L3 | "Idle timeout to stop server" (default 30 minutes) was only reset by client action events, which are posted at the start of a transfer. A transfer taking longer stopped the server in the middle. | `services/AbstractServerService` | Timer checks timestamp of last transferred data of FTP and SFTP and re-schedules itself. |
| L4 | Wake lock is obtained with a timeout of 60 minutes. Device could go to standby during longer transfers. | `util/WakelockUtil`, `services/AbstractServerService` | Wake lock is not reference counted anymore and is renewed every 5 minutes while data is transferred. |

## Transfer code

| # | Problem | Location | Fix |
|---|---------|----------|-----|
| L5 | FTP data was copied with a 4096 byte buffer, session statistics were updated for every chunk. | `io/AndroidIoDataConnection` | 64k buffer. |
| L6 | Buffer handling relied on side effects (position/limit set by hand), a partial write would have lost data. Directory listings were written with a single `write()`. | `io/AndroidIoDataConnection` | Standard clear / flip / write-until-empty loop. |
| L7 | Byte counters were `int`, overflow above 2 GB. | `io/Counting*ByteChannel` | `long`. |
| L8 | Uploads were counted as written bytes in session statistics. | `io/AndroidIoDataConnection` | Correct direction. |
| L9 | SFTP read returned short reads and decremented the stream position at end of file, causing the stream to be re-opened. | `sshd-core` `SftpSubsystem.FileHandle` | Fill buffer, do not touch position at EOF. |

## Offsets / resume

| # | Problem | Location | Fix |
|---|---------|----------|-----|
| L10 | `InputStream.skip()` was called once and its result ignored. It may skip less than requested, the transfer then starts at a wrong offset. | all `createInputStream()` | `Utils.skipFully()` / `Utils.openAtOffset()`, skipping on the raw stream so file based streams seek instead of read. |
| L11 | Upload at an offset which is neither 0 nor end of file: `RandomAccessFile` was created in try-with-resources and therefore closed before the returned stream was used. Every write failed. Only single byte writes were implemented. | `filesystem/FsFile` | Keep file open until stream is closed, implement bulk write. |
| L12 | SAF upload with offset threw `UnsupportedOperationException`. | `filesystem/SafFile` | Append if offset equals current size, `IOException` otherwise. |
| L13 | SAF: mode `w` does not truncate since Android 10, overwriting a file with a smaller one left old content at the end. | `filesystem/SafFile` | Mode `wt`. |
| L14 | Root copy mode: download with offset on a new file object (FTP `REST`) failed with "cannot find parent dir". | `filesystem/RootFile` | Copy to tmp dir if not done yet. |

## Shared files and downloads

| # | Problem | Location | Fix |
|---|---------|----------|-----|
| L15 | "Background" copy task posted every copy to the main thread and polled once per second for the result. Large files froze the UI ("app not responding"). | `share/AbstractReceiveShareActivity` | Copy in the background thread. |
| L16 | Quick share of a single file copied in `onCreate()` on the main thread. | `share/ReceiveQuickShareActivity` | Uses the background task as well. |
| L17 | `onResume()` started the copy again each time the activity was resumed. | `share/ReceiveQuickShareActivity` | Started once. |
| L18 | 4096 byte copy buffer. | `share/AbstractReceiveShareActivity` | 64k. |
| L19 | Download service was never a foreground service and did not hold a wake lock. Android stops background services shortly after the app leaves the foreground. | `services/DownloadsService` | `startForeground()` and own wake lock, renewed during download. |
| L20 | Notification was rebuilt (including decoding a bitmap) for every 4k chunk. | `services/DownloadsService` | Updated once per second, 64k buffer. |
| L21 | Progress used `(int) size`, overflow above 2 GB. | `util/NotificationUtil` | Scaled to 0..1000, human readable sizes. |
| L22 | HTTP error responses were saved as file. | `services/DownloadsService` | Status >= 400 is an error. |

## Known limitations

State after the second review ([round-2.md](round-2.md)):

- Root storage can not continue uploads at an offset. Resumed uploads and
  `APPE` are rejected with an error now (R2-L3), they destroyed the file
  before.
- Root storage with offset for downloads reads and discards data up to the
  offset as `dd` output is a pipe.
- Root copy mode and quick share copy complete files, twice the space is
  needed temporarily.
- SFTP writes in random order are only supported by plain storage. A write
  at offset 0 truncates the file (R2-O3).
- Passive data connections are awaited for the idle time, 2 minutes if none
  is configured (R2-L7, was: without limit).
