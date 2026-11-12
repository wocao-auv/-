package com.example.android.notepad;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * 应用入口：启动时根据偏好设置决定是否使用深色模式。
 */
public class NotePadApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        switch (Prefs.getThemeMode(this)) {
            case Prefs.THEME_LIGHT:
            case Prefs.THEME_SEPIA:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case Prefs.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
