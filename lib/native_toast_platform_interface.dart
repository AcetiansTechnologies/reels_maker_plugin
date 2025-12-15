import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'native_toast_method_channel.dart';

abstract class NativeToastPlatform extends PlatformInterface {
  /// Constructs a NativeToastPlatform.
  NativeToastPlatform() : super(token: _token);

  static final Object _token = Object();

  static NativeToastPlatform _instance = MethodChannelNativeToast();

  /// The default instance of [NativeToastPlatform] to use.
  ///
  /// Defaults to [MethodChannelNativeToast].
  static NativeToastPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [NativeToastPlatform] when
  /// they register themselves.
  static set instance(NativeToastPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<void> showToast(String message);
  Future<void> openNativeScreen();

  Future<void> openCameraActivity();
}
