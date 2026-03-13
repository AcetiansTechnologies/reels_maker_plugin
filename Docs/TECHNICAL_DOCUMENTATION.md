# Reels Maker Plugin — Technical Documentation

> **Project:** Reels Maker Plugin (`native_toast`)  
> **Version:** 0.0.1  
> **Package:** `com.example.native_toast`  

---

## 1. Project Overview

**Reels Maker Plugin** is a Flutter plugin that provides native Android functionality for recording and editing short-form videos (reels/shorts). It bridges Flutter's Dart layer with native Android APIs via **Method Channels**, enabling high-performance video capture using **CameraX** and a full-featured video editor powered by **Media3 (ExoPlayer + Transformer)**. The plugin allows users to record videos in two modes (standard video and short-form), then edit them with trimming, cropping, color filters, text overlays, and voice-over narration — all processed natively on-device.

---

## 2. Technologies Used

| Category                | Technology                                      |
|-------------------------|-------------------------------------------------|
| **Programming Language** | **Java** (Android native), **Dart** (Flutter)  |
| **Framework**           | Flutter Plugin (Android platform)               |
| **Camera SDK**          | AndroidX CameraX (camera-core, camera2, camera-lifecycle, camera-view, camera-video) v1.3.2 |
| **Video Playback**      | AndroidX Media3 ExoPlayer v1.9.0                |
| **Video Processing**    | AndroidX Media3 Transformer v1.9.0, Media3 Effect v1.9.0 |
| **UI Toolkit**          | AndroidX AppCompat, RecyclerView, ConstraintLayout |
| **Build Tool**          | Gradle (Android Gradle Plugin 8.2.2)            |
| **Min SDK**             | Android API 24 (Android 7.0)                    |
| **Compile SDK**         | 34 (Android 14)                                 |
| **Java Version**        | Java 17                                         |
| **Architecture Pattern**| Plugin Architecture with Helper-based delegation |
| **Communication**       | Flutter MethodChannel (`native_toast`)           |
| **State Management**    | N/A (plugin, not a full app)                    |
| **Backend APIs**        | None (fully offline/on-device)                  |
| **Database**            | None                                            |

---

## 3. Key Features

1. **Video Recording** — Full video recording with CameraX, supporting front/back camera switching
2. **Short-Form Recording** — Dedicated "Short" mode with a 60-second timer, circular progress indicator, and auto-stop
3. **Video Editing Suite** — Complete post-recording editor with multiple tools
4. **Video Trimming** — Interactive trim handles with thumbnail timeline and real-time preview looping
5. **Video Cropping** — Custom interactive crop overlay with corner/edge-drag and pan gestures
6. **Color Filters** — 7 built-in filters (Original, B&W, Sepia, Invert, Warm, Cool, Vintage) with live preview
7. **Text Overlays** — Draggable, scalable text stickers with 3 background modes (None/White/Black)
8. **Voice-Over Recording** — Record audio narration over video with mic, preview playback, and mix/replace original audio
9. **Video Export** — Final export using Media3 Transformer combining all edits (trim, crop, filter, overlay, voice-over)
10. **Audio Controls** — Mute/unmute original video audio
11. **Flutter Integration** — Seamless method channel bridge returning edited video path back to Flutter

---

## 4. Modules in the Application

| Module                  | Description                                                      |
|-------------------------|------------------------------------------------------------------|
| **Plugin Bridge**       | Flutter ↔ Android communication via MethodChannel                |
| **Camera Module**       | Video recording with CameraX (Video + Short modes)               |
| **Video Editor**        | Central editing screen managing all editing tools                 |
| **Trim Module**         | Timeline-based video trimming with thumbnail generation           |
| **Filter Module**       | Color filter selection, preview, and application                  |
| **Text Overlay Module** | Text sticker creation, positioning, scaling, and styling          |
| **Voice-Over Module**   | Audio recording, playback preview, and mixing                     |
| **Crop Module**         | Interactive crop area selection with custom overlay view           |
| **Export Module**       | Final video composition and export using Media3 Transformer       |
| **Example App**        | Demo Flutter app showcasing plugin usage (record, edit, preview)  |

---

## 5. Classes and Components

### 5.1 Activities / Screens

