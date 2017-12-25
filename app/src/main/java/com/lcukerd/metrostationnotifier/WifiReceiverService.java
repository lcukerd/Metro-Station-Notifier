package com.lcukerd.metrostationnotifier;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;

import static android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION;

/**
 * Created by Programmer on 25-12-2017.
 */

public class WifiReceiverService extends Service {
    private final IBinder mBinder = new LocalBinder();
    private static final String tag = WifiReceiverService.class.getSimpleName();
    private WifiFunction wifiFunction = new WifiFunction();
    private resultReciever resultReciever;
    private String[] stationNames, stationBSSID;
    private SharedPreferences preferences;
    private long pauseDuration = 10000;
    private int failCount = 0;
    private String currStationKey = "CurrStation", notifyStationKey = "NotifyStation", notifyKey = "notify";

    private Handler handler = new Handler();
    private final Runnable wifiscanner = new Runnable() {
        @Override
        public void run() {
            wifiFunction.startScan(getBaseContext());
            handler.postDelayed(wifiscanner, pauseDuration);
        }
    };

    @Override
    public void onCreate() {
        Log.d(tag, "onCreate started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(tag, "onStartCommand started");

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        wifiscanner.run();

        IntentFilter filter = new IntentFilter();
        filter.addAction(SCAN_RESULTS_AVAILABLE_ACTION);
        resultReciever = new resultReciever();
        registerReceiver(resultReciever, filter);

        stationNames = getResources().getStringArray(R.array.StationList);
        stationBSSID = getResources().getStringArray(R.array.StationBSSID);

        showNotification(null);

        return START_STICKY;
    }

    void showNotification(String currStation) {

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent
                .getActivity(this, 0, resultIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(currStation == null ? "Not in Metro" : currStation)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(currStation == null ? Notification.PRIORITY_MIN :
                        currStation.equals(stationNames[preferences.getInt(notifyStationKey, 0)]) ?
                                Notification.PRIORITY_MIN : Notification.PRIORITY_MAX)
                .setContentIntent(resultPendingIntent)
                .setSound(currStation == null ? null : currStation.equals(stationNames[preferences.getInt(notifyStationKey, 0)]) ? alarmSound : null)
                .setVibrate(currStation == null ? null : currStation.equals(stationNames[preferences.getInt(notifyStationKey, 0)]) ? new long[]{0, 500} : null)
                .setAutoCancel(false)
                .build();

        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(903, notification);
    }

    void updateCurrStaion(String BSSID) {
        for (int i = 0; i < stationBSSID.length; i++) {
            if (stationBSSID[i].equals(BSSID)) {
                preferences.edit().putString(currStationKey, stationNames[i]).commit();
                showNotification(stationNames[i]);
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(tag, "onDestroy called");
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(903);
        unregisterReceiver(resultReciever);
    }

    public class resultReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Receiver", "started");
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> results = wifiFunction.getListofWifi();
                Log.d(tag, "Wifi Details " + results.size());
                failCount++;
                for (ScanResult result : results) {
                    //if (result.SSID.equals("Lucky-wifi")) {
                        failCount = 0;
                        updateCurrStaion(result.BSSID);
                    //}
                    Log.d(tag, result.BSSID + " " + result.SSID);
                }
                if (failCount > 55)
                {
                    showNotification(null);
                    pauseDuration = 5 * 60 * 1000;
                }
            }
        }
    }

    public class LocalBinder extends Binder {
        WifiReceiverService getService() {
            return WifiReceiverService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
