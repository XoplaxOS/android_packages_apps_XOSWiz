/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xoplax.xoswiz.util;

import android.os.Bundle;
import android.os.UserManager;
import com.xoplax.xoswiz.XOSWiz;
import com.xoplax.xoswiz.R;
import com.xoplax.xoswiz.ui.WebViewDialogFragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.spongycastle.math.ec.ECFieldElement;

import java.math.BigInteger;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;

public class XOSWizUtils {

    private static final String TAG = XOSWizUtils.class.getSimpleName();
    private static final Random sRandom = new Random();
    private static final Long INTERVAL_WEEK = 604800000L;

    public static final Pattern EMAIL_ADDRESS
            = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[A-Za-z]{2,6}" +
                    ")+"
    );

    private static final String KEY_UDID = "udid";

    private XOSWizUtils(){}

    public static void resetBackoff(SharedPreferences prefs) {
        if (InsertCoins.DEBUG) Log.d(TAG, "Resetting backoff");
        setBackoff(prefs, InsertCoins.DEFAULT_BACKOFF_MS);
    }

    private static int getBackoff(SharedPreferences prefs) {
        return prefs.getInt(InsertCoins.BACKOFF_MS, InsertCoins.DEFAULT_BACKOFF_MS);
    }

    private static void setBackoff(SharedPreferences prefs, int backoff) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(InsertCoins.BACKOFF_MS, backoff);
        editor.commit();
    }

    public static void scheduleRetry(Context context, SharedPreferences prefs, Intent intent) {
        int backoffTimeMs = getBackoff(prefs);
        int nextAttempt = backoffTimeMs / 2 + sRandom.nextInt(backoffTimeMs);
        if (InsertCoins.DEBUG) Log.d(TAG, "Scheduling retry, backoff = " +
                nextAttempt + " (" + backoffTimeMs + ") for " + intent.getAction());
        PendingIntent retryPendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + nextAttempt,
                retryPendingIntent);
        if (backoffTimeMs < InsertCoins.MAX_BACKOFF_MS) {
            setBackoff(prefs, backoffTimeMs * 2);
        }
    }

    public static void showNotification(Context context, int id, Notification notification) {
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

    public static void hideNotification(Context context, int id) {
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }

    public static void tryEnablingWifi(Context context) {
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
    }

    private static Intent getWifiSetupIntent(Context context) {
        Intent intent = new Intent(InsertCoins.ACTION_SETUP_WIFI);
        intent.putExtra(InsertCoins.EXTRA_FIRST_RUN, true);
        intent.putExtra(InsertCoins.EXTRA_ALLOW_SKIP, true);
        intent.putExtra(InsertCoins.EXTRA_SHOW_BUTTON_BAR, true);
        intent.putExtra(InsertCoins.EXTRA_ONLY_ACCESS_POINTS, true);
        intent.putExtra(InsertCoins.EXTRA_SHOW_SKIP, true);
        intent.putExtra(InsertCoins.EXTRA_AUTO_FINISH, true);
        intent.putExtra(InsertCoins.EXTRA_PREF_BACK_TEXT, context.getString(R.string.skip));
        return intent;
    }

    public static void launchWifiSetup(Activity context) {
        XOSWizUtils.tryEnablingWifi(context);
        Intent intent = getWifiSetupIntent(context);
        context.startActivityForResult(intent, InsertCoins.REQUEST_CODE_SETUP_WIFI);
    }

    public static void launchWifiSetup(Fragment fragment) {
        final Context context = fragment.getActivity();
        XOSWizUtils.tryEnablingWifi(context);
        Intent intent = getWifiSetupIntent(context);
        fragment.startActivityForResult(intent, InsertCoins.REQUEST_CODE_SETUP_WIFI);
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi != null && mWifi.isConnected();
    }

    public static boolean isGSMPhone(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int phoneType = telephonyManager.getPhoneType();
        return phoneType == TelephonyManager.PHONE_TYPE_GSM;
    }

    public static boolean isSimMissing(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telephonyManager.getSimState();
        return simState == TelephonyManager.SIM_STATE_ABSENT || simState == TelephonyManager.SIM_STATE_UNKNOWN;
    }

    public static boolean isUnableToModifyAccounts(Context context) {
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        Bundle restrictions = um.getUserRestrictions();
        return restrictions.getBoolean(UserManager.DISALLOW_MODIFY_ACCOUNTS, false);
    }

    public static String getDisplayVersion() {
        return SystemProperties.get("ro.cm.display.version");
    }

    public static String getUniqueDeviceId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(InsertCoins.SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        String udid = prefs.getString(KEY_UDID, null);
        if (udid != null) return udid;
        String wifiInterface = SystemProperties.get("wifi.interface");
        if (wifiInterface != null) {
            try {
                List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface networkInterface : interfaces) {
                    if (wifiInterface.equals(networkInterface.getDisplayName())) {
                        byte[] mac = networkInterface.getHardwareAddress();
                        if (mac != null) {
                            StringBuilder buf = new StringBuilder();
                            for (int i=0; i < mac.length; i++)
                                buf.append(String.format("%02X:", mac[i]));
                            if (buf.length()>0) buf.deleteCharAt(buf.length()-1);
                            if (InsertCoins.DEBUG) Log.d(TAG, "using wifi mac for id : " + buf.toString());
                            return digest(prefs, context.getPackageName() + buf.toString());
                        }
                    }

                }
            } catch (SocketException e) {
                Log.e(TAG, "Unable to get wifi mac address", e);
            }
        }
        //If we fail, just use android id.
        return digest(prefs, context.getPackageName() + Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
    }


    private static String digest(SharedPreferences prefs, String input) {
        try {
            String id = digest("MD5", input);
            prefs.edit().putString(KEY_UDID, id).commit();
            return id;
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] digestBytes(String algorithm, byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(bytes);
            return digest;
        } catch (Exception e) {
            return null;
        }
    }

    public static String digest(String algorithm, String id) {
        byte[] digestBytes = digestBytes(algorithm, id.getBytes());
        return encodeHex(digestBytes).toLowerCase().trim();
    }

    public static String encodeHex(byte[] bytes) {
        return new String(Hex.encodeHex(bytes));
    }

    public static String encodeHex(BigInteger integer) {
        return encodeHex(integer.toByteArray());
    }

    public static byte[] decodeHex(String hex) {
        try {
            return Hex.decodeHex(hex.toCharArray());
        } catch (DecoderException e) {
            Log.e(TAG, "Unable to decode hex string", e);
            throw new AssertionError(e);
        }
    }

}
