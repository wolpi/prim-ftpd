# Code review

Reviews of the app code of primitive ftpd on branch
`fix/review-large-files-hardening`.

| Review | Date | Result |
|--------|------|--------|
| First | 2026-11-21 | 22 problems with large files, 44 bugs, 9 security issues fixed. Commits `3554ec2`, `ec9f15d`. |
| Second | 2026-11-21 | Re-check with real clients and 5 GiB files, use without root, Android 10+. 7 transfer problems, 11 bugs, 1 security issue fixed. |

| File | Content |
|------|---------|
| [large-files.md](large-files.md) | First review: problems with large files and long running transfers |
| [bugs.md](bugs.md) | First review: functional bugs, crashes, thread-safety, resource leaks |
| [security.md](security.md) | First review: security risks, hardening, behaviour changes, open risks |
| [round-2.md](round-2.md) | Second review: all findings, open points, coverage |
| [non-root.md](non-root.md) | Does the app work without root? |
| [android-versions.md](android-versions.md) | Does the app work on Android 10 and higher? |
| [verification.md](verification.md) | What has been verified and what has not, for both reviews |
| [improve/](improve/README.md) | What has been changed in the code, per review |

The architecture of the project is described in
[../architecture/](../architecture/README.md).

The rest of this page describes the first review.

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
