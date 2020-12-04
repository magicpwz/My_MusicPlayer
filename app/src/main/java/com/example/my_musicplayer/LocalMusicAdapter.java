package com.example.my_musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LocalMusicAdapter extends RecyclerView.Adapter<LocalMusicAdapter.LocalMusicViewHolder> {
    Context context;
    List<LocalMusicBean>mDatas;//数据源成员变量

    OnItemClickListener onItemClickListener;

    //传递接口
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    //定义接口
    public interface OnItemClickListener{
        public void OnItemClick(View view,int position);
    }

    public LocalMusicAdapter(Context context, List<LocalMusicBean> mDatas) {
        this.context = context;
        this.mDatas = mDatas;
    }

    @NonNull
    @Override
    public LocalMusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //创建适配器,小卡片
        View view = LayoutInflater.from(context).inflate(R.layout.item_local_music, parent, false);
        LocalMusicViewHolder holder = new LocalMusicViewHolder(view);

        return holder;
    }

    //设置小卡片 进行赋值展示+控制每个小卡片
    @Override
    public void onBindViewHolder(@NonNull LocalMusicViewHolder holder, final int position) {
        LocalMusicBean musicBean = mDatas.get(position);
        holder.idTv.setText(musicBean.getId());
        holder.singerTv.setText(musicBean.getSinger());
        holder.songTv.setText(musicBean.getSong());
        holder.albumTv.setText(musicBean.getAlbum());
        holder.timeTv.setText(musicBean.getDuration());

        //每一项的点击事件
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //接口回调
                onItemClickListener.OnItemClick(view,position);
            }
        });

    }

    //返回集合的条目
    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    class LocalMusicViewHolder extends RecyclerView.ViewHolder{
        //初始化列表数据
        TextView idTv,songTv,singerTv,albumTv,timeTv;
        public LocalMusicViewHolder(@NonNull View itemView) {
            super(itemView);
            //编号
            idTv = itemView.findViewById(R.id.item_local_music_num);
            songTv = itemView.findViewById(R.id.item_local_music_song);
            singerTv = itemView.findViewById(R.id.item_local_music_singer);
            albumTv = itemView.findViewById(R.id.item_local_music_album);
            timeTv = itemView.findViewById(R.id.item_local_music_duration);


        }
    }


}
