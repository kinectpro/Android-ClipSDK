package com.flicktek.clip;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;

import com.flicktek.clip.common.R;
import com.flicktek.clip.wearable.common.Constants;
import com.flicktek.clip.wearable.common.NotificationModel;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.VIBRATOR_SERVICE;
import static android.os.Debug.isDebuggerConnected;
import static com.flicktek.clip.FlicktekManager.GESTURE_DOWN;
import static com.flicktek.clip.FlicktekManager.GESTURE_ENTER;
import static com.flicktek.clip.FlicktekManager.GESTURE_HOME;
import static com.flicktek.clip.FlicktekManager.GESTURE_PHYSICAL_BUTTON;
import static com.flicktek.clip.FlicktekManager.GESTURE_UP;

// extends UARTProfile
public class FlicktekCommands {
    private static String TAG = "FlicktekCommands";

    // Version for which this set of commands was built
    public static final String FIRMWARE_APPLICATION_VERSION = "v310517-0745";

    // Abstract interface to send data into the UART
    // So we can use different BLE profiles to send data
    // and share the code between the application and the wearable.

    public interface UARTInterface {
        public void sendString(final String data);

        public void sendDataBuffer(final byte[] data);
    }

    private UARTInterface mDataChannel = null;

    public void registerDataChannel(UARTInterface data_channel) {
        mDataChannel = data_channel;
    }

    // Time for the device to go to sleep after it gets out of focus
    private static final long ALARM_SLEEP_TIME = 10000;

    // Singleton
    private static FlicktekCommands mInstance = null;

    public static FlicktekCommands getInstance() {
        if (mInstance == null)
            mInstance = new FlicktekCommands();
        return mInstance;
    }

    // Clears the instance so we start fresh
    public static void onDestroy() {
        mInstance = null;
    }

    // Settings command values
    public final static int SET_NUMBER_GESTURE = 0xC0;
    public final static int SET_NUMBER_REPETITION = 0xC1;

    // Calibration attribute present
    public final static int CALIBRATION_IS_PRESENT = 1;
    public final static int CALIBRATION_IS_NOT_PRESENT = 0;

    // Calibration mode values
    public final static int CALIBRATION_MODE_NONE = 0;
    public final static int STATUS_CALIB = 1;
    public final static int STATUS_EXEC = 2;
    public final static int STATUS_SLEEP = 3;
    public final static int STATUS_IDLE = 0;
    public final static int STATUS_PRECALIB_AMP = 4;
    public final static int STATUS_PRECALIB_CAD = 5;
    public final static int STATUS_PRECALIB_SIM = 6;
    public final static int STATUS_PRECALIB_DEB = 7;

    // Gesture status values
    public final static int GESTURE_STATUS_NONE = 0;
    public final static int GESTURE_STATUS_STARTED = 1;
    public final static int GESTURE_STATUS_RECORDING = 2;
    public final static int GESTURE_STATUS_OK = 3;
    public final static int GESTURE_STATUS_ERROR1 = 4;
    public final static int GESTURE_STATUS_ERROR2 = 5;
    public final static int GESTURE_STATUS_ERROR3 = 6;
    public final static int GESTURE_STATUS_OKREPETITION = 7;
    public final static int GESTURE_STATUS_OKGESTURE = 8;
    public final static int GESTURE_STATUS_OKCALIBRATION = 9;
    public final static int GESTURE_STATUS_OKCAMP = 10;
    public final static int GESTURE_STATUS_OKCAD = 11;
    public final static int GESTURE_STATUS_OKCSIM = 12;

    // Command list
    public final static char COMMAND_START = '{';
    public final static char COMMAND_END = '}';

    public final static char REPORT_START = '[';
    public final static char REPORT_END = ']';

    public final static char COMMAND_GESTURE = 'G';
    public final static char COMMAND_CAS_GESTURE_STATUS = 'S';
    public final static char COMMAND_CAS_ERROR = 'E';
    public final static char COMMAND_CAS_GESTURE_QUALITY = 'Q';
    public final static char COMMAND_CAS_GESTURE_FEEDBACK = 'F';
    public final static char COMMAND_CAS_IS_CALIBRATED = 'C';
    public final static char COMMAND_OK = 'O';
    public final static char COMMAND_CHARGING_STATE = 't';

    public final static char COMMAND_CAS_WRITE = 'W';
    public final static char COMMAND_SETTING = 'T';
    public final static char COMMAND_SETTING_DATA = 'D';
    public final static char COMMAND_PING = 'P';
    public final static char COMMAND_DEBUG = 'z';
    public final static char COMMAND_HALT = 'H';
    public final static char COMMAND_VERSION = 'V';

    public final static char COMMAND_SAMPLE_RATIO = 'R';
    public final static char COMMAND_REQUEST_SAMPLE_RATIO = 'r';

    public final static char COMMAND_SENSOR_STREAMING = 's';

    public final static int VERSION_COMPILATION = 0;
    public final static int VERSION_REVISION = 1;

    public final static char COMMAND_ENABLE = 'e';
    public final static char COMMAND_DISABLE = 'd';

    // Calibration
    private int numGestures;
    private int numRepetitions;
    private int calibrationStatus;
    private int gestureStatus;

