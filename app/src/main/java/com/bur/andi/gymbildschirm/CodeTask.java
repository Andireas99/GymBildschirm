package com.bur.andi.gymbildschirm;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Andi on 07.08.2016.
 */
public class CodeTask extends AsyncTask<String, Void, String> {
    String ganzerCodeL;
    boolean websiteError = false;

    private CodeTaskDone listener;
    public Context mContext;

    public CodeTask(CodeTaskDone listener, Context context) {
        this.listener = listener;
        this.mContext = context;
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    @Override
    protected String doInBackground(String... params) {
        if(!isOnline()){
            return null;
        }
        try {

            String url = "https://sal.portal.bl.ch/gymow/dview/showterminliste.php?id=6zfgfbejsdtwgv3hcuwegujdbg";
            ganzerCodeL = getUrlSource(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ganzerCodeL;
    }

    @Override
    protected void onPostExecute(String result) {
        if(websiteError){
            Toast.makeText(mContext,"Website Error!",Toast.LENGTH_LONG).show();
        }
        listener.codeTaskDone(ganzerCodeL);
    }

    @Override
    protected void onPreExecute() {
        if(!isOnline()){
            Toast.makeText(mContext,"Keine Internetverbindung",Toast.LENGTH_SHORT).show();
            Log.e("GymBildschirm","Keine Internetverbindung");
            if(Tab1.swipeContainer!=null){
                Tab1.swipeContainer.setRefreshing(false);
                Tab2.swipeContainer.setRefreshing(false);
            }
        }
    }

    @Override
    protected void onProgressUpdate(Void... values) {
    }


    String getUrlSource(String url) throws IOException {
        Log.i("Info", "geturl start");
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        InputStream is = entity.getContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null){
            sb.append(line).append("\n");
        }

        String resString = sb.toString();

        is.close();
        Log.i("Info", "geturl end");
        resString = resString.replaceAll("&nbsp;","");
        resString = resString.replaceAll(";"," ");
        if(checkWebsiteError(resString)){
            websiteError = true;
            return null;
        }
        return resString;
    }

    private boolean checkWebsiteError(String code){

        if(code.contains("<title>Error</title>")){
            Log.e("GymBildschirm","Website Error!");
            return true;
        }

        return false;
    }
}