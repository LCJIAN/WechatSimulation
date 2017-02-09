package com.lcjian.wechatsimulation.entity;

public class Setting {

    public Location location;

    public CellInfo cellInfo;

    public static class Location {
        /**
         * 经度
         */
        public double longitude;
        /**
         * 纬度
         */
        public double latitude;
    }

    /**
     * 基站信息
     */
    public static class CellInfo {

        public int mnc;
        public int lac;
        public int ci;
        public int acc;
    }
}
