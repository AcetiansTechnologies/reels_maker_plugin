package com.example.native_toast;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;


public class ShowToastActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_toast);

        Button btn = findViewById(R.id.btnShowToast);
        btn.setOnClickListener(v ->
                Toast.makeText(this, "Toast from Native Screen", Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.imageViewBtn).setOnClickListener(v ->
                Toast.makeText(this, "Image Clicked", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.openCameraOptionsBtn).setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraOptionsScreen.class);
            startActivity(intent);
        });

    }
}