| Activity               | File                     | Responsibility                                                    |
|------------------------|--------------------------|-------------------------------------------------------------------|
| `CameraActivity`       | `CameraActivity.java`   | Full-screen camera for video recording with Video/Short mode toggle, timer, progress ring, and front/back camera switching |
| `EditVideoActivity`    | `EditVideoActivity.java` | Video editing screen with ExoPlayer preview, managing trim/crop/filter/text/voice-over tools, seekbar, and export |

### 5.2 Custom Views

| View                   | File                     | Responsibility                                              |
|------------------------|--------------------------|-------------------------------------------------------------|
| `CropOverlayView`     | `CropOverlayView.java`  | Custom `View` that renders a draggable crop rectangle with corner handles, shadow overlay, and returns normalized crop coordinates |

### 5.3 Plugin Entry Point

| Class                   | File                          | Responsibility                                               |
|-------------------------|-------------------------------|--------------------------------------------------------------|
| `NativeToastPlugin`    | `NativeToastPlugin.java`      | Implements `FlutterPlugin`, `ActivityAware`, and `MethodCallHandler`. Routes method calls (`recordVideo`, `editVideo`, `openCameraActivity`, `getPlatformVersion`) to native screens and returns results to Flutter |

### 5.4 Adapters

| Adapter                    | File                          | Used With            | Responsibility                             |
|----------------------------|-------------------------------|----------------------|--------------------------------------------|
| `VideoThumbnailAdapter`   | `VideoThumbnailAdapter.java`  | `RecyclerView`       | Displays generated video frame thumbnails in the trim timeline |
| `FilterAdapter` (inner)    | `FilterHelper.java`           | `RecyclerView`       | Displays filter options with preview thumbnails and selection state |

### 5.5 Helper Classes

| Helper                 | File                      | Responsibility                                                 |
|------------------------|---------------------------|----------------------------------------------------------------|
| `FilterHelper`         | `FilterHelper.java`       | Manages 7 color filters, generates preview thumbnails, applies filters to TextureView paint, and stores current Media3 Effect for export |
| `TextOverlayHelper`    | `TextOverlayHelper.java`  | Creates draggable/scalable text stickers, manages text editor UI, background mode cycling, and gesture handling |
| `TrimHelper`           | `TrimHelper.java`         | Manages trim handle drag, thumbnail generation via `MediaMetadataRetriever`, playhead tracking, and trim loop playback |
| `VideoExporter`        | `VideoExporter.java`      | Builds Media3 `Composition` with all effects (crop, filter, overlay, voice-over, trim), runs `Transformer`, and handles cleanup |
| `VoiceOverHelper`      | `VoiceOverHelper.java`    | Records voice via `MediaRecorder`, provides playback preview via `MediaPlayer`, manages recording state and UI |

### 5.6 Model Classes

| Model               | File                   | Responsibility                                 |
|----------------------|------------------------|-------------------------------------------------|
| `FilterItem`         | `FilterItem.java`      | Data holder for filter name, `ColorMatrix` (UI preview), and `Effect` (export) |
| `TextStickerData`    | `TextStickerData.java` | Data holder for text sticker content and background mode (0=None, 1=White, 2=Black) |

### 5.7 Dart Classes (Plugin Layer)

| Class                         | File                                   | Responsibility                                       |
|-------------------------------|----------------------------------------|------------------------------------------------------|
| `NativeToast`                 | `native_toast.dart`                    | Public API class exposing `getPlatformVersion()`, `showToast()`, `openNativeScreen()`, `openCameraActivity()` |
| `NativeToastPlatform`         | `native_toast_platform_interface.dart` | Abstract platform interface defining the contract for platform-specific implementations |
| `MethodChannelNativeToast`    | `native_toast_method_channel.dart`     | Method channel implementation invoking native methods via `native_toast` channel |

### 5.8 Example App Classes (Dart)

| Class           | File                | Responsibility                                                  |
|-----------------|---------------------|-----------------------------------------------------------------|
| `MyApp`         | `main.dart`         | Main app widget — invokes `recordVideo` and `editVideo` methods via MethodChannel, displays latest video, handles refresh |
| `VideoPreview`  | `video_screen.dart` | Stateful widget that plays a video file using `video_player` package with tap-to-play/pause |

### 5.9 Listeners / Interfaces

