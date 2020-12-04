package com.example.my_musicplayer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.icu.text.SimpleDateFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.wcy.lrcview.LrcView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };
    //获取组件
    ImageView nextIv,playIv,lastIv;
    TextView singerTv,songTv;
    CheckBox LrcPlayer;
    //歌曲列表
    RecyclerView musicRv;
    //数据源
    List<LocalMusicBean>mDatas;
    private LocalMusicAdapter adapter;
    //歌词加载控件
    LrcView musicLrc;
    //记录当前正在播放的音乐的位置
    int currentPlayPosition = -1;//默认值为-1，表示没有播放
    //记录暂停音乐时，进度条的位置
    int currentPausePositionInSong = 0;//默认为0
    //多媒体播放器类型
    MediaPlayer mediaPlayer;
    //多线程
    private Thread thread;
    //播放状态
    private boolean isPlaying = false;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        mediaPlayer = new MediaPlayer();
        //动态获取权限
        verifyStoragePermissions(this);

        //数据源初始化
        mDatas = new ArrayList<>();
        //创建适配器对象
        adapter = new LocalMusicAdapter(this, mDatas);
        //设置适配器
        musicRv.setAdapter(adapter);
        //设置布局管理器 当前页面、上下滑动、不反转
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        //传入布局管理器设置
        musicRv.setLayoutManager(layoutManager);

        //加载本地数据源
        loadLocalMusicData();
        //设置每一项的监听事件
        setEventListener();
        //歌词显示
        setMusicLrc();

    }

    private void setMusicLrc() {
        //歌词显示控件
        musicLrc = findViewById(R.id.music_lrc);
        //触发按钮
        LrcPlayer = findViewById(R.id.local_music_bottom_iv_icon);
        LrcPlayer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    musicLrc.setVisibility(View.VISIBLE);
                } else {
                    musicLrc.setVisibility(View.GONE);
                }
            }
        });
    }

    /*设置每一项的点击事件*/
    private void setEventListener() {
        adapter.setOnItemClickListener(new LocalMusicAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(View view, int position) {
                //存储位置
                currentPlayPosition = position;
                //获取播放的对象
                LocalMusicBean musicBean = mDatas.get(position);
                //底端播放设置函数
                playMusicInMusicBean(musicBean);


            }
        });
    }

    public void playMusicInMusicBean(LocalMusicBean musicBean) {
        /*根据传入对象，播放音乐*/
        //设置底部显示的歌手名称和歌曲名称
        singerTv.setText(musicBean.getSinger());
        songTv.setText(musicBean.getSong());
        stopMusic();//停止当前正在播放的音乐
        //重置多媒体播放器
        mediaPlayer.reset();//释放原来的资源
        //重新设置新的播放路径
        try {
            mediaPlayer.setDataSource(musicBean.getPath());
            //寻找歌词
            getLrcText(musicBean);
            playMusic();//播放音乐
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getLrcText(LocalMusicBean musicBean) {
        //获取歌词路径
        String musicPath = musicBean.getPath();
        //字符串替换
        String lrcPath = musicPath.replace(".mp3",".lrc");

        //查找歌词
        String lrcText = null;
        try {
            InputStream is = new FileInputStream(new File(lrcPath));
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            lrcText = new String(buffer,"gbk");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //加载歌词
        musicLrc.loadLrc(lrcText);
        isPlaying = true;
        //用于更新歌词
        thread = new Thread(runnable);
        thread.start();


    }
//    private Handler handler = new Handler(new Handler.Callback() {
//        @Override
//        public boolean handleMessage(Message msg) {
//            if (msg.what == 1) {
//                long time = mediaPlayer.getCurrentPosition();
//                if (musicLrc.hasLrc()) {
//                    musicLrc.updateTime(time);
//                }
//            }
//            return false;
//        }
//    });
    /**
     * 使用线程控制歌词显示
     */
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            while (isPlaying) {
                try {
                    Thread.sleep(200);
//                    handler.sendEmptyMessage(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    /*点击播放按钮播放音乐，或者暂停音乐重新播放
    * 播放音乐有两种情况
    * 1.从暂停到播放
    * 2.从停止到播放*/
    private void playMusic() {
        /*播放音乐的函数*/
        //媒体不为空且没有被播放
        if (mediaPlayer!=null&&!mediaPlayer.isPlaying()) {
            if (currentPausePositionInSong == 0) {
                //从头开始播放
                try {
                    mediaPlayer.prepare();//准备

                    mediaPlayer.start();//播放
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                //从暂停途中继续播放
                //挪动到指定位置
                mediaPlayer.seekTo(currentPausePositionInSong);
                mediaPlayer.start();
            }
            //按钮设置为暂停键
            playIv.setImageResource(R.mipmap.icon_pause);
        }
    }


    private void stopMusic() {
        /*停止当前正在播放的音乐*/
        if(mediaPlayer!=null){
            currentPausePositionInSong = 0;//进度条回零
            mediaPlayer.pause();//首先暂停
            mediaPlayer.seekTo(0);//进度条初始化到0
            mediaPlayer.stop();//停止播放音乐
            playIv.setImageResource(R.mipmap.icon_play);//按钮改完播放
            isPlaying = false;
//            handler.removeCallbacks(thread);
            thread = null;
        }
    }

    private void pauseMusic() {
        /*用于暂停音乐的函数*/
        if (mediaPlayer!=null&&mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            currentPausePositionInSong = mediaPlayer.getCurrentPosition();
            //更换图标
            playIv.setImageResource(R.mipmap.icon_play);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMusic();//停止音乐播放
    }

    //动态申请权限
    private void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /*加载本地存储的mp3音乐文件到集合当中*/
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void loadLocalMusicData() {
        //  1.获取ContentResolver对象
        ContentResolver resolver = getContentResolver();
        //  2.获取本地音乐存储的Uri地址
        //        Uri uri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;//内部存储的
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;//额外存储的---指SD卡中的歌曲
        //  3.开始查询地址
        Cursor cursor = resolver.query(uri, null, null, null, null);
        //  4.遍历Cursor

        int id=0;//歌曲编号
        assert cursor != null;
        while (cursor.moveToNext()){
            //获取音乐信息
            String song = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            String singer = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            id++;
            String sid = String.valueOf(id);
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
            String time = sdf.format(new Date(duration));
            LocalMusicBean bean = new LocalMusicBean(sid, song, singer, album, time, path);
            mDatas.add(bean);//封装数据
            System.out.println(bean);
        }
        //数据源变化，提示适配器更新
        adapter.notifyDataSetChanged();
    }

    private void initView() {
        /*初始化控件的函数*/
        nextIv = findViewById(R.id.local_music_bottom_iv_next);
        playIv = findViewById(R.id.local_music_bottom_iv_play);
        lastIv = findViewById(R.id.local_music_bottom_iv_last);
        singerTv = findViewById(R.id.local_music_bottom_tv_singer);
        songTv = findViewById(R.id.local_music_bottom_tv_song);
        musicRv = findViewById(R.id.local_music_rv);
        /*监听事件*/
        nextIv.setOnClickListener(this);
        lastIv.setOnClickListener(this);
        playIv.setOnClickListener(this);

    }

    /*
    * 重写OnClick方法,实现播放、暂停、上一曲、下一曲
    * */
    @Override
    public void onClick(View view) {
        //判断id来筛选组件
        switch (view.getId()){
            case R.id.local_music_bottom_iv_last:
                if (currentPlayPosition == 0) {
                    //如果当前位置为0，表示前面没有音乐了
                    Toast.makeText(this,"已经是第一首了，没有上一曲!",Toast.LENGTH_SHORT).show();
                }
                currentPlayPosition = currentPlayPosition - 1;
                LocalMusicBean lastBean = mDatas.get(currentPlayPosition);
                playMusicInMusicBean(lastBean);

                break;
            case R.id.local_music_bottom_iv_next:
                if (currentPlayPosition == mDatas.size()-1) {
                    //如果当前位置为最后一个了，表示前面没有音乐了
                    Toast.makeText(this,"已经是最后一首了，没有下一曲!",Toast.LENGTH_SHORT).show();
                    return;//如果没有这个return，会到导致activity关闭
                }
                currentPlayPosition = currentPlayPosition + 1;
                LocalMusicBean nextBean = mDatas.get(currentPlayPosition);
                playMusicInMusicBean(nextBean);
                break;
            case R.id.local_music_bottom_iv_play:

                if (currentPlayPosition == -1) {
                    //当前并没有选中的播放音乐
                    Toast.makeText(this,"请选择想要播放的音乐",Toast.LENGTH_SHORT).show();
                    return;//如果没有这个return，会到导致activity关闭
                }
                if (mediaPlayer.isPlaying()) {
                    //当前处于播放状态，需要暂停音乐
                    pauseMusic();//暂停音乐
                }else{
                    //此时没有音乐播放，点击开始播放音乐
                    playMusic();//播放音乐
                }
                break;


        }
    }


}