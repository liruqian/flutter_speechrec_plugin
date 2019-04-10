package cn.shuto.flutterspeechrecplugin;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.iflytek.cloud.*;
import com.iflytek.sunflower.FlowerCollector;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterSpeechrecPlugin
 */
public class FlutterSpeechrecPlugin implements MethodCallHandler, PluginRegistry.ActivityResultListener,
        PluginRegistry.RequestPermissionsResultListener {

    private static final int REQUEST_CODE_GRANT_PERMISSIONS = 2001;
    // 语音听写对象
    private SpeechRecognizer mIat;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    private Result result;
    private Context context;
    private String speechResult;

    FlutterSpeechrecPlugin(Context context) {
        this.context = context;
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_speechrec_plugin");
        channel.setMethodCallHandler(new FlutterSpeechrecPlugin(registrar.activity()));
        SpeechUtility.createUtility(registrar.context(), "appid=5c6a07ac");
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        this.result = result;
        if (call.method.equals("getPlatformVersion")) {

            String appVersionName = "";

            PackageManager manager = context.getPackageManager();
            try {
                PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
                appVersionName = info.versionName; // 版本名
            } catch (PackageManager.NameNotFoundException e) {
                // TODO Auto-generated catch blockd
                e.printStackTrace();
            }

            result.success("Android " + appVersionName);

        } else if (call.method.equals("startListen")) {
            startListen();
            Log.d("============", "onMethodCall:startListen ");
        } else if (call.method.equals("stopListen")) {
            stopListen();
            Log.d("============", "onMethodCall:stopListen ");
        } else {
            result.notImplemented();
        }
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this.context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this.context,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this.context,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO
                    },
                    REQUEST_CODE_GRANT_PERMISSIONS);
        }
    }

    private void startListen() {
        //requestPermissions();
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this.context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this.context,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED|| ContextCompat.checkSelfPermission(this.context,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this.context,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.CAMERA
                    },
                    REQUEST_CODE_GRANT_PERMISSIONS);
        } else {
            mIat = SpeechRecognizer.createRecognizer(context, mInitListener);
            if (mIat.isListening()) {
                mIat.stopListening();
            }
            // 移动数据分析，收集开始听写事件
            FlowerCollector.onEvent(context, "iat_recognize");
            mIatResults.clear();
            setParam();
            mIat.startListening(mRecognizerListener);
        }

    }

    /**
     * 参数设置
     *
     * @param
     * @return
     */
    public void setParam() {

//设置语法ID和 SUBJECT 为空，以免因之前有语法调用而设置了此参数；或直接清空所有参数，具体可参考 DEMO 的示例。
        mIat.setParameter(SpeechConstant.CLOUD_GRAMMAR, null);
        mIat.setParameter(SpeechConstant.SUBJECT, null);
//设置返回结果格式，目前支持json,xml以及plain 三种格式，其中plain为纯听写文本内容
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
//此处engineType为“cloud”
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, "cloud");
//设置语音输入语言，zh_cn为简体中文
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
//设置结果返回语言
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
// 设置语音前端点:静音超时时间，单位ms，即用户多长时间不说话则当做超时处理
//取值范围{1000～10000}
        mIat.setParameter(SpeechConstant.VAD_BOS, "4000");
//设置语音后端点:后端点静音检测时间，单位ms，即用户停止说话多长时间内即认为不再输入，
//自动停止录音，范围{0~10000}
        mIat.setParameter(SpeechConstant.VAD_EOS, "1000");
//设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "0");
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            if (code != ErrorCode.SUCCESS) {

            }
        }
    };

    /**
     * 听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
//            showTip("开始说话");
        }

        @Override
        public void onError(SpeechError error) {
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
//            showTip("结束说话");

        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {


            String text = JsonParser.parseIatResult(results.getResultString());
            String sn = null;
            // 读取json结果中的sn字段
            try {
                JSONObject resultJson = new JSONObject(results.getResultString());
                sn = resultJson.optString("sn");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mIatResults.put(sn, text);
            StringBuffer resultBuffer = new StringBuffer();
            for (String key : mIatResults.keySet()) {
                resultBuffer.append(mIatResults.get(key));
            }
//        mResultText.setText(resultBuffer.toString());
//        mResultText.setSelection(mResultText.length());
//        Intent resultIntent = new Intent();
//        Bundle bundle = new Bundle();
//        bundle.putString("result", resultBuffer.toString());
//        resultIntent.putExtras(bundle);
//        this.setResult(RESULT_OK, resultIntent);
//        finish();
            if (isLast) {
                Log.d("=========输出结果：", resultBuffer.toString());
                speechResult = resultBuffer.toString();
                result.success(speechResult);
            }
        }


        @Override
        public void onVolumeChanged(int volume, byte[] data) {
//            showTip("当前正在说话，音量大小：" + volume);
//            showTip("当前正在说话...");
//            Log.d(TAG, "返回音频数据：" + data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
//            	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
//            		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
//            		Log.d(TAG, "session id =" + sid);
//            	}
        }
    };

    private void stopListen() {
        if (mIat != null && mIat.isListening()) {
            Log.d("stopListen", "stopListen:stopListen ");
            mIat.stopListening();
        }
    }

    @Override
    public boolean onActivityResult(int i, int i1, Intent intent) {
        return false;
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        return false;
    }
}
