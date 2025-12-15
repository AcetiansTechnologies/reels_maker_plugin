package com.example.native_toast;

import android.os.Bundle;
import android.os.Environment;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gallery);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView recyclerView = findViewById(R.id.imagesRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        List<File> imageFiles = loadImages();
        MediaAdapter adapter = new MediaAdapter(imageFiles);
        recyclerView.setAdapter(adapter);
    }

    private List<File> loadImages() {
        List<File> images = new ArrayList<>();

        File imageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (imageDir != null && imageDir.exists()) {
            File[] files = imageDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String name = f.getName().toLowerCase();
                    if (name.endsWith(".jpg")
                            || name.endsWith(".jpeg")
                            || name.endsWith(".png")) {
                        images.add(f);
                    }
                }
            }
        }
        return images;
    }
}
