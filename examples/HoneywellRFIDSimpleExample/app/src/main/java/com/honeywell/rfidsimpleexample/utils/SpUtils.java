package com.honeywell.rfidsimpleexample.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.honeywell.rfidsimpleexample.MyApplication;

public class SpUtils {
    public static void putBoolean(String key, boolean value) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        sp.edit().putBoolean(key, value).apply();
    }

    public static boolean getBoolean(String key, boolean defValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        return sp.getBoolean(key, defValue);
    }

    public static void putInt(String key, int value) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        sp.edit().putInt(key, value).apply();
    }

    public static int getInt(String key, int defValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        return sp.getInt(key, defValue);
    }

    public static void putLong(String key, long value) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        sp.edit().putLong(key, value).apply();
    }

    public static long getLong(String key, long defValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        return sp.getLong(key, defValue);
    }

    public static void putString(String key, String value) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        sp.edit().putString(key, value).apply();
    }

    public static String getString(String key, String defValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        return sp.getString(key, defValue);
    }
}
