package com.example.yangleduo.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.example.yangleduo.service.KeepAliveService;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.base.Request;


/**
 * 检查更新
 */
public class UpdateApk {

//    private static String PT_URL = "yangLeDuo";
    private static String PT_URL = "xinXin";

    public static void update(Activity context){     //http://47.94.255.103
        HttpClient.getInstance().get("/ptVersion/checkUpdate","http://47.94.255.103")
                .params("ptName",PT_URL)
                .execute(new StringCallback() {
            @Override
            public void onSuccess(Response<String> response) {
                JSONObject versionObj = JSONObject.parseObject(response.body());
                if(versionObj == null){
                    Toast.makeText(context, "没有配置此平台更新信息！", Toast.LENGTH_LONG).show();
                    return;
                }
                String versionCode = versionObj.getString("ptVersion");
                String downloadAdd = versionObj.getString("downUrl");
                String message = versionObj.getString("ptUpdateInfo");
                //获取本地版本号
                String localVersionInfo = getAPPLocalVersion(context);
                int isUpdate = compareVersion(versionCode,localVersionInfo);

                if(isUpdate == 1){
                    new AlertDialog.Builder(context)
                            .setTitle("发现新版本")
                            .setMessage(message)
                            .setCancelable(false) //触摸窗口边界以外是否关闭窗口，设置 false
                            .setNeutralButton("立即更新", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Uri uri = Uri.parse(downloadAdd);
                                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                    context.startActivity(intent);
                                    //销毁service
                                    context.stopService(new Intent(context, KeepAliveService.class));
                                    //销毁activity
                                    context.finish();
                                    // System.exit(0);  //直接用这个全部销毁也可以
                                }
                            }).setPositiveButton("不更新", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //销毁service
                            context.stopService(new Intent(context,KeepAliveService.class));
                            //销毁activity
                            context.finish();
                        }
                    }).show();
                }else {
                    Toast.makeText(context, "已是最新版本！", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Response<String> response) {
                super.onError(response);
                Toast.makeText(context, "检查更新失败！", Toast.LENGTH_LONG).show();
            }
        });

    }



    /**
     * 获取当前apk的版本号
     * @param ctx
     * @return
     */
    public static String getAPPLocalVersion(Context ctx) {
        String appVersionName = null;
        double currentVersionCode = 0;
        PackageManager manager = ctx.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(ctx.getPackageName(), 0);
            appVersionName = info.versionName; // 版本名
            currentVersionCode = info.versionCode; // 版本号
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appVersionName;
    }


    /**
     * 版本号比较
     * 0代表相等，1代表version1大于version2，-1代表version1小于version2
     *
     * @param version1
     * @param version2
     * @return
     */
    public static int compareVersion(String version1, String version2) {
        if (version1.equals(version2)) {
            return 0;
        }
        String[] version1Array = version1.split("\\.");
        String[] version2Array = version2.split("\\.");
        int index = 0;
        // 获取最小长度值
        int minLen = Math.min(version1Array.length, version2Array.length);
        int diff = 0;
        // 循环判断每位的大小
        while (index < minLen
                && (diff = Integer.parseInt(version1Array[index])
                - Integer.parseInt(version2Array[index])) == 0) {
            index++;
        }
        if (diff == 0) {
            // 如果位数不一致，比较多余位数
            for (int i = index; i < version1Array.length; i++) {
                if (Integer.parseInt(version1Array[i]) > 0) {
                    return 1;
                }
            }

            for (int i = index; i < version2Array.length; i++) {
                if (Integer.parseInt(version2Array[i]) > 0) {
                    return -1;
                }
            }
            return 0;
        } else {
            return diff > 0 ? 1 : -1;
        }
    }

}
