package com.example.native_toast;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VideoThumbnailAdapter
        extends RecyclerView.Adapter<VideoThumbnailAdapter.Holder> {

    private final List<Bitmap> thumbnails;

    public VideoThumbnailAdapter(List<Bitmap> thumbnails) {
        this.thumbnails = thumbnails;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video_thumbnail, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(Holder h, int pos) {
        h.image.setImageBitmap(thumbnails.get(pos));
    }

    @Override
    public int getItemCount() {
        return thumbnails.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView image;
        Holder(View v) {
            super(v);
            image = v.findViewById(R.id.thumbImage);
        }
    }
}
