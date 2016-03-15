package com.rn.io.utils;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

/**
 * Created by azou on 15/02/16.
 */

public class IOUtils extends ReactContextBaseJavaModule implements ActivityEventListener {


    private Map<Integer, Promise> requests;
    private Map<Integer, File> captureRequests;
    public IOUtils(ReactApplicationContext reactContext) {
        super(reactContext);
        requests = new HashMap<>();
        captureRequests = new HashMap<>();
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
    public void pickFile(Promise cb){
        Long time = System.currentTimeMillis();
        int id = time.intValue();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        getCurrentActivity().startActivityForResult(intent, id);
        requests.put(id , cb);
    }

    @ReactMethod
    public void pickImage(Promise cb){
        Long time = System.currentTimeMillis();
        int id = time.intValue();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        intent.setType("image/*");
        getCurrentActivity().startActivityForResult(intent, id);
        requests.put(id, cb);
    }

    @ReactMethod
    public void takePicture(Promise cb){
        Long time = System.currentTimeMillis();
        int id = time.intValue();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        String formattedImageName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".jpg";
        File image_file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), formattedImageName);
        Uri imageUri = Uri.fromFile(image_file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        getCurrentActivity().startActivityForResult(intent, id);
        captureRequests.put(id, image_file);
        requests.put(id, cb);
    }

    @ReactMethod
    public void recordVideo(Promise cb){
        Long time = System.currentTimeMillis();
        int id = time.intValue();
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        getCurrentActivity().startActivityForResult(intent, id);
        requests.put(id, cb);
    }

    @ReactMethod
    public void pickVideo(Promise cb){
        Long time = System.currentTimeMillis();
        int id = time.intValue();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        getCurrentActivity().startActivityForResult(intent, id);
        requests.put(id, cb);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("result", String.format("%b %b %b", data == null ,
                requests.get(requestCode) == null ,
                captureRequests.get(requestCode) == null));

        // if this id corresponse to a camera requestcode
        //data is null when coming from camera or a cancel
        if(data == null && captureRequests.get(requestCode) == null) {
            requests.remove(requestCode);
            return ;
        }

        File f;
        if(captureRequests.get(requestCode) != null){
            f = captureRequests.get(requestCode);
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            getCurrentActivity().sendBroadcast(mediaScanIntent);
        }
        else if(data.getData().toString().contains("content://")){
            f = new File(getRealPathFromURI(getCurrentActivity(), data.getData()));
        } else{
            f = new File(data.getData().getPath());
        }

        WritableMap arguments = Arguments.createMap();
        arguments.putString("uri", f.toURI().toString());
        arguments.putString("path", f.getAbsolutePath());

        requests.get(requestCode).resolve(arguments);
        requests.remove(requestCode);
        captureRequests.remove(requestCode);
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

        FileAsyncHttpResponseHandler responder = new FileAsyncHttpResponseHandler(getCurrentActivity()) {
            @Override
            public void onStart() {
                if(start != null)
                    start.invoke();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, File response) {
                if(result != null){
                    WritableMap args = Arguments.createMap();
                    args.putString("path", response.getAbsolutePath());
                    args.putString("uri", response.toURI().toString());
                    result.resolve(args);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                if(result != null)
                    result.reject(String.valueOf(statusCode), throwable);
            }
        };

        client.get(args.getString("url"), params, responder);
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
