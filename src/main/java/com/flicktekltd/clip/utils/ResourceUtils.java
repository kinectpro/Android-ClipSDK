package com.flicktekltd.clip.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * @author Dmytri on 31.05.2017.
 */

public class ResourceUtils {

    private static final String TAG = ResourceUtils.class.getSimpleName();

    public static Drawable getIconFromPackageName(String packageName, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(packageName, 0);
            Context otherAppCtx = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
            int displayMetrics[] = {DisplayMetrics.DENSITY_XHIGH, DisplayMetrics.DENSITY_HIGH, DisplayMetrics.DENSITY_TV};

            for (int displayMetric : displayMetrics) {
                try {
                    Drawable d = otherAppCtx.getResources().getDrawableForDensity(pi.applicationInfo.icon, displayMetric);
                    if (d != null) {
                        return d;
                    }
                } catch (Resources.NotFoundException e) {
                    Log.d(TAG, "NameNotFound for" + packageName + " @ density: " + displayMetric);
                    continue;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Handle Error here
        }
        ApplicationInfo appInfo = null;
        try {
            appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
        return appInfo.loadIcon(pm);
    }
}