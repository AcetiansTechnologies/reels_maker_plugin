package com.example.native_toast;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;
import android.app.Activity;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;

public class CameraActivity extends AppCompatActivity {

    // Camera preview surface
    private PreviewView previewView;

    // Image and video capture use cases
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;

    // Holds the current recording session
    private Recording activeRecording;

    // Default camera is back
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

    private static final int PERMISSION_CODE = 101;
    private static final String TAG = "CameraActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);

        Button captureBtn = findViewById(R.id.captureImageBtn);
        Button recordBtn = findViewById(R.id.recordVideoBtn);
        Button switchBtn = findViewById(R.id.switchCameraBtn);

        // Take photo
        captureBtn.setOnClickListener(v -> takePhoto());

        // Start / stop video recording
        recordBtn.setOnClickListener(v -> {
            if (activeRecording != null) {
                stopVideoRecording();
                recordBtn.setText("Record Video");
            } else {
                startVideoRecording();
                recordBtn.setText("Stop");
            }
        });

        // Switch front/back camera
        switchBtn.setOnClickListener(v -> switchCamera());

        // Permissions check before starting camera
        if (arePermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, PERMISSION_CODE);
        }
    }

    // Simple permission check
    private boolean arePermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    // Initializes CameraX and binds use cases
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Preview setup
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Image capture setup
                imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();

                // Video recorder setup
                Recorder recorder = new Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build();

                videoCapture = VideoCapture.withOutput(recorder);

                // Rebind everything
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture);

            } catch (Exception e) {
                Log.e(TAG, "Failed to start camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Toggle between front and back camera
    private void switchCamera() {
        if (activeRecording != null) {
            stopVideoRecording();
        }

        cameraSelector = cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;

        startCamera();
    }

    // Capture still image
    private void takePhoto() {
        if (imageCapture == null) return;

        File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                Toast.makeText(CameraActivity.this, "Photo saved", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed", exception);
            }
        });
    }

    // Start video recording
    private void startVideoRecording() {
        if (videoCapture == null) return;

        File videoFile = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "VID_" + System.currentTimeMillis() + ".mp4");

        FileOutputOptions options = new FileOutputOptions.Builder(videoFile).build();

        activeRecording = videoCapture.getOutput().prepareRecording(this, options).withAudioEnabled().start(ContextCompat.getMainExecutor(this), event -> {

            if (event instanceof VideoRecordEvent.Start) {
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
            }

            if (event instanceof VideoRecordEvent.Finalize finalize) {

                Uri videoUri = finalize.getOutputResults().getOutputUri();

                // Return result to plugin
                Intent resultIntent = new Intent();
                resultIntent.setData(videoUri);
                setResult(Activity.RESULT_OK, resultIntent);

                activeRecording = null;
                finish(); // VERY IMPORTANT
            }
        });
    }

    // Stop recording safely
    private void stopVideoRecording() {
        if (activeRecording != null) {
            activeRecording.stop();
        }
    }


    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CODE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            if (granted) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera and audio permissions required", Toast.LENGTH_LONG).show();
            }
        }
    }
}
