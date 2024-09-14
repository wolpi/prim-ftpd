package org.primftpd.filesystem;

import android.os.Build;

import org.apache.ftpserver.util.IoUtils;
import org.primftpd.events.ClientActionEvent;
import org.primftpd.pojo.LsOutputBean;
import org.primftpd.pojo.LsOutputParser;
import org.primftpd.services.PftpdService;
import org.primftpd.util.Defaults;
import org.primftpd.util.StringUtils;
import org.primftpd.util.TmpDirType;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import eu.chainfire.libsuperuser.Shell;

public abstract class RootFile<T> extends AbstractFile {

    private static final int BUF_SIZE_DD_ERR_STREAM = 4096;

    private final Shell.Interactive shell;

    protected final LsOutputBean bean;

    private Process ddProcess;
    private File tmpDir;
    private boolean moveFileOnClose;

    public RootFile(Shell.Interactive shell, LsOutputBean bean, String absPath, PftpdService pftpdService) {
        super(
                absPath,
                bean.getName(),
                bean.getTimestamp(),
                bean.getSize(),
                true,
                bean.isExists(),
                bean.isDir(),
                pftpdService);
        this.shell = shell;
        this.bean = bean;
    }

    protected abstract T createFile(Shell.Interactive shell, LsOutputBean bean, String absPath, PftpdService pftpdService);

    @Override
    public ClientActionEvent.Storage getClientActionStorage() {
        return ClientActionEvent.Storage.ROOT;
    }

    public boolean isFile() {
        logger.trace("[{}] isFile() -> {}", name, bean.isFile());
        return bean.isFile();
    }

    public boolean isWritable() {
        logger.trace("[{}] isWritable()", name);
        return true;
    }

    public boolean isRemovable() {
        logger.trace("[{}] isRemovable()", name);
        return true;
    }

    public boolean setLastModified(long time) {
        logger.trace("[{}] setLastModified({})", name, time);

        String dateStr = Utils.touchDate(time);
        return runCommand("touch -m -t " + dateStr + " " + escapePath(absPath));
    }

    private static final SimpleDateFormat DEBUG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    static {
        DEBUG_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public long getLastModified() {
        logger.trace("[{}] RootFile getLastModified()", name);
        logger.trace("  returning date '{}', original ls-output line: {}",
                DEBUG_DATE_FORMAT.format(bean.getTimestamp()),
                bean.getOriginalLine());
        return super.getLastModified();
    }

    public boolean mkdir() {
        logger.trace("[{}] mkdir()", name);
        postClientAction(ClientActionEvent.ClientAction.CREATE_DIR);
        return runCommand("mkdir " + escapePath(absPath));
    }

    public boolean delete() {
        logger.trace("[{}] delete()", name);
        postClientAction(ClientActionEvent.ClientAction.DELETE);
        return runCommand("rm -rf " + escapePath(absPath));
    }

    public boolean move(RootFile<T> destination) {
        logger.trace("[{}] move({})", name, destination.getAbsolutePath());
        postClientAction(ClientActionEvent.ClientAction.RENAME);
        boolean success = runCommand("mv " + escapePath(absPath) + " " + escapePath(destination.getAbsolutePath()));
        if (success) {
            Utils.mediaScanFile(pftpdService.getContext(), getAbsolutePath());
        }
        return success;
    }

    public List<T> listFiles() {
        logger.trace("[{}] listFiles()", name);
        postClientAction(ClientActionEvent.ClientAction.LIST_DIR);

        List<T> result = new ArrayList<>();
        final LsOutputParser parser = new LsOutputParser();
        final List<LsOutputBean> beans = new ArrayList<>();
        shell.addCommand("ls -la " + escapePath(absPath), 0, new Shell.OnCommandLineListener() {
            @Override
            public void onSTDOUT(String s) {
                LsOutputBean bean = parser.parseLine(s);
                if (bean != null) {
                    beans.add(bean);
                }
            }
            @Override
            public void onSTDERR(String s) {
            }
            @Override
            public void onCommandResult(int i, int i1) {
            }
        });
        shell.waitForIdle();

        for (LsOutputBean bean : beans) {
            String path = absPath + "/" + bean.getName();
            result.add(createFile(shell, bean, path, pftpdService));
        }

        return result;
    }

    public OutputStream createOutputStream(long offset) throws IOException {
        logger.trace("[{}] createOutputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.UPLOAD);

        if (!bean.isExists()) {
            // if file does not exist, explicitly create it as root, see GH issue #117
            runCommand("touch" + " " + escapePath(absPath));
        }

        OutputStream os;
        if (pftpdService.getPrefsBean().isRootCopyFiles()) {
            os = createOutputStreamCopy(offset);
        } else {
            os = createOutputStreamDd(offset);
        }
        return new BufferedOutputStream(os) {
            @Override
            public void close() throws IOException {
                super.close();
                Utils.mediaScanFile(pftpdService.getContext(), getAbsolutePath());
            }
        };
    }

    public InputStream createInputStream(long offset) throws IOException {
        logger.trace("[{}] createInputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.DOWNLOAD);

        if (pftpdService.getPrefsBean().isRootCopyFiles()) {
            return createInputStreamCopy(offset);
        } else {
            return createInputStreamDd(offset);
        }
    }

    @Override
    public void handleClose() throws IOException {
        logger.trace("[{}] handleClose()", name);
        if (pftpdService.getPrefsBean().isRootCopyFiles()) {
            handleCloseCopy();
        } else {
            handleCloseDd();
        }
    }

    private OutputStream createOutputStreamDd(long offset) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("su", "-c", "dd", "of=" + escapePath(absPath));

        String ddCommand;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ddCommand = String.join(" ", processBuilder.command());
        } else {
            ddCommand = StringUtils.join(processBuilder.command(), ' ');
        }
        logger.trace("dd command: {}", ddCommand);
        ddProcess = processBuilder.start();

        return new TracingBufferedOutputStream(ddProcess.getOutputStream(), logger);
    }

    public InputStream createInputStreamDd(long offset) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("su", "-c", "dd", "if=" + escapePath(absPath));

        String ddCommand;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ddCommand = String.join(" ", processBuilder.command());
        } else {
            ddCommand = StringUtils.join(processBuilder.command(), ' ');
        }
        logger.trace("dd command: {}", ddCommand);
        ddProcess = processBuilder.start();

