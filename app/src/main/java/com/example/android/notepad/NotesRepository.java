package com.example.android.notepad;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 笔记数据访问工具类，统一封装对 ContentProvider 的查询与写入操作。
 */
public final class NotesRepository {

    private static final String[] PROJECTION = {
            NotePad.Notes._ID,
            NotePad.Notes.TITLE,
            NotePad.Notes.NOTE,
            NotePad.Notes.CREATED_DATE,
            NotePad.Notes.MODIFIED_DATE,
            NotePad.Notes.CATEGORY,
            NotePad.Notes.PINNED
    };

    private NotesRepository() {
    }

    /**
     * 按关键字（标题或内容）、分类查询笔记。置顶笔记始终排在最前。
     */
    public static List<Note> query(Context context, String search, String category,
                                   String sortOrder) {
        StringBuilder selection = new StringBuilder();
        ArrayList<String> args = new ArrayList<>();

        if (!TextUtils.isEmpty(search)) {
            selection.append("(")
                    .append(NotePad.Notes.TITLE).append(" LIKE ? OR ")
                    .append(NotePad.Notes.NOTE).append(" LIKE ?)");
            String like = "%" + search + "%";
            args.add(like);
            args.add(like);
        }
        if (category != null) {
            if (selection.length() > 0) {
                selection.append(" AND ");
            }
            selection.append(NotePad.Notes.CATEGORY).append(" = ?");
            args.add(category);
        }

        String order = NotePad.Notes.PINNED + " DESC, " + sortOrder;
        ContentResolver resolver = context.getContentResolver();
        List<Note> notes = new ArrayList<>();
        Cursor cursor = resolver.query(
                NotePad.Notes.CONTENT_URI,
                PROJECTION,
                selection.length() == 0 ? null : selection.toString(),
                args.isEmpty() ? null : args.toArray(new String[0]),
                order);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    notes.add(readNote(cursor));
                }
            } finally {
                cursor.close();
            }
        }
        return notes;
    }

    public static List<Note> queryAll(Context context) {
        return query(context, null, null, NotePad.Notes.DEFAULT_SORT_ORDER);
    }

    public static Note get(Context context, long id) {
        Cursor cursor = context.getContentResolver().query(
                ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, id),
                PROJECTION, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return readNote(cursor);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    public static long insert(Context context, Note note) {
        ContentValues values = toValues(note);
        values.remove(NotePad.Notes._ID);
        Uri uri = context.getContentResolver()
                .insert(NotePad.Notes.CONTENT_URI, values);
        return uri == null ? -1 : ContentUris.parseId(uri);
    }

    public static void update(Context context, Note note) {
        context.getContentResolver().update(
                ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, note.id),
                toValues(note), null, null);
    }

    public static void delete(Context context, long id) {
        context.getContentResolver().delete(
                ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, id),
                null, null);
    }

    public static void setPinned(Context context, long id, boolean pinned) {
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.PINNED, pinned ? 1 : 0);
        // 置顶只改变排列顺序，不更新修改时间
        context.getContentResolver().update(
                ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, id),
                values, null, null);
    }

    /**
     * 查询所有已使用的分类（去重、排序）。
     */
    public static List<String> categories(Context context) {
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        Cursor cursor = context.getContentResolver().query(
                NotePad.Notes.CONTENT_URI,
                new String[]{NotePad.Notes.CATEGORY},
                null, null, NotePad.Notes.CATEGORY + " ASC");
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String category = cursor.getString(0);
                    if (!TextUtils.isEmpty(category)) {
                        categories.add(category);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return new ArrayList<>(categories);
    }

    /**
     * 判断是否已存在相同标题与创建时间的笔记，用于恢复备份时去重。
     */
    public static boolean exists(Context context, String title, long created) {
        Cursor cursor = context.getContentResolver().query(
                NotePad.Notes.CONTENT_URI,
                new String[]{NotePad.Notes._ID},
                NotePad.Notes.TITLE + " = ? AND " + NotePad.Notes.CREATED_DATE + " = ?",
                new String[]{title == null ? "" : title, String.valueOf(created)},
                null);
        if (cursor != null) {
            try {
                return cursor.moveToFirst();
            } finally {
                cursor.close();
            }
        }
        return false;
    }

    private static ContentValues toValues(Note note) {
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.TITLE, note.title == null ? "" : note.title);
        values.put(NotePad.Notes.NOTE, note.content == null ? "" : note.content);
        values.put(NotePad.Notes.CREATED_DATE, note.created);
        values.put(NotePad.Notes.MODIFIED_DATE, note.modified);
        values.put(NotePad.Notes.CATEGORY,
                TextUtils.isEmpty(note.category) ? Note.DEFAULT_CATEGORY : note.category);
        values.put(NotePad.Notes.PINNED, note.pinned ? 1 : 0);
        return values;
    }

    private static Note readNote(Cursor cursor) {
        Note note = new Note();
        note.id = cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Notes._ID));
        note.title = cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.TITLE));
        note.content = cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.NOTE));
        note.created = cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Notes.CREATED_DATE));
        note.modified = cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Notes.MODIFIED_DATE));
        note.category = cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.CATEGORY));
        note.pinned = cursor.getInt(cursor.getColumnIndexOrThrow(NotePad.Notes.PINNED)) == 1;
        if (TextUtils.isEmpty(note.category)) {
            note.category = Note.DEFAULT_CATEGORY;
        }
        return note;
    }
}
