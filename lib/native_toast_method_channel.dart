import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'native_toast_platform_interface.dart';

/// An implementation of [NativeToastPlatform] that uses method channels.
class MethodChannelNativeToast extends NativeToastPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('native_toast');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>(
      'getPlatformVersion',
    );
    return version;
  }

  @override
  Future<void> showToast(String message) async {
    await methodChannel.invokeMethod("showToast", {"message": message});
  }

  @override
  Future<void> openNativeScreen() async {
    await methodChannel.invokeMethod("openNativeScreen");
  }
}
