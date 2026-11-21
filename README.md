# Primitive FTPd

FTP and SFTP server app for Android with external SD-card access.

<!--
[![Build Status](https://travis-ci.org/wolpi/prim-ftpd.png)](https://travis-ci.org/wolpi/prim-ftpd)
-->

![Code Size](https://img.shields.io/github/languages/code-size/wolpi/prim-ftpd.svg?style=popout)

[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=Q8TU8ZQX3WV8J)

How can you use this server on your device to access your files (see discussion regarding permission and restriction below in terms of Google and Play)?
* Install from F-Droid
* Download from GitHub releases
* Use SAF (if you don't grant full filesystem access)
* Use QuickShare (if you don't grant full filesystem access)

[<img alt="Get it on F-Droid" height="60" src="https://f-droid.org/badge/get-it-on.png" />](https://f-droid.org/app/org.primftpd)


You may download latest development snapshot from [GitHub packages](https://github.com/wolpi/prim-ftpd/packages/).

## Some features:
* Can optionally be started on system boot
* Shows statusbar notification when server is running
* Server can be stopped from statusbar
* Shows information about how to connect on main screen
* Optional wakelock while server runs to avoid uploads and downloads to be aborted
* Optional encryption via sftp
* Server can be announced
* Public key authentication for sftp
* Optional anonymous login
* Widget to start/stop server
* Plugins for powertoggles and tasker
* Android 7 Quicksettings Tile
* Optional root access
* Optional support for Android Storage Access Framework to be able to write external SD-cards
* Can calculate hash for complete files on server side (sftp extended command)

## Works without root

Root is optional. The default storage type ("Plain old filesystem") and the storage types
based on the Android Storage Access Framework need no root, a root shell is only opened for
the storage types "Super user" and "Virtual Folders".

On a device which is not rooted:
* Grant 'all files access' (see below) or use SAF / QuickShare.
* Allow notifications, so the server can be stopped from the statusbar.
* Ports must be above 1024 (defaults: 12345 for ftp, 1234 for sftp).
* Storage type "Super user" can not be used, the server tells you so when you try to start it.

Details: [code-review/non-root.md](code-review/non-root.md),
Android 10 and higher: [code-review/android-versions.md](code-review/android-versions.md).

## Large files

Files larger than 4 GB can be transferred by ftp, sftp and scp, interrupted transfers can be resumed
(downloads with all storage types, uploads with plain storage and SAF).
This has been tested with the server code running on a desktop JVM and plain storage, not yet on a device.
Some hints for transfers which take long:
* Keep the wakelock enabled, it is renewed while data is transferred.
* The server is not stopped by the 'idle timeout to stop server' while data is transferred.
* 'Idle timeout' of connections (default: none) does not interrupt running transfers.
* With storage type "Super user" the option to copy files to a tmp dir needs twice the space, resuming uploads is not supported.

Details: [code-review/large-files.md](code-review/large-files.md),
tests with 5 GiB files: [code-review/verification.md](code-review/verification.md).

## Documentation in this repository

* [architecture/](architecture/README.md): how the app is built (components, runtime flows, storage types, Android integration)
* [code-review/](code-review/README.md): findings of the code reviews, what has been verified
* [code-review/improve/](code-review/improve/README.md): what has been improved

## Translation
You may help translate this app in [hosted weblate](https://hosted.weblate.org/projects/pftpd/pftpd/).

[<img alt="Translation State Overview" src="https://hosted.weblate.org/widget/pftpd/287x66-black.png" />](https://hosted.weblate.org/engage/pftpd/)

[<img alt="Translation State Detail" src="https://hosted.weblate.org/widget/pftpd/horizontal-auto.svg" />](https://hosted.weblate.org/engage/pftpd/)

## App signing key

Pre-built releases published here on GitHub are signed with a key having this fingerprints:

```
SHA1: 51:C8:76:95:D7:D7:69:FD:65:6E:55:41:79:EE:09:84:CD:DA:83:3D
SHA256: 71:51:AE:6D:56:F1:56:E6:72:24:E7:CA:3F:9C:00:DA:6F:70:30:B6:6C:C8:44:3F:3B:B4:D3:F8:4E:EB:5C:95
```


## SFTP vs SCP

When accessing this app's server via scp, it might be necessary for some clients (specific versions of ssh coming with linux distros), to specify option -O. 

```
scp -O ...
```

Probably you should generate new hostkeys for pftpd in such a case. Since version 7.4 the app shows at which time hostkeys had been generated. That gives you an indication if they grew too old.


## Running in the background

Depending your Android version, Android keeps closing apps running in the background. Even if you think you configured explicitly not do this. Please visit https://dontkillmyapp.com/ on more information about battery saving options.

## External SD card read/write access - Android Storage Access Framework

Basically it requires selecting a directory, not the root of the SD-card.

If you select the root of the SD-card, it will work temporarily, but the permission possibly gets revoked and you have to re-select regularly. Revocation can happen as soon as you close the app, or Android kills it within a few hours or days (see above).

Depending on your Android version, permanent access to the root of the SD-card can be possible, if you first select a directory on your SD-card, then select the root of the SD-card. Though after an update, maybe you have to repeat these steps again. You can check whether the permission is stored permanently by visiting Android Settings &#8594; Apps &#8594; Special access &#8594; Directory access. If you see the primitive ftpd app there after you selected the root of the SD-card, there is a chance, that the permission to access the root of the SD-card will not be revoked.

## All files access permission

Google introduces more and more restrictions to filesystem access to Android. In order to access all
your files through this server you might have to grant it 'all files access' permission in Anroid settings.

![permission screen 1](fastlane/img/permission1.png)
![permission screen 2](fastlane/img/permission2.png)
![permission screen 3](fastlane/img/permission3.png)
![permission screen 4](fastlane/img/permission4.png)
![permission screen 5](fastlane/img/permission5.png)

To be able to allow 'All files access' an app must declare `android.permission.MANAGE_EXTERNAL_STORAGE` in it's manifest file.
Google has a policy wether an app is allowed to declare that permission and be published on Google Play.
As you can see below this app is considered as not compliant (which is not relevant anymore).

Mail from Google (has been relevant as long as the app has been available on Google Play):

![google play policy mail](fastlane/img/google-play-policy-mail.png)
