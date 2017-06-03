package com.flicktek.clip.eventbus;


public class ConnectingEvent {
    public String macAddress;
    public ConnectingEvent(String mac) {
        macAddress = mac;
    }
}
