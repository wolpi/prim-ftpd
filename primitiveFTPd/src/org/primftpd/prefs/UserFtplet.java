package org.primftpd.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.DefaultFtpReply;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.primftpd.prefs.LoadPrefsUtil;


import java.io.IOException;

public class UserFtplet extends DefaultFtplet {

    private static SharedPreferences prefs;
    
    public UserFtplet(Context context){
        prefs = LoadPrefsUtil.getPrefs(context);
    }

    //Downloading
    @Override
    public FtpletResult onDownloadStart(FtpSession session, FtpRequest request)
    throws FtpException, IOException {
        if (LoadPrefsUtil.allowDownload(prefs)) {
            return super.onDownloadStart(session, request); 
        }
        session.write(new DefaultFtpReply(FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, "File downloading is not allowed"));
        return FtpletResult.SKIP;
    }
    
    // Renaming
    @Override
    public FtpletResult onRenameStart(FtpSession session, FtpRequest request)
    throws FtpException, IOException {
        if (LoadPrefsUtil.allowRename(prefs)) {
            return super.onRenameStart(session, request); 
        }
        session.write(new DefaultFtpReply(FtpReply.REPLY_553_REQUESTED_ACTION_NOT_TAKEN_FILE_NAME_NOT_ALLOWED, "File renaming is not allowed"));
        return FtpletResult.SKIP;
    }
    
    // Deleting
    @Override
    public FtpletResult onDeleteStart(FtpSession session, FtpRequest request)
    throws FtpException, IOException {
        if (LoadPrefsUtil.allowDelete(prefs)) {
            return super.onDeleteStart(session, request); 
        }
        session.write(new DefaultFtpReply(FtpReply.REPLY_450_REQUESTED_FILE_ACTION_NOT_TAKEN, "File deletion is not allowed"));
        return FtpletResult.SKIP;
    }

    @Override
    public FtpletResult onRmdirStart(FtpSession session, FtpRequest request)
    throws FtpException, IOException {
        if (LoadPrefsUtil.allowDelete(prefs)) {
            return super.onRmdirStart(session, request); 
        }
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Directory deletion not allowed"));
        return FtpletResult.SKIP;
    }
    
    // Uploading/Editing/Creating
    @Override
    public FtpletResult onUploadStart(FtpSession session, FtpRequest request)
    throws FtpException, IOException {
        if (LoadPrefsUtil.allowUpload(prefs)) {
            return super.onUploadStart(session, request); 
        }
        session.write(new DefaultFtpReply(FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, "Uploading is not allowed"));
        return FtpletResult.SKIP;
    }
    
    @Override
    public FtpletResult onUploadUniqueStart(FtpSession session, FtpRequest request)
    throws FtpException, IOException {
        if (LoadPrefsUtil.allowUpload(prefs)) {
            return super.onUploadUniqueStart(session, request);
        }
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Uploading unique files is not allowed"));
        return FtpletResult.SKIP;
    }

    @Override
    public FtpletResult onAppendStart(FtpSession session, FtpRequest request)
    throws FtpException, IOException {
        if (LoadPrefsUtil.allowUpload(prefs)) {
            return super.onAppendStart(session, request);
        }
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Appending to files is not allowed"));
        return FtpletResult.SKIP;
    }

    @Override
    public FtpletResult onMkdirStart(FtpSession session, FtpRequest request)
    throws FtpException, IOException {
        if (LoadPrefsUtil.allowUpload(prefs)) {
            return super.onMkdirStart(session, request);
        }
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Creating new directories is not allowed"));
        return FtpletResult.SKIP;
    }
}