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

            case "showToast":
                String message = call.argument("message");
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                result.success(null);
                break;

            case "openNativeScreen":
                Intent screenIntent = new Intent(context, ShowToastActivity.class);
                screenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(screenIntent);
                result.success(null);
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
        if (requestCode == RECORD_VIDEO_REQUEST_CODE) {

            if (pendingResult == null) return true;

            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri videoUri = data.getData();
                pendingResult.success(
                        videoUri != null ? videoUri.toString() : null
                );
            } else {
                pendingResult.error(
                        "RECORDING_FAILED",
                        "Video recording cancelled or failed",
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