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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/**
 * Created by Andi on 12.08.2016.
 */
public class MyService extends JobIntentService {

    private final String CURRENT_MESSAGES_PATH = "Old_Messages.txt";
    private final String LOG_MESSAGES_PATH = "Log_Messages.txt";

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

        try {
            String out = new WebsiteFetcher(this).execute("").get();

            if (Objects.equals(intent.getAction(), "1")) {
                String[][] daten = getDaten(out);
                ArrayList<String> currentMessages = new ArrayList<>();
                if (daten != null) {
                    currentMessages = checkForNew(daten);
                } else {
                    String[] messages = readFile(CURRENT_MESSAGES_PATH);
                    for (String message : messages) {
                        currentMessages.add(arrayToMessage(csvToArray(message)));
                    }
                }

                Bundle bundle = new Bundle();
                bundle.putStringArrayList("messages", currentMessages);
                bundle.putStringArrayList("logs", readLog());
                ServiceObserver.getInstance().addMessagesToTab(bundle);
            } else {
                String[][] daten = getDaten(out);
                if (daten != null) {
                    checkForNew(daten);
                }
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onDestroy();
    }

    private String[][] getDaten(String sourceCode) {

        if (sourceCode == null) {
            return null;
        }

        String[][] entries;

        sourceCode = sourceCode.replaceFirst("(?s).*?<tr>.*?<.*?>", "")
                .replaceFirst("(?s)\\R</table>.*", "");

        String[] rawEntries = sourceCode.split("(?s)<tr>.*?<.*?>");

        int entryCount = rawEntries.length;

        entries = new String[entryCount][];
        for (int i = 0; i < entryCount; i++) {
            entries[i] = rawEntries[i].split("<.*>");
        }

        return entries;

    }

    private ArrayList<String> checkForNew(String[][] daten) {

        String[] csvEntries = new String[daten.length];
        ArrayList<String> oldMessages = new ArrayList<>(Arrays.asList(readFile(CURRENT_MESSAGES_PATH)));

        ArrayList<String> newMessages = new ArrayList<>();
        ArrayList<String> currentMessages = new ArrayList<>();

        for (int i = 0; i < daten.length; i++) {
            csvEntries[i] = (daten[i][0] + ";" + daten[i][1] + ";" + daten[i][2] + ";" + daten[i][3] + ";" + daten[i][4] + ";"
                    + daten[i][5]).replaceAll("\\R", "");
        }

        writeFile(CURRENT_MESSAGES_PATH, csvEntries);

        for (String csvEntry : csvEntries) {

            currentMessages.add(arrayToMessage(csvToArray(csvEntry)));
            if (!oldMessages.contains(csvEntry)) {
                newMessages.add(csvEntry);
                showNotification(csvEntry.split(";"));
            }
        }

        if (!newMessages.isEmpty()) {
            appendToLogFile(newMessages);
        }

        return currentMessages;
    }

    private String[] csvToArray(String input) {
        input = input.trim();
        input = input.replaceAll("\\R", "");

        return input.split(";");
    }

    private String arrayToMessage(String[] fields) {

        String datum = getFormattedDate(fields[0], fields[1]);
        String zeit = getFormattedTime(fields[2], fields[3]);
        String betreff = "";

        if (!fields[4].isEmpty()) {
            betreff = "<b>BETREFF:</b> " + fields[4];
        }

        return "<html>" + betreff + "<br>" + datum + "<br>" + zeit + "<br>" + fields[5] + "</html>";
    }

    String getFormattedTime(String time1, String time2) {
        time1 = time1.trim();
        time2 = time2.trim();

        String zeit = "";
        if (!time1.isEmpty() && !time2.isEmpty()) {
            zeit = "<b>ZEIT:</b> Von " + time1 + " bis " + time2;
        } else if (!time1.isEmpty()) {
            zeit = "<b>ZEIT:</b> Um " + time1;
        } else if (!time2.isEmpty()) {
            zeit = "<b>ZEIT:</b> Bis um " + time2;
        }
        return zeit;
    }

    String getFormattedDate(String date1, String date2) {
        date1 = date1.trim();
        date2 = date2.trim();

        String datum = "";
        if (!date1.isEmpty() && !date2.isEmpty()) {
            datum = "<b>DATUM:</b> Vom " + date1 + " bis zum " + date2 + " ";
        } else if (!date1.isEmpty()) {
            datum = "<b>DATUM:</b> Am " + date1 + " ";
        } else if (!date2.isEmpty()) {
            datum = "<b>DATUM:</b> Bis am " + date2 + " ";
        }
        return datum;
    }

    private String[] readFile(String path) {
        String[] output = new String[0];
        ArrayList<String> messagesList = new ArrayList<>();

        try (
                InputStream inputStream = openFileInput(path);
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader)
        ) {

            String curLine;
            while ((curLine = bufferedReader.readLine()) != null) {
                messagesList.add(curLine);
            }

            output = new String[messagesList.size()];
            for (int i = 0; i < messagesList.size(); i++) {
                output[i] = messagesList.get(i);
            }

        } catch (FileNotFoundException e) {
            Log.e("Error", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("Error", "Can not read file: " + e.toString());
        }

        return output;
    }

    private ArrayList<String> readLog() {

        String[] entries = readFile(LOG_MESSAGES_PATH);

        ArrayList<String> logList = new ArrayList<>();

        for (String entry : entries) {
            logList.add("<html><b>EMPFANGEN: </b>" + entry.split("\\|")[0] + "<br></html>"
                    + arrayToMessage(csvToArray(entry.replaceFirst(".*\\|", ""))));
        }

        return logList;
    }

    private void writeFile(String path, String[] messages) {
        OutputStreamWriter out = null;
        try {
            out = new OutputStreamWriter(openFileOutput(path, Context.MODE_PRIVATE));

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

    private void appendToLogFile(ArrayList<String> newMessages) {

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        String time = sdf.format(cal.getTime());

        for (int i = 0; i < newMessages.size(); i++) {
            newMessages.set(i, time + "|" + newMessages.get(i));
        }

        ArrayList<String> allLogs = new ArrayList<>(newMessages);
        allLogs.addAll(Arrays.asList(readFile(LOG_MESSAGES_PATH)));

        writeFile(LOG_MESSAGES_PATH, allLogs.toArray(new String[0]));
    }


    private String[] formatNotification(String[] daten) {

        String datum = getFormattedDate(daten[0], daten[1]).replaceAll("</?b>", "");
        String zeit = getFormattedTime(daten[2], daten[3]).replaceAll("</?b>", "");

        return new String[]{daten[4], datum + "\n" + zeit + "\n" + daten[5]};
    }

    private void showNotification(String[] daten) {
        Intent notificationIntent = new Intent(this, MyService.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        String id = "gymbildschirm_channel_1";

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationChannel mChannel = new NotificationChannel(id, "Gymbildschirm Channel", NotificationManager.IMPORTANCE_DEFAULT);
        mChannel.enableLights(true);

        notificationManager.createNotificationChannel(mChannel);

        String[] messageParts = formatNotification(daten);

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), id)
                .setTicker("Neue Nachricht")
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle(messageParts[0])
                .setContentIntent(pi)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageParts[1]))
                .setAutoCancel(true)
                .build();


        Random random = new Random();
        int m = random.nextInt(9999 - 1000) + 1000;
        notificationManager.notify(m, notification);
    }

    private static class WebsiteFetcher extends AsyncTask<String, Void, String> {
        final WeakReference<Context> contextRef;
        String sourceCode;

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