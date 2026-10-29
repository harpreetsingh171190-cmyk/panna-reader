package com.harpreet.panna;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Bounded literal search: AND terms, quoted phrases, exclusion and named fields; no regex. */
public final class PannaSearch {
    private PannaSearch() {}
    public interface Document {
        String[] values(String field);
        boolean has(String kind);
        boolean is(String state);
    }
    private static final Set<String> FIELDS = new HashSet<>(Arrays.asList("title", "text", "note", "tag", "author", "source", "published", "collection", "has", "is"));
    private static final Set<String> HAS = new HashSet<>(Arrays.asList("notes", "annotations", "highlights", "bookmarks", "source"));
    private static final Set<String> STATES = new HashSet<>(Arrays.asList("favorite", "pinned", "archived", "trash", "finished", "unread", "inprogress"));
    private static final class Term {
        final String field, value;
        final boolean excluded;
        Term(String field, String value, boolean excluded) { this.field = field; this.value = value.toLowerCase(Locale.ROOT); this.excluded = excluded; }
        boolean matches(Document doc) {
            if (field.equals("has")) return doc.has(value);
            if (field.equals("is")) return doc.is(value);
            for (String text : doc.values(field)) if (containsIgnoreCase(text, value)) return true;
            return false;
        }
    }
    /** Case-insensitive literal search without lower-casing (and copying) the whole text. */
    static boolean containsIgnoreCase(String text, String needle) {
        int n = needle.length(), last = text.length() - n;
        for (int p = 0; p <= last; p++) if (text.regionMatches(true, p, needle, 0, n)) return true;
        return n == 0;
    }
    public static final class Query {
        private final List<Term> terms;
        private Query(List<Term> terms) { this.terms = terms; }
        public boolean matches(Document doc) {
            for (Term term : terms) if (term.matches(doc) == term.excluded) return false;
            return true;
        }
    }
    public static Query parse(String source) {
        if (source.length() > 500) throw new IllegalArgumentException("Search is limited to 500 characters");
        ArrayList<Term> terms = new ArrayList<>();
        StringBuilder token = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i <= source.length(); i++) {
            char c = i == source.length() ? ' ' : source.charAt(i);
            if (i < source.length() && c == '\\' && i + 1 < source.length() && (source.charAt(i + 1) == '"' || source.charAt(i + 1) == '\\')) { token.append(source.charAt(++i)); continue; }
            if (i < source.length() && c == '"') { quoted = !quoted; continue; }
            if ((i == source.length() || Character.isWhitespace(c)) && !quoted) {
                if (token.length() > 0) { add(terms, token.toString()); token.setLength(0); }
            } else if (i < source.length()) token.append(c);
        }
        if (quoted) throw new IllegalArgumentException("Close the quoted phrase with a double quote");
        return new Query(terms);
    }
    private static void add(List<Term> terms, String token) {
        if (terms.size() >= 16) throw new IllegalArgumentException("Use at most 16 search terms");
        boolean excluded = token.startsWith("-") && token.length() > 1;
        if (excluded) token = token.substring(1);
        String field = "all"; int colon = token.indexOf(':');
        if (colon > 0 && FIELDS.contains(token.substring(0, colon).toLowerCase(Locale.ROOT))) {
            field = token.substring(0, colon).toLowerCase(Locale.ROOT); token = token.substring(colon + 1);
            if (token.isEmpty()) throw new IllegalArgumentException("Enter a value after " + field + ":");
        }
        String value = token.toLowerCase(Locale.ROOT);
        if (field.equals("has") && !HAS.contains(value)) throw new IllegalArgumentException("has: supports notes, annotations, highlights, bookmarks or source");
        if (field.equals("is") && !STATES.contains(value)) throw new IllegalArgumentException("Unknown is: state");
        terms.add(new Term(field, token, excluded));
    }
}
