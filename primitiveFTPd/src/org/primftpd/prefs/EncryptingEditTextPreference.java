package org.primftpd.prefs;

import org.primftpd.util.EncryptionUtil;
import org.primftpd.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class EncryptingEditTextPreference extends EditTextPreference
{

	public EncryptingEditTextPreference(Context context)
	{
		super(context);
	}

	public EncryptingEditTextPreference(
			Context context,
			AttributeSet attrs,
			int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public EncryptingEditTextPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}


	protected static final Logger logger = LoggerFactory.getLogger(EncryptingEditTextPreference.class);

	@Override
	public String getText()
	{
		logger.debug("getText()");

		return "";
	}

	@Override
	public void setText(String text)
	{
		logger.debug("setText()");

		if (StringUtils.isBlank(text)) {
			logger.debug("is blank");

			super.setText(null);
			return;
		}
		super.setText(EncryptionUtil.encrypt(text));
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
	{
		super.setText(restoreValue
				? getPersistedString(null)
				: (String) defaultValue);
	}
}
