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

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.Settings;

import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.telephony.TelephonyProperties;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.util.Log;

public class AirplaneModeEnabler implements Preference.OnPreferenceChangeListener {

    private final static String TAG = "AirplaneModeEnabler";
    private final Context mContext;

    private PhoneStateIntentReceiver mPhoneStateReceiver;

    private final CheckBoxPreference mCheckBoxPref;

    private static final int EVENT_SERVICE_STATE_CHANGED = 3;

    private int mBluetoothPersistState = 0;
    private int mWifiPersistState = 0;
    private final IntentFilter intentFilter = new IntentFilter();
    private BroadcastReceiver receiver = null;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_SERVICE_STATE_CHANGED:
                    onAirplaneModeChanged();
                    break;
            }
        }
    };

    private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onAirplaneModeChanged();
        }
    };

    public AirplaneModeEnabler(Context context, CheckBoxPreference airplaneModeCheckBoxPreference) {

        mContext = context;
        mCheckBoxPref = airplaneModeCheckBoxPreference;

        airplaneModeCheckBoxPreference.setPersistent(false);

        mPhoneStateReceiver = new PhoneStateIntentReceiver(mContext, mHandler);
        mPhoneStateReceiver.notifyServiceState(EVENT_SERVICE_STATE_CHANGED);

        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                    int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                            WifiManager.WIFI_STATE_UNKNOWN);
                    Log.i(TAG, "WifiManager WIFI_STATE_CHANGED_ACTION " + state);
                    if (state == WifiManager.WIFI_STATE_ENABLED && mWifiPersistState == 3) {
                        mWifiPersistState = 0;
                    } else if (state == WifiManager.WIFI_STATE_DISABLED
                            && (mWifiPersistState == 1 || mWifiPersistState == 3)) {
                        mWifiPersistState = 0;
                    }
                    onAirplaneModeChanged();
                } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    Log.i(TAG, "BluetoothAdapter ACTION_STATE_CHANGED " + state);
                    if (state == BluetoothAdapter.STATE_ON && mBluetoothPersistState == 2) {
                        mBluetoothPersistState = 0;
                    } else if (state == BluetoothAdapter.STATE_OFF && mBluetoothPersistState == 1) {
                        mBluetoothPersistState = 0;
                    }
                    onAirplaneModeChanged();
                }

            }
        };
    }

    public void resume() {
        mCheckBoxPref.setEnabled(true);
        mCheckBoxPref.setChecked(isAirplaneModeOn(mContext));

        mPhoneStateReceiver.registerIntent();
        mCheckBoxPref.setOnPreferenceChangeListener(this);
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), true,
                mAirplaneModeObserver);
        mContext.registerReceiver(receiver, intentFilter);
    }

    public void pause() {
        mPhoneStateReceiver.unregisterIntent();
        mCheckBoxPref.setOnPreferenceChangeListener(null);
        mContext.getContentResolver().unregisterContentObserver(mAirplaneModeObserver);
        mContext.unregisterReceiver(receiver);
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void setAirplaneModeOn(boolean enabling) {

        mCheckBoxPref.setEnabled(false);

        mBluetoothPersistState = getBluetoothPersistedState();
        mWifiPersistState = getWifiPersistedState();
        Log.i(TAG, "setAirplaneModeOn mBluetoothPersistState == " + mBluetoothPersistState
                + " mWifiPersistState == " + mWifiPersistState);

        if (!enabling) {
            if (mBluetoothPersistState == 1) {
                mBluetoothPersistState = 0;
            }
            if (mWifiPersistState == 2) {
                mWifiPersistState = 0;
            }
        }

        // Change the system setting
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON,
                enabling ? 1 : 0);
        // Update the UI to reflect system setting
        mCheckBoxPref.setChecked(enabling);

        // Post the intent
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enabling);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /**
     * Called when we've received confirmation that the airplane mode was set.
     * TODO: We update the checkbox summary when we get notified that mobile
     * radio is powered up/down. We should not have dependency on one radio
     * alone. We need to do the following: - handle the case of wifi/bluetooth
     * failures - mobile does not send failure notification, fail on timeout.
     */
    private void onAirplaneModeChanged() {
        Log.i(TAG, "onAirplaneModeChanged mBluetoothPersistState == " + mBluetoothPersistState
                + " mWifiPersistState == " + mWifiPersistState);

        mCheckBoxPref.setChecked(isAirplaneModeOn(mContext));

        if (mBluetoothPersistState == 0 && mWifiPersistState == 0) {
            mCheckBoxPref.setEnabled(true);
        }
    }

    /**
     * Called when someone clicks on the checkbox preference.
     */
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Boolean.parseBoolean(
                SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
            // In ECM mode, do not update database at this point
        } else {
            setAirplaneModeOn((Boolean) newValue);
        }
        return true;
    }

    public void setAirplaneModeInECM(boolean isECMExit, boolean isAirplaneModeOn) {
        if (isECMExit) {
            // update database based on the current checkbox state
            setAirplaneModeOn(isAirplaneModeOn);
        } else {
            // update summary
            onAirplaneModeChanged();
        }
    }

    private int getBluetoothPersistedState() {
        return Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.BLUETOOTH_ON,
                0);
    }

    private int getWifiPersistedState() {
        return Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.WIFI_ON, 0);
    }

}
