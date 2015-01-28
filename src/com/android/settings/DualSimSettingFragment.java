
package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.Dsds;
import com.android.internal.telephony.PhoneConstants;

public class DualSimSettingFragment extends PreferenceFragment {
    private static final String TAG = "DualSimSettingFragment";

    private static final String KEY_TOGGLE_SIM = "toggle_sim";
    private static final String KEY_NAME_SIM = "name_sim";
    private static final String KEY_ICON_SIM = "icon_sim";
    private static final String KEY_PIN_SIM = "enter_pin_sim";

    private EditTextPreference mNameSim;
    private IconListPreference mIconSim;
    private Preference mPinSim;
    private SimEnabler mSimEnabler;
    private CheckBoxPreference mEnableSimPreference;

    private PhoneConstants.SimId mSimId;
    private IntentFilter mFilter;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_REPORT_SIM_UNLOCK_RESULT)) {
                boolean success = intent.getBooleanExtra(PhoneConstants.SIM_UNLOCK_RESULT_KEY, false);
                PhoneConstants.SimId simId = (PhoneConstants.SimId)intent.getSerializableExtra(PhoneConstants.SIM_ID_KEY);
                if (success && (simId == mSimId)) {
                    if (mPinSim != null) {
                        getPreferenceScreen().removePreference(mPinSim);
                    }
                } else {
                    if (mPinSim != null) {
                        mPinSim.setEnabled(true);
                    }
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.dual_sim_setting_fragment);

        mSimId = (PhoneConstants.SimId)getArguments().getSerializable(PhoneConstants.SIM_ID_KEY);
        if (mSimId == null) mSimId = Dsds.defaultSimId();

        mFilter = new IntentFilter(Intent.ACTION_REPORT_SIM_UNLOCK_RESULT);
        setupUI();
    }


    private void setupUI() {
        int resIdEnable = R.string.enable_sim;
        int resIdName = R.string.sim_name;
        int resIdIcon = R.string.sim_icon;

        CheckBoxPreference enablesim = (CheckBoxPreference) findPreference(KEY_TOGGLE_SIM);
        enablesim.setTitle(resIdEnable);
        mSimEnabler = new SimEnabler(getActivity(), enablesim, mSimId);
        mEnableSimPreference = (CheckBoxPreference) findPreference(KEY_TOGGLE_SIM);
        mEnableSimPreference.setTitle(resIdEnable);

        mNameSim = (EditTextPreference) findPreference(KEY_NAME_SIM);
        mNameSim.setSummary(Settings.SimCardInfo.getSimName(getActivity().getContentResolver(), mSimId));
        mNameSim.setText(Settings.SimCardInfo.getSimName(getActivity().getContentResolver(), mSimId));
        mNameSim.setTitle(resIdName);
        mNameSim.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                mNameSim.setSummary(summary);

                ContentResolver resolver = getActivity().getContentResolver();
                Settings.SimCardInfo.SetSimName(resolver, mSimId, summary);
                return true;
            }
        });

        mIconSim = (IconListPreference) findPreference(KEY_ICON_SIM);
        mIconSim.setSimIconResources(mSimId);
        int simIcon = Settings.SimCardInfo.getSimLargeIcon(getActivity().getContentResolver(), mSimId);
        mIconSim.setValue(simIcon);
        mIconSim.setTitle(resIdIcon);
        mIconSim.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final Integer[] icons = (Integer[]) newValue;
                ContentResolver resolver = getActivity().getContentResolver();
                Settings.SimCardInfo.SetSimLargeIcon(resolver, mSimId, icons[0]);
                Settings.SimCardInfo.SetSimSmallIcon(resolver, mSimId, icons[1]);

                Intent intent = new Intent(Intent.ACTION_SIM_ICON_CHANGED);
                intent.putExtra(PhoneConstants.SIM_ID_KEY, mSimId);
                getActivity().sendBroadcast(intent);

                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(mReceiver, mFilter);
        mNameSim.setSummary(Settings.SimCardInfo.getSimName(getActivity().getContentResolver(), mSimId));
        mIconSim.setSimIconResources(mSimId);
        int simIcon = Settings.SimCardInfo.getSimLargeIcon(getActivity().getContentResolver(), mSimId);
        mIconSim.setValue(simIcon);
        mSimEnabler.resume();

        int state = TelephonyManager.getDefault().getSimStateDs(mSimId.ordinal());
        if (state == TelephonyManager.SIM_STATE_PIN_REQUIRED
                || state == TelephonyManager.SIM_STATE_PUK_REQUIRED) {
            if (mPinSim != null) {
                mPinSim.setEnabled(true);
            } else {
                addPreferencesFromResource(R.xml.enter_pin_sim);
                mPinSim = (Preference) findPreference(KEY_PIN_SIM);
                mPinSim.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(Intent.ACTION_ENTER_SIM_PIN);
                        intent.putExtra(PhoneConstants.SIM_ID_KEY, mSimId);
                        DualSimSettingFragment.this.getActivity().sendBroadcast(intent);
                        return true;
                    }
                });
            }
        } else {
            if (mPinSim != null) {
                getPreferenceScreen().removePreference(mPinSim);
                mPinSim = null;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mSimEnabler.pause();
    }

}
