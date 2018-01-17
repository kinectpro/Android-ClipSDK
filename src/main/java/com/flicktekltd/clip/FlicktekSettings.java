package com.flicktekltd.clip;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

public class FlicktekSettings {
    private final String TAG = "FlicktekSettings";

    // Singleton
    private static FlicktekSettings mInstance = null;
    private SharedPreferences mPreferences;

    public static final String APPLICATION_VERSION = "application_version";
    public static final String APPLICATION_REVISION = "application_revision";
    public static final String APPLICATION_VERSION_CODE = "application_code";

    public static final String FIRMWARE_VERSION = "firmware_version";
    public static final String FIRMWARE_REVISION = "firmware_revision";

    // General diagnosis information on the about
    public static final String VERSION_BASE_OS = "version_base_os";
    public static final String VERSION_CODENAME = "version_codename";
    public static final String VERSION_PRODUCT = "version_product";
    public static final String VERSION_DEBUG = "version_debugging";

    // Device to connect automatically
    public static final String DEVICE_MAC_SELECTED = "mac_address_device";
    public static final String DEVICE_CLIP_NAME = "device_clip_name";

    // Is this the first time we install the device?
    public static final String FIRST_INSTALL = "first_install";

    // Implementation of roaming, the application can tell the wear to disconnect
    // and connect to the phone

    public static final String CONNECT_DEVICE_KEY = "connect_device";
    public static final String CONNECT_DEVICE_PHONE = "connect_phone";
    public static final String CONNECT_DEVICE_WEAR = "connect_wear";

    // Upload gestures
    public static final String UPLOAD_GESTURES_WEAR = "gestures_wear_upload";
    public static final String UPLOAD_GESTURES_APP = "gestures_app_upload";

    private boolean mIsWatchConnectedToPhone;

    public static final String PORTRAIT_LOCK_ORIENTATION = "orientation_lock_portrait";
    public static final String CHARGING_STATE = "charging";
    public static final String LAST_CHARGING_DATE = "last_charge_date";

    public boolean isDemo() {
        return false;
    }

    // Settings
    public static final String SETTINGS_DISABLE_VIBRATION_FEEDBACK = "DISABLE_VIBRATION";

    private boolean mIsCallingFromWatch = true;

    public static FlicktekSettings getInstance() {
        if (mInstance == null)
            mInstance = new FlicktekSettings();
        return mInstance;
    }

    public void saveSettingsInformation(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);

            putString(FlicktekSettings.APPLICATION_VERSION, packageInfo.versionName);
            putInt(FlicktekSettings.APPLICATION_VERSION_CODE, packageInfo.versionCode);

            if (!Debug.isDebuggerConnected()) {
                putString(FlicktekSettings.VERSION_DEBUG, "Debugging");
            } else {
                putString(FlicktekSettings.VERSION_DEBUG, "Not debugging");
            }

            putString(FlicktekSettings.VERSION_PRODUCT, Build.PRODUCT);
            putString(FlicktekSettings.VERSION_CODENAME, Build.VERSION.CODENAME);

            //putString(FlicktekSettings.VERSION_BASE_OS, Build.VERSION.BASE_OS);
        } catch (PackageManager.NameNotFoundException e) {
            //Handle exception
        } catch (Exception e) {

        }
    }

    // type can be FlicktekSettings.CONNECT_DEVICE_WEAR or FlicktekSettings.CONNECT_DEVICE_PHONE
    public boolean isAutoConnect(String type) {
        String macSelected = FlicktekSettings.getInstance().getString(FlicktekSettings.DEVICE_MAC_SELECTED, null);
        if (macSelected == null || macSelected.length() == 0)
            return false;

        String connect = FlicktekSettings.getInstance().getString(FlicktekSettings.CONNECT_DEVICE_KEY, CONNECT_DEVICE_WEAR);
        if (connect == null || connect.length() == 0)
            return false;

        if (connect.equals(type)) {
            return true;
        }

        return false;
    }

    public void setPreferencesActivity(Context context) {
        boolean first_launch = (mPreferences == null);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (first_launch) {
            saveSettingsInformation(context);
        }
    }

    public boolean putString(String key, String value) {
        if (mPreferences == null)
            return false;

        String old_value = mPreferences.getString(key, null);
        mPreferences.edit().putString(key, value).apply();

        // Send the message for the wear or application activity to respond
        Log.v(TAG, "Settings onSettingsEvent key=" + key + " value=" + value);
        EventBus.getDefault().post(new onSettingsEvent(key, value, old_value));
        return true;
    }

    public boolean putInt(String key, int value) {
        if (mPreferences == null)
            return false;

        mPreferences.edit().putInt(key, value).apply();
        return true;
    }

    // We check where the clip should be connected into, by default WEAR is our option
    // in case of not knowing where.
    public boolean shouldConnectWearable() {
        String str = getString(FlicktekSettings.CONNECT_DEVICE_KEY, FlicktekSettings.CONNECT_DEVICE_WEAR);
        if (str.equals(FlicktekSettings.CONNECT_DEVICE_WEAR))
            return true;
        return false;
    }

    public boolean shouldConnectPhone() {
        String str = getString(FlicktekSettings.CONNECT_DEVICE_KEY, FlicktekSettings.CONNECT_DEVICE_WEAR);
        if (str.equals(FlicktekSettings.CONNECT_DEVICE_PHONE))
            return true;
        return false;
    }

    @Nullable
    public String getString(String key, String default_value) {
        if (mPreferences == null)
            return default_value;

        return mPreferences.getString(key, default_value);
    }

    @Nullable
    public int getInt(String key, int default_value) {
        if (mPreferences == null)
            return default_value;

        return mPreferences.getInt(key, default_value);
    }

    public boolean isCallingFromWatch() {
        return mIsCallingFromWatch;
    }

    public void setCallingFromWatch(boolean callingFromWatch) {
        mIsCallingFromWatch = callingFromWatch;
    }

    public boolean isWatchConnectedToPhone() {
        return mIsWatchConnectedToPhone;
    }

    public synchronized void setIsWatchConnectedToPhone(boolean mIsWatchConnectedToPhone) {
        this.mIsWatchConnectedToPhone = mIsWatchConnectedToPhone;
    }

    public boolean isInstallationFinished() {
        String str = getString(FlicktekSettings.FIRST_INSTALL, null);
        return (str != null && str.length() > 0);
    }

    public boolean getBoolean(String key, boolean default_value) {
        if (mPreferences == null)
            return default_value;

        return mPreferences.getBoolean(key, default_value);
    }

    public void putBoolean(String key, boolean value) {
        if (mPreferences == null)
            return;

        mPreferences.edit().putBoolean(key, value).apply();
    }

    public class onSettingsEvent {
        public String key;
        public String value;
        public String old_value;

        public onSettingsEvent(String key, String value, String old_value) {
            this.key = key;
            this.value = value;
            this.old_value = old_value;
        }
    }
}