| Interface                         | Defined In              | Purpose                                                    |
|-----------------------------------|-------------------------|------------------------------------------------------------|
| `FilterHelper.OnFilterApplied`    | `FilterHelper.java`     | Callback when a filter is applied                          |
| `FilterAdapter.OnItemSelected`    | `FilterHelper.java`     | Callback when a filter item is selected in RecyclerView    |
| `TrimHelper.TrimListener`        | `TrimHelper.java`       | Provides player reference, video duration, and trim change notifications |
| `TextOverlayHelper.TextOverlayListener` | `TextOverlayHelper.java` | Notifies when text editing starts and requests player pause |
| `VoiceOverHelper.VoiceOverListener`     | `VoiceOverHelper.java`   | Provides player position and recording state change notifications |
| `VideoExporter.ExportListener`    | `VideoExporter.java`    | Callbacks for export completion and error                  |
| `PluginRegistry.ActivityResultListener` | Android SDK       | Implemented by `NativeToastPlugin` to receive activity results |

---

## 6. Methods Created

### Class: `NativeToastPlugin`

| Method                             | Description                                                            |
|------------------------------------|------------------------------------------------------------------------|
| `onAttachedToEngine()`             | Registers the `native_toast` method channel with the Flutter engine    |
| `onMethodCall()`                   | Routes incoming Flutter method calls (`getPlatformVersion`, `recordVideo`, `openCameraActivity`, `editVideo`) to corresponding native logic |
| `onDetachedFromEngine()`           | Cleans up the method channel on detach                                 |
| `onActivityResult()`              | Receives results from `CameraActivity` and `EditVideoActivity`, returns video path to Flutter via `pendingResult` |
| `onAttachedToActivity()`          | Stores the activity reference and registers the activity result listener |
| `onDetachedFromActivity()`        | Clears the activity reference                                          |
| `onReattachedToActivityForConfigChanges()` | Re-attaches activity on config changes                        |
| `onDetachedFromActivityForConfigChanges()` | Detaches activity on config changes                           |

---

### Class: `CameraActivity`

| Method                        | Description                                                                   |
|-------------------------------|-------------------------------------------------------------------------------|
| `onCreate()`                  | Initializes camera preview, buttons, mode toggles, and permission checks      |
| `startCamera()`               | Sets up CameraX with `Preview` and `VideoCapture` use cases at HD quality     |
| `switchCamera()`              | Toggles between front and back camera, restarts the camera                    |
| `startVideoRecording()`       | Begins recording to an MP4 file with audio, starts timer, updates UI          |
| `stopVideoRecording()`        | Stops current recording, resets timer and button state                         |
| `startTimer()`                | Starts the 1-second interval timer handler                                    |
| `stopTimer()`                 | Removes timer callbacks                                                       |
| `updateModeUI()`              | Switches UI between Video mode (MM:SS timer) and Short mode (progress ring)   |
| `resetTimerUI()`              | Resets timer display text and progress bar to initial state                    |
| `updateShortProgress()`       | Calculates and updates the circular progress percentage for Short mode        |
| `formatTime()`                | Formats seconds into `MM:SS` string                                           |
| `setActive()`                 | Applies active/inactive button styling for mode selection                      |
| `hasPermissions()`            | Checks if CAMERA and RECORD_AUDIO permissions are granted                     |
| `requestPermissions()`        | Requests CAMERA and RECORD_AUDIO runtime permissions                          |
| `onRequestPermissionsResult()`| Handles permission grant/denial response                                      |
| `onBackPressed()`             | Stops recording if active before navigating back                              |
| `onDestroy()`                 | Stops timer on activity destruction                                           |
| `onActivityResult()`          | Receives result from `EditVideoActivity`, sends video path back to Flutter    |
| `getLatestVideoPath()`        | Scans Movies directory for the most recently modified `.mp4` file             |

---

### Class: `EditVideoActivity`

