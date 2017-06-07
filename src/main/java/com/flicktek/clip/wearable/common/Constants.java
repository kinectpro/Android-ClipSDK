/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.flicktek.clip.wearable.common;

/**
 * Constants used for exchanging data and messages between handheld and Android Wear devices.
 */
public final class Constants {


    /**
     * Base path for all messages between the handheld and wearable.
     */
    private static final String BASE_PATH = "/flicktek";

    /**
     * Action sent from a wearable to disconnect from a device. It must have the profile name set as data.
     */
    public static final String ACTION_DISCONNECT = BASE_PATH + "/disconnect";

    /**
     * Constants for the Clip
     */

    public static final class FLICKTEK_CLIP {
        public static final String START_ACTIVITY_PATH = "/start-activity";

        public static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";

        public static final String GESTURE = "/gesture";
        public static final String COUNT_PATH = "/count";

        public static final String IMAGE_PATH = "/image";
        public static final String IMAGE_KEY = "photo";
        public static final String COUNT_KEY = "count";

        // The wearable wants to perform an action
        public static final String LAUNCH_ACTIVITY = "/activity";
        public static final String LAUNCH_FRAGMENT = "/fragment";
        public static final String LAUNCH_INTENT = "/intent";
        public static final String UNIFIED_INTENT = "/unified";

        /* media constants*/
        public static final String VOLUME_ACTION = "/volume";
        public static final String VOLUME_UP = "volumeup";
        public static final String VOLUME_DOWN = "volumedown";
        public static final String GET_VOLUME = "get_volume";
        public static final String VOLUME_AMPLITUDE = "volume_amplitude";
        public static final String IS_MUSIC_PLAYING_PATH = "is_music_playing_path";
        public static final String IS_MUSIC_PLAYING = "is_music_playing";

        // Reports for analytics and general usage
        public static final String BATTERY = "/battery";
        public static final String DEVICE_MAC_ADDRESS = "/mac_address";
        public static final String DEVICE_CONNECTION_STATE = "/connection_state";
        public static final String DEVICE_STATE = "/device_state";

        public static final String ANALYTICS_SCREEN = "/analytics";
        public static final String ANALYTICS_CALIBRATION = "/analytics_device_calibration";

        // Phone interface, are we calling?, reject, answer, call!
        public static final String PHONE_OUTGOING_CALL_STARTED = "/phone_outgoing_call_started";
        public static final String PHONE_OUTGOING_CALL_ENDED = "/phone_outgoing_call_ended";
        public static final String PHONE_INCOMING_CALL_ENDED = "/phone_incoming_call_ended";
        public static final String PHONE_INCOMING_CALL_ANSWERED = "/phone_incoming_call_answered";
        public static final String PHONE_INCOMING_CALL_RECEIVED = "/phone_incoming_call_received";
        public static final String PHONE_MISSED_CALL = "/phone_missed_call";
        public static final String PHONE_ACCEPT_CALL = "/phone_accept_call";

        // Notifications
        public static final int NOTIFICATION_WATCH_ID = 1;
        public static final String NOTIFICATION_PATH = "/notification_path";
        public static final String NOTIFICATION_ID = "notification-id";
        public static final String NOTIFICATION_KEY_ID = "notification-key-id";
        public static final String NOTIFICATION_KEY_TITLE = "title";
        public static final String NOTIFICATION_KEY_APPLICATION_NAME = "application_name";
        public static final String NOTIFICATION_KEY_TIME_POSTED = "time_posted";
        public static final String NOTIFICATION_KEY_CONTENT = "content";
        public static final String NOTIFICATION_KEY_BITMAP_ASSET = "asset";
        public static final String NOTIFICATION_KEY_SMALL_BITMAP_PARCELABLE = "small_icon_bytes";
        public static final String NOTIFICATION_ACTION_DISMISS = "com.flicktek.clip.notifications.DISMISS";
        public static final String NOTIFICATION_REMOVE_PATH = "/notification_remove";
        public static final String NOTIFICATION_REMOVE_ID = "/notification_remove_id";
        public static final String UNBLOCK_NOTIFICATION_ROUTE = "/unblock_notification_path";
        public static final String UNBLOCK_NOTIFICATION_KEY= "unblock_notification_key";

