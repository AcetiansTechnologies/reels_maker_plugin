package com.example.native_toast.helpers;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.media3.common.Effect;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.BitmapOverlay;
import androidx.media3.effect.Crop;
import androidx.media3.effect.OverlayEffect;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.EditedMediaItemSequence;
import androidx.media3.transformer.Effects;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.Transformer;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for video export functionality.
 */
@OptIn(markerClass = UnstableApi.class)
public class VideoExporter {

    public interface ExportListener {
        void onExportComplete(String outputPath);
        void onExportError(String error);
    }

    private final Activity activity;
    private final ExportListener listener;
    private Transformer transformer;
    private ProgressDialog progressDialog;

    public VideoExporter(Activity activity, ExportListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public void export(ExportConfig config) {
        progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage("Saving Video...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        File inputFile = new File(config.inputPath);
        
        // NOTE: Don't cleanup storage before export - the input file would be deleted!

        // Build effects
        ImmutableList.Builder<Effect> effectsBuilder = new ImmutableList.Builder<>();
        
        // 1. Crop Effect
        if (config.normalizedCropRect != null) {
            float leftUi = config.normalizedCropRect[0];
            float topUi = config.normalizedCropRect[1];
            float rightUi = config.normalizedCropRect[2];
            float bottomUi = config.normalizedCropRect[3];
            
            float leftGl = leftUi * 2 - 1;
            float rightGl = rightUi * 2 - 1;
            float topGl = (1 - topUi) * 2 - 1;
            float bottomGl = (1 - bottomUi) * 2 - 1;
            
            effectsBuilder.add(new Crop(leftGl, rightGl, bottomGl, topGl));
        }
        
        // 2. Filter Effect
        if (config.filterEffect != null) {
            effectsBuilder.add(config.filterEffect);
        }

        // 3. Overlay Effect
        if (config.overlayContainer != null && config.overlayContainer.getChildCount() > 0) {
            Bitmap overlayBitmap = createBitmapFromView(config.overlayContainer);
            if (overlayBitmap != null) {
                try {
                    BitmapOverlay bitmapOverlay = BitmapOverlay.createStaticBitmapOverlay(overlayBitmap);
                    effectsBuilder.add(new OverlayEffect(ImmutableList.of(bitmapOverlay)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Build video item
        MediaItem videoItem = MediaItem.fromUri(config.inputPath);
        MediaItem.ClippingConfiguration clipping = new MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(config.startTrimMs)
            .setEndPositionMs(config.endTrimMs)
            .build();

        EditedMediaItem.Builder videoEditedItemBuilder = new EditedMediaItem.Builder(
            videoItem.buildUpon().setClippingConfiguration(clipping).build()
        ).setRemoveAudio(config.removeMainAudio);

        videoEditedItemBuilder.setEffects(new Effects(
            ImmutableList.of(),
            effectsBuilder.build()
        ));
        
        EditedMediaItem videoEditedItem = videoEditedItemBuilder.build();

        // Build sequences
        List<EditedMediaItemSequence> sequences = new ArrayList<>();
        sequences.add(new EditedMediaItemSequence.Builder(videoEditedItem).build());

        if (config.voiceOverPath != null && new File(config.voiceOverPath).exists()) {
            MediaItem voiceItem = MediaItem.fromUri(config.voiceOverPath);
            EditedMediaItem voiceEditedItem = new EditedMediaItem.Builder(voiceItem).build();
            sequences.add(new EditedMediaItemSequence.Builder(voiceEditedItem).build());
        }

        Composition composition = new Composition.Builder(sequences).build();

        transformer = new Transformer.Builder(activity)
            .addListener(new Transformer.Listener() {
                @Override
                public void onCompleted(Composition composition, ExportResult exportResult) {
                    activity.runOnUiThread(() -> {
                        if (progressDialog != null) progressDialog.dismiss();
                        
                        // Delete the input file (original video)
                        if (inputFile.exists()) inputFile.delete();
                        
                        // Delete voice over temp file if exists
                        if (config.voiceOverPath != null) new File(config.voiceOverPath).delete();
                        
                        // Cleanup all old files in the storage directory, keeping only the new output
                        File outputFile = new File(config.outputPath);
                        cleanupStorage(config.storageDir, outputFile);
                        
                        listener.onExportComplete(config.outputPath);
                    });
                }

                @Override
                public void onError(Composition composition, ExportResult exportResult, ExportException e) {
                    activity.runOnUiThread(() -> {
                        if (progressDialog != null) progressDialog.dismiss();
                        listener.onExportError(e.getMessage());
                    });
                }
            })
            .build();

        transformer.start(composition, config.outputPath);
    }

    private void cleanupStorage(File storageDir, File keepFile) {
        if (storageDir == null || !storageDir.exists()) return;
        if (keepFile == null) return;
        
        String keepPath = keepFile.getAbsolutePath();
        File[] files = storageDir.listFiles();
        if (files == null) return;
        
        for (File f : files) {
            // Keep the output file, delete everything else
            if (!f.getAbsolutePath().equals(keepPath)) {
                deleteRecursive(f);
            }
        }
    }

    private void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursive(child);
            }
        }
        f.delete();
    }

    private Bitmap createBitmapFromView(View view) {
        if (view.getWidth() <= 0 || view.getHeight() <= 0) return null;
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    public void cancel() {
        if (transformer != null) {
            transformer.cancel();
        }
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    /**
     * Configuration for video export.
     */
    public static class ExportConfig {
        public String inputPath;
        public String outputPath;
        public String voiceOverPath;
        public long startTrimMs;
        public long endTrimMs;
        public boolean removeMainAudio;
        public float[] normalizedCropRect;
        public Effect filterEffect;
        public FrameLayout overlayContainer;
        public File storageDir;

        public ExportConfig(String inputPath, String outputPath) {
            this.inputPath = inputPath;
            this.outputPath = outputPath;
        }
    }
}
