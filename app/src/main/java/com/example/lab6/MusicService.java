package com.example.lab6;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import java.text.SimpleDateFormat;

import android.os.Parcel;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by 王杏婷 on 2017/11/11.
 */
public class MusicService extends Service {

    public MediaPlayer mediaPlayer=new MediaPlayer();
    public int musicTotalTime;
    public IBinder binder = new MyBinder();
    public IBinder onBind(Intent intent){
        return binder;
    }

    public MusicService() {
        try {
            mediaPlayer.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath()+"/久石譲 - Summer.mp3");
           // mediaPlayer.setDataSource("/data/hw_init/version/region_comm/china/media/Pre-loaded/Music/Honor.mp3");
            mediaPlayer.prepare();
            musicTotalTime = mediaPlayer.getDuration();
            mediaPlayer.setLooping(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public class MyBinder extends Binder{
        protected boolean onTransact(int code, Parcel data,Parcel reply,int flags) throws RemoteException{
            switch (code){
                case 101:
                    playOrPause();
                    break;
                case 102:
                    stop();
                    break;
                case 103:
                    quit();
                    break;
                case 104:
                    //界面刷新，服务返回数据函数
                    int musicTime=mediaPlayer.getCurrentPosition();
                    int totalTime=mediaPlayer.getDuration();
                    int isPlay=0;
                    if (mediaPlayer.isPlaying()){
                        isPlay=1;
                    }
                    reply.writeInt(musicTime);
                    reply.writeInt(totalTime);
                    reply.writeInt(isPlay);
                    return true;
                case 105:
                    //拖动进度条
                    TrackingTouch(data.readInt());
                    break;
                case 106:
                    try {
                        mediaPlayer.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath()+"/久石譲 - Summer.mp3");
                        //mediaPlayer.setDataSource("/data/hw_init/version/region_comm/china/media/Pre-loaded/Music/Honor.mp3");
                        mediaPlayer.prepare();
                        mediaPlayer.setLooping(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
            return super.onTransact(code,data,reply,flags);
        }
    }

    public void playOrPause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            try {
                mediaPlayer.prepare();
                mediaPlayer.seekTo(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void quit() {
        mediaPlayer.stop();
        mediaPlayer.release();
    }
    //拖动进度条，服务处理函数
    public void TrackingTouch(int position){
        mediaPlayer.seekTo(position);
    }
    public void onDestroy(){
        super.onDestroy();
        mediaPlayer.stop();
        mediaPlayer.release();
    }
}
