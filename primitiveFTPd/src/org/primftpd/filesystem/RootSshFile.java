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

import eu.chainfire.libsuperuser.Shell;

public class RootSshFile extends RootFile<SshFile> implements SshFile {

    private final Session session;

    public RootSshFile(Shell.Interactive shell, LsOutputBean bean, String absPath, Session session) {
        super(shell, bean, absPath);
        this.session = session;
    }

    protected RootSshFile createFile(Shell.Interactive shell, LsOutputBean bean, String absPath) {
        return new RootSshFile(shell, bean, absPath, session);
    }

    @Override
    public boolean move(SshFile target) {
        logger.trace("move()");
        return super.move((RootFile)target);
    }

    @Override
    public String readSymbolicLink() throws IOException {
        logger.trace("[{}] readSymbolicLink()", name);
        return bean.getLinkTarget();
    }

    @Override
    public void createSymbolicLink(SshFile arg0)
            throws IOException
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
                return SshUtils.getAttribute(this, attribute, followLinks);
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
        logger.trace("[{}] create()", name);
        // called e.g. when uploading a new file
        return true;
    }

    @Override
    public SshFile getParentFile() {
        logger.trace("[{}] getParentFile()", name);
        return null;
    }

    @Override
    public boolean isExecutable() {
        logger.trace("[{}] isExecutable()", name);
        return false;
    }

    @Override
    public void handleClose() throws IOException {
        // TODO ssh handleClose
        logger.trace("[{}] handleClose()", name);
    }

    @Override
    public List<SshFile> listSshFiles() {
        return listFiles();
    }

    @Override
    public void setAttribute(Attribute attribute, Object value) throws IOException {
        // TODO ssh setAttribute
        logger.trace("[{}] setAttribute()", name);
    }

    @Override
    public void setAttributes(Map<Attribute, Object> attributes) throws IOException {
        // TODO ssh setAttributes
        logger.trace("[{}] setAttributes()", name);
    }

    @Override
    public void truncate() throws IOException {
        // TODO ssh truncate
        logger.trace("[{}] truncate()", name);
    }
}
