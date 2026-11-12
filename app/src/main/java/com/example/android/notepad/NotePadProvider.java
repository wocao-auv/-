package com.example.android.notepad;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;

/**
 * NotePad 的 ContentProvider，负责笔记数据的增删改查。
 * 数据库表结构：notes(_id, title, note, created, modified, category, pinned)
 */
public class NotePadProvider extends ContentProvider {

    private static final String DATABASE_NAME = "note_pad.db";
    private static final int DATABASE_VERSION = 2;
    private static final String NOTES_TABLE_NAME = "notes";

    private static final int NOTES = 1;
    private static final int NOTE_ID = 2;

    private static final HashMap<String, String> sNotesProjectionMap = new HashMap<>();
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes", NOTES);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID);

        sNotesProjectionMap.put(NotePad.Notes._ID, NotePad.Notes._ID);
        sNotesProjectionMap.put(NotePad.Notes.TITLE, NotePad.Notes.TITLE);
        sNotesProjectionMap.put(NotePad.Notes.NOTE, NotePad.Notes.NOTE);
        sNotesProjectionMap.put(NotePad.Notes.CREATED_DATE, NotePad.Notes.CREATED_DATE);
        sNotesProjectionMap.put(NotePad.Notes.MODIFIED_DATE, NotePad.Notes.MODIFIED_DATE);
        sNotesProjectionMap.put(NotePad.Notes.CATEGORY, NotePad.Notes.CATEGORY);
        sNotesProjectionMap.put(NotePad.Notes.PINNED, NotePad.Notes.PINNED);
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(NOTES_TABLE_NAME);
        qb.setProjectionMap(sNotesProjectionMap);

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                break;
            case NOTE_ID:
                qb.appendWhere(NotePad.Notes._ID + "=" + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        String orderBy = TextUtils.isEmpty(sortOrder)
                ? NotePad.Notes.DEFAULT_SORT_ORDER : sortOrder;

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor cursor = qb.query(db, projection, selection, selectionArgs,
                null, null, orderBy);
        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NOTES:
                return NotePad.Notes.CONTENT_TYPE;
            case NOTE_ID:
                return NotePad.Notes.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (sUriMatcher.match(uri) != NOTES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values = initialValues != null
                ? new ContentValues(initialValues) : new ContentValues();

        Long now = System.currentTimeMillis();
        if (!values.containsKey(NotePad.Notes.CREATED_DATE)) {
            values.put(NotePad.Notes.CREATED_DATE, now);
        }
        if (!values.containsKey(NotePad.Notes.MODIFIED_DATE)) {
            values.put(NotePad.Notes.MODIFIED_DATE, now);
        }
        if (!values.containsKey(NotePad.Notes.TITLE)) {
            values.put(NotePad.Notes.TITLE, "");
        }
        if (!values.containsKey(NotePad.Notes.NOTE)) {
            values.put(NotePad.Notes.NOTE, "");
        }
        if (!values.containsKey(NotePad.Notes.CATEGORY)) {
            values.put(NotePad.Notes.CATEGORY, Note.DEFAULT_CATEGORY);
        }
        if (!values.containsKey(NotePad.Notes.PINNED)) {
            values.put(NotePad.Notes.PINNED, 0);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(NOTES_TABLE_NAME, NotePad.Notes.NOTE, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, rowId);
            if (getContext() != null) {
                getContext().getContentResolver().notifyChange(noteUri, null);
                getContext().getContentResolver().notifyChange(NotePad.Notes.CONTENT_URI, null);
            }
            return noteUri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case NOTES:
                count = db.delete(NOTES_TABLE_NAME, where, whereArgs);
                break;
            case NOTE_ID:
                String noteId = uri.getPathSegments().get(1);
                count = db.delete(NOTES_TABLE_NAME,
                        NotePad.Notes._ID + "=" + noteId
                                + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
                        whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
            getContext().getContentResolver().notifyChange(NotePad.Notes.CONTENT_URI, null);
        }
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case NOTES:
                count = db.update(NOTES_TABLE_NAME, values, where, whereArgs);
                break;
            case NOTE_ID:
                String noteId = uri.getPathSegments().get(1);
                count = db.update(NOTES_TABLE_NAME, values,
                        NotePad.Notes._ID + "=" + noteId
                                + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
                        whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
            getContext().getContentResolver().notifyChange(NotePad.Notes.CONTENT_URI, null);
        }
        return count;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + NOTES_TABLE_NAME + " ("
                    + NotePad.Notes._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + NotePad.Notes.TITLE + " TEXT,"
                    + NotePad.Notes.NOTE + " TEXT,"
                    + NotePad.Notes.CREATED_DATE + " INTEGER,"
                    + NotePad.Notes.MODIFIED_DATE + " INTEGER,"
                    + NotePad.Notes.CATEGORY + " TEXT DEFAULT '" + Note.DEFAULT_CATEGORY + "',"
                    + NotePad.Notes.PINNED + " INTEGER NOT NULL DEFAULT 0"
                    + ");");
            db.execSQL("CREATE INDEX notes_modified_index ON "
                    + NOTES_TABLE_NAME + " (" + NotePad.Notes.MODIFIED_DATE + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                // 第一版升级到第二版：新增分类与置顶字段
                db.execSQL("ALTER TABLE " + NOTES_TABLE_NAME
                        + " ADD COLUMN " + NotePad.Notes.CATEGORY
                        + " TEXT DEFAULT '" + Note.DEFAULT_CATEGORY + "'");
                db.execSQL("ALTER TABLE " + NOTES_TABLE_NAME
                        + " ADD COLUMN " + NotePad.Notes.PINNED
                        + " INTEGER NOT NULL DEFAULT 0");
            }
        }
    }
}
