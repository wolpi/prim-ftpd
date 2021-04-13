package org.primftpd.prefs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.common.util.ThreadUtils;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.apache.sshd.server.Command;
import org.primftpd.prefs.LoadPrefsUtil;

import java.util.concurrent.ExecutorService;
import java.io.IOException;

public class UserSftpSubsystem extends SftpSubsystem {

    private static SharedPreferences prefs;

    public UserSftpSubsystem(Context context) {
        this(context, null);
    }

    public UserSftpSubsystem(Context context, ExecutorService executorService) {
        this(context, executorService, false);
    }

    public UserSftpSubsystem(Context context, ExecutorService executorService, boolean shutdownOnExit) {
        super(executorService, false);
        prefs = LoadPrefsUtil.getPrefs(context);
    }

    @Override
    protected void process(Buffer buffer) throws IOException {
        int type = buffer.getByte();
        int id = buffer.getInt();

        switch (type) {

            // Downloading/Readin Files
            case SSH_FXP_READ: {
                if (!(LoadPrefsUtil.allowDownload(prefs))) {
                    sendStatus(id, SSH_FX_OP_UNSUPPORTED, "File downloading is not allowed");
                    return;
                }
                break;
            }

            // Renaming
            case SSH_FXP_RENAME: {
                if (!LoadPrefsUtil.allowRename(prefs)) {
                    sendStatus(id, SSH_FX_OP_UNSUPPORTED, "File renaming is not allowed");
                    return;
                }
                break;
            }

            // Deletion
            case SSH_FXP_REMOVE: {
                if (!LoadPrefsUtil.allowDelete(prefs)) {
                    sendStatus(id, SSH_FX_OP_UNSUPPORTED, "File deletion is not allowed");
                    return;
                }
                break;
            }
            case SSH_FXP_RMDIR: {
                if (!LoadPrefsUtil.allowDelete(prefs)) {
                    sendStatus(id, SSH_FX_OP_UNSUPPORTED, "Directory deletion is not allowed");
                    return;
                }
                break;
            }

            // Misc
            case SSH_FXP_WRITE: {
                if (!LoadPrefsUtil.allowUpload(prefs)) {
                    sendStatus(id, SSH_FX_OP_UNSUPPORTED, "Uploading is not allowed");
                    return;
                }
                break;
            }
            case SSH_FXP_MKDIR: {
                if (!LoadPrefsUtil.allowUpload(prefs)) {
                    sendStatus(id, SSH_FX_OP_UNSUPPORTED, "Creating new directories is not allowed");
                    return;
                }
                break;
            }
            default: {
                break;
            }
        }
        super.process(buffer);
    }
}