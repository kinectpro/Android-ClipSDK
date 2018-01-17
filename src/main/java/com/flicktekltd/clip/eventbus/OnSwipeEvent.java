package com.flicktekltd.clip.eventbus;

import com.flicktekltd.clip.FlicktekManager;

public class OnSwipeEvent {

    public

    @FlicktekManager.SwipeEvents
    int mEvent;

    public OnSwipeEvent(@FlicktekManager.SwipeEvents int event) {
        mEvent = event;
    }
}
