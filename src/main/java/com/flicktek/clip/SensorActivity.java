package com.flicktek.clip;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.flicktek.clip.eventbus.OnLongPressEvent;
import com.flicktek.clip.eventbus.OnSwipeEvent;
import com.flicktek.clip.eventbus.onSingleTapUpEvent;

import org.greenrobot.eventbus.EventBus;

import static com.flicktek.clip.SensorActivity.ORIENTATIONS.HAND_DOWN;
import static com.flicktek.clip.SensorActivity.ORIENTATIONS.HAND_LEVEL;
import static com.flicktek.clip.SensorActivity.ORIENTATIONS.HAND_UP;
import static com.flicktek.clip.SensorActivity.ORIENTATIONS.NORMAL;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

// For Math.xxx()
// For xxx()

public class SensorActivity extends Activity implements SensorEventListener {
    private static final String TAG = "Sensor";

    private SensorManager mSensorManager;
    private static final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    private final boolean USE_GYROSCOPE = true;

    private static final float SHAKE_THRESHOLD = 1.1f;
    private static final int SHAKE_WAIT_TIME_MS = 250;
    private static final float ROTATION_THRESHOLD = 2.0f;
    private static final int ROTATION_WAIT_TIME_MS = 100;

    private Sensor mAccelerometer;
    private Sensor mMagnetic;
    private Sensor mGyro;

    private static SensorState mSensorStateCache = null;

    public static class SensorState {
        public float[] mAccelerometerReading = new float[3];
        public float[] mGravity = new float[3];
        public float[] mRotationMatrix = new float[9];
        public float[] mOrientationAngles = new float[3];

        public double mPitch = 0;
        public double mRoll = 0;

        public int mCurrentRollState = 0;
        public int mCurrentPitchState = 0;

        public void populate() {
            System.arraycopy(mAccelerometerReading, 0, mSensorStateCache.mAccelerometerReading,
                    0, mAccelerometerReading.length);

            System.arraycopy(mGravity, 0, mSensorStateCache.mGravity,
                    0, mGravity.length);

            System.arraycopy(mRotationMatrix, 0, mSensorStateCache.mRotationMatrix,
                    0, mRotationMatrix.length);

            System.arraycopy(mOrientationAngles, 0, mSensorStateCache.mOrientationAngles,
                    0, mOrientationAngles.length);

            mPitch = SensorActivity.mPitch;
            mRoll = SensorActivity.mRoll;

            mCurrentPitchState = SensorActivity.mCurrentPitchState;
            mCurrentRollState = SensorActivity.mCurrentRollState;
        }

        public boolean isEmpty() {
            if (mPitch == 0 && mRoll == 0)
                return true;
            return false;
        }
    }

    public static final class ORIENTATIONS {
        public static final int NORMAL = 0;

        public static final int HAND_UP = 1;         // < -45
        public static final int HAND_DOWN = 2;       // > 45
        public static final int HAND_LEVEL = 3;      //

        public static final int WATCH_FACE_USER = 4; // 27 ... 80
        public static final int WATCH_VERTICAL = 5;  // -75 .. -105
        public static final int PALM_UP = 6;         // < -130  .. > 130
        public static final int PALM_DOWN = 7;       // < 20 .. > ~20

        public String[] STRINGS = {"HAND_UP", "HAND_DOWN", "HAND_LEVEL", "PALM_UP", "PALM_DOWN"};
    }

    public static int mCurrentRollState = NORMAL;
    public static int mCurrentPitchState = HAND_LEVEL;

    public static int calculatePitchState() {
        int state = HAND_LEVEL;

        if (mPitch < -45) {
            state = HAND_UP;
        } else if (mPitch > 45) {
            state = HAND_DOWN;
        } else {
            if (mPitch < 35 && mPitch > -35) {
                state = HAND_LEVEL;
            }
        }

        if (state != mCurrentPitchState) {
            switch (state) {
                case HAND_UP:
                    EventBus.getDefault().post(new onHandUp());
                    break;
                case HAND_DOWN:
                    EventBus.getDefault().post(new onHandDown());
                    break;
                case HAND_LEVEL:
                    EventBus.getDefault().post(new onHandLevel());
                    break;
            }

            // Disable check for the next few ms
            long now = System.currentTimeMillis();
            mLastShakeTime = now;
        }

        mCurrentPitchState = state;
        return mCurrentPitchState;
    }

