package org.apache.ftpserver.command.impl;

import org.apache.ftpserver.command.AbstractCommand;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.impl.LocalizedFtpReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class QUOT extends AbstractCommand {

    private final Logger LOG = LoggerFactory.getLogger(QUOT.class);

    public void execute(final FtpIoSession session,
                        final FtpServerContext context, final FtpRequest request)
            throws IOException {
        // see GH issue #207
        LOG.debug("QUOT received, ignoring");
    }
}
