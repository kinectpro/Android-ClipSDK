package com.flicktekltd.clip.flickgym;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class MyGLObject {
    private static final String TAG = "MyGLObject";
    public static boolean mDebug = false;
    protected boolean mFinished = false;

    protected final FloatBuffer vertexBuffer;
    protected final ShortBuffer drawListBuffer;

    // number of coordinates per vertex in this array
    protected float mRatio = 1.0f;

    protected static final int COORDS_PER_VERTEX = 3;
    protected static final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    protected final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    // The matrix must be included as a modifier of gl_Position.
                    // Note that the uMVPMatrix factor *must be first* in order
                    // for the matrix multiplication product to be correct.
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    protected final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    protected float weaveCoords[] = {
            -0.5f, 0.5f, 0.0f,  // top left
            -0.5f, -0.5f, 0.0f, // bottom left
            0.5f, -0.5f, 0.0f,  // bottom right
            0.5f, 0.5f, 0.0f}; // top right

    protected short drawOrder[] = {0, 1, 2, 0, 2, 3}; // order to draw vertices

    protected final int mProgram;

    public int getValuePos(int pos) {
        //if (mSensorNumber > 1)
        //    return 5500;

        pos *= 4;
        if (pos < 0 || pos + mSensorNumber >= mSensorData.length) {
            return 5500;
        }

        try {
            return mSensorData[pos + mSensorNumber];
        } catch (Exception e) {
            e.printStackTrace();
            return 5500;
        }
    }

    public MyGLObject(float ratio) {
        ratio -= ratio / 8.0f;

        if (ratio > 3.5f)
            ratio = 3.5f;

        this.mRatio = ratio;

        //generateWeave(3, -1.0f, 1.0f);
        //generateWeave(4, -1.0f, 1.0f);
        //generateWeave(7, -1.0f, 1.0f);
        generate(80, ratio);

        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                weaveCoords.length * 4);

        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(weaveCoords);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // prepare shaders and OpenGL program
        int vertexShader = MyGLRenderer.loadShader(
                GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables
    }

    public static float fInterpolateCosine(float a, float b, float x) {
        float z = (float) (0.5f * (1.0f - Math.cos(x * Math.PI)));
        // printf ("þ %f: %f ",x,(1-cos (x*3.141592)));
        return (a * (1.0f - z) + b * z);
    }

    public static float fInterpolateLinear(float a, float b, float t) {
        return (float) (a * (1.0f - t) + b * t);
    }

    public static float interpolate_overhauser_float(float p1, float p2, float p3, float p4, float t) {
        float t3, t2, c1, c2, c3, c4;
        t2 = t * t;
        t3 = t2 * t;
        c1 = -t3 + 2 * t2 - t;
        c2 = 3 * t3 - 5 * t2 + 2;
        c3 = -3 * t3 + 4 * t2 + t;
        c4 = t3 - t2;
        c1 /= 2;
        c2 /= 2;
        c3 /= 2;
        c4 /= 2;
        return (c1 * p1 + c2 * p2 + c3 * p3 + c4 * p4);
    }

    public void printVertex(String text, int point, float array[]) {
        if (!mDebug) {
            return;
        }
        Log.v(TAG, text + " " + point + " (" +
                array[point * 3 + 0] + "," +
                array[point * 3 + 1] + "," +
                array[point * 3 + 2] + ")");
    }

    float color[] = {0.2f, 0.709803922f, 0.898039216f, 1.0f};

    public void setColor(float new_color[]) {
        for (int t = 0; t < new_color.length; t++) {
            new_color[t] /= 3.0f;
        }
        color = new_color;
    }

    protected int mPositionHandle;
    protected int mColorHandle;
    protected int mMVPMatrixHandle;

    protected boolean mInvalidateBuffer = false;

    public void draw(float[] mvpMatrix, float elapsed) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        if (mInvalidateBuffer) {
            //mInvalidateBuffer = false;
            try {
                interpolate(elapsed);
            } catch (Exception e) {
                e.printStackTrace();
            }

            vertexBuffer.put(weaveCoords);
            vertexBuffer.position(0);
        }

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        MyGLRenderer.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        MyGLRenderer.checkGlError("glUniformMatrix4fv");

        // Draw the square
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

    // Interpolates a value from the path using overhauser beziers
    public float getValue(int path[], int num_vertices, int position) {
        int p;

        float step = ((float) position) / ((float) (num_vertices) / (path.length - 1));
        int istep = (int) step;
        float t = step - istep;

        p = (istep - 1);
        if (p < 0) {
            p = 0;
        }

        float p0 = path[p];
        float p1 = path[(istep + 0)];
        float p2 = path[(istep + 1)];

        p = (istep + 2);
        if (p >= path.length)
            p = path.length - 1;

        float p3 = path[p];

        float val = interpolate_overhauser_float(p0, p1, p2, p3, t);

        if (mDebug) {
            String str = position + " step " + step + " iStep " + istep;
            Log.v(TAG, str + "  (" + p0 + "," + p1 + "," + p2 + "," + p3 + ", t = " + t + ") = " + val);
        }

        return val;
    }

    public void generate(int size, float ratio) {

    }

    public void interpolate(float elapsed_time) {
    }

    public boolean isFinished() {
        return mFinished;
    }

    //---------------------------------------------------------------------------------
    // Sensor data
    //---------------------------------------------------------------------------------

    protected int mSensorData[];
    protected int mSensorNumber = 0;

    protected float mMin = 4000;
    protected float mMax = 10000;

    // Time to display the effect
    public float mDisplayTime = 0;

    // Number of steps to interpolate this wave into.
    // We divide the signal in equidistant steps and we render
    // interpolating between them.
    public long mSteps = 6;

    protected int mTotalPoints = 0;
    protected int mRealPoints = 0;

    protected float mMultiplier = 1.0f;

    private int mMaxPoint = 0;
    private int mMaxSensor = 0;

    public void populate(int steps, float displayTime, int sensor[], int sensor_number,
                         int max_point, int max_sensor,
                         float min, float max,
                         float scale) {

        mSensorData = sensor;
        mSensorNumber = sensor_number;
        mInvalidateBuffer = true;

        mSteps = steps;
        mDisplayTime = displayTime;

        mMin = min;
        mMax = max;

        mMaxPoint = max_point;
        mMaxSensor = max_sensor;

        int path_size = (mSensorData.length / 4) / steps;
        mPath = new int[path_size];

        //3.0f
        mMultiplier = scale;
    }

    // Function to setup a path to render.
    // A path is a subsection of the array that represents sensor data.
    // Path of data to interpolate or render.
    protected int mPath[];

    // The object is in charge of creating the right vertices and indexes from a path
    public void setPath(int path[]) {
    }

    public void flush(int position) {
        if (mSensorData == null)
            return;

        for (int t = 0; t < mPath.length; t++) {
            if (position + mSensorNumber >= mSensorData.length) {
                mPath[t] = 5500;
                //mInvalidateBuffer = false;
                mFinished = true;
            } else {
                mPath[t] = mSensorData[position + mSensorNumber];
                position += 3;
            }
        }

        setPath(mPath);
    }
}
