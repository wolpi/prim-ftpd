#!/usr/bin/python3

from getpass import getpass
from shutil import copyfile
import subprocess
import os
import sys
import json

def doGithubUpload(githubToken, uploadUrl, apkPath, name):
    subprocess.run([
        "curl", "-v",
        "-X", "POST",
        "-H", "Authorization: token " + githubToken,
        "-H", "Accept: application/vnd.github.v3+json",
        "-H", "Content-Type: application/octet-stream",
        "--data-binary", "@" + apkPath,
              uploadUrl + "?name=" + "primitiveFTPd-" + name + ".apk",
              ], stdout=subprocess.PIPE, check=True)

def doRemoteGithubThings(tagName, tagNameGooglePlay, apkPath, apkPathGoogleplay):
    # check if origin uses ssh url
    proc = subprocess.run([
        "git",
        "remote",
        "-v"
    ], stdout=subprocess.PIPE, check=True)
    print("found git remotes:")
    print(proc.stdout)
    print()
    isSshUrl = 'origin\tgit@' in proc.stdout.decode('utf8')

    # check if GITHUB_TOKEN is set
    githubTokenPresent = False
    githubToken = None
    try:
        githubToken = os.environ['GITHUB_TOKEN']
        githubTokenPresent = len(githubToken) > 1
    except:
        dummy=True

    milestoneNumber = None
    if isSshUrl and githubTokenPresent:
        # find github milestone id
        proc = subprocess.run([
            "gh",
            "api",
            "/repos/wolpi/prim-ftpd/milestones"
        ], stdout=subprocess.PIPE, check=True)
        milestoneList = json.loads(proc.stdout)
        for milestone in milestoneList:
            print("checking milestone: " + milestone['title'])
            if milestone['title'] == releaseVersion:
                milestoneNumber = milestone['number']
                break
    milestoneFound = milestoneNumber != None

    if isSshUrl and githubTokenPresent and milestoneFound:
        # push master
        print()
        print("pushing master")
        subprocess.run([
            "git",
            "push",
            "origin",
            "master"
        ], stdout=subprocess.PIPE, check=True)

        # push tags
        print()
        print("pushing tag")
        subprocess.run([
            "git",
            "push",
            "origin",
            tagName
        ], stdout=subprocess.PIPE, check=True)

        print()
        print("pushing tag google play")
        subprocess.run([
            "git",
            "push",
            "origin",
            tagNameGooglePlay
        ], stdout=subprocess.PIPE, check=True)

        # close milestone
        print()
        print("closing github milestone")
        subprocess.run([
            "gh", "api",
            "--method", "PATCH",
            "-f", "state='closed'",
            "/repos/wolpi/prim-ftpd/milestones/" + str(milestoneNumber),
            ], stdout=subprocess.PIPE, check=True)

        # create new milestone
        print()
        print("creating new github milestone")
        subprocess.run([
            "gh", "api",
            "--method", "POST",
            "-f", "title='" + str(newVersion) + "'",
            "/repos/wolpi/prim-ftpd/milestones",
            ], stdout=subprocess.PIPE, check=True)

        # create github release
        print()
        print("creating github release")
        proc = subprocess.run([
            "gh", "api",
            "/repos/wolpi/prim-ftpd/releases",
            "--method", "POST",
            "-f", "tag_name='" + str(tagName) + "'",
            "-f", "name='" + str(tagName) + "'",
            "-f", "body='See [milestone](https://github.com/wolpi/prim-ftpd/milestone/" + str(milestoneNumber) + "?closed=1) for changes.\n'",
            "-f", "target_commitish='master'",
            "-f", "draft=false",
            "-f", "prerelease=false",
            ], stdout=subprocess.PIPE, check=True)
        releaseObj = json.loads(proc.stdout)

        # upload file
        print()
        print("uploading apk to github")
        # cannot use 'gh' as uploads have different hostname
        uploadUrl = releaseObj['upload_url']
        if '{' in uploadUrl:
            index = uploadUrl.index('{')
            uploadUrl = uploadUrl[0:index]
        print("using upload url: " + uploadUrl)
        print()
        doGithubUpload(githubToken, uploadUrl, apkPath, releaseVersion)
        doGithubUpload(githubToken, uploadUrl, apkPathGoogleplay, releaseVersion + "-googleplay")
        print()
        print("done")
        print()

    else:
        print()
        if not isSshUrl:
            print("you are not using ssh url, stopping")
        if not githubTokenPresent:
            print("env var GITHUB_TOKEN not present, stopping")
        if not milestoneFound:
            print("no matching github milestone found, stopping")
        print("you should push and manage release as well as milestone!!!")
        print()


