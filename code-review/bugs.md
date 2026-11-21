# Bugs

## Plain file system

| # | Problem | Location |
|---|---------|----------|
| B1 | Empty path caused `StringIndexOutOfBoundsException` in `absolute()` / `absoluteOrHome()`. | `filesystem/Utils` |
| B2 | `getParentFile()` of root dir caused NPE. | `filesystem/FsSshFile` |
| B3 | Waiting for media scanner connection could loop forever and swallowed interrupts. | `filesystem/MediaScannerClient` |
| B4 | `Utils.parent("/foo")` returned an empty string. | `filesystem/Utils` |

## Root storage

| # | Problem | Location |
|---|---------|----------|
| B5 | `user` was assigned after the home dir object had been created, home dir had a null user (NPE in owner name, unknown client ip). | `filesystem/RootFtpFileSystemView` |
| B6 | `runCommand()` unboxed a null `Boolean` if the shell did not report a result. | `filesystem/RootFile` |
| B7 | `ls` without output caused `IndexOutOfBoundsException`. | `filesystem/RootFileSystemView` |
| B8 | `.` and `..` printed by `ls -la` were returned as children. | `filesystem/RootFile` |
| B9 | Children of `/` got paths with double slash. | `filesystem/RootFile` |
| B10 | Empty tmp dir caused `ArrayIndexOutOfBoundsException` in copy mode. | `filesystem/RootFile` |
| B11 | Parent of top level files resolved to an empty path which crashed. | `filesystem/RootSshFile` via `Utils.parent()` |
| B12 | Unused link resolution built a command with unescaped quoting and could NPE. | `filesystem/RootFileSystemView` |

## SAF / read-only SAF

| # | Problem | Location |
|---|---------|----------|
| B13 | Path with a nonexistent intermediate directory resolved to the root directory. | `filesystem/RoSafFileSystemView` |
| B14 | Root object was initialised from a cursor which was not positioned, leaving a half initialised object. | `filesystem/RoSafFile` |
| B15 | Null cursors and cursors without rows were not handled, cursor leaked if positioning failed. | `filesystem/RoSafFile`, `RoSafFileSystemView`, `SafFile`, `SafFileSystemView` |
| B16 | `Toast` was created in server threads without looper, which throws. | `filesystem/SafFile` |
| B17 | Creating a `SafFile` queried the content provider for the name of the parent just for a trace message, for every file of a listing. | `filesystem/SafFile` |
| B18 | Streams returned by the content resolver may be null. | `filesystem/SafFile`, `RoSafFile` |
| B19 | List of nonexistent parent dirs was a view of another list. | `filesystem/SafFileSystemView` |

## Virtual storage and quick share

| # | Problem | Location |
|---|---------|----------|
| B20 | `/fsfoo` matched prefix `/fs` (same for other prefixes). | `filesystem/VirtualFileSystemView` |
| B21 | NPE when listing or posting client actions for paths without delegate. | `filesystem/VirtualFile` |
| B22 | Move with a target without delegate caused NPE, move between different kinds of storage used paths of the wrong storage. | `filesystem/VirtualFile` |
| B23 | Parent of an SFTP file was resolved in the namespace of the delegate (prefix lost) or was null. | `filesystem/VirtualSshFile` |
| B24 | Empty argument of `CWD` crashed. | `filesystem/VirtualFtpFileSystemView` |
| B25 | Home and working dir of quick share FTP view were reported as file. | `filesystem/QuickShareFtpFileSystemView` |
| B26 | File name `..` pointed to parent of tmp dir. | `filesystem/QuickShareFileSystemView` |

## Thread-safety

| # | Problem | Location |
|---|---------|----------|
| B27 | Shared `SimpleDateFormat` instances used by several connections. | `pojo/LsOutputParser`, `filesystem/Utils`, `filesystem/RootFile`, `util/FilenameUnique` |
| B28 | `HashMap` shared by connections. | `services/AndroidPrefsUserManager`, `filesystem/StorageManagerUtil` |
| B29 | Flags of download service shared by threads without `volatile`. | `services/DownloadsService` |
| B30 | Static wake lock accessed without synchronisation. | `util/WakelockUtil` |

## Services, UI and others

| # | Problem | Location |
|---|---------|----------|
| B31 | Start intent without extras caused NPE. | `services/AbstractServerService` |
| B32 | Failing registration of service announcement crashed the server start, unregistering a null listener crashed the stop. | `services/AbstractServerService` |
| B33 | NPE if password is not set, null user name. | `services/AndroidPrefsUserManager` |
| B34 | Idle timeout was multiplied without null check. | `services/SshServerService` |
| B35 | Ed25519 host key was only found if key types had been queried before. | `services/SshServerService` |
| B36 | Tile may be null, double registration at event bus throws. | `services/QuickSettingsService` |
| B37 | `updateAppWidget()` replaces all views, click handlers were not set again. | `util/ServicesStartStopUtil` |
| B38 | `ExecutorService` in try-with-resources: `close()` is not available on older Android versions and blocks the main thread; progress dialog was never shown. | `ui/CleanSpaceFragment` |
| B39 | Text of client actions grew without limit. | `ui/ClientActionFragment` |
| B40 | Time of log entries used 12 hour format without am/pm, quotes were not escaped. | `log/CsvEncoder` |
| B41 | Stream of sample authorized keys file was never closed. | `util/SampleAuthKeysFileCreator` |
| B42 | `reader.ready()` was used to detect end of file; comments and lines with several blanks produced errors. | `pojo/KeyParser` |
| B43 | Missing null checks for share intents and results of document picker, streams not closed independently. | `share/*` |
| B44 | Stop message for downloads was handled by a service which might be gone. | `services/DownloadsService` |
