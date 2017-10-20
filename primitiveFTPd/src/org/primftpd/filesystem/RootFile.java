package org.primftpd.filesystem;

import org.primftpd.pojo.LsOutputBean;
import org.primftpd.pojo.LsOutputParser;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public abstract class RootFile<T> extends AbstractFile {

    private final Shell.Interactive shell;

    protected final LsOutputBean bean;

    public RootFile(Shell.Interactive shell, LsOutputBean bean, String absPath) {
        super(
                absPath,
                bean.getName(),
                bean.getDate().getTime(),
                bean.getSize(),
                true,
                bean.isExists(),
                bean.isDir());
        this.shell = shell;
        this.bean = bean;
    }

    protected abstract T createFile(Shell.Interactive shell, LsOutputBean bean, String absPath);

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
        // TODO root setLastModified()
        return false;
    }

    public boolean mkdir() {
        logger.trace("[{}] mkdir()", name);
        return runCommand("mkdir " + absPath);
    }

    public boolean delete() {
        logger.trace("[{}] delete()", name);
        return runCommand("rm -rf " + absPath);
    }

    public boolean move(RootFile<T> destination) {
        logger.trace("[{}] move({})", name, destination.getAbsolutePath());
        return runCommand("mv " + absPath + " " + destination.getAbsolutePath());
    }

    public List<T> listFiles() {
        logger.trace("[{}] listFiles()", name);

        List<T> result = new ArrayList<>();
        final LsOutputParser parser = new LsOutputParser();
        final List<LsOutputBean> beans = new ArrayList<>();
        shell.addCommand("ls -lA " + absPath, 0, new Shell.OnCommandLineListener() {
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
            result.add(createFile(shell, bean, path));
        }

        return result;
    }

    public OutputStream createOutputStream(long offset) throws IOException {
        logger.trace("[{}] createOutputStream(offset: {})", name, offset);

        // new file or existing file?
        final String pathToUpdatePerm;
        if (bean.isExists()) {
            pathToUpdatePerm = absPath;
        } else {
            pathToUpdatePerm = absPath.substring(0, absPath.lastIndexOf('/'));
        }

        // remember current permission
        final String perm = readCommandOutput("stat -c %a " + pathToUpdatePerm);

        // set perm to be able to read file
        runCommand("chmod 0777 " + pathToUpdatePerm);

        return new FileOutputStream(absPath) {
            @Override
            public void close() throws IOException {
                super.close();
                // restore permission
                runCommand("chmod 0" + perm + " " + pathToUpdatePerm);
            }
        };
    }

    public InputStream createInputStream(long offset) throws IOException {
        logger.trace("[{}] createInputStream(offset: {})", name, offset);

        // remember current permission
        final String perm = readCommandOutput("stat -c %a " + absPath);

        // set perm to be able to read file
        runCommand("chmod 0777 " + absPath);

        return new FileInputStream(absPath) {
            @Override
            public void close() throws IOException {
                super.close();
                // restore permission
                runCommand("chmod 0" + perm + " " + absPath);
            }
        };
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
