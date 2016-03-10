package com.rn.io.utils;


import android.content.Intent;
import android.net.Uri;
import android.support.annotation.IntDef;
import android.telecom.Call;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;

import com.facebook.react.ReactActivity;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.File;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by azou on 15/02/16.
 */

public class IOUtils extends ReactContextBaseJavaModule implements ActivityEventListener {


    final private Map<Integer, Callback> requests = new HashMap();
    private Promise prom;

    public IOUtils(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "IOUtils";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = MapBuilder.newHashMap();
        return constants;
    }

    @ReactMethod
    public void resolve(final String path) {
        try {
            getCurrentActivity()
                    .getContentResolver()
                    .openInputStream(Uri.parse(path));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ReactMethod
    public void tempFile(String prefix, String suffix, Callback cb) {
        try {
            cb.invoke(File.createTempFile(prefix, suffix).toURI());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ReactMethod
    public void file(Callback cb){
        Long time= System.currentTimeMillis();
        int id = time.intValue();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        getCurrentActivity().startActivityForResult(intent, id);
        requests.put(id , cb);
    }

    @ReactMethod
    public void image(Callback cb){
        Long time= System.currentTimeMillis();
        int id = time.intValue();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        getCurrentActivity().startActivityForResult(intent, id);
        requests.put(id, cb);
    }

    @ReactMethod
    public void video(Callback cb){
        Long time= System.currentTimeMillis();
        int id = time.intValue();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setType("video/*|image/*");
        intent.setType("video/*");
        getCurrentActivity().startActivityForResult(intent, id);
        requests.put(id, cb);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(data == null) {
            requests.remove(requestCode);
            return ;
        }

//        Log.d("RESULT", String.format("%d %d %s", requestCode, resultCode, data.toString()));

        WritableMap arguments = Arguments.createMap();
        arguments.putString("uri", data.getData().toString());
        arguments.putString("path", data.getData().getEncodedPath());

        requests.get(requestCode).invoke(arguments);
        requests.remove(requestCode);

    }

    @ReactMethod
    public void download(final ReadableMap args, final Callback start, final Promise result) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();

        try {
            params.put("name", getCurrentActivity()
                    .getContentResolver()
                    .openInputStream(Uri.parse(args.getString("uri"))));
        } catch (Exception e) {
            e.printStackTrace();
        }

        FileAsyncHttpResponseHandler responder = new FileAsyncHttpResponseHandler(getCurrentActivity()) {
            @Override
            public void onStart() {
                if(start != null)
                    start.invoke();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, File response) {
                if(result != null)
                    result.resolve(
                            response.toURI()
                    );
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                if(result != null)
                    result.reject(String.valueOf(statusCode), throwable);
            }
        };

        client.get(args.getString("uploadUrl"), params, responder);
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    @ReactMethod
    public void upload(final ReadableMap args, final Callback start, final Promise result) {
        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        ReadableMap file = args.getMap("file");

        try {
            Uri uri = Uri.parse(file.getString("uri"));
            params.put(file.getString("name"),
                    getCurrentActivity().getContentResolver().openInputStream(uri),
                    file.getString("filename"),
                    getMimeType(file.getString("filepath")));
        } catch (Exception e) {
            result.reject(e);
        }

        FileAsyncHttpResponseHandler responder = new FileAsyncHttpResponseHandler(getCurrentActivity()) {
            @Override
            public void onStart() {
                if(start != null)
                    start.invoke();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, File response) {
                if(result != null){

                    WritableMap arguments = Arguments.createMap();
                    WritableMap headerObj = Arguments.createMap();
                    for(Header H : headers){
                        headerObj.putString(H.getName(), H.getValue());
                    }
                    arguments.putInt("statusCode", statusCode);
                    arguments.putMap("headers", headerObj);
                    result.resolve(arguments);
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                if(result != null)
                    result.reject(String.valueOf(statusCode), throwable);
            }
        };

        client.post(args.getString("uploadUrl"), params, responder);
    }

}