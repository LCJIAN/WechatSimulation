package com.lcjian.wechatsimulation.utils;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
}