def runBuild():
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


def commit(msg, path):
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

def gitTag(tag):
    print("tagging")
    print()
    subprocess.run([
        "git",
        "tag",
        "-a",
        tag,
        "-m",
        "'" + tag + "'"
    ], stdout=subprocess.PIPE, check=True)


def copyToReleasesDir(isGoogleplayVersion):
    apkPath = "primitiveFTPd/build/outputs/apk/release/primitiveFTPd-release.apk"
    if len(releasesPath) > 0:
        print("copy to releases dir")
        targetPath = releasesPath + "/primitiveFTPd-" + \
                     (releaseVersion if fullBuild else oldSnapshotVersion) + \
                     ("-googleplay" if isGoogleplayVersion else "") + \
                     ".apk"
        copyfile(apkPath, targetPath)
        return targetPath
    else:
        print("releases dir not set, no copy")



def handleGooglePlayVersion(fullBuild, releaseVersion):
    print("handling version for google play")
    manifestPath = "primitiveFTPd/AndroidManifest.xml"
    permissionExternalStorageEnabled = '<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />'
    permissionExternalStorageOutcommented = '<!--<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />-->'

    content = open(manifestPath, "r").read()
    content = content.replace(permissionExternalStorageEnabled, permissionExternalStorageOutcommented)
    file = open(manifestPath, "w")
    file.write(content)
    file.close()

    runBuild()

    tagNameGooglePlay = "google-play-prim-ftpd-" + releaseVersion
    if fullBuild:
        # commit permission out commenting
        msg = "removing permission to mange external storage for version " + releaseVersion
        commit(msg, manifestPath)

        # tag
        gitTag(tagNameGooglePlay)

    # enable permission again
    content = content.replace(permissionExternalStorageOutcommented, permissionExternalStorageEnabled)
    file = open(manifestPath, "w")
    file.write(content)
    file.close()

    if fullBuild:
        # commit
        msg = "re-adding permission to mange external storage for next version after " + releaseVersion
        commit(msg, manifestPath)

    return tagNameGooglePlay


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

fullBuild = len(sys.argv) > 1 and sys.argv[1] == '--full'

storePassword = getpass("store password: ")
keyPassword = getpass("key password: ")

# figure out version
pathBuildFile = "primitiveFTPd/build.gradle"
oldVersionCode = 0
oldSnapshotVersion = ""
content = ""
with open(pathBuildFile, "r") as file:
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


if fullBuild:
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
    file = open(pathBuildFile, "w")
    file.write(content)
    file.close()

runBuild()

if fullBuild:
    # commit version change
    msg = "set version to " + releaseVersion
    commit(msg, pathBuildFile)

    # tag
    gitTagName = "prim-ftpd-" + releaseVersion
    gitTag(gitTagName)

    # google play version
    apkPath = copyToReleasesDir(False)
    gitTagNameGooglePlay = handleGooglePlayVersion(fullBuild, releaseVersion)
    apkPathGoogleplay = copyToReleasesDir(True)

    # write new snapshot version in file
    content = content.replace(oldVersionCodeLine, newVersionCodeLine)
    content = content.replace(releaseVersionLine, newSnapshotVersionLine)
    print("writing new snapshot version in file")
    print()
    file = open(pathBuildFile, "w")
    file.write(content)
    file.close()

    # commit
    msg = "set version to " + newSnapshotVersion + " and code to " + newVersionCode
    commit(msg, pathBuildFile)
else:
    apkPath = copyToReleasesDir(False)
    gitTagNameGooglePlay = handleGooglePlayVersion(False, None)
    apkPathGoogleplay= copyToReleasesDir(True)


if fullBuild:
    doRemoteGithubThings(gitTagName, gitTagNameGooglePlay, apkPath, apkPathGoogleplay)
