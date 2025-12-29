package com.example.native_toast;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;
import java.io.File;       // for File class
import java.util.Arrays;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import java.util.HashMap;
import java.util.Map;

import androidx.core.app.ActivityCompat;

import com.example.native_toast.NativeToastPlugin;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Locale;
import androidx.camera.video.PendingRecording;


public class CameraActivity extends AppCompatActivity {

    enum Mode {VIDEO, SHORT}

    private Mode currentMode = Mode.VIDEO;

    private PreviewView previewView;
    private VideoCapture<Recorder> videoCapture;
    private Recording activeRecording;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

    private ImageButton recordBtn;
    private TextView videoTimer;
    private TextView shortTimer;
    private ProgressBar shortProgress;

    private final Handler timerHandler = new Handler();
    private int secondsElapsed = 0;
    private static final int SHORT_MAX_SECONDS = 60;

    private static final int PERMISSION_CODE = 101;
    private String currentVideoPath;
    private static final int REQ_VIDEO_EDITOR = 2001;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        previewView = findViewById(R.id.previewView);
        recordBtn = findViewById(R.id.recordBtn);
        videoTimer = findViewById(R.id.videoTimer);
        shortTimer = findViewById(R.id.shortTimer);
        shortProgress = findViewById(R.id.shortProgress);

        ImageButton switchCameraBtn = findViewById(R.id.switchCameraBtn);
        AppCompatButton videoMode = findViewById(R.id.videoMode);
        AppCompatButton shortMode = findViewById(R.id.shortMode);

        // Initialize UI based on current mode
        updateModeUI();
        setActive(videoMode, shortMode);

        videoMode.setOnClickListener(v -> {
            if (activeRecording != null) {
                Toast.makeText(this, "Stop recording first", Toast.LENGTH_SHORT).show();
                return;
            }
            currentMode = Mode.VIDEO;
            updateModeUI();
            setActive(videoMode, shortMode);
        });

        shortMode.setOnClickListener(v -> {
            if (activeRecording != null) {
                Toast.makeText(this, "Stop recording first", Toast.LENGTH_SHORT).show();
                return;
            }
            currentMode = Mode.SHORT;
            updateModeUI();
            setActive(shortMode, videoMode);
        });

        recordBtn.setOnClickListener(v -> {
            if (activeRecording == null) {
                startVideoRecording();
            } else {
                stopVideoRecording();
            }
        });

        switchCameraBtn.setOnClickListener(v -> switchCamera());

