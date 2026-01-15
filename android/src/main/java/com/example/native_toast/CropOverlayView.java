package com.example.native_toast;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class CropOverlayView extends View {

    private Paint borderPaint;
    private Paint cornerPaint;
    private Paint shadowPaint;
    private RectF cropRect;
    private float cornerSize = 40f;
    private float minSize = 100f;

    private int activeEdge = 0; // 0=none, 1=TL, 2=TR, 3=BL, 4=BR, 5=Center
    private float lastX, lastY;

    public CropOverlayView(Context context) {
        super(context);
        init();
    }

    public CropOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);

        cornerPaint = new Paint();
        cornerPaint.setColor(Color.WHITE);
        cornerPaint.setStyle(Paint.Style.FILL);

        shadowPaint = new Paint();
        shadowPaint.setColor(0xAA000000); // Dark overlay
        shadowPaint.setStyle(Paint.Style.FILL);
        
        cropRect = new RectF(100, 100, 600, 600);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            float w = getWidth();
            float h = getHeight();
            float cw = w * 0.8f;
            float ch = h * 0.8f;
            cropRect.set((w-cw)/2, (h-ch)/2, (w+cw)/2, (h+ch)/2);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        float w = getWidth();
        float h = getHeight();
        
        // Shadows
        canvas.drawRect(0, 0, w, cropRect.top, shadowPaint);
        canvas.drawRect(0, cropRect.bottom, w, h, shadowPaint);
        canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, shadowPaint);
        canvas.drawRect(cropRect.right, cropRect.top, w, cropRect.bottom, shadowPaint);

        // Border
        canvas.drawRect(cropRect, borderPaint);

        // Corners
        float t = 8f; 
        float l = 40f; 
        
        // TL
        canvas.drawRect(cropRect.left - t/2, cropRect.top - t/2, cropRect.left + l, cropRect.top + t/2, cornerPaint);
        canvas.drawRect(cropRect.left - t/2, cropRect.top - t/2, cropRect.left + t/2, cropRect.top + l, cornerPaint);
        
        // TR
        canvas.drawRect(cropRect.right - l, cropRect.top - t/2, cropRect.right + t/2, cropRect.top + t/2, cornerPaint);
        canvas.drawRect(cropRect.right - t/2, cropRect.top - t/2, cropRect.right + t/2, cropRect.top + l, cornerPaint);
        
        // BL
        canvas.drawRect(cropRect.left - t/2, cropRect.bottom - t/2, cropRect.left + l, cropRect.bottom + t/2, cornerPaint);
        canvas.drawRect(cropRect.left - t/2, cropRect.bottom - l, cropRect.left + t/2, cropRect.bottom + t/2, cornerPaint);
        
        // BR
        canvas.drawRect(cropRect.right - l, cropRect.bottom - t/2, cropRect.right + t/2, cropRect.bottom + t/2, cornerPaint);
        canvas.drawRect(cropRect.right - t/2, cropRect.bottom - l, cropRect.right + t/2, cropRect.bottom + t/2, cornerPaint);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                activeEdge = getHitEdge(x, y);
                lastX = x;
                lastY = y;
                return activeEdge != 0;
                
            case MotionEvent.ACTION_MOVE:
                float dx = x - lastX;
                float dy = y - lastY;
                updateCrop(x, y, dx, dy);
                lastX = x;
                lastY = y;
                invalidate();
                return true;
                
            case MotionEvent.ACTION_UP:
                activeEdge = 0;
                return true;
        }
        return super.onTouchEvent(event);
    }
    
    private int getHitEdge(float x, float y) {
        float touchSlop = 60f;
        if (dist(x,y, cropRect.left, cropRect.top) < touchSlop) return 1;
        if (dist(x,y, cropRect.right, cropRect.top) < touchSlop) return 2;
        if (dist(x,y, cropRect.left, cropRect.bottom) < touchSlop) return 3;
        if (dist(x,y, cropRect.right, cropRect.bottom) < touchSlop) return 4;
        if (cropRect.contains(x, y)) return 5; 
        return 0;
    }
    
    private void updateCrop(float x, float y, float dx, float dy) {
        float w = getWidth();
        float h = getHeight();
        
        switch (activeEdge) {
            case 1: // TL
                cropRect.left = Math.max(0, Math.min(x, cropRect.right - minSize));
                cropRect.top = Math.max(0, Math.min(y, cropRect.bottom - minSize));
                break;
            case 2: // TR
                cropRect.right = Math.max(cropRect.left + minSize, Math.min(x, w));
                cropRect.top = Math.max(0, Math.min(y, cropRect.bottom - minSize));
                break;
            case 3: // BL
                cropRect.left = Math.max(0, Math.min(x, cropRect.right - minSize));
                cropRect.bottom = Math.max(cropRect.top + minSize, Math.min(y, h));
                break;
            case 4: // BR
                cropRect.right = Math.max(cropRect.left + minSize, Math.min(x, w));
                cropRect.bottom = Math.max(cropRect.top + minSize, Math.min(y, h));
                break;
            case 5: // Center
                if (cropRect.left + dx >= 0 && cropRect.right + dx <= w) {
                     cropRect.left += dx;
                     cropRect.right += dx;
                }
                if (cropRect.top + dy >= 0 && cropRect.bottom + dy <= h) {
                    cropRect.top += dy;
                    cropRect.bottom += dy;
                }
                break;
        }
    }
    
    private float dist(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x2-x1, y2-y1);
    }
    
    public float[] getNormalizedCrop() {
        return new float[] {
            cropRect.left / getWidth(),
            cropRect.top / getHeight(),
            cropRect.right / getWidth(),
            cropRect.bottom / getHeight()
        };
    }
}
