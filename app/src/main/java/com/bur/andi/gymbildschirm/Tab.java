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
import java.util.Objects;

public class Tab extends ListFragment {

    ArrayAdapter<Spanned> adapter;
    private ListView lv;
    private SwipeRefreshLayout swipeContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.tab, container, false);
        lv = v.findViewById(android.R.id.list);

        swipeContainer = v.findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                MyService.start(getActivity());
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

        ArrayList<String> entries;
        ArrayList<Spanned> as = new ArrayList<>();

        if (getArguments() != null) {
            entries = getArguments().getStringArrayList("entries");

            for (int i = 0; i < Objects.requireNonNull(entries).size(); i++) {
                as.add(Html.fromHtml(entries.get(i), Html.FROM_HTML_MODE_COMPACT));
            }
        }

        adapter = new ArrayAdapter<>(lv.getContext(), android.R.layout.simple_list_item_1, as);
        lv.setAdapter(adapter);
        ((ArrayAdapter) lv.getAdapter()).notifyDataSetChanged();

        Log.i("GymBildschirm", "onActivityCreated Tab1");
    }

    void stopRefreshing(){
        swipeContainer.setRefreshing(false);
    }

}