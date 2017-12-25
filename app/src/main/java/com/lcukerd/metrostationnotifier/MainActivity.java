package com.lcukerd.metrostationnotifier;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Programmer on 21-12-2017.
 */

public class MainActivity extends AppCompatActivity {

    private List<String> permissionsList = new ArrayList<>();
    private static final String tag = MainActivity.class.getSimpleName();
    private SharedPreferences preferences;
    private TextView currStation;
    private Spinner stationListSpinner;
    private Switch aSwitch;
    private Activity activity;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private String currStationKey = "CurrStation", notifyStationKey = "NotifyStation", notifyKey = "notify";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        checkPermission();

        currStation = findViewById(R.id.currStation);
        currStation.setText(preferences.getString(currStationKey, "No station detected"));
        stationListSpinner = findViewById(R.id.stationList);
        stationListSpinner.setSelection(preferences.getInt(notifyStationKey, 0));
        stationListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                preferences.edit().putInt(notifyStationKey, i).commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        aSwitch = findViewById(R.id.autoStart);
        aSwitch.setChecked(preferences.getBoolean(notifyKey, false));

        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                preferences.edit().putBoolean(notifyKey, b).commit();
            }
        });

        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                  String key) {
                if (key.equals(currStationKey))
                    currStation.setText(preferences.getString(currStationKey, "No station detected"));
                if (key.equals(notifyKey)) {
                    if (preferences.getBoolean(notifyKey, false))
                        startService(new Intent(activity, WifiReceiverService.class));
                    else
                        stopService(new Intent(activity, WifiReceiverService.class));
                }
                Log.d(tag, "Preference Changed" + key);
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    private void checkPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (permissionsList.size() > 0) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setCancelable(false);
            alertBuilder.setTitle("Location Permission");
            alertBuilder.setMessage("App does not use location but wifi. " +
                    "Android does not let app use wifi unless location permission is accepted.");
            alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(activity,
                            permissionsList.toArray(new String[permissionsList.size()]), 1);
                }
            });
            AlertDialog alert = alertBuilder.create();
            alert.show();
        } else if (preferences.getBoolean(notifyKey, false))
            startService(new Intent(this, WifiReceiverService.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (preferences.getBoolean(notifyKey, false))
                        startService(new Intent(this, WifiReceiverService.class));
                } else {
                    Toast.makeText(this, "App Cannot run without permission", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }

    }
}
