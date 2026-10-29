package com.harpreet.panna;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/** Metadata validation, collection membership and interoperable catalog text. */
public final class PannaResearch {
    private PannaResearch() {}
    public static boolean validSource(String source) {
        if (source.isEmpty()) return true;
        if (source.length() > 2048) return false;
        try {
            URI uri = new URI(source);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost() != null && !uri.getHost().isEmpty() && uri.getRawUserInfo() == null
                && (uri.getPort() == -1 || (uri.getPort() >= 0 && uri.getPort() <= 65535));
        } catch (URISyntaxException e) { return false; }
    }
    public static boolean validDate(String date) {
        if (date.isEmpty()) return true;
        if (!date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) return false;
        int y = Integer.parseInt(date.substring(0, 4)), m = Integer.parseInt(date.substring(5, 7)), d = Integer.parseInt(date.substring(8, 10));
        if (y < 1 || m < 1 || m > 12) return false;
        int[] days = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) days[1] = 29;
        return d >= 1 && d <= days[m - 1];
    }
    public static boolean validCollection(String name) {
        if (name.isEmpty() || name.length() > 80 || !name.equals(name.trim())) return false;
        for (int i = 0; i < name.length(); i++) if (Character.isISOControl(name.charAt(i))) return false;
        return true;
    }
    public static boolean inCollection(List<String> memberships, String selected) {
        return selected == null || (selected.isEmpty() ? memberships.isEmpty() : memberships.contains(selected));
    }
    /** Quoted RFC 4180 cell; prefix spreadsheet formula-like text with an apostrophe. */
    public static String csvCell(String value) {
        String text = value == null ? "" : value;
        int first = 0;
        while (first < text.length() && (Character.isWhitespace(text.charAt(first)) || text.charAt(first) == '\uFEFF')) first++;
        boolean controlPrefix = !text.isEmpty() && (text.charAt(0) == '\t' || text.charAt(0) == '\r' || text.charAt(0) == '\n');
        if (controlPrefix || (first < text.length() && "=+-@".indexOf(text.charAt(first)) >= 0)) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
    public static String csvRow(String... values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++) { if (i > 0) out.append(','); out.append(csvCell(values[i])); }
        return out.append("\r\n").toString();
    }
}
