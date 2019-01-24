package com.bur.andi.gymbildschirm;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

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


@SuppressLint("ParcelCreator")
public class MainActivity extends AppCompatActivity implements ServiceResultReceiver.Receiver, RefreshListener {

    Toolbar toolbar;
    ViewPager pager;
    ViewPagerAdapter viewAdapter;
    SlidingTabLayout tabs;
    CharSequence tabsTitles[] = {"Nachrichten", "Log"};

    private final String CURRENT_MESSAGES_PATH = "Old_Messages.txt";
    private final String LOG_MESSAGES_PATH = "Log_Messages.txt";

    static Context mContext;

    public static ArrayList messagesList;
    public static ArrayList logList;

    private ServiceResultReceiver mServiceResultReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("Info", "OnCreate");

        messagesList = new ArrayList() {
        };
        logList = new ArrayList() {
        };

        toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        viewAdapter = new ViewPagerAdapter(getSupportFragmentManager(), this, tabsTitles, tabsTitles.length);

        pager = findViewById(R.id.pager);
        pager.setAdapter(viewAdapter);

        tabs = findViewById(R.id.tabs);
        tabs.setDistributeEvenly(true);

        tabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return ContextCompat.getColor(getApplicationContext(), R.color.tabsScrollColor);
            }
        });

        tabs.setViewPager(pager);

        mContext = getApplicationContext();

        mServiceResultReceiver = new ServiceResultReceiver(new Handler());
        mServiceResultReceiver.setReceiver(this);

        startBootReceiver();

        MyService.start(getApplicationContext(), mServiceResultReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Log.i("Info", "Einstellungen");
            Toast.makeText(this, "Noch keine Einstellungen", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startBootReceiver() {
        Log.i("Info", "startBootReceiver");

        Intent bootReceiverIntent = new Intent(this, ReceiverBoot.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, bootReceiverIntent, 0);

        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
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

    public void addMessageToTab() {
        logList.clear();
        Collections.addAll(logList, readLog(false));

        viewAdapter.refreshContent();
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

    @Override
    public void refresh() {
        MyService.start(getApplicationContext(), mServiceResultReceiver);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultCode == 1) {
            String[][] daten = getDaten(resultData.getString("code"));
            checkForNew(daten);
            addMessageToTab();
        } else {
            String[][] daten = getDaten(resultData.getString("code"));
            checkForNew(daten);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}