    // BUG: This is not populated from wherever is supposed to be populated
    private int currentGestureIndex;
    private int currentGestureIteration;

    private boolean mIsApplicationPaused = false;
    private boolean mIsApplicationVisible = false;
    private int mDevice_State = STATUS_IDLE;

    // Currently configured sampling rate
    public int mSamplingRate = 1;
    public int mSamplingRateTicks = 2500;

    public FlicktekCommands() {
        Log.d(TAG, "FlicktekCommands");
    }

    private Context mContext;

    private PendingIntent mAlarmPendingIntent;

    // The application is out of view so we are on paused
    // This will make the service to try to launch the MainActivity intent in case
    // we have a detected gesture and we are not on focus.

    private BroadcastReceiver sleep_receiver = null;

    // Is the application focused on the screen?
    // otherwise we will launch the intent in case something tries to wake us up.
    public void setApplicationFocus(boolean focused) {
        mIsApplicationVisible = focused;
    }

    public boolean getApplicationFocus() {
        return mIsApplicationVisible;
    }

    Vibrator vibrator;

    public void vibration_long() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    vibrator = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);

                    long[] vibrationPattern = {50, 100, 50, 100, 50, 100, 0};
                    //-1 - don't repeat
                    final int indexInPatternToRepeat = -1;
                    vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                } catch (Exception e) {

                }
            }
        }).start();
    }

    public static final int VIBRATION_DEFAULT = 0;
    public static final int VIBRATION_ENTER = GESTURE_ENTER;
    public static final int VIBRATION_HOME = GESTURE_HOME;
    public static final int VIBRATION_UP = GESTURE_UP;
    public static final int VIBRATION_DOWN = GESTURE_DOWN;
    public static final int VIBRATION_BUTTON = VIBRATION_DOWN + 1;
    public static final int VIBRATION_SLEEP = VIBRATION_BUTTON + 1;
    public static final int VIBRATION_EXECUTION = VIBRATION_SLEEP + 1;
    public static final int VIBRATION_LAST = VIBRATION_EXECUTION + 1;

    private long[][] vibrationPatterns = {
            {0, 20, 10, 50},  // VIBRATION_DEFAULT
            {0, 50, 20, 10},  // VIBRATION_ENTER
            {0, 50, 20, 50},  // VIBRATION_HOME
            {0, 20, 15, 40},  // VIBRATION_UP
            {0, 40, 15, 20},  // VIBRATION_DOWN
            {0, 30, 10, 40},  // VIBRATION_BUTTON
            {0, 20, 10, 20},  // VIBRATION_SLEEP
            {0, 40, 10, 10},  // VIBRATION_EXECUTION
            {0, 20, 10, 50}
    };

    public void vibration_patterns(final int pattern) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    vibrator = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);

                    int event = pattern;
                    if (event == GESTURE_PHYSICAL_BUTTON) {
                        event = VIBRATION_BUTTON;
                    }

                    if (event > VIBRATION_LAST)
                        event = VIBRATION_DEFAULT;

                    //-1 - don't repeat
                    final int indexInPatternToRepeat = -1;
                    vibrator.vibrate(vibrationPatterns[event], indexInPatternToRepeat);
                } catch (Exception e) {

                }
            }
        }).start();
    }

    public void setApplicationPaused(boolean applicationPaused) {
        if (mContext == null)
            return;

        AlarmManager alarmManager;
        if (applicationPaused)
            Log.v(TAG, "++++++++++++ APPLICATION PAUSED! ++++++++++++++ " + mDevice_State);
        else
            Log.v(TAG, "++++++++++++ APPLICATION WAKEUP! ++++++++++++++ " + mDevice_State);

        if (applicationPaused) {
            if (mDevice_State == STATUS_SLEEP || mAlarmPendingIntent != null) {
                if (mAlarmPendingIntent != null)
                    Log.v(TAG, "+ Device is going to sleep in a few seconds!");
                else
                    Log.v(TAG, "+ Device is sleeping!");
                mIsApplicationPaused = applicationPaused;
                return;
            }

            if (sleep_receiver == null) {
                sleep_receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context c, Intent i) {
                        Log.v(TAG, "+++++++++++ Sleep receiver!   ++++++++++++");
                        if (!mIsApplicationVisible) {
                            Log.v(TAG, "+ Turn off device");
                            //vibration_patterns(VIBRATION_SLEEP);
                            writeStatus_Sleep();
                        }
                        mAlarmPendingIntent = null;
                    }
                };

                mContext.registerReceiver(sleep_receiver, new IntentFilter("com.flicktek.sleep"));
            }

            Log.v(TAG, "+++++++++++ GO TO SLEEP IN " + ALARM_SLEEP_TIME + "+++++++++++");

            mAlarmPendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent("com.flicktek.sleep"), 0);

            alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_SLEEP_TIME, mAlarmPendingIntent);
        } else {
            if (mAlarmPendingIntent != null && mContext != null) {
                Log.v(TAG, "+ Cancel alarm, we don't want to sleep anymore");
                alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                alarmManager.cancel(mAlarmPendingIntent);
            }
            mAlarmPendingIntent = null;

            if (mDevice_State == STATUS_EXEC) {
                Log.v(TAG, "++++++++++++ ALREADY ON EXECUTION! ++++++++++++++");
            } else {

            }
            writeStatus_Exec();
        }

        mIsApplicationPaused = applicationPaused;
    }

    public void onBatteryValueReceived(final int value) {
        Log.v(TAG, "onBatteryValueReceived " + value);
        FlicktekManager.getInstance().setBatteryLevel(value);
        EventBus.getDefault().post(new onBatteryEvent(value));
    }

    public void init(Context context) {
        Log.d(TAG, "init: ");
        mContext = context;
        numGestures = context.getResources().getInteger(R.integer.calibration_gestures);
        numRepetitions = context.getResources().getInteger(R.integer.calibration_iterations);
    }

    public void onQueryForCalibration() {
        Log.v(TAG, "-------------- IS CALIBRATED -------------------");
        writeSingleCommand(COMMAND_CAS_IS_CALIBRATED, 0);
    }

    public void onQueryForChargingState() {
        writeSingleCommand(COMMAND_CHARGING_STATE, 0);
    }

    public void setCaptureRate(int value) {
        Log.v(TAG, "------- SET SAMPLE RATIO FROM TABLE ------------");
        writeSingleCommand(COMMAND_SAMPLE_RATIO, value);
        mSamplingRate = value;
    }

    public void writeShutdown() {
        Log.v(TAG, "---------- SHUTDOWN -------------");
        writeSingleCommand(COMMAND_HALT, 1);
    }

    public void onReadyToSendData(boolean ready) {
        Log.v(TAG, "onReadyToSendData " + ready);
        Log.v(TAG, "---------- LETS REPORT WE ARE ALIVE------------");
        writeSingleCommand(COMMAND_OK, 1);
    }

    public void onQueryVersions() {
        Log.v(TAG, "------------- REQUEST REVISION ----------------");
        writeSingleCommand(COMMAND_VERSION, VERSION_REVISION);
        Log.v(TAG, "----------- REQUEST COMPILATION ---------------");
        writeSingleCommand(COMMAND_VERSION, VERSION_COMPILATION);

        // Request sampling rate configured
        writeSingleCommand(COMMAND_REQUEST_SAMPLE_RATIO, 1);
    }

    public void onDeviceRespondedToConnection() {
        mDevice_State = STATUS_IDLE;
        onQueryVersions();
        Log.v(TAG, "----------- REQUEST SAMPLING RATE 200HZ ---------------");
        setCaptureRate(1);
    }

    public void onNeedToStartActivityFromNotification(NotificationModel model) {
        // The application hasn't been launched so we don't capture notifications
        if (mContext == null) {
            Log.v(TAG, "### NO CONTEXT FOR NOTIFICATION ###");
            return;
        }
        // If application is not visible then we have to relaunch ourselves and notify that we want
        // to show the notification.
        Log.v(TAG, "########## LAUNCH NOTIFICATION ##########");
        String packageName = mContext.getPackageName();
        Intent launchIntent = mContext.getPackageManager().getLaunchIntentForPackage(packageName);
        launchIntent.putExtra(Constants.FLICKTEK_CLIP.NOTIFICATION_KEY_ID, model.getKeyId());
        mContext.startActivity(launchIntent);
        // Otherwise we let the application to capture the notification and figure out what to do
        // with it.
    }

    public void onGestureChanged(int value) {
        mLastGesture = value;
        if (!mIsApplicationVisible &&
                (value == GESTURE_ENTER || value == GESTURE_PHYSICAL_BUTTON)) {
            Log.v(TAG, "########## RELAUNCH ##########");
            if (mContext != null) {
                Intent LaunchIntent = mContext.getPackageManager().getLaunchIntentForPackage(mContext.getPackageName());
                mContext.startActivity(LaunchIntent);
            } else {
                Log.v(TAG, "!!!!!!!!! NO CONTEXT !!!!!!!!!");
            }

            /*
            final Intent activity = new Intent(mContext, MainActivity.class);
            activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.getApplicationContext().startActivity(activity);
            */
        }

        if (value == FlicktekManager.GESTURE_NONE) {
            EventBus.getDefault().post(new onGestureNotClassified());
        } else {
            EventBus.getDefault().post(new onGestureEvent(value));
            vibration_patterns(value);
        }

        Log.d(TAG, "onGestureChanged: " + value +
                " Paused " + mIsApplicationPaused +
                " Visible " + mIsApplicationVisible);
    }

    //---------- Write commands -----------------------------------------------------

    public void writeSingleCommand(char command, int value) {
        byte buf[] = new byte[4];
        buf[0] = COMMAND_START;
        buf[1] = (byte) command;

        if (value < '0')
            value += '0';

        buf[2] = (byte) value;
        buf[3] = COMMAND_END;

        if (mDataChannel != null) {
            Log.v(TAG, "++++++++++++++++ COMMAND " + new String(buf) + "++++++++++++++++++");
            mDataChannel.sendDataBuffer(buf);
        } else {
            Log.v(TAG, "+++++++++++ COMMAND FAILED " + new String(buf) + "+++++++++++++++++");
        }
    }

    public void writeStatus_Ping() {
        Log.v(TAG, "writeStatus_Ping");
        writeSingleCommand(COMMAND_PING, 1);
    }

    public void writeStatus_Sleep() {
        Log.v(TAG, "writeStatus_Sleep");
        writeSingleCommand(COMMAND_CAS_WRITE, STATUS_SLEEP);
    }

    public void writeStatus_Exec() {
        Log.v(TAG, "writeStatus_Exec");
        writeSingleCommand(COMMAND_CAS_WRITE, STATUS_EXEC);
    }

    public void writeStatus_Calib() {
        Log.v(TAG, "writeStatus_Calib");
        writeSingleCommand(COMMAND_CAS_WRITE, STATUS_CALIB);
    }

    public void writeGestureStatus(int status) {
        Log.v(TAG, "writeGestureStatus " + status);
        writeSingleCommand(COMMAND_CAS_GESTURE_STATUS, status);
    }

    public void writeSettingsCommand(int value) {
        Log.v(TAG, "writeSettingsCommand " + value);
        switch (value) {
            case SET_NUMBER_GESTURE:
                writeSingleCommand(COMMAND_SETTING_DATA, numGestures);
                break;
            case SET_NUMBER_REPETITION:
                writeSingleCommand(COMMAND_SETTING_DATA, numRepetitions);
                break;
        }
        writeSingleCommand(COMMAND_SETTING, value);
    }

    public void writeGestureStatusStart() {
        Log.v(TAG, "writeGestureStatusStart");
        writeSingleCommand(COMMAND_CAS_GESTURE_STATUS, GESTURE_STATUS_STARTED);
    }

    public void writeStatus_Pre_Amp() {
        Log.v(TAG, "writeStatus_Pre_Amp");
        writeSingleCommand(COMMAND_CAS_WRITE, STATUS_PRECALIB_AMP);
    }

    public void writeStatus_Pre_Deb() {
        Log.v(TAG, "writeStatus_Pre_Deb");
        writeSingleCommand(COMMAND_CAS_WRITE, STATUS_PRECALIB_DEB);
    }

    public void writeStatus_Pre_Cad() {
        Log.v(TAG, "writeStatus_Pre_Cad");
        writeSingleCommand(COMMAND_CAS_WRITE, STATUS_PRECALIB_CAD);
    }

    public void writeStatus_Pre_Sim() {
        Log.v(TAG, "writeStatus_Pre_Sim");
        writeSingleCommand(COMMAND_CAS_WRITE, STATUS_PRECALIB_SIM);
    }

    public void setStartSensorCapturing(boolean stream) {
        Log.v(TAG, "setStartSensorCapturing");
        int value = 0;
        if (stream)
            value = 1;
        writeSingleCommand(COMMAND_SENSOR_STREAMING, value);
    }

    //----------------------------------------------------------------------------

    public void setGesture(int value) {
        Log.d(TAG, "setGesture: ");
        setGesturesNumber(value);
        writeSettingsCommand(SET_NUMBER_GESTURE);
    }

    public void setRepetition(Integer value) {
        Log.d(TAG, "setRepetition: " + value.toString());
        setIterationsNumber(value);
        writeSettingsCommand(SET_NUMBER_REPETITION);
    }

    public int getGesturesNumber() {
        Log.d(TAG, "getGesturesNumber: " + Integer.toString(numGestures));
        return numGestures;
    }

    public void setGesturesNumber(int val) {
        Log.d(TAG, "setGesturesNumber: " + Integer.toString(val));
        numGestures = val;
    }

    public int getIterationsNumber() {
        Log.d(TAG, "getIterationsNumber: " + Integer.toString(numRepetitions));
        return numRepetitions;
    }

    public void setIterationsNumber(int val) {
        Log.d(TAG, "setIterationsNumber: " + Integer.toString(val));
        numRepetitions = val;
    }

    public void startCalibration() {
        Log.d(TAG, "startCalibration: ");
        calibrationStatus = CALIBRATION_MODE_NONE;
        gestureStatus = GESTURE_STATUS_NONE;
        currentGestureIndex = 1;
        currentGestureIteration = 1;
        writeStatus_Calib();

        if (isDebuggerConnected()) {
            Log.v(TAG, "--------------- DEBUG ACTIVE -------------------");

            // Report all the prints through the BLE UART channel
            writeSingleCommand(COMMAND_DEBUG, 1);

            // Fake calibration!
            Log.v(TAG, "-----------FAKE CALIBRATION ACTIVE -------------");
            writeSingleCommand(COMMAND_DEBUG, 2);
        } else {
            writeSingleCommand(COMMAND_DEBUG, 0);
        }
    }

    public void nextCalibrationStep() {
        Log.d(TAG, "nextCalibrationStep: ");
        writeGestureStatus(GESTURE_STATUS_STARTED);
    }

    public void onCalibrationModeWritten(int _value) {
        Log.d(TAG, "onCalibrationModeWritten " + _value);
        //This info is only used so far for the calibration therefore I move it to the calibration status only
        calibrationStatus = _value;
        if (_value == STATUS_CALIB) {
            EventBus.getDefault().post(new onCalibrationWritten(_value));
            Log.d(TAG, "onCalibrationModeWritten: calibration mode scritto correttamente " + _value);
        }

        if (_value == STATUS_EXEC) {
            Log.d(TAG, "onCalibrationModeWritten: execution mode scritto correttamente " + _value);
        }
    }

    public void repeatCalibrationStep() {
        Log.d(TAG, "repeatCalibrationStep: EMPTY ");
    }

    public void stopCalibration() {
        Log.d(TAG, "stopCalibration: ");
        //TODO: implement the control on the Calibration_Attribute
        writeStatus_Exec();
    }

    public int getCalibrationStatus() {
        Log.d(TAG, "getCalibrationStatus: ");
        return calibrationStatus;
    }

    public int getGestureStatus() {
        Log.d(TAG, "getGestureStatus: ");
        return gestureStatus;
    }

    public int getGestureIndex() {
        Log.d(TAG, "getGestureIndex: ");
        return currentGestureIndex;
    }

    public int getGestureIteration() {
        Log.d(TAG, "getGestureIteration: ");
        return currentGestureIteration;
    }

    public void onGestureStatusWritten(int _value) {
        Log.d(TAG, "onGestureStatusWritten: " + _value + currentGestureIndex + " Iteration " + currentGestureIteration);
        if (_value == GESTURE_STATUS_STARTED) {
            gestureStatus = GESTURE_STATUS_STARTED;
            EventBus.getDefault().post(new onCalibrationStepStarted(currentGestureIndex, currentGestureIteration));
            return;
        }
    }

    public void onGestureStatusFeedback(int _value) {
        switch (_value) {
            case GESTURE_STATUS_NONE:
                Log.v(TAG, "GESTURE_STATUS_NONE");
                break;
            case GESTURE_STATUS_STARTED:
                Log.v(TAG, "GESTURE_STATUS_STARTED");
                break;
            case GESTURE_STATUS_RECORDING:
                Log.v(TAG, "GESTURE_STATUS_RECORDING");
                break;
            case GESTURE_STATUS_OK:
                Log.v(TAG, "GESTURE_STATUS_OK");
                break;
            case GESTURE_STATUS_ERROR1:
                Log.v(TAG, "GESTURE_STATUS_ERROR1");
                break;
            case GESTURE_STATUS_ERROR2:
                Log.v(TAG, "GESTURE_STATUS_ERROR2");
                break;
            case GESTURE_STATUS_ERROR3:
                Log.v(TAG, "GESTURE_STATUS_ERROR3");
                break;
            case GESTURE_STATUS_OKREPETITION:
                Log.v(TAG, "GESTURE_STATUS_OKREPETITION");
                break;
            case GESTURE_STATUS_OKGESTURE:
                Log.v(TAG, "GESTURE_STATUS_OKGESTURE");
                break;
            case GESTURE_STATUS_OKCALIBRATION:
                Log.v(TAG, "GESTURE_STATUS_OKCALIBRATION");
                break;
            case GESTURE_STATUS_OKCAMP:
                Log.v(TAG, "GESTURE_STATUS_OKCAMP");
                break;
            case GESTURE_STATUS_OKCAD:
                Log.v(TAG, "GESTURE_STATUS_OKCAD");
                break;
            case GESTURE_STATUS_OKCSIM:
                Log.v(TAG, "GESTURE_STATUS_OKCSIM");
                break;
        }
    }

    // A valid response for a command is [ACK:CV] Being C the command and V the data value sent

    public void responseACK(byte cmd, byte number) {
        int value = number - '0';

        switch (cmd) {
            case COMMAND_CAS_IS_CALIBRATED:
                if (value == 0) {
                    Log.v(TAG, "Clip is not calibrated!");
                    FlicktekManager.getInstance().setCalibration(false);
                    EventBus.getDefault().post(new onNotCalibrated());
                } else {
                    FlicktekManager.getInstance().setCalibration(true);
                    writeStatus_Exec();
                }
                break;
            case COMMAND_CAS_WRITE:
                switch (value) {
                    case STATUS_SLEEP:
                        Log.v(TAG, "+ STATUS_SLEEP");
                        // Vibrate when we go to sleep
                        if (FlicktekSettings.getInstance().getInt(
                                FlicktekSettings.SETTINGS_DISABLE_VIBRATION_FEEDBACK, 1) == 0)
                            vibration_patterns(VIBRATION_SLEEP);
                        break;
                    case STATUS_EXEC:
                        Log.v(TAG, "+ STATUS_EXEC");
                        if (FlicktekSettings.getInstance().getInt(
                                FlicktekSettings.SETTINGS_DISABLE_VIBRATION_FEEDBACK, 1) == 0)
                            vibration_patterns(VIBRATION_EXECUTION);
                        break;
                    case STATUS_CALIB:
                        Log.v(TAG, "+ STATUS_CALIB");
                        onCalibrationModeWritten(value);
                        break;
                }

                mDevice_State = value;
                return;
            case COMMAND_CAS_GESTURE_STATUS:
                onGestureStatusFeedback(value);
                onGestureStatusWritten(value);
                return;
            default:
                break;
        }

        EventBus.getDefault().post(new onDeviceACK(true, cmd, value));
    }

    public void responseNAK(byte cmd, byte number) {
        int value = number - '0';
        EventBus.getDefault().post(new onDeviceACK(false, cmd, value));
    }

    // Data packages always have the following format [CMD:DATA]
    public void processReport(String cmd, String response) {
        switch (cmd) {
            case "ACK":
                byte[] bytes = response.getBytes();
                responseACK(bytes[0], bytes[1]);
                break;
            case "NAK":
                byte[] bytesNAK = response.getBytes();
                responseNAK(bytesNAK[0], bytesNAK[1]);
                break;
            case "BT":
                if (response.equals("1")) {
                    Log.v(TAG, "Main Button pressed " + mDevice_State);
                }

                if (mDevice_State == STATUS_SLEEP) {
                    setApplicationPaused(false);
                }

                EventBus.getDefault().post(new onButtonPressed(response));
                break;
            case "STK":
                int res = Integer.valueOf(response);
                FlicktekManager.getInstance().setSamplingRateTicks(res);
                EventBus.getDefault().post(new onStreamingSpeed(res));
                break;
            case "GIT":
                Log.v(TAG, "+ FIRMWARE REVISION " + response);
                FlicktekManager.getInstance().setFirmwareRevision(response);
                EventBus.getDefault().post(new onRevisionRequested(response));
                break;
            case "VER":
                Log.v(TAG, "+ FIRMWARE VERSION " + response);
                FlicktekManager.getInstance().setFirmwareVersion(response);
                EventBus.getDefault().post(new onVersionRequested(response));
                break;
            case "STS_DATA":
                Log.v(TAG, "+ STARTING DATA CAPTURING " + response);
                int data_values = Integer.valueOf(response);
                DataCaptureModeStart(data_values);
                break;
            case "STE_DATA":
                // Parcial stream, send whatever we have to be displayed
                if (mDataCapturing) {
                    mDataCapturing = false;
                    EventBus.getDefault().post(new onGestureRawData(mLastGesture, u16t_sensor,
                            toByteArray(sensor_buffer)));
                    Log.v(TAG, "+ End of stream but we don't have all the data");
                }

                Log.v(TAG, "+ ENDING DATA CAPTURING " + response);
                int data_streamed = Integer.valueOf(response);
                DataCaptureModeEnd(data_streamed);
                break;
        }
    }

    private static final String REPORT_PATTERN = "\\[(\\w+):(.*)\\]";

    public boolean mDataCapturing = false;
    public int mDataSize = 0;

    private int mLastGesture = 0;

    public void DataCaptureModeStart(int data_values) {
        mDataSize = data_values;
        mDataCapturing = true;
        u16t_sensor = new int[data_values * 4];

        for (int t = 0; t < u16t_sensor.length; t++) {
            u16t_sensor[t] = 5500;
        }

        sensor_buffer = new ArrayList<Byte>();

        decoding_state = STREAM_FULL_16BITS;
        decoding_position = 0;
        decoding_16bits = -1;
        decoding_sensor = 0;
        decoding_total_bytes = 0;
    }

    public void DataCaptureModeEnd(int data_values) {
        if (mDataSize != data_values) {
            Log.v(TAG, "Failed streaming data!");
        }
        mDataCapturing = false;
    }

    public static void printHex(byte[] buf) {
        StringBuilder sb = new StringBuilder();
        sb.append("Data " + buf.length + " [ ");

        for (byte b : buf) {
            sb.append(String.format("%02X ", b));
        }

        sb.append("]");
        Log.v(TAG, sb.toString());
    }

    public static final int PAYLOAD_SIGNAL = 0xFF;
    public static final int PAYLOAD_8BITS = 0xFE;
    public static final int PAYLOAD_16BITS = 0xFD;

    public static final int STREAM_FULL_16BITS = 0;
    public static final int STREAM_SIGNAL_COUNT = 1;
    public static final int STREAM_INC_8BITS = 2;

    private int decoding_state = STREAM_FULL_16BITS;
    private int decoding_position = 0;
    private int decoding_16bits = -1;
    private int decoding_sensor = 0;
    private int decoding_total_bytes = 0;
    public boolean decoding_debug = false;

    private int u16t_sensor[];
    private ArrayList<Byte> sensor_buffer;

    public static byte[] toByteArray(ArrayList<Byte> in) {
        final int n = in.size();
        byte ret[] = new byte[n];
        for (int i = 0; i < n; i++) {
            ret[i] = in.get(i);
        }
        return ret;
    }

    private final int centre_signal[] = {5488, 5488, 5488, 5488};

    public static int unsignedToBytes(byte b) {
        return b & 0xFF;
    }

    void StreamDecode(byte data_byte) {
        int old_value;
        int unsigned_value = unsignedToBytes(data_byte);
        int signed_value = data_byte;

        if (decoding_sensor == 0 && decoding_16bits == -1) {
            switch (unsigned_value) {
                case PAYLOAD_SIGNAL:
                    decoding_state = STREAM_SIGNAL_COUNT;
                    if (decoding_debug)
                        Log.v(TAG, "---------- STREAM_SIGNAL_COUNT --------- \n");
                    return;
                case PAYLOAD_8BITS:
                    decoding_state = STREAM_INC_8BITS;
                    if (decoding_debug)
                        Log.v(TAG, "---------- STREAM_INC_8BITS --------- \n");
                    return;
                case PAYLOAD_16BITS:
                    decoding_state = STREAM_FULL_16BITS;
                    if (decoding_debug)
                        Log.v(TAG, "---------- STREAM_FULL_16BITS --------- \n");
                    return;
            }
        }

        switch (decoding_state) {
            case STREAM_FULL_16BITS:
                if (decoding_16bits == -1) {
                    decoding_16bits = unsigned_value << 8;
                    return;
                }

                unsigned_value += decoding_16bits;
                decoding_16bits = -1;
                break;
            case STREAM_INC_8BITS:
                old_value = u16t_sensor[(decoding_position - 1) * 4 + decoding_sensor];
                unsigned_value = old_value + signed_value;
                break;
            case STREAM_SIGNAL_COUNT:
                if (decoding_debug)
                    Log.v(TAG, "- Repeat " + unsigned_value);

                int pos = (decoding_position) * 4;
                for (int t = 0; t < unsigned_value; t++) {
                    for (int c = 0; c < 4; c++) {
                        u16t_sensor[pos + c] = centre_signal[c];
                    }
                    pos += 4;
                    decoding_position++;
                }
                decoding_sensor = 0;
                return;
        }

        //Log.v(TAG, " " + unsigned_value);
        int pos = decoding_position * 4 + decoding_sensor;
        if (pos >= u16t_sensor.length) {
            Log.v(TAG, "! Problems streaming ");
            return;
        }

        u16t_sensor[pos] = unsigned_value;
        decoding_sensor++;
        if (decoding_sensor == 4) {
            if (decoding_debug)
                Log.v(TAG, decoding_position + " " +
                        u16t_sensor[decoding_position * 4 + 0] + " " +
                        u16t_sensor[decoding_position * 4 + 1] + " " +
                        u16t_sensor[decoding_position * 4 + 2] + " " +
                        u16t_sensor[decoding_position * 4 + 3]);

            decoding_sensor = 0;
            decoding_position++;

            if (decoding_position == mDataSize) {
                mDataCapturing = false;
                EventBus.getDefault().post(new onGestureRawData(mLastGesture, u16t_sensor,
                        toByteArray(sensor_buffer)));
                Log.v(TAG, "+ End of stream! " + decoding_total_bytes + " bytes transferred");
                return;
            }
        }
    }

    public boolean onBufferArrived(byte[] data) {
        if (!mDataCapturing)
            return false;

        //if (decoding_debug)
        printHex(data);

        decoding_total_bytes += data.length;
        for (byte b : data) {
            sensor_buffer.add(b);
            StreamDecode(b);
        }
        return true;
    }

    // Basic check to search for binary data on the stream.
    public static boolean isSampleData(byte[] buf) {
        for (int t = 0; t < buf.length; t++) {
            if (buf[t] >= 127)
                return true;

            if (buf[t] < 32 && buf[t] != '\n') {
                return true;
            }
        }
        return false;
    }

    public void onCommandArrived(byte[] buf_str) {
        // Data can be lost so we look at the buffer to evaluate
        // if the data is binary of not.
        //
        // We assume we have at least a byte outside ascii values.
        if (isSampleData(buf_str)) {
            onBufferArrived(buf_str);
            return;
        }

        // We send an empty package to mark end of stream.
        if (buf_str.length == 0) {
            Log.v(TAG, "- Empty package");
            if (mDataCapturing) {
                // Parcial stream, send whatever we have to be displayed
                EventBus.getDefault().post(new onGestureRawData(mLastGesture, u16t_sensor,
                        toByteArray(sensor_buffer)));
                mDataCapturing = false;
            }
            return;
        }

        String data = new String(buf_str);
        Log.v(TAG, "************************** " + data + " **************************");
        try {
            String report = new String(buf_str, "UTF-8");
            Pattern pattern = Pattern.compile(REPORT_PATTERN);
            Matcher matcher = pattern.matcher(report);
            if (matcher.matches()) {
                //Log.v(TAG, "GROUPS " + matcher.groupCount());
                for (int t = 0; t < matcher.groupCount() + 1; t++) {
                    Log.v(TAG, matcher.group(t));
                }

                if (matcher.groupCount() == 2) {
                    processReport(matcher.group(1), matcher.group(2));
                } else {
                    Log.v(TAG, "+ Too many items on report " + data);
                }
            }
        } catch (Exception ex) {
            Log.v(TAG, "+ Exception parsing " + data + " " + ex.toString());
        }

        // Found single value command
        if (buf_str[0] == COMMAND_START && buf_str[3] == COMMAND_END) {
            int cmd = buf_str[1];
            int value = buf_str[2];

            if (mDataCapturing) {
                mDataCapturing = false;
                Log.v(TAG, "+ Found command outside streaming!");

                // Parcial stream, send whatever we have to be displayed
                EventBus.getDefault().post(new onGestureRawData(mLastGesture, u16t_sensor,
                        toByteArray(sensor_buffer)));
            }

            // If it is a number we like it in digital form.
            if (value >= '0' && value <= '9') {
                value -= '0';
            }

            switch (cmd) {
                case COMMAND_CHARGING_STATE:
                    boolean charging;
                    if (value == 1) {
                        Log.v(TAG, "------------------ CHARGING REPORT! -------------------");
                        charging = true;
                    } else {
                        charging = false;
                        Log.v(TAG, "------------------ DISCHARGING REPORT!-----------------");
                    }

                    FlicktekManager.getInstance().setChargingState(charging);
                    EventBus.getDefault().post(new onChargingState(charging));
                    break;
                case COMMAND_GESTURE:
                    mLastGesture = value;
                    onGestureChanged(value);
                    return;
                case COMMAND_CAS_GESTURE_STATUS:
                case COMMAND_CAS_GESTURE_FEEDBACK:
                    onGestureStatusFeedback(value);
                    onGestureStatusWritten(value);
                    EventBus.getDefault().post(new onGestureStatusEvent(value));
                    return;
                case COMMAND_CAS_GESTURE_QUALITY:
                    EventBus.getDefault().post(new onGestureQuality(value));
                    break;
                case COMMAND_OK:
                    if (value == 'K') {
                        Log.v(TAG, "------------------ OK FOUND! -------------------");
                        if (!FlicktekManager.getInstance().isHandshakeOk()) {
                            FlicktekManager.getInstance().setHandshakeOk(true);
                            onDeviceRespondedToConnection();
                            EventBus.getDefault().post(new onDeviceReady());
                        }
                    }
                    break;
            }
            return;
        }

        //EventBus.getDefault().post(new CharacterEvent(_value));
    }

    //------------------------------------------------------------------------------
    // FLICKTEK MESSAGES
    //------------------------------------------------------------------------------

    public class onNotCalibrated {
    }

    public class onCalibrationWritten {
        public int status;

        public onCalibrationWritten(int status) {
            this.status = status;
        }
    }

    public class onCalibrationStepStarted {
        public int currentGestureIndex;
        public int currentGestureIteration;

        public onCalibrationStepStarted(int currentGestureIndex, int currentGestureIteration) {
            this.currentGestureIndex = currentGestureIndex;
            this.currentGestureIteration = currentGestureIteration;
        }
    }

    public class onGestureStatusEvent {
        public Integer status;
        public Integer value;
        public Integer unit;
        public Integer decimal;

        public onGestureStatusEvent(int status) {
            this.value = status;
            this.unit = status % 10;
            this.status = this.unit;
            status = status / 10;
            this.decimal = status % 10;
        }
    }

    public class onCalibrationStepEvent {
    }

    public class onDeviceReady {

    }

    public class onChargingState {
        public Boolean isCharging;

        public onChargingState(boolean isCharging) {
            this.isCharging = isCharging;
        }
    }


    public class onCalibrationAttributeEvent {
        public Integer quality;
        public Integer unit;
        public Integer decimal;

        public onCalibrationAttributeEvent(int quality) {
            this.quality = quality;
            this.unit = quality % 10;
            quality = quality / 10;
            this.decimal = quality % 10;
        }
    }

    public class onBatteryEvent {
        public Integer value;

        public onBatteryEvent(int value) {
            this.value = value;
        }
    }

    // We have a gesture which is not classified by the algorithm
    public class onGestureNotClassified {
        public onGestureNotClassified() {

        }
    }

    public class onButtonPressed {
        public String value;

        public onButtonPressed(String value) {
            this.value = value;
        }
    }

    public class onVersionRequested {
        public String value;

        public onVersionRequested(String version) {
            value = version;
        }
    }

    public class onRevisionRequested {
        public String value;

        public onRevisionRequested(String revision) {
            value = revision;
        }
    }

    public class onStreamingSpeed {
        public int ticks;

        public onStreamingSpeed(int value) {
            ticks = value;
        }
    }

    public class onDeviceACK {
        public boolean ACK;  // ACK or NAK
        public byte command;
        public int value;

        public onDeviceACK(boolean isOK, byte command, int value) {
            this.ACK = isOK;
            this.command = command;
            this.value = value;
        }
    }

    // Gestures classified. the could be FlicktekManager.GESTURE_XXXX
    public class onGestureEvent {
        public Integer status;
        public Integer quality;

        public onGestureEvent(int value) {
            this.status = value;
        }

        public onGestureEvent(int value, int quality) {
            this.status = value;
            this.quality = quality;
        }
    }

    // Quality comes randomly normally after the gesture and it is a value between 0 and 9
    public class onGestureQuality {
        public Integer quality;

        public onGestureQuality(int value) {
            this.quality = value;
        }
    }

    public class onGestureRawData {
        public Integer gesture;
        public int u16t_sensor[];
        public byte byte_array[];

        public onGestureRawData(int value, int sensor_data[], byte raw[]) {
            this.gesture = value;
            this.u16t_sensor = sensor_data;
            this.byte_array = raw;
        }
    }

    public class onNotificationEvent {
        public NotificationModel model;

        public onNotificationEvent(NotificationModel model) {
            this.model = model;
        }
    }
}
