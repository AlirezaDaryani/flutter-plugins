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

  static void startAndroid(
      {bool runForegroundService = true,
      String? notificationTitle,
      String? notificationDescription,
      int? detectionFrequency}) {
    _channelAndroid.invokeMethod('start_android', {
      "foreground": runForegroundService,
      "notification_title": notificationTitle,
      "notification_desc": notificationDescription,
      "detection_frequency": detectionFrequency
    });
  }

  static void stopAndroid() {
    _channelAndroid.invokeMethod('stop_android');
  }
}
