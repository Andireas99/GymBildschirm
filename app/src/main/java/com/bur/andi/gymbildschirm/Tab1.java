package com.bur.andi.gymbildschirm;


import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class Tab1 extends ListFragment {

    static ArrayAdapter<Spanned> adapter;
    ListView lv;
    public static SwipeRefreshLayout swipeContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.tab_1, container, false);
        lv = (ListView) v.findViewById(android.R.id.list);

        swipeContainer = (SwipeRefreshLayout) v.findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                MainActivity.runCodeTask();
            }
        });

        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ArrayList<Spanned> as = new ArrayList<>();

        for (int i = 0; i < MainActivity.messagesList.size(); i++) {
            as.add(Html.fromHtml(MainActivity.messagesList.get(i).toString()));
        }

        adapter = new ArrayAdapter<>(lv.getContext(), android.R.layout.simple_list_item_1, as);
        lv.setAdapter(adapter);
        ((ArrayAdapter) lv.getAdapter()).notifyDataSetChanged();

        Log.i("GymBildschirm", "onActivityCreated Tab1");
    }

}