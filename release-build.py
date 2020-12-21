#!/usr/bin/python3

from getpass import getpass
from shutil import copyfile
import subprocess
import os
import sys
import json


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

gitTag = "prim-ftpd-" + releaseVersion
if fullBuild:
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
    print()
    subprocess.run([
        "git",
        "tag",
        "-a",
        gitTag,
        "-m",
        "'" + gitTag + "'"
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

apkPath = "primitiveFTPd/build/outputs/apk/release/primitiveFTPd-release.apk"
if len(releasesPath) > 0:
    print("copy to releases dir")
    targetPath = releasesPath + "/primitiveFTPd-" + (releaseVersion if fullBuild else oldSnapshotVersion) + ".apk"
    copyfile(apkPath, targetPath)
else:
    print("releases dir not set, no copy")

if fullBuild:
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

        # push tag
        print()
        print("pushing tag")
        subprocess.run([
            "git",
            "push",
            "origin",
            gitTag
        ], stdout=subprocess.PIPE, check=True)

        # create github release
        print()
        print("creating github release")
        proc = subprocess.run([
            "gh", "api",
            "/repos/wolpi/prim-ftpd/releases",
            "-X", "POST",
            "-F", "tag_name=" + str(gitTag),
            "-F", "name=" + str(gitTag),
            "-F", "body=See [milestone](https://github.com/wolpi/prim-ftpd/milestone/" + str(milestoneNumber) + "?closed=1) for changes.\n",
            "-F", "target_commitish=master",
            "-F", "draft=false",
            "-F", "prerelease=false",
        ], stdout=subprocess.PIPE, check=True)
        releaseObj = json.loads(proc.stdout)

        # close milestone
        print()
        print("closing github milestone")
        subprocess.run([
            "gh", "api",
            "-X", "PATCH",
            "-F", "state=closed",
            "/repos/wolpi/prim-ftpd/milestones/" + str(milestoneNumber),
        ], stdout=subprocess.PIPE, check=True)

        # create new milestone
        print()
        print("creating new github milestone")
        subprocess.run([
            "gh", "api",
            "-X", "POST",
            "-F", "title=" + str(newVersion),
            "/repos/wolpi/prim-ftpd/milestones",
        ], stdout=subprocess.PIPE, check=True)

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
        subprocess.run([
            "curl", "-v",
            "-X", "POST",
            "-H", "Authorization: token " + githubToken,
            "-H", "Accept: application/vnd.github.v3+json",
            "-H", "Content-Type: application/octet-stream",
            "--data-binary", "@" + apkPath,
            uploadUrl + "?name=" + "primitiveFTPd-" + releaseVersion + ".apk",
        ], stdout=subprocess.PIPE, check=True)
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

