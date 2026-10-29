package com.amrik.panna;

import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** One atomic local document: validation completes before the live shelf is replaced. */
public final class PannaShelf {
    public final ArrayList<PannaEntry> entries = new ArrayList<>();
    private final AtomicFile disk;
    public static final int MAX_BACKUP_BYTES = 24 * 1024 * 1024;
    public PannaShelf(Context context) { disk = new AtomicFile(new File(context.getFilesDir(), "panna_shelf.json")); }
    public void load() throws Exception {
        try (InputStream in = disk.openRead()) { restore(read(in, MAX_BACKUP_BYTES)); }
        catch (FileNotFoundException ignored) { }
    }
    public String backup() throws JSONException {
        JSONArray a = new JSONArray();
        for (PannaEntry e : entries) a.put(e.json());
        return new JSONObject().put("format", "panna-reader").put("version", 4).put("entries", a).toString(2);
    }
    public void restore(String source) throws JSONException {
        JSONObject root = new JSONObject(source);
        if (!"panna-reader".equals(root.optString("format")) || (root.optInt("version") < 1 || root.optInt("version") > 4))
            throw new JSONException("Expected a Panna Reader backup in format version 1, 2, 3 or 4");
        JSONArray a = root.getJSONArray("entries");
        if (a.length() > 500) throw new JSONException("Maximum 500 entries");
        ArrayList<PannaEntry> next = new ArrayList<>();
        HashSet<String> ids = new HashSet<>();
        for (int i = 0; i < a.length(); i++) {
            PannaEntry e = PannaEntry.from(a.getJSONObject(i));
            if (!ids.add(e.id)) throw new JSONException("Duplicate entry ID");
            next.add(e);
        }
        entries.clear(); entries.addAll(next);
    }
    public void save() throws Exception {
        byte[] bytes = backup().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BACKUP_BYTES) throw new IOException("Library is full. Export a backup, then permanently remove items from Trash.");
        FileOutputStream out = null;
        try { out = disk.startWrite(); out.write(bytes); disk.finishWrite(out); }
        catch (Exception e) { if (out != null) disk.failWrite(out); throw e; }
    }
    public PannaEntry find(String id) {
        for (PannaEntry e : entries) if (e.id.equals(id)) return e;
        return null;
    }
    public static String read(InputStream in, int limit) throws IOException {
        if (in == null) throw new IOException("Cannot open document");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192]; int n;
        while ((n = in.read(buffer)) != -1) {
            if (out.size() + n > limit) throw new IOException("File exceeds size limit");
            out.write(buffer, 0, n);
        }
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(out.toByteArray()))
                .toString().replaceFirst("^\\uFEFF", "");
        } catch (java.nio.charset.CharacterCodingException e) { throw new IOException("Please choose a UTF-8 text file", e); }
    }
}
