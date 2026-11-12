package com.example.android.notepad;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * NotePad 数据库契约类，定义了 ContentProvider 的 Authority、URI 以及笔记表的列名。
 * 结构与 Android 官方 NotePad 示例保持一致，并扩展了分类与置顶字段。
 */
public final class NotePad {

    /** ContentProvider 的 Authority，必须与 AndroidManifest 中的声明一致 */
    public static final String AUTHORITY = "com.example.notepad";

    private NotePad() {
    }

    public static final class Notes implements BaseColumns {

        /** 笔记集合的 URI：content://com.example.notepad/notes */
        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/notes");

        /** 笔记集合的 MIME 类型 */
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.google.note";

        /** 单条笔记的 MIME 类型 */
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.google.note";

        /** 默认排序：最近修改的排在前面 */
        public static final String DEFAULT_SORT_ORDER = "modified DESC";

        /** 笔记标题 */
        public static final String TITLE = "title";

        /** 笔记正文 */
        public static final String NOTE = "note";

        /** 创建时间（毫秒时间戳） */
        public static final String CREATED_DATE = "created";

        /** 最近修改时间（毫秒时间戳） */
        public static final String MODIFIED_DATE = "modified";

        /** 笔记分类 */
        public static final String CATEGORY = "category";

        /** 是否置顶：0 否，1 是 */
        public static final String PINNED = "pinned";
    }
}