        try {
            // workaround for weird errors
            long sleep = 250;
            if (size < 100) {
                sleep = 1000;
            }
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }

        BufferedInputStream bis = new BufferedInputStream(ddProcess.getInputStream());
        bis.skip(offset);
        return bis;
    }

    public void handleCloseDd() throws IOException {
        try {
            // workaround for weird errors
            long sleep = 250;
            if (size < 100) {
                sleep = 1000;
            }
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        super.handleClose();
        if (ddProcess != null) {
            logDdErrorStream(ddProcess);
            ddProcess = null;
        } else {
            logger.trace("no dd process");
        }
    }

    private OutputStream createOutputStreamCopy(long offset) throws IOException {
        tmpDir = Defaults.buildTmpDir(this.pftpdService.getContext(), TmpDirType.ROOT_COPY);
        moveFileOnClose = true;
        String name = getName();
        if (name.contains("/")) {
            name = name.substring(name.lastIndexOf("/") + 1);
        }
        File tmpFile = new File(tmpDir, name);
        logger.trace("  using output stream tmp: {}", tmpFile.getAbsolutePath());
        return new FileOutputStream(tmpFile) {
            @Override
            public void close() throws IOException {
                super.close();
                logger.trace("tmp out file stream close()");
                handleCloseCopy();
            }
        };
    }

    public InputStream createInputStreamCopy(long offset) throws IOException {
        if (offset == 0) {
            tmpDir = Defaults.buildTmpDir(this.pftpdService.getContext(), TmpDirType.ROOT_COPY);
            runCommand("cp" + " " + escapePath(absPath) + " " + escapePath(tmpDir.getAbsolutePath()));
        }
        File tmpFile = tmpDir.listFiles()[0];
        FileInputStream fis = new FileInputStream(tmpFile);
        fis.skip(offset);
        return fis;
    }

    public void handleCloseCopy() throws IOException {
        if (moveFileOnClose) {
            runCommand("mv" + " " +
                    escapePath(new File(tmpDir, getName()).getAbsolutePath()) + " " + escapePath(absPath));
            moveFileOnClose = false;
        }
        if (tmpDir != null) {
            runCommand("rm -rf " + " " + escapePath(tmpDir.getAbsolutePath()));
            tmpDir = null;
        }
    }

    private void logDdErrorStream(Process proc) throws IOException {
        try {
            proc.waitFor();
            int exitCode = proc.exitValue();
            if (exitCode != 0) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IoUtils.copy(proc.getErrorStream(), baos, BUF_SIZE_DD_ERR_STREAM);
                String ddErr = baos.toString();
                logger.error("dd exit code: '{}', error stream: '{}'", exitCode, ddErr);
                logger.error("{}", ddErr);
            } else {
                logger.trace("dd exited with 0");
            }
        } catch (InterruptedException e) {
            logger.error("interrupted while waiting for dd process to exit", e);
        }
    }

    protected static String escapePath(String path) {
        if (path != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("'");
            for (int i=0; i<path.length(); i++) {
                char c = path.charAt(i);
                if (c == '\'') {
                    sb.append("'\\''"); // bash escaping is awesome
                } else {
                    sb.append(c);
                }
            }
            sb.append("'");
            path = sb.toString();
        }
        return path;
    }

    protected boolean runCommand(String cmd) {
        logger.trace("running cmd: '{}'", cmd);
        final Boolean[] wrapper = new Boolean[1];
        shell.addCommand(cmd, 0, new Shell.OnCommandLineListener() {
            @Override
            public void onSTDOUT(String s) {
            }
            @Override
            public void onSTDERR(String s) {
            }
            @Override
            public void onCommandResult(int i, int i1) {
                wrapper[0] = i == 0;
            }
        });
        shell.waitForIdle();
        return wrapper[0];
    }

    protected String readCommandOutput(String cmd) {
        final StringBuilder sb = new StringBuilder();
        shell.addCommand(cmd, 0, new Shell.OnCommandLineListener() {
            @Override
            public void onCommandResult(int i, int i1) {
            }
            @Override
            public void onSTDOUT(String s) {
                if (s != null) {
                    sb.append(s);
                }
            }
            @Override
            public void onSTDERR(String s) {
            }
        });
        shell.waitForIdle();
        String result = sb.toString();
        logger.trace("read output of cmd '{}': '{}'", cmd, result);
        return result;
    }
}
