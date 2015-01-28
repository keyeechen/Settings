/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.Dsds;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;


public class SimEnabler implements Preference.OnPreferenceChangeListener {
    private  String mTag = "SimEnabler";

    private final Context mContext;

    private PhoneStateListener mPhoneStateListener;
    private TelephonyManager mTelephonyManager;

    private final CheckBoxPreference mCheckBoxPref;

    private final PhoneConstants.SimId mSimId;
    private boolean mCheckBoxEnabled;
    private boolean mCurEnabled;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                boolean airplaneEnabled = Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
                Log.d(mTag, "airplaneEnabled: " + airplaneEnabled);
                mCheckBoxEnabled = Dsds.hasIcc(mSimId.ordinal()) && !airplaneEnabled;
                mCheckBoxPref.setEnabled(mCheckBoxEnabled);
            }
        }
    };

    public SimEnabler(Context context, CheckBoxPreference activeSimCheckBoxPreference,
                                     PhoneConstants.SimId simId) {
        mContext = context;
        mCheckBoxPref = activeSimCheckBoxPreference;
        mSimId = simId;
        mTag = mTag + simId;
        activeSimCheckBoxPreference.setPersistent(false);

        mTelephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        mPhoneStateListener = new PhoneStateListener() {
                @Override
                public void onServiceStateChanged(ServiceState serviceState) {
                    onSimEnableChanged(serviceState);
                }
        };

        mCurEnabled = isSimEnabled();

    }

    public void resume() {
        boolean simEnabled = isSimEnabled();
        mCheckBoxPref.setEnabled(false);
        mCheckBoxPref.setChecked(simEnabled);
        mCheckBoxPref.setSummary(simEnabled ? null :
                mContext.getString(R.string.enable_sim_summary));
        boolean airplaneEnabled = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        mCheckBoxEnabled = Dsds.hasIcc(mSimId.ordinal()) && !airplaneEnabled;
        mCheckBoxPref.setEnabled(mCheckBoxEnabled);
        mCurEnabled = isSimEnabled();

        mTelephonyManager.listenDs(mSimId.ordinal(), mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        mCheckBoxPref.setOnPreferenceChangeListener(this);

        IntentFilter intentFilter =
                new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mContext.registerReceiver(mReceiver, intentFilter);
    }

    public void pause() {
        mTelephonyManager.listenDs(mSimId.ordinal(), mPhoneStateListener, 0);
        mCheckBoxPref.setOnPreferenceChangeListener(null);
        mContext.unregisterReceiver(mReceiver);
    }

    private boolean isSimEnabled() {
        boolean enabled;
        if (mSimId == PhoneConstants.SimId.SIM1) {
            enabled = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.ENABLE_SIM1, 1) != 0;
        } else {
            enabled = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.ENABLE_SIM2, 1) != 0;
        }
        return enabled;
    }

    private void enableSim(boolean enabling) {
        mCurEnabled = enabling;
        mCheckBoxPref.setSummary(enabling ?
                  R.string.enabling_sim : R.string.disabling_sim);

        if (PhoneConstants.SimId.SIM1 == mSimId) {
            Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ENABLE_SIM1, enabling ? 1 : 0);
        } else {
            Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ENABLE_SIM2, enabling ? 1 : 0);
        }

        Intent intent = new Intent(Intent.ACTION_SIM_ENABLE_CHANGED);
        intent.putExtra(PhoneConstants.STATE_KEY, enabling);
        intent.putExtra(PhoneConstants.SIM_ID_KEY, mSimId);
        mContext.sendBroadcastAsUser (intent, UserHandle.ALL);
    }

    private void onSimEnableChanged(ServiceState serviceState) {
        int state = serviceState.getState();
        Log.d(mTag, "onSimEnableChanged, state: " + state);

        // we only check STATE_IN_SERVICE and STATE_POWER_OFF
        if (state == ServiceState.STATE_IN_SERVICE) {
            if (mCurEnabled) {
                // we enabled this sim card, so this state is meet
                mCheckBoxPref.setChecked(true);
                mCheckBoxPref.setSummary(null);
                mCheckBoxPref.setEnabled(mCheckBoxEnabled);
                return;
            } else {
                // we disabled this sim card, so this state don't meet, ignore.
                return;
            }
        }
        else if (state == ServiceState.STATE_POWER_OFF) {
            if (mCurEnabled) {
                // we just enable this sim card, so this state don't meet, ignore.
                return;
            } else {
                // we just disable this sim card, so this state is meet
                mCheckBoxPref.setChecked(false);
                mCheckBoxPref.setSummary(mContext.getString(R.string.enable_sim_summary));
                mCheckBoxPref.setEnabled(mCheckBoxEnabled);
            }
        } else {
            // other ServiceState, we don't care
            return;
        }
    }

    /**
     * Called when someone clicks on the checkbox preference.
     */
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mCheckBoxPref.setEnabled(false);
        enableSim((Boolean) newValue);
        return true;
    }
}
