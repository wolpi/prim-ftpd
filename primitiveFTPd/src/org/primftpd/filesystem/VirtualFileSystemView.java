package org.primftpd.filesystem;

import org.primftpd.services.PftpdService;

public abstract class VirtualFileSystemView<
        TFsFile extends FsFile<TMina, ? extends FsFileSystemView>,
        TRootFile extends RootFile<TMina, ? extends RootFileSystemView>,
        TSafFile extends SafFile<TMina, ? extends SafFileSystemView>,
        TRoSafFile extends RoSafFile<TMina, ? extends RoSafFileSystemView>,
        TMina> extends AbstractFileSystemView {

    public static final String PREFIX_FS = "fs";
    public static final String PREFIX_ROOT = "superuser";
    public static final String PREFIX_SAF = "saf";
    public static final String PREFIX_ROSAF = "rosaf";

    protected final FsFileSystemView<TFsFile, TMina> fsFileSystemView;
    protected final RootFileSystemView<TRootFile, TMina> rootFileSystemView;
    protected final SafFileSystemView<TSafFile, TMina> safFileSystemView;
    protected final RoSafFileSystemView<TRoSafFile, TMina> roSafFileSystemView;

    public VirtualFileSystemView(
            PftpdService pftpdService,
            FsFileSystemView<TFsFile, TMina> fsFileSystemView,
            RootFileSystemView<TRootFile, TMina> rootFileSystemView,
            SafFileSystemView<TSafFile, TMina> safFileSystemView,
            RoSafFileSystemView<TRoSafFile, TMina> roSafFileSystemView) {
        super(pftpdService);
        this.fsFileSystemView = fsFileSystemView;
        this.rootFileSystemView = rootFileSystemView;
        this.safFileSystemView = safFileSystemView;
        this.roSafFileSystemView = roSafFileSystemView;
    }

    public abstract TMina createFile(String absPath, AbstractFile delegate);
    public abstract TMina createFile(String absPath, boolean exists);

    protected abstract String absolute(String file);

    public TMina getFile(String file) {
        String absoluteVirtualPath = absolute(file);
        logger.debug("getFile '{}', absolute: '{}'", file, absoluteVirtualPath);
        if ("/".equals(absoluteVirtualPath)) {
            return createFile(absoluteVirtualPath, true);
        } else if (absoluteVirtualPath.startsWith("/" + PREFIX_FS)) {
            String realPath = toRealPath(absoluteVirtualPath, "/" + PREFIX_FS);
            logger.debug("Using FS '{}' for '{}'", realPath, absoluteVirtualPath);
            AbstractFile delegate = fsFileSystemView.getFile(realPath);
            return createFile(absoluteVirtualPath, delegate);
        } else if (absoluteVirtualPath.startsWith("/" + PREFIX_ROOT)) {
            String realPath = toRealPath(absoluteVirtualPath, "/" + PREFIX_ROOT);
            logger.debug("Using ROOT '{}' for '{}'", realPath, absoluteVirtualPath);
            AbstractFile delegate = rootFileSystemView.getFile(realPath);
            return createFile(absoluteVirtualPath, delegate);
        } else if (absoluteVirtualPath.startsWith("/" + PREFIX_SAF)) {
            String realPath = toRealPath(absoluteVirtualPath, "/" + PREFIX_SAF);
            logger.debug("Using SAF '{}' for '{}'", realPath, absoluteVirtualPath);
            AbstractFile delegate = safFileSystemView.getFile(realPath);
            return createFile(absoluteVirtualPath, delegate);
        } else if (absoluteVirtualPath.startsWith("/" + PREFIX_ROSAF)) {
            String realPath = toRealPath(absoluteVirtualPath, "/" + PREFIX_ROSAF);
            logger.debug("Using ROSAF '{}' for '{}'", realPath, absoluteVirtualPath);
            AbstractFile delegate = roSafFileSystemView.getFile(realPath);
            return createFile(absoluteVirtualPath, delegate);
        } else{
            logger.debug("Using VirtualFile for unknown path '{}'", absoluteVirtualPath);
            return createFile(absoluteVirtualPath, false);
        }
    }

    private String toRealPath(String path, String prefix) {
        String realPath = path.substring(prefix.length());
        if (realPath.isEmpty()) {
            realPath = "/";
        }
        return realPath;
    }
}
