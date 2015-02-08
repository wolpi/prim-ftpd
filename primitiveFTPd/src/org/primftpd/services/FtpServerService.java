package org.primftpd.services;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.listener.ListenerFactory;
import org.primftpd.AndroidPrefsUserManager;
import org.primftpd.filesystem.FtpFileSystemView;

import android.os.Looper;

/**
 * Implements a FTP server.
 */
public class FtpServerService extends AbstractServerService
{
	private FtpServer ftpServer;

	@Override
	protected ServerServiceHandler createServiceHandler(
		Looper serviceLooper,
		AbstractServerService service)
	{
		return new ServerServiceHandler(serviceLooper, service, "ftp");
	}

	@Override
	protected Object getServer()
	{
		return ftpServer;
	}

	@Override
	protected void stopServer()
	{
		ftpServer.stop();
		ftpServer = null;
	}

    @Override
	protected void launchServer() {
    	ListenerFactory listenerFactory = new ListenerFactory();
    	listenerFactory.setPort(prefsBean.getPort());

    	FtpServerFactory serverFactory = new FtpServerFactory();
    	serverFactory.addListener("default", listenerFactory.createListener());

    	// user manager & file system
    	serverFactory.setUserManager(new AndroidPrefsUserManager(prefsBean));
    	serverFactory.setFileSystem(new FileSystemFactory() {
			@Override
			public FileSystemView createFileSystemView(User user) throws FtpException
			{
				return new FtpFileSystemView(user);
			}
    	});

    	// XXX SSL
    	// ssl listener
//    	KeyStore keyStore = KeyStoreUtil.loadKeyStore(getResources());
//    	if (keyStore != null) {
//	    	SslConfiguration sslConfig = KeyStoreUtil.createSslConfiguration(keyStore);
//	    	if (sslConfig != null) {
//		    	ListenerFactory sslListenerFactory = new ListenerFactory();
//		    	sslListenerFactory.setPort(prefsBean.getSslPort());
//		    	sslListenerFactory.setImplicitSsl(true);
//		    	sslListenerFactory.setSslConfiguration(sslConfig);
//		    	serverFactory.addListener("ssl", sslListenerFactory.createListener());
//	    	}
//    	}

    	// do start server
    	ftpServer = serverFactory.createServer();
    	try {
    		ftpServer.start();
    	} catch (Exception e) {
    		// note: createServer() throws RuntimeExceptions, too
    		ftpServer = null;
			handleServerStartError(e);
		}
    }
}
