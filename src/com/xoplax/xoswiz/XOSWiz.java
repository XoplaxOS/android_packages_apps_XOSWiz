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

package com.xoplax.xoswiz;

import android.app.Application;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;

import com.xoplax.xoswiz.util.XOSWizUtils;

public class InsertCoins extends Application {

    public static final String TAG = "XOSWiz";
    // Leave this off for release
    public static final boolean DEBUG = false;

    public static final String ACCOUNT_TYPE_GOOGLE = "com.google";

    public static final String ACTION_SETUP_WIFI = "com.android.net.wifi.SETUP_WIFI_NETWORK";

    public static final String EXTRA_FIRST_RUN = "firstRun";
    public static final String EXTRA_ALLOW_SKIP = "allowSkip";
    public static final String EXTRA_SHOW_SKIP = "extra_prefs_show_button_bar";
    public static final String EXTRA_AUTO_FINISH = "wifi_auto_finish_on_connect";
    public static final String EXTRA_SHOW_BUTTON_BAR = "extra_prefs_show_button_bar";
    public static final String EXTRA_PREF_BACK_TEXT = "extra_prefs_set_back_text";
    public static final String EXTRA_ONLY_ACCESS_POINTS = "only_access_points";

    public static final String BACKOFF_MS = "backoff_ms";
    public static final int DEFAULT_BACKOFF_MS = 3000;
    public static final int MAX_BACKOFF_MS = 1000 * 60 * 60 * 6; // 6 hours
    public static final String MINIMUM_APP_VERSION = "minimum_app_version";

    public static final int REQUEST_CODE_SETUP_WIFI = 0;
    public static final int REQUEST_CODE_SETUP_CMAccount = 1;

    public static final String SETTINGS_PREFERENCES = "com.xoplax.account_preferences";

    public static final int NOTIFICATION_ID_PASSWORD_RESET = 666;
    public static final int NOTIFICATION_ID_INCOMPATIBLE_VERSION = 667;

    private StatusBarManager mStatusBarManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mStatusBarManager = (StatusBarManager)getSystemService(Context.STATUS_BAR_SERVICE);
    }

    public void disableStatusBar() {
        mStatusBarManager.disable(StatusBarManager.DISABLE_EXPAND | StatusBarManager.DISABLE_NOTIFICATION_ALERTS
                | StatusBarManager.DISABLE_NOTIFICATION_TICKER | StatusBarManager.DISABLE_RECENT | StatusBarManager.DISABLE_HOME
                | StatusBarManager.DISABLE_SEARCH);
    }

    public void enableStatusBar() {
        mStatusBarManager.disable(StatusBarManager.DISABLE_NONE);
    }

}
