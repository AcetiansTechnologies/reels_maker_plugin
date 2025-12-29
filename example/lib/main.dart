import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:native_toast_example/video_screen.dart';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  late Future<File?> _videoFuture;
  int count = 0;

  @override
  void initState() {
    super.initState();
    requestStoragePermission();
    _videoFuture = getLatestVideo();
  }

  Future<bool> requestStoragePermission() async {
    if (await Permission.storage.isGranted) return true;
    final status = await Permission.storage.request();
    return status.isGranted;
  }

  // Get latest video from Movies folder
  Future<File?> getLatestVideo() async {
    final baseDir = await getExternalStorageDirectory();
    if (baseDir == null) return null;

    final moviesDir = Directory('${baseDir.path}/Movies');
    if (!await moviesDir.exists()) return null;

    final videoFiles = await moviesDir
        .list()
        .where((e) => e is File && e.path.endsWith('.mp4'))
        .cast<File>()
        .toList();

    if (videoFiles.isEmpty) return null;

    // Sort by last modified date descending
    videoFiles.sort((a, b) => b.lastModifiedSync().compareTo(a.lastModifiedSync()));
    return videoFiles.first;
  }

  // Update UI with latest video
  Future<void> refreshLatestVideo() async {
    setState(() {
      count++;
      _videoFuture = getLatestVideo();
    });
    await _videoFuture;
  }

  // Update UI with a specific video path (after recording)
  Future<void> setVideoFile(String path) async {
    setState(() {
      _videoFuture = Future.value(File(path));
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: RefreshIndicator(
        onRefresh: refreshLatestVideo,
        child: Scaffold(
          appBar: AppBar(title: Text('Latest Video (Refresh count: $count)')),
          body: Padding(
            padding: const EdgeInsets.all(12),
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              children: [
                const SizedBox(height: 16),
                FutureBuilder<File?>(
                  future: _videoFuture,
                  builder: (context, snapshot) {
                    if (snapshot.connectionState == ConnectionState.waiting) {
                      return SizedBox(
                        height: MediaQuery.of(context).size.height * 0.5,
                        child: const Center(child: CircularProgressIndicator()),
                      );
                    }
                    if (snapshot.hasError) {
                      return Text('Error: ${snapshot.error}');
                    }
                    final file = snapshot.data;
                    if (file == null) return const Text('No video found');
                    return VideoPreview(file: file);
                  },
                ),
                const SizedBox(height: 16),
                const Text("Pull down to refresh"),
                const SizedBox(height: 16),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    TextButton(
                      style: TextButton.styleFrom(
                        backgroundColor: Colors.blueAccent,
                      ),
                      onPressed: () async {
                        const channel = MethodChannel('native_toast');

                        try {
                          debugPrint("Starting video recording");

                          final result = await channel.invokeMethod('recordVideo');

                          debugPrint("Result received from native");
                          if (result != null && result['videoPath'] != null) {
                            await setVideoFile(result['videoPath']);
                            debugPrint("Video path: ${result['videoPath']}");
                          } else {
                            debugPrint("No video returned");
                          }
                        } on PlatformException catch (e) {
                          debugPrint("Native error: ${e.code} - ${e.message}");
                        }
                      },
                      child: const Text(
                        'Record New Reel',
                        style: TextStyle(color: Colors.white),
                      ),
                    ),
                    const SizedBox(width: 12),
                    TextButton(
                      onPressed: () {
                        // Add post video logic
                      },
                      child: const Text('Post Video'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
