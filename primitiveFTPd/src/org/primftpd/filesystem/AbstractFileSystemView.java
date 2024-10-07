package org.primftpd.filesystem;

import org.primftpd.services.PftpdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFileSystemView {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final PftpdService pftpdService;

    public AbstractFileSystemView(PftpdService pftpdService) {
        this.pftpdService = pftpdService;
    }

    public final PftpdService getPftpdService() {
        return pftpdService;
    }
}
