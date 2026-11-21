# Improvements of the second review

Findings are described in [../round-2.md](../round-2.md), tests in
[../verification.md](../verification.md).

## Transfers and large files

| Improvement | Files | Finding |
|-------------|-------|---------|
| Modification time sent by the client before closing a file is kept: buffered data is written before attributes are applied, attributes are applied again when the file has been closed. | `sshd-core-0.14.0/.../server/sftp/SftpSubsystem.java` | R2-L1 |
| Closing an SFTP handle reports errors of closing the output stream to the client instead of swallowing them. Input stream and file are still closed in every case. | `sshd-core-0.14.0/.../server/sftp/SftpSubsystem.java` | R2-L2 |
| Root storage rejects uploads at an offset with an error instead of replacing the file by the transferred part. | `primitiveFTPd/.../filesystem/RootFile.java` | R2-L3 |
| Root copy mode remembers its temporary file, uploads over existing files reach their target. | `primitiveFTPd/.../filesystem/RootFile.java` | R2-L4 |
| Copy of shared files reports failures: copy errors are no longer swallowed, the user gets a message "could not copy n of m file(s)". | `primitiveFTPd/.../share/AbstractReceiveShareActivity.java` | R2-L5 |
| Legacy SCP downloads resolve relative paths against the home directory, error messages show the path. | `sshd-core-0.14.0/.../common/scp/ScpHelper.java` | R2-L6 |
| Waiting for a passive FTP data connection ends after the idle time (2 minutes if none is configured) with reply 425. | `primitiveFTPd/.../io/AndroidIoDataConnectionFactory.java` | R2-L7 |

## Stability

| Improvement | Files | Finding |
|-------------|-------|---------|
| `startForeground()` is called for every start of a server service: successful start, failed start and start request for a running server. A failed start no longer crashes the app. | `primitiveFTPd/.../services/ServerServiceHandler.java` | R2-B1 |
| Wake lock, root shell and "running" state are reference counted under one static lock. A server which fails or stops leaves them to the other server. Only one root shell can be opened. | `primitiveFTPd/.../services/ServerServiceHandler.java` | R2-B2 |
| Exceptions while starting a server are caught and shown as start error. `createServer()` of the FTP server is inside the guarded block. | `primitiveFTPd/.../services/ServerServiceHandler.java`, `FtpServerService.java` | R2-B3 |
| `check-file` sends exactly one response. | `sshd-core-0.14.0/.../server/sftp/SftpSubsystem.java` | R2-B4 |
| Virtual paths are normalised, `..` leads from a storage type back to the list of storage types. Relative directory changes starting with a dot are always relative to the working directory. | `primitiveFTPd/.../filesystem/VirtualFileSystemView.java`, `VirtualFtpFileSystemView.java` | R2-B5 |
| Download service stops itself when its work is done and ends its thread. At most 5 redirects by `Location` header. | `primitiveFTPd/.../services/DownloadsService.java` | R2-B6 |
| Copying shared files only releases the wake lock if it took it. | `primitiveFTPd/.../share/AbstractReceiveShareActivity.java`, `util/WakelockUtil.java` | R2-B7 |
| Only one idle stop timer per service. | `primitiveFTPd/.../services/AbstractServerService.java` | R2-B8 |
| ed25519 signatures derive key parameters from the standard encoding of the keys, no reflection on internals of BouncyCastle. | `primitiveFTPd/.../crypto/SignatureEd25519.java` | R2-B9 |
| Storage type "super user" without root access: server does not start and says why. | `primitiveFTPd/.../services/ServerServiceHandler.java` | R2-B10 |
| Log message corrected. | `primitiveFTPd/.../services/FtpServerService.java` | R2-B11 |

## Security

| Improvement | Files | Finding |
|-------------|-------|---------|
| A passive data connection from a wrong address is dropped and the server keeps waiting for the client. Other hosts can no longer make transfers fail by connecting first. | `primitiveFTPd/.../io/AndroidIoDataConnectionFactory.java` | R2-S1 |

## Documentation

| Improvement | Files |
|-------------|-------|
| Architecture of the project. | `architecture/` |
| Second review, check for devices without root, verification with real clients. | `code-review/round-2.md`, `non-root.md`, `verification.md` |
| This list of improvements. | `code-review/improve/` |
| README: use without root, large files, pointers to the documentation. | `README.md` |

## Behaviour changes to be aware of

1. Root storage: FTP `APPE` and resumed uploads fail with an error (they
   destroyed the file before).
2. A passive FTP data connection must be opened within the idle time, or
   within 2 minutes if no idle time is configured.
3. Storage type "super user" on a device without root: the server does not
   start anymore.
4. An SFTP client is told if closing an uploaded file failed.
