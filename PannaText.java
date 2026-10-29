package com.harpreet.panna;

import java.util.Locale;

/** Platform-independent reading calculations; offsets use TextView's UTF-16 indexing. */
public final class PannaText {
    private PannaText() {}
    public static final int MAX_TEXT_BYTES = 2 * 1024 * 1024;
    public static int words(String text) {
        String t = text.trim();
        return t.isEmpty() ? 0 : t.split("\\s+").length;
    }
    public static int minutes(String text) { return Math.max(1, (words(text) + 199) / 200); }
    public static double progress(int offset, int length) {
        return length == 0 ? 0 : Math.max(0, Math.min(1, (double) offset / length));
    }
    public static boolean matches(String query, String... fields) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (String f : fields) if (f.toLowerCase(Locale.ROOT).contains(q)) return true;
        return false;
    }
    /** Stay under the engine limit, prefer whitespace, and never split a surrogate pair. */
    public static int speechChunkEnd(String text, int start, int maxUnits) {
        if (maxUnits < 2 || start < 0 || start > text.length()) throw new IllegalArgumentException("Invalid chunk bounds");
        int end = start + Math.min(maxUnits, text.length() - start);
        if (end < text.length()) {
            if (end > start && Character.isHighSurrogate(text.charAt(end - 1)) && Character.isLowSurrogate(text.charAt(end))) end--;
            for (int p = end - 1; p > start + (end - start) / 2; p--) {
                if (Character.isWhitespace(text.charAt(p))) { end = p + 1; break; }
            }
        }
        return end;
    }
    public static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