| Method                    | Description                                                                     |
|---------------------------|---------------------------------------------------------------------------------|
| `onCreate()`              | Initializes views, helpers, player, listeners, and seekbar update loop          |
| `initViews()`             | Binds all XML views to Java fields (player, buttons, trim handles, crop, voice, text) |
| `initHelpers()`           | Creates `FilterHelper`, `VoiceOverHelper`, `TextOverlayHelper`, `TrimHelper`, and `VideoExporter` with callbacks |
| `setupPlayer()`           | Configures ExoPlayer with MediaItem, sets up playback state and completion listeners |
| `setupListeners()`        | Wires up click listeners for play, back, save, mute, trim, voice, text, filters, crop, and seekbar |
| `enterTrimMode()`         | Switches UI to trim view with handles, thumbnails, and trim loop                |
| `enterVoiceMode()`        | Switches UI to voice-over recording mode                                        |
| `enterCropMode()`         | Switches UI to crop overlay mode                                                |
| `exitCropMode()`          | Saves normalized crop rect and exits to normal mode                             |
| `enterFiltersMode()`      | Switches UI to filter selection with horizontal RecyclerView                    |
| `exitToNormalMode()`       | Returns to default editing UI, hides all mode-specific controls                |
| `startExport()`           | Builds `ExportConfig` with all current edits and triggers `VideoExporter.export()` |
| `updatePlayPauseIcon()`   | Toggles play/pause icon based on player state                                   |
| `toggleMute()`            | Mutes/unmutes video audio and updates icon                                      |
| `startUpdateSeekBar()`    | Starts a 200ms polling loop to sync seekbar and time display with player position |
| `updateTimeDisplay()`     | Updates the time label with current position and total duration                  |
| `formatTime()`            | Formats milliseconds into `M:SS` string                                         |
| `handleBack()`            | Exits current mode or finishes activity with `RESULT_CANCELED`                  |
| `finishWithResult()`      | Sets result intent with the final exported video path and finishes              |
| `getOutputPath()`         | Generates a timestamped output file path in Movies directory                    |
| `getSafeInputPath()`      | Copies content URI video to a temp file for processing                          |

---

### Class: `CropOverlayView`

| Method                | Description                                                          |
|-----------------------|----------------------------------------------------------------------|
| `init()`              | Initializes border, corner, and shadow paints; sets default crop rect |
| `onLayout()`          | Centers crop rect at 80% of view dimensions on layout change          |
| `onDraw()`            | Draws shadow overlay, crop border, and corner handles                 |
| `onTouchEvent()`      | Handles touch for corner drag, edge drag, and center pan              |
| `getHitEdge()`        | Determines which corner/edge/center was touched (with 60px slop)      |
| `updateCrop()`        | Updates crop rect based on drag direction and active handle           |
| `dist()`              | Calculates Euclidean distance between two points                      |
| `getNormalizedCrop()` | Returns crop rect as normalized `[left, top, right, bottom]` array (0.0–1.0) |

---

### Class: `FilterHelper`

| Method                | Description                                                           |
|-----------------------|-----------------------------------------------------------------------|
| `getCurrentEffect()`  | Returns the currently selected Media3 `Effect` for export            |
| `setupFilters()`      | Initializes 7 filter definitions with `ColorMatrix` (UI) and `Effect` (export) |
| `setupRecycler()`     | Configures horizontal `RecyclerView` with `FilterAdapter`, generates preview thumbnail |
| `removeDecoration()`  | Removes item decoration from the RecyclerView                         |
| `applyFilter()`       | Applies selected filter to `TextureView` via `Paint.setColorFilter()` and stores effect |

---

### Class: `TrimHelper`

| Method                           | Description                                                         |
|----------------------------------|---------------------------------------------------------------------|
| `getStartTrimMs()` / `getEndTrimMs()` | Returns current trim start/end in milliseconds                |
| `setTrimRange()`                 | Sets trim range programmatically                                    |
| `initTrimThumbnailsAndHandles()` | Generates thumbnail strip and positions trim handles based on current range |
| `setupHandleDrag()`             | Configures touch drag for left/right trim handles with clamping     |
| `updatePlayheadPosition()`      | Updates the playhead indicator position based on current player time |
| `updateSelectedRangeUI()`       | Updates the highlighted selection area between trim handles          |
| `updateTrimTimes()`             | Calculates trim timestamps from handle positions and updates labels  |
| `generateThumbnails()`          | Extracts 8 evenly-spaced frame thumbnails using `MediaMetadataRetriever` |
| `startLoop()` / `stopLoop()`    | Manages trim preview loop that constrains playback within trim range |
| `formatTime()`                   | Formats milliseconds into `M:SS` string                             |

