package org.primftpd.pojo;

import java.util.Date;

public class LsOutputBean {
    private final boolean exists;

    private final boolean isFile;
    private final boolean isDir;
    private final boolean isLink;

    private final boolean userReadable;
    private final boolean userWritable;
    private final boolean userExecutable;

    private final boolean groupReadable;
    private final boolean groupWritable;
    private final boolean groupExecutable;

    private final boolean otherReadable;
    private final boolean otherWritable;
    private final boolean otherExecutable;

    private final boolean hasAcl;

    private final long linkCount;
    private final long size;

    private final Date date;

    private final String user;
    private final String group;
    private final String name;
    private final String linkTarget;

    public LsOutputBean(String name) {
        this.exists = false;
        this.name = name;

        this.isFile = false;
        this.isDir = false;
        this.isLink = false;
        this.userReadable = false;
        this.userWritable = false;
        this.userExecutable = false;
        this.groupReadable = false;
        this.groupWritable = false;
        this.groupExecutable = false;
        this.otherReadable = false;
        this.otherWritable = false;
        this.otherExecutable = false;
        this.hasAcl = false;
        this.linkCount = 0;
        this.size = 0;
        this.date = null;
        this.user = null;
        this.group = null;
        this.linkTarget = null;
    }

    public LsOutputBean(
            boolean isFile,
            boolean isDir,
            boolean isLink,
            boolean userReadable,
            boolean userWritable,
            boolean userExecutable,
            boolean groupReadable,
            boolean groupWritable,
            boolean groupExecutable,
            boolean otherReadable,
            boolean otherWritable,
            boolean otherExecutable,
            boolean hasAcl,
            long linkCount,
            long size,
            Date date,
            String user,
            String group,
            String name,
            String linkTarget) {
        this.exists = true;
        this.isFile = isFile;
        this.isDir = isDir;
        this.isLink = isLink;
        this.userReadable = userReadable;
        this.userWritable = userWritable;
        this.userExecutable = userExecutable;
        this.groupReadable = groupReadable;
        this.groupWritable = groupWritable;
        this.groupExecutable = groupExecutable;
        this.otherReadable = otherReadable;
        this.otherWritable = otherWritable;
        this.otherExecutable = otherExecutable;
        this.hasAcl = hasAcl;
        this.linkCount = linkCount;
        this.size = size;
        this.date = date;
        this.user = user;
        this.group = group;
        this.name = name;
        this.linkTarget = linkTarget;
    }

    public boolean isExists() {
        return exists;
    }

    public boolean isFile() {
        return isFile;
    }

    public boolean isDir() {
        return isDir;
    }

    public boolean isLink() {
        return isLink;
    }

    public boolean isUserReadable() {
        return userReadable;
    }

    public boolean isUserWritable() {
        return userWritable;
    }

    public boolean isUserExecutable() {
        return userExecutable;
    }

    public boolean isGroupReadable() {
        return groupReadable;
    }

    public boolean isGroupWritable() {
        return groupWritable;
    }

    public boolean isGroupExecutable() {
        return groupExecutable;
    }

    public boolean isOtherReadable() {
        return otherReadable;
    }

    public boolean isOtherWritable() {
        return otherWritable;
    }

    public boolean isOtherExecutable() {
        return otherExecutable;
    }

    public boolean isHasAcl() {
        return hasAcl;
    }

    public long getLinkCount() {
        return linkCount;
    }

    public long getSize() {
        return size;
    }

    public Date getDate() {
        return date;
    }

    public String getUser() {
        return user;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getLinkTarget() {
        return linkTarget;
    }
}
