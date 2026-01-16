package com.example.native_toast.models;

/**
 * Data class for text sticker content and styling.
 */
public class TextStickerData {
    public String text;
    public int bgMode; // 0=None, 1=White, 2=Black

    public TextStickerData(String text, int bgMode) {
        this.text = text;
        this.bgMode = bgMode;
    }
}
