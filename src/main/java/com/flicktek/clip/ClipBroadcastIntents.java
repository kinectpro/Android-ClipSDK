package com.flicktek.clip;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class ClipBroadcastIntents {
    private static final String TAG = "ClipBroadcast";

    public static final String ACTION_URI_SEND = "com.flicktek.clip.ACTION_URI_SEND";
    public static final String ACTION_URI_GESTURE = "com.flicktek.clip.ACTION_GESTURE";
    public static final String ACTION_URI_WAKEUP = "com.flicktek.clip.ACTION_WAKEUP";

    public static final String EXTRA_URI = "com.flicktek.clip.EXTRA_URI";

    public static final String EXTRA_GESTURE_ENTER = "ENTER";
    public static final String EXTRA_GESTURE_HOME = "HOME";
    public static final String EXTRA_GESTURE_UP = "UP";
    public static final String EXTRA_GESTURE_DOWN = "DOWN";
    public static final String EXTRA_GESTURE_UNKNOWN = "UNKNOWN";

    public static void openIntent(Activity activity, String intentString,
                                  int requestCode) {
        Intent intent = new Intent(intentString);

        // verify that the intent resolves
        PackageManager packageManager = activity.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
        boolean isIntentSafe = activities.size() > 0;

        if (isIntentSafe) {
            activity.startActivityForResult(intent, requestCode);
        } else {
            Toast.makeText(activity, "Application was not installed...", Toast.LENGTH_LONG).show();
        }
    }

    public static String result(Intent data) {
        if (data != null)
            if (data.hasExtra(EXTRA_URI))
                return data.getStringExtra(EXTRA_URI);
        return "";
    }

    public static void openBroadcastIntent(Context context, String action, String uri) {
        context.sendBroadcast(new Intent(action).putExtra(EXTRA_URI, uri));
    }

    public static void openBroadcastIntent(Context context, String package_name,
                                           String extra_command, String command) {
        Intent i = new Intent(package_name);
        i.putExtra(extra_command, command);
        context.sendBroadcast(i);
    }

    public static void openBroadcastGesture(Context context, int value) {
        if (context == null) {
            Log.v(TAG, "No context to send gesture broadcast");
            return;
        }

        Log.v(TAG, "openBroadcastGesture " + value);
        try {
            String gesture = "";
            switch (value) {
                case 1:
                    gesture = EXTRA_GESTURE_ENTER;
                    break;
                case 2:
                    gesture = EXTRA_GESTURE_HOME;
                    break;
                case 3:
                    gesture = EXTRA_GESTURE_UP;
                    break;
                case 4:
                    gesture = EXTRA_GESTURE_DOWN;
                    break;
                default:
                    gesture = EXTRA_GESTURE_UNKNOWN;
                    break;
            }

            ClipBroadcastIntents.openBroadcastIntent(context, ClipBroadcastIntents.ACTION_URI_GESTURE, gesture);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
