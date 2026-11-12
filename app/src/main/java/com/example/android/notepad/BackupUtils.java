package com.example.android.notepad;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 笔记导出与备份工具：支持导出 txt、备份为 JSON、从 JSON 恢复。
 */
public final class BackupUtils {

    private static final String JSON_VERSION = "version";
    private static final String JSON_EXPORTED_AT = "exportedAt";
    private static final String JSON_NOTES = "notes";
    private static final String JSON_TITLE = "title";
    private static final String JSON_CONTENT = "content";
    private static final String JSON_CREATED = "created";
    private static final String JSON_MODIFIED = "modified";
    private static final String JSON_CATEGORY = "category";
    private static final String JSON_PINNED = "pinned";

    private BackupUtils() {
    }

    public static String noteToText(Note note) {
        StringBuilder builder = new StringBuilder();
        builder.append(note.hasTitle() ? note.title : "无标题").append('\n');
        builder.append("分类：").append(note.category).append('\n');
        builder.append("创建时间：").append(TimeUtils.full(note.created)).append('\n');
        builder.append("修改时间：").append(TimeUtils.full(note.modified)).append('\n');
        builder.append("------------------------------\n\n");
        builder.append(note.content == null ? "" : note.content).append('\n');
        return builder.toString();
    }

    public static String notesToText(List<Note> notes) {
        StringBuilder builder = new StringBuilder();
        builder.append("记事本导出\n");
        builder.append("导出时间：").append(TimeUtils.full(System.currentTimeMillis())).append('\n');
        builder.append("笔记数量：").append(notes.size()).append('\n');
        builder.append("==============================\n\n");
        for (Note note : notes) {
            builder.append(noteToText(note));
            builder.append("\n==============================\n\n");
        }
        return builder.toString();
    }

    public static String notesToJson(List<Note> notes) throws JSONException {
        JSONObject root = new JSONObject();
        root.put(JSON_VERSION, 1);
        root.put(JSON_EXPORTED_AT, System.currentTimeMillis());
        JSONArray array = new JSONArray();
        for (Note note : notes) {
            JSONObject item = new JSONObject();
            item.put(JSON_TITLE, note.title == null ? "" : note.title);
            item.put(JSON_CONTENT, note.content == null ? "" : note.content);
            item.put(JSON_CREATED, note.created);
            item.put(JSON_MODIFIED, note.modified);
            item.put(JSON_CATEGORY, note.category == null ? Note.DEFAULT_CATEGORY : note.category);
            item.put(JSON_PINNED, note.pinned);
            array.put(item);
        }
        root.put(JSON_NOTES, array);
        return root.toString(2);
    }

    public static List<Note> notesFromJson(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONArray array = root.optJSONArray(JSON_NOTES);
        List<Note> notes = new ArrayList<>();
        if (array == null) {
            return notes;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            Note note = new Note();
            note.title = item.optString(JSON_TITLE, "");
            note.content = item.optString(JSON_CONTENT, "");
            note.created = item.optLong(JSON_CREATED, System.currentTimeMillis());
            note.modified = item.optLong(JSON_MODIFIED, note.created);
            note.category = item.optString(JSON_CATEGORY, Note.DEFAULT_CATEGORY);
            note.pinned = item.optBoolean(JSON_PINNED, false);
            notes.add(note);
        }
        return notes;
    }

    /** 导出 txt 时加上 BOM，避免 Windows 记事本打开中文乱码 */
    public static void writeText(Context context, Uri uri, String text, boolean withBom)
            throws IOException {
        String content = withBom ? "\uFEFF" + text : text;
        try (OutputStream output = context.getContentResolver().openOutputStream(uri, "wt")) {
            if (output == null) {
                throw new IOException("无法写入文件");
            }
            output.write(content.getBytes(StandardCharsets.UTF_8));
            output.flush();
        }
    }

    public static String readText(Context context, Uri uri) throws IOException {
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IOException("无法读取文件");
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int length;
            while ((length = input.read(chunk)) != -1) {
                buffer.write(chunk, 0, length);
            }
            String text = buffer.toString(StandardCharsets.UTF_8.name());
            if (text.startsWith("\uFEFF")) {
                text = text.substring(1);
            }
            return text;
        }
    }

    /** 生成安全的文件名，去掉文件系统不允许的字符 */
    public static String safeFileName(String name) {
        String base = TextUtils.isEmpty(name) ? "笔记" : name.trim();
        base = base.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");
        if (base.length() > 30) {
            base = base.substring(0, 30);
        }
        return base;
    }
}
