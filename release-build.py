#!/usr/bin/python3

from getpass import getpass
from shutil import copyfile
import subprocess
import os
import sys

# check env vars
keystorePath = ""
releasesPath = ""
try:
    keystorePath = os.environ['PFTPD_KEYSTORE']
    releasesPath = os.environ['PFTPD_RELEASES']
except:
    dummy=True

if len(keystorePath) < 1:
    print("env var PFTPD_KEYSTORE not set!")
    sys.exit(-1)


storePassword = getpass("store password: ")
keyPassword = getpass("key password: ")

# figure out version
path = "primitiveFTPd/build.gradle"
oldVersionCode = 0
oldSnapshotVersion = ""
content = ""
with open(path, "r") as file:  
    content = file.read()
 
    index = content.find("versionCode")
    endIndex = content.find("\n", index)
    line = content[index:endIndex]
    oldVersionCode = line[line.find(" ")+1:len(line)]

    index = content.find("versionName")
    endIndex = content.find("\n", index)
    line = content[index:endIndex]
    oldSnapshotVersion = line[line.find("\"")+1:len(line)-1]

newVersionCode = str(int(oldVersionCode)+1)
releaseVersion = oldSnapshotVersion[0:oldSnapshotVersion.find("-")]
major = oldSnapshotVersion[0:releaseVersion.find(".")]
oldMinor = int(releaseVersion[releaseVersion.find(".")+1:len(releaseVersion)])
newMinor = oldMinor + 1
newVersion = major + "." + str(newMinor)
newSnapshotVersion = newVersion + "-SNAPSHOT"

print("oldVersionCode: " + oldVersionCode)
print("newVersionCode: " + newVersionCode)
print("oldSnapshotVersion: " + oldSnapshotVersion)
print("releaseVersion: " + releaseVersion)
print("major: " + major)
print("oldMinor: " + str(oldMinor))
print("newMinor: " + str(newMinor))
print("newVersion: " + newVersion)
print("newSnapshotVersion: " + newSnapshotVersion)
print()

# write release version in file
oldVersionCodeLine = "versionCode " + oldVersionCode
newVersionCodeLine = "versionCode " + newVersionCode 
oldVersionLine = "versionName \"" + oldSnapshotVersion + "\""
releaseVersionLine = "versionName \"" + releaseVersion + "\""
newSnapshotVersionLine = "versionName \"" + newSnapshotVersion + "\""

content = content.replace(oldVersionLine, releaseVersionLine)
print("writing release version in file")
print()
file = open(path, "w")
file.write(content)
file.close()

# run build
print("running build")
print()
subprocess.run([
    "./gradlew",
    "clean",
    "assembleRelease",
    "-Pandroid.injected.signing.store.file=" + keystorePath,
    "-Pandroid.injected.signing.key.alias=prim fptd sign key",
    "-Pandroid.injected.signing.store.password=" + storePassword,
    "-Pandroid.injected.signing.key.password=" + keyPassword
], stdout=subprocess.PIPE, check=True)

# commit version change
msg = "set version to " + releaseVersion
print("commiting, \"" + msg + "\"")
print()
subprocess.run([
    "git",
    "add",
    path
], stdout=subprocess.PIPE, check=True)
subprocess.run([
    "git",
    "commit",
    "-m",
    msg
], stdout=subprocess.PIPE, check=True)

# tag
print("tagging")
print() #git tag -a prim-ftpd-1.0.1 -m 'prim-ftpd-1.0.1'
subprocess.run([
    "git",
    "tag",
    "-a",
    "prim-ftpd-" + releaseVersion,
    "-m",
    "'prim-ftpd-" + releaseVersion + "'"
], stdout=subprocess.PIPE, check=True)

# write new snapshot version in file
content = content.replace(oldVersionCodeLine, newVersionCodeLine)
content = content.replace(releaseVersionLine, newSnapshotVersionLine)
print("writing new snapshot version in file")
print()
file = open(path, "w")
file.write(content)
file.close()

# commit
msg = "set version to " + newSnapshotVersion + " and code to " + newVersionCode
print("commiting, \"" + msg + "\"")
print()
subprocess.run([
    "git",
    "add",
    path
], stdout=subprocess.PIPE, check=True)
subprocess.run([
    "git",
    "commit",
    "-m",
    msg
], stdout=subprocess.PIPE, check=True)

if len(releasesPath) > 0:
    print("copy to releases dir")
    copyfile("primitiveFTPd/build/outputs/apk/release/primitiveFTPd-release.apk", releasesPath + "/primitiveFTPd-" + releaseVersion + ".apk")
else:
    print("releases dir not set, no copy")

print()
print("you should push !!!")
print()
