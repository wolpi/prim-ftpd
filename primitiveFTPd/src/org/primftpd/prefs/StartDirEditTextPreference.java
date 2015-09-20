package org.primftpd.prefs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import org.primftpd.util.Defaults;

public class StartDirEditTextPreference extends EditTextPreference {

	public StartDirEditTextPreference(Context context) {
		super(context);
	}

	public StartDirEditTextPreference(
			Context context,
			AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	public StartDirEditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		setText(Defaults.HOME_DIR.getAbsolutePath());
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return Defaults.HOME_DIR.getAbsolutePath();
	}
}