    public static synchronized boolean calculateGestureState() {
        calculatePitchRoll(gravity[0], gravity[1], gravity[2]);

        // We don't have sensor data or a calculation.
        if (mPitch == 0 && mRoll == 0)
            return false;

        calculatePitchState();

        if (mSensorStateCache == null) {
            mSensorStateCache = new SensorState();
        }

        mSensorStateCache.populate();
        return true;
    }

    @Nullable
    public static synchronized SensorState consumeGestureState() {
        SensorState ret = mSensorStateCache;
        if (mSensorStateCache == null || mSensorStateCache.isEmpty())
            return null;

        mSensorStateCache = null;
        return ret;
    }

    public GestureDetector mDetector;

    //Roll & Pitch are the angles which rotate by the axis X and y
    public static double mRoll = 0.00, mPitch = 0.00;

    public SensorActivity() {
    }

    public int mLastRoll = 0;
    public int lastRoll = 0;

    public static void calculatePitchRoll(float x_Buff, float y_Buff, float z_Buff) {
        mRoll = Math.atan2(y_Buff, z_Buff) * 57.3f;
        mPitch = Math.atan2((-x_Buff), Math.sqrt(y_Buff * y_Buff + z_Buff * z_Buff)) * 57.3f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDetector = new GestureDetector(this, new GestureListener());

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (USE_GYROSCOPE) {
            mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI);

        mSensorManager.registerListener(this, mMagnetic, SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI);

        if (USE_GYROSCOPE) {
            mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Don't receive any more updates from either sensor.
        mSensorManager.unregisterListener(this);
        EventBus.getDefault().unregister(this);
    }

    private final static double EPSILON = 0.00001;

    public static float linear_acceleration[] = new float[3];
    public static float gravity[] = new float[3];

    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp;

    public void onSensorGyroscope(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_GYROSCOPE)
            return;

        detectRotation(event);

        // This time step's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.

        if (timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            // Axis of the rotation sample, not normalized yet.
            float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];

            // Calculate the angular speed of the sample
            float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

            // Normalize the rotation vector if it's big enough to get the axis
            if (omegaMagnitude > EPSILON) {
                axisX /= omegaMagnitude;
                axisY /= omegaMagnitude;
                axisZ /= omegaMagnitude;
            }

            // Integrate around this axis with the angular speed by the time step
            // in order to get a delta rotation from this sample over the time step
            // We will convert this axis-angle representation of the delta rotation
            // into a quaternion before turning it into the rotation matrix.
            float thetaOverTwo = omegaMagnitude * dT / 2.0f;
            float sinThetaOverTwo = (float) sin(thetaOverTwo);
            float cosThetaOverTwo = (float) cos(thetaOverTwo);
            deltaRotationVector[0] = sinThetaOverTwo * axisX;
            deltaRotationVector[1] = sinThetaOverTwo * axisY;
            deltaRotationVector[2] = sinThetaOverTwo * axisZ;
            deltaRotationVector[3] = cosThetaOverTwo;

            //Log.v(TAG, "deltaRotation " + deltaRotationVector[0] + "," +
            //        deltaRotationVector[1] + "," +deltaRotationVector[2] + "," +deltaRotationVector[3]);
        }

