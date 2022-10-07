package com.example.yangleduo;


import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.yangleduo.service.KeepAliveService;
import com.example.yangleduo.util.HttpClient;
import com.example.yangleduo.util.NotificationSetUtil;
import com.example.yangleduo.util.UpdateApk;
import com.example.yangleduo.util.WindowPermissionCheck;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;


/**
 * 停止接单取消所有网络请求
 * 远程公告、频率等
 * try catch
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private EditText etUname,etPaw;
    private TextView tvStart,tvStop,tvLog,tvBrow;
    private Handler mHandler;
    private String token;
    /*
    接单成功音乐提示播放次数（3次）
    播放的次数是count+1次
     */
    private int count;
    private SharedPreferences userInfo;
    private static String LOGIN_URL = "";
    private static String BROW_OPEN = "";
    private Dialog dialog;
    private int minPl;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去掉标题栏
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, KeepAliveService.class);
        //启动保活服务
        startService(intent);
        ignoreBatteryOptimization();//忽略电池优化

        if(!checkFloatPermission(this)){
            //权限请求方法
            requestSettingCanDrawOverlays();
        }

        initView();
    }

    private void initView(){
        //检查更新
        UpdateApk.update(MainActivity.this);
        //是否开启通知权限
        openNotification();
        //是否开启悬浮窗权限
        WindowPermissionCheck.checkPermission(this);
        //获取平台地址
        getPtAddress();
        mHandler = new Handler();
        tvBrow = findViewById(R.id.tv_brow);
        etUname = findViewById(R.id.et_username);
        etPaw = findViewById(R.id.et_password);
        tvStart = findViewById(R.id.tv_start);
        tvStop = findViewById(R.id.tv_stop);
        tvLog = findViewById(R.id.tv_log);
        //读取用户信息
        getUserInfo();
        //设置textView为可滚动方式
        tvLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        tvLog.setTextIsSelectable(true);
        tvStart.setOnClickListener(this);
        tvStop.setOnClickListener(this);
        tvBrow.setOnClickListener(this);
        tvLog.setText("本金一般72小时内返款完成~");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.tv_start:

                mHandler.removeCallbacksAndMessages(null);

                if(LOGIN_URL == ""){
                    tvLog.setText("获取最新网址中,请3秒后重试...");
                }else {
                    login(etUname.getText().toString(),etPaw.getText().toString());
                }
                break;
            case R.id.tv_stop:
                stop();
                break;
            case R.id.tv_brow:
                if(LOGIN_URL == ""){
                    tvLog.setText("获取最新网址中,请3秒后重试...");
                }else {
                    browOpen();
                }
                break;
        }

    }

    private void browOpen(){
        Uri uri = Uri.parse(BROW_OPEN);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    /**
     * 重写activity的onKeyDown方法，点击返回键后不销毁activity
     * 可参考：https://blog.csdn.net/qq_36713816/article/details/71511860
     * 另外一种解决办法：重写onBackPressed方法，里面不加任务内容，屏蔽返回按钮
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }




    public void getPtAddress(){

        HttpClient.getInstance().get("/ptVersion/checkUpdate","http://47.94.255.103")
                .params("ptName","yangLeDuo")
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            JSONObject ptAddrObj = JSONObject.parseObject(response.body());
                            if(ptAddrObj == null){
                                Toast.makeText(MainActivity.this, "没有配置此平台更新信息！", Toast.LENGTH_LONG).show();
                                return;
                            }
                            LOGIN_URL = ptAddrObj.getString("ptUrl");
                            BROW_OPEN = ptAddrObj.getString("openUrl");
                            minPl = Integer.parseInt(ptAddrObj.getString("pinLv"));
                            //公告弹窗
                            String[] gongGao = ptAddrObj.getString("ptAnnoun").split(";");
                            announcementDialog(gongGao);

                        }catch (Exception e){
                            sendLog("获取网址："+e.getMessage());
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        sendLog(response.getException().toString());
                    }
                });
    }





    /**
     * 登录平台
     * @param username
     * @param password
     */
    private void login(String username, String password){

        tvLog.setText(new SimpleDateFormat("HH:mm:ss").format(new Date()) + ": 正在登陆中..."+"\n");

        HttpClient.getInstance().post("/user/login", LOGIN_URL)
                .params("account",username)
                .params("password",password)
                .headers("Referer",LOGIN_URL)
                .headers("Content-Type","application/json")
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            JSONObject loginJsonObj = JSONObject.parseObject(response.body());
                            if("0".equals(loginJsonObj.getString("code"))){
                                //保存账号和密码
                                saveUserInfo(username,password);
                                sendLog("登录成功，正在检查账号状态是否正常...");
                                if("1".equals(loginJsonObj.getJSONObject("data").getString("invite_auth"))){
                                    sendLog("此账号接不到单，请联系师傅处理！");
                                    return;
                                }
                                sendLog("账号状态正常");
                                //获取token
                                token = loginJsonObj.getJSONObject("data").getString("token");
                                getTask();
                                return;
                            }
                            sendLog(loginJsonObj.getString("msg"));
                        }catch (Exception e){
                            sendLog("登录平台："+e.getMessage());
                        }
                    }
                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        sendLog("登录ERR:"+response.getException().toString());
                    }
                });
    }



    public void getTask(){
        HttpClient.getInstance().post("/task",LOGIN_URL)
                .headers("Referer",LOGIN_URL)
                .headers("Auth-Token",token)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            String resp = response.body();
                            if(resp.contains("!DOCTYPE")){
                                sendLog("未知错误,请过几分钟重试...");
                                jieDan();
                                return;
                            }
                            JSONObject taskObj = JSONObject.parseObject(response.body());
                            int code = taskObj.getInteger("code");
                            //{"code":40001,"msg":"抱歉，暂时没有可接的任务","url":"\/task"}
                            //{"code":40009,"msg":"每天最多只能领取2次任务","url":"\/task"}
                            //{"code":20038,"msg":"请先前往【个人中心】设置收款信息 再领取任务","url":"\/task"}
                            //{"code":20037,"msg":"请先前往【个人中心】设置收货地址后 再领取任务","url":"\/task"}
                            //{"code":40012,"msg":"亲，请先完成当前未完成的任务哦","url":"\/task"}
                            if(0 == code){
                                getTaskInfo();
                            }else if(40009 == code || 40012 == code || 20037 == code || 20038 == code){
                                sendLog(taskObj.getString("msg"));
                                playMusic(R.raw.fail,3000,0);
                            }else {
                                sendLog(taskObj.getString("msg"));
                                jieDan();
                            }
                        }catch (Exception e){
                            sendLog("getTask："+e.getMessage());
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        sendLog("getTask："+response.getException().toString());
                        jieDan();
                    }
                });
    }



    public void jieDan(){
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getTask();
            }
        }, minPl);
    }



    public void getTaskInfo(){
        HttpClient.getInstance().get("/task/sales/list?per_page=8&page=1&tab_index=0",LOGIN_URL)
                .headers("Referer",LOGIN_URL)
                .headers("Auth-Token",token)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            JSONObject obj = JSONObject.parseObject(response.body());
                            JSONArray jsonArray = obj.getJSONObject("data").getJSONArray("list");
                            JSONObject taskObj = jsonArray.getJSONObject(0);
//                        Double comm = Double.parseDouble(taskObj.getString("brokerage"));
                            playMusic(R.raw.success,3000,2);
                            sendLog(obj.getString("msg"));
                            sendLog("-------------------------------");
                            sendLog("任务关键词："+taskObj.getString("goods_key"));
                            sendLog("-------------------------------");
                            getShangPinUrl(taskObj.getString("order_id"));
                            // cancel(taskObj.getString("order_id"));
                        }catch (Exception e){
                            sendLog("getTaskInfo："+e.getMessage());
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        sendLog("任务详情出错~"+response.getException());
                    }
                });
    }



    public void getShangPinUrl(String taskId){
        HttpClient.getInstance().get("/task/sales/"+taskId,LOGIN_URL)
                .headers("Referer",LOGIN_URL)
                .headers("Auth-Token",token)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            JSONObject obj = JSONObject.parseObject(response.body());
                            String url = obj.getJSONObject("data").getJSONObject("goods").getString("main_img");
                            sendLog("商品图（复制网址浏览器打开）："+url);
                        }catch (Exception e){
                            sendLog("getShangPinUrl:"+e.getMessage());
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        sendLog("获取任务链接出错啦,正在重试~");
                    }
                });
    }


    public void cancel(String taskId){
        HttpClient.getInstance().post("/task/cancel",LOGIN_URL)
                .params("cancelCat","4")
                .params("cancelRemark","")
                .params("order_id",taskId)
                .headers("Referer",LOGIN_URL)
                .headers("Auth-Token",token)
                .headers("Content-Type","application/json")
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        jieDan();
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        sendLog("取消任务详情出错啦,正在重试~");
                        jieDan();
                    }
                });
    }



    private void openNotification(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //判断是否需要开启通知栏功能
            NotificationSetUtil.OpenNotificationSetting(this);
        }
    }



    //权限打开
    private void requestSettingCanDrawOverlays() {
        int sdkInt = Build.VERSION.SDK_INT;
        if (sdkInt >= Build.VERSION_CODES.O) {//8.0以上
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivityForResult(intent, 1);
        } else if (sdkInt >= Build.VERSION_CODES.M) {//6.0-8.0
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1);
        } else {//4.4-6.0以下
            //无需处理了
        }
    }


    //判断是否开启悬浮窗权限   context可以用你的Activity.或者tiis
    public static boolean checkFloatPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return true;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                Class cls = Class.forName("android.content.Context");
                Field declaredField = cls.getDeclaredField("APP_OPS_SERVICE");
                declaredField.setAccessible(true);
                Object obj = declaredField.get(cls);
                if (!(obj instanceof String)) {
                    return false;
                }
                String str2 = (String) obj;
                obj = cls.getMethod("getSystemService", String.class).invoke(context, str2);
                cls = Class.forName("android.app.AppOpsManager");
                Field declaredField2 = cls.getDeclaredField("MODE_ALLOWED");
                declaredField2.setAccessible(true);
                Method checkOp = cls.getMethod("checkOp", Integer.TYPE, Integer.TYPE, String.class);
                int result = (Integer) checkOp.invoke(obj, 24, Binder.getCallingUid(), context.getPackageName());
                return result == declaredField2.getInt(cls);
            } catch (Exception e) {
                return false;
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AppOpsManager appOpsMgr = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                if (appOpsMgr == null)
                    return false;
                int mode = appOpsMgr.checkOpNoThrow("android:system_alert_window", android.os.Process.myUid(), context
                        .getPackageName());
                return mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_IGNORED;
            } else {
                return Settings.canDrawOverlays(context);
            }
        }
    }



    /**
     * 停止接单
     */
    public void stop(){
        OkGo.getInstance().cancelAll();
        //Handler中已经提供了一个removeCallbacksAndMessages去清除Message和Runnable
        mHandler.removeCallbacksAndMessages(null);
        sendLog("已停止接单");
    }


    /**
     * 接单成功后通知铃声
     * @param voiceResId 音频文件
     * @param milliseconds 需要震动的毫秒数
     */
    private void playMusic(int voiceResId, long milliseconds,int total){

        count = total;//不然会循环播放

        //播放语音
        MediaPlayer player = MediaPlayer.create(MainActivity.this, voiceResId);
        player.start();
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                //播放完成事件
                if(count != 0){
                    player.start();
                }
                count --;
            }
        });

        //震动
        Vibrator vib = (Vibrator) this.getSystemService(Service.VIBRATOR_SERVICE);
        //延迟的毫秒数
        vib.vibrate(milliseconds);
    }

    /**
     * 日志更新
     * @param log
     */
    public void sendLog(String log){
        scrollToTvLog();
        tvLog.append(new SimpleDateFormat("HH:mm:ss").format(new Date()) + ": "+log+"\n");
    }


    /**
     * 忽略电池优化
     */

    public void ignoreBatteryOptimization() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        boolean hasIgnored = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            hasIgnored = powerManager.isIgnoringBatteryOptimizations(getPackageName());
            //  判断当前APP是否有加入电池优化的白名单，如果没有，弹出加入电池优化的白名单的设置对话框。
            if(!hasIgnored) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:"+getPackageName()));
                startActivity(intent);
            }
        }
    }


    /**
     * 弹窗公告
     */
    public void announcementDialog(String[] lesson){

        dialog = new AlertDialog
                .Builder(this)
                .setTitle("公告")
                .setCancelable(false) //触摸窗口边界以外是否关闭窗口，设置 false
                .setPositiveButton("我知道了", null)
                //.setMessage("")
                .setItems(lesson,null)
                .create();
        dialog.show();
    }


    /**
     * 保存用户信息
     */
    private void saveUserInfo(String username,String password){

        userInfo = getSharedPreferences("userData", MODE_PRIVATE);
        SharedPreferences.Editor editor = userInfo.edit();//获取Editor
        //得到Editor后，写入需要保存的数据
        editor.putString("username",username);
        editor.putString("password", password);
        editor.commit();//提交修改

    }

    /**
     * 读取用户信息
     */
    private void getUserInfo(){
        userInfo = getSharedPreferences("userData", MODE_PRIVATE);
        String username = userInfo.getString("username", null);//读取username
        String passwrod = userInfo.getString("password", null);//读取password
        if(username!=null && passwrod!=null){
            etUname.setText(username);
            etPaw.setText(passwrod);
        }
    }


    public void scrollToTvLog(){
        int tvHeight = tvLog.getHeight();
        int tvHeight2 = getTextViewHeight(tvLog);
        if(tvHeight2>tvHeight){
            tvLog.scrollTo(0,tvHeight2-tvLog.getHeight());
        }
    }



    private int getTextViewHeight(TextView textView) {
        Layout layout = textView.getLayout();
        int desired = layout.getLineTop(textView.getLineCount());
        int padding = textView.getCompoundPaddingTop() +
                textView.getCompoundPaddingBottom();
        return desired + padding;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //关闭弹窗，不然会 报错（虽然不影响使用）
        dialog.dismiss();
    }
}