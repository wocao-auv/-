package com.example.android.notepad;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 笔记编辑界面：新建 / 修改 / 删除笔记，支持分类与置顶。
 */
public class NoteEditorActivity extends BaseActivity {

    private static final String MIME_TEXT = "text/plain";
    private static final String[] PRESET_CATEGORIES = {
            "工作", "学习", "生活", "灵感", Note.DEFAULT_CATEGORY
    };

    private MaterialToolbar toolbar;
    private EditText titleEdit;
    private EditText contentEdit;
    private TextView metaText;
    private MaterialButton categoryButton;
    private MaterialButton pinButton;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private long noteId = -1;
    private long createdTime;
    private long modifiedTime;
    private String category = Note.DEFAULT_CATEGORY;
    private boolean pinned;
    private boolean isNew = true;
    private boolean loading;
    private boolean deleting;

    private String savedTitle = "";
    private String savedContent = "";
    private String savedCategory = Note.DEFAULT_CATEGORY;
    private boolean savedPinned;

    private Note pendingExportNote;

    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument(MIME_TEXT),
                    uri -> {
                        if (uri == null || pendingExportNote == null) {
                            return;
                        }
                        Note note = pendingExportNote;
                        pendingExportNote = null;
                        executor.execute(() -> {
                            try {
                                BackupUtils.writeText(this, uri,
                                        BackupUtils.noteToText(note), true);
                                toastOnUi("笔记已导出");
                            } catch (IOException e) {
                                toastOnUi("导出失败：" + e.getMessage());
                            }
                        });
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        View root = findViewById(R.id.root);
        toolbar = findViewById(R.id.toolbar);
        titleEdit = findViewById(R.id.edit_title);
        contentEdit = findViewById(R.id.edit_content);
        metaText = findViewById(R.id.edit_meta);
        categoryButton = findViewById(R.id.button_category);
        pinButton = findViewById(R.id.button_pin);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> {
            saveNote();
            finish();
        });
        setupSystemBars(root);

        float fontSize = Prefs.getFontSize(this);
        contentEdit.setTextSize(fontSize);
        titleEdit.setTextSize(fontSize + 7);

        categoryButton.setOnClickListener(v -> showCategoryDialog());
        pinButton.setOnClickListener(v -> {
            pinned = !pinned;
            updatePinButton();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveNote();
                finish();
            }
        });

        Intent intent = getIntent();
        Uri data = intent.getData();
        if (Intent.ACTION_EDIT.equals(intent.getAction()) && data != null) {
            noteId = ContentUris.parseId(data);
            isNew = false;
            toolbar.setTitle(R.string.editor_title_edit);
            loadNote(noteId);
        } else {
            isNew = true;
            createdTime = System.currentTimeMillis();
            modifiedTime = createdTime;
            toolbar.setTitle(R.string.editor_title_new);
            titleEdit.requestFocus();
            updateCategoryButton();
            updatePinButton();
            updateMeta();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!deleting && !loading) {
            saveNote();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void loadNote(long id) {
        loading = true;
        executor.execute(() -> {
            Note note = NotesRepository.get(this, id);
            runOnUiThread(() -> {
                loading = false;
                if (note == null) {
                    Toast.makeText(this, "笔记不存在或已被删除", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                createdTime = note.created;
                modifiedTime = note.modified;
                category = note.category;
                pinned = note.pinned;
                titleEdit.setText(note.title);
                contentEdit.setText(note.content);
                savedTitle = note.title == null ? "" : note.title;
                savedContent = note.content == null ? "" : note.content;
                savedCategory = category;
                savedPinned = pinned;
                updateCategoryButton();
                updatePinButton();
                updateMeta();
            });
        });
    }

    private boolean hasChanges() {
        return !titleEdit.getText().toString().equals(savedTitle)
                || !contentEdit.getText().toString().equals(savedContent)
                || !category.equals(savedCategory)
                || pinned != savedPinned;
    }

    /** 保存当前笔记。空白的新建笔记不会写入数据库。 */
    private void saveNote() {
        if (loading) {
            return;
        }
        String title = titleEdit.getText().toString().trim();
        String content = contentEdit.getText().toString();
        if (isNew && TextUtils.isEmpty(title) && content.trim().isEmpty()) {
            return;
        }
        if (!isNew && !hasChanges()) {
            return;
        }
        if (TextUtils.isEmpty(title)) {
            title = "无标题";
        }
        long now = System.currentTimeMillis();

        if (isNew) {
            Note note = new Note();
            note.title = title;
            note.content = content;
            note.category = category;
            note.pinned = pinned;
            note.created = createdTime;
            note.modified = now;
            long id = NotesRepository.insert(this, note);
            if (id > 0) {
                noteId = id;
                isNew = false;
            }
        } else {
            Note note = new Note();
            note.id = noteId;
            note.title = title;
            note.content = content;
            note.category = category;
            note.pinned = pinned;
            note.created = createdTime;
            note.modified = now;
            NotesRepository.update(this, note);
        }

        modifiedTime = now;
        savedTitle = title;
        savedContent = content;
        savedCategory = category;
        savedPinned = pinned;
        updateMeta();
    }

    private void updateMeta() {
        String created = TimeUtils.full(createdTime);
        String modified = TimeUtils.full(modifiedTime);
        metaText.setText(getString(R.string.editor_meta, created, modified));
    }

    private void updateCategoryButton() {
        categoryButton.setText(getString(R.string.editor_category_prefix, category));
    }

    private void updatePinButton() {
        pinButton.setText(pinned ? R.string.editor_pinned : R.string.editor_pin);
        pinButton.setIconResource(R.drawable.ic_pin);
        pinButton.setChecked(pinned);
    }

    private void showCategoryDialog() {
        final List<String> options = new ArrayList<>(Arrays.asList(PRESET_CATEGORIES));
        for (String existing : NotesRepository.categories(this)) {
            if (!options.contains(existing)) {
                options.add(existing);
            }
        }
        final String customLabel = "自定义分类…";
        options.add(customLabel);

        new AlertDialog.Builder(this)
                .setTitle(R.string.editor_choose_category)
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    String selected = options.get(which);
                    if (customLabel.equals(selected)) {
                        showCustomCategoryDialog();
                    } else {
                        category = selected;
                        updateCategoryButton();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showCustomCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.editor_category_hint);
        input.setSingleLine(true);

        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(input, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle(R.string.editor_custom_category)
                .setView(container)
                .setPositiveButton("确定", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(value)) {
                        category = value;
                        updateCategoryButton();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("删除笔记")
                .setMessage("确定删除这条笔记吗？删除后无法恢复。")
                .setPositiveButton("删除", (dialog, which) -> {
                    deleting = true;
                    if (noteId > 0) {
                        NotesRepository.delete(this, noteId);
                    }
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void exportNote() {
        String title = titleEdit.getText().toString().trim();
        String content = contentEdit.getText().toString();
        if (TextUtils.isEmpty(title) && content.trim().isEmpty()) {
            Toast.makeText(this, "没有可导出的内容", Toast.LENGTH_SHORT).show();
            return;
        }
        Note note = new Note();
        note.title = title;
        note.content = content;
        note.category = category;
        note.pinned = pinned;
        note.created = createdTime;
        note.modified = System.currentTimeMillis();
        pendingExportNote = note;
        exportLauncher.launch(BackupUtils.safeFileName(title) + ".txt");
    }

    private void toastOnUi(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            saveNote();
            Toast.makeText(this, R.string.editor_saved, Toast.LENGTH_SHORT).show();
            finish();
            return true;
        } else if (id == R.id.action_export) {
            exportNote();
            return true;
        } else if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
