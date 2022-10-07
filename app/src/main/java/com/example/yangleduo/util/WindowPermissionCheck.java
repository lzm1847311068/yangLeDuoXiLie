package com.example.yangleduo.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

public class WindowPermissionCheck {

    public static void checkPermission(Activity activity){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {

            new AlertDialog.Builder(activity)
                    .setTitle("请手动将悬浮窗权限打开")
                    .setMessage("请开启悬浮窗权限,不然软件可能无法后台运行")
                    .setCancelable(false) //触摸窗口边界以外是否关闭窗口，设置 false
                    .setNeutralButton("不开启", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    }).setPositiveButton("开启", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Uri uri = Uri.parse("package:" + activity.getPackageName());
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri);
                    activity.startActivityForResult(intent, 0);
                }
            }).show();
        }
    }

}
