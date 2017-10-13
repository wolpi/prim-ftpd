package org.primftpd.pojo;

import java.util.Date;

class LsOutputBuilder {

    private boolean isFile;
    private boolean isDir;
    private boolean isLink;

    private boolean userReadable;
    private boolean userWritable;
    private boolean userExecutable;

    private boolean groupReadable;
    private boolean groupWritable;
    private boolean groupExecutable;

    private boolean otherReadable;
    private boolean otherWritable;
    private boolean otherExecutable;

    private boolean hasAcl;

    private long linkCount;
    private long size;

    private Date date;

    private String user;
    private String group;
    private String name;
    private String linkTarget;

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

    public boolean isDir() {
        return isDir;
    }

    public void setDir(boolean dir) {
        isDir = dir;
    }

    public boolean isLink() {
        return isLink;
    }

    public void setLink(boolean link) {
        isLink = link;
    }

    public boolean isUserReadable() {
        return userReadable;
    }

    public void setUserReadable(boolean userReadable) {
        this.userReadable = userReadable;
    }

    public boolean isUserWritable() {
        return userWritable;
    }

    public void setUserWritable(boolean userWritable) {
        this.userWritable = userWritable;
    }

    public boolean isUserExecutable() {
        return userExecutable;
    }

    public void setUserExecutable(boolean userExecutable) {
        this.userExecutable = userExecutable;
    }

    public boolean isGroupReadable() {
        return groupReadable;
    }

    public void setGroupReadable(boolean groupReadable) {
        this.groupReadable = groupReadable;
    }

    public boolean isGroupWritable() {
        return groupWritable;
    }

    public void setGroupWritable(boolean groupWritable) {
        this.groupWritable = groupWritable;
    }

    public boolean isGroupExecutable() {
        return groupExecutable;
    }

    public void setGroupExecutable(boolean groupExecutable) {
        this.groupExecutable = groupExecutable;
    }

    public boolean isOtherReadable() {
        return otherReadable;
    }

    public void setOtherReadable(boolean otherReadable) {
        this.otherReadable = otherReadable;
    }

    public boolean isOtherWritable() {
        return otherWritable;
    }

    public void setOtherWritable(boolean otherWritable) {
        this.otherWritable = otherWritable;
    }

    public boolean isOtherExecutable() {
        return otherExecutable;
    }

    public void setOtherExecutable(boolean otherExecutable) {
        this.otherExecutable = otherExecutable;
    }

    public boolean isHasAcl() {
        return hasAcl;
    }

    public void setHasAcl(boolean hasAcl) {
        this.hasAcl = hasAcl;
    }

    public long getLinkCount() {
        return linkCount;
    }

    public void setLinkCount(long linkCount) {
        this.linkCount = linkCount;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLinkTarget() {
        return linkTarget;
    }

    public void setLinkTarget(String linkTarget) {
        this.linkTarget = linkTarget;
    }

    LsOutputBean build() {
        return new LsOutputBean(
                isFile,
                isDir,
                isLink,
                userReadable,
                userWritable,
                userExecutable,
                groupReadable,
                groupWritable,
                groupExecutable,
                otherReadable,
                otherWritable,
                otherExecutable,
                hasAcl,
                linkCount,
                size,
                date,
                user,
                group,
                name,
                linkTarget
        );
    }
}
