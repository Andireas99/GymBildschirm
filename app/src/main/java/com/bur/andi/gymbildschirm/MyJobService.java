package com.bur.andi.gymbildschirm;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.util.Log;

public class MyJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i("Gymbildschirm", "MyJobService.onStartJob");
        //Intent service = new Intent(getApplicationContext(), MyService.class);
        //getApplicationContext().startForegroundService(service);
        MyService.start(getApplicationContext());
        JobUtil.scheduleJob(getApplicationContext());
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
