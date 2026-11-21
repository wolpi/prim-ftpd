# Security

## Fixed

| # | Risk | Location | Fix |
|---|------|----------|-----|
| S1 | **Allowed IP pattern failed open.** If the remote address was not an `InetSocketAddress` (or null) the connection was allowed although a pattern was configured. | `util/RemoteIpChecker` | Deny. |
| S2 | **Client IP extraction.** `/192.168.100.100:54321` is longer than 15 characters and was treated as IPv6, the port stayed part of the "ip". Exact patterns never matched and client action log showed ports. `hostname/ip` was not handled. | `util/IpAddressProvider`, `util/RemoteIpChecker` | Parse by number of colons, use `getHostAddress()`. |
| S3 | **Confused deputy via share intents.** `ReceiveQuickShareActivity` and `ReceiveSaveAsActivity` are exported. Another app could send a `file://` URI (or a URI of the app's own file provider, which exposes the file system root) pointing to private files of the app, e.g. host keys or preferences with the password hash. The app copied them to the quick share dir and served them, or to a chosen directory. | `share/AbstractReceiveShareActivity` | URIs pointing into the data dir of the app or to its own provider are refused. |
| S4 | **Path traversal by file name.** Name of the target file was taken from shared text or from the display name of a foreign provider without sanitising, `../` was possible. | `util/FilenameUnique` | Path separators and control characters are replaced, length is limited, `.` and `..` are not accepted. |
| S5 | **FTP data connection hijacking.** By default a single passive port (5678) is used and any host could connect to it instead of the client and receive the file or listing. | `services/FtpServerService` | Passive ip check enabled. |
| S6 | **FTP bounce.** `PORT` with an arbitrary address made the server connect to third hosts. | `services/FtpServerService` | Active ip check enabled. |
| S7 | **Weak SSH algorithms** offered by sshd 0.14 defaults. | `services/SshServerService` | `arcfour128`, `arcfour256`, `hmac-md5`, `hmac-md5-96`, `diffie-hellman-group1-sha1` removed. |
| S8 | Password hashes were compared with `String.equals()`. | `services/AndroidPrefsUserManager` | Constant time comparison. |
| S9 | Rejected data connection resulted in a NPE instead of a clean error. | `io/AndroidIoDataConnectionFactory` | `IOException`. |

## Checked, no change needed

- Shell commands of root storage: paths are put in single quotes and single
  quotes are escaped. Newlines inside single quotes do not end a command.
- `PendingIntent`s are immutable.
- `allowBackup` is disabled.
- FTP log filter of ftpserver masks passwords, the app logs the length of the
  password only.
- File provider is not exported.

## Open risks (not fixed)

| # | Risk | Why not fixed |
|---|------|---------------|
| O1 | Passwords are stored as SHA-512 with a static salt. | Needs a migration of stored passwords, should use a slow hash with random salt. |
| O2 | `sshd-core` 0.14 is very old and has known protocol weaknesses (e.g. no strict key exchange, CBC ciphers, SHA-1 MACs remain enabled for compatibility). | Replacing the library is a project of its own. |
| O3 | Tasker receivers are exported without permission. Any app can start and stop the server if the preference is enabled. | Known, covered by the security advisory referenced in the preference. Disabled by default. |
| O4 | File provider of the file picker uses `root-path`, i.e. the whole file system. | Only used inside the app, would need changes of the vendored picker. |
| O5 | FTP is not encrypted, anonymous login gives full access. | By design, user's choice. |
| O6 | Public key authentication and anonymous SSH login accept any user name. | By design. |
| O7 | No limit of failed logins for SSH besides the default of sshd. | Not analysed in depth. |
| O8 | Service started without preferences stops itself without calling `startForeground()`. | Edge case, would need a notification for a server which does not run. |
