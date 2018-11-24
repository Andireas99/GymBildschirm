package com.bur.andi.gymbildschirm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Random;

/**
 * Created by Andi on 12.08.2016.
 */
public class MyService extends JobIntentService implements CodeTaskDone {

    private final String path = "Old_Messages.txt";
    private final String pathLog = "Log_Messages.txt";

    public static void start(Context context){

        Intent starter = new Intent(context, MyService.class);
        enqueueWork(context, MyService.class, 1000, starter);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.i("Gymbildschirm", "MyService.onHandleWork");
        new CodeTaskS(this).execute("");
    }

    @Override
    public void codeTaskDone(String output) {
    }

    public void split(String GanzerCode) {
        Log.i("InfoSE", "split");

        if (GanzerCode == null) {
            return;
        }

        GanzerCode = GanzerCode.replaceAll("&nbsp;?", "");

        String[] codes;
        String[][] codes2;
        String[][] daten;
        int anzahl = 0;

        codes = GanzerCode.split("<tr>|</table>");

        for (int i = 0; i < codes.length - 1; i++) {
            if (codes[i] != null) {
                anzahl++;
            } else {
                break;
            }
        }
        codes2 = new String[anzahl - 1][13];
        for (int i = 0; i < anzahl - 1; i++) {
            codes2[i] = codes[i + 1].split("[<>]");
        }

        daten = new String[anzahl - 1][6];
        int bb = 0;

        for (int a = 0; a < anzahl - 1; a++) {
            for (int b = 2; bb < 6; b = b + 2) {
                daten[a][bb] = codes2[a][b];
                bb++;
            }
            bb = 0;
        }

        checkForNew(daten);

    }

    public void checkForNew(String[][] daten) {
        String[] oldMessages = readFile();
        String[] messages = new String[daten.length];

        ArrayList<String> newMessages = new ArrayList<>();
        boolean newMessage = true;
        int a, b;

        int l = daten.length - 1;
        for (int i = 0; i < daten.length; i++) {
            messages[i] = (daten[l - i][0] + ";" + daten[l - i][1] + ";" + daten[l - i][2] + ";" + daten[l - i][3] + ";" + daten[l - i][4] + ";"
                    + daten[l - i][5]).replaceAll("\\n", "");

        }
        writeFile(messages);
        if (oldMessages.length > 0) {
            for (a = 0; a < daten.length; a++) {
                for (b = 0; b < oldMessages.length; b++) {

                    if (oldMessages[b].equals(messages[a])) {
                        newMessage = false;
                        break;
                    }

                }

                if (newMessage) {
                    newMessages.add(messages[a]);
                }
                newMessage = true;
            }
        } else {
            Collections.addAll(newMessages, messages);
        }

        if (newMessages.size() > 0) {
            writeLog(newMessages);
            for (int i = 0; i < newMessages.size(); i++) {

                showNotification("Neue Nachricht", newMessages.get(i));
            }

        } else {
            Log.i("InfoSE", "no new messages");
        }
        onDestroy();

    }

    public static String[] formatToParts(String input) {
        input = input.trim();
        input = input.replaceAll("\\n", "").replaceAll("\\r", "");
        String[] daten = input.split(";");
        String output[] = new String[2];
        String datum = "";
        String zeit = "";

        if (!daten[0].isEmpty() && !daten[1].isEmpty()) {
            datum = "DATUM: Vom " + daten[0] + " bis zum " + daten[1] + " ";
        } else if (!daten[0].isEmpty() && daten[1].isEmpty()) {
            datum = "DATUM: Am " + daten[0] + " ";
        } else if (daten[0].isEmpty() && !daten[1].isEmpty()) {
            datum = "DATUM: Bis am " + daten[1] + " ";
        }

        if (!daten[2].isEmpty() && !daten[3].isEmpty()) {
            zeit = "ZEIT: Von " + daten[2] + " bis " + daten[3] + " ";
        } else if (!daten[2].isEmpty() && daten[3].isEmpty()) {
            zeit = "ZEIT: Um " + daten[2] + " ";
        } else if (daten[2].isEmpty() && !daten[3].isEmpty()) {
            zeit = "ZEIT: Bis um " + daten[3] + " ";
        }

        output[0] = daten[4];
        output[1] = datum + "\r\n" + zeit + "\r\n" + daten[5];
        return output;
    }

    public String getMessageString(String input) {

        input = input.trim();
        input = input.replaceAll("\\n", "").replaceAll("\\r", "");
        String[] daten = input.split(";");
        String output;
        String datum = "";
        String zeit = "";
        String betreff = "";

        if (!daten[4].isEmpty()) {
            betreff = "<b>BETREFF:</b> " + daten[4];
        }

        if (!daten[0].isEmpty() && !daten[1].isEmpty()) {
            datum = "<b>DATUM:</b> Vom " + daten[0] + " bis zum " + daten[1] + " ";
        } else if (!daten[0].isEmpty() && daten[1].isEmpty()) {
            datum = "<b>DATUM:</b> Am " + daten[0] + " ";
        } else if (daten[0].isEmpty() && !daten[1].isEmpty()) {
            datum = "<b>DATUM:</b> Bis am " + daten[1] + " ";
        }

        if (!daten[2].isEmpty() && !daten[3].isEmpty()) {
            zeit = "<b>ZEIT:</b> Von " + daten[2] + " bis " + daten[3] + " ";
        } else if (!daten[2].isEmpty() && daten[3].isEmpty()) {
            zeit = "<b>ZEIT:</b> Um " + daten[2] + " ";
        } else if (daten[2].isEmpty() && !daten[3].isEmpty()) {
            zeit = "<b>ZEIT:</b> Bis um " + daten[3] + " ";
        }

        output = "<html>" + betreff + "<br>" + datum + "<br>" + zeit + "<br>" + daten[5] + "</html>";
        return output;
    }

