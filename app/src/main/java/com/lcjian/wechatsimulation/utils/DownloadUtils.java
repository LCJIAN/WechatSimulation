package com.lcjian.wechatsimulation.utils;

import android.text.TextUtils;

import java.io.File;
import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import timber.log.Timber;

public class DownloadUtils {

    public static boolean download(String url, File destination) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        Request request = new Request.Builder().url(url).build();
        Response response = null;
        BufferedSink sink = null;
        try {
            response = new OkHttpClient().newCall(request).execute();
            if (response.isSuccessful()) {
                File file = new File(destination, url.substring(url.lastIndexOf("/") + 1));
                sink = Okio.buffer(Okio.sink(file));
                sink.writeAll(response.body().source());
                sink.flush();
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            Timber.e(e);
            return false;
        } finally {
            try {
                if (sink != null) {
                    sink.close();
                }
            } catch (IOException e) {
                Timber.e(e);
            }
            if (response != null) {
                response.close();
            }
        }
    }

    public static String getContent(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        try {
            Response response = new OkHttpClient().newCall(new Request.Builder().url(url).build()).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                return null;
            }
        } catch (IOException e) {
            Timber.e(e);
            return null;
        }
    }
}
