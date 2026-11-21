# Second review – 2026-11-21

Second pass over the code after the fixes of the first review, with three
goals:

1. find what the first review missed or broke,
2. check large files again, this time **by running the real server code**
   with real clients and files larger than 4 GiB (see
   [verification.md](verification.md)),
3. check that the app works on a device **without root**
   (see [non-root.md](non-root.md)).

Ids: `R2-L` large files / transfers, `R2-B` bugs, `R2-S` security.
What has been changed for each finding is listed in
[improve/round-2.md](improve/round-2.md).

## Large files and transfers

| # | Problem | Location | Found by |
|---|---------|----------|----------|
| R2-L1 | **Modification time of uploads was lost.** OpenSSH (`sftp put -p`, `scp -p` which uses SFTP since OpenSSH 9) sets the time on the open handle *before* it closes the file. Data still in the buffer of the output stream was written afterwards by `close()` and set the time to "now". Hit every file whose last block was smaller than the buffer (8 KiB for plain storage, 1 MiB for SAF). | `sshd-core` `SftpSubsystem.FileHandle` | test (3 of 4 sizes failed) |
| R2-L2 | **Failed uploads were reported as successful.** `FileHandle.close()` closed the output stream with `closeQuietly()`. Buffered data is written by `close()`: if that failed (no space left, provider error) the client still got "OK". SAF buffers up to 1 MiB, so the end of a file could be missing silently. | `sshd-core` `SftpSubsystem.FileHandle` | reading |
| R2-L3 | **Root storage: resumed upload destroyed the file.** The offset of `createOutputStream()` was ignored by `dd` and by copy mode. A resumed upload or FTP `APPE` replaced the file by the part which was sent. Was listed as "known limitation" in the first review, but it is silent data loss. | `filesystem/RootFile` | reading |
| R2-L4 | **Root copy mode: upload over an existing file was lost.** The temporary file is created with the last path segment as name, but moved to its target using `getName()`, which is the complete path for files which exist (name printed by `ls -ld`). `mv` failed, then the temporary directory was deleted. | `filesystem/RootFile` | reading |
| R2-L5 | **Copy of shared files ignored errors.** Exceptions while copying (e.g. no space left for a large file) were only logged, the truncated file looked like a successful "save as" or quick share. | `share/AbstractReceiveShareActivity` | reading |
| R2-L6 | **Legacy SCP: download with relative path failed.** `scp -O host:file` looked for `/file` in the root of the file system instead of the home directory ("no such file or directory", with the Java object name in the message). | `sshd-core` `ScpHelper.send()` | test |
| R2-L7 | **Passive FTP data connection was awaited without limit.** `accept()` of a channel ignores the socket timeout. A client which never connected kept its session and a worker thread blocked for good. Was a known limitation of the first review. | `io/AndroidIoDataConnectionFactory` | reading |

Checked and found working (no change): transfers of 5 GiB by FTP (passive and
active), SFTP and legacy SCP in both directions, resume of downloads and
uploads at offsets above 4 GiB, `APPE`, size and listing of files above
4 GiB, SSH re-keying during transfers (5 times for 5 GiB), FTP transfers
taking longer than the idle timeout, closing of really idle FTP sessions.

## Bugs

