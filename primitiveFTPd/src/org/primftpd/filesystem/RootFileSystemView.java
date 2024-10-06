package org.primftpd.filesystem;

import org.primftpd.pojo.LsOutputBean;
import org.primftpd.pojo.LsOutputParser;
import org.primftpd.services.PftpdService;

import java.util.List;

import androidx.annotation.NonNull;
import eu.chainfire.libsuperuser.Shell;

public abstract class RootFileSystemView<TFile extends RootFile<TMina, ? extends RootFileSystemView>, TMina> extends AbstractFileSystemView {

    private final MediaScannerClient mediaScannerClient;
    protected final Shell.Interactive shell;

    public RootFileSystemView(PftpdService pftpdService, Shell.Interactive shell) {
        super(pftpdService);
        this.mediaScannerClient = new MediaScannerClient(pftpdService.getContext());
        this.shell = shell;
    }

    public final MediaScannerClient getMediaScannerClient() {
        return mediaScannerClient;
    }

    public final Shell.Interactive getShell() {
        return shell;
    }

    protected abstract TFile createFile(String absPath, LsOutputBean bean);

    protected abstract String absolute(String file);

    public TFile getFile(String file) {
        logger.trace("getFile({})", file);

        String abs = absolute(file);
        logger.trace("  getFile(abs: {})", abs);

        final LsOutputParser parser = new LsOutputParser();
        final LsOutputBean[] wrapper = new LsOutputBean[1];
        final String cmd = "ls -lad " + RootFile.escapePath(abs);
        logger.trace("  running command: {}", cmd);
        shell.addCommand(cmd, 0, new Shell.OnCommandResultListener() {
            @Override
            public void onCommandResult(int commandCode, int exitCode, @NonNull List<String> output) {
                if (exitCode == 0) {
                    wrapper[0] = parser.parseLine(output.get(0));
                } else {
                    logger.error("could not run 'ls' command (exitCode: {})", exitCode);
                    for (String line : output) {
                        logger.error("{}", line);
                    }
                }
            }
        });
        shell.waitForIdle();
        LsOutputBean bean = wrapper[0];
        if (bean != null) {
            // don't deal with links on your own -> just causes errors, let the OS deal with it
            //if (bean.isLink()) {
            //    bean = findFinalLinkTarget(bean, parser);
            //    // TODO make sym link target absolute
            //    abs = bean.getName();
            //}
            return createFile(abs, bean);
        } else {
            // probably new
            String name;
            if (abs.contains("/")) {
                name = abs.substring(abs.lastIndexOf('/') + 1);
            } else {
                name = abs;
            }
            bean = new LsOutputBean(name);
            return createFile(abs, bean);
        }
    }

    protected LsOutputBean findFinalLinkTarget(LsOutputBean bean, final LsOutputParser parser ) {
        LsOutputBean tmp = bean;
        final LsOutputBean[] wrapper = new LsOutputBean[1];
        int i=0;
        while (tmp.isLink()) {
            shell.addCommand("ls -lad \"" + tmp.getLinkTarget() + "\"", 0, new Shell.OnCommandLineListener() {
                @Override
                public void onSTDOUT(@NonNull String s) {
                    wrapper[0] = parser.parseLine(s);
                }
                @Override
                public void onSTDERR(@NonNull String s) {
                    logger.error("stderr: {}", s);
                }
                @Override
                public void onCommandResult(int i, int i1) {
                }
            });
            shell.waitForIdle();

            tmp = wrapper[0];
            i++;
            if (i > 20) {
                break;
            }
        }
        return tmp;
    }

}
