package com.bur.andi.gymbildschirm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/**
 * Created by Andi on 12.08.2016.
 */
public class MyService extends JobIntentService {

    private final String CURRENT_MESSAGES_PATH = "Old_Messages.txt";
    private final String LOG_MESSAGES_PATH = "Log_Messages.txt";

    public static ArrayList<String> messagesList;
    public static ArrayList<String> logList;

    public static void start(Context context) {

        Intent starter = new Intent(context, MyService.class);
        starter.setAction("1");
        enqueueWork(context, MyService.class, 1000, starter);
    }

    public static void startBg(Context context) {
        Intent starter = new Intent(context, MyService.class);
        starter.setAction("2");
        enqueueWork(context, MyService.class, 1000, starter);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.i("Gymbildschirm", "MyService.onHandleWork");

        messagesList = new ArrayList<String>() {
        };
        logList = new ArrayList<String>() {
        };

        try {
            String out = new WebsiteFetcher(this).execute("").get();

            if (intent.getAction().equals("1")) {
                String[][] daten = getDaten(out);
                checkForNew(daten);
                Bundle bundle = new Bundle();
                bundle.putStringArrayList("messages", messagesList);
                bundle.putStringArray("log", readLog(false));
                ServiceObserver.getInstance().addMessagesToTab(bundle);
            } else {
                String[][] daten = getDaten(out);
                checkForNew(daten);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onDestroy();
    }

    public String[][] getDaten(String ganzerCode) {
        Log.i("Info", "split");

        if (ganzerCode == null) {
            String[] messages = readFile();
            if (messagesList != null) {
                messagesList.clear();
            }
            for (String message : messages) {
                Log.i("GymBildschirm", message);
                messagesList.add(getMessageString(message));
            }
            return null;
        }

        String[] codes;
        String[][] codes2;
        String[][] daten;
        int anzahl = 0;

        codes = ganzerCode.split("<tr>|</table>");

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
                daten[a][bb] = codes2[a][b].trim();
                bb++;
            }
            bb = 0;
        }

        return daten;

    }

    public void checkForNew(String[][] daten) {
        String[] oldMessages = readFile();
        String[] messages = new String[daten.length];

        ArrayList<String> newMessages = new ArrayList<>();
        boolean newMessage = true;
        int a, b;
        messagesList.clear();


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

        for (String message : messages) {
            messagesList.add(getMessageString(message));
        }

        if (newMessages.size() > 0) {
            writeLog(newMessages);
            for (int i = 0; i < newMessages.size(); i++) {
                showNotification("Neue Nachricht", newMessages.get(i));
            }

        } else {
            Log.i("Info", "no new messages");
        }

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

        Log.i("Info", "input: " + input);

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

        if (daten.length < 6) {
            output = "<html>" + betreff + "<br>" + datum + "<br>" + zeit + "</html>";
        } else {
            output = "<html>" + betreff + "<br>" + datum + "<br>" + zeit + "<br>" + daten[5] + "</html>";
        }

        return output;
    }

    private String[] readFile() {
        Log.i("Info", "readFile start");
        String[] output = null;
        ArrayList<String> messagesList = new ArrayList<>();

        try {
            InputStream inputStream = this.openFileInput(CURRENT_MESSAGES_PATH);

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
                Log.i("Info", "readfile done");
            }
        } catch (FileNotFoundException e) {
            Log.e("Error", "File not found: " + e.toString());
            output = new String[]{};
        } catch (IOException e) {
            Log.e("Error", "Can not read file: " + e.toString());
        }

        return output;
    }

    private String[] readLog(boolean returnPlain) {
        Log.i("Info", "readLog start");

        ArrayList<String> logList = new ArrayList<>();

        try {
            InputStream inputStream = this.openFileInput(LOG_MESSAGES_PATH);

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
                        plainOutput[i] = plainList.get(i).replaceAll("&nbsp ", "");
                    }
                    return plainOutput;
                }

                while ((receiveString = bufferedReader.readLine()) != null) {
                    if (receiveString.split("\\|").length == 2) {
                        Log.i("Info", "logList getMessageString");
                        logList.add("<html><b>Empfangen: </b>" + receiveString.split("\\|")[0] + "<br></html>" + getMessageString(receiveString.split("\\|")[1]));
                    } else {
                        logList.add("<html><b>ERROR</b><br></html>" + receiveString);
                        Log.e("Log Error", receiveString);
                        //Toast.makeText(this,"Log Fehler",Toast.LENGTH_SHORT).show();
                    }

                }
                if (logList.size() == 0) {
                    Log.e("Error", "Keine Log Inhalt");
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

    public void writeFile(String[] messages) {
        OutputStreamWriter out = null;
        try {
            out = new OutputStreamWriter(this.openFileOutput(CURRENT_MESSAGES_PATH, Context.MODE_PRIVATE));

            StringBuilder output = new StringBuilder();
            for (String message : messages) {
                output.append(message).append("\n");
            }
            out.write(output.toString());

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

    public void writeLog(ArrayList<String> newMessages) {
        Log.i("Info", "writeLog");

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        String time = sdf.format(cal.getTime());

        String oldMessages[] = readLog(true);

        OutputStreamWriter out = null;
        try {
            out = new OutputStreamWriter(this.openFileOutput(LOG_MESSAGES_PATH, Context.MODE_PRIVATE));

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

        NotificationChannel mChannel = new NotificationChannel(id, "Gymbildschirm Channel", NotificationManager.IMPORTANCE_DEFAULT);
        mChannel.enableLights(true);

        notificationManager.createNotificationChannel(mChannel);

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), id)
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

    private static class WebsiteFetcher extends AsyncTask<String, Void, String> {
        String sourceCode;
        WeakReference<Context> contextRef;

        WebsiteFetcher(Context mContext) {
            contextRef = new WeakReference<>(mContext);
        }

        @Override
        protected String doInBackground(String... params) {

            if (isOnline()) {

                try {
                    sourceCode = getSourceCode();
                } catch (IOException e) {

                    e.printStackTrace();
                }
                return sourceCode;
            } else {
                Log.e("GymBildschirm", "Keine Internetverbindung");
                return null;
            }
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

        private String getSourceCode() throws IOException {
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
            if (hasWebsiteError(resString)) {
                return null;
            }
            return resString;
        }

        boolean isOnline() {
            ConnectivityManager cm = (ConnectivityManager) contextRef.get().getSystemService(Context.CONNECTIVITY_SERVICE);
            return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
        }

        private boolean hasWebsiteError(String code) {

            if (code.contains("<title>Error</title>")) {
                Log.e("GymBildschirm", "Website Error!");
                return true;
            }

            return false;
        }
    }

}