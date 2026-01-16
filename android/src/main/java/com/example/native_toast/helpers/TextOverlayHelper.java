package com.example.native_toast.helpers;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.native_toast.models.TextStickerData;

/**
 * Helper class for text overlay/sticker functionality.
 */
public class TextOverlayHelper {

    public interface TextOverlayListener {
        void onEditingStarted(TextView view);
        void pausePlayer();
    }

    private final Activity activity;
    private final FrameLayout overlayContainer;
    private final RelativeLayout editorLayout;
    private final EditText editorInput;
    private final ImageButton bgToggleBtn;
    private final InputMethodManager imm;
    private final TextOverlayListener listener;

    private TextView currentlyEditingView = null;
    private int currentTextBgMode = 0; // 0=None, 1=White, 2=Black

    public TextOverlayHelper(Activity activity, FrameLayout overlayContainer,
                            RelativeLayout editorLayout, EditText editorInput,
                            ImageButton bgToggleBtn, ImageButton doneBtn,
                            InputMethodManager imm, TextOverlayListener listener) {
        this.activity = activity;
        this.overlayContainer = overlayContainer;
        this.editorLayout = editorLayout;
        this.editorInput = editorInput;
        this.bgToggleBtn = bgToggleBtn;
        this.imm = imm;
        this.listener = listener;
        
        // Setup done button
        doneBtn.setOnClickListener(v -> handleDone());
        bgToggleBtn.setOnClickListener(v -> toggleBackgroundMode());
    }

    public void enterTextMode() {
        listener.pausePlayer();
        editorLayout.setVisibility(View.VISIBLE);
        
        if (currentlyEditingView != null) {
            TextStickerData data = (TextStickerData) currentlyEditingView.getTag();
            if (data != null) {
                editorInput.setText(data.text);
                currentTextBgMode = data.bgMode;
            }
        } else {
            editorInput.setText("");
            currentTextBgMode = 0;
        }
        
        editorInput.requestFocus();
        updateEditorStyle();
        if (imm != null) imm.showSoftInput(editorInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void toggleBackgroundMode() {
        currentTextBgMode = (currentTextBgMode + 1) % 3;
        updateEditorStyle();
    }

    private void updateEditorStyle() {
        switch (currentTextBgMode) {
            case 0: // None
                editorInput.setTextColor(Color.WHITE);
                editorInput.setBackground(null);
                break;
            case 1: // White
                editorInput.setTextColor(Color.BLACK);
                editorInput.setBackground(createRoundedBackground(Color.WHITE));
                break;
            case 2: // Black
                editorInput.setTextColor(Color.WHITE);
                editorInput.setBackground(createRoundedBackground(Color.BLACK));
                break;
        }
    }

    private void handleDone() {
        String text = editorInput.getText().toString().trim();
        if (!text.isEmpty()) {
            if (currentlyEditingView != null) {
                updateSticker(currentlyEditingView, text, currentTextBgMode);
            } else {
                addTextSticker(text, currentTextBgMode);
            }
        }
        currentlyEditingView = null;
        if (imm != null) imm.hideSoftInputFromWindow(editorInput.getWindowToken(), 0);
        editorLayout.setVisibility(View.GONE);
    }
    
    private void updateSticker(TextView tv, String text, int bgMode) {
        tv.setText(text);
        tv.setTag(new TextStickerData(text, bgMode));
        
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
    }

    private void addTextSticker(String text, int bgMode) {
        TextView tv = new TextView(activity);
        tv.setText(text);
        tv.setTextSize(24);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(30, 20, 30, 20);
        tv.setTag(new TextStickerData(text, bgMode));
        
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

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        tv.setLayoutParams(params);

        overlayContainer.addView(tv);
        
        tv.post(() -> {
            tv.setX((overlayContainer.getWidth() - tv.getWidth()) / 2f);
            tv.setY((overlayContainer.getHeight() - tv.getHeight()) / 2f);
        });
        
        setupStickerGestures(tv);
    }
    
    public GradientDrawable createRoundedBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(30f);
        return drawable;
    }

    private void setupStickerGestures(View view) {
        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(activity, 
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    float scaleFactor = detector.getScaleFactor();
                    float newScaleX = view.getScaleX() * scaleFactor;
                    float newScaleY = view.getScaleY() * scaleFactor;
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
                        if (Math.abs(event.getRawX() - lastTouchX) > 10 || 
                            Math.abs(event.getRawY() - lastTouchY) > 10) {
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
                            currentlyEditingView = (TextView) v;
                            listener.onEditingStarted(currentlyEditingView);
                            enterTextMode();
                        }
                        break;
                }
                return true;
            }
        });
    }

    public FrameLayout getOverlayContainer() {
        return overlayContainer;
    }

    public void setCurrentlyEditingView(TextView view) {
        this.currentlyEditingView = view;
    }
}
