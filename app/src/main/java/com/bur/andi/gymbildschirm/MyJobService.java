package com.bur.andi.gymbildschirm;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

public class MyJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i("Gymbildschirm", "JobUtil 3 - MyJobService.onStartJob");

        MyService.startBg(getApplicationContext());
        JobUtil.scheduleJob(getApplicationContext());
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
