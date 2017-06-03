package com.flicktek.clip.eventbus;


public class DeviceToWearEvent {
    public String mac_address;
    public String name;
    public DeviceToWearEvent(String name, String mac_address) {
        this.name = name;
        this.mac_address = mac_address;
    }
}
