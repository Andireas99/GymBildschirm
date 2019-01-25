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
public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    private Tab1 tab1;
    private Tab2 tab2;

    private CharSequence titles[];
    private int numbOfTabs;

    private FragmentManager manager;

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
            tab1 = new Tab1();
            return tab1;
        } else {
            Log.i("Info", "ViewPagerAdapter Tab2");
            tab2 = new Tab2();
            return tab2;
        }
    }

    void refreshContent() {

        tab1.adapter.clear();
        final FragmentTransaction ft1 = manager.beginTransaction();
        ft1.detach(tab1);
        ft1.attach(tab1);
        ft1.commit();
        tab1.setRefreshing(false);

        tab2.adapter.clear();
        final FragmentTransaction ft2 = manager.beginTransaction();
        ft2.detach(tab2);
        ft2.attach(tab2);
        ft2.commit();
        tab2.setRefreshing(false);
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