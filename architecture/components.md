# Components

## Modules

```mermaid
flowchart TD
    APP[primitiveFTPd<br/>Android application]
    POJO[pftpd-pojo-lib<br/>plain Java]
    SSHD[sshd-core-0.14.0<br/>Android library, vendored]
    APP --> POJO
    APP --> SSHD
    SSHD --> POJO
```

## Packages of the app (`org.primftpd`)

| Package | Responsibility | Most important classes |
|---------|----------------|------------------------|
| *(root)* | Application object and entry points which are not activities. | `PftpdApp` (registers BouncyCastle as first security provider, initialises logging), `BootUpReceiver` (start on boot), `StartStopWidgetProvider` (widget, also target of notification actions) |
| `ui` | Screens. | `MainTabsActivity` (tabs), `PftpdFragment` (main screen: server state, addresses, storage type, permissions, key generation), `QrFragment`, `CleanSpaceFragment`, `ClientActionFragment`, `KeysFingerprintsFragment`, `PubKeyAuthKeysFragment`, `AboutFragment`, `LeanbackActivity` / `LeanbackFragment` (Android TV), `SharedViewModel`, `StartServerAndExitActivity`, `StopServerAndExitActivity` (shortcuts) |
| `prefs` | Preferences. | `LoadPrefsUtil` (keys, defaults, reads `SharedPreferences`), `PrefsBean` (immutable, serializable snapshot handed to the services), `StorageType`, `ServerToStart`, `Logging`, `FtpPrefsFragment` and custom preference widgets (`PortEditTextPreference`, `FtpPassivePortsEditTextPreference`, `EncryptingEditTextPreference`) |
| `services` | The servers as Android services. | `AbstractServerService`, `FtpServerService`, `SshServerService`, `ServerServiceHandler`, `AndroidPrefsUserManager`, `PubKeyAuthenticator`, `PftpdService` (interface the file system classes use), `QuickSettingsService` (tile), `DownloadsService` (download a shared URL) |
| `io` | FTP transport adapted to Android. | `PrimNioListener`, `PrimFtpHandlerAdapter`, `PrimFtpIoSession`, `AndroidIoDataConnectionFactory`, `AndroidIoDataConnection`, `CountingReadableByteChannel`, `CountingWritableByteChannel` |
| `filesystem` | Mapping of server file APIs to storage. | See [storage.md](storage.md) |
| `share` | Receiving data shared by other apps. | `ReceiveQuickShareActivity`, `ReceiveSaveAsActivity`, `AbstractReceiveShareActivity`, `TargetDir`, `QuickShareBean` |
| `remotecontrol` | Control by other apps. | `TaskerReceiverFire`, `TaskerReceiverQuery`, `TaskerEditActionActivity`, `TaskerEditConditionActivity`, `PftpdPowerTogglesPlugin` |
| `events` | Messages on the event bus. | `ClientActionEvent`, `ServerStateChangedEvent`, `ServerInfoRequestEvent`, `ServerInfoResponseEvent`, `DataTransferredEvent` |
| `crypto` | SSH host keys. | `HostKeyAlgorithm` (ed25519, ECDSA 256, RSA 2048 / 4096: generate, store, read), `SignatureEd25519` (ed25519 signatures for the old SSH library) |
| `log` | Logging. | `LogController` (configures logback from the logging preference), `CsvEncoder` |
| `util` | Helpers. | `ServicesStartStopUtil`, `NotificationUtil`, `WakelockUtil`, `Defaults`, `IpAddressProvider`, `RemoteIpChecker`, `KeyFingerprintProvider`, `FilenameUnique`, `EncryptionUtil`, `StringUtils` |
| `filepicker` | Directory picker (vendored NoNonsense-FilePicker with small wrappers). | `ResettingFilePickerActivity` |

## Services in detail

```mermaid
classDiagram
    class AbstractServerService {
        <<abstract>>
        PrefsBean prefsBean
        onStartCommand()
        onDestroy()
        launchServer(shell) boolean
        stopServer()
        postClientAction()
    }
    class FtpServerService
    class SshServerService
    class ServerServiceHandler {
        handleStart()
        handleStop()
    }
    class PftpdService {
        <<interface>>
        postClientAction()
        getPrefsBean()
        getContext()
    }
    AbstractServerService <|-- FtpServerService
    AbstractServerService <|-- SshServerService
    PftpdService <|.. AbstractServerService
    AbstractServerService --> ServerServiceHandler : messages START / STOP
    FtpServerService --> AndroidPrefsUserManager
    SshServerService --> AndroidPrefsUserManager : password login
    SshServerService --> PubKeyAuthenticator : key login
```

* `AbstractServerService` owns everything both protocols have in common:
  Android life cycle, the timer which stops idle servers, renewal of the wake
  lock during transfers, DNS-SD announcement, posting of client actions.
* `FtpServerService.launchServer()` configures Apache FtpServer: listener
  (`PrimNioListener`), passive ports, IP checks of data connections, user
  manager, file system factory, login limits.
* `SshServerService.launchServer()` configures Apache SSHD: algorithms,
  authenticators, SFTP subsystem and SCP command, file system factory, host
  keys.
* `ServerServiceHandler` runs on its own `HandlerThread` per service and does
  the actual start and stop, so the main thread is never blocked. It also
  owns the state shared by both services (root shell, wake lock, number of
  running servers).

## Libraries

| Library | Used for |
|---------|----------|
| Apache FtpServer 1.2.1 (`ftplet-api`, `ftpserver-core`) | FTP protocol |
| Apache MINA core 2.2.5 | Network I/O of both servers |
| Apache MINA SSHD 0.14.0 (vendored) | SSH, SFTP, SCP |
| BouncyCastle 1.83 (`bcprov`, `bcpkix`, `bcutil`) | Key generation, key formats, ed25519 |
| greenrobot EventBus 3.3.1 | Communication between services and UI |
| slf4j 2.0.17 + logback-android 3.0.0 | Logging |
| libsuperuser 1.1.1 | Root shell for storage type "super user" |
| Apache HttpClient 5.6 | Download of shared URLs |
| zxing barcodescanner 1.9.8 | QR code of the server URL |
| AndroidX (appcompat, fragment, preference, documentfile, lifecycle, recyclerview, coordinatorlayout), Material | UI, Storage Access Framework helpers |

## `pftpd-pojo-lib`

| Class | Purpose |
|-------|---------|
| `KeyParser` | Parses lines of `authorized_keys` (RSA, ECDSA, ed25519) into `PublicKey` objects. |
| `LsOutputParser`, `LsOutputBean`, `LsOutputBuilder` | Parse the output of `ls -l` / `ls -ld`. This is how the root storage learns about files. |

Tests: `KeyParserTests`, `LsOutputParserTests` (JUnit 4) with sample key
files and `ls` outputs in `src/test/resources`.

## `sshd-core-0.14.0`

Upstream code of Apache MINA SSHD 0.14.0 with local changes. The parts the
app depends on most:

| Class | Role |
|-------|------|
| `server/sftp/SftpSubsystem` | SFTP protocol (version 3). `FileHandle` turns positional reads and writes of clients into the stream API of `SshFile`. Extended command `check-file` (hash of a file) was added for prim-sync. |
| `common/scp/ScpHelper`, `server/command/ScpCommand` | Legacy SCP protocol. |
| `common/file/SshFile`, `FileSystemView` | File interfaces implemented by `org.primftpd.filesystem`. |
| `server/session/ServerSession`, `common/session/AbstractSession` | SSH transport incl. re-keying after 1 GB or 1 hour. |
