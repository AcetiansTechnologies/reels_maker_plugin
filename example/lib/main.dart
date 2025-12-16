import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:native_toast/native_toast.dart';
import 'package:native_toast_example/video_screen.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:path_provider/path_provider.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final NativeToast _nativeToastPlugin = NativeToast();

  int count = 0;

  late Future<File?> _videoFuture;

  @override
  void initState() {
    super.initState();
    requestStoragePermission();
    _videoFuture = listReelsVideos();

  }

  Future<bool> requestStoragePermission() async {
    if (await Permission.storage.isGranted) {
      return true;
    }
    final status = await Permission.storage.request();
    return status.isGranted;
  }

  Future<File?> listReelsVideos() async {
    final baseDir = await getExternalStorageDirectory();
    if (baseDir == null) return null;

    final moviesDir = Directory('${baseDir.path}/Movies');
    if (!await moviesDir.exists()) return null;

    final videofiles = await moviesDir
        .list()
        .where((e) => e is File)
        .toList();

    if (videofiles.isEmpty) return null;
   //i will pick the last video
    return File(videofiles.last.path);
  }

  Future<void> loadVideosData() async {
    setState(() {
      count++;
      _videoFuture = listReelsVideos();
    });
    await _videoFuture;
  }



  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: RefreshIndicator(
        onRefresh: loadVideosData,
        child: Scaffold(
          appBar: AppBar(
            title: Text('Refresh Count: $count'),
          ),
          body: Padding(
            padding: const EdgeInsets.all(12),
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              children: [

                const SizedBox(height: 16),
                FutureBuilder<File?>(
                  future: _videoFuture,
                  builder: (context, snapshot) {
                    if (snapshot.connectionState ==
                        ConnectionState.waiting) {
                      return SizedBox(
                        height:
                        MediaQuery.of(context).size.height * 0.5,
                        child: const Center(
                          child: CircularProgressIndicator(),
                        ),
                      );
                    }

                    if (snapshot.hasError) {
                      return Text('Error: ${snapshot.error}');
                    }

                    final file = snapshot.data;
                    if (file == null) {
                      return const Text('No video found');
                    }

                    return VideoPreview(file: file);
                  },
                ),
                const SizedBox(height: 16),
                Text("Pull down to refresh"),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    TextButton(
                      style: TextButton.styleFrom(
                        backgroundColor: Colors.blueAccent,
                      ),
                      onPressed: () async {
                        try {
                          await const MethodChannel(
                            'native_toast',
                          ).invokeMethod('recordVideo');
                        } on PlatformException catch (e) {
                          debugPrint(e.message);
                        }
                      },
                      child: const Text(
                        'Record New Reel',
                        style: TextStyle(color: Colors.white),
                      ),
                    ),
                    const SizedBox(width: 12),
                    TextButton(
                      onPressed: () {},
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
