import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:video_player/video_player.dart';

class VideoScreen extends StatefulWidget {
  final String videoPath;

  const VideoScreen({super.key, required this.videoPath});

  @override
  State<VideoScreen> createState() => _VideoScreenState();
}

class _VideoScreenState extends State<VideoScreen> {
  static const platform = MethodChannel('native_toast');
  String? _videoPath;
  VideoPlayerController? _controller;
  @override
  void initState() {
    super.initState();

    _controller = VideoPlayerController.file(File(widget.videoPath))
      ..initialize().then((_) {
        setState(() {});
        _controller!.play();
      });
  }

  Future<void> _recordVideo() async {
    try {
      // Call the native method and wait for the path string
      final String? path = await platform.invokeMethod('recordVideo');
      if (path != null) {
        _initializePlayer(path);
      }
    } on PlatformException catch (e) {
      debugPrint("Failed to record video: '${e.message}'.");
    }
  }

  void _initializePlayer(String path) {
    // Dispose the old controller if it exists
    _controller?.dispose();

    setState(() {
      _videoPath = path;
      // Create a new controller
      _controller = VideoPlayerController.file(File(_videoPath!))
        ..initialize().then((_) {
          // Ensure the first frame is shown after the video is initialized
          setState(() {});
          _controller?.play();
        });
    });
  }

  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Native Video Recorder')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            if (_videoPath != null &&
                _controller != null &&
                _controller!.value.isInitialized)
              AspectRatio(
                aspectRatio: _controller!.value.aspectRatio,
                child: VideoPlayer(_controller!),
              )
            else
              const Text('No video selected.'),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: _recordVideo,
              child: const Text('Record Video with Native Camera'),
            ),
          ],
        ),
      ),
    );
  }
}
