package org.primftpd;

import android.os.Environment;

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.primftpd.util.EncryptionUtil;
import org.primftpd.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AndroidPrefsUserManager implements UserManager {

	public static final String ANONYMOUS_USER_NAME = "anonymous";

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
        return createUser(prefsBean.getUserName(), prefsBean.getPassword());
	}

    protected User anonymousUser() {
        return createUser(ANONYMOUS_USER_NAME, null);
    }

    private User createUser(String username, String password) {
        BaseUser user = new BaseUser();
        user.setEnabled(true);

        String rootDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        logger.debug("rootDir: {}", rootDir);
        user.setHomeDirectory(rootDir);

        user.setMaxIdleTime(60);
        user.setName(username);
        if(password != null) {
            user.setPassword(prefsBean.getPassword());
        }
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
		return new String[]{prefsBean.getUserName(), ANONYMOUS_USER_NAME};
	}

	@Override
	public void delete(String username) throws FtpException {
	}

	@Override
	public void save(User user) throws FtpException {
	}

	@Override
	public boolean doesExist(String username) {
		return prefsBean.getUserName().equals(username) || ANONYMOUS_USER_NAME.equals(username);
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
		} else if(authentication instanceof AnonymousAuthentication) {
            if(prefsBean.isAnonymousLogin()) {
                return anonymousUser();
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
		return prefsBean.getUserName().equals(username);
	}

}
