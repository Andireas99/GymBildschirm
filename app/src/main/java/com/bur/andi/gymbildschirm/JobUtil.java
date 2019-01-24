package com.bur.andi.gymbildschirm;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

class JobUtil {

    static void scheduleJob(Context context) {
        Log.i("Gymbildschirm", "JobUtil 1 - JobUtil.scheduleJob");
        ComponentName serviceComponent = new ComponentName(context, MyJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setMinimumLatency(5 * 60 * 1000);
        builder.setOverrideDeadline(10 * 60 * 1000);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
        Log.i("Gymbildschirm", "JobUtil 2 - JobUtil.scheduleJob done");
    }

}
