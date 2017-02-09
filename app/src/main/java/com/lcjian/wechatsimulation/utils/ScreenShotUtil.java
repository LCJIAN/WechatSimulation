package com.lcjian.wechatsimulation.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenShotUtil {

    private static final String TAG = "ScreenShotUtil";

    private static final String CLASS1_NAME = "android.view.SurfaceControl";

    private static final String CLASS2_NAME = "android.view.Surface";

    private static final String METHOD_NAME = "screenshot";

    private static ScreenShotUtil instance;

    private ScreenShotUtil() {

    }

    public static ScreenShotUtil getInstance() {
        synchronized (ScreenShotUtil.class) {
            if (instance == null) {
                instance = new ScreenShotUtil();
            }
        }
        return instance;
    }

    private Bitmap screenShot(int width, int height) {
        Log.i(TAG, "android.os.Build.VERSION.SDK : " + android.os.Build.VERSION.SDK_INT);
        Class<?> surfaceClass;
        Method method;
        try {
            Log.i(TAG, "width : " + width);
            Log.i(TAG, "height : " + height);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                surfaceClass = Class.forName(CLASS1_NAME);
            } else {
                surfaceClass = Class.forName(CLASS2_NAME);
            }
            method = surfaceClass.getDeclaredMethod(METHOD_NAME, int.class, int.class);
            method.setAccessible(true);
            return (Bitmap) method.invoke(null, width, height);
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException | ClassNotFoundException e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    /**
     * Takes a screenshot of the current display and shows an animation.
     */
    @SuppressLint("NewApi")
    public String takeScreenshot(Context context, String fileFullPath) {
        if (TextUtils.isEmpty(fileFullPath)) {
            DateFormat format = SimpleDateFormat.getDateInstance();
            String fileName = format.format(new Date(System.currentTimeMillis())) + ".png";
            fileFullPath = "/data/local/tmp/" + fileName;
        }

        if (ShellUtils.checkRootPermission()) {
            ShellUtils.execCommand("/system/bin/screencap -p " + fileFullPath, true);
        } else {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display mDisplay = wm.getDefaultDisplay();
            Matrix mDisplayMatrix = new Matrix();
            DisplayMetrics mDisplayMetrics = new DisplayMetrics();
            // We need to orient the screenshot correctly (and the Surface api seems to take screenshots
            // only in the natural orientation of the device :!)
            mDisplay.getRealMetrics(mDisplayMetrics);
            float[] dims = {mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels};
            float degrees = getDegreesForRotation(mDisplay.getRotation());
            boolean requiresRotation = (degrees > 0);
            if (requiresRotation) {
                // Get the dimensions of the device in its native orientation
                mDisplayMatrix.reset();
                mDisplayMatrix.preRotate(-degrees);
                mDisplayMatrix.mapPoints(dims);
                dims[0] = Math.abs(dims[0]);
                dims[1] = Math.abs(dims[1]);
            }

            Bitmap mScreenBitmap = screenShot((int) dims[0], (int) dims[1]);
            if (mScreenBitmap != null) {
                if (requiresRotation) {
                    // Rotate the screenshot to the current orientation
                    Bitmap ss = Bitmap.createBitmap(mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels,
                            Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(ss);
                    c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
                    c.rotate(degrees);
                    c.translate(-dims[0] / 2, -dims[1] / 2);
                    c.drawBitmap(mScreenBitmap, 0, 0, null);
                    c.setBitmap(null);
                    mScreenBitmap = ss;
                    if (!ss.isRecycled()) {
                        ss.recycle();
                    }
                }

                // Optimizations
                mScreenBitmap.setHasAlpha(false);
                mScreenBitmap.prepareToDraw();

                saveBitmap2file(mScreenBitmap, fileFullPath);
            }
        }

        return fileFullPath;
    }

    private void saveBitmap2file(Bitmap bmp, String fileName) {
        int quality = 100;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, quality, os);
        InputStream is = new ByteArrayInputStream(os.toByteArray());
        byte[] buffer = new byte[1024];
        int len;
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
            while ((len = is.read(buffer)) != -1) {
                stream.write(buffer, 0, len);
            }
            stream.flush();
        } catch (IOException e) {
            Log.i(TAG, e.toString());
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Log.i(TAG, e.toString());
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Log.i(TAG, e.toString());
                }
            }
        }
        if (!bmp.isRecycled()) {
            bmp.recycle();
        }
    }

    /**
     * @return the current display rotation in degrees
     */
    private float getDegreesForRotation(int value) {
        switch (value) {
            case Surface.ROTATION_90:
                return 360f - 90f;
            case Surface.ROTATION_180:
                return 360f - 180f;
            case Surface.ROTATION_270:
                return 360f - 270f;
        }
        return 0f;
    }
}
