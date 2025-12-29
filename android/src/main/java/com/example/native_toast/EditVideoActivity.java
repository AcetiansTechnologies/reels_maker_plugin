package com.example.native_toast;

import android.content.Intent;
import android.app.Activity;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
//import com.arthenica.ffmpegkit.FFmpegKit;
//import com.arthenica.ffmpegkit.ReturnCode;


public class EditVideoActivity extends AppCompatActivity {

    enum UiMode {
        NORMAL, TRIM
    }

    private TextView startTimeText, endTimeText;

    private long startTrimMs = 0;
    private long endTrimMs = 0;


    private UiMode currentUiMode = UiMode.NORMAL;
    private View leftHandle, rightHandle;

    private long videoDurationMs;
    private float startPercent = 0f;
    private float endPercent = 1f;

    private RelativeLayout playbackControls;
    private RecyclerView thumbnailRecycler;

    private RelativeLayout trimControls;


    private VideoView videoView;
    private ImageButton playBtn, backBtn, saveBtn;
    private ImageButton trimBtn, audioBtn, textBtn, voiceoverBtn, filtersBtn;
    private SeekBar seekBar;
    private TextView timeDisplay;
    private String videoPath;
    private String videoUri;

    private MediaPlayer.OnPreparedListener onPreparedListener;
    private Runnable updateSeekBar;
    private final android.os.Handler handler = new android.os.Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_video);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get video path from intent
        Intent intent = getIntent();
        videoPath = intent.getStringExtra("videoPath");
        videoUri = intent.getStringExtra("videoUri");


        // Initialize views

        playbackControls = findViewById(R.id.playbackControls);
        trimControls = findViewById(R.id.trimControls);

        thumbnailRecycler = findViewById(R.id.thumbnailRecycler);
        thumbnailRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        leftHandle = findViewById(R.id.leftHandle);
        rightHandle = findViewById(R.id.rightHandle);
        startTimeText = findViewById(R.id.startTimeText);
        endTimeText = findViewById(R.id.endTimeText);


        videoView = findViewById(R.id.videoView);
        playBtn = findViewById(R.id.playBtn);
        backBtn = findViewById(R.id.backBtn);
        saveBtn = findViewById(R.id.saveBtn);
        seekBar = findViewById(R.id.seekBar);
        timeDisplay = findViewById(R.id.timeDisplay);

        trimBtn = findViewById(R.id.trimBtn);
        audioBtn = findViewById(R.id.audioBtn);
        textBtn = findViewById(R.id.textBtn);
        voiceoverBtn = findViewById(R.id.voiceoverBtn);
        filtersBtn = findViewById(R.id.filtersBtn);

        // Setup video view
        if (videoUri != null) {
            videoView.setVideoURI(Uri.parse(videoUri));
        } else if (videoPath != null) {
            videoView.setVideoPath(videoPath);
        }

        // When video is ready
        onPreparedListener = mp -> {
            videoDurationMs = mp.getDuration();
            seekBar.setMax(mp.getDuration());

            endTrimMs = videoDurationMs;
            startTimeText.setText(formatTime(0));
            endTimeText.setText(formatTime((int) videoDurationMs));

            updateTimeDisplay();
        };

        videoView.setOnPreparedListener(onPreparedListener);

        // When video completes
        videoView.setOnCompletionListener(mp -> {
            if (currentUiMode == UiMode.TRIM) {
                videoView.seekTo((int) (startPercent * videoDurationMs));
                videoView.start();
            } else {
                playBtn.setImageResource(R.drawable.ic_play);
            }
        });

        endTrimMs = videoView.getDuration();

        // Play/Pause button
        playBtn.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                playBtn.setImageResource(R.drawable.ic_play);
                handler.removeCallbacks(trimLoopRunnable);
            } else {
                videoView.seekTo((int) startTrimMs);
                videoView.start();
                playBtn.setImageResource(R.drawable.ic_pause);

                if (currentUiMode == UiMode.TRIM) {
                    handler.post(trimLoopRunnable);
                }
            }
        });


        // Seek bar listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    videoView.seekTo(progress);
                    updateTimeDisplay();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (videoView.isPlaying()) {
                    startUpdateSeekBar();
                }
            }
        });

        // Back button - return to camera
        backBtn.setOnClickListener(v -> onBackPressed());

        // Save button - navigate to Flutter
        saveBtn.setOnClickListener(v -> {
            String finalVideoPath = videoPath; // the edited video path

            if (finalVideoPath == null || finalVideoPath.isEmpty()) {
                setResult(Activity.RESULT_CANCELED); // no video
            } else {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("video_path", finalVideoPath);
                setResult(Activity.RESULT_OK, resultIntent); // send result back
            }

            finish(); // close EditVideoActivity
        });



        // Trim options
        trimBtn.setOnClickListener(v -> {
            enterTrimMode();
        });


        audioBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Audio editor coming soon", Toast.LENGTH_SHORT).show();
        });

        textBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Text editor coming soon", Toast.LENGTH_SHORT).show();
        });

        voiceoverBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Voiceover coming soon", Toast.LENGTH_SHORT).show();
        });

        filtersBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Filters coming soon", Toast.LENGTH_SHORT).show();
        });
    }


    private String getSafeInputPath() throws Exception {

        if (videoPath != null) return videoPath;

        File temp = new File(getCacheDir(), "input_video.mp4");

        try (InputStream in = getContentResolver().openInputStream(Uri.parse(videoUri)); OutputStream out = new FileOutputStream(temp)) {

            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        return temp.getAbsolutePath();
    }

    private String getOutputPath() {
        File dir = new File(getExternalFilesDir(null), "trimmed");
        if (!dir.exists()) dir.mkdirs();

        return new File(dir, "TRIM_" + System.currentTimeMillis() + ".mp4").getAbsolutePath();
    }

    private String buildTrimCommand(String input, String output) {

        float start = startTrimMs / 1000f;
        float end = endTrimMs / 1000f;

        return String.format(Locale.US, "-y -ss %.3f -to %.3f -i \"%s\" -c copy \"%s\"", start, end, input, output);
    }

    private void updateTrimTimes() {

        int width = trimControls.getWidth();

        float leftX = leftHandle.getX();
        float rightX = rightHandle.getX() + rightHandle.getWidth();

        startTrimMs = (long) ((leftX / width) * videoDurationMs);
        endTrimMs = (long) ((rightX / width) * videoDurationMs);

        startTimeText.setText(formatTime((int) startTrimMs));
        endTimeText.setText(formatTime((int) endTrimMs));
    }

    private void setupHandleDrag(View handle, boolean isLeft) {

        handle.setOnTouchListener((v, event) -> {
            RelativeLayout parent = trimControls;
            int parentWidth = parent.getWidth();

            switch (event.getAction()) {

                case MotionEvent.ACTION_MOVE:
                    float x = event.getRawX() - parent.getX();
                    x = Math.max(0, Math.min(x, parentWidth));

                    if (isLeft) {
                        float maxLeft = rightHandle.getX() - handle.getWidth();
                        x = Math.min(x, maxLeft);
                        handle.setX(x);
                        startPercent = x / parentWidth;
                    } else {
                        float minRight = leftHandle.getX() + leftHandle.getWidth();
                        x = Math.max(x, minRight);
                        handle.setX(x);
                        endPercent = x / parentWidth;
                    }

                    updateTrimPlayback();
                    updateTrimTimes();

                    return true;
            }
            return true;
        });
    }

    private final Runnable trimLoopRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentUiMode == UiMode.TRIM && videoView.isPlaying()) {
                if (videoView.getCurrentPosition() >= endTrimMs) {
                    videoView.seekTo((int) startTrimMs);
                }
                handler.postDelayed(this, 30);
            }
        }
    };

    private void updateTrimPlayback() {
        long startMs = (long) (startPercent * videoDurationMs);
        long endMs = (long) (endPercent * videoDurationMs);

        if (videoView.getCurrentPosition() < startMs || videoView.getCurrentPosition() > endMs) {
            videoView.seekTo((int) startMs);
        }
    }

    private List<Bitmap> generateThumbnails() {
        List<Bitmap> list = new ArrayList<>();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            if (videoUri != null) {
                retriever.setDataSource(this, Uri.parse(videoUri));
            } else {
                retriever.setDataSource(videoPath);
            }

            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

            if (durationStr == null) return list;

            long durationMs = Long.parseLong(durationStr);

            int count = 10;
            long interval = durationMs / count;

            for (int i = 0; i < count; i++) {
                Bitmap bmp = retriever.getFrameAtTime(i * interval * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (bmp != null) list.add(bmp);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                Toast.makeText(this, "Error creatuing thumnail", Toast.LENGTH_SHORT).show();
            }
        }

        return list;
    }


    private void enterTrimMode() {
        currentUiMode = UiMode.TRIM;

        playbackControls.setVisibility(View.GONE);
        trimControls.setVisibility(View.VISIBLE);

        List<Bitmap> thumbs = generateThumbnails();
        thumbnailRecycler.setAdapter(new VideoThumbnailAdapter(thumbs));


        setupHandleDrag(leftHandle, true);
        setupHandleDrag(rightHandle, false);

    }


    private void exitToNormalMode() {
        currentUiMode = UiMode.NORMAL;

        playbackControls.setVisibility(View.VISIBLE);

        trimControls.setVisibility(View.GONE);
        handler.removeCallbacks(trimLoopRunnable);
        startTrimMs = 0;
        endTrimMs = videoDurationMs;

    }


    private void startUpdateSeekBar() {
        updateSeekBar = () -> {
            seekBar.setProgress(videoView.getCurrentPosition());
            updateTimeDisplay();
            handler.postDelayed(updateSeekBar, 500);
        };
        handler.post(updateSeekBar);
    }

    private void updateTimeDisplay() {
        int current = videoView.getCurrentPosition();
        int total = videoView.getDuration();

        String currentStr = formatTime(current);
        String totalStr = formatTime(total);

        timeDisplay.setText(String.format(Locale.getDefault(), "%s / %s", currentStr, totalStr));
    }

    private String formatTime(int millis) {
        if (millis < 0) return "0:00";
        int totalSeconds = millis / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    @Override
    public void onBackPressed() {

        if (currentUiMode != UiMode.NORMAL) {
            // We are inside some edit mode (Trim, Audio, etc.)
            exitToNormalMode();
            return;
        }

        if (videoView.isPlaying()) {
            videoView.stopPlayback();
        }
        handler.removeCallbacks(updateSeekBar);
//        super.onBackPressed();
        // insted of onbackpress i am going to naviagte to the camera activity
        // reson why we are finishing The camera activity when we are coming to edit
        // video activity because. From the edit activity edit video activity
        // if I go to like when I click go button on the top right. Then it will
        // finish. And if we have not finished the camera activity, it will
        // show up the camera activity. So that's why we are doing this.
        Intent cameraIntent = new Intent(this, CameraActivity.class);
        startActivity(cameraIntent);
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
        handler.removeCallbacks(updateSeekBar);
    }
}