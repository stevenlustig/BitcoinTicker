package com.stevenlustig.bitcointicker;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Usage:
 * Your Activity should call {@link PermissionUtil#doWithPermission(Activity, int, String[], boolean, PermissionResultCallback)}
 * Your Activity should also redirect its {@link Activity#onRequestPermissionsResult(int, String[], int[])} to the method here, {@link PermissionUtil#onRequestPermissionsResult(Activity, int, String[], int[])} 
 * Your Activity should also redirect its {@link Activity#onResume()} to the method here, {@link PermissionUtil#onResume(Activity)} 
 */
public class PermissionUtil {
    private static List<PendingPermissionRequest> mPendingPermissionRequests = new ArrayList<>();
    private static List<PendingPermissionRequest> mPendingResumeRequests = new ArrayList<>();

    /**
     * @param activity - the calling Activity
     * @param requestCode - A request code. You will pass this same requestCode to {@link PermissionUtil#onRequestPermissionsResult(Activity, int, String[], int[])}
     * @param permissions - List of permissions you are requesting. See {@link android.Manifest.permission}
     * @param promptIfPermanentlyDenied - Use this sparingly. If the user permanently denied the permission and this is set to true, the user will be redirected to preferences to re-enable the permission.
     *                                  Only use this for a task which the user just explicitly clicked on.
     * @param callback - callbacks for GRANTED and DENIED
     */
    public static void doWithPermission(final Activity activity, int requestCode, String[] permissions, boolean promptIfPermanentlyDenied, final PermissionResultCallback callback) {
        final List<String> deniedPermissions = new ArrayList<>();

        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permission);
            }
        }

        if (deniedPermissions.size() == 0) {
            callback.onGranted(activity);
        }
        else {
            // Keep track of the shouldShowRequestPermissionRationale() status prior to requesting permission
            // This will help determine if the user has checked off "don't ask again"
            Map<String, Boolean> shouldShowPermissionRationales = new HashMap<>();
            for (String permission : permissions) {
                shouldShowPermissionRationales.put(permission, ActivityCompat.shouldShowRequestPermissionRationale(activity, permission));
            }

            mPendingPermissionRequests.add(new PendingPermissionRequest(activity, requestCode, permissions, shouldShowPermissionRationales, promptIfPermanentlyDenied, callback));
            ActivityCompat.requestPermissions(activity, deniedPermissions.toArray(new String[0]), requestCode);
        }
    }

    /**
     * Your activity needs to redirect it's Activity#onRequestPermissionsResult(int, String[], int[])} to here
     */
    public static void onRequestPermissionsResult(final Activity activity, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Match up pending request(s)
        List<PendingPermissionRequest> permissionRequests = new ArrayList<>();
        for (PendingPermissionRequest pendingPermissionRequest : mPendingPermissionRequests) {
            if (pendingPermissionRequest.mRequestCode == requestCode) {
                permissionRequests.add(pendingPermissionRequest);
            }
        }
        mPendingPermissionRequests.removeAll(permissionRequests);

        for (final PendingPermissionRequest permissionRequest : permissionRequests) {
            final List<String> deniedPermissions = new ArrayList<>();
            List<String> permanentlyDeniedPermissions = new ArrayList<>();

            for (int i=0; i<permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    deniedPermissions.add(permissions[i]);
                }
            }

            for (String permission : deniedPermissions) {
                Boolean previouslyShouldShowRequestPermissionRationale = permissionRequest.mShouldShowRequestPermissionRationales.get(permission);

                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) && Boolean.FALSE.equals(previouslyShouldShowRequestPermissionRationale)) {
                    permanentlyDeniedPermissions.add(permission);
                }
            }

            if (deniedPermissions.size() == 0) {
                // All permissions granted
                permissionRequest.mCallback.onGranted(activity);
            }
            else if (permanentlyDeniedPermissions.size() == 0 || !permissionRequest.mPromptIfPermanentlyDenied) {
                // Permissions just denied, or we don't want to bother the user
                permissionRequest.mCallback.onDenied(deniedPermissions.toArray(new String[0]));
            }
            else {
                // Permissions were permanently denied prior to this check. Redirect the user to Settings
                ArrayList<String> promptForPermissions = new ArrayList<>();
                for (String permission : deniedPermissions) {
                    promptForPermissions.add(permission.replace("android.permission.", ""));
                }

                new AlertDialog.Builder(activity)
                        .setTitle(R.string.permissions_required_title)
                        .setMessage(activity.getString(R.string.permissions_required_message, TextUtils.join(", ", promptForPermissions)))
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                permissionRequest.mCallback.onDenied(deniedPermissions.toArray(new String[0]));
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                permissionRequest.mCallback.onDenied(deniedPermissions.toArray(new String[0]));
                            }
                        })
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mPendingResumeRequests.add(new PendingPermissionRequest(activity, permissionRequest.mRequestCode, permissionRequest.mPermissions, permissionRequest.mShouldShowRequestPermissionRationales, permissionRequest.mPromptIfPermanentlyDenied, permissionRequest.mCallback));

                                Uri uri = Uri.parse("package:" + activity.getPackageName());
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri);
                                intent.addCategory(Intent.CATEGORY_DEFAULT);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                activity.startActivity(intent);
                            }
                        })
                        .show();
            }

            permissionRequest.mCallback.onDenied(deniedPermissions.toArray(new String[0]));
        }
    }

    public static void onResume(Activity activity) {
        // Match up pending request(s)
        List<PendingPermissionRequest> permissionRequests = new ArrayList<>();
        for (PendingPermissionRequest pendingPermissionRequest : mPendingResumeRequests) {
            if (pendingPermissionRequest.mActivityClass == activity.getClass()) {
                permissionRequests.add(pendingPermissionRequest);
            }
        }
        mPendingResumeRequests.removeAll(permissionRequests);

        for (PendingPermissionRequest permissionRequest : permissionRequests) {
            doWithPermission(activity, permissionRequest.mRequestCode, permissionRequest.mPermissions, permissionRequest.mPromptIfPermanentlyDenied, permissionRequest.mCallback);
        }
    }

    private static class PendingPermissionRequest {
        private Class<?> mActivityClass;
        private int mRequestCode;
        private String[] mPermissions;
        private Map<String, Boolean> mShouldShowRequestPermissionRationales;
        private boolean mPromptIfPermanentlyDenied;
        private PermissionResultCallback mCallback;

        private PendingPermissionRequest(Activity activity, int requestCode, String[] permissions, Map<String, Boolean> shouldShowRequestPermissionRationales ,boolean promptIfPermanentlyDenied, PermissionResultCallback callback) {
            this.mActivityClass = activity.getClass();
            this.mRequestCode = requestCode;
            this.mPermissions = permissions;
            this.mShouldShowRequestPermissionRationales = shouldShowRequestPermissionRationales;
            this.mPromptIfPermanentlyDenied = promptIfPermanentlyDenied;
            this.mCallback = callback;
        }
    }

    /**
     * Be sure to use "useThisActivity" to ensure that you are working with the most recent active Activity
     * Other references to Activity may refer to a destroyed Activity
     */
    interface PermissionResultCallback {
        void onDenied(String[] deniedPermissions);
        void onGranted(Activity useThisActivity);
    }
}
