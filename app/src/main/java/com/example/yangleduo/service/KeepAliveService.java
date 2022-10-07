package com.example.yangleduo.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.yangleduo.MainActivity;
import com.example.yangleduo.R;


/**
 * 前台保活服务
 * 参考地址：https://www.bilibili.com/read/cv11826368
 */
public class KeepAliveService extends Service {

    private MediaPlayer mMediaPlayer;
    private AudioManager mAudioManager;
    private PowerManager.WakeLock wakeLock;

    //服务通信
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //与Activity进行通信
        return null;
    }

    //服务创建时
    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onCreate() {
        super.onCreate();
        //服务创建时创建前台通知
        Notification notification = createForegroundNotification();
        //启动前台服务
        startForeground(1,notification);
        //有业务逻辑的代码可写在onCreate下

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(mAudioFocusChange, 3, 1);

        mMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.silent);
        mMediaPlayer.setLooping(true);
        startPlayMusic();

        if(wakeLock == null) {
            PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    this.getClass().getCanonicalName());
            wakeLock.acquire();
        }
    }


    private void startPlayMusic() {
        MediaPlayer mediaPlayer = mMediaPlayer;
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
    }

    private void stopPlayMusic() {
        MediaPlayer mediaPlayer = mMediaPlayer;
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }


    //创建前台通知，可写成方法体，也可单独写成一个类
    @SuppressLint("WrongConstant")
    private Notification createForegroundNotification(){
        //前台通知的id名，任意
        String channelId = "yld";
        //前台通知的名称，任意
        String channelName = "保持后台运行";
        //发送通知的等级，此处为高，根据业务情况而定
        int importance = NotificationManager.IMPORTANCE_HIGH;
        //判断Android版本，不同的Android版本请求不一样，以下代码为官方写法
        //Android 版本号对应的SDK版本：https://blog.csdn.net/keke921231/article/details/105858555
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(channelId,channelName,importance);
            channel.setLightColor(Color.BLUE);
            //设置锁屏显示通知
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);//任何情况下都会显示通知
            NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        //点击通知时可进入的Activity
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,0);

        //最终创建的通知，以下代码为官方写法
        //注释部分是可扩展的参数，根据自己的功能需求添加
        //可参考：https://www.cnblogs.com/stars-one/p/8371051.html
        return new NotificationCompat.Builder(this,channelId)
                .setContentTitle("养乐多助手")
                .setContentText("运行中...")
                .setSmallIcon(R.mipmap.index)//通知显示的图标
                .setContentIntent(pendingIntent)//点击通知进入Activity
                .setPriority(NotificationCompat.PRIORITY_MAX) //设置通知的优先级为最大
                .setCategory(Notification.CATEGORY_TRANSPORT) //设置通知类别
                .setOngoing(true) //设置它为一个正在进行的通知，通常表示一个后台任务
                .setVisibility(Notification.VISIBILITY_PUBLIC)  //控制锁定屏幕中通知的可见详情级别
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.index))  //设置下拉列表中的图标（大图标）
                .build();
    }


    private AudioManager.OnAudioFocusChangeListener mAudioFocusChange = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int i) {
            if (i == -2) {
                return;
            }
            if (i == -1) {
                mAudioManager.abandonAudioFocus(mAudioFocusChange);
            } else if (i == 1) {
                try {
                    startPlayMusic();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };



    //服务销毁时
    @Override
    public void onDestroy() {
        super.onDestroy();
        //在服务被销毁时，关闭前台服务
        stopForeground(true);
        stopPlayMusic();
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }
}
