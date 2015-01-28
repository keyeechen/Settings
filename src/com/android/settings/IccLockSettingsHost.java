package com.android.settings;

import android.app.Activity;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.View;

import com.android.internal.telephony.Dsds;
import com.android.internal.telephony.PhoneConstants;

public class IccLockSettingsHost extends Activity {
    private static final boolean DEBUG = true;
    private static final String TAG = "IccLockSettingsHost";

    /** Used both by {@link ActionBar} and {@link ViewPagerAdapter} */
    private static final int TAB_INDEX_SIM1 = 0;
    private static final int TAB_INDEX_SIM2 = 1;
    private static final int TAB_COUNT      = 1;
    private static final int TAB_COUNT_DSDS = 2;

    private static final boolean IS_DUAL_SIM_SOLUTION = Dsds.isDualSimSolution();

    private IccLockSettings[] mIccLockSettingsFragment = new IccLockSettings[2];

    public class ViewPagerAdapter extends FragmentPagerAdapter {
        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            mIccLockSettingsFragment[position] = new IccLockSettings();
            Bundle args = new Bundle();
            PhoneConstants.SimId simid = position == TAB_INDEX_SIM1 ? PhoneConstants.SimId.SIM1 : PhoneConstants.SimId.SIM2;
            args.putSerializable(PhoneConstants.SIM_ID_KEY, simid);
            mIccLockSettingsFragment[position].setArguments(args);
            return mIccLockSettingsFragment[position];
        }

        @Override
        public int getCount() {
            if(IS_DUAL_SIM_SOLUTION)
                return TAB_COUNT_DSDS;
            else
                return TAB_COUNT;
        }

        @Override
        public int getItemPosition(Object object) {
            return PagerAdapter.POSITION_NONE;
        }
    }

    boolean mDuringSwipe = false;
    boolean mUserTabClick = false;

    private class PageChangeListener implements OnPageChangeListener {
        private int mCurrentPosition = -1;
        /**
         * Used during page migration, to remember the next position {@link #onPageSelected(int)}
         * specified.
         */
        private int mNextPosition = -1;

        @Override
        public void onPageScrolled(
                int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            if (DEBUG) Log.d(TAG, "onPageSelected: " + position);
            final ActionBar actionBar = getActionBar();

            if (mCurrentPosition == position) {
                Log.w(TAG, "Previous position and next position became same (" + position + ")");
            }

            actionBar.selectTab(actionBar.getTabAt(position));
            mNextPosition = position;
        }

        public void setCurrentPosition(int position) {
            mCurrentPosition = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {

            switch (state) {
                case ViewPager.SCROLL_STATE_IDLE: {
                    if (DEBUG) Log.d(TAG, "onPageScrollStateChanged() with SCROLL_STATE_IDLE");
                    // Interpret IDLE as the end of migration (both swipe and tab click)
                    mDuringSwipe = false;
                    mUserTabClick = false;

                    invalidateOptionsMenu();
                    mCurrentPosition = mNextPosition;
                    break;
                }
                case ViewPager.SCROLL_STATE_DRAGGING: {
                    if (DEBUG) Log.d(TAG, "onPageScrollStateChanged() with SCROLL_STATE_DRAGGING");
                    mDuringSwipe = true;
                    mUserTabClick = false;

                    invalidateOptionsMenu();
                    break;
                }
                case ViewPager.SCROLL_STATE_SETTLING: {
                    if (DEBUG) Log.d(TAG, "onPageScrollStateChanged() with SCROLL_STATE_SETTLING");
                    mDuringSwipe = true;
                    mUserTabClick = false;
                    break;
                }
                default:
                    break;
            }
        }
    }


    private ViewPager mViewPager;
    private ViewPagerAdapter mViewPagerAdapter = new ViewPagerAdapter(getFragmentManager());
    private final PageChangeListener mPageChangeListener = new PageChangeListener();

    private final TabListener mTabListener = new TabListener() {
        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (DEBUG) Log.d(TAG, "onTabUnselected(). tab: " + tab);
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (DEBUG) {
                Log.d(TAG, "onTabSelected(). tab: " + tab + ", mDuringSwipe: " + mDuringSwipe);
            }

            if (!mDuringSwipe) {
                mUserTabClick = true;
            }

            if (mViewPager.getCurrentItem() != tab.getPosition()) {
                mViewPager.setCurrentItem(tab.getPosition(), true);
            }
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            if (DEBUG) Log.d(TAG, "onTabReselected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Utils.isMonkeyRunning()) {
            finish();
            return;
        }

        setContentView(R.xml.sim_lock_settings_host);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setOnPageChangeListener(mPageChangeListener);

        if (IS_DUAL_SIM_SOLUTION) {
            setupTab(PhoneConstants.SimId.SIM1);
            setupTab(PhoneConstants.SimId.SIM2);
            setCurrentTab(TAB_INDEX_SIM1);
            getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            getActionBar().setDisplayShowTitleEnabled(false);
            getActionBar().setDisplayShowHomeEnabled(false);
        } else {
            final Tab tab = getActionBar().newTab();
            tab.setContentDescription("sim1");
            tab.setTabListener(mTabListener);
            getActionBar().addTab(tab);
        }
    }

    private void setupTab(PhoneConstants.SimId simid) {
        final Tab tab = getActionBar().newTab();
        tab.setContentDescription(simid.toString());
        tab.setTabListener(mTabListener);

        String simName = Settings.SimCardInfo.getSimName(this.getContentResolver(), simid);
        int simIcon = Settings.SimCardInfo.getSimSmallIcon(this.getContentResolver(), simid);

        tab.setText(simName);
        tab.setIcon(simIcon);
        getActionBar().addTab(tab);
    }

    private void setCurrentTab(int index) {
        mViewPager.setCurrentItem(index, false);
        mPageChangeListener.setCurrentPosition(index);
        mDuringSwipe = false;
        mUserTabClick = false;
    }

    public void notifyDataChanged() {
        mViewPagerAdapter.notifyDataSetChanged();
    }

    public int getTabCount() {
        return mViewPagerAdapter.getCount();
    }
}


