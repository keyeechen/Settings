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

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.res.Resources;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.RadioButton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.provider.Settings.SimCardInfo;
import android.telephony.TelephonyManager;
import android.widget.SimpleAdapter;
import android.widget.ImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.preference.DialogPreference;
import java.lang.Integer;
import java.lang.String;
import com.android.internal.telephony.PhoneConstants;
import com.android.settings.R;

/**
 * A {@link Preference} that displays a list of entries as
 * a dialog.
 * <p>
 * This preference will store a string into the SharedPreferences. This string will be the value
 * from the {@link #setEntryValues(CharSequence[])} array.
 *
 * @attr ref android.R.styleable#ListPreference_entries
 * @attr ref android.R.styleable#ListPreference_entryValues
 */
public class IconListPreference extends DialogPreference {

    private int[] mEntries;
    private int[] mSmallIconEntries;
    private int mWidgetLayoutIds = R.layout.preference_list_icon_widget;

    private int mValue;
    private int mClickedDialogEntryIndex;
    private Context mContext;
    private ImageView mImageView;

    public IconListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        setWidgetLayoutResource(mWidgetLayoutIds);
        mEntries = SimCardInfo.sGsmSimLargeIconResources;
        mSmallIconEntries = SimCardInfo.sGsmSimSmallIconResources;
    }

    public IconListPreference(Context context) {
        this(context, null);
    }

    public void setSimIconResources(PhoneConstants.SimId simId) {
        String networkMode = TelephonyManager.getDefault().getNetworkModeDs(simId.ordinal());
        if (TelephonyManager.WCDMA_MODE.equals(networkMode)) {
            mEntries = SimCardInfo.sWcdmaSimLargeIconResources;
            mSmallIconEntries = SimCardInfo.sWcdmaSimSmallIconResources;
        } else if (TelephonyManager.TDSCDMA_MODE.equals(networkMode)) {
            mEntries = SimCardInfo.sTdcdmaSimLargeIconResources;
            mSmallIconEntries = SimCardInfo.sTdcdmaSimSmallIconResources;
        } else {
            mEntries = SimCardInfo.sGsmSimLargeIconResources;
            mSmallIconEntries = SimCardInfo.sGsmSimSmallIconResources;
        }
    }

    /**
     * Sets the value of the key. This should be one of the entries in
     * {@link #getEntryValues()}.
     *
     * @param value The value to set for the key.
     */
    public void setValue(int value) {
        if (value == SimCardInfo.sGsmSimLargeIconResources[0] || value == SimCardInfo.sWcdmaSimLargeIconResources[0] || value == SimCardInfo.sTdcdmaSimLargeIconResources[0]
                || value == SimCardInfo.sGsmSimSmallIconResources[0] || value == SimCardInfo.sWcdmaSimSmallIconResources[0] || value == SimCardInfo.sTdcdmaSimSmallIconResources[0]) {
            value = 0;
        }
        mValue = value;

        persistInt(value);
    }

    /**
     * Returns the value of the key. This should be one of the entries in
     * {@link #getEntryValues()}.
     *
     * @return The value of the key.
     */
    public int getValue() {
        return mValue;
    }

    /**
     * Returns the entry corresponding to the current value.
     *
     * @return The entry corresponding to the current value, or null.
     */
    public int getEntry() {
        int index = getValueIndex();
        return index >= 0 && mEntries != null ? mEntries[index] : null;
    }

    /**
     * Returns the index of the given value (in the entry values array).
     *
     * @param value The value whose index should be returned.
     * @return The index of the value, or -1 if not found.
     */
    public int findIndexOfValue(int value) {
        if (mEntries != null) {
            if (value == 0) return 0;

            for (int i = mEntries.length - 1; i >= 0; i--) {
                if (mEntries[i] == value) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int getValueIndex() {
        return findIndexOfValue(mValue);
    }

    private static class MyRadioAdapter extends SimpleAdapter
    {
        private int mCheckedIndex;
    
        public MyRadioAdapter(Context context, List<? extends Map<String, ?>> data,
            int resource, String[] from, int[] to, int checkedIndex) {
            super(context, data, resource, from, to);
            mCheckedIndex = checkedIndex;
        }
    
        @Override
        public View getView(final int position, View convertView, ViewGroup parent)
        {
            View view = super.getView(position, convertView, parent);
    
            RadioButton radio = (RadioButton) view.findViewById(R.id.radio_button);
    
            if(position == mCheckedIndex)
                radio.setChecked(true);
            else
                radio.setChecked(false);
            return view;
        }
    }


    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);

        if (mEntries == null) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array.");
        }

        mClickedDialogEntryIndex = getValueIndex();

        SimpleAdapter adapter = new MyRadioAdapter(mContext, putImage(),
            R.layout.preference_list_icon_item, new String[]{"image"}, new
            int[]{R.id.image}, mClickedDialogEntryIndex);

        builder.setTitle(R.string.change_sim_icon_summary);
        builder.setSingleChoiceItems(adapter, mClickedDialogEntryIndex,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mClickedDialogEntryIndex = which;

                        /*
                         * Clicking on an item simulates the positive button
                         * click, and dismisses the dialog.
                         */
                        IconListPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                        dialog.dismiss();
                    }
        });

        /*
         * The typical interaction for list-based dialogs is to have
         * click-on-an-item dismiss the dialog instead of the user having to
         * press 'Ok'.
         */
        builder.setPositiveButton(null, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult && mClickedDialogEntryIndex >= 0 && mEntries != null) {
             mImageView.setImageResource(mEntries[mClickedDialogEntryIndex]);

            Integer[] value = new Integer[2];
            if (mClickedDialogEntryIndex == 0) {
                value[0] = 0;
                value[1] = 0;
            } else {
                value[0] = mEntries[mClickedDialogEntryIndex];
                value[1] = mSmallIconEntries[mClickedDialogEntryIndex];
            }

            if (callChangeListener(value)) {
                setValue(value[0]);
            }
        }
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        final LayoutInflater layoutInflater =
            (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int layoutResId = getLayoutResource();
        final View layout = layoutInflater.inflate(layoutResId, parent, false);

        final ViewGroup widgetFrame = (ViewGroup)layout.findViewById(com.android.internal.R.id.widget_frame);
        layoutInflater.inflate(mWidgetLayoutIds, widgetFrame);

        mImageView = (ImageView) layout.findViewById(R.id.dual_sim_icon);

        int value = getValue();
        if( value == 0) {
            mImageView.setImageResource(mEntries[0]);
        } else {
            mImageView.setImageResource(value);
        }

        return layout;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        int value = restoreValue ? getPersistedInt(mValue) : (Integer) defaultValue;
        setValue(value);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.value = getValue();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setValue(myState.value);
    }

    private static class SavedState extends BaseSavedState {
        int value;

        public SavedState(Parcel source) {
            super(source);
            value = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(value);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public List<Map<String, Object>> putImage() {
        List list = new ArrayList<Map<String, Object>> ();
        for(int i=0; i < mEntries.length; i++) {
            Map  map;
            map = new HashMap<String, Object>();
            map.put("image", mEntries[i]);
            list.add(map);
        }
        return list;
    }
}
