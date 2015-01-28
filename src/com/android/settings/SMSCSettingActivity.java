/*
 * (C) Copyright 2012 Marvell International Ltd.
 * All Rights Reserved

 * MARVELL CONFIDENTIAL
 * Copyright 2008 ~ 2012 Marvell International Ltd All Rights Reserved.
 * The source code contained or described herein and all documents related to
 * the source code ("Material") are owned by Marvell International Ltd or its
 * suppliers or licensors. Title to the Material remains with Marvell International Ltd
 * or its suppliers and licensors. The Material contains trade secrets and
 * proprietary and confidential information of Marvell or its suppliers and
 * licensors. The Material is protected by worldwide copyright and trade secret
 * laws and treaty provisions. No part of the Material may be used, copied,
 * reproduced, modified, published, uploaded, posted, transmitted, distributed,
 * or disclosed in any way without Marvell's prior express written permission.
 *
 * No license under any patent, copyright, trade secret or other intellectual
 * property right is granted to or conferred upon you by disclosure or delivery
 * of the Materials, either expressly, by implication, inducement, estoppel or
 * otherwise. Any license under such intellectual property rights must be
 * express and approved by Marvell in writing.
 *
 */

package com.android.settings;

import com.android.settings.R;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.AsyncResult;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.Phone;

public class SMSCSettingActivity extends Activity implements OnClickListener {
    private static final int EVENT_QUERY_SMSC_DONE  = 1001;
    private static final int EVENT_UPDATE_SMSC_DONE = 1002;
    private String mDefault = "";
    private Phone mPhone;
    private Button mOkButton;
    private Button mCancelButton;
    private EditText mSmscEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smsc_setting);
        mPhone = PhoneFactory.getDefaultPhone();
        mOkButton = (Button) findViewById(R.id.smsc_ok);
        mCancelButton = (Button) findViewById(R.id.smsc_cancel);
        mSmscEditText = (EditText) findViewById(R.id.smsc);
        mOkButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
        setSmscEditText();
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.smsc_ok:
            mPhone.setSmscAddress(mSmscEditText.getText().toString(), mHandler.obtainMessage(EVENT_UPDATE_SMSC_DONE));
            break;
        case R.id.smsc_cancel:
            finish();
            break;
        }
    }

    public void setSmscEditText() {
        mPhone.getSmscAddress(mHandler.obtainMessage(EVENT_QUERY_SMSC_DONE));
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
            case EVENT_QUERY_SMSC_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    Toast.makeText(getApplicationContext(), R.string.no_valid_smsc, Toast.LENGTH_SHORT).show();
                    mSmscEditText.setText(mDefault);
                } else {
                    mSmscEditText.setText((String) ar.result);
                    mDefault = (String) ar.result;
                }
                break;
            case EVENT_UPDATE_SMSC_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    mSmscEditText.setText(mDefault);
                    Toast.makeText(getApplicationContext(), R.string.update_error, Toast.LENGTH_SHORT).show();
                } else {
                    mDefault = mSmscEditText.getText().toString();
                    Toast.makeText(getApplicationContext(), R.string.update_success, Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            }
        }
    };
}

