package com.example.native_toast.models;

import android.graphics.ColorMatrix;
import androidx.media3.common.Effect;

/**
 * Represents a video filter with its UI preview and Media3 effect.
 */
public class FilterItem {
    public String name;
    public ColorMatrix colorMatrix; // For UI preview
    public Effect effect; // For ExoPlayer/Transformer

    public FilterItem(String name, ColorMatrix matrix, Effect effect) {
        this.name = name;
        this.colorMatrix = matrix;
        this.effect = effect;
    }
}
