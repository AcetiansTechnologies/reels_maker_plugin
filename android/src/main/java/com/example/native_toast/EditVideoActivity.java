package com.example.native_toast;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
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
import androidx.media3.common.Effect;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.native_toast.helpers.FilterHelper;
import com.example.native_toast.helpers.TextOverlayHelper;
import com.example.native_toast.helpers.TrimHelper;
import com.example.native_toast.helpers.VideoExporter;
import com.example.native_toast.helpers.VoiceOverHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

@OptIn(markerClass = UnstableApi.class)
public class EditVideoActivity extends AppCompatActivity {

    enum UiMode {
        NORMAL, TRIM, VOICE, CROP, FILTERS
    }

    // Core UI
    private UiMode currentUiMode = UiMode.NORMAL;
    private LinearLayout playbackControls;
    private LinearLayout trimControls;
    private LinearLayout voiceOverControls;
    private RecyclerView thumbnailRecycler;
    
    // Player
    private PlayerView playerView;
    private ExoPlayer player;
    private long videoDurationMs;
    private String videoPath;
    private String videoUri;
    private boolean isMuted = false;
    private boolean isSeeking = false;
    
    // Buttons
    private ImageButton playBtn, backBtn, saveBtn, muteBtn;
    private ImageButton trimBtn, audioBtn, textBtn, voiceoverBtn, filtersBtn, cropBtn;
    private SeekBar seekBar;
    private TextView timeDisplay;
    
    // Trim UI
    private View leftHandle, rightHandle, selectedRangeView, playheadView;
    private TextView startTimeText, endTimeText;
    
    // Crop
    private FrameLayout cropContainer;
    private CropOverlayView cropOverlayView;
    private ImageButton cropDoneBtn;
    private float[] normalizedCropRect = null;
    
    // Voice
    private ImageButton voiceRecordBtn, voiceDeleteBtn;
    private TextView voiceStatusText;
    private CheckBox voiceMixBox;
    
    // Text
    private FrameLayout textOverlayContainer;
    private RelativeLayout textEditorLayout;
    private EditText textEditorInput;
    private ImageButton textBgToggleBtn, textDoneBtn;
    private InputMethodManager imm;
    
    // Handler
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBar;
    
    // === HELPERS ===
    private FilterHelper filterHelper;
    private VoiceOverHelper voiceHelper;
    private TextOverlayHelper textHelper;
    private TrimHelper trimHelper;
    private VideoExporter exporter;

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

