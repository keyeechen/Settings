package com.android.settings;

import java.util.HashMap;

import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

public class DebugLogSetting extends PreferenceActivity implements OnPreferenceChangeListener {

	private ListPreference mLogLevelSettingPrefList;
	private ListPreference mAnrPanicSettingPrefList;

	@Override
	public boolean onPreferenceChange(Preference preference, Object objValue) {
		if (preference == mLogLevelSettingPrefList) {
			SystemProperties.set("persist.sys.debug.dump", objValue.toString());
		} else if (preference == mAnrPanicSettingPrefList) {
			SystemProperties.set("persist.sys.debug.anr", objValue.toString());
		}

		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(createPreferenceHierarchy());
	}

	private PreferenceScreen createPreferenceHierarchy() {
        // Root
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
		// Log level
		String [] mLogLevel = {"High","Middle","Low"};
		String [] mLogLevelValue = {"high","mid","low"};
		String mProp1 = SystemProperties.get("persist.sys.debug.dump");
		mLogLevelSettingPrefList = new ListPreference(this);
		mLogLevelSettingPrefList.setTitle("Log level setting");
		mLogLevelSettingPrefList.setEntries(mLogLevel);
		mLogLevelSettingPrefList.setEntryValues(mLogLevelValue);
		if ( mProp1 != null )
			mLogLevelSettingPrefList.setDefaultValue(mProp1);
		else
			mLogLevelSettingPrefList.setDefaultValue(mLogLevelValue[2]);
		root.addPreference(mLogLevelSettingPrefList);
		mLogLevelSettingPrefList.setOnPreferenceChangeListener(this);
		// ANR panic
		String [] mAnrPanic = {"Turn on","Turn off"};
		String [] mAnrPanicValue = {"panic", "log"};
		String mProp2 = SystemProperties.get("persist.sys.debug.anr");
		mAnrPanicSettingPrefList = new ListPreference(this);
		mAnrPanicSettingPrefList.setTitle("ANR panic setting");
		mAnrPanicSettingPrefList.setEntries(mAnrPanic);
		mAnrPanicSettingPrefList.setEntryValues(mAnrPanicValue);
		if ( mProp2 !=null && mProp2.equals(mAnrPanicValue[0]) )
			mAnrPanicSettingPrefList.setDefaultValue(mAnrPanicValue[0]);
		else
			mAnrPanicSettingPrefList.setDefaultValue(mAnrPanicValue[1]);
		root.addPreference(mAnrPanicSettingPrefList);
		mAnrPanicSettingPrefList.setOnPreferenceChangeListener(this);
		return root;
	}
}

