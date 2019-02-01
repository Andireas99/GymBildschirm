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

    private static String[] formatToParts(String input) {
        input = input.trim();
        input = input.replaceAll("\\n", "").replaceAll("\\r", "");
        String[] daten = input.split(";");
        String output[] = new String[2];
        String datum = "";
        String zeit = "";

        if (!daten[0].isEmpty() && !daten[1].isEmpty()) {
            datum = "DATUM: Vom " + daten[0] + " bis zum " + daten[1] + " ";
        } else if (!daten[0].isEmpty()) {
            datum = "DATUM: Am " + daten[0] + " ";
        } else if (daten[1].isEmpty()) {
            datum = "DATUM: Bis am " + daten[1] + " ";
        }

        if (!daten[2].isEmpty() && !daten[3].isEmpty()) {
            zeit = "ZEIT: Von " + daten[2] + " bis " + daten[3] + " ";
        } else if (!daten[2].isEmpty()) {
            zeit = "ZEIT: Um " + daten[2] + " ";
        } else if (!daten[3].isEmpty()) {
            zeit = "ZEIT: Bis um " + daten[3] + " ";
        }

        output[0] = daten[4];
        output[1] = datum + "\r\n" + zeit + "\r\n" + daten[5];
        return output;
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
                    String[] messages = readOldMessages();
                    for (String message : messages) {
                        currentMessages.add(getMessageString(message));
                    }
                }

                Bundle bundle = new Bundle();
                bundle.putStringArrayList("messages", currentMessages);
                bundle.putStringArrayList("logs", readLog(false));
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

        //Log.i("Gymbildschirm", "rawEntries: "+Arrays.toString(rawEntries));

        int entryCount = rawEntries.length;

        entries = new String[entryCount][];
        for (int i = 0; i < entryCount; i++) {
            entries[i] = rawEntries[i].split("<.*>");
            //Log.i("Gymbildschirm", "entries "+i+": "+Arrays.toString(entries[i]));
        }

        return entries;

    }

    private ArrayList<String> checkForNew(String[][] daten) {
        String[] oldMessages = readOldMessages();
        String[] messages = new String[daten.length];

        ArrayList<String> newMessages = new ArrayList<>();
        ArrayList<String> messagesOut = new ArrayList<>();
        boolean newMessage = true;

        for (int i = 0; i < daten.length; i++) {
            messages[i] = (daten[i][0] + ";" + daten[i][1] + ";" + daten[i][2] + ";" + daten[i][3] + ";" + daten[i][4] + ";"
                    + daten[i][5]).replaceAll("\\R", "");
        }

        writeFile(CURRENT_MESSAGES_PATH, messages);

        for (int i = 0; i < daten.length; i++) {
            for (String oldMessage : oldMessages) {
                if (oldMessage.equals(messages[i])) {
                    newMessage = false;
                    break;
                }
            }

            if (newMessage) {
                newMessages.add(messages[i]);
            }
            newMessage = true;

        }

        for (String message : messages) {
            messagesOut.add(getMessageString(message));
        }

        if (newMessages.size() > 0) {
            for (int i = 0; i < newMessages.size(); i++) {
                showNotification(newMessages.get(i));
            }
            appendToLogFile(newMessages);
        }

        return messagesOut;
    }

    private String getMessageString(String input) {

        input = input.trim();
        input = input.replaceAll("\\n", "").replaceAll("\\r", "");
        String[] daten = input.split(";");
        String output;
        String datum = "";
        String zeit = "";
        String betreff = "";

        for (int i = 0; i < daten.length; i++) {
            daten[i] = daten[i].trim();
        }

        //Log.i("Info", "input: " + input);

        if (!daten[4].isEmpty()) {
            betreff = "<b>BETREFF:</b> " + daten[4];
        }

        if (!daten[0].isEmpty() && !daten[1].isEmpty()) {
            datum = "<b>DATUM:</b> Vom " + daten[0] + " bis zum " + daten[1] + " ";
        } else if (!daten[0].isEmpty()) {
            datum = "<b>DATUM:</b> Am " + daten[0] + " ";
        } else if (!daten[1].isEmpty()) {
            datum = "<b>DATUM:</b> Bis am " + daten[1] + " ";
        }

        if (!daten[2].isEmpty() && !daten[3].isEmpty()) {
            zeit = "<b>ZEIT:</b> Von " + daten[2] + " bis " + daten[3] + " ";
        } else if (!daten[2].isEmpty()) {
            zeit = "<b>ZEIT:</b> Um " + daten[2] + " ";
        } else if (!daten[3].isEmpty()) {
            zeit = "<b>ZEIT:</b> Bis um " + daten[3] + " ";
        }

        if (daten.length < 6) {
            output = "<html>" + betreff + "<br>" + datum + "<br>" + zeit + "</html>";
        } else {
            output = "<html>" + betreff + "<br>" + datum + "<br>" + zeit + "<br>" + daten[5] + "</html>";
        }

        return output;
    }

    private String[] readOldMessages() {
        String[] output = null;
        ArrayList<String> messagesList = new ArrayList<>();

        try {
            InputStream inputStream = openFileInput(CURRENT_MESSAGES_PATH);

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
            }
        } catch (FileNotFoundException e) {
            Log.e("Error", "File not found: " + e.toString());
            output = new String[]{};
        } catch (IOException e) {
            Log.e("Error", "Can not read file: " + e.toString());
        }

        return output;
    }

    private ArrayList<String> readLog(boolean returnPlain) {

        ArrayList<String> logList = new ArrayList<>();

        try {
            InputStream inputStream = openFileInput(LOG_MESSAGES_PATH);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;

                if (returnPlain) {
                    ArrayList<String> plainList = new ArrayList<>();
                    while ((receiveString = bufferedReader.readLine()) != null) {
                        plainList.add(receiveString.replaceAll("&nbsp ", ""));
                    }
                    return plainList;
                }

                while ((receiveString = bufferedReader.readLine()) != null) {
                    if (receiveString.split("\\|").length == 2) {
                        logList.add("<html><b>Empfangen: </b>" + receiveString.split("\\|")[0] + "<br></html>" + getMessageString(receiveString.split("\\|")[1]));
                    } else {
                        logList.add("<html><b>ERROR</b><br></html>" + receiveString);
                        Log.e("Error", "receiveString: " + receiveString);
                    }

                }
                if (logList.size() == 0) {
                    Log.e("Error", "Kein Log Inhalt");
                    return new ArrayList<>();
                }

                inputStream.close();

            } else {
                return new ArrayList<>();
            }
        } catch (FileNotFoundException e) {
            Log.e("Error", "File not found: " + e.toString());
            logList = new ArrayList<>();
        } catch (IOException e) {
            Log.e("Error", "Can not read file: " + e.toString());
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
            newMessages.set(i, time + " |" + newMessages.get(i));
        }

        ArrayList<String> allLogs = new ArrayList<>(newMessages);
        allLogs.addAll(readLog(true));

        writeFile(LOG_MESSAGES_PATH, allLogs.toArray(new String[0]));
    }

    private void showNotification(String message) {
        Intent notificationIntent = new Intent(this, MyService.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        String id = "gymbildschirm_channel_1";

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationChannel mChannel = new NotificationChannel(id, "Gymbildschirm Channel", NotificationManager.IMPORTANCE_DEFAULT);
        mChannel.enableLights(true);

        notificationManager.createNotificationChannel(mChannel);

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), id)
                .setTicker("Neue Nachricht")
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