        public static final String BLOCKED_NOTIFICATION_KEY = "blocked_notification_key";
        public static final String NOTIFICATION_PACKAGE_KEY = "package";
        public static final String NOTIFICAION_APPLICATION_NAME_KEY = "application_name_key";
        public static final String NOTIFICAION_APPLICATION_NAME_KEY_ID = "application_name_key_id";
        public static final String NOTIFICATION_IS_BLOCKED_KEY = "is_blocked_key";
        public static final String GETTING_BLOCKED_NOTIFICATION_REQUEST_TO_WATCH = "getting_blocked_notification_request_to_watch";
        public static final String GETTING_BLOCKED_NOTIFICATION_REQUEST_TO_PHONE = "getting_blocked_notification_request_to_phone";
        public static final String NOTIFICATION_CALLING = "notification_calling";
        public static final String NOTIFICATION_PACKAGE = "notification_package";
        public static final String RAW_SENSOR_GESTURE = "/sensor_raw_data";
        public static final String RAW_GESTURE = "raw_compressed";
        public static final String CANCEL_NOTIFICATION = "cancel_notification";
        public static final String NOTIFICATION_KEY_EXTRA_TEXT = "extra_text";
        public static final String NOTIFICATION_KEY_EXTRA_SUB_TEXT = "extra_sub_text";
        public static final String NOTIFICATION_KEY_EXTRA_BIG_TEXT = "extra_bit_text";

        // Settings set on the FlicktekSetting
        public static final String APPLICATION_SETTING = "/app_setting";

        public static final String FITNESS_UNITS_PATH = "fitness_units_path";

        // Tells the phone to disconnect from CLIP
        public static final String DEVICE_PHONE_DISCONNECT = "/phone_disconnect";
    }

    /**
     * Constants for the UART profile.
     */
    public static final class UART {
        /**
         * The profile name.
         */
        public static final String PROFILE = "uart";

        /**
         * Base path for UART messages between the handheld and wearable.
         */
        private static final String PROFILE_PATH = BASE_PATH + "/uart";

        /**
         * An UART device is connected.
         */
        public static final String DEVICE_CONNECTED = PROFILE_PATH + "/connected";
        /**
         * An UART device is disconnected.
         */
        public static final String DEVICE_DISCONNECTED = PROFILE_PATH + "/disconnected";
        /**
         * An UART device is disconnected due to a link loss.
         */
        public static final String DEVICE_LINKLOSS = PROFILE_PATH + "/link_loss";
        /**
         * Path used for syncing UART configurations.
         */
        public static final String CONFIGURATIONS = PROFILE_PATH + "/configurations";
        /**
         * An action with a command was clicked.
         */
        public static final String COMMAND = PROFILE_PATH + "/command";

        public static final class Configuration {
            public static final String NAME = "name";
            public static final String COMMANDS = "commands";

            public static final class Command {
                public static final String ICON_ID = "icon_id";
                public static final String MESSAGE = "message";
                public static final String EOL = "eol";
            }
        }
    }

    public static final class IFTTT {
        public static final String IFTTT_PATH_TO_WATCH_DATA = "ifttt_path_to_watch";
        public static final String IFTTT_PATH_TO_WATCH_STATUS = "ifttt_path_to_watch_status";
        public static final String IFTTT_PATH_FROM_WATCH = "ifttt_path_from_watch";

        public static final String IFTTT_STATUS_FAILURE = "Request failure";
        public static final String IFTTT_STATUS_SUCCESS = "Request success";

        public static final String IFTTT_STATUS_GETTING_DATA = "getting_data_to_wear";
    }
}