---

### Class: `TextOverlayHelper`

| Method                      | Description                                                         |
|-----------------------------|---------------------------------------------------------------------|
| `enterTextMode()`           | Opens text editor overlay with keyboard, loads existing sticker data |
| `toggleBackgroundMode()`    | Cycles sticker background through None → White → Black              |
| `updateEditorStyle()`       | Updates editor input appearance based on current background mode     |
| `handleDone()`              | Creates/updates text sticker from editor input, hides keyboard      |
| `updateSticker()`           | Updates an existing sticker's text and background                   |
| `addTextSticker()`          | Creates a new draggable/scalable `TextView` sticker centered on screen |
| `createRoundedBackground()` | Creates a `GradientDrawable` with rounded corners for sticker background |
| `setupStickerGestures()`    | Configures drag (single-finger) and pinch-to-scale gestures on sticker |
| `getOverlayContainer()`     | Returns the `FrameLayout` containing all text stickers for export capture |
| `setCurrentlyEditingView()` | Sets which sticker is currently being edited                        |

---

### Class: `VideoExporter`

| Method                   | Description                                                            |
|--------------------------|------------------------------------------------------------------------|
| `export()`               | Builds and starts Media3 `Transformer` with crop, filter, overlay, voice-over, and trim effects |
| `cleanupStorage()`       | Deletes all files in storage directory except the new export output    |
| `deleteRecursive()`      | Recursively deletes a file or directory                                |
| `createBitmapFromView()` | Renders a `View` hierarchy to a `Bitmap` for overlay compositing       |
| `cancel()`               | Cancels an in-progress export and dismisses progress dialog            |

---

### Class: `VoiceOverHelper`

| Method                  | Description                                                          |
|-------------------------|----------------------------------------------------------------------|
| `setupPlayButton()`     | Programmatically creates and adds a play button to the controls container |
| `getVoiceOverPath()`    | Returns the file path of the recorded voice-over audio               |
| `getVoiceStartMs()`     | Returns the video timestamp where voice recording started            |
| `isRecording()`         | Returns whether voice recording is currently active                   |
| `hasRecording()`        | Checks if a voice-over file exists on disk                           |
| `startRecording()`      | Initializes `MediaRecorder` with AAC format and begins recording     |
| `stopRecording()`       | Stops recording, releases recorder, updates UI to show preview options |
| `deleteRecording()`     | Deletes the voice-over file and resets UI                            |
| `playPreview()`         | Plays (or stops) the recorded voice-over using `MediaPlayer`         |
| `updateUIForMode()`     | Updates button visibility based on whether a recording exists        |
| `release()`             | Releases `MediaRecorder` and `MediaPlayer` resources                 |

---

### Class: `VideoThumbnailAdapter`

| Method                | Description                                         |
|-----------------------|-----------------------------------------------------|
| `onCreateViewHolder()` | Inflates `item_video_thumbnail` layout             |
| `onBindViewHolder()`  | Sets `Bitmap` thumbnail to `ImageView`              |
| `getItemCount()`      | Returns the number of thumbnails                    |

---

## 7. Third-Party Libraries Used

### Android Native (Gradle Dependencies)

| Library                              | Version  | Purpose                                           |
|--------------------------------------|----------|---------------------------------------------------|
| `androidx.appcompat:appcompat`       | 1.6.1    | Backward-compatible Activity and theme support     |
| `androidx.recyclerview:recyclerview` | 1.3.2    | RecyclerView for thumbnails and filter lists       |
| `androidx.constraintlayout:constraintlayout` | 2.1.4 | Flexible layout engine for complex UIs       |
| `androidx.camera:camera-core`        | 1.3.2    | CameraX core framework for camera access           |
| `androidx.camera:camera-camera2`     | 1.3.2    | CameraX Camera2 interop implementation             |
| `androidx.camera:camera-lifecycle`   | 1.3.2    | CameraX lifecycle-aware camera binding              |
| `androidx.camera:camera-view`        | 1.3.2    | CameraX `PreviewView` widget                       |
| `androidx.camera:camera-video`       | 1.3.2    | CameraX video recording API                        |
| `com.google.guava:guava`            | 31.1     | `ListenableFuture` and `ImmutableList` utilities   |
| `androidx.media3:media3-exoplayer`   | 1.9.0    | ExoPlayer for video playback                       |
| `androidx.media3:media3-transformer` | 1.9.0    | Video transformation and export pipeline            |
| `androidx.media3:media3-common`      | 1.9.0    | Shared Media3 data types (`MediaItem`, `Effect`)   |
| `androidx.media3:media3-ui`          | 1.9.0    | `PlayerView` widget for ExoPlayer                  |
| `androidx.media3:media3-effect`      | 1.9.0    | Video effects (crop, RGB filters, bitmap overlay)  |
| `junit:junit`                        | 4.13.2   | Unit testing framework                             |
| `org.mockito:mockito-core`           | 5.0.0    | Mocking framework for unit tests                   |

