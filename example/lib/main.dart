import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:native_toast/native_toast.dart';
import 'package:native_toast_example/video_screen.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  final _nativeToastPlugin = NativeToast();

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      platformVersion =
          await _nativeToastPlugin.getPlatformVersion() ??
          'Unknown platform version';
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Plugin example app')),
        body: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Center(child: Text('Running on: $_platformVersion\n')),
            TextButton(
              onPressed: () {
                // NativeToast().showToast("Hi this is Native Toast");
                NativeToast().openNativeScreen();
              },
              style: TextButton.styleFrom(backgroundColor: Colors.blueAccent),
              child: Text("Show toast", style: TextStyle(color: Colors.white)),
            ),

            TextButton(
              onPressed: () async {
                try {
                  final String? path = await const MethodChannel(
                    'native_toast',
                  ).invokeMethod('recordVideo');

                  if (path != null && context.mounted) {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => VideoScreen(videoPath: path),
                      ),
                    );
                  }
                } on PlatformException catch (e) {
                  debugPrint('Recording failed: ${e.message}');
                }
              },
              style: TextButton.styleFrom(backgroundColor: Colors.blueAccent),
              child: const Text(
                "Open Camera Activity",
                style: TextStyle(color: Colors.white),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
