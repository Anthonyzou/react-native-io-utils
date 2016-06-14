package com.rn.io.utils;


import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.webkit.MimeTypeMap;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
import com.koushikdutta.ion.builder.Builders;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Created by azou on 15/02/16.
 */

public class IOUtils extends ReactContextBaseJavaModule implements ActivityEventListener {


    private SparseArray<Promise> requests = new SparseArray<>();
    private SparseArray<File> captureRequests = new SparseArray<>();
    private ReactApplicationContext context;

    public IOUtils(ReactApplicationContext reactContext) {
        super(reactContext);
        context = reactContext;
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
    public void pickFile(Promise cb){
        Long time = System.currentTimeMillis();
        Integer id = Math.abs(time.intValue());

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        getCurrentActivity().startActivityForResult(intent, id);
        requests.put(id , cb);
    }

    @ReactMethod
    public void pickImage(Promise cb){
        Long time = System.currentTimeMillis();
        Integer id = Math.abs(time.intValue());

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        intent.setType("image/*");
        getCurrentActivity().startActivityForResult(intent, id);
        requests.put(id, cb);
    }

    @ReactMethod
    public void takePicture(Promise cb){
        Long time = System.currentTimeMillis();
        Integer id = Math.abs(time.intValue());

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        String formattedImageName = DateFormat.getDateTimeInstance().format(new Date()) + ".jpg";
        File image_file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), formattedImageName);
        Uri imageUri = Uri.fromFile(image_file);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        getCurrentActivity().startActivityForResult(intent, id);
        captureRequests.put(id, image_file);
        requests.put(id, cb);
    }

    @ReactMethod
    public void recordVideo(Promise cb){
        Long time = System.currentTimeMillis();
        Integer id = Math.abs(time.intValue());

        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        getCurrentActivity().startActivityForResult(intent, id);
        requests.put(id, cb);
    }

    @ReactMethod
    public void pickVideo(Promise cb){
        Long time = System.currentTimeMillis();
        Integer id = Math.abs(time.intValue());

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        getCurrentActivity().startActivityForResult(intent, id);
        requests.put(id, cb);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try{
            File f;
            if(captureRequests.get(requestCode) != null){
                f = captureRequests.get(requestCode);
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                context.sendBroadcast(mediaScanIntent);
            } else{
                f = new File(getPath(context,data.getData()));
            }

            WritableMap arguments = Arguments.createMap();
            arguments.putString("uri", f.toURI().toString());
            arguments.putString("path", f.getAbsolutePath());

            requests.get(requestCode).resolve(arguments);

        }
        catch(Exception e){
            requests.get(requestCode).reject("error", e.getMessage());
        }
        requests.remove(requestCode);
        captureRequests.remove(requestCode);
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


    @ReactMethod
    public void download(final ReadableMap args, final Promise result) {

        Builders.Any.B builder =  Ion.with(context).load(args.getString("url"));

        if(args.hasKey("headers")){
            ReadableMap headers = args.getMap("headers");
            ReadableMapKeySetIterator iterator = headers.keySetIterator();
            while (iterator.hasNextKey()) {
                String key = iterator.nextKey();
                builder = builder.addHeader(key, headers.getString(key));
            }
        }

        builder
            .write(new File(context.getCacheDir(), args.getString("name")))
            .withResponse()
            .setCallback(new FutureCallback<Response<File>>() {
                @Override
                public void onCompleted(Exception e, Response<File> response) {
                    WritableMap args = Arguments.createMap();
                    if(response.getResult()!=null){
                        args.putString("path", response.getResult().getAbsolutePath());
                        args.putString("uri", response.getResult().toURI().toString());
                    }
                    result.resolve(args);
                }
            })
        ;
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
    public void upload(final ReadableMap args, final Promise result) {
        ReadableMapKeySetIterator iterator;
        Builders.Any.B builder = Ion.with(context)
                .load(args.getString("uploadUrl"));

        if(args.hasKey("headers")) {
            ReadableMap headers = args.getMap("headers");
            iterator = headers.keySetIterator();
            while (iterator.hasNextKey()) {
                String key = iterator.nextKey();
                builder = builder.addHeader(key, headers.getString(key));
            }
        }

        if(args.hasKey("params")){
            ReadableMap reqParams = args.getMap("params");
            iterator = reqParams.keySetIterator();
            while (iterator.hasNextKey()) {
                String key = iterator.nextKey();
                builder.addQuery(key, reqParams.getString(key));
            }
        }

        ReadableArray files = args.getArray("files");
        for(int i = 0; i < files.size(); i++){
            ReadableMap file = files.getMap(i);
            String filepath = file.getString("filepath");
            File f = new File(filepath);
            builder.setMultipartFile(file.getString("name"), getMimeType(filepath), f);
        }

        builder
            .asString()
            .withResponse()
            .setCallback(new FutureCallback<Response<String>>() {
                @Override
                public void onCompleted(Exception e, Response<String> httpResult) {
                    if(e!=null){
                        result.reject(e);
                        return;
                    }

                    WritableMap arguments = Arguments.createMap();
                    arguments.putString("data", httpResult.getResult());
                    result.resolve(arguments);
                }
            })
        ;
    }
}
