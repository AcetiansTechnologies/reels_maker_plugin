package com.example.native_toast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.EditedMediaItemSequence;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.Transformer;
import androidx.media3.ui.PlayerView;
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

@OptIn(markerClass = UnstableApi.class)
public class EditVideoActivity extends AppCompatActivity {

    enum UiMode {
        NORMAL, TRIM, VOICE
    }

    private TextView startTimeText, endTimeText;
    private long startTrimMs = 0;
    private long endTrimMs = 0;

    private UiMode currentUiMode = UiMode.NORMAL;
    private View leftHandle, rightHandle;

    private long videoDurationMs;
    private float startPercent = 0f;
    private float endPercent = 1f;

    private LinearLayout playbackControls; // Changed to LinearLayout
    private RecyclerView thumbnailRecycler;

    private LinearLayout trimControls;
    private LinearLayout voiceOverControls;
    private ImageButton voiceRecordBtn, voiceDeleteBtn;
    private TextView voiceStatusText;
    private CheckBox voiceMixBox;

    // ExoPlayer & Transformer
    private PlayerView playerView;
    private ExoPlayer player;
    private Transformer transformer;

    private ImageButton playBtn, backBtn, saveBtn;
    private ImageButton muteBtn;
    private ImageButton trimBtn, audioBtn, textBtn, voiceoverBtn, filtersBtn;
    private SeekBar seekBar;
    private TextView timeDisplay;
    private String videoPath;
    private String videoUri;
    private View selectedRangeView;
    private View playheadView;

    private Runnable updateSeekBar;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isSeeking = false;
    private boolean isMuted = false;

    private ProgressDialog progressDialog;

