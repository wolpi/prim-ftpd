package org.primftpd.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.widget.Toast;

import org.primftpd.R;
import org.primftpd.util.Defaults;

import java.io.File;

public class StartDirEditTextPreference extends EditTextPreference implements Preference.OnPreferenceChangeListener
{
	public StartDirEditTextPreference(
			Context context,
			AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		setOnPreferenceChangeListener(this);
	}

	public StartDirEditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnPreferenceChangeListener(this);
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return Defaults.HOME_DIR.getAbsolutePath();
	}

	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		super.onPrepareDialogBuilder(builder);
		builder.setNeutralButton(R.string.reset, this);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		super.onClick(dialog, which);

		if (which == DialogInterface.BUTTON_NEUTRAL) {
			setText(Defaults.HOME_DIR.getAbsolutePath());
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		File file = new File(newValue.toString());
		boolean valid = file.exists() && file.isDirectory();
		if (!valid) {
			Toast.makeText(
				getContext(),
				R.string.invalidDir,
				Toast.LENGTH_LONG).show();
		}
		return valid;
	}
}
