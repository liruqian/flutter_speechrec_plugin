import 'dart:async';

import 'package:flutter/services.dart';

class FlutterSpeechrecPlugin {
  static const MethodChannel _channel =
      const MethodChannel('flutter_speechrec_plugin');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    print('=======version:$version');
    return version;
  }

  static Future<void> get startListen async {
    await _channel.invokeMethod('startListen');
  }

  static Future<String> get stopListen async {
    String resutlt = await _channel.invokeMethod('stopListen');
    return resutlt;
    print('=======stopListen:$resutlt');
  }
}