    private String[] readFile() {
        Log.i("InfoSE", "readFile start");
        String[] output = null;
        ArrayList<String> messagesList = new ArrayList<>();

        try {
            InputStream inputStream = this.openFileInput(path);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;

                while ((receiveString = bufferedReader.readLine()) != null) {
                    messagesList.add(receiveString);
                }
                if (messagesList.size() == 0) {
                    return new String[]{};
                }

                inputStream.close();

                output = new String[messagesList.size()];
                for (int i = 0; i < messagesList.size(); i++) {
                    output[i] = messagesList.get(i);
                }
                Log.i("InfoSE", "readfile done");
            }
        } catch (FileNotFoundException e) {
            Log.e("ErrorSE", "File not found: " + e.toString());
            output = new String[]{};
        } catch (IOException e) {
            Log.e("ErrorSE", "Can not read file: " + e.toString());
        }

        return output;
    }

    public void writeFile(String[] messages) {
        OutputStreamWriter out = null;
        try {
            out = new OutputStreamWriter(this.openFileOutput(path, Context.MODE_PRIVATE));

            String output = "";
            for (String message : messages) {
                output = output + message + "\n";

            }
            out.write(output);

        } catch (IOException ignored) {
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String[] readLog(boolean returnPlain) {
        Log.i("Info", "readLog start");

        ArrayList<String> logList = new ArrayList<>();

        try {
            InputStream inputStream = this.openFileInput(pathLog);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;

                if (returnPlain) {
                    ArrayList<String> plainList = new ArrayList<>();
                    while ((receiveString = bufferedReader.readLine()) != null) {
                        plainList.add(receiveString);
                    }
                    String[] plainOutput = new String[plainList.size()];
                    for (int i = 0; i < plainList.size(); i++) {
                        plainOutput[i] = plainList.get(i);
                    }
                    return plainOutput;
                }

                while ((receiveString = bufferedReader.readLine()) != null) {
                    if (receiveString.split("\\|").length == 2) {
                        logList.add("<html><b>Empfangen: </b>" + receiveString.split("\\|")[0] + "<br></html>" + getMessageString(receiveString.split("\\|")[1]));
                    } else {
                        logList.add("<html><b>ERROR</b><br></html>" + receiveString);
                        Log.e("Log Error", receiveString);
                        //Toast.makeText(this,"Log Fehler",Toast.LENGTH_SHORT).show();
                    }

                }
                if (logList.size() == 0) {
                    Log.e("Error", "Kein Log Inhalt");
                    return new String[0];
                }

                inputStream.close();

                Log.i("Info", "read log done");
            } else {
                return new String[0];
            }
        } catch (FileNotFoundException e) {
            Log.e("Error", "File not found: " + e.toString());
            logList = new ArrayList<>();
        } catch (IOException e) {
            Log.e("Error", "Can not read file: " + e.toString());
        }
        String[] output = new String[logList.size()];
        for (int i = 0; i < logList.size(); i++) {
            output[i] = logList.get(i);
        }

        return output;
    }

    public void writeLog(ArrayList<String> newMessages) {
        Log.i("Info", "writeLog");

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        String time = sdf.format(cal.getTime());

        String oldMessages[] =   readLog(true);

        OutputStreamWriter out = null;
        try {
            out = new OutputStreamWriter(this.openFileOutput(pathLog, Context.MODE_PRIVATE));

            for (String message : newMessages) {
                out.write(time + " |" + message + "\n");
            }
            for (String message : oldMessages) {
                out.write(message + "\n");
            }

        } catch (IOException ignored) {
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void showNotification(String title, String message) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        String id = "gymbildschirm_channel_1";

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationChannel mChannel = new NotificationChannel(id, "Gymbildschirm Channel",NotificationManager.IMPORTANCE_DEFAULT);
        mChannel.enableLights(true);

        notificationManager.createNotificationChannel(mChannel);

        Notification notification = new Builder(getApplicationContext(), id)
                .setTicker(title)
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle(formatToParts(message)[0])
                .setContentIntent(pi)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(formatToParts(message)[0] + "\n" + formatToParts(message)[1]))
                .setAutoCancel(true)
                .build();

        Random random = new Random();
        int m = random.nextInt(9999 - 1000) + 1000;
        notificationManager.notify(m, notification);
    }


    public class CodeTaskS extends AsyncTask<String, Void, String> {
        String sourceCode;
        Context mContext;

        public CodeTaskS(Context mContext) {
            this.mContext = mContext;
        }

        public boolean isOnline() {
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
        }

        @Override
        protected String doInBackground(String... params) {

            if (!isOnline()) {
                Log.e("GymBildschirm", "Keine Internetverbindung");
                return null;
            }

            try {
                String url = "https://sal.portal.bl.ch/gymow/dview/showterminliste.php?id=6zfgfbejsdtwgv3hcuwegujdbg";
                sourceCode = getUrlSource(url);
            } catch (IOException e) {

                e.printStackTrace();
            }
            return sourceCode;
        }


        @Override
        protected void onPostExecute(String result) {
            split(result);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }

        String getUrlSource(String url) throws IOException {
            Log.i("InfoSE", "geturl start");
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");

            String resString = sb.toString();

            is.close();
            Log.i("InfoSE", "geturl end");
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