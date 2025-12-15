import 'package:flutter_test/flutter_test.dart';
import 'package:native_toast/native_toast.dart';
import 'package:native_toast/native_toast_method_channel.dart';
import 'package:native_toast/native_toast_platform_interface.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockNativeToastPlatform
    with MockPlatformInterfaceMixin
    implements NativeToastPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<void> showToast(String message) {
    return NativeToastPlatform.instance.showToast(message);
  }

  @override
  Future<void> openCameraActivity() {
    // TODO: implement openCameraActivity
    throw UnimplementedError();
  }

  @override
  Future<void> openNativeScreen() {
    // TODO: implement openNativeScreen
    throw UnimplementedError();
  }
}

void main() {
  final NativeToastPlatform initialPlatform = NativeToastPlatform.instance;

  test('$MethodChannelNativeToast is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelNativeToast>());
  });

  test('getPlatformVersion', () async {
    NativeToast nativeToastPlugin = NativeToast();
    MockNativeToastPlatform fakePlatform = MockNativeToastPlatform();
    NativeToastPlatform.instance = fakePlatform;

    expect(await nativeToastPlugin.getPlatformVersion(), '42');
  });
}
