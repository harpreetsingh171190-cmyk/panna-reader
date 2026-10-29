package com.amrik.panna;

import org.json.JSONException;
import org.json.JSONObject;

/** A named UTF-16 position within a reading. */
public final class PannaBookmark {
    public final String name;
    public final int offset;
    public PannaBookmark(String name, int offset) { this.name = name; this.offset = offset; }
    public JSONObject json() throws JSONException { return new JSONObject().put("name", name).put("offset", offset); }
    public static PannaBookmark from(JSONObject data, int length) throws JSONException {
        String name = data.getString("name").trim();
        int offset = data.getInt("offset");
        if (name.isEmpty() || name.length() > 80 || offset < 0 || offset > length)
            throw new JSONException("Invalid bookmark");
        return new PannaBookmark(name, offset);
    }
}
