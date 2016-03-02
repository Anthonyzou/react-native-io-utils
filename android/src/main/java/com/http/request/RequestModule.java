package com.http.request;


import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import com.facebook.react.ReactActivity;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.File;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * Created by azou on 15/02/16.
 */

public class RequestModule extends ReactContextBaseJavaModule {

    final AsyncHttpClient client = new AsyncHttpClient();

    public RequestModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "RequestAndroid";
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
    public void file(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        getCurrentActivity().startActivityForResult(intent, 1);
    }

    @ReactMethod
    public void image(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        getCurrentActivity().startActivityForResult(intent, 2);
    }

    @ReactMethod
    public void video(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*|image/*");
        intent.setType("video/*");
        getCurrentActivity().startActivityForResult(intent, 3);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ((ReactActivity) getCurrentActivity()).onActivityResult(requestCode, resultCode, data);

        if(data == null) return;

        Log.d("RESULT", String.format("%d %d %s", requestCode, resultCode, data.toString()));
        try {
            getCurrentActivity().getContentResolver().openInputStream(data.getData()).close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ReactMethod
    public void upload(final ReadableMap args, final Callback start, final Promise result) {
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
                    result.resolve(statusCode);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                if(result != null)
                    result.reject(String.valueOf(statusCode), throwable);
            }
        };

        client.post(args.getString("url"), params, responder);
    }
}