package com.example.native_toast;

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
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
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

import android.view.Gravity;
import android.view.ScaleGestureDetector;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.view.inputmethod.InputMethodManager;
import android.graphics.Color;
import android.graphics.Canvas;
import androidx.media3.effect.Crop;
import androidx.media3.effect.OverlayEffect;
import androidx.media3.effect.BitmapOverlay;
import androidx.media3.effect.TextureOverlay;
import com.google.common.collect.ImmutableList;

import android.graphics.drawable.GradientDrawable;

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
        NORMAL, TRIM, VOICE, CROP
    }

    private TextView startTimeText, endTimeText;
    private long startTrimMs = 0;
    private long endTrimMs = 0;

    private UiMode currentUiMode = UiMode.NORMAL;
    private View leftHandle, rightHandle;

    private long videoDurationMs;
    private float startPercent = 0f;
    private float endPercent = 1f;

    private LinearLayout playbackControls;
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
    private ImageButton trimBtn, audioBtn, textBtn, voiceoverBtn, filtersBtn, cropBtn;
    private SeekBar seekBar;
    private TextView timeDisplay;
    private String videoPath;
    private String videoUri;
    private View selectedRangeView;
    private View playheadView;
    
    // Crop Variables
    private FrameLayout cropContainer;
    private CropOverlayView cropOverlayView;
    private ImageButton cropDoneBtn;
    private float[] normalizedCropRect = null; // null if no crop

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

    // Text Overlay Variables
    private FrameLayout textOverlayContainer;
    private RelativeLayout textEditorLayout;
    private EditText textEditorInput;
    private ImageButton textBgToggleBtn, textDoneBtn;
    private int currentTextBgMode = 0; // 0=None, 1=White, 2=Black
    private InputMethodManager imm;

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

        // Text Overlay Init
        textOverlayContainer = findViewById(R.id.textOverlayContainer);
        textEditorLayout = findViewById(R.id.textEditorLayout);
        textEditorInput = findViewById(R.id.textEditorInput);
        textBgToggleBtn = findViewById(R.id.textBgToggleBtn);
        textDoneBtn = findViewById(R.id.textDoneBtn);
        imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);

        textBgToggleBtn.setOnClickListener(v -> toggleTextBackgroundMode());
        textDoneBtn.setOnClickListener(v -> handleTextDone());

        // Crop Init
        cropContainer = findViewById(R.id.cropContainer);
        cropOverlayView = findViewById(R.id.cropOverlayView);
        cropDoneBtn = findViewById(R.id.cropDoneBtn);
        cropBtn = findViewById(R.id.cropBtn);

        cropBtn.setOnClickListener(v -> enterCropMode());
        cropDoneBtn.setOnClickListener(v -> exitCropMode(true));
        filtersBtn.setOnClickListener(v -> Toast.makeText(this, "Filters coming soon", Toast.LENGTH_SHORT).show());
        
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
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeeking = false;
            }
        });

        backBtn.setOnClickListener(v -> handleBack());
        saveBtn.setOnClickListener(v -> startTrimExport());
        trimBtn.setOnClickListener(v -> enterTrimMode());
        voiceoverBtn.setOnClickListener(v -> enterVoiceMode());

        audioBtn.setOnClickListener(v -> Toast.makeText(this, "Audio editor coming soon", Toast.LENGTH_SHORT).show());
        textBtn.setOnClickListener(v -> enterTextMode());
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
        cropContainer.setVisibility(View.GONE);
        playheadView.setVisibility(View.GONE);

        handler.removeCallbacks(trimLoopRunnable);
        if (player != null) updatePlayPauseIcon();
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

    // ============= TEXT EDITOR LOGIC =============

    private void enterTextMode() {
        if (player != null && player.isPlaying()) {
            player.pause();
            updatePlayPauseIcon();
        }
        textEditorLayout.setVisibility(View.VISIBLE);
        textEditorInput.setText("");
        textEditorInput.requestFocus();
        currentTextBgMode = 0;
        updateTextEditorStyle();
        // Show keyboard
        if (imm != null) imm.showSoftInput(textEditorInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void toggleTextBackgroundMode() {
        currentTextBgMode = (currentTextBgMode + 1) % 3;
        updateTextEditorStyle();
    }

    private void updateTextEditorStyle() {
        switch (currentTextBgMode) {
            case 0: // None
                textEditorInput.setTextColor(Color.WHITE);
                textEditorInput.setBackground(null);
                textBgToggleBtn.setImageResource(R.drawable.ic_mackeup); 
                break;
            case 1: // White
                textEditorInput.setTextColor(Color.BLACK);
                textEditorInput.setBackground(createRoundedBackground(Color.WHITE));
                break;
            case 2: // Black
                textEditorInput.setTextColor(Color.WHITE);
                textEditorInput.setBackground(createRoundedBackground(Color.BLACK));
                break;
        }
    }

    private void handleTextDone() {
        String text = textEditorInput.getText().toString().trim();
        if (!text.isEmpty()) {
            addTextSticker(text, currentTextBgMode);
        }
        // Hide keyboard
        if (imm != null) imm.hideSoftInputFromWindow(textEditorInput.getWindowToken(), 0);
        textEditorLayout.setVisibility(View.GONE);
    }

    private void addTextSticker(String text, int bgMode) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(24);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(30, 20, 30, 20); // Increased padding for better look

        // Apply style
        if (bgMode == 0) {
            tv.setTextColor(Color.WHITE);
            tv.setBackground(null);
        } else if (bgMode == 1) {
            tv.setTextColor(Color.BLACK);
            tv.setBackground(createRoundedBackground(Color.WHITE));
        } else {
            tv.setTextColor(Color.WHITE);
            tv.setBackground(createRoundedBackground(Color.BLACK));
        }

        // Layout params (center initially)
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        tv.setLayoutParams(params);

        textOverlayContainer.addView(tv);
        
        // Center the view in the container explicitly after layout
        tv.post(() -> {
            tv.setX((textOverlayContainer.getWidth() - tv.getWidth()) / 2f);
            tv.setY((textOverlayContainer.getHeight() - tv.getHeight()) / 2f);
        });
        
        setupStickerGestures(tv);
    }
    
    private GradientDrawable createRoundedBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(30f); // 30px radius
        return drawable;
    }

    private void setupStickerGestures(View view) {
        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                float newScaleX = view.getScaleX() * scaleFactor;
                float newScaleY = view.getScaleY() * scaleFactor;
                // Limit scale
                newScaleX = Math.max(0.5f, Math.min(newScaleX, 5.0f));
                newScaleY = Math.max(0.5f, Math.min(newScaleY, 5.0f));
                view.setScaleX(newScaleX);
                view.setScaleY(newScaleY);
                return true;
            }
        });

        view.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;
            float lastTouchX, lastTouchY;
            boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleDetector.onTouchEvent(event);
                if (scaleDetector.isInProgress()) return true;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        lastTouchX = event.getRawX();
                        lastTouchY = event.getRawY();
                        isDragging = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(event.getRawX() - lastTouchX) > 10 || Math.abs(event.getRawY() - lastTouchY) > 10) {
                            isDragging = true;
                            v.animate()
                                    .x(event.getRawX() + dX)
                                    .y(event.getRawY() + dY)
                                    .setDuration(0)
                                    .start();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            // Maybe edit text again on click? (Optional)
                        }
                        break;
                }
                return true;
            }
        });
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

        MediaItem.ClippingConfiguration clipping = new MediaItem.ClippingConfiguration.Builder().setStartPositionMs(startTrimMs).setEndPositionMs(endTrimMs).build();

        EditedMediaItem.Builder videoEditedItemBuilder = new EditedMediaItem.Builder(videoItem.buildUpon().setClippingConfiguration(clipping).build()).setRemoveAudio(removeMainAudio);

        // ADD EFFECTS
        ImmutableList.Builder<androidx.media3.common.Effect> effectsBuilder = new ImmutableList.Builder<>();
        
        // 1. Crop Effect
        if (normalizedCropRect != null) {
            // Mapping Normalized [L, T, R, B] to Pixel Coordinates? 
            // Effect Crop(left, right, bottom, top) relative to input values.
            // Crop(float left, float right, float bottom, float top) constructor takes values in range -1 to 1 for normalized? 
            // No, Media3 Crop doc says: "The values are in the range [-1, 1], where (-1, -1) corresponds to the bottom-left corner".
            // Wait, let's verify Media3 Crop API.
            // "Crop(float left, float right, float bottom, float top)" -> "Removes the outer portion of the frame."
            // "The coordinates are normalized to the interval [-1, 1], with (-1, -1) being the bottom-left corner."
            
            // My UI returns 0..1 where (0,0) is TOP-LEFT, (1,1) is BOTTOM-RIGHT.
            // Map UI(0..1) to GL(-1..1):
            // x_gl = x_ui * 2 - 1
            // y_gl = (1 - y_ui) * 2 - 1  (Y is flipped in GL usually)
            
            float leftUi = normalizedCropRect[0];
            float topUi = normalizedCropRect[1];
            float rightUi = normalizedCropRect[2];
            float bottomUi = normalizedCropRect[3];
            
            float leftGl = leftUi * 2 - 1;
            float rightGl = rightUi * 2 - 1;
            // Top in UI (0) -> Top in GL (1). Bottom in UI (1) -> Bottom in GL (-1).
            float topGl = (1 - topUi) * 2 - 1; 
            float bottomGl = (1 - bottomUi) * 2 - 1;
            
            // Constructor: Crop(float left, float right, float bottom, float top)
            effectsBuilder.add(new Crop(leftGl, rightGl, bottomGl, topGl));
        }

        // 2. Overlay Effect
        if (textOverlayContainer.getChildCount() > 0) {
            Bitmap overlayBitmap = createBitmapFromView(textOverlayContainer);
            if (overlayBitmap != null) {
                try {
                   BitmapOverlay bitmapOverlay = BitmapOverlay.createStaticBitmapOverlay(overlayBitmap);
                   effectsBuilder.add(new OverlayEffect(ImmutableList.of(bitmapOverlay)));
                } catch (Exception e) {
                   e.printStackTrace();
                }
            }
        }
        
        // Correct usage for Transformer:
        videoEditedItemBuilder.setEffects(new androidx.media3.transformer.Effects(
               ImmutableList.of(),
               effectsBuilder.build()
        ));
        
        EditedMediaItem videoEditedItem = videoEditedItemBuilder.build();

        // 2. Sequences
        List<EditedMediaItemSequence> sequences = new ArrayList<>();
        sequences.add(new EditedMediaItemSequence.Builder(videoEditedItem).build());

        if (hasVoiceOver) {
            MediaItem voiceItem = MediaItem.fromUri(voiceOverPath);
            EditedMediaItem voiceEditedItem = new EditedMediaItem.Builder(voiceItem).build();
            sequences.add(new EditedMediaItemSequence.Builder(voiceEditedItem).build());
        }

        Composition composition = new Composition.Builder(sequences).build();

        transformer = new Transformer.Builder(this).addListener(new Transformer.Listener() {
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
        }).build();

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
            try (InputStream in = getContentResolver().openInputStream(Uri.parse(videoUri)); OutputStream out = new FileOutputStream(temp)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            }
            return temp.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
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
                        float startX = (startTrimMs / (float) videoDurationMs) * width;
                        float endX = (endTrimMs / (float) videoDurationMs) * width;
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
                    if (player != null) {
                        player.seekTo(startTrimMs);
                        player.pause();
                    }
                } else {
                    float minRight = leftHandle.getX() + leftHandle.getWidth();
                    minRight = Math.max(minRight, leftHandle.getX() + 30 + leftHandle.getWidth());
                    x = Math.max(x, minRight);
                    handle.setX(x);
                    updateTrimTimes();
                    if (player != null) {
                        player.seekTo(endTrimMs);
                        player.pause();
                    }
                }
                updateSelectedRangeUI();
                updatePlayheadPosition();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
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
        } catch (Exception e) {
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
            }
        }
        return list;
    }

    private void startUpdateSeekBar() {
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
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
        timeDisplay.setText(String.format(Locale.getDefault(), "%s / %s", formatTime((int) current), formatTime((int) total)));
    }

    private String formatTime(int millis) {
        if (millis < 0) return "0:00";
        int totalSeconds = millis / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
    private void handleBack() {
        if (currentUiMode != UiMode.NORMAL) {
            exitToNormalMode();
        } else {
            if (player != null) player.stop();
            startActivity(new Intent(this, CameraActivity.class));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBack();
    }


    private Bitmap createBitmapFromView(View view) {
        if (view.getWidth() == 0 || view.getHeight() == 0) return null;
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
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