### Flutter/Dart (pubspec.yaml)

| Library                        | Version  | Purpose                                         |
|--------------------------------|----------|-------------------------------------------------|
| `plugin_platform_interface`    | ^2.0.2   | Base class for federated plugin platform interfaces |
| `flutter_lints`                | ^6.0.0   | Recommended Dart lint rules                      |

### Example App (pubspec.yaml)

| Library                | Version  | Purpose                                           |
|------------------------|----------|----------------------------------------------------|
| `video_player`         | ^2.8.1   | Flutter video playback widget for preview           |
| `package_info_plus`    | ^9.0.0   | App package metadata retrieval                      |
| `permission_handler`   | ^12.0.1  | Runtime permission request handling                 |
| `path_provider`        | ^2.1.5   | Access to device file system directories            |
| `cupertino_icons`      | ^1.0.8   | iOS-style icon set                                  |

---

## 8. API Integrations

> **This plugin operates entirely offline.** There are no backend API calls, REST endpoints, or network integrations. All video recording, editing, and export operations are performed locally on-device using native Android APIs.

### Method Channel API (Flutter ↔ Android)

The plugin communicates via a single `MethodChannel` named `native_toast`:

| Method Call         | Direction        | Request Type | Parameters                    | Response                                   | Feature                     |
|---------------------|------------------|--------------|-------------------------------|--------------------------------------------|-----------------------------|
| `getPlatformVersion`| Flutter → Native | Invoke       | None                          | `String` — Android version                 | Platform info               |
| `recordVideo`       | Flutter → Native | Invoke       | None                          | `Map { videoPath: String }` or error       | Opens camera, returns recorded video path |
| `editVideo`         | Flutter → Native | Invoke       | `{ videoPath: String }`       | `Map { videoPath: String }` or error       | Opens editor, returns exported video path |
| `openCameraActivity`| Flutter → Native | Invoke       | None                          | `void` (fire-and-forget)                   | Opens camera without result callback |

---

## 9. Database Usage

> **No database is used in this project.** All data is transient:
> - Recorded videos are saved as `.mp4` files in the app's external Movies directory
> - Voice-over audio is saved as `voice_temp.aac` in the app's external files directory
> - Exported videos are saved with `edited_<timestamp>.mp4` naming convention
> - After export, old files (including the original recording) are cleaned up to save storage

---

## 10. Deployment Details

| Aspect                | Details                                                     |
|-----------------------|-------------------------------------------------------------|
| **Distribution**      | Flutter plugin package (not a standalone app)                |
| **Plugin Version**    | `0.0.1`                                                     |
| **Min SDK**           | API 24 (Android 7.0 Nougat)                                 |
| **Compile SDK**       | 34 (Android 14)                                              |
| **Java Compatibility**| Java 17                                                     |
| **Gradle Plugin**     | Android Gradle Plugin 8.2.2                                  |
| **Package Name**      | `com.example.native_toast`                                   |
| **Plugin Class**      | `NativeToastPlugin`                                          |
| **Platforms**         | Android only (no iOS implementation)                         |
| **Build Type**        | Library (com.android.library)                                |
| **Integration**       | Consumed via `path` or `pub.dev` dependency in any Flutter app |

---

## 11. Challenges and Solutions

### 11.1 Complex Implementations

