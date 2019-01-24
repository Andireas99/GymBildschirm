package com.bur.andi.gymbildschirm;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Created by Andi on 12.08.2016.
 */
public class MyService extends JobIntentService {

    private static ServiceResultReceiver mServiceResultReceiver;

    public static void start(Context context, ServiceResultReceiver serviceResultReceiver) {
        mServiceResultReceiver = serviceResultReceiver;

        Intent starter = new Intent(context, MyService.class);
        starter.putExtra("serviceResultReceiver", serviceResultReceiver);
        starter.setAction("1");
        enqueueWork(context, MyService.class, 1000, starter);
    }

    public static void startBg(Context context) {
        Intent starter = new Intent(context, MyService.class);
        starter.putExtra("serviceResultReceiver", mServiceResultReceiver);
        starter.setAction("2");
        enqueueWork(context, MyService.class, 1000, starter);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.i("Gymbildschirm", "MyService.onHandleWork");
        ResultReceiver mResultReceiver = intent.getParcelableExtra("serviceResultReceiver");

        if (mResultReceiver != null) {
            try {
                String out = new CodeTaskS(this).execute("").get();
                Bundle bundle = new Bundle();
                bundle.putString("code", out);
                mResultReceiver.send(Integer.parseInt(Objects.requireNonNull(intent.getAction())), bundle);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("Gymoberwil", "serviceResultReceiver is null");
        }
    }

    private static class CodeTaskS extends AsyncTask<String, Void, String> {
        String sourceCode;
        WeakReference<Context> contextRef;

        CodeTaskS(Context mContext) {
            contextRef = new WeakReference<>(mContext);
        }

        boolean isOnline() {
            ConnectivityManager cm = (ConnectivityManager) contextRef.get().getSystemService(Context.CONNECTIVITY_SERVICE);
            return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
        }

        @Override
        protected String doInBackground(String... params) {

            if (!isOnline()) {
                Log.e("GymBildschirm", "Keine Internetverbindung");
                return null;
            }

            try {
                sourceCode = getUrlSource();
            } catch (IOException e) {

                e.printStackTrace();
            }
            return sourceCode;
        }


        @Override
        protected void onPostExecute(String result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }

        private String getUrlSource() throws IOException {
            Log.i("Info", "geturl start");
            URL url = new URL("https://sal.portal.bl.ch/gymow/dview/showterminliste.php?id=6zfgfbejsdtwgv3hcuwegujdbg");

            HttpURLConnection response = (HttpURLConnection) url.openConnection();
            InputStream is = response.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            String resString = sb.toString();

            is.close();
            Log.i("Info", "geturl end");
            resString = resString.replaceAll("&nbsp;", "");
            resString = resString.replaceAll(";", " ");
            if (checkWebsiteError(resString)) {
                return null;
            }
            return resString;
        }

        private boolean checkWebsiteError(String code) {

            if (code.contains("<title>Error</title>")) {
                Log.e("GymBildschirm", "Website Error!");
                return true;
            }

            return false;
        }
    }

}