        initViews();
        initHelpers();
        setupPlayer();
        setupListeners();
        startUpdateSeekBar();
    }

    private void initViews() {
        playerView = findViewById(R.id.playerView);
        playbackControls = findViewById(R.id.playbackControls);
        trimControls = findViewById(R.id.trimControls);
        voiceOverControls = findViewById(R.id.voiceOverControls);
        thumbnailRecycler = findViewById(R.id.thumbnailRecycler);
        
        playBtn = findViewById(R.id.playBtn);
        backBtn = findViewById(R.id.backBtn);
        saveBtn = findViewById(R.id.saveBtn);
        muteBtn = findViewById(R.id.muteBtn);
        seekBar = findViewById(R.id.seekBar);
        timeDisplay = findViewById(R.id.timeDisplay);
        
        trimBtn = findViewById(R.id.trimBtn);
        audioBtn = findViewById(R.id.audioBtn);
        textBtn = findViewById(R.id.textBtn);
        voiceoverBtn = findViewById(R.id.voiceoverBtn);
        filtersBtn = findViewById(R.id.filtersBtn);
        cropBtn = findViewById(R.id.cropBtn);
        
        leftHandle = findViewById(R.id.leftHandle);
        rightHandle = findViewById(R.id.rightHandle);
        selectedRangeView = findViewById(R.id.selectedRangeView);
        playheadView = findViewById(R.id.playheadView);
        startTimeText = findViewById(R.id.startTimeText);
        endTimeText = findViewById(R.id.endTimeText);
        
        cropContainer = findViewById(R.id.cropContainer);
        cropOverlayView = findViewById(R.id.cropOverlayView);
        cropDoneBtn = findViewById(R.id.cropDoneBtn);
        
        voiceRecordBtn = findViewById(R.id.voiceRecordBtn);
        voiceDeleteBtn = findViewById(R.id.voiceDeleteBtn);
        voiceStatusText = findViewById(R.id.voiceStatusText);
        voiceMixBox = findViewById(R.id.voiceMixBox);
        
        textOverlayContainer = findViewById(R.id.textOverlayContainer);
        textEditorLayout = findViewById(R.id.textEditorLayout);
        textEditorInput = findViewById(R.id.textEditorInput);
        textBgToggleBtn = findViewById(R.id.textBgToggleBtn);
        textDoneBtn = findViewById(R.id.textDoneBtn);
        
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    }

    private void initHelpers() {
        // Filter Helper
        filterHelper = new FilterHelper(this, playerView, thumbnailRecycler, 
            videoUri, videoPath, effect -> {
                // Filter applied callback - effect stored in helper
            });
        
        // Voice Over Helper
        voiceHelper = new VoiceOverHelper(this, getExternalFilesDir(null),
            voiceStatusText, voiceRecordBtn, voiceDeleteBtn, voiceOverControls,
            new VoiceOverHelper.VoiceOverListener() {
                @Override
                public long getPlayerPosition() {
                    return player != null ? player.getCurrentPosition() : 0;
                }
                @Override
                public void onRecordingStateChanged(boolean isRecording) {
                    if (isRecording && player != null) {
                        player.play();
                    }
                }
            });
        
        // Text Overlay Helper
        textHelper = new TextOverlayHelper(this, textOverlayContainer, textEditorLayout,
            textEditorInput, textBgToggleBtn, textDoneBtn, imm,
            new TextOverlayHelper.TextOverlayListener() {
                @Override
                public void onEditingStarted(TextView view) {
                    // Already handled in helper
                }
                @Override
                public void pausePlayer() {
                    if (player != null && player.isPlaying()) {
                        player.pause();
                        updatePlayPauseIcon();
                    }
                }
            });
        
        // Trim Helper
        trimHelper = new TrimHelper(this, trimControls, thumbnailRecycler,
            leftHandle, rightHandle, selectedRangeView, playheadView,
            startTimeText, endTimeText, videoUri, videoPath,
            new TrimHelper.TrimListener() {
                @Override
                public void onTrimChanged(long startMs, long endMs) {
                    // Trim values stored in helper
                }
                @Override
                public ExoPlayer getPlayer() { return player; }
                @Override
                public long getVideoDuration() { return videoDurationMs; }
            });
        
        // Video Exporter
        exporter = new VideoExporter(this, new VideoExporter.ExportListener() {
            @Override
            public void onExportComplete(String outputPath) {
                finishWithResult(outputPath);
            }
            @Override
            public void onExportError(String error) {
                Toast.makeText(EditVideoActivity.this, "Export Failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        playerView.setUseController(false);

        String source = (videoUri != null && !videoUri.isEmpty()) ? videoUri : videoPath;
        if (source == null || source.isEmpty()) {
            Toast.makeText(this, "No video provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        MediaItem mediaItem = MediaItem.fromUri(source);
        player.setMediaItem(mediaItem);
        player.prepare();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    videoDurationMs = player.getDuration();
                    if (trimHelper.getEndTrimMs() == 0) {
                        trimHelper.setTrimRange(0, videoDurationMs);
                    }
                    seekBar.setMax((int) videoDurationMs);
                    updateTimeDisplay();
                    
                    if (currentUiMode == UiMode.TRIM) {
                        trimHelper.startLoop();
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    player.seekTo(0);
                    player.pause();
                    updatePlayPauseIcon();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseIcon();
                if (currentUiMode == UiMode.TRIM) {
                    if (isPlaying) trimHelper.startLoop();
                    else trimHelper.stopLoop();
                }
            }
        });
    }

    private void setupListeners() {
        playBtn.setOnClickListener(v -> {
            if (player != null) {
                if (player.isPlaying()) player.pause();
                else player.play();
            }
        });

        backBtn.setOnClickListener(v -> handleBack());
        saveBtn.setOnClickListener(v -> startExport());
        muteBtn.setOnClickListener(v -> toggleMute());
        
        trimBtn.setOnClickListener(v -> enterTrimMode());
        voiceoverBtn.setOnClickListener(v -> enterVoiceMode());
        textBtn.setOnClickListener(v -> textHelper.enterTextMode());
        filtersBtn.setOnClickListener(v -> enterFiltersMode());
        cropBtn.setOnClickListener(v -> enterCropMode());
        cropDoneBtn.setOnClickListener(v -> exitCropMode(true));
        
        voiceRecordBtn.setOnClickListener(v -> {
            if (voiceHelper.isRecording()) {
                voiceHelper.stopRecording();
                if (player != null) player.pause();
            } else {
                voiceHelper.startRecording();
            }
        });
        
        voiceDeleteBtn.setOnClickListener(v -> voiceHelper.deleteRecording());

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
    }

    // ============= MODE SWITCHING =============

    private void enterTrimMode() {
        currentUiMode = UiMode.TRIM;
        voiceOverControls.setVisibility(View.GONE);
        playbackControls.setVisibility(View.GONE);
        trimControls.setVisibility(View.VISIBLE);
        
        playheadView.setVisibility(View.VISIBLE);
        startTimeText.setVisibility(View.VISIBLE);
        endTimeText.setVisibility(View.VISIBLE);
        leftHandle.setVisibility(View.VISIBLE);
        rightHandle.setVisibility(View.VISIBLE);
        selectedRangeView.setVisibility(View.VISIBLE);
        
        View processingContainer = findViewById(R.id.trimSelectionContainer);
        if (processingContainer != null) {
            processingContainer.setBackgroundResource(R.drawable.bg_trim_selection);
            if (processingContainer instanceof ViewGroup) {
                ((ViewGroup) processingContainer).setClipChildren(true);
                ((ViewGroup) processingContainer).setClipToPadding(true);
            }
        }
        
        filterHelper.removeDecoration();

        if (player != null) {
            player.pause();
            player.seekTo(trimHelper.getStartTrimMs());
        }
        trimHelper.initTrimThumbnailsAndHandles();
    }

    private void enterVoiceMode() {
        currentUiMode = UiMode.VOICE;
        trimControls.setVisibility(View.GONE);
        playbackControls.setVisibility(View.VISIBLE);
        voiceOverControls.setVisibility(View.VISIBLE);
        playheadView.setVisibility(View.GONE);

        if (player != null) player.pause();
        voiceHelper.updateUIForMode();
    }

    private void enterCropMode() {
        currentUiMode = UiMode.CROP;
        playbackControls.setVisibility(View.GONE);
        trimControls.setVisibility(View.GONE);
        voiceOverControls.setVisibility(View.GONE);
        cropContainer.setVisibility(View.VISIBLE);
        
        if (player != null) {
            player.pause();
            updatePlayPauseIcon();
        }
    }
    
    private void exitCropMode(boolean save) {
        if (save) {
            normalizedCropRect = cropOverlayView.getNormalizedCrop();
        }
        exitToNormalMode();
    }

    private void enterFiltersMode() {
        currentUiMode = UiMode.FILTERS;
        playbackControls.setVisibility(View.VISIBLE);
        voiceOverControls.setVisibility(View.GONE);
        cropContainer.setVisibility(View.GONE);
        trimControls.setVisibility(View.VISIBLE);
        
        playheadView.setVisibility(View.GONE);
        startTimeText.setVisibility(View.GONE);
        endTimeText.setVisibility(View.GONE);
        leftHandle.setVisibility(View.GONE);
        rightHandle.setVisibility(View.GONE);
        selectedRangeView.setVisibility(View.GONE);
        
        View processingContainer = findViewById(R.id.trimSelectionContainer);
        if (processingContainer != null) {
            processingContainer.setVisibility(View.VISIBLE);
            processingContainer.setBackground(null);
        }
        
        filterHelper.setupRecycler();
        thumbnailRecycler.setVisibility(View.VISIBLE);
    }

    private void exitToNormalMode() {
        currentUiMode = UiMode.NORMAL;
        playbackControls.setVisibility(View.VISIBLE);
        trimControls.setVisibility(View.GONE);
        voiceOverControls.setVisibility(View.GONE);
        cropContainer.setVisibility(View.GONE);
        thumbnailRecycler.setVisibility(View.GONE);
        playheadView.setVisibility(View.GONE);

        trimHelper.stopLoop();
        if (player != null) updatePlayPauseIcon();
    }

    // ============= EXPORT =============
    
    private void startExport() {
        if (player != null && player.isPlaying()) player.pause();
        
        String inputPath = (videoPath != null) ? videoPath : getSafeInputPath();
        String outputPath = getOutputPath();
        
        boolean hasVoiceOver = voiceHelper.hasRecording();
        boolean removeMainAudio = isMuted;
        if (!removeMainAudio && hasVoiceOver && !voiceMixBox.isChecked()) {
            removeMainAudio = true;
        }
        
        VideoExporter.ExportConfig config = new VideoExporter.ExportConfig(inputPath, outputPath);
        config.startTrimMs = trimHelper.getStartTrimMs();
        config.endTrimMs = trimHelper.getEndTrimMs();
        config.removeMainAudio = removeMainAudio;
        config.normalizedCropRect = normalizedCropRect;
        config.filterEffect = filterHelper.getCurrentEffect();
        config.overlayContainer = textHelper.getOverlayContainer();
        config.voiceOverPath = voiceHelper.getVoiceOverPath();
        config.storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);
        
        exporter.export(config);
    }

    // ============= HELPERS =============

    private void updatePlayPauseIcon() {
        if (player != null && player.isPlaying()) {
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
        muteBtn.setImageResource(isMuted ? R.drawable.ic_no_music : R.drawable.ic_music);
    }

    private void startUpdateSeekBar() {
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (player != null && !isSeeking) {
                    seekBar.setProgress((int) player.getCurrentPosition());
                    updateTimeDisplay();
                    if (currentUiMode == UiMode.TRIM) trimHelper.updatePlayheadPosition();
                }
                handler.postDelayed(this, 200);
            }
        };
        handler.post(updateSeekBar);
    }

    private void updateTimeDisplay() {
        if (player != null) {
            long current = player.getCurrentPosition();
            long duration = player.getDuration();
            timeDisplay.setText(formatTime((int) current) + " / " + formatTime((int) duration));
        }
    }

    private String formatTime(int millis) {
        int totalSeconds = millis / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private void handleBack() {
        if (currentUiMode != UiMode.NORMAL) {
            exitToNormalMode();
        } else {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    private void finishWithResult(String finalPath) {
        if (finalPath == null || finalPath.isEmpty()) {
            setResult(Activity.RESULT_CANCELED);
        } else {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("resultPath", finalPath);
            setResult(Activity.RESULT_OK, resultIntent);
        }
        finish();
    }

    private String getOutputPath() {
        // Save to Movies directory (same as input) for consistency
        File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);
        return new File(dir, "edited_" + System.currentTimeMillis() + ".mp4").getAbsolutePath();
    }

    private String getSafeInputPath() {
        if (videoUri == null) return videoPath;
        try {
            File tempFile = new File(getCacheDir(), "temp_video.mp4");
            try (InputStream is = getContentResolver().openInputStream(Uri.parse(videoUri));
                 OutputStream os = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) > 0) os.write(buffer, 0, len);
            }
            return tempFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        handler.removeCallbacksAndMessages(null);
        voiceHelper.release();
    }
}