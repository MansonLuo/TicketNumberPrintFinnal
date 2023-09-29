package com.example.ticketnumberprintfinnal.tools;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

public class PathTool {
    public static String getRealPathFromUri(Context context, Uri uri) {
        String filePath = "";
        String scheme = uri.getScheme();
        if (scheme == null)
            filePath = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            filePath = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    filePath = cursor.getString(columnIndex);
                }
                cursor.close();
            }
            if (TextUtils.isEmpty(filePath)) {
                filePath = getFilePathForNonMediaUri(context, uri);
            }
        }
        return filePath;
    }

    //非媒体文件中查找
    private static String getFilePathForNonMediaUri(Context context, Uri uri) {
        String filePath = "";
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow("_data");
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
        }
        return filePath;
    }
}