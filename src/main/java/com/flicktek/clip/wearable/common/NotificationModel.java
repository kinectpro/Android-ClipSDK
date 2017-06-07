package com.flicktek.clip.wearable.common;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

public class NotificationModel {

    private int id;
    private String title;
    private String text;
    private String key;
    private int messagesCount;
    private Drawable icon;
    private byte[] smallIconBytes;
    private String applicationName;
    private String applicationPackage;
    private long timePosted;
    private boolean isFromWakeLock;

    // In case we don't have the icon, this a google wearable asset so we can query for it.
    private Object asset;
    private boolean selected;

    public NotificationModel(@NonNull String title, @NonNull String text, String key, Drawable icon, Object asset) {
        this.title = title;
        this.text = text;
        this.key = key;
        this.icon = icon;
        this.asset = asset;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getKeyId() {
        return key;
    }

    public void setKey(String id) {
        this.key = id;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Object getAsset() {
        return asset;
    }

    // Call the method on the smartphone to perform the action
    public void performAction(Activity mainActivity) {

    }

    public byte[] getSmallIconBytes() {
        return smallIconBytes;
    }

    public void setSmallIconBytes(byte[] smallIconBytes) {
        this.smallIconBytes = smallIconBytes;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public long getTimePosted() {
        return timePosted;
    }

    public void setTimePosted(long timePosted) {
        this.timePosted = timePosted;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isFromWakeLock() {
        return isFromWakeLock;
    }

    public void setFromWakeLock(boolean fromWakeLock) {
        isFromWakeLock = fromWakeLock;
    }

    public int getMessagesCount() {
        return messagesCount;
    }

    public void setMessagesCount(int messagesCount) {
        this.messagesCount = messagesCount;
    }

    public String getApplicationPackage() {
        return applicationPackage;
    }

    public void setApplicationPackage(String applicationPackage) {
        this.applicationPackage = applicationPackage;
    }
}