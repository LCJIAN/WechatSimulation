package com.lcjian.wechatsimulation.utils;

import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import timber.log.Timber;

public class DownloadUtils {

    public static File download(String url, File destination) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        Request request = new Request.Builder().url(url).build();
        Response response = null;
        BufferedSink sink = null;
        try {
            if (!destination.exists() && !destination.mkdirs()) {
                return null;
            }
            response = new OkHttpClient().newCall(request).execute();
            if (response.isSuccessful()) {
                File file = new File(destination, url.substring(url.lastIndexOf("/") + 1));
                sink = Okio.buffer(Okio.sink(file));
                sink.writeAll(response.body().source());
                sink.flush();
                return file;
            } else {
                return null;
            }
        } catch (IOException e) {
            Timber.e(e);
            return null;
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
        Response response = null;
        try {
            response = new OkHttpClient().newCall(new Request.Builder().url(url).build()).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                return null;
            }
        } catch (IOException e) {
            Timber.e(e);
            return null;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    public static String post(String url, Map<String, String> params) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        Response response = null;
        try {
            FormBody.Builder builder = new FormBody.Builder();
            for (Map.Entry<String, String> param : params.entrySet()) {
                builder.add(param.getKey(), param.getValue());
            }
            response = new OkHttpClient().newCall(new Request.Builder().url(url).post(builder.build()).build()).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                return null;
            }
        } catch (IOException e) {
            Timber.e(e);
            return null;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    public static String post(String url, Map<String, String> headers, Map<String, String> params) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        Response response = null;
        try {
            Request.Builder requestBuilder = new Request.Builder().url(url);
            if (headers != null && !headers.isEmpty()) {
                requestBuilder.headers(Headers.of(headers));
            }
            if (params != null && !params.isEmpty()) {
                FormBody.Builder builder = new FormBody.Builder();
                for (Map.Entry<String, String> param : params.entrySet()) {
                    builder.add(param.getKey(), param.getValue());
                }
                requestBuilder.post(builder.build());
            }
            response = new OkHttpClient().newCall(requestBuilder.build()).execute();
            return response.body().string();
        } catch (IOException e) {
            Timber.e(e);
            return null;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    public static String get(String url, HashMap<String, String> headers, HashMap<String, String> params) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        Response response = null;
        try {
            HttpUrl.Builder builder = HttpUrl.parse(url).newBuilder();
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, String> param : params.entrySet()) {
                    builder.addQueryParameter(param.getKey(), param.getValue());
                }
            }
            Request.Builder requestBuilder = new Request.Builder().url(builder.build());
            if (headers != null && !headers.isEmpty()) {
                requestBuilder.headers(Headers.of(headers));
            }
            response = new OkHttpClient().newCall(requestBuilder.build()).execute();
            return response.body().string();
        } catch (IOException e) {
            Timber.e(e);
            return null;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }
}
