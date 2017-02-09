package com.lcjian.wechatsimulation.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Base64;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileUtils {

    private static String fileToBase64(File file) {
        if (file.isDirectory()) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        InputStream is = null;
        ByteArrayOutputStream os = null;
        try {
            is = new FileInputStream(file);
            os = new ByteArrayOutputStream();
            byte[] temp = new byte[1024];
            for (int len = is.read(temp); len != -1; len = is.read(temp)) {
                os.write(temp, 0, len);
                stringBuilder.append(Arrays.toString(Base64.encode(os.toByteArray(), Base64.NO_WRAP)));
                os.reset();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return stringBuilder.toString();
    }

    public static String firstFileToBase64(String directory) {
        File file = new File(directory);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            List<File> tempFiles = new ArrayList<>();
            for (File item : files) {
                if (!item.isDirectory()) {
                    tempFiles.add(item);
                }
            }
            if (tempFiles.isEmpty()) {
                return null;
            } else if (tempFiles.size() == 1) {
                return fileToBase64(tempFiles.get(0));
            } else {
                Collections.sort(tempFiles, new Comparator<File>() {
                    @Override
                    public int compare(File lhs, File rhs) {
                        return (int) (lhs.lastModified() - rhs.lastModified());
                    }
                });
                return fileToBase64(tempFiles.get(0));
            }
        } else {
            return null;
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static boolean deleteDir(String dir) {
        return deleteDir(new File(dir));
    }

    public static File generateQRCode(String text, File directory) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            int width = 512;
            int height = 512;
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (bitMatrix.get(x, y))
                        bmp.setPixel(x, y, Color.BLACK);
                    else
                        bmp.setPixel(x, y, Color.WHITE);
                }
            }
            if (!directory.exists() && !directory.mkdirs()) {
                return null;
            }
            File file = new File(directory, System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = null;
            BufferedOutputStream bos = null;
            try {
                fos = new FileOutputStream(file);
                bos = new BufferedOutputStream(fos);
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                bos.flush();
                return file;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (bos != null) {
                    try {
                        bos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }
}
