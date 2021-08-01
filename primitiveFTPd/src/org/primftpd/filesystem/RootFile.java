package org.primftpd.filesystem;

import android.os.Build;

import org.apache.ftpserver.util.IoUtils;
import org.primftpd.events.ClientActionEvent;
import org.primftpd.pojo.LsOutputBean;
import org.primftpd.pojo.LsOutputParser;
import org.primftpd.services.PftpdService;
import org.primftpd.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public abstract class RootFile<T> extends AbstractFile {

    private static final int BUF_SIZE_DD_ERR_STREAM = 4096;

    private final Shell.Interactive shell;

    protected final LsOutputBean bean;

    private Process ddProcess;

    public RootFile(Shell.Interactive shell, LsOutputBean bean, String absPath, PftpdService pftpdService) {
        super(
                absPath,
                bean.getName(),
                bean.getDate() != null ? bean.getDate().getTime() : 0,
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
        return runCommand("touch -t " + dateStr + " \"" + absPath + "\"");
    }

    public boolean mkdir() {
        logger.trace("[{}] mkdir()", name);
        postClientAction(ClientActionEvent.ClientAction.CREATE_DIR);
        return runCommand("mkdir \"" + absPath + "\"");
    }

    public boolean delete() {
        logger.trace("[{}] delete()", name);
        postClientAction(ClientActionEvent.ClientAction.DELETE);
        return runCommand("rm -rf \"" + absPath + "\"");
    }

    public boolean move(RootFile<T> destination) {
        logger.trace("[{}] move({})", name, destination.getAbsolutePath());
        postClientAction(ClientActionEvent.ClientAction.RENAME);
        return runCommand("mv \"" + absPath + "\" \"" + destination.getAbsolutePath() + "\"");
    }

    public List<T> listFiles() {
        logger.trace("[{}] listFiles()", name);
        postClientAction(ClientActionEvent.ClientAction.LIST_DIR);

        List<T> result = new ArrayList<>();
        final LsOutputParser parser = new LsOutputParser();
        final List<LsOutputBean> beans = new ArrayList<>();
        shell.addCommand("ls -la " + absPath, 0, new Shell.OnCommandLineListener() {
            @Override
            public void onLine(String s) {
                LsOutputBean bean = parser.parseLine(s);
                if (bean != null) {
                    beans.add(bean);
                }
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

    private String escapePathForDD(String path) {
        return path.replaceAll(" ", "\\ ");
    }

    public OutputStream createOutputStream(long offset) throws IOException {
        logger.trace("[{}] createOutputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.UPLOAD);

        if (!bean.isExists()) {
            // if file does not exist, explicitly create it as root, see GH issue #117
            runCommand("touch" + " \"" + absPath + "\"");
        }

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("su", "-c", "dd", "of=" + escapePathForDD(absPath));
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

    public InputStream createInputStream(long offset) throws IOException {
        logger.trace("[{}] createInputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.DOWNLOAD);

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("su", "-c", "dd", "if=" + escapePathForDD(absPath));
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
            Thread.sleep(250);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }

        BufferedInputStream bis = new BufferedInputStream(ddProcess.getInputStream());
        bis.skip(offset);
        return bis;
    }

    @Override
    public void handleClose() throws IOException {
        super.handleClose();
        if (ddProcess != null) {
            logDdErrorStream(ddProcess);
            ddProcess = null;
        } else {
            logger.trace("no dd process");
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

    protected boolean runCommand(String cmd) {
        logger.trace("running cmd: '{}'", cmd);
        final Boolean[] wrapper = new Boolean[1];
        shell.addCommand(cmd, 0, new Shell.OnCommandLineListener() {
            @Override
            public void onLine(String s) {
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
            public void onLine(String s) {
                if (s != null) {
                    sb.append(s);
                }
            }
        });
        shell.waitForIdle();
        String result = sb.toString();
        logger.trace("read output of cmd '{}': '{}'", cmd, result);
        return result;
    }
}
