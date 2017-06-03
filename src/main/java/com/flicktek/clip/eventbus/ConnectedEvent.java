package com.flicktek.clip.eventbus;


public class ConnectedEvent {
    public String mac_address;
    public String name;
    public ConnectedEvent(String name, String mac_address) {
        this.name = name;
        this.mac_address = mac_address;
    }
}
