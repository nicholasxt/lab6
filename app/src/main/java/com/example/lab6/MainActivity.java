package com.example.lab6;


import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private TextView musicStatus, musicTime, musicTotal;
    private SeekBar seekBar;

    private Button btnPlayOrPause, btnStop, btnQuit;
    private SimpleDateFormat time = new SimpleDateFormat("mm:ss");
    private MusicService musicService = new MusicService();
    private static boolean hasPermission = false;
    private static boolean isCreate = false;
    private static boolean isStopped = false;
    private ObjectAnimator animator;
    private IBinder mBinder;

    //  回调onServiceConnected 函数，通过IBinder 获取 Service对象，实现Activity与 Service的绑定
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = service;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceConnection = null;
        }
    };
    private void findViewById() {
        isCreate = true;
        musicTime = (TextView) findViewById(R.id.currentTime);
        musicTotal = (TextView) findViewById(R.id.endTime);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        btnPlayOrPause = (Button) findViewById(R.id.play);
        btnStop = (Button) findViewById(R.id.stop);
        btnQuit = (Button) findViewById(R.id.quit);
        musicStatus = (TextView) findViewById(R.id.state);
        seekBar.setProgress(0);
        seekBar.setMax(musicService.musicTotalTime);
        musicTotal.setText(time.format(musicService.musicTotalTime));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(MainActivity.this); //动态获取读取内置存储权限

        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        findViewById();
        myListener();
        MyThread();
    }
    public void onDestroy(){
        super.onDestroy();
        unbindService(serviceConnection);
    }
    //定义一个新线程
    private void MyThread(){
        Thread mThread = new Thread(){
            public void run(){
                while (true){
                    try{
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (serviceConnection!=null && hasPermission == true){
                        mHandler.obtainMessage(123).sendToTarget();
                    }
                }
            }
        };
        mThread.start();
    }
    @SuppressLint("HandlerLeak")
    final Handler mHandler = new Handler(){
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            int timeNow = 0;
            int totaltime = 0;
            int isPlay = 0;
            try{
                int code = 104;
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                mBinder.transact(code,data,reply,0);
                timeNow = reply.readInt();
                totaltime = reply.readInt();
                isPlay = reply.readInt();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            switch (msg.what){
                case 123:
                    if (isCreate){
                        musicStatus.setText(" ");
                    }
                    else if (isStopped){
                        musicStatus.setText("Stopped");
                        btnPlayOrPause.setText("PLAY");
                        animator.pause();
                    }
                    else if (isPlay == 1){
                        musicStatus.setText("Playing");
                        btnPlayOrPause.setText("Paused");
                        if (animator.isRunning()){
                            animator.resume();
                        }
                        else {
                            animator.start();
                        }
                    }
                    else {
                        musicStatus.setText("Paused");
                        btnPlayOrPause.setText("PLAY");
                        animator.pause();
                    }
                    seekBar.setProgress(timeNow);
                    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                           musicTime.setText(time.format(progress));
                        }
                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }
                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                            try{
                                int code = 105;
                                Parcel data = Parcel.obtain();
                                Parcel reply = Parcel.obtain();
                                data.writeInt(seekBar.getProgress());
                                mBinder.transact(code, data, reply, 0);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    });
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    private static int REQUEST_EXTERNAL_STORAGE = 0;
    private static String[] PERMISSIONS_STORAGE = {"android.permission.READ_EXTERNAL_STORAGE"};
    public static void verifyStoragePermissions(Activity activity){
        try {
            //检测是否有读取权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.READ_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED){
                //没有读取的权限，去申请读取的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity,PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
            else {
                hasPermission = true;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    //请求权限会弹出询问框，用户选择后，系统会调用如下回调函数：
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(grantResults.length >0 &&grantResults[0]==PackageManager.PERMISSION_GRANTED){
            //用户同意授权
            startPlaying();
            hasPermission = true;
        }else{
            //用户拒绝授权
            System.exit(0);
        }
        return;
    }
    private void startPlaying(){
        try {
            int code = 106;
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            mBinder.transact(code,data,reply,0);
            reply.recycle();
            data.recycle();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        int timeNow = 0;
        int totalTime = 0;
        try {
            int code = 104;
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            mBinder.transact(code,data,reply,0);
            timeNow = reply.readInt();
            totalTime = reply.readInt();
            reply.recycle();
            data.recycle();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        seekBar.setProgress(timeNow);
        seekBar.setMax(totalTime);
        musicTotal.setText(time.format(totalTime));
    }

    private void myListener() {
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        animator = ObjectAnimator.ofFloat(imageView, "rotation", 0f, 360.0f);
        animator.setDuration(10000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(-1);
        btnPlayOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCreate = false;
                isStopped = false;
                try {
                    int code=101;
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    mBinder.transact(code,data,reply,0);
                }catch (RemoteException e){
                    e.printStackTrace();
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isStopped = true;
                isCreate = false;
                try{
                    int code = 102;
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    mBinder.transact(code,data,reply,0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        //  停止服务时，必须解除绑定，写入btnQuit按钮中
        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unbindService(serviceConnection);
                serviceConnection = null;
                try {
                    int code = 103;
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    mBinder.transact(code,data,reply,0);
                    MainActivity.this.finish();
                    System.exit(0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}


