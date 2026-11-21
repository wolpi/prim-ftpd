# Code review – 2026-11-21

Review of the app code of primitive ftpd, covering correctness bugs, handling of
large files / long running transfers and security risks. All fixes described
here are on branch `fix/review-large-files-hardening`
(commits `3554ec2` and `ec9f15d`, 48 files).

| File | Content |
|------|---------|
| [large-files.md](large-files.md) | Problems with large files and long running transfers |
| [bugs.md](bugs.md) | Functional bugs, crashes, thread-safety, resource leaks |
| [security.md](security.md) | Security risks, hardening, behaviour changes, open risks |
| [verification.md](verification.md) | What has been verified and what has not |

## Scope

Read in full:

- `primitiveFTPd/src/org/primftpd/filesystem` (plain, root, SAF, read-only SAF, quick share, virtual)
- `primitiveFTPd/src/org/primftpd/io` (FTP listener and data connections)
- `primitiveFTPd/src/org/primftpd/services`
- `primitiveFTPd/src/org/primftpd/share`
- `primitiveFTPd/src/org/primftpd/log`, receivers, widget provider
- most of `primitiveFTPd/src/org/primftpd/util` and `prefs`
- `pftpd-pojo-lib` (`KeyParser`, `LsOutputParser`)
- `AndroidManifest.xml` (exported components, permissions, providers)

Only scanned for risky patterns, not read line by line:

- `primitiveFTPd/src/org/primftpd/ui` (`CleanSpaceFragment` and
  `ClientActionFragment` were read in full, key generation in `PftpdFragment` partly)
- `remotecontrol` edit activities

Not reviewed:

- vendored file picker (`filepicker/nononsenseapps`)
- vendored `sshd-core-0.14.0`, apart from the SFTP file handle in `SftpSubsystem`
- `crypto/HostKeyAlgorithm`, `crypto/SignatureEd25519`
- `util/StringUtils` (copied library code)
- resources, translations, build scripts

## Summary

- **Large files:** long transfers were cut off by three independent timeouts
  (hard coded 60s FTP idle time, server auto stop timer, 60 min wake lock).
  Transfer code was slow (4k buffers) and some code paths were broken for
  offsets / resume. Copying of shared files ran on the main thread.
- **Bugs:** about 30 functional bugs, mostly in root, read-only SAF and
  virtual storage, plus thread-safety problems of shared date formats.
- **Security:** IP filter failed open, share intents could make the app copy
  its own private files, file names of shared data were not sanitised,
  FTP data connections were not bound to the client, weak SSH algorithms
  were offered.

## Behaviour changes to be aware of

1. With idle timeout preference at 0 (default) idle FTP sessions are not
   closed after 60 seconds anymore.
2. FTP data connections must come from the same IP address as the control
   connection (passive and active mode).
3. SSH server does not offer `arcfour128`, `arcfour256`, `hmac-md5`,
   `hmac-md5-96` and `diffie-hellman-group1-sha1` anymore.
4. SAF: overwriting an existing file truncates it, uploads at an offset equal
   to the current size append, other offsets are rejected with an error.
