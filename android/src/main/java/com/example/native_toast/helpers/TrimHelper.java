package com.example.native_toast.helpers;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.RecyclerView;

import com.example.native_toast.VideoThumbnailAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Helper class for video trimming functionality.
 */
public class TrimHelper {

    public interface TrimListener {
        void onTrimChanged(long startMs, long endMs);
        ExoPlayer getPlayer();
        long getVideoDuration();
    }

    private final Activity activity;
    private final LinearLayout trimControls;
    private final RecyclerView thumbnailRecycler;
    private final View leftHandle;
    private final View rightHandle;
    private final View selectedRangeView;
    private final View playheadView;
    private final TextView startTimeText;
    private final TextView endTimeText;
    private final String videoUri;
    private final String videoPath;
    private final TrimListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long startTrimMs = 0;
    private long endTrimMs = 0;
    private float startPercent = 0f;
    private float endPercent = 1f;
    private boolean isLooping = false;

    private final Runnable trimLoopRunnable = new Runnable() {
        @Override
        public void run() {
            if (isLooping && listener.getPlayer() != null && listener.getPlayer().isPlaying()) {
                long current = listener.getPlayer().getCurrentPosition();
                if (current >= endTrimMs) listener.getPlayer().seekTo(startTrimMs);
                else if (current < startTrimMs) listener.getPlayer().seekTo(startTrimMs);
                handler.postDelayed(this, 30);
            }
        }
    };

    public TrimHelper(Activity activity, LinearLayout trimControls, RecyclerView thumbnailRecycler,
                     View leftHandle, View rightHandle, View selectedRangeView, View playheadView,
                     TextView startTimeText, TextView endTimeText,
                     String videoUri, String videoPath, TrimListener listener) {
        this.activity = activity;
        this.trimControls = trimControls;
        this.thumbnailRecycler = thumbnailRecycler;
        this.leftHandle = leftHandle;
        this.rightHandle = rightHandle;
        this.selectedRangeView = selectedRangeView;
        this.playheadView = playheadView;
        this.startTimeText = startTimeText;
        this.endTimeText = endTimeText;
        this.videoUri = videoUri;
        this.videoPath = videoPath;
        this.listener = listener;
    }

    public long getStartTrimMs() { return startTrimMs; }
    public long getEndTrimMs() { return endTrimMs; }

    public void setTrimRange(long start, long end) {
        this.startTrimMs = start;
        this.endTrimMs = end;
    }

    public void initTrimThumbnailsAndHandles() {
        new Thread(() -> {
            List<Bitmap> thumbs = generateThumbnails();
            activity.runOnUiThread(() -> {
                thumbnailRecycler.setAdapter(new VideoThumbnailAdapter(thumbs));
                trimControls.post(() -> {
                    int width = trimControls.getWidth();
                    long duration = listener.getVideoDuration();
                    if (width > 0 && duration > 0) {
                        float startX = (startTrimMs / (float) duration) * width;
                        float endX = (endTrimMs / (float) duration) * width;
                        endX = endX - rightHandle.getWidth();
                        leftHandle.setX(startX);
                        rightHandle.setX(endX);
                        updateSelectedRangeUI();
                    }
                });
            });
        }).start();
        setupHandleDrag(leftHandle, true);
        setupHandleDrag(rightHandle, false);
    }

    private void setupHandleDrag(View handle, boolean isLeft) {
        handle.setOnTouchListener((v, event) -> {
            int parentWidth = trimControls.getWidth();
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float x = event.getRawX() - trimControls.getX();
                x = Math.max(0, Math.min(x, parentWidth - handle.getWidth()));
                
                if (isLeft) {
                    float maxLeft = rightHandle.getX() - 30;
                    x = Math.min(x, maxLeft);
                    handle.setX(x);
                    updateTrimTimes();
                    if (listener.getPlayer() != null) {
                        listener.getPlayer().seekTo(startTrimMs);
                        listener.getPlayer().pause();
                    }
                } else {
                    float minRight = leftHandle.getX() + leftHandle.getWidth() + 30;
                    x = Math.max(x, minRight);
                    handle.setX(x);
                    updateTrimTimes();
                    if (listener.getPlayer() != null) {
                        listener.getPlayer().seekTo(endTrimMs);
                        listener.getPlayer().pause();
                    }
                }
                updateSelectedRangeUI();
                updatePlayheadPosition();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (listener.getPlayer() != null) {
                    listener.getPlayer().seekTo(startTrimMs);
                    listener.getPlayer().play();
                }
            }
            return true;
        });
    }

    public void updatePlayheadPosition() {
        long duration = listener.getVideoDuration();
        if (duration <= 0) return;
        View parent = (View) playheadView.getParent();
        int parentWidth = parent.getWidth();
        if (parentWidth == 0) return;
        long current = listener.getPlayer().getCurrentPosition();
        float percent = current / (float) duration;
        playheadView.setX(percent * parentWidth);
    }

    private void updateSelectedRangeUI() {
        View parent = (View) selectedRangeView.getParent();
        int parentWidth = parent.getWidth();
        if (parentWidth == 0) return;
        float leftX = leftHandle.getX();
        float rightX = rightHandle.getX() + rightHandle.getWidth();
        int width = (int) (rightX - leftX);
        if (width < 0) width = 0;
        selectedRangeView.setX(leftX);
        selectedRangeView.getLayoutParams().width = width;
        selectedRangeView.requestLayout();
    }

    private void updateTrimTimes() {
        int width = trimControls.getWidth();
        if (width <= 0) return;
        float leftX = leftHandle.getX();
        float rightX = rightHandle.getX() + rightHandle.getWidth();
        startPercent = Math.max(0f, Math.min(leftX / width, 1f));
        endPercent = Math.max(0f, Math.min(rightX / width, 1f));
        
        long duration = listener.getVideoDuration();
        startTrimMs = (long) (startPercent * duration);
        endTrimMs = (long) (endPercent * duration);
        
        startTimeText.setText(formatTime((int) startTrimMs));
        endTimeText.setText(formatTime((int) endTrimMs));
        
        listener.onTrimChanged(startTrimMs, endTrimMs);
    }

    private List<Bitmap> generateThumbnails() {
        List<Bitmap> list = new ArrayList<>();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (videoUri != null) retriever.setDataSource(activity, Uri.parse(videoUri));
            else retriever.setDataSource(videoPath);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr == null) return list;
            long durationMs = Long.parseLong(durationStr);
            int count = 8;
            long interval = durationMs / count;
            for (int i = 0; i < count; i++) {
                Bitmap bmp = retriever.getFrameAtTime(i * interval * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (bmp != null) {
                    Bitmap scaled = Bitmap.createScaledBitmap(bmp, 150, 150, false);
                    list.add(scaled);
                    if (bmp != scaled) bmp.recycle();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { retriever.release(); } catch (IOException e) {}
        }
        return list;
    }

    public void startLoop() {
        isLooping = true;
        handler.post(trimLoopRunnable);
    }

    public void stopLoop() {
        isLooping = false;
        handler.removeCallbacks(trimLoopRunnable);
    }

    private String formatTime(int millis) {
        int totalSeconds = millis / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
}
