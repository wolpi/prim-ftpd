# Primitive FTPd

FTP server app for android.

<!--
[![Build Status](https://travis-ci.org/wolpi/prim-ftpd.png)](https://travis-ci.org/wolpi/prim-ftpd)
-->
![Code Size](https://img.shields.io/github/languages/code-size/wolpi/prim-ftpd.svg?style=popout)

[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=Q8TU8ZQX3WV8J)

[<img alt="Get it on F-Droid" height="60" src="https://f-droid.org/badge/get-it-on.png" />](https://f-droid.org/app/org.primftpd)

[<img alt="Get it on Google Play" height="60" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" />](https://play.google.com/store/apps/details?id=org.primftpd)

[<img alt="Get it on Amazon" height="45" style="margin-left: 8px;" src="https://images-na.ssl-images-amazon.com/images/G/01/AmazonMobileApps/amazon-apps-store-us-black.png" />](http://www.amazon.com/wolpi-primitive-FTPd/dp/B00KERCPNY/ref=sr_1_1)


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
* Optional support for Android Storage Access Framework to access external sd-card the official way (NOTE requires selecting a directory, not the root of the sd-card).


## Development Snapshot
You may download latest development snapshot from [GitHub packages](https://github.com/wolpi/prim-ftpd/packages/).


## Translation
You may help translate this app in [hosted weblate](https://hosted.weblate.org/projects/pftpd/pftpd/).


## Permission
Google introduces more and more restrictions to filesystem access to Android. In order to access all
your files through this server you might have to grant it 'all files access' permission in Anroid settings.

![permission screen 1](fastlane/img/permission1.png)
![permission screen 2](fastlane/img/permission2.png)
![permission screen 3](fastlane/img/permission3.png)
![permission screen 4](fastlane/img/permission4.png)
![permission screen 5](fastlane/img/permission5.png)

To be able to allow 'All files access' an app must declare `android.permission.MANAGE_EXTERNAL_STORAGE` in it's manifest file.
Google has a policy wether an app is allowed to declare that permission and be published on Google Play.
As you can see below this app is considered as not compliant.

How can you use this server on your device to access your files?
* Install from f-droid
* Download from GitHub releases
* Use SAF
* Use QuickShare

Mail from Google:

![google play policy mail](fastlane/img/google-play-policy-mail.png)
