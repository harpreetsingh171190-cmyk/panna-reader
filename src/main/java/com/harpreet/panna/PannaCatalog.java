package com.harpreet.panna;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Platform-independent catalog rules shared by the UI and backup merge. */
public final class PannaCatalog {
    private PannaCatalog() {}
    public interface Key<T> { String of(T entry); }
    public static final class Merge<T> {
        public final ArrayList<T> entries;
        public final int added, skipped;
        private Merge(ArrayList<T> entries, int added, int skipped) {
            this.entries = entries; this.added = added; this.skipped = skipped;
        }
    }
    /** Existing IDs always win, including items in Trash. Never mutates either input. */
    public static <T> Merge<T> merge(List<T> current, List<T> incoming, Key<T> key, int limit) {
        if (limit < 0 || current.size() > limit) throw new IllegalArgumentException("Library exceeds entry limit");
        ArrayList<T> result = new ArrayList<>(current);
        HashSet<String> known = new HashSet<>();
        for (T item : current) {
            String id = key.of(item);
            if (id == null || id.isEmpty() || !known.add(id)) throw new IllegalArgumentException("Invalid or duplicate local entry ID");
        }
        int skipped = 0;
        for (T item : incoming) {
            String id = key.of(item);
            if (id == null || id.isEmpty()) throw new IllegalArgumentException("Invalid incoming entry ID");
            if (!known.add(id)) { skipped++; continue; }
            if (result.size() >= limit) throw new IllegalArgumentException("Merge would exceed " + limit + " entries, including Trash. Permanently delete items you no longer need, or restore instead.");
            result.add(item);
        }
        return new Merge<>(result, result.size() - current.size(), skipped);
    }
    public static boolean inSection(boolean trashed, boolean archived, String section) {
        if (section.equals("Trash")) return trashed;
        if (trashed) return false;
        if (section.equals("Archived")) return archived;
        return !archived;
    }
    public static int pinPriority(boolean left, boolean right) {
        return left == right ? 0 : left ? -1 : 1;
    }
    public static boolean tagMatches(String actual, String selected) {
        return selected == null || actual.equals(selected);
    }
}
