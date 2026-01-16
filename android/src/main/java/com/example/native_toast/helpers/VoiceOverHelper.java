package com.example.native_toast.helpers;

import android.app.Activity;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.native_toast.R;

import java.io.File;
import java.io.IOException;

/**
 * Helper class for voice-over recording and playback functionality.
 */
public class VoiceOverHelper {

    public interface VoiceOverListener {
        long getPlayerPosition();
        void onRecordingStateChanged(boolean isRecording);
    }

    private final Activity activity;
    private final File storageDir;
    private final TextView statusText;
    private final ImageButton recordBtn;
    private final ImageButton deleteBtn;
    private final LinearLayout controlsContainer;
    private final VoiceOverListener listener;

    private MediaRecorder mediaRecorder;
    private MediaPlayer voicePlayer;
    private ImageButton playBtn;
    
    private String voiceOverPath = null;
    private boolean isRecordingVoice = false;
    private long voiceStartMs = 0;

    public VoiceOverHelper(Activity activity, File storageDir, TextView statusText,
                          ImageButton recordBtn, ImageButton deleteBtn,
                          LinearLayout controlsContainer, VoiceOverListener listener) {
        this.activity = activity;
        this.storageDir = storageDir;
        this.statusText = statusText;
        this.recordBtn = recordBtn;
        this.deleteBtn = deleteBtn;
        this.controlsContainer = controlsContainer;
        this.listener = listener;
        
        setupPlayButton();
    }

    private void setupPlayButton() {
        float density = activity.getResources().getDisplayMetrics().density;
        playBtn = new ImageButton(activity);
        playBtn.setLayoutParams(new LinearLayout.LayoutParams(
            (int)(48 * density),
            (int)(48 * density)
        ));
        playBtn.setBackgroundResource(R.drawable.btn_bg_sq);
        playBtn.setImageResource(R.drawable.ic_play);
        playBtn.setColorFilter(Color.WHITE);
        playBtn.setPadding(24, 24, 24, 24);
        playBtn.setContentDescription("Play Voice");
        playBtn.setVisibility(View.GONE);
        playBtn.setOnClickListener(v -> playPreview());
        
        // Add to controls container if it has a button container
        if (controlsContainer.getChildCount() > 2) {
            View btnContainer = controlsContainer.getChildAt(2);
            if (btnContainer instanceof LinearLayout) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    (int)(48 * density),
                    (int)(48 * density)
                );
                lp.setMarginStart((int)(24 * density));
                playBtn.setLayoutParams(lp);
                ((LinearLayout) btnContainer).addView(playBtn);
            }
        }
    }

    public String getVoiceOverPath() {
        return voiceOverPath;
    }

    public long getVoiceStartMs() {
        return voiceStartMs;
    }

    public boolean isRecording() {
        return isRecordingVoice;
    }

    public boolean hasRecording() {
        return voiceOverPath != null && new File(voiceOverPath).exists();
    }

    public void startRecording() {
        if (voiceOverPath != null && new File(voiceOverPath).exists()) {
            new File(voiceOverPath).delete();
        }

        voiceOverPath = new File(storageDir, "voice_temp.aac").getAbsolutePath();
        voiceStartMs = listener.getPlayerPosition();

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(voiceOverPath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecordingVoice = true;
            statusText.setText("Recording...");
            recordBtn.setColorFilter(0xFFFF0000);
            listener.onRecordingStateChanged(true);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Recording failed", Toast.LENGTH_SHORT).show();
            voiceOverPath = null;
        }
    }

    public void stopRecording() {
        if (isRecordingVoice && mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaRecorder = null;
            isRecordingVoice = false;

            statusText.setText("Voice recorded. Tap play to preview.");
            recordBtn.clearColorFilter();
            deleteBtn.setVisibility(View.VISIBLE);
            playBtn.setVisibility(View.VISIBLE);
            listener.onRecordingStateChanged(false);
        }
    }

    public void deleteRecording() {
        if (voiceOverPath != null && new File(voiceOverPath).exists()) {
            new File(voiceOverPath).delete();
        }
        voiceOverPath = null;
        statusText.setText("Tap mic to record");
        deleteBtn.setVisibility(View.GONE);
        playBtn.setVisibility(View.GONE);
    }

    public void playPreview() {
        if (voiceOverPath == null || !new File(voiceOverPath).exists()) {
            Toast.makeText(activity, "No voice recording", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (voicePlayer != null) {
            voicePlayer.release();
            voicePlayer = null;
            statusText.setText("Voice recorded. Tap play to preview.");
            return;
        }
        
        try {
            voicePlayer = new MediaPlayer();
            voicePlayer.setDataSource(voiceOverPath);
            voicePlayer.prepare();
            voicePlayer.setOnCompletionListener(mp -> {
                statusText.setText("Voice recorded. Tap play to preview.");
                voicePlayer.release();
                voicePlayer = null;
            });
            voicePlayer.start();
            statusText.setText("Playing voice...");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Playback failed", Toast.LENGTH_SHORT).show();
        }
    }

    public void updateUIForMode() {
        if (hasRecording()) {
            deleteBtn.setVisibility(View.VISIBLE);
            playBtn.setVisibility(View.VISIBLE);
            statusText.setText("Voice recorded. Tap play to preview.");
        } else {
            deleteBtn.setVisibility(View.GONE);
            playBtn.setVisibility(View.GONE);
            statusText.setText("Tap mic to record");
        }
    }

    public void release() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {}
            mediaRecorder = null;
        }
        if (voicePlayer != null) {
            try {
                voicePlayer.release();
            } catch (Exception e) {}
            voicePlayer = null;
        }
    }
}
