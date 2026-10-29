package com.harpreet.panna;

import org.json.JSONException;
import org.json.JSONObject;

public final class PannaHighlight {
    public final int start, end;
    public String note;
    public int color;
    public PannaHighlight(int start, int end, String note, int color) {
        this.start = start; this.end = end; this.note = note; this.color = color;
    }
    public JSONObject json() throws JSONException {
        return new JSONObject().put("start", start).put("end", end).put("note", note).put("color", color);
    }
    public static PannaHighlight from(JSONObject data, String text) throws JSONException {
        int start = data.getInt("start"), end = data.getInt("end"), color = data.getInt("color");
        String note = data.getString("note");
        if (!PannaStudy.validRange(text, start, end) || color < 0 || color > 2 || note.length() > 3000)
            throw new JSONException("Invalid highlight");
        return new PannaHighlight(start, end, note, color);
    }
}
