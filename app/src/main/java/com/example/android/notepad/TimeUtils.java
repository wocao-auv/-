package com.example.android.notepad;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 时间格式化工具。列表使用友好的相对时间，编辑页与导出使用完整时间。
 */
public final class TimeUtils {

    private TimeUtils() {
    }

    public static String full(long time) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new Date(time));
    }

    /** 列表时间戳：今天 08:30 / 昨天 21:05 / 3月8日 12:00 / 2025年3月8日 */
    public static String smart(long time) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(time);

        boolean sameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR);
        boolean sameDay = sameYear
                && now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR);

        if (sameDay) {
            return "今天 " + new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date(time));
        }

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        boolean isYesterday = yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR)
                && yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR);
        if (isYesterday) {
            return "昨天 " + new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date(time));
        }

        if (sameYear) {
            return new SimpleDateFormat("M月d日 HH:mm", Locale.getDefault())
                    .format(new Date(time));
        }
        return new SimpleDateFormat("yyyy年M月d日", Locale.getDefault())
                .format(new Date(time));
    }

    /** 用于生成备份、导出文件名 */
    public static String fileStamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date());
    }
}
