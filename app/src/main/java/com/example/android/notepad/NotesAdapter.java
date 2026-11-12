package com.example.android.notepad;

import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 笔记列表适配器：卡片展示标题、内容摘要、分类、时间戳与置顶标记。
 */
public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    public interface Listener {
        void onNoteClick(Note note);

        void onNoteLongClick(Note note);
    }

    private final List<Note> notes = new ArrayList<>();
    private final Listener listener;
    private float fontScale = 1f;

    public NotesAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Note> data) {
        notes.clear();
        notes.addAll(data);
        notifyDataSetChanged();
    }

    public void setFontScale(float scale) {
        if (Math.abs(scale - fontScale) > 0.01f) {
            fontScale = scale;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.bind(note);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView snippet;
        private final TextView category;
        private final TextView time;
        private final ImageView pin;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.note_title);
            snippet = itemView.findViewById(R.id.note_snippet);
            category = itemView.findViewById(R.id.note_category);
            time = itemView.findViewById(R.id.note_time);
            pin = itemView.findViewById(R.id.note_pin);
        }

        void bind(Note note) {
            String displayTitle = note.hasTitle() ? note.title.trim() : "无标题";
            title.setText(displayTitle);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17 * fontScale);

            String content = note.content == null ? "" : note.content.trim();
            String preview = content.replace('\n', ' ');
            if (TextUtils.isEmpty(preview)) {
                preview = "暂无内容";
            }
            snippet.setText(preview);
            snippet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14 * fontScale);

            category.setText(note.category);
            time.setText(TimeUtils.smart(note.modified > 0 ? note.modified : note.created));
            time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12 * fontScale);
            pin.setVisibility(note.pinned ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> listener.onNoteClick(note));
            itemView.setOnLongClickListener(v -> {
                listener.onNoteLongClick(note);
                return true;
            });
        }
    }
}
