import 'native_toast_platform_interface.dart';

class NativeToast {
  Future<String?> getPlatformVersion() {
    return NativeToastPlatform.instance.getPlatformVersion();
  }

  Future<void> showToast(String message) {
    return NativeToastPlatform.instance.showToast(message);
  }

  Future<void> openNativeScreen() {
    return NativeToastPlatform.instance.openNativeScreen();
  }

  void openCameraActivity() {
    NativeToastPlatform.instance.openCameraActivity();
  }
}
