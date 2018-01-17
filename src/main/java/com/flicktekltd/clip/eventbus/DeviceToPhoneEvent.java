package com.flicktekltd.clip.eventbus;


public class DeviceToPhoneEvent {
    public String mac_address;
    public String name;
    public DeviceToPhoneEvent(String name, String mac_address) {
        this.name = name;
        this.mac_address = mac_address;
    }
}
