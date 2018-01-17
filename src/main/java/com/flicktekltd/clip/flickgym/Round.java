package com.flicktekltd.clip.flickgym;

import android.util.Log;

/**
 * A two-dimensional square for use as a drawn object in OpenGL ES 2.0.
 */
public class Round extends MyGLObject {
    private static final String TAG = "Wave";

    private boolean mDebugInterpolation = false;

    @Override
    public void setPath(int path[]) {
        if (mDebugInterpolation)
            Log.v(TAG, "----------- PATH " + mSensorNumber + " " + path.length + " ------------");

        if (path.length < 4) {
            mInvalidateBuffer = false;
            return;
        }

        mPath = path.clone();

        int pos = 0;
        for (int c = 0; c < mRealPoints; c++) {
            float value = getValue(mPath, mRealPoints, c);
            float circle_r = 0.6f;

            float scaled = Math.abs(((value - 5500) / (mMax)) * (mRatio * mMultiplier));
            float r = scaled;
            float t = (float) (2 * Math.PI * c) / mRealPoints;

            if (c == 0 || c == mRealPoints - 1) {
                weaveCoords[pos + 0] = (float) ((circle_r + r) * Math.cos(t));
                weaveCoords[pos + 1] = (float) ((circle_r + r) * Math.sin(t));
                pos += 3;
            } else {
                weaveCoords[pos + 0] = (float) ((circle_r + r) * Math.cos(t));
                weaveCoords[pos + 1] = (float) ((circle_r + r) * Math.sin(t));
                pos += 3;

                //r = r / 2;
                float cirle_m = circle_r - 0.01f;
                if (r > cirle_m)
                    r = cirle_m;

                weaveCoords[pos + 0] = (float) ((cirle_m - r) * Math.cos(t));
                weaveCoords[pos + 1] = (float) ((cirle_m - r) * Math.sin(t));
                pos += 3;
            }
        }

        mInvalidateBuffer = true;
    }

    @Override
    public void generate(int size, float ratio) {
        generateWeave(size, -ratio, ratio);
    }

    public void generateWeave(int points, float xStart, float xEnd) {
        mRealPoints = points;
        mTotalPoints = points + (points - 2);
        int total_points = mTotalPoints;

        int total_indices = (total_points - 2);
        int point = 0;
        weaveCoords = new float[total_points * 3];

        float xInc = (xEnd - xStart) / (points - 1);

        int pos = 0;
        float xInterpol = xStart;

        weaveCoords[pos++] = xStart;
        weaveCoords[pos++] = 0;
        weaveCoords[pos++] = 0;
        total_points -= 2;

        printVertex("Start     ", point, weaveCoords);

        point++;

        while (total_points > 0) {
            xInterpol += xInc;

            float yPos = (float) (Math.random() * 0.5);
            weaveCoords[pos++] = xInterpol;
            weaveCoords[pos++] = yPos;
            weaveCoords[pos++] = 0;
            total_points--;

            printVertex("Vertex Up", point, weaveCoords);
            point++;

            weaveCoords[pos++] = xInterpol;
            weaveCoords[pos++] = -yPos;
            weaveCoords[pos++] = 0;
            total_points--;

            printVertex("Vertex Dw", point, weaveCoords);
            point++;
        }

        weaveCoords[pos++] = xEnd;
        weaveCoords[pos++] = 0;
        weaveCoords[pos++] = 0;
        printVertex("Final    ", point, weaveCoords);

        drawOrder = new short[total_indices * 3];
        pos = 0;
        short count = 0;
        short index = 0;
        while (total_indices > 0) {
            drawOrder[pos++] = (short) (count + 0);
            drawOrder[pos++] = (short) (count + 1);
            drawOrder[pos++] = (short) (count + 2);
            count += 1;
            total_indices--;

            /*
            Log.v(TAG, "Index " + index + " (" +
                    drawOrder[(index * 3) + 0] + "," +
                    drawOrder[(index * 3) + 1] + "," +
                    drawOrder[(index * 3) + 2] + ")");
            */
            index++;
        }

        //Log.v(TAG, "---------------------------");
    }


    @Override
    public void interpolate(float elapsed_time) {
        if (mSensorData == null)
            return;

        if (elapsed_time > mDisplayTime) {
            for (int c = 0; c < mPath.length; c++) {
                mPath[c] = (int) ((mPath[c] + 5500) / 2);
            }
            setPath(mPath);
            return;
        }

        float elapsed = fInterpolateCosine(elapsed_time, mDisplayTime, (elapsed_time / mDisplayTime));

        float current_step = ((elapsed / mDisplayTime) * (mSteps - 1));
        int istep = (int) current_step;
        float t = current_step - istep;

        int pos1 = (int) ((istep) * mPath.length);
        int pos2 = (int) ((istep + 1) * mPath.length);

        float z = (float) (0.5f * (1.0f - Math.cos(t * Math.PI)));

        int p;
        for (int c = 0; c < mPath.length; c++) {
            int idx1, idx2;
            // Always Invert second row
            if (istep % 2 == 0) {
                idx1 = c;
                idx2 = (mPath.length - 1) - c;
            } else {
                idx1 = (mPath.length - 1) - c;
                idx2 = c;
            }

            int v1 = getValuePos(pos1 + idx1);
            int v2 = getValuePos(pos2 + idx2);

            // Cosine interpolation with z precalculated
            mPath[c] = (int) fInterpolateLinear(v1, v2, z); //(v1 * (1.0f - t) + v2 * t);
        }

        // Scale down the borders to please the eye a bit
        mPath[0] = (int) ((mPath[0] + 5500) / 2.0f);
        mPath[mPath.length - 1] = (int) ((mPath[mPath.length - 1] + 5500) / 2.0f);

        setPath(mPath);
        //Log.v(TAG, "Frame " + mSensorNumber + " " + elapsed + " " + istep + " t = " + t);
    }

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     */
    public Round(float radius) {
        // initialize vertex byte buffer for shape coordinates
        super(radius);
    }
}