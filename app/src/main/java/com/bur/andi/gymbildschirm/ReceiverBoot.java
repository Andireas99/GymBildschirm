package com.bur.andi.gymbildschirm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Andi on 12.08.2016.
 */
public class ReceiverBoot extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("InfoS", "Start service");
        Intent alarmIntent = new Intent(context, ReceiverService.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        int interval = 10 * 60000;

        manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);

    }

    public static class ReceiverService extends  BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("Info","ReceiverService onReceive");
            context.startService(new Intent(context, MyService.class));
        }
    }

}
