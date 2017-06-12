package com.flicktek.clip;

import android.os.Debug;
import android.support.annotation.IntDef;
import android.util.Log;

import com.flicktek.clip.eventbus.BluetoothStateEvent;
import com.flicktek.clip.eventbus.ConnectedEvent;
import com.flicktek.clip.eventbus.ConnectingEvent;
import com.flicktek.clip.eventbus.DeviceToPhoneEvent;
import com.flicktek.clip.eventbus.DeviceToWearEvent;
import com.flicktek.clip.eventbus.DisconnectedEvent;
import com.flicktek.clip.eventbus.DisconnectingEvent;
import com.flicktek.clip.eventbus.LinkLossEvent;

import org.greenrobot.eventbus.EventBus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;

public class FlicktekManager {
    private static final String TAG = "FlickTek";

    // Singleton
    private static FlicktekManager mInstance = null;
    private boolean developmentBoard;

    public static FlicktekManager getInstance() {
        if (mInstance == null) {
            mInstance = new FlicktekManager();
        }
        return mInstance;
    }

    public final static int DEBUG_DISABLED = 0;
    public final static int DEBUG_ENABLED = 1;
    public final static int DEBUG_CRAZY = 10;

    // Debug levels
    public static int mDebugLevel = DEBUG_DISABLED;

    public final static int GESTURE_NONE = 0;
    public final static int GESTURE_ENTER = 1;
    public final static int GESTURE_HOME = 2;
    public final static int GESTURE_UP = 3;
    public final static int GESTURE_DOWN = 4;
    public final static int GESTURE_BACK = 5;
    public final static int GESTURE_PHYSICAL_BUTTON = 10;


    /* Swipe events*/
    public static final int SWIPE_TOP = 20;
    public static final int SWIPE_BOTTOM = 21;
    public static final int SWIPE_LEFT = 22;
    public static final int SWIPE_RIGHT = 23;