| Challenge                                    | Solution                                                                      |
|----------------------------------------------|-------------------------------------------------------------------------------|
| **Multi-effect video export pipeline**       | Uses Media3 `Composition` with `EditedMediaItemSequence` to compose crop, color filter, bitmap overlay, and voice-over into a single export pass |
| **Real-time filter preview**                | Applies `ColorMatrixColorFilter` via `Paint` on the `TextureView` layer for instant visual feedback, while storing the corresponding `RgbMatrix`/`RgbFilter` for export |
| **Interactive crop overlay**                | Custom `CropOverlayView` with corner-hit detection (60px touch slop), edge resizing, center panning, and normalized coordinate conversion for Media3 `Crop` effect |
| **Text sticker with gestures**              | Combined `ScaleGestureDetector` (pinch-to-zoom) with custom `OnTouchListener` (drag), including tap-to-edit detection via drag distance threshold |
| **Trim loop playback**                      | 30ms polling `Runnable` that constrains playback within trim bounds during preview, with handle-drag seeking |

### 11.2 Performance Optimizations

| Optimization                                 | Details                                                                      |
|----------------------------------------------|------------------------------------------------------------------------------|
| **Background thumbnail generation**          | Both `TrimHelper` and `FilterHelper` generate thumbnails on background threads to avoid UI blocking |
| **Scaled thumbnails**                        | Thumbnails are scaled to 150×150px to minimize memory usage                  |
| **Efficient seekbar polling**                | 200ms update interval for seekbar position (not frame-level) to reduce UI overhead |
| **Storage cleanup after export**             | Automatically deletes original recording, temp files, and old exports to prevent storage bloat |
| **MediaMetadataRetriever lifecycle**          | Properly releases `MediaMetadataRetriever` in finally blocks to prevent resource leaks |

### 11.3 Workarounds

| Issue                                        | Workaround                                                                   |
|----------------------------------------------|------------------------------------------------------------------------------|
| **Content URI vs file path**                | `getSafeInputPath()` copies content URI to a temp file when only a URI is available |
| **Filter matrix transposition**             | Color matrices are transposed between Android's row-major `ColorMatrix` (UI preview) and Media3's column-major `RgbMatrix` (GLSL export) |
| **Fallback video path**                     | `getLatestVideoPath()` scans for the most recent `.mp4` if the explicit path is not returned properly from the editor |
| **Crop coordinate system conversion**       | `VideoExporter` converts normalized UI coordinates (0–1) to OpenGL coordinates (-1 to +1) with Y-axis inversion for Media3 `Crop` effect |

---

## 12. Folder Structure

```
reels_maker_plugin/
├── lib/                                          # Dart plugin layer
│   ├── native_toast.dart                         # Public API class
│   ├── native_toast_method_channel.dart           # MethodChannel implementation
│   └── native_toast_platform_interface.dart       # Abstract platform interface
│
├── android/                                      # Native Android code
│   ├── build.gradle                              # Plugin Gradle config & dependencies
│   ├── settings.gradle                           # Gradle settings
│   └── src/main/
│       ├── AndroidManifest.xml                   # Permissions & activity declarations
│       ├── java/com/example/native_toast/
│       │   ├── NativeToastPlugin.java            # Plugin entry point
│       │   ├── CameraActivity.java               # Camera recording screen
│       │   ├── EditVideoActivity.java            # Video editing screen
│       │   ├── CropOverlayView.java              # Custom crop view
│       │   ├── VideoThumbnailAdapter.java        # Thumbnail RecyclerView adapter
│       │   ├── helpers/
│       │   │   ├── FilterHelper.java             # Filter logic & adapter
│       │   │   ├── TextOverlayHelper.java        # Text sticker logic
│       │   │   ├── TrimHelper.java               # Trim handle & timeline logic
│       │   │   ├── VideoExporter.java            # Export pipeline
│       │   │   └── VoiceOverHelper.java          # Voice recording logic
│       │   └── models/
│       │       ├── FilterItem.java               # Filter data model
│       │       └── TextStickerData.java          # Text sticker data model
│       └── res/
│           ├── layout/
│           │   ├── activity_camera.xml           # Camera screen layout
│           │   ├── activity_edit_video.xml        # Editor screen layout
│           │   └── item_video_thumbnail.xml       # Thumbnail item layout
│           ├── drawable/                          # 28 vector drawables (icons, backgrounds)
│           └── values/
│               └── styles.xml                    # Plugin theme definition
│
├── example/                                      # Example Flutter app
│   ├── lib/
│   │   ├── main.dart                             # Demo app with record/edit/preview
│   │   └── video_screen.dart                     # Video preview widget
│   ├── pubspec.yaml                              # Example app dependencies
│   └── android/                                  # Example app Android config
│
├── test/                                         # Test directory
├── pubspec.yaml                                  # Plugin package config
├── analysis_options.yaml                         # Dart analysis rules
├── CHANGELOG.md                                  # Version changelog
├── LICENSE                                       # License file
└── README.md                                     # Plugin readme
```

