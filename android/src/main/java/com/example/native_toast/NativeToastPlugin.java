package com.example.native_toast;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;

import java.io.File;       // for File class
import java.util.Arrays;   // for Arrays.sort


/**
 * NativeToastPlugin
 * Handles method calls from Flutter and launches native Android screens.
 */
public class NativeToastPlugin implements
        FlutterPlugin,
        MethodChannel.MethodCallHandler,
        ActivityAware,
        PluginRegistry.ActivityResultListener {

    private MethodChannel channel;
    private Context context;
    private Activity activity;

    // Used to send result back to Flutter after video recording
    public static MethodChannel.Result pendingResult;

    private static final int RECORD_VIDEO_REQUEST_CODE = 101;
    private static final int EDIT_VIDEO_REQUEST_CODE = 102;

    // Called when plugin is attached to Flutter engine
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        channel = new MethodChannel(binding.getBinaryMessenger(), "native_toast");
        channel.setMethodCallHandler(this);
        context = binding.getApplicationContext();
    }

    // Handles calls coming from Flutter
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {

        switch (call.method) {

            case "getPlatformVersion":
                result.success("Android " + android.os.Build.VERSION.RELEASE);
                break;

            case "recordVideo":
                if (activity == null) {
                    result.error("NO_ACTIVITY", "Activity is not attached", null);
                    return;
                }
                pendingResult = result;
                Intent cameraIntent = new Intent(activity, CameraActivity.class);
                activity.startActivityForResult(cameraIntent, RECORD_VIDEO_REQUEST_CODE);
                break;

            case "openCameraActivity":
                Toast.makeText(context, "clicked and this toast is from java", Toast.LENGTH_SHORT).show();
                Intent cameraActivityIntent = new Intent(context, CameraActivity.class);
                cameraActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(cameraActivityIntent);
                break;

            case "editVideo":
                if (activity == null) {
                    result.error("NO_ACTIVITY", "Activity is not attached", null);
                    return;
                }
                String videoPath = call.argument("videoPath");
                if (videoPath == null || videoPath.isEmpty()) {
                    result.error("INVALID_PATH", "Video path is required", null);
                    return;
                }
                pendingResult = result;
                Intent editIntent = new Intent(activity, EditVideoActivity.class);
                editIntent.putExtra("video_path", videoPath);
                activity.startActivityForResult(editIntent, EDIT_VIDEO_REQUEST_CODE);
                break;

            default:
                result.notImplemented();
        }
    }

    // Clean up channel
    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        channel = null;
    }

    // Receives result from CameraActivity
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECORD_VIDEO_REQUEST_CODE || requestCode == EDIT_VIDEO_REQUEST_CODE) {

            if (pendingResult == null) return true;

            if (resultCode == Activity.RESULT_OK && data != null) {
                // Check for resultPath (from EditVideoActivity)
                String resultPath = data.getStringExtra("resultPath");
                if (resultPath != null) {
                    java.util.Map<String, Object> resultMap = new java.util.HashMap<>();
                    resultMap.put("videoPath", resultPath);
                    pendingResult.success(resultMap);
                } else {
                    // Check for URI (from CameraActivity)
                    Uri videoUri = data.getData();
                    if (videoUri != null) {
                        java.util.Map<String, Object> resultMap = new java.util.HashMap<>();
                        resultMap.put("videoPath", videoUri.toString());
                        pendingResult.success(resultMap);
                    } else {
                        pendingResult.success(null);
                    }
                }
            } else {
                pendingResult.error(
                        "OPERATION_FAILED",
                        "Video operation cancelled or failed",
                        null
                );
            }

            pendingResult = null;
            return true;
        }
        return false;
    }

    // Activity is attached (important for startActivityForResult)
    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addActivityResultListener(this);
    }

    // Activity detached
    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }

    // Config change handling
    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity();
    }



}