| # | Problem | Location |
|---|---------|----------|
| R2-B1 | **A server which could not be started crashed the app** (Android 9+). The services are started with `startForegroundService()`, Android then requires `startForeground()`. If the start failed the service stopped itself without that call and Android kills the app, including the other server which runs fine. Easy to hit: SFTP has no host key yet and the server is started by widget, tile or on boot (both servers are started by default); a port is in use; a start request arrives while the server runs already (shortcut, Tasker). | `services/ServerServiceHandler` |
| R2-B2 | **A server which failed or stopped took shared resources away from the other one.** `handleStop()` released the wake lock and closed the root shell without checking whether the other server still needed them, and set widget and notification to "stopped". The root shell is a static field but was guarded by a lock per handler, both services could open one each. | `services/ServerServiceHandler` |
| R2-B3 | Runtime exceptions while starting a server (e.g. invalid passive ports, `createServer()`) were not caught on the handler thread and crashed the app instead of showing the start error. | `services/ServerServiceHandler`, `services/FtpServerService` |
| R2-B4 | SFTP extended command `check-file` (hash of a file, used by prim-sync): `break` was missing, after the reply a second response "unsupported" was sent for the same request. | `sshd-core` `SftpSubsystem` |
| R2-B5 | Virtual storage did not resolve `..`. `CDUP` in `/fs` led to `/fs/..`, which is the root of the plain file system, the list of storage types could not be reached again by going up. Relative `CWD ..` was also checked against top level names first. | `filesystem/VirtualFileSystemView`, `VirtualFtpFileSystemView` |
| R2-B6 | The download service never stopped itself after a download and its thread was never ended. A server answering with a `Location` header again and again caused endless recursion. | `services/DownloadsService` |
| R2-B7 | The wake lock of a running server was released when "save as" had copied a shared file: the copy task takes and releases the same static wake lock. | `share/AbstractReceiveShareActivity`, `util/WakelockUtil` |
| R2-B8 | The timer which stops idle servers was scheduled once more for every start request, the oldest one fired first. | `services/AbstractServerService` |
| R2-B9 | `SignatureEd25519` read private fields of BouncyCastle's key classes by reflection on the runtime class. BouncyCastle returns subclasses on newer Java versions (`BC15EdDSAPublicKey`), the field is not found there and the SSH key exchange fails with the default (ed25519) host key. Android currently gets the base classes, so the app worked, but only by coincidence. | `crypto/SignatureEd25519` |
| R2-B10 | Storage type "super user" on a device without root: the server started, but no file could be accessed and nothing told the user why. | `services/ServerServiceHandler` |
| R2-B11 | Log message of the FTP service said "ssh server". | `services/FtpServerService` |

## Security

| # | Risk | Location |
|---|------|----------|
| R2-S1 | **Any host could make FTP transfers of others fail.** With the IP check of passive data connections (first review, S5) a connection from a wrong address made the server close the passive socket and fail the transfer. A host which connects to the (single, predictable) passive port first could do that for every transfer. It could not read data. | `io/AndroidIoDataConnectionFactory` |

## Open points (not changed)

| # | Point | Why not changed |
|---|-------|-----------------|
| R2-O1 | The handler thread of a server service is never ended, one idle thread remains per start/stop cycle. | Callbacks of the shared root shell are bound to the looper of the thread which opened it. Ending that thread while the other server uses the shell would hang all root operations. Needs a rework of the shell handling which can not be tested without a rooted device. |
| R2-O2 | Root storage: a failing `dd` (e.g. no space left) is only logged, the upload is reported as successful. | Can not be verified without a rooted device, exit codes of `su` implementations differ. |
| R2-O3 | SFTP write at offset 0 truncates the file (plain storage, same in upstream SSHD 0.14). Clients which modify the beginning of an existing file in place (e.g. sshfs) lose the rest. | SCP and FTP `STOR` rely on truncation at offset 0, separating the cases needs a larger change of the file API. |
| R2-O4 | FTP `RMD` deletes non-empty directories for SAF and root storage (`rm -rf`, provider semantics). | Behaviour of the original app, clients may rely on it. |
| R2-O5 | A cancelled or failed download and a failed copy of shared data leave the partial file. | Would need storage type specific deletion, user is told about the failure now. |
| R2-O6 | A service restarted by Android after the process was killed (`START_STICKY`, intent is null) does not start its server again. | Would need loading of preferences in the service, behaviour of the original app. |
| R2-O7 | SCP download with wild cards scans the real file system relative to the working directory of the process. | Vendored code, rarely used. |
| R2-O8 | `PftpdFragment.onResume()` uses an `ExecutorService` in try-with-resources, which waits for the task on the main thread. | Task is short (list of network interfaces), no visible effect. |

## Coverage of this review

Read completely again: `io`, `services`, `filesystem`, `pftpd-pojo-lib`,
`SftpSubsystem` and `ScpHelper` of `sshd-core`, `AbstractReceiveShareActivity`,
`TargetDir`, `FilenameUnique`, `ServicesStartStopUtil`, `WakelockUtil`,
`NotificationUtil`, `Defaults`, `RemoteIpChecker`, `IpAddressProvider`,
`EncryptionUtil`, `SignatureEd25519`, widget provider, boot receiver,
application class, manifest.

Read in parts: `PftpdFragment` (storage type and permission handling),
`LoadPrefsUtil`, `PortEditTextPreference`.

**Not read in this pass:** `ReceiveQuickShareActivity`, `ReceiveSaveAsActivity`,
the other fragments and dialogs of `ui`, `remotecontrol`, `log`,
`KeyFingerprintProvider`, `HostKeyAlgorithm`, the vendored file picker,
`StringUtils`, the rest of `sshd-core`.

An automated multi-agent review was started in parallel but was cut off by a
usage limit before it reported anything, none of the findings above come
from it.
