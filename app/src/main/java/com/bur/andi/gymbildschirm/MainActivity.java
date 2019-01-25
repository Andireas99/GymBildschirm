package com.bur.andi.gymbildschirm;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;


@SuppressLint("ParcelCreator")
public class MainActivity extends AppCompatActivity implements Observer {

    Toolbar toolbar;
    ViewPager pager;
    ViewPagerAdapter viewAdapter;
    SlidingTabLayout tabs;
    CharSequence tabsTitles[] = {"Nachrichten", "Log"};

    public static ArrayList<String> messagesList;
    public static ArrayList<String> logList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("Info", "OnCreate");

        messagesList = new ArrayList<String>() {
        };
        logList = new ArrayList<String>() {
        };


        toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        viewAdapter = new ViewPagerAdapter(getSupportFragmentManager(), tabsTitles, tabsTitles.length);

        pager = findViewById(R.id.pager);
        pager.setAdapter(viewAdapter);

        tabs = findViewById(R.id.tabs);
        tabs.setDistributeEvenly(true);

        tabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return ContextCompat.getColor(getApplicationContext(), R.color.tabsScrollColor);
            }
        });

        tabs.setViewPager(pager);


        ServiceObserver.getInstance().addObserver(this);

        startBootReceiver();

        MyService.start(getApplicationContext());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Log.i("Info", "Einstellungen");
            Toast.makeText(this, "Noch keine Einstellungen", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startBootReceiver() {
        Log.i("Info", "startBootReceiver");

        Intent bootReceiverIntent = new Intent(this, ReceiverBoot.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, bootReceiverIntent, 0);

        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
    }

    public void addMessageToTab() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewAdapter.refreshContent();
            }
        });
    }

    @Override
    public void update(Observable o, Object arg) {
        Bundle bundle = (Bundle) arg;

        messagesList = bundle.getStringArrayList("messages");
        logList = new ArrayList<>(Arrays.asList(bundle.getStringArray("log")));

        addMessageToTab();
    }
}