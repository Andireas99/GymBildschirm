package com.bur.andi.gymbildschirm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Observable;
import java.util.Observer;


public class MainActivity extends AppCompatActivity implements Observer {

    private ViewPagerAdapter viewAdapter;
    private final CharSequence tabsTitles[] = {"Nachrichten", "Log"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        viewAdapter = new ViewPagerAdapter(getSupportFragmentManager(), tabsTitles, tabsTitles.length);

        ViewPager pager = findViewById(R.id.pager);
        pager.setAdapter(viewAdapter);

        SlidingTabLayout tabs = findViewById(R.id.tabs);
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

    private void startBootReceiver() {
        Log.i("Info", "startBootReceiver");

        Intent bootReceiverIntent = new Intent(this, ReceiverBoot.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, bootReceiverIntent, 0);

        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
    }

    @Override
    public void update(Observable o, final Object arg) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewAdapter.refreshContent((Bundle) arg);
            }
        });

    }
}