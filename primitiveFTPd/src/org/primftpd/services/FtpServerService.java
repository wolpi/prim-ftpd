package org.primftpd.services;

import android.net.Uri;
import android.os.Looper;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ipfilter.SessionFilter;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.mina.core.session.IoSession;
import org.primftpd.events.ClientActionEvent;
import org.primftpd.filesystem.FsFtpFileSystemView;
import org.primftpd.filesystem.QuickShareFtpFileSystemView;
import org.primftpd.filesystem.RoSafFtpFileSystemView;
import org.primftpd.filesystem.RootFtpFileSystemView;
import org.primftpd.filesystem.SafFtpFileSystemView;
import org.primftpd.util.RemoteIpChecker;
import org.primftpd.util.StringUtils;

import java.io.File;
import java.net.SocketAddress;

import eu.chainfire.libsuperuser.Shell;

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
		return new ServerServiceHandler(serviceLooper, service, getServiceName());
	}

	@Override
	protected Object getServer()
	{
		return ftpServer;
	}

	@Override
	protected int getPort()
	{
		return prefsBean.getPort();
	}

	@Override
	protected String getServiceName()
	{
		return "ftp";
	}

	@Override
	protected ClientActionEvent.Protocol getProtocol() {
		return ClientActionEvent.Protocol.FTP;
	}

	@Override
	protected void stopServer()
	{
		ftpServer.stop();
		ftpServer = null;
	}

	@Override
	protected boolean launchServer(final Shell.Interactive shell) {
		ListenerFactory listenerFactory = new ListenerFactory();
		listenerFactory.setPort(prefsBean.getPort());

		DataConnectionConfigurationFactory dataConConfigFactory = new DataConnectionConfigurationFactory();
		String passivePorts = prefsBean.getFtpPassivePorts();
		if (StringUtils.isNotBlank(passivePorts)){
			dataConConfigFactory.setPassivePorts(passivePorts);
		}
		if (prefsBean.getIdleTimeout() != null) {
			listenerFactory.setIdleTimeout(prefsBean.getIdleTimeout());
			dataConConfigFactory.setIdleTime(prefsBean.getIdleTimeout());
		}

		listenerFactory.setSessionFilter(new SessionFilter() {
			@Override
			public boolean accept(IoSession session) {
				SocketAddress remoteAddress = session.getRemoteAddress();
				return RemoteIpChecker.ipAllowed(remoteAddress, prefsBean, logger);
			}
		});
		listenerFactory.setDataConnectionConfiguration(dataConConfigFactory.createDataConnectionConfiguration());

		FtpServerFactory serverFactory = new FtpServerFactory();
		serverFactory.addListener("default", listenerFactory.createListener());

		// user manager & file system
		serverFactory.setUserManager(new AndroidPrefsUserManager(prefsBean));
		serverFactory.setFileSystem(new FileSystemFactory() {
			@Override
			public FileSystemView createFileSystemView(User user) {
				if (quickShareBean != null) {
					logger.debug("launching server in quick share mode");
					return new QuickShareFtpFileSystemView(
							new File(quickShareBean.getPathToFile()),
							user,
							FtpServerService.this);
				} else {
					switch (prefsBean.getStorageType()) {
						case PLAIN:
							return new FsFtpFileSystemView(FtpServerService.this, prefsBean.getStartDir(), user);
						case ROOT:
							return new RootFtpFileSystemView(shell, FtpServerService.this, prefsBean.getStartDir(), user);
						case SAF:
							return new SafFtpFileSystemView(
									getApplicationContext(),
									Uri.parse(prefsBean.getSafUrl()),
									getContentResolver(),
									FtpServerService.this,
									user);
						case RO_SAF:
							return new RoSafFtpFileSystemView(
									Uri.parse(prefsBean.getSafUrl()),
									getContentResolver(),
									FtpServerService.this,
									user);
					}
				}
				return null;
			}
		});

		// connection settings with some security improvements
		ConnectionConfigFactory conCfg = new ConnectionConfigFactory();
		conCfg.setAnonymousLoginEnabled(prefsBean.isAnonymousLogin());
		conCfg.setMaxLoginFailures(5);
		conCfg.setLoginFailureDelay(2000);
		serverFactory.setConnectionConfig(conCfg.createConnectionConfig());

		// do start server
		ftpServer = serverFactory.createServer();
		try {
			ftpServer.start();
			return true;
		} catch (Exception e) {
			// note: createServer() throws RuntimeExceptions, too
			ftpServer = null;
			handleServerStartError(e);
			return false;
		}
	}
}
