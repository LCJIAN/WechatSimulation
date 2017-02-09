package com.lcjian.wechatsimulation.utils;

public class Utils {

    public static boolean getValue(Boolean aBoolean) {
        return aBoolean == null ? false : aBoolean;
    }

    public static int getValue(Integer integer) {
        return integer == null ? 0 : integer;
    }
}
