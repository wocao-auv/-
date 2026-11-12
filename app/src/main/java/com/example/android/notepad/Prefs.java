package com.example.android.notepad;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 偏好设置读取工具：主题、正文字号、默认排序。
 */
public final class Prefs {

    private static final String PREFS_NAME = "notepad_settings";

    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SEPIA = 2;
    public static final int THEME_SYSTEM = 3;

    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_SORT = "sort_order";

    public static final String SORT_MODIFIED = NotePad.Notes.MODIFIED_DATE + " DESC";
    public static final String SORT_CREATED = NotePad.Notes.CREATED_DATE + " DESC";
    public static final String SORT_TITLE = NotePad.Notes.TITLE + " COLLATE NOCASE ASC";

    public static final int DEFAULT_FONT_SIZE = 17;
    public static final int MIN_FONT_SIZE = 13;
    public static final int MAX_FONT_SIZE = 24;

    private Prefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static int getThemeMode(Context context) {
        return prefs(context).getInt(KEY_THEME, THEME_SYSTEM);
    }

    public static void setThemeMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_THEME, mode).apply();
    }

    /** 返回当前主题模式对应的 Activity 主题资源 */
    public static int getActivityTheme(Context context) {
        return getThemeMode(context) == THEME_SEPIA
                ? R.style.Theme_NotePad_Sepia : R.style.Theme_NotePad;
    }

    public static int getFontSize(Context context) {
        int size = prefs(context).getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
        return Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, size));
    }

    public static void setFontSize(Context context, int size) {
        prefs(context).edit().putInt(KEY_FONT_SIZE, size).apply();
    }

    /** 正文字号相对默认值的缩放比例，用于列表字号 */
    public static float getFontScale(Context context) {
        return getFontSize(context) / (float) DEFAULT_FONT_SIZE;
    }

    public static String getSortOrder(Context context) {
        String sort = prefs(context).getString(KEY_SORT, SORT_MODIFIED);
        if (SORT_CREATED.equals(sort) || SORT_TITLE.equals(sort) || SORT_MODIFIED.equals(sort)) {
            return sort;
        }
        return SORT_MODIFIED;
    }

    public static void setSortOrder(Context context, String sortOrder) {
        prefs(context).edit().putString(KEY_SORT, sortOrder).apply();
    }
}
