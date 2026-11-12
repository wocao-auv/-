package com.example.android.notepad;

/**
 * 笔记实体类，用于在界面与数据层之间传递数据。
 */
public class Note {

    /** 未分类笔记使用的默认分类名 */
    public static final String DEFAULT_CATEGORY = "未分类";

    public long id = -1;
    public String title = "";
    public String content = "";
    public String category = DEFAULT_CATEGORY;
    public long created;
    public long modified;
    public boolean pinned;

    public Note() {
    }

    public boolean hasTitle() {
        return title != null && !title.trim().isEmpty();
    }
}
