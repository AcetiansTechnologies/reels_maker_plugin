package com.example.native_toast.helpers;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.Effect;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.RgbFilter;
import androidx.media3.effect.RgbMatrix;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.native_toast.models.FilterItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for video filter functionality.
 */
@OptIn(markerClass = UnstableApi.class)
public class FilterHelper {

    public interface OnFilterApplied {
        void onFilterApplied(Effect effect);
    }

    private final Activity activity;
    private final PlayerView playerView;
    private final RecyclerView recyclerView;
    private final String videoUri;
    private final String videoPath;
    private final OnFilterApplied listener;

    private List<FilterItem> filterItems;
    private FilterAdapter filterAdapter;
    private RecyclerView.ItemDecoration filterDecoration;
    private Effect currentEffect = null;

    public FilterHelper(Activity activity, PlayerView playerView, RecyclerView recyclerView,
                       String videoUri, String videoPath, OnFilterApplied listener) {
        this.activity = activity;
        this.playerView = playerView;
        this.recyclerView = recyclerView;
        this.videoUri = videoUri;
        this.videoPath = videoPath;
        this.listener = listener;
        
        setupFilters();
    }

    public Effect getCurrentEffect() {
        return currentEffect;
    }

    private void setupFilters() {
        filterItems = new ArrayList<>();
        
        // Original
        filterItems.add(new FilterItem("Original", new ColorMatrix(), null));
        
        // Black & White (Grayscale)
        ColorMatrix bwMatrix = new ColorMatrix();
        bwMatrix.setSaturation(0);
        RgbFilter bwEffect = RgbFilter.createGrayscaleFilter();
        filterItems.add(new FilterItem("B&W", bwMatrix, bwEffect));
         
        // Sepia
        ColorMatrix sepiaMatrix = new ColorMatrix();
        float[] sepiaValuesUI = new float[] {
            0.393f, 0.769f, 0.189f, 0, 0,
            0.349f, 0.686f, 0.168f, 0, 0,
            0.272f, 0.534f, 0.131f, 0, 0,
            0, 0, 0, 1, 0
        };
        sepiaMatrix.set(sepiaValuesUI);
        
        RgbMatrix sepiaEffect = new RgbMatrix() {
            @Override
            public float[] getMatrix(long presentationTimeUs, boolean useHdr) {
                return new float[] {
                    0.393f, 0.349f, 0.272f, 0, 
                    0.769f, 0.686f, 0.534f, 0, 
                    0.189f, 0.168f, 0.131f, 0, 
                    0,      0,      0,      1  
                };
            }
        };
        filterItems.add(new FilterItem("Sepia", sepiaMatrix, sepiaEffect));
        
        // Invert
        float[] invert = new float[] {
            -1,  0,  0,  0, 255,
             0, -1,  0,  0, 255,
             0,  0, -1,  0, 255,
             0,  0,  0,  1,   0
        };
        ColorMatrix invertMatrix = new ColorMatrix(invert);
        RgbFilter invertEffect = RgbFilter.createInvertedFilter();
        filterItems.add(new FilterItem("Invert", invertMatrix, invertEffect));
        
        // Warm - increase red/yellow tones
        float[] warm = new float[] {
            1.2f, 0,    0,    0,  20,
            0,    1.1f, 0,    0,  10,
            0,    0,    0.9f, 0, -10,
            0,    0,    0,    1,   0
        };
        ColorMatrix warmMatrix = new ColorMatrix(warm);
        RgbMatrix warmEffect = (presentationTimeUs, useHdr) -> new float[] {
            1.2f, 0,    0,    0,
            0,    1.1f, 0,    0,
            0,    0,    0.9f, 0,
            0,    0,    0,    1
        };
        filterItems.add(new FilterItem("Warm", warmMatrix, warmEffect));
        
        // Cool - increase blue tones
        float[] cool = new float[] {
            0.9f, 0,    0,    0, -10,
            0,    1.0f, 0,    0,   0,
            0,    0,    1.2f, 0,  20,
            0,    0,    0,    1,   0
        };
        ColorMatrix coolMatrix = new ColorMatrix(cool);
        RgbMatrix coolEffect = new RgbMatrix() {
            @NonNull
            @Override
            public float[] getMatrix(long presentationTimeUs, boolean useHdr) {
                return new float[] {
                    0.9f, 0,    0,    0,
                    0,    1.0f, 0,    0,
                    0,    0,    1.2f, 0,
                    0,    0,    0,    1
                };
            }
        };
        filterItems.add(new FilterItem("Cool", coolMatrix, coolEffect));
        
        // Vintage - faded colors with slight sepia
        float[] vintage = new float[] {
            0.6f, 0.3f, 0.1f, 0, 30,
            0.2f, 0.7f, 0.1f, 0, 20,
            0.2f, 0.2f, 0.6f, 0, 10,
            0,    0,    0,    1,  0
        };
        ColorMatrix vintageMatrix = new ColorMatrix(vintage);
        RgbMatrix vintageEffect = new RgbMatrix() {
            @NonNull
            @Override
            public float[] getMatrix(long presentationTimeUs, boolean useHdr) {
                return new float[] {
                    0.6f, 0.2f, 0.2f, 0,
                    0.3f, 0.7f, 0.2f, 0,
                    0.1f, 0.1f, 0.6f, 0,
                    0,    0,    0,    1
                };
            }
        };
        filterItems.add(new FilterItem("Vintage", vintageMatrix, vintageEffect));
    }

