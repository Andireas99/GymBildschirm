package com.bur.andi.gymbildschirm;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

/**
 * Created by hp1 on 21-01-2015.
 * Modified by Andi
 */
class ViewPagerAdapter extends FragmentStatePagerAdapter {

    private Tab tab1;
    private Tab tab2;

    private final CharSequence titles[];
    private final int numbOfTabs;

    private final FragmentManager manager;

    ViewPagerAdapter(FragmentManager fm, CharSequence mTitles[], int mNumbOfTabs) {
        super(fm);

        manager = fm;
        this.titles = mTitles;
        this.numbOfTabs = mNumbOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        if (position == 0) {
            Log.i("Info", "ViewPagerAdapter Tab1");
            tab1 = new Tab();
            return tab1;
        } else {
            Log.i("Info", "ViewPagerAdapter Tab2");
            tab2 = new Tab();
            return tab2;
        }
    }

    void refreshContent(Bundle bundle) {

        Bundle bundle1 = new Bundle();
        Bundle bundle2 = new Bundle();

        bundle1.putStringArrayList("entries", bundle.getStringArrayList("messages"));
        bundle2.putStringArrayList("entries", bundle.getStringArrayList("logs"));

        tab1.adapter.clear();
        tab1.setArguments(bundle1);
        final FragmentTransaction ft1 = manager.beginTransaction();
        ft1.detach(tab1);
        ft1.attach(tab1);
        ft1.commit();
        tab1.stopRefreshing();

        tab2.adapter.clear();
        tab2.setArguments(bundle2);
        final FragmentTransaction ft2 = manager.beginTransaction();
        ft2.detach(tab2);
        ft2.attach(tab2);
        ft2.commit();
        tab2.stopRefreshing();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titles[position];
    }

    @Override
    public int getCount() {
        return numbOfTabs;
    }


}