        timestamp = event.timestamp;
        float[] deltaRotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);

        // User code should concatenate the delta rotation we computed with the current rotation
        // in order to get the updated rotation.
        // rotationCurrent = rotationCurrent * deltaRotationMatrix;
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);

            // alpha is calculated as t / (t + dT)
            // with t, the low-pass filter's time-constant
            // and dT, the event delivery rate

            final float alpha = 0.8f;

            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            linear_acceleration[0] = event.values[0] - gravity[0];
            linear_acceleration[1] = event.values[1] - gravity[1];
            linear_acceleration[2] = event.values[2] - gravity[2];

            detectShake(event);

            long now = System.currentTimeMillis();
            if ((now - mLastShakeTime) > SHAKE_WAIT_TIME_MS) {
                calculateGestureState();
            }

            //Log.v(TAG, "gravity " + gravity[0] + "," + gravity[1] + "," +gravity[2]);

        } else if (event.sensor.getType() == (Sensor.TYPE_MAGNETIC_FIELD)) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        } else {
            onSensorGyroscope(event);
        }

        //EventBus.getDefault().post(new onSensorRawData(event.sensor.getType(), event.values));
    }

    // Shake detection
    private static long mLastShakeTime = 0;
    private long mShakeTime = 0;
    private long mRotationTime = 0;

    public boolean mIsShacking = false;

    public void onShacking() {
        if (mIsShacking)
            return;

        mIsShacking = true;
        //Log.v(TAG, "Started shacking");
        EventBus.getDefault().post(new onShackingStarted());
    }

    public void onNotShacking() {
        if (!mIsShacking)
            return;

        mIsShacking = false;

        //Log.v(TAG, "Stopped shacking");
        EventBus.getDefault().post(new onShackingStopped());
    }

    public void onRotating() {
        //Log.v(TAG, "Started rotating");
        EventBus.getDefault().post(new onRotating());
    }

    public void onRotationStop() {
        //Log.v(TAG, "Stopped rotating");
        EventBus.getDefault().post(new onRotationStopped());
    }

    // References:
    //  - http://jasonmcreynolds.com/?p=388
    //  - http://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125
    private void detectShake(SensorEvent event) {
        long now = System.currentTimeMillis();

        if ((now - mShakeTime) > SHAKE_WAIT_TIME_MS) {
            mShakeTime = now;

            float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
            float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
            float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;

            // gForce will be close to 1 when there is no movement
            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            // Change background color if gForce exceeds threshold;
            // otherwise, reset the color
            if (gForce > SHAKE_THRESHOLD) {
                mLastShakeTime = now;
                onShacking();
            } else {
                onNotShacking();
            }
        }
    }

    private void detectRotation(SensorEvent event) {
        long now = System.currentTimeMillis();

        if ((now - mRotationTime) > ROTATION_WAIT_TIME_MS) {
            mRotationTime = now;
            mShakeTime = now;

            // Change background color if rate of rotation around any
            // axis and in any direction exceeds threshold;
            // otherwise, reset the color
            if (Math.abs(event.values[0]) > ROTATION_THRESHOLD ||
                    Math.abs(event.values[1]) > ROTATION_THRESHOLD ||
                    Math.abs(event.values[2]) > ROTATION_THRESHOLD) {
                onRotating();
            } else {
                onRotationStop();
            }
        }
    }

    // Magnetometer is not working on moto360
    public void refreshText() {
        updateOrientationAngles();
        String anglesText = "X: " + mOrientationAngles[0] + "\nY: " + mOrientationAngles[1] + "\nZ: " + mOrientationAngles[2];
        //angles.setText(anglesText);
        Log.v(TAG, "angles:  " + anglesText);
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.

    // TODO Magnetometer is not working on moto360 so we cannot calculate the rotation matrix

    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.

        // "mRotationMatrix" now has up-to-date information.
        mSensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);

        // "mOrientationAngles" now has up-to-date information.
        mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            EventBus.getDefault().post(new onSingleTapUpEvent());
            return super.onSingleTapUp(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            Log.d(TAG, "OnLongPress");
            EventBus.getDefault().post(new OnLongPressEvent());
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                        result = true;
                    }
                } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom();
                    } else {
                        onSwipeTop();
                    }
                    result = true;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }


        public void onSwipeRight() {
            Log.v(TAG, "onSwipeRight ");
            EventBus.getDefault().post(new OnSwipeEvent(FlicktekManager.SWIPE_RIGHT));
        }

        public void onSwipeLeft() {
            Log.v(TAG, "onSwipeLeft ");
            EventBus.getDefault().post(new OnSwipeEvent(FlicktekManager.SWIPE_LEFT));
        }

        public void onSwipeTop() {
            Log.v(TAG, "onSwipeTop ");
            EventBus.getDefault().post(new OnSwipeEvent(FlicktekManager.SWIPE_TOP));
        }

        public void onSwipeBottom() {
            Log.v(TAG, "onSwipeBottom ");
            EventBus.getDefault().post(new OnSwipeEvent(FlicktekManager.SWIPE_BOTTOM));
            // Display time!
        }
    }

    public class onSensorRawData {
        public onSensorRawData(int type, float raw_data[]) {
        }
    }

    public static class onHandUp {
        public onHandUp() {
        }
    }

    public static class onHandDown {
        public onHandDown() {
        }
    }

    public static class onHandLevel {
        public onHandLevel() {
        }
    }

    public class onPalmUp {
        public onPalmUp() {
        }
    }

    public class onPalmDown {
        public onPalmDown() {
        }
    }

    public class onShackingStarted {
        public onShackingStarted() {
        }
    }

    public class onShackingStopped {
        public onShackingStopped() {
        }
    }

    public class onRotating {
        public onRotating() {
        }
    }

    public class onRotationStopped {
        public onRotationStopped() {

        }
    }
}