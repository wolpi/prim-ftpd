package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.pojo.LsOutputBean;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RootSshFile extends RootFile<SshFile, RootSshFileSystemView> implements SshFile {

    private final Session session;

    public RootSshFile(RootSshFileSystemView fileSystemView, String absPath, LsOutputBean bean, Session session) {
        super(fileSystemView, absPath, bean);
        this.session = session;
    }

    protected RootSshFile createFile(String absPath, LsOutputBean bean) {
        return new RootSshFile(getFileSystemView(), absPath, bean, session);
    }

    @Override
    public String getClientIp() {
        return SshUtils.getClientIp(session);
    }

    @Override
    public boolean move(SshFile target) {
        return super.move((RootSshFile)target);
    }

    @Override
    public String readSymbolicLink() {
        logger.trace("[{}] readSymbolicLink()", name);
        return bean.getLinkTarget();
    }

    @Override
    public void createSymbolicLink(SshFile arg0)
    {
        // TODO ssh createSymbolicLink
        logger.trace("[{}] createSymbolicLink()", name);
    }

    @Override
    public String getOwner() {
        logger.trace("[{}] getOwner()", name);
        try {
            return (String)getAttribute(Attribute.Owner, false);
        } catch (IOException e) {
            logger.error("getOwner()", e);
        }
        return null;
    }

    @Override
    public Object getAttribute(Attribute attribute, boolean followLinks)
            throws IOException
    {
        logger.trace("[{}] getAttribute({})", name, attribute);
        switch (attribute) {
            case Owner:
                logger.trace("  [{}] getAttribute({}) -> {}", new Object[]{name, attribute, bean.getUser()});
                return bean.getUser();
            case Group:
                logger.trace("  [{}] getAttribute({}) -> {}", new Object[]{name, attribute, bean.getGroup()});
                return bean.getGroup();
            case IsSymbolicLink:
                logger.trace("  [{}] getAttribute({}) -> {}", new Object[]{name, attribute, bean.isLink()});
                return bean.isLink();
            case Permissions:
                Set<Permission> tmp = new HashSet<>();
                if (bean.isUserReadable())
                    tmp.add(SshFile.Permission.UserRead);
                if (bean.isUserWritable())
                    tmp.add(Permission.UserWrite);
                if (bean.isUserExecutable())
                    tmp.add(Permission.UserExecute);
                if (bean.isGroupReadable())
                    tmp.add(Permission.GroupRead);
                if (bean.isGroupWritable())
                    tmp.add(Permission.GroupWrite);
                if (bean.isGroupExecutable())
                    tmp.add(Permission.GroupExecute);
                if (bean.isOtherReadable())
                    tmp.add(Permission.OthersRead);
                if (bean.isOtherWritable())
                    tmp.add(Permission.OthersWrite);
                if (bean.isOtherExecutable())
                    tmp.add(Permission.OthersExecute);
                logger.trace("  [{}] getAttribute({}) -> {}", new Object[]{name, attribute, tmp});
                return  tmp.isEmpty() ? EnumSet.noneOf(Permission.class) : EnumSet.copyOf(tmp);
            default:
                return SshUtils.getAttribute(this, attribute);
        }
    }

    @Override
    public Map<Attribute, Object> getAttributes(boolean followLinks)
            throws IOException
    {
        logger.trace("[{}] getAttributes()", name);

        Map<SshFile.Attribute, Object> attributes = new HashMap<>();
        for (SshFile.Attribute attr : Attribute.values()) {
            attributes.put(attr, getAttribute(attr, followLinks));
        }

        return attributes;
    }

    @Override
    public boolean create() throws IOException {
        // This call is required by SSHFS, because it calls STAT on created new files.
        // This call is not required by normal clients who simply open, write and close the file.
        boolean result = runCommand("touch" + " " + escapePath(absPath));
        logger.trace("[{}] create() -> {}", name, result);
        return result;
    }

    @Override
    public SshFile getParentFile() {
        logger.trace("[{}] getParentFile()", name);
        String parentPath = Utils.parent(absPath);
        logger.trace("[{}]   getParentFile() -> {}", name, parentPath);
        return getFileSystemView().getFile(parentPath);
    }

    @Override
    public List<SshFile> listSshFiles() {
        return listFiles();
    }
}