    // Voice Over Variables
    private MediaRecorder mediaRecorder;
    private String voiceOverPath = null;
    private boolean isRecordingVoice = false;
    private long voiceStartMs = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_video);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        videoPath = intent.getStringExtra("video_path");
        videoUri = intent.getStringExtra("video_uri");

        playbackControls = findViewById(R.id.playbackControls);
        trimControls = findViewById(R.id.trimControls);
        voiceOverControls = findViewById(R.id.voiceOverControls);
        
        voiceRecordBtn = findViewById(R.id.voiceRecordBtn);
        voiceDeleteBtn = findViewById(R.id.voiceDeleteBtn);
        voiceStatusText = findViewById(R.id.voiceStatusText);
        voiceMixBox = findViewById(R.id.voiceMixBox);

        thumbnailRecycler = findViewById(R.id.thumbnailRecycler);
        thumbnailRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        leftHandle = findViewById(R.id.leftHandle);
        rightHandle = findViewById(R.id.rightHandle);
        startTimeText = findViewById(R.id.startTimeText);
        endTimeText = findViewById(R.id.endTimeText);
        selectedRangeView = findViewById(R.id.selectedRangeView);
        playheadView = findViewById(R.id.playheadView);

        playerView = findViewById(R.id.playerView);
        playBtn = findViewById(R.id.playBtn);
        backBtn = findViewById(R.id.backBtn);
        saveBtn = findViewById(R.id.saveBtn);
        seekBar = findViewById(R.id.seekBar);
        timeDisplay = findViewById(R.id.timeDisplay);
        muteBtn = findViewById(R.id.muteBtn);

        trimBtn = findViewById(R.id.trimBtn);
        audioBtn = findViewById(R.id.audioBtn);
        textBtn = findViewById(R.id.textBtn);
        voiceoverBtn = findViewById(R.id.voiceoverBtn);
        filtersBtn = findViewById(R.id.filtersBtn);

        setupPlayer();

        playBtn.setOnClickListener(v -> {
            if (player == null) return;
            if (player.isPlaying()) {
                player.pause();
                if (isRecordingVoice) stopVoiceRecording();
            } else {
                if (currentUiMode == UiMode.TRIM && player.getCurrentPosition() >= endTrimMs) {
                    player.seekTo(startTrimMs);
                }
                player.play();
            }
            updatePlayPauseIcon();
        });
        
        muteBtn.setOnClickListener(v -> toggleMute());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    player.seekTo(progress);
                    updateTimeDisplay();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { isSeeking = true; }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { isSeeking = false; }
        });

        backBtn.setOnClickListener(v -> onBackPressed());
        saveBtn.setOnClickListener(v -> startTrimExport());
        trimBtn.setOnClickListener(v -> enterTrimMode());
        voiceoverBtn.setOnClickListener(v -> enterVoiceMode());

        audioBtn.setOnClickListener(v -> Toast.makeText(this, "Audio editor coming soon", Toast.LENGTH_SHORT).show());
        textBtn.setOnClickListener(v -> Toast.makeText(this, "Text editor coming soon", Toast.LENGTH_SHORT).show());
        filtersBtn.setOnClickListener(v -> Toast.makeText(this, "Filters coming soon", Toast.LENGTH_SHORT).show());

        voiceRecordBtn.setOnClickListener(v -> {
            if (isRecordingVoice) {
                stopVoiceRecording();
                if (player != null) player.pause();
            } else {
                startVoiceRecording();
                if (player != null) player.play();
            }
            updatePlayPauseIcon();
        });
        
        voiceDeleteBtn.setOnClickListener(v -> {
            if (voiceOverPath != null) {
                new File(voiceOverPath).delete();
                voiceOverPath = null;
                voiceStatusText.setText("Voice deleted. Tap mic to record.");
                voiceDeleteBtn.setVisibility(View.GONE);
                voiceRecordBtn.setImageResource(R.drawable.ic_mic); 
            }
        });
    }

    private void setupPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        MediaItem mediaItem;
        if (videoUri != null) {
            mediaItem = MediaItem.fromUri(Uri.parse(videoUri));
        } else if (videoPath != null) {
            mediaItem = MediaItem.fromUri(Uri.fromFile(new File(videoPath)));
        } else {
            return;
        }

        player.setMediaItem(mediaItem);
        player.prepare();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    videoDurationMs = player.getDuration();
                    seekBar.setMax((int) videoDurationMs);
                    if (endTrimMs == 0) endTrimMs = videoDurationMs;
                    updateTimeDisplay();
                    startTimeText.setText(formatTime(0));
                    endTimeText.setText(formatTime((int) videoDurationMs));
                } else if (playbackState == Player.STATE_ENDED) {
                    if (currentUiMode == UiMode.TRIM) {
                        player.seekTo(startTrimMs);
                        player.play();
                    } else if (currentUiMode == UiMode.VOICE && isRecordingVoice) {
                        stopVoiceRecording();
                        player.seekTo(startTrimMs);
                        player.pause();
                        updatePlayPauseIcon();
                    } else {
                        player.seekTo(0);
                        player.pause();
                        updatePlayPauseIcon();
                    }
                }
            }
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseIcon();
                if (isPlaying) {
                    startUpdateSeekBar();
                    if (currentUiMode == UiMode.TRIM) handler.post(trimLoopRunnable);
                } else {
                    handler.removeCallbacks(updateSeekBar);
                    handler.removeCallbacks(trimLoopRunnable);
                    if (isRecordingVoice) stopVoiceRecording();
                }
            }
        });
    }

    private void updatePlayPauseIcon() {
        if (player.isPlaying()) {
            playBtn.setImageResource(R.drawable.ic_pause);
        } else {
            playBtn.setImageResource(R.drawable.ic_play);
        }
    }
    
    private void toggleMute() {
        isMuted = !isMuted;
        if (player != null) {
            player.setVolume(isMuted ? 0f : 1f);
        }
        // Assuming ic_music means sound ON, maybe ic_volume_off needed for mute?
        // Using tint or alpha for now as standard icons might differ
        if (isMuted) {
             muteBtn.setAlpha(0.5f);
             muteBtn.setImageResource(R.drawable.ic_no_music); // Ideally use ic_music_off if available
        } else {
             muteBtn.setAlpha(1.0f);
             muteBtn.setImageResource(R.drawable.ic_music); 
        }
    }

    // ============= MODE SWITCHING =============

    private void enterTrimMode() {
        currentUiMode = UiMode.TRIM;
        voiceOverControls.setVisibility(View.GONE);
        playbackControls.setVisibility(View.GONE);
        trimControls.setVisibility(View.VISIBLE);
        playheadView.setVisibility(View.VISIBLE);
        
        if (player != null) {
            player.pause();
            if (endTrimMs == 0) endTrimMs = videoDurationMs;
            player.seekTo(startTrimMs);
        }
        initTrimThumbnailsAndHandles();
    }
    
    private void enterVoiceMode() {
        currentUiMode = UiMode.VOICE;
        trimControls.setVisibility(View.GONE);
        playbackControls.setVisibility(View.VISIBLE); 
        voiceOverControls.setVisibility(View.VISIBLE);
        playheadView.setVisibility(View.GONE);

        if (player != null) {
            player.pause();
        }
        
        if (voiceOverPath != null && new File(voiceOverPath).exists()) {
             voiceDeleteBtn.setVisibility(View.VISIBLE);
             voiceStatusText.setText("Voice recorded.");
        } else {
             voiceDeleteBtn.setVisibility(View.GONE);
             voiceStatusText.setText("Tap mic to record");
        }
    }

    private void exitToNormalMode() {
        currentUiMode = UiMode.NORMAL;
        playbackControls.setVisibility(View.VISIBLE);
        trimControls.setVisibility(View.GONE);
        voiceOverControls.setVisibility(View.GONE);
        playheadView.setVisibility(View.GONE);
        
        handler.removeCallbacks(trimLoopRunnable);
        if (player != null) updatePlayPauseIcon();
    }

    // ============= VOICE RECORDING LOGIC =============

    private void startVoiceRecording() {
        if (player == null) return;
        
        if (voiceOverPath != null && new File(voiceOverPath).exists()) {
            new File(voiceOverPath).delete(); 
        }
        
        voiceOverPath = new File(getExternalFilesDir(null), "voice_temp.aac").getAbsolutePath();
        voiceStartMs = player.getCurrentPosition();
        
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(voiceOverPath);
        
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecordingVoice = true;
            voiceStatusText.setText("Recording...");
            voiceRecordBtn.setColorFilter(0xFFFF0000); 
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show();
            voiceOverPath = null;
        }
    }

    private void stopVoiceRecording() {
        if (isRecordingVoice && mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaRecorder = null;
            isRecordingVoice = false;
            
            voiceStatusText.setText("Voice recorded");
            voiceRecordBtn.clearColorFilter();
            voiceDeleteBtn.setVisibility(View.VISIBLE);
        }
    }

    // ============= EXPORT LOGIC =============

    private void startTrimExport() {
        if (player != null && player.isPlaying()) player.pause();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving Video...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String inputPath = (videoPath != null) ? videoPath : getSafeInputPath();
        File inputFile = new File(inputPath);
        String outputPath = getOutputPath();
        
        // Cleanup old files
        cleanupStorage(inputFile);

        // --- Config ---
        boolean hasVoiceOver = (voiceOverPath != null && new File(voiceOverPath).exists());
        
        // User Logic for Mute/KeepOriginal:
        // 1. If Global Mute is ON (isMuted) -> Remove Main Audio.
        // 2. If Voice Over is ON and "Keep Original" is OFF -> Remove Main Audio.
        boolean removeMainAudio = isMuted;
        if (!removeMainAudio && hasVoiceOver) {
             if (!voiceMixBox.isChecked()) {
                 removeMainAudio = true; // "Keep only voice over"
             }
        }

        // --- Build Composition ---
        
        MediaItem videoItem = MediaItem.fromUri(inputPath);
        
        MediaItem.ClippingConfiguration clipping = new MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startTrimMs)
                .setEndPositionMs(endTrimMs)
                .build();
        
        EditedMediaItem videoEditedItem = new EditedMediaItem.Builder(videoItem.buildUpon().setClippingConfiguration(clipping).build())
                .setRemoveAudio(removeMainAudio)
                .build();

        // 2. Sequences
        List<EditedMediaItemSequence> sequences = new ArrayList<>();
        sequences.add(new EditedMediaItemSequence.Builder(videoEditedItem).build());
        
        if (hasVoiceOver) {
             MediaItem voiceItem = MediaItem.fromUri(voiceOverPath);
             EditedMediaItem voiceEditedItem = new EditedMediaItem.Builder(voiceItem).build();
             sequences.add(new EditedMediaItemSequence.Builder(voiceEditedItem).build());
        }

        Composition composition = new Composition.Builder(sequences).build();

        transformer = new Transformer.Builder(this)
                .addListener(new Transformer.Listener() {
                    @Override
                    public void onCompleted(Composition composition, ExportResult exportResult) {
                         runOnUiThread(() -> {
                            if (progressDialog != null) progressDialog.dismiss();
                            if (inputFile.exists()) inputFile.delete();
                            if (voiceOverPath != null) new File(voiceOverPath).delete();
                            finishWithResult(outputPath);
                        });
                    }
                    @Override
                    public void onError(Composition composition, ExportResult exportResult, ExportException exportException) {
                        runOnUiThread(() -> {
                            if (progressDialog != null) progressDialog.dismiss();
                            Toast.makeText(EditVideoActivity.this, "Export Failed: " + exportException.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                })
                .build();

        transformer.start(composition, outputPath);
    }
    
    // ... helpers ...
    
    private void finishWithResult(String finalPath) {
        if (finalPath == null || finalPath.isEmpty()) {
            setResult(Activity.RESULT_CANCELED);
        } else {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("video_path", finalPath);
            setResult(Activity.RESULT_OK, resultIntent);
        }
        finish();
    }
    
    private void cleanupStorage(File keepFile) {
        File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".mp4"));
            if (files != null) {
                for (File f : files) {
                    if (!f.getAbsolutePath().equals(keepFile.getAbsolutePath())) f.delete();
                }
            }
        }
        File oldTrimDir = new File(getExternalFilesDir(null), "trimmed");
        if (oldTrimDir.exists()) deleteRecursive(oldTrimDir);
    }
    private void deleteRecursive(File f) {
        if (f.isDirectory()) for (File c : f.listFiles()) deleteRecursive(c);
        f.delete();
    }
    private String getOutputPath() {
        File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "FINAL_" + System.currentTimeMillis() + ".mp4").getAbsolutePath();
    }
    private String getSafeInputPath() {
        if (videoPath != null) return videoPath;
        try {
            File temp = new File(getCacheDir(), "input_temp.mp4");
            try (InputStream in = getContentResolver().openInputStream(Uri.parse(videoUri)); 
                 OutputStream out = new FileOutputStream(temp)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            }
            return temp.getAbsolutePath();
        } catch (Exception e) { return null; }
    }
    
    private void updatePlayheadPosition() {
        if (videoDurationMs <= 0) return;
        View parent = (View) playheadView.getParent();
        int parentWidth = parent.getWidth();
        if (parentWidth == 0) return;
        long current = player.getCurrentPosition();
        float percent = current / (float) videoDurationMs;
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
        if (width == 0) return;
        float leftX = leftHandle.getX();
        float rightX = rightHandle.getX() + rightHandle.getWidth();
        if (width <= 0) return;
        startPercent = leftX / width;
        endPercent = rightX / width;
        startPercent = Math.max(0f, Math.min(startPercent, 1f));
        endPercent = Math.max(0f, Math.min(endPercent, 1f));
        startTrimMs = (long) (startPercent * videoDurationMs);
        endTrimMs = (long) (endPercent * videoDurationMs);
        startTimeText.setText(formatTime((int) startTrimMs));
        endTimeText.setText(formatTime((int) endTrimMs));
    }
    private void initTrimThumbnailsAndHandles() {
        new Thread(() -> {
            List<Bitmap> thumbs = generateThumbnails();
            runOnUiThread(() -> {
                 thumbnailRecycler.setAdapter(new VideoThumbnailAdapter(thumbs));
                 trimControls.post(() -> {
                     int width = trimControls.getWidth();
                     if (width > 0 && videoDurationMs > 0) {
                        float startX = (startTrimMs / (float)videoDurationMs) * width;
                        float endX = (endTrimMs / (float)videoDurationMs) * width;
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
            LinearLayout parent = trimControls;
            int parentWidth = parent.getWidth();
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float x = event.getRawX() - parent.getX();
                x = Math.max(0, Math.min(x, parentWidth - handle.getWidth()));
                if (isLeft) {
                    float maxLeft = rightHandle.getX() - handle.getWidth();
                    maxLeft = Math.min(maxLeft, rightHandle.getX() - 30); 
                    x = Math.min(x, maxLeft);
                    handle.setX(x);
                    updateTrimTimes();
                    if (player != null) { player.seekTo(startTrimMs); player.pause(); }
                } else {
                    float minRight = leftHandle.getX() + leftHandle.getWidth();
                    minRight = Math.max(minRight, leftHandle.getX() + 30 + leftHandle.getWidth());
                    x = Math.max(x, minRight);
                    handle.setX(x);
                    updateTrimTimes();
                    if (player != null) { player.seekTo(endTrimMs); player.pause(); }
                }
                updateSelectedRangeUI();
                updatePlayheadPosition();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                 if (player != null) { player.seekTo(startTrimMs); player.play(); }
            }
            return true;
        });
    }
    private final Runnable trimLoopRunnable = new Runnable() {
        @Override public void run() {
            if (currentUiMode == UiMode.TRIM && player != null && player.isPlaying()) {
                long current = player.getCurrentPosition();
                if (current >= endTrimMs) player.seekTo(startTrimMs);
                else if (current < startTrimMs) player.seekTo(startTrimMs);
                handler.postDelayed(this, 30);
            }
        }
    };
    private List<Bitmap> generateThumbnails() {
        List<Bitmap> list = new ArrayList<>();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (videoUri != null) retriever.setDataSource(this, Uri.parse(videoUri));
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
        } catch (Exception e) {} finally { try { retriever.release(); } catch (IOException e) {} }
        return list;
    }
    private void startUpdateSeekBar() {
        updateSeekBar = new Runnable() {
            @Override public void run() {
                if (player != null && player.isPlaying()) {
                    if (!isSeeking) seekBar.setProgress((int) player.getCurrentPosition());
                    updateTimeDisplay();
                    updatePlayheadPosition();
                }
                handler.postDelayed(this, 30);
            }
        };
        handler.post(updateSeekBar);
    }
    private void updateTimeDisplay() {
        if (player == null) return;
        long current = player.getCurrentPosition();
        long total = player.getDuration();
        if (total < 0) total = 0;
        timeDisplay.setText(String.format(Locale.getDefault(), "%s / %s", formatTime((int)current), formatTime((int)total)));
    }
    private String formatTime(int millis) {
        if (millis < 0) return "0:00";
        int totalSeconds = millis / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
    @Override public void onBackPressed() {
        if (currentUiMode != UiMode.NORMAL) { exitToNormalMode(); return; }
        if (player != null) player.stop();
        startActivity(new Intent(this, CameraActivity.class));
        finish();
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (player != null) { player.release(); player = null; }
        handler.removeCallbacksAndMessages(null);
    }
}