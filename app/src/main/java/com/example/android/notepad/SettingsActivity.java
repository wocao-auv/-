package com.example.android.notepad;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * 偏好设置：主题外观、正文字号、默认排序方式。
 */
public class SettingsActivity extends BaseActivity {

    private RadioGroup themeGroup;
    private RadioGroup sortGroup;
    private SeekBar fontSeek;
    private TextView fontValue;
    private TextView fontPreview;

    private boolean binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        View root = findViewById(R.id.root);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        themeGroup = findViewById(R.id.theme_group);
        sortGroup = findViewById(R.id.sort_group);
        fontSeek = findViewById(R.id.font_seek);
        fontValue = findViewById(R.id.font_value);
        fontPreview = findViewById(R.id.font_preview);

        toolbar.setNavigationOnClickListener(v -> finish());
        setupSystemBars(root);

        binding = true;
        themeGroup.check(themeRadioId(Prefs.getThemeMode(this)));
        sortGroup.check(sortRadioId(Prefs.getSortOrder(this)));

        int fontSize = Prefs.getFontSize(this);
        fontSeek.setMax(Prefs.MAX_FONT_SIZE - Prefs.MIN_FONT_SIZE);
        fontSeek.setProgress(fontSize - Prefs.MIN_FONT_SIZE);
        fontValue.setText(getString(R.string.settings_font_value, fontSize));
        fontPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        binding = false;

        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (binding) {
                return;
            }
            int mode = themeMode(checkedId);
            Prefs.setThemeMode(this, mode);
            applyThemeMode(mode);
        });

        sortGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (binding) {
                return;
            }
            Prefs.setSortOrder(this, sortOrder(checkedId));
        });

        fontSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int size = Prefs.MIN_FONT_SIZE + progress;
                fontValue.setText(getString(R.string.settings_font_value, size));
                fontPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Prefs.setFontSize(SettingsActivity.this,
                        Prefs.MIN_FONT_SIZE + seekBar.getProgress());
            }
        });
    }

    private void applyThemeMode(int mode) {
        switch (mode) {
            case Prefs.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case Prefs.THEME_SEPIA:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                recreate();
                break;
            case Prefs.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                recreate();
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private int themeRadioId(int mode) {
        switch (mode) {
            case Prefs.THEME_LIGHT:
                return R.id.theme_light;
            case Prefs.THEME_DARK:
                return R.id.theme_dark;
            case Prefs.THEME_SEPIA:
                return R.id.theme_sepia;
            default:
                return R.id.theme_system;
        }
    }

    private int themeMode(int radioId) {
        if (radioId == R.id.theme_light) {
            return Prefs.THEME_LIGHT;
        } else if (radioId == R.id.theme_dark) {
            return Prefs.THEME_DARK;
        } else if (radioId == R.id.theme_sepia) {
            return Prefs.THEME_SEPIA;
        }
        return Prefs.THEME_SYSTEM;
    }

    private int sortRadioId(String sortOrder) {
        if (Prefs.SORT_CREATED.equals(sortOrder)) {
            return R.id.sort_by_created;
        } else if (Prefs.SORT_TITLE.equals(sortOrder)) {
            return R.id.sort_by_title;
        }
        return R.id.sort_by_modified;
    }

    private String sortOrder(int radioId) {
        if (radioId == R.id.sort_by_created) {
            return Prefs.SORT_CREATED;
        } else if (radioId == R.id.sort_by_title) {
            return Prefs.SORT_TITLE;
        }
        return Prefs.SORT_MODIFIED;
    }
}
