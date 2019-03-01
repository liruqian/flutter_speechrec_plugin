import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_speechrec_plugin/flutter_speechrec_plugin.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _speechResult = '';

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: new Column(
          children: <Widget>[
            new Center(
              child: new Text(_speechResult),
            ),
            new Center(
              child: new RaisedButton(
                onPressed: () => FlutterSpeechrecPlugin.platformVersion,
                child: new Text('获取版本号'),
              ),
            ),
            new Center(
              child: new RaisedButton(
                onPressed: () => FlutterSpeechrecPlugin.startListen,
                child: new Text('开始录音'),
              ),
            ),
            new Center(
              child: new RaisedButton(
                onPressed: () {
                  stopListen();
                },
                child: new Text('结束录音'),
              ),
            )
          ],
        ),
      ),
    );
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> startListen() async {
    // Platform messages may fail, so we use a try/catch PlatformException.
      FlutterSpeechrecPlugin.startListen;
    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
   
  }

  Future<void> stopListen() async {
    String speechResult;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
       speechResult = await FlutterSpeechrecPlugin.stopListen;
    } on PlatformException {
      speechResult = 'Failed to get';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _speechResult = _speechResult + speechResult +",";
    });
  }
}
