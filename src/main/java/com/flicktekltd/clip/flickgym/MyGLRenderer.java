package com.flicktekltd.clip.flickgym;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.flicktekltd.clip.FlicktekCommands;

import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Provides drawing instructions for a GLSurfaceView object. This class
 * must override the OpenGL ES drawing lifecycle methods:
 * <ul>
 * <li>{@link GLSurfaceView.Renderer#onSurfaceCreated}</li>
 * <li>{@link GLSurfaceView.Renderer#onDrawFrame}</li>
 * <li>{@link GLSurfaceView.Renderer#onSurfaceChanged}</li>
 * </ul>
 */
public class MyGLRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "MyGLRenderer";
    private Wave mWave[] = new Wave[4];
    private Round mCircle[] = new Round[4];

    int mMaxPoint = 0;
    int mTotalPoints = 0;

    public boolean mShowWave = false;
    public boolean mShowCircle = false;

    public float mDisplayTime = 800.0f / 1000.0f;
    public int mSteps = 2;

    public RenderEnd mWaveEndCallback = null;

    private int mQuality = 9;

    public void startRoundEffect() {
        if (mSensor == null)
            return;

        mShowCircle = true;
        mShowWave = false;
        createEffect(mSensor);
    }

    public void startFlatEffect() {
        if (mSensor == null)
            return;

        mShowCircle = false;
        mShowWave = true;
        createEffect(mSensor);
    }

    public void toogleRender() {
        mShowCircle = !mShowCircle;
        mShowWave = !mShowWave;

        if (mCurrentSensor != null) {
            createEffect(mCurrentSensor);
        }
    }

    // Check if we have empty samples at the beginning and at the end
    boolean notDiscardSample(int sensor[], int pos) {
        int value = 0;
        pos *= 4;
        for (int t = 0; t < 4; t++) {
            value += Math.abs(sensor[pos++] - 5488);
        }

        if (value > 412)
            return true;

        return false;
    }

    public int[] cleanupStream(int sensor[]) {
        int nsamples = (sensor.length / 4);
        int start = 0;
        int end = nsamples - 1;

        boolean discard = false;
        while (start < nsamples / 3 && !discard) {
            discard = notDiscardSample(sensor, start);
            if (!discard)
                start++;
        }

        discard = false;
        while (end > nsamples - (nsamples / 3) && !discard) {
            discard = notDiscardSample(sensor, end);
            if (!discard)
                end--;
        }

        Log.v(TAG, "Start (" + start + "," + end + ")");
        return Arrays.copyOfRange(sensor, start * 4, end * 4);
    }

    // We need a function to center the window with the maximum value

    int mCurrentSensor[];

    public void createEffect(int sensor[]) {
        float min = 65535;
        float max = -65535;

        mCurrentSensor = sensor;

        int max_pos = 0;
        for (int t = 0; t < sensor.length; t++) {
            int value = sensor[t];
            if (value < min)
                min = value;

            if (value > max) {
                max = value;
                max_pos = t;
            }
        }

        //sensor[max_pos] = 0;

        int max_point = max_pos / 4;
        int max_sensor = max_pos % 4;

        mMaxPoint = max_point;
        mTotalPoints = sensor.length / 4;

        int length = (mTotalPoints / mSteps);
        if (length == 0)
            return;

        float position = (mMaxPoint % length);

        // Temporal invert to check if we are doing the equations right
        if (mMaxPoint >= length) {
            position = (length - mMaxPoint);
        }

        if (position != 0) {
            mAngle = 90 - (float) (360.0f) * (position / length);
        } else {
            mAngle = 0;
        }

        Log.v(TAG, " Max point = " + mMaxPoint +
                " Position " + position + " Length " + length +
                " mTotalPoints " + mTotalPoints);
        Log.v(TAG, "               Degrees " + mAngle);

        // We don't have enough samples to create a graph
        if (sensor.length < 16)
            return;

        setQuality(10);

        for (int t = 0; t < 4; t++) {
            // It might be that we are not yet initialized
            if (mShowWave && mWave[t] != null)
                mWave[t].populate(mSteps, mDisplayTime, sensor, t,
                        max_point, max_sensor, min, max, 3.0f);

            if (mShowCircle && mWidth > 100) {
                if (mCircle[t] != null)
                    mCircle[t].populate(mSteps, mDisplayTime, sensor, t,
                            max_point, max_sensor,
                            min, max, 1.5f);
            }
        }
    }

    int mSensor[] = null;

    public void setGestureRawData(FlicktekCommands.onGestureRawData gestureRawData) {
        int len = gestureRawData.u16t_sensor.length;
        Log.v(TAG, "+++++++++++++++++++++++++ setGestureRawData " + len + " ++++++++++++++++++++++++++++");

        if (len == 0)
            return;

        int sensor[] = cleanupStream(gestureRawData.u16t_sensor);
        if (sensor.length > 16) {
            mSensor = sensor;
        }
    }

    public void setBackgroundColor(float r, float g, float b) {
        BASE_COLOR_R = r;
        BASE_COLOR_G = g;
        BASE_COLOR_B = b;
        mBkgColor[0] = mBkgColor_New[0] = r;
        mBkgColor[1] = mBkgColor_New[1] = g;
        mBkgColor[2] = mBkgColor_New[2] = b;
    }

    private int mGlOne0 = GLES20.GL_ONE;
    private int mGlOne1 = GLES20.GL_ONE;
    public void setBlendMode(int glOne, int glOne1) {
        mGlOne0 = glOne;
        mGlOne1 = glOne1;
    }

    public interface RenderEnd {
        void onRenderEnd();
    }

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mRotationMatrix = new float[16];

    private float mAngle;

    private int mFrame = 0;

    private long mStartTime = 0;
    private long mEndTime = 0;

    private float mRatio = 1.0f;

    public float BASE_COLOR_R = 0.0f;
    public float BASE_COLOR_G = 0.0f;
    public float BASE_COLOR_B = 0.0f;

    /*
    float color_1[] = {248.0f/255f, 248f/255f, 179.0f/255f, 0.2f};
    float color_2[] = {  0.0f,      127f/255f, 240.0f/255f, 0.2f};
    float color_3[] = { 46.0f/255f, 166f/255f, 154.0f/255f, 0.2f};
    float color_4[] = {199.0f/255f,  66f/255f,  95.0f/255f, 0.2f};
    */

    int wave_colors[][] = {
            {0xFF, 0xBE, 0x78, 0xFF},
            {0x8C, 0xCB, 0xF9, 0xFF},
            {0xFF, 0x77, 0x7C, 0xFF},
            {0xAF, 0xDF, 0x8E, 0xFF}
    };

    int path[][] = {{5500, 5500, 6300, 8000, 10000, 8500, 8500, 7000, 6500, 5500},
            {5500, 4400, 7800, 5000, 7000, 4500, 10000, 4500, 6200, 5500},
            {5500, 5000, 2300, 7000, 4500, 3000, 3000, 8400, 6500, 5500},
            {5500, 7000, 6300, 3000, 4000, 5500, 8000, 5000, 4700, 5000}};

    float[] getColor(int color) {
        float ret[] = new float[4];
        for (int t = 0; t < 4; t++) {
            ret[t] = wave_colors[color][t] / 255.0f;
        }
        return ret;
    }

    // Colour transition to display the quality levels

    float mBkgColor_Old[] = new float[3];
    float mBkgColor_New[] = new float[3];
    float mBkgColor[] = new float[3];

    private long mStartTime_ColorTransition = 0;
    private static final float COLOR_TRANSITION_TIME = 600;

    public void setBackgroundColor() {
        if (mStartTime_ColorTransition == 0) {
            return;
        }

        float t = (mStartTime_ColorTransition - System.currentTimeMillis()) / COLOR_TRANSITION_TIME;

        // Finished transition
        if (t > 1.0f) {
            t = 1.0f;
            mStartTime_ColorTransition = 0;
            for (int c = 0; c < 3; c++) {
                mBkgColor_Old[c] = mBkgColor_New[c];
            }
        }

        for (int c = 0; c < 3; c++) {
            mBkgColor[c] = Wave.fInterpolateCosine(mBkgColor_Old[c], mBkgColor_New[c], t);
        }

        GLES20.glClearColor(mBkgColor[0], mBkgColor[1], mBkgColor[2], 1.0f);
    }

    public void setQuality(Integer quality) {
        this.mQuality = quality;
        mStartTime_ColorTransition = System.currentTimeMillis();
        for (int c = 0; c < 3; c++) {
            mBkgColor_Old[c] = mBkgColor[c];
        }

        if (quality < 7) {
            if (BASE_COLOR_R == 1.0f)
                mBkgColor_New[0] = quality / 7.0f;
            else
                mBkgColor_New[0] = 1.0f - quality / 7.0f;
        } else {
            float val = quality;
            mBkgColor_New[0] = mBkgColor[0];
            mBkgColor_New[1] = mBkgColor[1];
            mBkgColor_New[2] = mBkgColor[2];
        }
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(BASE_COLOR_R, BASE_COLOR_G, BASE_COLOR_B, 1.0f);
    }

    public void startContinuesRendering(RenderEnd callback) {
        mWaveEndCallback = callback;
        mStartTime = System.currentTimeMillis();
        mFrame = 0;
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        float[] scratch = new float[16];

        mEndTime = System.currentTimeMillis();
        long dt = mEndTime - mStartTime;
        float dt_secs = dt / 1000.0f;

        // We let the animation run for one step to let it fade to 0
        if (dt_secs > mDisplayTime + mDisplayTime / mSteps) {
            if (mWaveEndCallback != null) {
                Log.v(TAG, "onFinalRender " + mFrame + " Elapsed " + dt + " fps = " + mFrame / (dt / 1000.f));
                mWaveEndCallback.onRenderEnd();
            }
        }

        mFrame++;

        setBackgroundColor();
        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(mGlOne0, mGlOne1);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        int finished = 0;

        if (mShowWave) {
            for (int t = 0; t < 4; t++) {
                mWave[t].draw(mMVPMatrix, dt_secs);
                if (mWave[t].isFinished())
                    finished++;
            }
        }

        if (mShowCircle && mHeight > 100) {
            // Used the rotation required for the circle to center on the maximum point of
            // the sensor data.

            float angle = mAngle;
            for (int t = 0; t < 4; t++) {

                // Rotate the transformation matrix for the rendering
                // We rotate every sensor a bit so we can show all the signal.
                Matrix.setRotateM(mRotationMatrix, 0, angle, 0, 0, 1.0f);
                Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0);
                angle += 15;
                mCircle[t].draw(scratch, dt_secs);
            }

            //mTriangle.draw(scratch);
        }

        // Draw square
        //mSquare.draw(mMVPMatrix);

        // Create a rotation for the triangle
        // Use the following code to generate constant rotation.
        // Leave this code out when using TouchEvents.
        // long time = SystemClock.uptimeMillis() % 4000L;
        // float angle = 0.090f * ((int) time);

        //Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, 1.0f);

        // Combine the rotation matrix with the projection and camera view
        // Note that the mMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        //Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0);

        // Draw triangle
        //mTriangle.draw(scratch);

        if (finished == 4) {
            mWaveEndCallback.onRenderEnd();
        }
    }

    int mWidth;
    int mHeight;

    // 1 Flat
    // 2 Circle
    int mRenderType = 0;

    public void forceFlatLine() {
        mRenderType = 1;
    }

    public void forceRoundLine() {
        mRenderType = 2;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height);

           mRatio = (float) width / height;

        mWidth = width;
        mHeight = height;

        if (mRenderType == 0) {
            if (mWidth > 100) {
                mShowWave = false;
                mShowCircle = true;
            } else {
                mShowWave = true;
                mShowCircle = false;
            }
        } else {
            if (mRenderType == 1) {
                mShowWave = true;
                mShowCircle = false;
            } else {
                mShowWave = false;
                mShowCircle = true;
            }
        }

        for (int t = 0; t < 4; t++) {
            mWave[t] = new Wave(mRatio);
            mWave[t].setColor(getColor(t));
            mWave[t].setPath(path[t]);

            mCircle[t] = new Round(mRatio);
            mCircle[t].setColor(getColor(t));
            mCircle[t].setPath(path[t]);
        }

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -mRatio, mRatio, -1, 1, 3, 7);
    }

    /**
     * Utility method for compiling a OpenGL shader.
     * <p>
     * <p><strong>Note:</strong> When developing shaders, use the checkGlError()
     * method to debug shader coding errors.</p>
     *
     * @param type       - Vertex or fragment shader type.
     * @param shaderCode - String containing the shader code.
     * @return - Returns an id for the shader.
     */
    public static int loadShader(int type, String shaderCode) {

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     * <p>
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     * <p>
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    /**
     * Returns the rotation angle of the triangle shape (mTriangle).
     *
     * @return - A float representing the rotation angle.
     */
    public float getAngle() {
        return mAngle;
    }

    /**
     * Sets the rotation angle of the triangle shape (mTriangle).
     */
    public void setAngle(float angle) {
        mAngle = angle;
    }

}