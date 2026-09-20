package com.wapo.flagship;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.wapo.android.commons.util.Logger;

import java.util.LinkedList;

/**
 * Created by elamgodilj on 6/22/16.
 * <p>
 * Class for requesting dynamic permissions
 * <p>
 * https://developer.android.com/training/permissions/requesting.html
 */

public class RequestPermissions {

    private static final String TAG = RequestPermissions.class.getSimpleName();

    public final static String[] DANGEROUS_PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION};

    /**
     * This method will check for a permission.
     * If not available it'll request for the permission
     *
     * @param activity The activity requesting the permission
     * @param PERMISSIONS_REQUEST_CODE Can be used by the calling activity to check whether a specific permission
     * was granted
     */
    public static void checkForPermissions(Activity activity, int PERMISSIONS_REQUEST_CODE) {
        LinkedList<String> permissionRequestQueue = new LinkedList<>();
        for (int i = 0; i < DANGEROUS_PERMISSIONS.length; i++) {
            if (ContextCompat.checkSelfPermission(activity, DANGEROUS_PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                permissionRequestQueue.add(DANGEROUS_PERMISSIONS[i]);
            }
        }
        if (!permissionRequestQueue.isEmpty()) {
            String[] permissionArray = new String[permissionRequestQueue.size()];
            ActivityCompat.requestPermissions(activity,
                    permissionRequestQueue.toArray(permissionArray),
                    PERMISSIONS_REQUEST_CODE);
        }
    }

    public static boolean checkForPermission(Activity activity, String permission, int PERMISSIONS_REQUEST_CODE, String explanationTitle, String explanationDescription) {
        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                if (activity instanceof PermissionsMeasurementActivity) {
                    ((PermissionsMeasurementActivity) activity).notFirstRequest();
                }
                showExplanationDialog(activity, permission, PERMISSIONS_REQUEST_CODE, true, explanationTitle, explanationDescription);
            } else if (isFirstPermissionRequest(activity, permission)) {
                //The user is being asked for camera permission for the first time.
                if (activity instanceof PermissionsMeasurementActivity) {
                    ((PermissionsMeasurementActivity) activity).firstRequest();
                }
                ActivityCompat.requestPermissions(activity, new String[]{permission}, PERMISSIONS_REQUEST_CODE);
                setFirstPermissionRequest(activity, permission, false);
            } else {
                if (activity instanceof PermissionsMeasurementActivity) {
                    ((PermissionsMeasurementActivity) activity).doNotAskBoxChecked();
                }
                showExplanationDialog(activity, permission, PERMISSIONS_REQUEST_CODE, false, explanationTitle, explanationDescription);
            }
            return false;
        } else {
            return true;
        }
    }

    private static void showExplanationDialog(final Activity activity, final String permission, final int PERMISSIONS_REQUEST_CODE, final boolean shouldRequest, String title, String description) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(activity);
        alertBuilder.setCancelable(false);
        alertBuilder.setTitle(title);
        StringBuilder message = new StringBuilder(description);
        if (!shouldRequest) {
            message.append("\nTo update this permission, please go to the Apps page within your device's settings.");
        }
        alertBuilder.setMessage(message.toString());
        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (shouldRequest) {
                    try {
                        ActivityCompat.requestPermissions(activity, new String[]{permission}, PERMISSIONS_REQUEST_CODE);
                    } catch (NullPointerException npe) {
                        Logger.e(TAG, String.format("Error checking for %s permission.", permission), npe);
                    }
                }
            }
        });

        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    private static boolean isFirstPermissionRequest(Context context, String permission) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(permission, true);
    }

    private static void setFirstPermissionRequest(Context context, String permission, boolean requested) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(permission, requested).commit();
    }

    public interface PermissionsMeasurementActivity {
        void firstRequest();
        void notFirstRequest();
        void doNotAskBoxChecked();
    }
}
