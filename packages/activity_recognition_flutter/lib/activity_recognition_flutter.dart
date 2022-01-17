library activity_recognition;

import 'dart:async';

import 'package:flutter/services.dart';

part 'activity_recognition_domain.dart';

/// Main entry to activity recognition API. Use as a singleton like
///
///   `ActivityRecognition()`
///
class ActivityRecognition {
  static const EventChannel _eventChannel =
      const EventChannel('activity_recognition_flutter');
  Stream<ActivityEvent>? _stream;

  static const MethodChannel _channelAndroid =
      const MethodChannel('activity_recognition_flutter_android');
  static ActivityRecognition _instance = ActivityRecognition._();

  ActivityRecognition._();

  /// Get the [ActivityRecognition] singleton.
  factory ActivityRecognition() => _instance;

  static void getDataAndroid(Future handler(MethodCall call)) {
    _channelAndroid.setMethodCallHandler(handler);
  }

  /// Requests continuous [ActivityEvent] updates.
  ///
  /// The Stream will output the *most probable* [ActivityEvent].
  /// By default the foreground service is enabled, which allows the
  /// updates to be streamed while the app runs in the background.
  /// The programmer can choose to not enable to foreground service.
  Stream<ActivityEvent> activityStream(
      {bool runForegroundService = true,
      String? notificationTitle,
      String? notificationDescription,
      int? detectionFrequency}) {
    if (_stream == null) {
      _stream = _eventChannel.receiveBroadcastStream({
        "foreground": runForegroundService,
        "notification_title": notificationTitle,
        "notification_desc": notificationDescription,
        "detection_frequency": detectionFrequency
      }).map(
              (json) => ActivityEvent.fromString(json));
    }
    return _stream!;
  }
}