    @IntDef({SWIPE_TOP, SWIPE_BOTTOM, SWIPE_LEFT, SWIPE_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SwipeEvents {
    }

    public final static int STATUS_NONE = 1;
    public final static int STATUS_CONNECTING = 4;
    public final static int STATUS_CONNECTED = 5;
    public final static int STATUS_READY = 6;
    public final static int STATUS_DISCONNECTED = 7;
    public final static int STATUS_DISCONNECTING = 8;

    //---------------- DEVICE -------------------------

    private static String mDeviceName = "";
    private static String mMacAddress = "";
    private static String mFirmwareVersion = "";
    private static String mFirmwareRevision = "";

    //-------------- DEVICE STATE ---------------------
    // First handshake between devices happened
    private boolean mIsHandshakeOk = false;
    private boolean mIsCalibrated = false;
    private boolean mIsCalibrating = false;
    private int mReconnectAttempts = 0;
    private int mLastPing = 0;
    private int mBatteryLevel = 0;

    private boolean mIsCharging = false;

    public int mSamplingRateTicks = 0;

    public void setSamplingRateTicks(int samplingRateTicks) {
        this.mSamplingRateTicks = samplingRateTicks;
    }

    //-------------- CONNECTION -----------------------

    private int mBluetoothState = STATUS_NONE;

    public void setBluetoothState(int bluetoothState) {
        mBluetoothState = bluetoothState;
        EventBus.getDefault().post(new BluetoothStateEvent(bluetoothState));
    }

    private boolean mIsConnected = false;
    private int mStatus = STATUS_NONE;

    // Configures if the menu will go back if you click home
    // Or requires a double input to exit
    public boolean mIsDoubleGestureHomeExit = false;

    public boolean isConnected() {
        return mIsConnected;
    }

    public boolean isConnecting() {
        return (mStatus == STATUS_CONNECTING);
    }

    // Read from settings permanent information
    public void init() {
        mDeviceName = FlicktekSettings.getInstance().getString(FlicktekSettings.DEVICE_CLIP_NAME, "");
        mMacAddress = FlicktekSettings.getInstance().getString(FlicktekSettings.DEVICE_MAC_SELECTED, "");
        mFirmwareVersion = FlicktekSettings.getInstance().getString(FlicktekSettings.FIRMWARE_VERSION, "");
    }

    public FlicktekManager() {
        init();
    }

    public void setChargingState(boolean chargingState) {
        mIsCharging = chargingState;
        String charge = "Not charging";

        if (mIsCharging) {
            charge = "Charging";
            String date = new Date().toString();
            FlicktekSettings.getInstance().putString(FlicktekSettings.LAST_CHARGING_DATE, date);
        }

        FlicktekSettings.getInstance().putString(FlicktekSettings.CHARGING_STATE, charge);
    }

    public boolean isCharging() {
        return mIsCharging;
    }

    public boolean isSameFirmwareVersion() {
        String firmware_version = getFirmwareVersion();
        if (firmware_version == null) {
            FlicktekCommands.getInstance().onQueryVersions();
            return false;
        }

        if (firmware_version.equals(FlicktekCommands.FIRMWARE_APPLICATION_VERSION)) {
            return true;
        }
        return false;
    }

    public void onRelease() {
        Log.v(TAG, "*********** onRelease ***********" + mMacAddress);
        mIsHandshakeOk = false;
        mFirmwareVersion = "";
        mFirmwareRevision = "";
        mReconnectAttempts = 0;
        mLastPing = 0;
        mBatteryLevel = 0;
        mStatus = STATUS_NONE;
        mIsConnected = false;
        mIsCalibrated = false;
    }

    public void onConnecting(String macAddress) {
        mStatus = STATUS_CONNECTING;
        mIsConnected = false;
        setMacAddress(macAddress);
        EventBus.getDefault().post(new ConnectingEvent(macAddress));
    }

    public void setName(String device_name) {
        mDeviceName = device_name;
        FlicktekSettings.getInstance().putString(FlicktekSettings.DEVICE_CLIP_NAME, device_name);
    }

    public void onConnected(String name, String macAddress) {
        if (mIsConnected && macAddress.equals(mMacAddress)) {
            Log.v(TAG, "Already connected to " + macAddress);
            return;
        }

        mStatus = STATUS_CONNECTED;

        mIsConnected = true;
        mIsCalibrating = false;
        mBatteryLevel = 0;
        setMacAddress(macAddress);
        setName(name);
        EventBus.getDefault().post(new ConnectedEvent(name, macAddress));
    }

    public void onDisconnected() {
        FlicktekManager.getInstance().setHandshakeOk(false);
        mStatus = STATUS_DISCONNECTED;
        mIsConnected = false;
        mReconnectAttempts++;
        EventBus.getDefault().post(new DisconnectedEvent());
    }

    public void onLinkloss() {
        EventBus.getDefault().post(new LinkLossEvent());
        onDisconnected();
    }

    public void onDeviceReady() {
        mStatus = STATUS_READY;
        mIsConnected = true;
    }

    public void onDisconnecting() {
        mStatus = STATUS_DISCONNECTING;
        mIsConnected = false;
        EventBus.getDefault().post(new DisconnectingEvent());
    }

    public void sendDeviceMessage(byte[] buf) {

    }

    public boolean isCalibrating() {
        return mIsCalibrating;
    }

    public void setCalibrationMode(boolean calibration) {
        mIsCalibrating = calibration;
    }

    //------------- GESTURES -------------------------

    public static String getGestureString(int gesture) {
        switch (gesture) {
            case GESTURE_ENTER:
                return "ENTER";
            case GESTURE_HOME:
                return "HOME";
            case GESTURE_UP:
                return "UP";
            case GESTURE_DOWN:
                return "DOWN";
            case GESTURE_BACK:
                return "BACK";
        }
        return "NONE";
    }

    public void onShutdown() {
        // Disconnect device?
        FlicktekSettings.getInstance().putString(FlicktekSettings.DEVICE_MAC_SELECTED, "");
        FlicktekCommands.getInstance().vibration_long();

        FlicktekCommands.onDestroy();
        mInstance = null;
    }

    public interface BackMenu {
        void backFragment();
    }

    //------------- SMARTWATCH -------------------------

    public void backMenu(BackMenu mainActivity) {
        if (mainActivity == null) {
            return;
        }
        mainActivity.backFragment();
    }

    //------------- Getters and setters -------------------------

    public void setBatteryLevel(int value) {
        mBatteryLevel = value;
    }

    public int getBatteryLevel() {
        return mBatteryLevel;
    }

    public String getFirmwareVersion() {
        return mFirmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        FlicktekSettings.getInstance().putString(FlicktekSettings.FIRMWARE_VERSION, firmwareVersion);
        this.mFirmwareVersion = firmwareVersion;
    }

    public String getFirmwareRevision() {
        return mFirmwareRevision;
    }

    public void setFirmwareRevision(String firmwareRevision) {
        FlicktekSettings.getInstance().putString(FlicktekSettings.FIRMWARE_REVISION, firmwareRevision);
        this.mFirmwareRevision = firmwareRevision;
    }

    public boolean isHandshakeOk() {
        return mIsHandshakeOk;
    }

    public void setHandshakeOk(boolean isHandshakeOk) {
        this.mIsHandshakeOk = isHandshakeOk;
    }

    public boolean isCalibrated() {
        return mIsCalibrated;
    }

    public void setCalibration(boolean isCalibrated) {
        mIsCalibrated = isCalibrated;
    }

    public int getLastPing() {
        return mLastPing;
    }

    public void setLastPing(int lastPing) {
        mLastPing = lastPing;
    }

    public void setMacAddress(String mac_address) {
        FlicktekSettings.getInstance().putString(FlicktekSettings.DEVICE_MAC_SELECTED, mac_address);
        mMacAddress = mac_address;
    }

    public String getMacAddress() {
        if (mMacAddress == null || mMacAddress.length() == 0)
            init();

        return mMacAddress;
    }

    // Checks if the device is one of our NRF52 or it declares itself as development board
    public boolean isDevelopmentBoard() {
        if (mMacAddress == null)
            return false;

        if (mMacAddress.equals("F4:79:33:74:44:89")) {
            return true;
        }

        if (Debug.isDebuggerConnected()) {
            return true;
        }

        return false;
    }

    //------------- Notification system -------------------------

    //------------- Device roaming -------------------------

    public void sendClipToWear() {
        Log.v(TAG, "sendClipToWear");

        // Make sure we have the right data from settings
        init();
        FlicktekSettings.getInstance().putString(
                FlicktekSettings.CONNECT_DEVICE_KEY,
                FlicktekSettings.CONNECT_DEVICE_WEAR);

        EventBus.getDefault().post(new DeviceToWearEvent(mMacAddress, mDeviceName));
    }

    public void sendClipToPhone() {
        Log.v(TAG, "sendClipToPhone");
        init();
        FlicktekSettings.getInstance().putString(
                FlicktekSettings.CONNECT_DEVICE_KEY,
                FlicktekSettings.CONNECT_DEVICE_PHONE);

        EventBus.getDefault().post(new DeviceToPhoneEvent(mMacAddress, mDeviceName));
    }
}
