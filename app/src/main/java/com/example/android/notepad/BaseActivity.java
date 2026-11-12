package com.example.android.notepad;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * 所有界面的基类：统一处理主题（护眼模式）与系统栏、输入法内边距。
 */
public abstract class BaseActivity extends AppCompatActivity {

    private int appliedTheme;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        appliedTheme = Prefs.getActivityTheme(this);
        setTheme(appliedTheme);
        super.onCreate(savedInstanceState);
    }

    /** 让内容避开状态栏、导航栏与输入法，并设置状态栏图标明暗 */
    protected void setupSystemBars(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return insets;
        });

        boolean lightIcons = !isNightMode();
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(lightIcons);
        controller.setAppearanceLightNavigationBars(lightIcons);
    }

    private boolean isNightMode() {
        int mode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    /** 主题是否已在设置页被修改 */
    protected boolean hasThemeChanged() {
        return appliedTheme != Prefs.getActivityTheme(this);
    }
}
