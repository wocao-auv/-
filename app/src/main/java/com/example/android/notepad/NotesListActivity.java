package com.example.android.notepad;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 笔记列表界面：
 * 1. 每条笔记显示修改时间戳；
 * 2. 支持按标题或内容搜索；
 * 3. 支持分类筛选、排序、导出与备份恢复。
 */
public class NotesListActivity extends BaseActivity implements NotesAdapter.Listener {

    private static final String MIME_JSON = "application/json";
    private static final String MIME_TEXT = "text/plain";

    private MaterialToolbar toolbar;
    private TextInputEditText searchInput;
    private ChipGroup categoryChipGroup;
    private RecyclerView notesList;
    private View emptyView;
    private TextView emptyTitle;
    private TextView emptyHint;
    private NotesAdapter adapter;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private String searchKeyword = "";
    private String categoryFilter;
    private int loadToken;
    private Runnable pendingSearch;
    private Note pendingExportNote;

    private final ActivityResultLauncher<String> exportNoteLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument(MIME_TEXT),
                    uri -> {
                        if (uri == null || pendingExportNote == null) {
                            return;
                        }
                        String text = BackupUtils.noteToText(pendingExportNote);
                        pendingExportNote = null;
                        writeFile(uri, text, true, "笔记已导出");
                    });

    private final ActivityResultLauncher<String> exportAllLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument(MIME_TEXT),
                    uri -> {
                        if (uri == null) {
                            return;
                        }
                        executor.execute(() -> {
                            List<Note> all = NotesRepository.queryAll(this);
                            String text = BackupUtils.notesToText(all);
                            try {
                                BackupUtils.writeText(this, uri, text, true);
                                toastOnUi("已导出 " + all.size() + " 条笔记");
                            } catch (IOException e) {
                                toastOnUi("导出失败：" + e.getMessage());
                            }
                        });
                    });

    private final ActivityResultLauncher<String> backupLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument(MIME_JSON),
                    uri -> {
                        if (uri == null) {
                            return;
                        }
                        executor.execute(() -> {
                            List<Note> all = NotesRepository.queryAll(this);
                            try {
                                String json = BackupUtils.notesToJson(all);
                                BackupUtils.writeText(this, uri, json, false);
                                toastOnUi("备份完成，共 " + all.size() + " 条笔记");
                            } catch (Exception e) {
                                toastOnUi("备份失败：" + e.getMessage());
                            }
                        });
                    });

    private final ActivityResultLauncher<String[]> restoreLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri != null) {
                            restoreFrom(uri);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list);

        View root = findViewById(R.id.root);
        toolbar = findViewById(R.id.toolbar);
        searchInput = findViewById(R.id.search_input);
        categoryChipGroup = findViewById(R.id.category_chips);
        notesList = findViewById(R.id.notes_list);
        emptyView = findViewById(R.id.empty_view);
        emptyTitle = findViewById(R.id.empty_title);
        emptyHint = findViewById(R.id.empty_hint);
        FloatingActionButton fab = findViewById(R.id.fab);

        setSupportActionBar(toolbar);
        setupSystemBars(root);

        adapter = new NotesAdapter(this);
        notesList.setLayoutManager(new LinearLayoutManager(this));
        notesList.setAdapter(adapter);

        fab.setOnClickListener(v -> startActivity(
                new Intent(this, NoteEditorActivity.class)
                        .setAction(Intent.ACTION_INSERT)));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(pendingSearch);
                pendingSearch = () -> {
                    searchKeyword = s.toString().trim();
                    refreshNotes();
                };
                handler.postDelayed(pendingSearch, 250);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasThemeChanged()) {
            recreate();
            return;
        }
        adapter.setFontScale(Prefs.getFontScale(this));
        refreshNotes();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(pendingSearch);
        executor.shutdownNow();
    }

    /** 按当前搜索关键字、分类与排序方式重新加载列表 */
    private void refreshNotes() {
        final String keyword = searchKeyword;
        final String category = categoryFilter;
        final String sortOrder = Prefs.getSortOrder(this);
        final int token = ++loadToken;

        executor.execute(() -> {
            List<Note> notes = NotesRepository.query(this, keyword, category, sortOrder);
            List<String> categories = NotesRepository.categories(this);
            runOnUiThread(() -> {
                if (token != loadToken || isFinishing()) {
                    return;
                }
                adapter.submit(notes);
                rebuildCategoryChips(categories);
                updateEmptyView(notes.isEmpty());
            });
        });
    }

    private void updateEmptyView(boolean empty) {
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        notesList.setVisibility(empty ? View.GONE : View.VISIBLE);
        boolean searching = !TextUtils.isEmpty(searchKeyword) || categoryFilter != null;
        emptyTitle.setText(searching ? "没有找到相关笔记" : "还没有笔记");
        emptyHint.setText(searching ? "换个关键字或分类试试" : "点击右下角的 + 新建一条笔记");
    }

    private void rebuildCategoryChips(List<String> categories) {
        categoryChipGroup.removeAllViews();
        addCategoryChip(null, "全部");
        for (String category : categories) {
            addCategoryChip(category, category);
        }
        if (categoryFilter != null && !categories.contains(categoryFilter)) {
            addCategoryChip(categoryFilter, categoryFilter);
        }
    }

    private void addCategoryChip(String value, String label) {
        Chip chip = (Chip) getLayoutInflater()
                .inflate(R.layout.item_category_chip, categoryChipGroup, false);
        chip.setText(label);
        boolean checked = value == null
                ? categoryFilter == null : value.equals(categoryFilter);
        chip.setChecked(checked);
        chip.setOnClickListener(v -> {
            categoryFilter = value;
            refreshNotes();
        });
        categoryChipGroup.addView(chip);
    }

    @Override
    public void onNoteClick(Note note) {
        Intent intent = new Intent(this, NoteEditorActivity.class)
                .setAction(Intent.ACTION_EDIT)
                .setData(ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, note.id));
        startActivity(intent);
    }

    @Override
    public void onNoteLongClick(Note note) {
        String[] options = {
                note.pinned ? "取消置顶" : "置顶",
                "导出为文本",
                "删除笔记"
        };
        new AlertDialog.Builder(this)
                .setTitle(note.hasTitle() ? note.title : "无标题")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            NotesRepository.setPinned(this, note.id, !note.pinned);
                            refreshNotes();
                            break;
                        case 1:
                            pendingExportNote = note;
                            exportNoteLauncher.launch(
                                    BackupUtils.safeFileName(note.title) + ".txt");
                            break;
                        default:
                            confirmDelete(note);
                            break;
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmDelete(Note note) {
        new AlertDialog.Builder(this)
                .setTitle("删除笔记")
                .setMessage("确定删除「" + (note.hasTitle() ? note.title : "无标题") + "」吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    NotesRepository.delete(this, note.id);
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                    refreshNotes();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void restoreFrom(Uri uri) {
        executor.execute(() -> {
            try {
                String json = BackupUtils.readText(this, uri);
                List<Note> backups = BackupUtils.notesFromJson(json);
                runOnUiThread(() -> confirmRestore(backups));
            } catch (Exception e) {
                toastOnUi("无法读取备份文件：" + e.getMessage());
            }
        });
    }

    private void confirmRestore(List<Note> backups) {
        if (backups.isEmpty()) {
            Toast.makeText(this, "备份文件中没有笔记", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("恢复备份")
                .setMessage("备份中包含 " + backups.size() + " 条笔记，是否导入？")
                .setPositiveButton("导入", (dialog, which) -> importNotes(backups))
                .setNegativeButton("取消", null)
                .show();
    }

    private void importNotes(List<Note> backups) {
        executor.execute(() -> {
            int imported = 0;
            int skipped = 0;
            for (Note note : backups) {
                if (NotesRepository.exists(this, note.title, note.created)) {
                    skipped++;
                    continue;
                }
                note.id = -1;
                if (NotesRepository.insert(this, note) > 0) {
                    imported++;
                }
            }
            final int importedCount = imported;
            final int skippedCount = skipped;
            runOnUiThread(() -> {
                String message = "已恢复 " + importedCount + " 条笔记";
                if (skippedCount > 0) {
                    message += "，跳过 " + skippedCount + " 条重复笔记";
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                refreshNotes();
            });
        });
    }

    private void writeFile(Uri uri, String text, boolean bom, String successMessage) {
        executor.execute(() -> {
            try {
                BackupUtils.writeText(this, uri, text, bom);
                toastOnUi(successMessage);
            } catch (IOException e) {
                toastOnUi("导出失败：" + e.getMessage());
            }
        });
    }

    private void toastOnUi(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notes_list, menu);
        return true;
    }

    /** 排序选择对话框，当前排序方式带单选标记 */
    private void showSortDialog() {
        final String[] labels = {
                getString(R.string.sort_modified),
                getString(R.string.sort_created),
                getString(R.string.sort_title)
        };
        final String[] values = {
                Prefs.SORT_MODIFIED,
                Prefs.SORT_CREATED,
                Prefs.SORT_TITLE
        };
        String current = Prefs.getSortOrder(this);
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) {
                checked = i;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_sort)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    Prefs.setSortOrder(this, values[which]);
                    refreshNotes();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sort) {
            showSortDialog();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_backup) {
            backupLauncher.launch("记事本备份_" + TimeUtils.fileStamp() + ".json");
            return true;
        } else if (id == R.id.action_restore) {
            restoreLauncher.launch(new String[]{MIME_JSON, MIME_TEXT,
                    "application/octet-stream"});
            return true;
        } else if (id == R.id.action_export_all) {
            exportAllLauncher.launch("记事本导出_" + TimeUtils.fileStamp() + ".txt");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
