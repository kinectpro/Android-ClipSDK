/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.flicktek.clip.flickgym;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.flicktek.clip.FlicktekCommands;
import com.flicktek.clip.FlicktekManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * A view container where OpenGL ES graphics can be drawn on screen.
 * This view can also be used to capture touch events, such as a user
 * interacting with drawn objects.
 */
public class MyGLSurfaceView extends GLSurfaceView implements MyGLRenderer.RenderEnd {
    private static final String TAG = "MyGLSurfaceView";
    public final MyGLRenderer mRenderer;
    private int mQuality = 9;

    void openGLInit() {
        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
        EventBus.getDefault().register(this);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        EventBus.getDefault().unregister(this);
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRenderer = new MyGLRenderer();
        openGLInit();
    }

    public MyGLSurfaceView(Context context) {
        super(context);
        mRenderer = new MyGLRenderer();
        openGLInit();
    }

    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;

    @Override
    public void onRenderEnd() {
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGesturePerformed(FlicktekCommands.onGestureEvent gestureEvent) {
        Log.d(TAG, "onGesturePerformed: " + gestureEvent.status.toString());
        int gesture = gestureEvent.status;

        switch (gesture) {
            case (FlicktekManager.GESTURE_UP):
                break;

            case (FlicktekManager.GESTURE_DOWN):
                break;
        }
    }

    boolean mRound = false;
    public void setFlat(boolean flat) {
        mRound = !flat;
    }

    public void setRound(boolean round) {
        mRound = round;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGestureRawData(final FlicktekCommands.onGestureRawData gestureData) {
        mRenderer.setGestureRawData(gestureData);
        if (mRound) {
            startRoundEffect();
        } else {
            startFlatEffect();
        }
    }

    public void startRoundEffect() {
        mRenderer.startContinuesRendering(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mRenderer.startRoundEffect();
    }

    public void startFlatEffect() {
        mRenderer.startContinuesRendering(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mRenderer.startFlatEffect();
    }

    public void toogleRender() {
        mRenderer.startContinuesRendering(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mRenderer.toogleRender();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:

                float dx = x - mPreviousX;
                float dy = y - mPreviousY;

                // reverse direction of rotation above the mid-line
                if (y > getHeight() / 2) {
                    dx = dx * -1;
                }

                // reverse direction of rotation to left of the mid-line
                if (x < getWidth() / 2) {
                    dy = dy * -1;
                }

                mRenderer.setAngle(
                        mRenderer.getAngle() +
                                ((dx + dy) * TOUCH_SCALE_FACTOR));  // = 180.0f / 320
                requestRender();
        }

        mPreviousX = x;
        mPreviousY = y;
        return true;
    }

    public void setQuality(Integer quality) {
        this.mQuality = quality;
        mRenderer.setQuality(quality);
    }
}
