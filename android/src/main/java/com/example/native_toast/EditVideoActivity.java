package com.example.native_toast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
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

    private LinearLayout trimControls;

    // ExoPlayer & Transformer
    private PlayerView playerView;
    private ExoPlayer player;
    private Transformer transformer;

    private ImageButton playBtn, backBtn, saveBtn;
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

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_video);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get video path from intent
        Intent intent = getIntent();
        videoPath = intent.getStringExtra("video_path");
        videoUri = intent.getStringExtra("video_uri");

        // Initialize views
        playbackControls = findViewById(R.id.playbackControls);
        trimControls = findViewById(R.id.trimControls);

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

        trimBtn = findViewById(R.id.trimBtn);
        audioBtn = findViewById(R.id.audioBtn);
        textBtn = findViewById(R.id.textBtn);
        voiceoverBtn = findViewById(R.id.voiceoverBtn);
        filtersBtn = findViewById(R.id.filtersBtn);

        setupPlayer();

        // Play/Pause button
        playBtn.setOnClickListener(v -> {
            if (player == null) return;
            if (player.isPlaying()) {
                player.pause();
            } else {
                if (currentUiMode == UiMode.TRIM && player.getCurrentPosition() >= endTrimMs) {
                    player.seekTo(startTrimMs);
                }
                player.play();
            }
            updatePlayPauseIcon();
        });

        // Seek bar listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    player.seekTo(progress);
                    updateTimeDisplay();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeeking = false;
            }
        });

        // Back button - return to camera
        backBtn.setOnClickListener(v -> onBackPressed());

        // Save button
        saveBtn.setOnClickListener(v -> {
            // Trim and Save
            startTrimExport();
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
                    if (endTrimMs == 0) {
                        endTrimMs = videoDurationMs;
                    }
                    updateTimeDisplay();
                    startTimeText.setText(formatTime(0));
                    endTimeText.setText(formatTime((int) videoDurationMs));
                } else if (playbackState == Player.STATE_ENDED) {
                    if (currentUiMode == UiMode.TRIM) {
                        player.seekTo(startTrimMs);
                        player.play();
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
                    if (currentUiMode == UiMode.TRIM) {
                        handler.post(trimLoopRunnable);
                    }
                } else {
                    handler.removeCallbacks(updateSeekBar);
                    handler.removeCallbacks(trimLoopRunnable);
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

    private void updatePlayheadPosition() {
        if (videoDurationMs <= 0) return;

        View parent = (View) playheadView.getParent();
        int parentWidth = parent.getWidth();
        if (parentWidth == 0) return; // Not laid out yet

        long current = player.getCurrentPosition();

        float percent = current / (float) videoDurationMs;
        float x = percent * parentWidth;

        // Visual Playhead logic: It represents global time, but visually clamped in the UI? 
        // Actually playhead should move across the whole bar.
        // However user wants "playheadView should start where the drag handle ends" 
        // -> This implies looping behavior visualization. 
        // But visually the playhead needs to map to current video time.
        
        playheadView.setX(x);
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
    
    // ----------- Trimming Logic (Transformer) -----------

    private void startTrimExport() {
        // If we haven't trimmed (full duration), just return original
        // But user wants "only one video", so if we return original, we should probably delete others too?
        // Let's stick to the trim flow first.
        if (startTrimMs == 0 && endTrimMs >= videoDurationMs) {
             // If not trimming, we still need to ensure "only one video" rule? 
             // Maybe just cleanup others and keep this one.
             cleanupStorage(new File(videoPath));
             finishWithResult(videoPath);
             return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Trimming Video...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String inputPath = (videoPath != null) ? videoPath : getSafeInputPath();
        File inputFile = new File(inputPath);
        
        // Output to standard movies directory (no "trimmed" folder)
        String outputPath = getOutputPath();

        // CLEANUP: Delete everything except the input file before we start writing
        cleanupStorage(inputFile);

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(inputPath)
                .setClippingConfiguration(
                        new MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(startTrimMs)
                                .setEndPositionMs(endTrimMs)
                                .build())
                .build();

        transformer = new Transformer.Builder(this)
                .addListener(new Transformer.Listener() {
                    @Override
                    public void onCompleted(Composition composition, ExportResult exportResult) {
                        runOnUiThread(() -> {
                            if (progressDialog != null) progressDialog.dismiss();
                            
                            // SUCCESS: Now delete the input file so ONLY the new trimmed video remains
                            if (inputFile.exists()) {
                                inputFile.delete();
                            }
                            
                            finishWithResult(outputPath);
                        });
                    }

                    @Override
                    public void onError(Composition composition, ExportResult exportResult, ExportException exportException) {
                        runOnUiThread(() -> {
                            if (progressDialog != null) progressDialog.dismiss();
                            Toast.makeText(EditVideoActivity.this, "Trim Failed: " + exportException.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                })
                .build();

        transformer.start(mediaItem, outputPath);
    }
    
    // Deletes all MP4s in the directory EXCEPT the keepFile
    private void cleanupStorage(File keepFile) {
        // We use DIRECTORY_MOVIES for both Camera and Edit
        File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".mp4"));
            if (files != null) {
                for (File f : files) {
                    // Don't delete the file we are currently reading from!
                    if (!f.getAbsolutePath().equals(keepFile.getAbsolutePath())) {
                        f.delete();
                    }
                }
            }
        }
        
        // Also remove the old "trimmed" folder if it exists from previous runs
        File oldTrimDir = new File(getExternalFilesDir(null), "trimmed");
        if (oldTrimDir.exists()) {
            deleteRecursive(oldTrimDir);
        }
    }
    
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        }
        fileOrDirectory.delete();
    }

    private String getOutputPath() {
        // Save directly to Movies folder, no subfolder
        File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);
        if (!dir.exists()) dir.mkdirs();
        // Use a consistent prefix or just generic
        return new File(dir, "FINAL_" + System.currentTimeMillis() + ".mp4").getAbsolutePath();
    }

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

    private String getSafeInputPath() {
        if (videoPath != null) return videoPath;
        // If URI only, need a real file path for FFMPEG/Transformer mostly?
        // Transformer supports URIs, so we might just use the URI string for MediaItem.
        // But let's fallback to cache copy if needed.
        try {
            File temp = new File(getCacheDir(), "input_temp.mp4");
            try (InputStream in = getContentResolver().openInputStream(Uri.parse(videoUri)); 
                 OutputStream out = new FileOutputStream(temp)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            return temp.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    // ----------- Trim UI Logic -----------

    private void updateTrimTimes() {
        int width = trimControls.getWidth();
        if (width == 0) return;

        float leftX = leftHandle.getX();
        float rightX = rightHandle.getX() + rightHandle.getWidth();
        
        // Safety check to avoid NaN or division by zero
        if (width <= 0) return;
        
        // Calculate based on centers/edges handles
        startPercent = leftX / width;
        endPercent = rightX / width;
        
        // Clamp percentages
        startPercent = Math.max(0f, Math.min(startPercent, 1f));
        endPercent = Math.max(0f, Math.min(endPercent, 1f));

        startTrimMs = (long) (startPercent * videoDurationMs);
        endTrimMs = (long) (endPercent * videoDurationMs);

        startTimeText.setText(formatTime((int) startTrimMs));
        endTimeText.setText(formatTime((int) endTrimMs));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupHandleDrag(View handle, boolean isLeft) {
        handle.setOnTouchListener((v, event) -> {
            LinearLayout parent = trimControls;
            int parentWidth = parent.getWidth();

            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float x = event.getRawX() - parent.getX();
                // Ensure drag stays within parent bounds
                x = Math.max(0, Math.min(x, parentWidth - handle.getWidth()));

                if (isLeft) {
                    // Left handle cannot cross right handle
                    float maxLeft = rightHandle.getX() - handle.getWidth();
                    // Optional: minimum gap
                    maxLeft = Math.min(maxLeft, rightHandle.getX() - 30); 
                    x = Math.min(x, maxLeft);
                    
                    handle.setX(x);
                    
                    // Logic: "playheadView should start where the drag handle ends"
                    // If we dragging left handle, update start time and seek player to it for validation
                    updateTrimTimes();
                    if (player != null) {
                         player.seekTo(startTrimMs);
                         player.pause(); // Pause while dragging for precision
                    }
                    
                } else {
                    // Right handle cannot cross left handle
                    float minRight = leftHandle.getX() + leftHandle.getWidth();
                    minRight = Math.max(minRight, leftHandle.getX() + 30 + leftHandle.getWidth());
                    x = Math.max(x, minRight);
                    
                    handle.setX(x);
                    
                    updateTrimTimes();
                     if (player != null) {
                        player.seekTo(endTrimMs); // Preview the end frame
                        player.pause();
                    }
                }

                updateSelectedRangeUI();
                updatePlayheadPosition();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                 // Resume or prepare loop when release? 
                 // User likely wants to preview the loop now.
                 if (player != null) {
                     player.seekTo(startTrimMs);
                     player.play();
                 }
            }
            return true;
        });
    }

    private final Runnable trimLoopRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentUiMode == UiMode.TRIM && player != null && player.isPlaying()) {
                long current = player.getCurrentPosition();
                if (current >= endTrimMs) {
                    // Loop back to start
                    player.seekTo(startTrimMs);
                } else if (current < startTrimMs) {
                    player.seekTo(startTrimMs);
                }
                handler.postDelayed(this, 30);
            }
        }
    };

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
            int count = 8; // Number of thumbs to fit
            long interval = durationMs / count;

            for (int i = 0; i < count; i++) {
                Bitmap bmp = retriever.getFrameAtTime(i * interval * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (bmp != null) {
                    // resize to small to save memory
                    Bitmap scaled = Bitmap.createScaledBitmap(bmp, 150, 150, false);
                    list.add(scaled);
                    if (bmp != scaled) bmp.recycle();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                // ignore
            }
        }
        return list;
    }


    private void enterTrimMode() {
        currentUiMode = UiMode.TRIM;
        selectedRangeView.post(this::updateSelectedRangeUI);
        
        playbackControls.setVisibility(View.GONE);
        trimControls.setVisibility(View.VISIBLE);

        if (player != null) {
            player.pause();
            playBtn.setImageResource(R.drawable.ic_play);
            // Default trim = full video initially
             if (endTrimMs == 0) endTrimMs = videoDurationMs;
             player.seekTo(startTrimMs);
        }

        // Generate thumbnails async if needed, or main thread for simplicity (small number)
        new Thread(() -> {
            List<Bitmap> thumbs = generateThumbnails();
            runOnUiThread(() -> {
                 thumbnailRecycler.setAdapter(new VideoThumbnailAdapter(thumbs));
                 // Init handles positions after layout
                 trimControls.post(() -> {
                     
                     // Reset handles to edges if just entering or if logic requires
                     // But if we want to remember previous trim state, we should check startTrimMs
                     
                     int width = trimControls.getWidth();
                     if (width > 0 && videoDurationMs > 0) {
                        float startX = (startTrimMs / (float)videoDurationMs) * width;
                        float endX = (endTrimMs / (float)videoDurationMs) * width;
                        // correct right handle anchor
                        endX = endX - rightHandle.getWidth(); 
                        
                        leftHandle.setX(startX);
                        rightHandle.setX(endX); // Right handle x is its left edge
                        
                        updateSelectedRangeUI();
                     }
                 });
            });
        }).start();

        setupHandleDrag(leftHandle, true);
        setupHandleDrag(rightHandle, false);
    }


    private void exitToNormalMode() {
        currentUiMode = UiMode.NORMAL;
        playbackControls.setVisibility(View.VISIBLE);
        trimControls.setVisibility(View.GONE);
        handler.removeCallbacks(trimLoopRunnable);
        
        // Reset full playback range visually or logical? 
        // Usually we stay with the trim, but here "exit" implies cancelling trim mode view
        // But maybe we keep the trim times applied? 
        // The user says "Back" cancels everything usually. 
        startTrimMs = 0;
        endTrimMs = videoDurationMs;
        
        if (player != null) updatePlayPauseIcon();
    }


    private void startUpdateSeekBar() {
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (player != null && player.isPlaying()) {
                    if (!isSeeking) {
                        int pos = (int) player.getCurrentPosition();
                        seekBar.setProgress(pos);
                    }
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

        String currentStr = formatTime((int) current);
        String totalStr = formatTime((int) total);

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
            exitToNormalMode();
            return;
        }
        if (player != null) {
            player.stop();
        }
        Intent cameraIntent = new Intent(this, CameraActivity.class);
        startActivity(cameraIntent);
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        handler.removeCallbacksAndMessages(null);
    }
}