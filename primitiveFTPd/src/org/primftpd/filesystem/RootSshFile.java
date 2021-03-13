package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.services.PftpdService;
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
    private final RootSshFileSystemView fileSystemView;

    public RootSshFile(
            Shell.Interactive shell,
            LsOutputBean bean,
            String absPath,
            PftpdService pftpdService,
            Session session,
            RootSshFileSystemView fileSystemView) {
        super(shell, bean, absPath, pftpdService);
        this.session = session;
        this.fileSystemView = fileSystemView;
    }

    protected RootSshFile createFile(Shell.Interactive shell, LsOutputBean bean, String absPath, PftpdService pftpdService) {
        return new RootSshFile(shell, bean, absPath, pftpdService, session, fileSystemView);
    }

    @Override
    public String getClientIp() {
        return SshUtils.getClientIp(session);
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
    public SshFile getParentFile() {
        logger.trace("[{}] getParentFile()", name);
        String parentPath = Utils.parent(absPath);
        logger.trace("[{}]   getParentFile() -> {}", name, parentPath);
        return fileSystemView.getFile(parentPath);
    }

    @Override
    public List<SshFile> listSshFiles() {
        return listFiles();
    }
}
