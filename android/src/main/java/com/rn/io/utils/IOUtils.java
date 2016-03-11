package com.rn.io.utils;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.IntDef;
import android.telecom.Call;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;

import com.facebook.common.file.FileUtils;
import com.facebook.react.ReactActivity;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
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


    private Map<Integer, Callback> requests;

    public IOUtils(ReactApplicationContext reactContext) {
        super(reactContext);
        requests = new HashMap<>();
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
    public void tempFile(String prefix, String suffix, Promise cb) {
        try {
            File f = File.createTempFile(prefix, suffix);
            WritableMap params = Arguments.createMap();
            params.putString("path", f.getAbsolutePath());
            params.putString("uri", f.toURI().toString());
            cb.resolve(params);
        } catch (IOException e) {
            cb.reject(e);
        }
    }

    @ReactMethod
    public void customSelect(String type, Callback cb){
        Long time = System.currentTimeMillis();
        int id = time.intValue();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(type);
        getCurrentActivity().startActivityForResult(intent, id);
        requests.put(id , cb);
    }

    @ReactMethod
    public void file(Callback cb){
        Long time = System.currentTimeMillis();
        int id = time.intValue();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        getCurrentActivity().startActivityForResult(intent, id);
        requests.put(id , cb);
    }

    @ReactMethod
    public void image(Callback cb){
        Long time = System.currentTimeMillis();
        int id = time.intValue();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        intent.setType("image/*");
        getCurrentActivity().startActivityForResult(intent, id);
        requests.put(id, cb);
    }

    @ReactMethod
    public void camera(Callback cb){
        Long time = System.currentTimeMillis();
        int id = time.intValue();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        intent.setType("image/*");
        getCurrentActivity().startActivityForResult(intent, id);
        requests.put(id, cb);
    }

    @ReactMethod
    public void recordVideo(Callback cb){
        Long time = System.currentTimeMillis();
        int id = time.intValue();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        intent.setType("image/*");
        getCurrentActivity().startActivityForResult(intent, id);
        requests.put(id, cb);
    }

    @ReactMethod
    public void video(Callback cb){
        Long time = System.currentTimeMillis();
        int id = time.intValue();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        getCurrentActivity().startActivityForResult(intent, id);
        requests.put(id, cb);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requests.get(requestCode) == null) { return; }
        if(data == null ) {
            requests.remove(requestCode);
            return ;
        }

        File f;
        if(data.getData().toString().contains("content://")){
            f = new File(getRealPathFromURI(getCurrentActivity(), data.getData()));
        }
        else{
            f = new File(data.getData().getPath());
        }

        WritableMap arguments = Arguments.createMap();
        arguments.putString("uri", f.toURI().toString());
        arguments.putString("path", f.getAbsolutePath());

        requests.get(requestCode).invoke(arguments);
        requests.remove(requestCode);

    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @ReactMethod
    public void download(final ReadableMap args, final Callback start, final Promise result) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();

        try {
            params.put("name",
                    getCurrentActivity()
                            .getContentResolver()
                            .openInputStream(Uri.parse(args.getString("uri")))
            );
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
        ReadableArray files = args.getArray("files");
        ReadableMap headers = args.getMap("headers");

        ReadableMapKeySetIterator iterator = headers.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            client.addHeader(key, headers.getString(key));
        }
        for(int i = 0; i < files.size(); i++){
            ReadableMap file = files.getMap(i);
            try {
                String filepath = file.getString("filepath");
                File f = new File(filepath);
                params.put(
                        file.getString("name"),
                        f,
                        getMimeType(filepath),
                        file.getString("filename")
                );

            } catch (Exception e) {
                result.reject(e);
                return;
            }
        }


        FileAsyncHttpResponseHandler responder = new FileAsyncHttpResponseHandler(getCurrentActivity()) {
            @Override
            public void onStart() {
                if(start != null){
                    Log.d("start", "START");
                    start.invoke();
                }

            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, File response) {
                if(result != null){

                    WritableMap arguments = Arguments.createMap();
                    WritableMap headerObj = Arguments.createMap();
                    for(Header H : headers){
                        headerObj.putString(H.getName(), H.getValue());
                    }

                    arguments.putInt("status", statusCode);
                    arguments.putMap("headers", headerObj);
                    try {
                        arguments.putString("data", org.apache.commons.io.FileUtils.readFileToString(response, "UTF-8"));
                    } catch (IOException e) {
                        result.reject(e);
                        return;
                    }
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