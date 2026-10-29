package com.harpreet.panna;

import org.json.JSONObject;
import org.json.JSONArray;
import java.util.ArrayList;
import org.json.JSONException;
import java.util.UUID;

public final class PannaEntry {
    public String id = UUID.randomUUID().toString();
    public String title = "", text = "", note = "", tag = "";
    public String author = "", sourceUrl = "", published = "";
    public final ArrayList<String> collections = new ArrayList<>();
    public boolean favorite, pinned, archived, trashed;
    public int offset;
    public final ArrayList<PannaHighlight> highlights = new ArrayList<>();
    public final ArrayList<PannaBookmark> bookmarks = new ArrayList<>();
    public long updated = System.currentTimeMillis();
    private String countedText; private int countedWords;
    /** Word count cached per text instance; recalculates automatically when text is replaced. */
    public int words() {
        if (countedText != text) { countedWords = PannaText.words(text); countedText = text; }
        return countedWords;
    }
    public int minutes() { return Math.max(1, (words() + 199) / 200); }
    public JSONObject json() throws JSONException {
        JSONArray marks = new JSONArray();
        for (PannaBookmark b : bookmarks) marks.put(b.json());
        JSONArray highlightsJson = new JSONArray();
        for (PannaHighlight h : highlights) highlightsJson.put(h.json());
        return new JSONObject().put("author", author).put("sourceUrl", sourceUrl).put("published", published).put("collections", new JSONArray(collections)).put("highlights", highlightsJson).put("pinned", pinned).put("archived", archived).put("trashed", trashed).put("bookmarks", marks).put("id", id).put("title", title).put("text", text)
            .put("note", note).put("tag", tag).put("favorite", favorite)
            .put("offset", offset).put("updated", updated);
    }
    public static PannaEntry from(JSONObject o) throws JSONException {
        PannaEntry e = new PannaEntry();
        e.id = o.getString("id"); e.title = o.getString("title"); e.text = o.getString("text");
        if (e.id.isEmpty() || e.title.trim().isEmpty() || e.title.length() > 240 ||
            e.text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > PannaText.MAX_TEXT_BYTES)
            throw new JSONException("Invalid reading entry");
        e.author = o.optString("author", ""); e.sourceUrl = o.optString("sourceUrl", ""); e.published = o.optString("published", "");
        if (e.author.length() > 240 || !PannaResearch.validSource(e.sourceUrl) || !PannaResearch.validDate(e.published)) throw new JSONException("Invalid source metadata");
        if (o.has("collections")) {
            JSONArray names = o.getJSONArray("collections");
            if (names.length() > 10) throw new JSONException("Maximum 10 collections per reading");
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                if (!PannaResearch.validCollection(name) || e.collections.contains(name)) throw new JSONException("Invalid or duplicate collection name");
                e.collections.add(name);
            }
        }
        e.note = o.optString("note", ""); e.tag = o.optString("tag", "");
        if (e.note.length() > 20000 || e.tag.length() > 80) throw new JSONException("Entry fields too long");
        for (String flag : new String[]{"pinned", "archived", "trashed"})
            if (o.has(flag) && !(o.get(flag) instanceof Boolean)) throw new JSONException("Invalid " + flag + " flag");
        e.pinned = o.optBoolean("pinned", false); e.archived = o.optBoolean("archived", false); e.trashed = o.optBoolean("trashed", false);
        e.favorite = o.optBoolean("favorite", false);
        e.offset = PannaText.clamp(o.optInt("offset", 0), 0, e.text.length());
        e.updated = Math.max(0, o.optLong("updated", 0));
        if (o.has("bookmarks")) {
            JSONArray marks = o.getJSONArray("bookmarks");
            if (marks.length() > 100) throw new JSONException("Maximum 100 bookmarks per reading");
            for (int i = 0; i < marks.length(); i++) e.bookmarks.add(PannaBookmark.from(marks.getJSONObject(i), e.text.length()));
        }
        if (o.has("highlights")) {
            JSONArray ranges = o.getJSONArray("highlights");
            if (ranges.length() > 200) throw new JSONException("Maximum 200 highlights per reading");
            for (int i = 0; i < ranges.length(); i++) {
                PannaHighlight h = PannaHighlight.from(ranges.getJSONObject(i), e.text);
                for (PannaHighlight old : e.highlights)
                    if (PannaStudy.overlaps(h.start, h.end, old.start, old.end)) throw new JSONException("Overlapping highlights");
                e.highlights.add(h);
            }
        }
        return e;
    }
}
