package com.android.wubydax;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class GearDeviceInfo extends LinearLayout {
    private Context mContext;
    private List<String> mAllKeys, mTitles;
    private String mPackageName;
    private ContentResolver mContentResolver;

    public GearDeviceInfo(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mPackageName = mContext.getPackageName();
        mContentResolver = mContext.getContentResolver();
        int allKeysId = mContext.getResources().getIdentifier("device_info_keys", "array", mPackageName);
        int titlesId = mContext.getResources().getIdentifier("device_info_titles", "array", mPackageName);
        mAllKeys = Arrays.asList(mContext.getResources().getStringArray(allKeysId));
        mTitles = Arrays.asList(mContext.getResources().getStringArray(titlesId));
        setOrientation(VERTICAL);
    }


    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        setUpViews();
    }

    private void setUpViews() {
        KeyguardManager keyguardManager = (KeyguardManager) getContext().getSystemService(Context.KEYGUARD_SERVICE);
        boolean isVisible = Settings.System.getInt(mContentResolver, "device_info_visibility", 1) != 0;
        if (getChildCount() > 0) {
            removeAllViews();
        }
        if (isVisible && !keyguardManager.isKeyguardLocked()) {
            int layoutId = mContext.getResources().getIdentifier("gear_info_item_layout", "layout", mPackageName);
            int nameId = mContext.getResources().getIdentifier("attrName", "id", mPackageName);
            int size = mAllKeys.size();
            for (int i = 0; i < size; i++) {
                boolean isShown = Settings.System.getInt(mContentResolver, mAllKeys.get(i), 1) != 0;

                if (isShown) {
                    View view = LayoutInflater.from(mContext).inflate(layoutId, this, false);
                    addView(view);
                    ((TextView) view.findViewById(nameId)).setText(mTitles.get(i));
                    ((TextView) view.findViewById(nameId)).setTextColor(Settings.System.getInt(mContentResolver, "gear_info_names_color", Color.WHITE));
                    setUpValues(view, mAllKeys.get(i));
                }
            }
        }

    }

    private void setUpValues(View view, String key) {
        String value = "N/A";
        switch (key) {
            case "device_model":
                value = Build.MODEL;
                break;
            case "device_android_version":
                value = Build.VERSION.RELEASE;
                break;
            case "device_build_version":
                value = Build.DISPLAY;
                break;
            case "device_battery_level":
                Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                assert batteryIntent != null;
                int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                value = String.valueOf(level) + "%";
                break;
            case "device_network_name":
                TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
                value = telephonyManager.getNetworkOperatorName();
                break;
            case "device_wifi_info":
                ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    int rssi = wifiInfo.getRssi();
                    int signalStrength = WifiManager.calculateSignalLevel(rssi, 10) * 10;
                    int ip = wifiInfo.getIpAddress();
                    String ipAddress = String.format(Locale.ENGLISH, "%d.%d.%d.%d",
                            (ip & 0xff),
                            (ip >> 8 & 0xff),
                            (ip >> 16 & 0xff),
                            (ip >> 24 & 0xff));
                    value = wifiInfo.getSSID() + ", signal strength is " + String.valueOf(signalStrength) + "%" + ", IP " + ipAddress;
                } else {
                    value = "Wifi disconnected";
                }
                break;
            case "device_next_alarm":
                AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();
                if (alarmClockInfo != null) {
                    long time = alarmClockInfo.getTriggerTime();
                    value = new SimpleDateFormat("EEE, MMM d, HH:mm", Locale.getDefault()).format(time);
                } else {
                    value = "Alarm not set";
                }
                break;
            case "device_up_time":
                long upTime = SystemClock.uptimeMillis();
                value = String.format(Locale.getDefault(), "%02d hrs %02d mins",
                        TimeUnit.MILLISECONDS.toHours(upTime),
                        TimeUnit.MILLISECONDS.toMinutes(upTime) -
                                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(upTime))
                );
                break;
        }
        int valueId = mContext.getResources().getIdentifier("attrValue", "id", mPackageName);
        ((TextView) view.findViewById(valueId)).setText(value);
        ((TextView) view.findViewById(valueId)).setTextColor(Settings.System.getInt(mContentResolver, "gear_info_values_color", Color.WHITE));
    }



}