---

## 13. Class Index Table

| #  | Class Name                 | Type       | Package / Path                                  | Lines |
|----|---------------------------|------------|--------------------------------------------------|-------|
| 1  | `NativeToastPlugin`        | Plugin     | `com.example.native_toast`                       | 170   |
| 2  | `CameraActivity`           | Activity   | `com.example.native_toast`                       | 392   |
| 3  | `EditVideoActivity`        | Activity   | `com.example.native_toast`                       | 573   |
| 4  | `CropOverlayView`          | Custom View| `com.example.native_toast`                       | 187   |
| 5  | `VideoThumbnailAdapter`    | Adapter    | `com.example.native_toast`                       | 47    |
| 6  | `FilterHelper`             | Helper     | `com.example.native_toast.helpers`               | 362   |
| 7  | `FilterHelper.FilterAdapter` | Inner Adapter | `com.example.native_toast.helpers`          | ~100  |
| 8  | `TextOverlayHelper`        | Helper     | `com.example.native_toast.helpers`               | 239   |
| 9  | `TrimHelper`               | Helper     | `com.example.native_toast.helpers`               | 242   |
| 10 | `VideoExporter`            | Helper     | `com.example.native_toast.helpers`               | 228   |
| 11 | `VideoExporter.ExportConfig` | Data Class | `com.example.native_toast.helpers`             | ~17   |
| 12 | `VoiceOverHelper`          | Helper     | `com.example.native_toast.helpers`               | 219   |
| 13 | `FilterItem`               | Model      | `com.example.native_toast.models`                | 20    |
| 14 | `TextStickerData`          | Model      | `com.example.native_toast.models`                | 15    |
| 15 | `NativeToast`              | Dart Class | `lib/native_toast.dart`                          | 20    |
| 16 | `NativeToastPlatform`      | Dart Abstract | `lib/native_toast_platform_interface.dart`     | 35    |
| 17 | `MethodChannelNativeToast` | Dart Impl  | `lib/native_toast_method_channel.dart`            | 35    |
| 18 | `MyApp` / `_MyAppState`    | Dart Widget| `example/lib/main.dart`                          | 189   |
| 19 | `VideoPreview` / `_VideoPreviewState` | Dart Widget | `example/lib/video_screen.dart`         | 84    |

---

## 14. Resource Assets Summary

### Layout Files (3)
- `activity_camera.xml` — Camera recording screen (126 lines)
- `activity_edit_video.xml` — Video editor screen (546 lines)
- `item_video_thumbnail.xml` — Single thumbnail item for RecyclerView

### Drawable Resources (28)
| Category             | Resources                                                                    |
|----------------------|------------------------------------------------------------------------------|
| **Trim UI**          | `bg_selected_range`, `bg_trim_handle_left`, `bg_trim_handle_right`, `bg_trim_selection` |
| **Buttons**          | `btn_bg`, `btn_bg_green`, `btn_bg_sq`, `btn_bg_two`, `border_white`         |
| **Icons**            | `ic_arrow_go`, `ic_back`, `ic_background`, `ic_check`, `ic_crop`, `ic_mackeup`, `ic_mic`, `ic_music`, `ic_no_music`, `ic_pause`, `ic_play`, `ic_refresh`, `ic_scissors`, `ic_stop`, `ic_text`, `ic_video` |
| **Recording**        | `record_active`, `record_idle`, `circular_progress`                          |

### Styles (1)
- `PluginAppTheme` — Extends `Theme.AppCompat.Light.NoActionBar`

---

> **Document prepared for project handover and reporting.**  
> All classes, methods, and components have been documented from direct source code analysis.
