
package com.android.settings;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.widget.ImageView;

import com.android.internal.telephony.Dsds;
import com.android.internal.telephony.PhoneConstants;

public class DualSimSettingsHost extends Activity {
    private ViewPager mViewPager;
    private final PageChangeListener mPageChangeListener = new PageChangeListener();
    private ViewPagerAdapter mViewPagerAdapter;

    private int[] mTabSimIcon = new int[2];

    private static final int TAB_INDEX_SIM  = 0;
    private static final int TAB_INDEX_SIM2 = 1;
    private static final int TAB_COUNT      = 1;
    private static final int TAB_COUNT_DSDS = 2;
    private static final boolean IS_DUAL_SIM_SOLUTION = Dsds.isDualSimSolution();

    private BroadcastReceiver mBroadcastReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.xml.dual_sim_settings_host);
        mViewPagerAdapter = new ViewPagerAdapter(getFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setOnPageChangeListener(mPageChangeListener);

        if (IS_DUAL_SIM_SOLUTION) {
            setupSimTab(PhoneConstants.SimId.SIM1);
            setupSimTab(PhoneConstants.SimId.SIM2);
            getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        } else {
            setupSimTab(PhoneConstants.SimId.SIM1);
        }

        Intent intent = getIntent();
        PhoneConstants.SimId simId;
        if (IS_DUAL_SIM_SOLUTION && intent != null && intent.hasExtra(PhoneConstants.SIM_ID_KEY)) {
            simId = (PhoneConstants.SimId)intent.getSerializableExtra(PhoneConstants.SIM_ID_KEY);
        } else {
            simId = Dsds.defaultSimId();
        }
        int index = simId == PhoneConstants.SimId.SIM1 ? TAB_INDEX_SIM : TAB_INDEX_SIM2;
        if (IS_DUAL_SIM_SOLUTION) getActionBar().setSelectedNavigationItem(index);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SIM_ICON_CHANGED);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                PhoneConstants.SimId simId = (PhoneConstants.SimId)intent.getSerializableExtra(PhoneConstants.SIM_ID_KEY);
                refreshCurrentTabIcon(simId);
                mViewPagerAdapter.notifyDataSetChanged();
            }
        };
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private class PageChangeListener implements OnPageChangeListener {

        PageChangeListener() {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            final ActionBar actionBar = getActionBar();
            if (IS_DUAL_SIM_SOLUTION) {
                final int actionBarSelectedNavIndex = actionBar.getSelectedNavigationIndex();
                if (actionBarSelectedNavIndex != position) {
                    actionBar.setSelectedNavigationItem(position);
                }
            } else {
                actionBar.selectTab(actionBar.getTabAt(TAB_INDEX_SIM));
            }
            invalidateOptionsMenu();
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }

    public class ViewPagerAdapter extends FragmentPagerAdapter {
        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fr = new DualSimSettingFragment();
            Bundle args = new Bundle();
            switch (position) {
                case TAB_INDEX_SIM:
                    args.putSerializable(PhoneConstants.SIM_ID_KEY, PhoneConstants.SimId.SIM1);
                    fr.setArguments(args);
                    return fr;
                case TAB_INDEX_SIM2:
                    if (IS_DUAL_SIM_SOLUTION) {
                        args.putSerializable(PhoneConstants.SIM_ID_KEY, PhoneConstants.SimId.SIM2);
                        fr.setArguments(args);
                        return fr;
                    }
            }
            throw new IllegalStateException("No fragment at position " + position);
        }

        @Override
        public int getCount() {
            return IS_DUAL_SIM_SOLUTION ? TAB_COUNT_DSDS : TAB_COUNT;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

    private final TabListener mTabListener = new TabListener() {
        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mViewPager.getCurrentItem() != tab.getPosition()) {
                mViewPager.setCurrentItem(tab.getPosition(), true);
            }
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    };

    private void setupSimTab(PhoneConstants.SimId simId) {
        final Tab tab = getActionBar().newTab();
        tab.setContentDescription(simId.toString());
        tab.setTabListener(mTabListener);
        String simName = Settings.SimCardInfo.getSimName(getContentResolver(), simId);
        int simIcon = Settings.SimCardInfo.getSimSmallIcon(getContentResolver(), simId);
        mTabSimIcon[simId.ordinal()] = simIcon;
        tab.setText(simName);
        tab.setIcon(simIcon);
        getActionBar().addTab(tab);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

    private void refreshCurrentTabIcon(PhoneConstants.SimId simId) {
        int simIcon = Settings.SimCardInfo.getSimSmallIcon(getContentResolver(), simId);
        if (simIcon == mTabSimIcon[simId.ordinal()]) {
            return;
        } else {
            mTabSimIcon[simId.ordinal()] = simIcon;
            getActionBar().getTabAt(simId.ordinal()).setIcon(simIcon);
            return;
        }
    }

}
