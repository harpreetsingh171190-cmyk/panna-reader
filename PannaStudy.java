package com.harpreet.panna;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure text analysis. Every position refers to the unchanged UTF-16 source text. */
public final class PannaStudy {
    private PannaStudy() {}
    public static final class Heading {
        public final String title;
        public final int offset, level;
        private Heading(String title, int offset, int level) { this.title = title; this.offset = offset; this.level = level; }
    }
    private static final Pattern ATX = Pattern.compile("^ {0,3}(#{1,6})[ \\t]+(.+?)\\s*$");
    private static final Pattern FENCE = Pattern.compile("^ {0,3}(`{3,}|~{3,})(.*)$");
    private static final Pattern UNDERLINE = Pattern.compile("^ {0,3}(=+|-+)[ \\t]*$");
    public static List<Heading> headings(String text) {
        ArrayList<Heading> result = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        int offset = 0, previousOffset = 0, fenceLength = 0;
        char fenceChar = 0;
        String previous = "";
        for (String raw : lines) {
            String line = raw.endsWith("\r") ? raw.substring(0, raw.length() - 1) : raw;
            Matcher fence = FENCE.matcher(line);
            if (fence.matches()) {
                String run = fence.group(1);
                if (fenceChar == 0 && (run.charAt(0) != '`' || fence.group(2).indexOf('`') < 0)) {
                    fenceChar = run.charAt(0); fenceLength = run.length();
                } else if (fenceChar == run.charAt(0) && run.length() >= fenceLength && fence.group(2).trim().isEmpty()) {
                    fenceChar = 0; fenceLength = 0;
                }
                previous = "";
            } else if (fenceChar != 0) previous = "";
            else {
                Matcher atx = ATX.matcher(line), under = UNDERLINE.matcher(line);
                if (atx.matches()) {
                    String title = atx.group(2).replaceFirst("[ \\t]+#+[ \\t]*$", "").trim();
                    if (!title.isEmpty()) result.add(new Heading(title, offset, atx.group(1).length()));
                    previous = "";
                } else if (under.matches() && !previous.trim().isEmpty() && !previous.startsWith("    ") && !previous.startsWith("\t")) {
                    result.add(new Heading(previous.trim(), previousOffset, under.group(1).charAt(0) == '=' ? 1 : 2));
                    previous = "";
                } else { previous = line; previousOffset = offset; }
            }
            offset += raw.length() + 1;
            if (result.size() >= 300) break;
        }
        return result;
    }
    public static boolean boundary(String text, int offset) {
        return offset >= 0 && offset <= text.length() && !(offset > 0 && offset < text.length()
            && Character.isHighSurrogate(text.charAt(offset - 1)) && Character.isLowSurrogate(text.charAt(offset)));
    }
    public static boolean validRange(String text, int start, int end) {
        return start < end && boundary(text, start) && boundary(text, end);
    }
    public static boolean overlaps(int start, int end, int otherStart, int otherEnd) {
        return start < otherEnd && otherStart < end;
    }
}
