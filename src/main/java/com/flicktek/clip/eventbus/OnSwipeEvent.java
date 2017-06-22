package com.flicktek.clip.eventbus;

import com.flicktek.clip.FlicktekManager;

public class OnSwipeEvent {

    public

    @FlicktekManager.SwipeEvents
    int mEvent;

    public OnSwipeEvent(@FlicktekManager.SwipeEvents int event) {
        mEvent = event;
    }
}
