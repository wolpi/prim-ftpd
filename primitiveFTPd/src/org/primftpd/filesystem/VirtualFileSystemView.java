package org.primftpd.filesystem;

import org.primftpd.services.PftpdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class VirtualFileSystemView<
        MinaType,
        FsType extends FsFile<MinaType>,
        RootType extends RootFile<MinaType>,
        SafType extends SafFile<MinaType>,
        RoSafType extends RoSafFile<MinaType>
        > {

    public static final String PREFIX_FS = "fs";
    public static final String PREFIX_ROOT = "superuser";
    public static final String PREFIX_SAF = "saf";
    public static final String PREFIX_ROSAF = "rosaf";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final FsFileSystemView<FsType, MinaType> fsFileSystemView;
    protected final RootFileSystemView<RootType, MinaType> rootFileSystemView;
    protected final SafFileSystemView<SafType, MinaType> safFileSystemView;
    protected final RoSafFileSystemView<RoSafType, MinaType> roSafFileSystemView;
    protected final PftpdService pftpdService;

    public VirtualFileSystemView(
            FsFileSystemView<FsType, MinaType> fsFileSystemView,
            RootFileSystemView<RootType, MinaType> rootFileSystemView,
            SafFileSystemView<SafType, MinaType> safFileSystemView,
            RoSafFileSystemView<RoSafType, MinaType> roSafFileSystemView,
            PftpdService pftpdService) {
        this.fsFileSystemView = fsFileSystemView;
        this.rootFileSystemView = rootFileSystemView;
        this.safFileSystemView = safFileSystemView;
        this.roSafFileSystemView = roSafFileSystemView;
        this.pftpdService = pftpdService;
    }

    public abstract MinaType createFile(String absPath, AbstractFile delegate, PftpdService pftpdService);
    public abstract MinaType createFile(String absPath, AbstractFile delegate, boolean exists, PftpdService pftpdService);

    protected abstract String absolute(String file);

    public MinaType getFile(String file) {
        String absoluteVirtualPath = absolute(file);
        logger.debug("getFile '{}', absolute: '{}'", file, absoluteVirtualPath);
        if ("/".equals(absoluteVirtualPath)) {
            return createFile(absoluteVirtualPath, null, true, pftpdService);
        } else if (absoluteVirtualPath.startsWith("/" + PREFIX_FS)) {
            String realPath = toRealPath(absoluteVirtualPath, "/" + PREFIX_FS);
            realPath = pftpdService.getPrefsBean().getStartDir().getAbsolutePath() + "/" + realPath;
            logger.debug("Using FS '{}' for '{}'", realPath, absoluteVirtualPath);
            AbstractFile delegate = fsFileSystemView.getFile(realPath);
            return createFile(absoluteVirtualPath, delegate, pftpdService);
        } else if (absoluteVirtualPath.startsWith("/" + PREFIX_ROOT)) {
            String realPath = toRealPath(absoluteVirtualPath, "/" + PREFIX_ROOT);
            logger.debug("Using ROOT '{}' for '{}'", realPath, absoluteVirtualPath);
            AbstractFile delegate = rootFileSystemView.getFile(realPath);
            return createFile(absoluteVirtualPath, delegate, pftpdService);
        } else if (absoluteVirtualPath.startsWith("/" + PREFIX_SAF)) {
            String realPath = toRealPath(absoluteVirtualPath, "/" + PREFIX_SAF);
            logger.debug("Using SAF '{}' for '{}'", realPath, absoluteVirtualPath);
            AbstractFile delegate = safFileSystemView.getFile(realPath);
            return createFile(absoluteVirtualPath, delegate, pftpdService);
        } else if (absoluteVirtualPath.startsWith("/" + PREFIX_ROSAF)) {
            String realPath = toRealPath(absoluteVirtualPath, "/" + PREFIX_ROSAF);
            logger.debug("Using ROSAF '{}' for '{}'", realPath, absoluteVirtualPath);
            AbstractFile delegate = roSafFileSystemView.getFile(realPath);
            return createFile(absoluteVirtualPath, delegate, pftpdService);
        } else {
            logger.debug("Using VirtualFile for unknown path '{}'", absoluteVirtualPath);
            return createFile(absoluteVirtualPath, null, false, pftpdService);
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
