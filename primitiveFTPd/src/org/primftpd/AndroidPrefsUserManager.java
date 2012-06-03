package org.primftpd;

import java.util.ArrayList;
import java.util.List;

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.primftpd.util.EncryptionUtil;
import org.primftpd.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Environment;

public class AndroidPrefsUserManager implements UserManager {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private final PrefsBean prefsBean;

	public AndroidPrefsUserManager(PrefsBean prefsBean) {
		this.prefsBean = prefsBean;
	}

	protected List<Authority> buildAuthorities() {
		List<Authority> authorities = new ArrayList<Authority>();

		WritePermission writePerm = new WritePermission();
		authorities.add(writePerm);

		//TransferRatePermission ratePerm = new TransferRatePermission(100, 100);
		TransferRatePermission ratePerm = new TransferRatePermission(0, 0);
		authorities.add(ratePerm);

		ConcurrentLoginPermission concurLoginPerm = new ConcurrentLoginPermission(10, 10);
		authorities.add(concurLoginPerm);

		return authorities;
	}

	protected User buildUser() {
		BaseUser user = new BaseUser();
		user.setEnabled(true);

		String rootDir = Environment.getExternalStorageDirectory().getAbsolutePath();
		logger.debug("rootDir: {}", rootDir);
		user.setHomeDirectory(rootDir);

		user.setMaxIdleTime(60);
		user.setName(prefsBean.getUserName());
		user.setPassword(prefsBean.getPassword());
		user.setAuthorities(buildAuthorities());
		return user;
	}

	@Override
	public User getUserByName(String username) throws FtpException {
		if (doesExist(username)) {
			return buildUser();
		}
		return null;
	}

	@Override
	public String[] getAllUserNames() throws FtpException {
		return new String[]{prefsBean.getUserName()};
	}

	@Override
	public void delete(String username) throws FtpException {
	}

	@Override
	public void save(User user) throws FtpException {
	}

	@Override
	public boolean doesExist(String username) {
		return prefsBean.getUserName().equals(username);
	}

	@Override
	public User authenticate(Authentication authentication)
			throws AuthenticationFailedException
	{
		if (authentication instanceof UsernamePasswordAuthentication) {
			UsernamePasswordAuthentication auth = (UsernamePasswordAuthentication) authentication;

			if (doesExist(auth.getUsername())) {
				String pw = auth.getPassword();
				if (!StringUtils.isBlank(pw)) {
					String encryptedPW = EncryptionUtil.encrypt(pw);
					String storedPW = prefsBean.getPassword();
					if (storedPW.equals(encryptedPW)) {
						return buildUser();
					}
				}
			}
		}
		throw new AuthenticationFailedException();
	}

	@Override
	public String getAdminName() throws FtpException {
		return prefsBean.getUserName();
	}

	@Override
	public boolean isAdmin(String username) throws FtpException {
		return doesExist(username);
	}

}