    public void setupRecycler() {
        // Set horizontal layout manager
        LinearLayoutManager lm = new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(lm);
        
        // Add spacing
        if (filterDecoration == null) {
            filterDecoration = new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                    outRect.left = 12;
                    outRect.right = 12;
                }
            };
        }
        recyclerView.removeItemDecoration(filterDecoration);
        recyclerView.addItemDecoration(filterDecoration);
        
        // Generate thumbnail
        new Thread(() -> {
            Bitmap thumb = null;
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                if (videoUri != null) retriever.setDataSource(activity, Uri.parse(videoUri));
                else retriever.setDataSource(videoPath);
                thumb = retriever.getFrameAtTime(0);
                if (thumb != null) {
                    thumb = Bitmap.createScaledBitmap(thumb, 150, 150, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { retriever.release(); } catch(IOException e) {}
            }
             
            Bitmap finalThumb = thumb;
            activity.runOnUiThread(() -> {
                filterAdapter = new FilterAdapter(filterItems, finalThumb, this::applyFilter);
                recyclerView.setAdapter(filterAdapter);
            });
        }).start();
    }

    public void removeDecoration() {
        if (filterDecoration != null && recyclerView != null) {
            recyclerView.removeItemDecoration(filterDecoration);
        }
    }

    private void applyFilter(FilterItem item) {
        currentEffect = item.effect;
        Log.d("FilterHelper", "Applying filter: " + item.name);
        
        // Apply to Preview (TextureView Layer Paint)
        View surfaceView = playerView.getVideoSurfaceView();
        if (surfaceView instanceof TextureView) {
            TextureView tv = (TextureView) surfaceView;
            if (item.name.equals("Original")) {
                tv.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            } else {
                Paint paint = new Paint();
                paint.setColorFilter(new ColorMatrixColorFilter(item.colorMatrix));
                tv.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
            }
        }
        
        if (listener != null) {
            listener.onFilterApplied(currentEffect);
        }
    }

    // ============= FILTER ADAPTER =============
    
    private class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.Holder> {
        private List<FilterItem> items;
        private Bitmap thumbnail;
        private OnItemSelected itemListener;
        private int selectedPos = 0;

        interface OnItemSelected {
            void onSelect(FilterItem item);
        }

        public FilterAdapter(List<FilterItem> items, Bitmap thumbnail, OnItemSelected listener) {
            this.items = items;
            this.thumbnail = thumbnail;
            this.itemListener = listener;
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout container = new LinearLayout(parent.getContext());
            container.setOrientation(LinearLayout.VERTICAL);
            container.setGravity(Gravity.CENTER);
            container.setLayoutParams(new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT, 
                RecyclerView.LayoutParams.MATCH_PARENT
            ));
            container.setPadding(8, 8, 8, 8);
            
            ImageView imageView = new ImageView(parent.getContext());
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(80, 80);
            imageView.setLayoutParams(imgParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setId(View.generateViewId());
            
            imageView.setClipToOutline(true);
            imageView.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 12);
                }
            });
            
            TextView nameView = new TextView(parent.getContext());
            nameView.setTextColor(Color.WHITE);
            nameView.setTextSize(11);
            nameView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams txtParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            txtParams.topMargin = 8;
            nameView.setLayoutParams(txtParams);
            
            container.addView(imageView);
            container.addView(nameView);
            
            return new Holder(container, imageView, nameView);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            FilterItem item = items.get(position);
            
            if (thumbnail != null) {
                holder.image.setImageBitmap(thumbnail);
            } else {
                holder.image.setImageDrawable(new android.graphics.drawable.ColorDrawable(0xFF444444));
            }
            
            ColorMatrixColorFilter cf = new ColorMatrixColorFilter(item.colorMatrix);
            holder.image.setColorFilter(cf);
            holder.name.setText(item.name);
            
            if (selectedPos == position) {
                holder.image.setBackgroundColor(0xFFFFFFFF);
                holder.image.setPadding(3, 3, 3, 3);
                holder.name.setTextColor(0xFFFFFFFF);
            } else {
                holder.image.setBackground(null);
                holder.image.setPadding(0, 0, 0, 0);
                holder.name.setTextColor(0xFFAAAAAA);
            }
            
            holder.itemView.setOnClickListener(v -> {
                int old = selectedPos;
                selectedPos = holder.getAdapterPosition(); 
                notifyItemChanged(old);
                notifyItemChanged(selectedPos);
                itemListener.onSelect(item);
            });
        }
        
        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            ImageView image;
            TextView name;
            public Holder(View v, ImageView img, TextView txt) {
                super(v);
                image = img;
                name = txt;
            }
        }
    }
}