        if (hasPermissions()) {
            startCamera();
        } else {
            requestPermissions();
        }
    }

    /* ---------------- CAMERA ---------------- */

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                Recorder recorder = new Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build();

                videoCapture = VideoCapture.withOutput(recorder);

                provider.unbindAll();
                provider.bindToLifecycle(this, cameraSelector, preview, videoCapture);

            } catch (Exception e) {
                Log.e("Camera", "Start failed", e);
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void switchCamera() {
        if (activeRecording != null) {
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
            stopVideoRecording();
        }
        cameraSelector = cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;
        startCamera();
    }

    /* ---------------- RECORDING ---------------- */

    private void startVideoRecording() {
        secondsElapsed = 0;
        startTimer();

        // Update button appearance based on mode
        if (currentMode == Mode.VIDEO) {
            recordBtn.setBackgroundResource(R.drawable.record_active);
            recordBtn.setImageResource(R.drawable.ic_stop);
        } else {
            // Short mode: keep button subtle so progress ring and timer are visible
            recordBtn.setBackgroundResource(R.drawable.record_idle);
//            recordBtn.setImageResource(R.drawable.ic_video);
            recordBtn.setImageDrawable(null);
        }

        File file = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "VID_" + System.currentTimeMillis() + ".mp4");
        currentVideoPath = file.getAbsolutePath();

        FileOutputOptions outputOptions =
                new FileOutputOptions.Builder(file).build();

        PendingRecording pendingRecording =
                videoCapture.getOutput()
                        .prepareRecording(this, outputOptions);

        if (hasPermissions()) {
            pendingRecording = pendingRecording.withAudioEnabled();
        }

        activeRecording =
                pendingRecording.start(
                        ContextCompat.getMainExecutor(this),
                        event -> {
                            if (event instanceof VideoRecordEvent.Finalize) {
                                VideoRecordEvent.Finalize finalize =
                                        (VideoRecordEvent.Finalize) event;

                                stopTimer();

                                if (finalize.hasError()) {
                                    Toast.makeText(
                                            this,
                                            "Recording error: " + finalize.getError(),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    recordBtn.setBackgroundResource(R.drawable.record_idle);
                                    recordBtn.setImageResource(R.drawable.ic_video);
                                    resetTimerUI();
                                } else {
                                    Intent editIntent = new Intent(CameraActivity.this, EditVideoActivity.class);
                                    editIntent.putExtra("video_path", currentVideoPath);
                                    editIntent.putExtra(
                                            "video_uri",
                                            finalize.getOutputResults().getOutputUri().toString()
                                    );

                                    startActivityForResult(editIntent, REQ_VIDEO_EDITOR);
                                    }
                            }
                        }
                );
    }

    private void stopVideoRecording() {
        if (activeRecording != null) {
            activeRecording.stop();
            activeRecording = null;
        }
        stopTimer();
        recordBtn.setBackgroundResource(R.drawable.record_idle);
        recordBtn.setImageResource(R.drawable.ic_video);
        resetTimerUI();
    }
    /* ---------------- TIMER LOGIC ---------------- */

    private void startTimer() {
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            secondsElapsed++;

            if (currentMode == Mode.VIDEO) {
                // Video mode: show MM:SS at top, no limit
                videoTimer.setText(formatTime(secondsElapsed));
            } else {
                // Short mode: show seconds in button, progress ring
                shortTimer.setText(String.valueOf(secondsElapsed));
                updateShortProgress(secondsElapsed);

                // Auto-stop at 15 seconds
                if (secondsElapsed >= SHORT_MAX_SECONDS) {
                    stopVideoRecording();
                    return;
                }
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    /* ---------------- UI HELPERS ---------------- */

    private void updateModeUI() {
        if (currentMode == Mode.VIDEO) {
            videoTimer.setVisibility(TextView.VISIBLE);
            videoTimer.setText("00:00");
            shortTimer.setVisibility(TextView.GONE);
            shortProgress.setVisibility(ProgressBar.GONE);
            recordBtn.setImageResource(R.drawable.ic_video);
            recordBtn.setBackgroundResource(R.drawable.record_idle);
        } else {
            // Short mode
            videoTimer.setVisibility(TextView.GONE);
            shortTimer.setVisibility(TextView.VISIBLE);
            shortTimer.setText("0");
            shortProgress.setVisibility(ProgressBar.VISIBLE);
            shortProgress.setMax(100);
            shortProgress.setProgress(0);
            recordBtn.setImageResource(R.drawable.ic_video);
            recordBtn.setBackgroundResource(R.drawable.record_idle);
        }
    }
    private void resetTimerUI() {
        if (currentMode == Mode.VIDEO) {
            videoTimer.setText("00:00");
        } else {
            shortTimer.setText("0");
            shortProgress.setProgress(0);
        }
    }

    private void updateShortProgress(int sec) {
        // Calculate percentage: if 8 seconds out of 15, that's ~53%
        int progressPercentage = (int) ((sec / (float) SHORT_MAX_SECONDS) * 100);
        shortProgress.setProgress(progressPercentage);
    }

    private String formatTime(int totalSec) {
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec);
    }

    private void setActive(AppCompatButton active, AppCompatButton inactive) {
        active.setBackgroundResource(R.drawable.btn_bg);
        active.setTextColor(getColor(android.R.color.white));

        inactive.setBackgroundResource(R.drawable.btn_bg_two);
        inactive.setTextColor(getColor(android.R.color.darker_gray));
    }

    /* ---------------- PERMISSIONS ---------------- */

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(code, p, r);
        if (code == PERMISSION_CODE && r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera and audio permissions are required", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /* ---------------- LIFECYCLE ---------------- */

    @Override
    public void onBackPressed() {
        if (activeRecording != null) {
            stopVideoRecording();
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_VIDEO_EDITOR) { // editor finished
            if (NativeToastPlugin.pendingResult != null) {
                HashMap<String, Object> map = new HashMap<>();
                String fileP=getLatestVideoPath();
                map.put("videoPath", fileP);
                NativeToastPlugin.pendingResult.success(map);
                NativeToastPlugin.pendingResult = null;
            }

            finish(); // return to Flutter
        }
    }

    private String getLatestVideoPath() {
        File moviesDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (moviesDir == null || !moviesDir.exists()) return null;

        File[] files = moviesDir.listFiles((dir, name) -> name.endsWith(".mp4"));
        if (files == null || files.length == 0) return null;

        // Sort by last modified descending
        Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        return files[0].getAbsolutePath();
    }


}