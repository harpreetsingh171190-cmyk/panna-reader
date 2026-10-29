package com.amrik.panna;

import android.app.*;
import android.os.Build;
import android.os.Bundle;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.*;
import android.text.style.BackgroundColorSpan;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Fully native, offline library and focused reader. No network or storage permissions. */
public final class PannaLibraryActivity extends Activity {
    private static final int HIGHLIGHT_ACTION = 7001;
    private static final int IMPORT = 10, EXPORT = 11, RESTORE = 12, EXPORT_TEXT = 13, MERGE = 14, EXPORT_CSV = 15;
    private PannaShelf shelf;
    private PannaVoice voice;
    private String pendingTextExport;
    private SharedPreferences prefs;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private LinearLayout root, cards, libraryActions;
    private final HashSet<String> selectedIds = new HashSet<>();
    private final ArrayList<String> visibleIds = new ArrayList<>();
    private ScrollView readingScroll;
    private TextView readingBody, readingProgress;
    private PannaEntry current;
    private String query = "", filter = "All", order = "Recent", tagFilter, collectionFilter;
    private boolean dark, busy, loadFailed;
    private int bg, surface, ink, muted, accent;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        voice = new PannaVoice(this, this::message);
        prefs = getSharedPreferences("panna_preferences", MODE_PRIVATE);
        dark = prefs.getBoolean("dark", false);
        shelf = new PannaShelf(this);
        try { shelf.load(); }
        catch (Exception e) { loadFailed = true; }
        if (!loadFailed && shelf.entries.isEmpty() && !prefs.getBoolean("welcomed", false)) {
            PannaEntry welcome = new PannaEntry();
            welcome.title = "A little space to read"; welcome.tag = "Start here";
            welcome.text = "Welcome to Panna Reader\n\nYour quiet, offline reading space.\n\nTap Add to import a UTF-8 TXT or Markdown file, or paste something you want to read. Markdown appears as plain text.\n\nOpen a card to read. Your place is remembered automatically. Use the star to keep favorites, and Notes to save your thoughts or a tag.\n\nUse A− and A+ to change the type size. Settings includes a dark theme, serif type, and backup tools.\n\nA backup includes your library, notes, favorites and reading positions. Keep a copy somewhere safe before removing the app. Backups are plain JSON: anyone with the file can read them.\n\nTake your time. One page is enough to begin.";
            shelf.entries.add(welcome);
            if (persist()) prefs.edit().putBoolean("welcomed", true).apply();
        }
        if (state != null) {
            String[] selectedState = state.getStringArray("batch_ids"); if (selectedState != null) selectedIds.addAll(Arrays.asList(selectedState));
            query = state.getString("query", ""); filter = state.getString("filter", "All");
            order = state.getString("order", "Recent"); tagFilter = state.getString("tag_filter"); collectionFilter = state.getString("collection_filter");
        }
        String selected = state == null ? null : state.getString("reading_id");
        if (selected != null) { current = shelf.find(selected); if (current != null && current.trashed) current = null; }
        if (current != null) showReader(); else showLibrary();
        if (loadFailed) new AlertDialog.Builder(this).setTitle("Library could not be opened")
            .setMessage("Your stored library has been left untouched. Close and retry, or use Settings to restore a backup. Changes are disabled until a backup is restored.")
            .setPositiveButton("OK", null).show();
        if (state == null) receiveSharedText(getIntent());
    }
    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private void palette() {
        bg = Color.parseColor(dark ? "#101C1B" : "#F5F4EE");
        surface = Color.parseColor(dark ? "#1B2C29" : "#FFFFFF");
        ink = Color.parseColor(dark ? "#EDF2E6" : "#173D36");
        muted = Color.parseColor(dark ? "#ACBFB7" : "#61746C");
        accent = Color.parseColor(dark ? "#BDDF9E" : "#246D5C");
        if (!dark && prefs.getBoolean("sepia", false)) {
            bg = Color.parseColor("#F2E6CD"); surface = Color.parseColor("#FFF3DC");
            ink = Color.parseColor("#513F2D"); muted = Color.parseColor("#78644B"); accent = Color.parseColor("#805633");
        }
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        int uiFlags = dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (!dark && Build.VERSION.SDK_INT >= 26) uiFlags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(uiFlags);
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private GradientDrawable box(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private TextView text(String s, int size, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setPadding(0, dp(5), 0, dp(5)); return t;
    }
    private Button button(String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(ink);
        b.setMinWidth(0); b.setMinimumWidth(0); b.setMinHeight(dp(48)); b.setTextSize(13); b.setOnClickListener(v -> action.run()); return b;
    }
    private void gap(LinearLayout l, int n) { View v = new View(this); l.addView(v, new LinearLayout.LayoutParams(1, dp(n))); }
    private void shell() {
        palette(); root = column(); root.setBackgroundColor(bg); root.setPadding(dp(18), dp(12), dp(18), dp(4)); setContentView(root);
    }
    private EditText input(String hint, String value, boolean multi) {
        EditText e = new EditText(this); e.setHint(hint); e.setText(value); e.setTextColor(ink); e.setHintTextColor(muted);
        e.setTextSize(16); e.setSingleLine(!multi); e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setBackground(box(surface, 12));
        if (multi) { e.setGravity(Gravity.TOP); e.setMinLines(4); e.setMaxLines(9); }
        return e;
    }
    private void showLibrary() {
        voice.stop(); getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        current = null; readingScroll = null; shell();
        LinearLayout heading = row(); LinearLayout identity = column();
        TextView title = text("Panna Reader", 29, ink); title.setTypeface(null, Typeface.BOLD);
        identity.addView(title); identity.addView(text("A little space to read.", 14, muted));
        heading.addView(identity, new LinearLayout.LayoutParams(0, -2, 1));
        heading.addView(button("Settings", this::settings)); root.addView(heading); gap(root, 12);
        int saved = 0, started = 0;
        for (PannaEntry e : shelf.entries) if (!e.trashed) { saved++; if (e.offset > 0) started++; }
        TextView stats = text(saved + " saved  ·  " + started + " started  ·  View stats ›", 13, muted);
        stats.setMinHeight(dp(48)); stats.setGravity(Gravity.CENTER_VERTICAL);
        stats.setOnClickListener(v -> statistics()); stats.setFocusable(true); root.addView(stats); gap(root, 10);
        EditText search = input("Search · author:name · \"exact phrase\"", query, false); root.addView(search);
        search.setFilters(new InputFilter[]{new InputFilter.LengthFilter(500)});
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int f) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) { query = s.toString(); selectedIds.clear(); renderCards(); }
            public void afterTextChanged(Editable e) {}
        });
        LinearLayout controls = row();
        controls.addView(button("Filter: " + filter, this::chooseFilter), new LinearLayout.LayoutParams(0, -2, 1));
        controls.addView(button("Sort: " + order, this::chooseOrder), new LinearLayout.LayoutParams(0, -2, 1));
        controls.addView(button(tagFilter == null ? "Tags: all" : tagFilter.isEmpty() ? "Untagged" : "Tag: " + tagFilter, this::chooseTags), new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(controls);
        LinearLayout research = row();
        research.addView(button(collectionFilter == null ? "Collections: all" : collectionFilter.isEmpty() ? "Unfiled" : "Collection: " + collectionFilter, this::chooseCollectionFilter), new LinearLayout.LayoutParams(0, -2, 1));
        research.addView(button("Search help", this::searchHelp), new LinearLayout.LayoutParams(0, -2, 1)); root.addView(research);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); cards = column(); scroll.addView(cards);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        libraryActions = column(); root.addView(libraryActions); renderCards();
    }
    private void chooseFilter() {
        String[] options = {"All", "Favorites", "Pinned", "Unread", "In progress", "Finished", "Archived", "Trash"};
        new AlertDialog.Builder(this).setTitle("Show entries").setItems(options, (d, i) -> { selectedIds.clear(); filter = options[i]; tagFilter = null; collectionFilter = null; query = ""; showLibrary(); }).show();
    }
    private void chooseOrder() {
        String[] options = {"Recent", "Title", "Shortest"};
        new AlertDialog.Builder(this).setTitle("Sort library").setItems(options, (d, i) -> { order = options[i]; showLibrary(); }).show();
    }
    private void renderCards() {
        if (cards == null) return;
        cards.removeAllViews(); ArrayList<PannaEntry> found = new ArrayList<>();
        final PannaSearch.Query compiled;
        try { compiled = PannaSearch.parse(query); }
        catch (IllegalArgumentException error) {
            visibleIds.clear(); selectedIds.clear(); cards.addView(text(error.getMessage(), 15, muted)); refreshLibraryActions(); return;
        }
        for (PannaEntry e : shelf.entries) {
            double p = PannaText.progress(e.offset, e.text.length());
            if (!PannaCatalog.inSection(e.trashed, e.archived, filter) || !PannaCatalog.tagMatches(e.tag, tagFilter)) continue;
            if (filter.equals("Pinned") && !e.pinned) continue;
            if (!PannaResearch.inCollection(e.collections, collectionFilter) || !compiled.matches(searchDocument(e))) continue;
            if (filter.equals("Favorites") && !e.favorite || filter.equals("Unread") && e.offset != 0 ||
                filter.equals("In progress") && (p <= 0 || p >= 1) || filter.equals("Finished") && p < 1) continue;
            found.add(e);
        }
        Collections.sort(found, (a,b) -> {
            int rank = PannaCatalog.pinPriority(a.pinned, b.pinned);
            if (rank != 0) return rank;
            int compared = order.equals("Title") ? a.title.compareToIgnoreCase(b.title)
                : order.equals("Shortest") ? Integer.compare(a.text.length(), b.text.length()) : Long.compare(b.updated, a.updated);
            return compared == 0 ? a.id.compareTo(b.id) : compared;
        });
        visibleIds.clear(); for (PannaEntry e : found) visibleIds.add(e.id); selectedIds.retainAll(visibleIds);
        if (found.isEmpty()) { gap(cards, 35); cards.addView(text(filter.equals("Trash") ? (query.isEmpty() && tagFilter == null && collectionFilter == null ? "Trash is empty" : "No matching items in Trash") : "No matching readings", 23, ink)); cards.addView(text("Try another search, tag or filter. Add a text file or paste an article to get started.", 15, muted)); }
        for (PannaEntry e : found) {
            LinearLayout card = column(); card.setPadding(dp(18), dp(13), dp(18), dp(13)); card.setBackground(box(surface, 18));
            if (selectedIds.contains(e.id)) ((GradientDrawable) card.getBackground()).setStroke(dp(2), accent);
            String label = e.tag.isEmpty() ? "YOUR LIBRARY" : e.tag;
            card.addView(text((selectedIds.contains(e.id) ? "SELECTED · " : "") + (e.pinned ? "PINNED · " : "") + (e.archived ? "ARCHIVED · " : "") + (e.favorite ? "★  " : "") + label, 11, accent));
            TextView t = text(e.title, 21, ink); t.setTypeface(null, Typeface.BOLD); card.addView(t);
            if (!e.author.isEmpty() || !e.published.isEmpty()) card.addView(text(e.author + (e.author.isEmpty() || e.published.isEmpty() ? "" : " · ") + e.published, 12, muted));
            if (!e.collections.isEmpty()) card.addView(text("Collections: " + TextUtils.join(" · ", e.collections), 11, accent));
            TextView excerpt = text(e.text.replace('\n', ' '), 14, muted); excerpt.setMaxLines(2); excerpt.setEllipsize(TextUtils.TruncateAt.END); card.addView(excerpt);
            int percent = (int) Math.round(PannaText.progress(e.offset, e.text.length()) * 100);
            card.addView(text(e.minutes() + " min read  ·  " + percent + "% read" + (e.note.isEmpty() ? "" : "  ·  note"), 12, accent));
            card.setOnLongClickListener(v -> { toggleSelected(e.id); return true; });
            card.setOnClickListener(v -> { if (!selectedIds.isEmpty()) { toggleSelected(e.id); return; } if (e.trashed) trashActions(e); else { current = e; showReader(); } }); card.setContentDescription((selectedIds.contains(e.id) ? "Selected: " : "") + (e.trashed ? "Restore or delete " : "Read ") + e.title);
            card.setSelected(selectedIds.contains(e.id)); card.setFocusable(true); card.setClickable(true);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.setMargins(0, dp(5), 0, dp(9)); cards.addView(card, params);
        }
        refreshLibraryActions();
    }
    private void toggleSelected(String id) {
        if (!selectedIds.add(id)) selectedIds.remove(id);
        renderCards();
    }
    private void refreshLibraryActions() {
        if (libraryActions == null) return;
        libraryActions.removeAllViews();
        if (selectedIds.isEmpty()) {
            libraryActions.addView(text("Hold a card to select readings for batch actions.", 11, muted));
            Button add = button("+ Add to your library", this::addMenu);
            add.setTextColor(dark ? bg : Color.WHITE); add.setBackground(box(accent, 15));
            libraryActions.addView(add, new LinearLayout.LayoutParams(-1, dp(54)));
        } else {
            LinearLayout bar = row();
            bar.addView(button("Actions (" + selectedIds.size() + ")", this::batchMenu), new LinearLayout.LayoutParams(0, -2, 1));
            bar.addView(button("Select all", () -> { selectedIds.addAll(visibleIds); renderCards(); }), new LinearLayout.LayoutParams(0, -2, 1));
            bar.addView(button("Cancel", () -> { selectedIds.clear(); renderCards(); }), new LinearLayout.LayoutParams(0, -2, 1));
            libraryActions.addView(bar);
        }
    }
    private void batchMenu() {
        if (!writable() || selectedIds.isEmpty()) return;
        if (filter.equals("Trash")) {
            new AlertDialog.Builder(this).setTitle("Selected readings: " + selectedIds.size())
                .setItems(new String[]{"Restore selected"}, (d,i) -> applyBatch("restore", null)).show();
            return;
        }
        String[] labels = {"Favorite selected", "Remove favorites", "Pin selected", "Unpin selected", "Archive selected", "Return from Archive", "Set tag", "Move selected to Trash", "Add to collection", "Remove from collection"};
        String[] operations = {"favorite", "unfavorite", "pin", "unpin", "archive", "unarchive", "tag", "trash"};
        new AlertDialog.Builder(this).setTitle("Selected readings: " + selectedIds.size()).setItems(labels, (d,i) -> {
            if (i == 6) {
                EditText tag = input("Tag (leave empty to clear)", "", false);
                tag.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
                new AlertDialog.Builder(this).setTitle("Tag selected readings").setView(tag).setNegativeButton("Cancel", null)
                    .setPositiveButton("Save tag", (a,b) -> applyBatch("tag", tag.getText().toString().trim())).show();
            } else if (i == 8) chooseCollectionName(name -> applyBatch("add_collection", name));
            else if (i == 9) chooseExistingCollection(name -> applyBatch("remove_collection", name));
            else applyBatch(operations[i], null);
        }).show();
    }
    private void applyBatch(String operation, String tag) {
        if (!writable()) return;
        if (operation.equals("add_collection")) for (PannaEntry e : shelf.entries)
            if (selectedIds.contains(e.id) && !e.trashed && !e.collections.contains(tag) && e.collections.size() >= 10) {
                message("One selected reading already has 10 collections. Nothing was changed."); return;
            }
        String before;
        try { before = shelf.backup(); } catch (Exception e) { message("Could not prepare batch changes."); return; }
        int affected = 0;
        for (PannaEntry e : shelf.entries) if (selectedIds.contains(e.id)) {
            if (operation.equals("restore")) { if (!e.trashed) continue; e.trashed = false; }
            else {
                if (e.trashed) continue;
                switch (operation) {
                    case "favorite": e.favorite = true; break;
                    case "unfavorite": e.favorite = false; break;
                    case "pin": e.pinned = true; break;
                    case "unpin": e.pinned = false; break;
                    case "archive": e.archived = true; break;
                    case "unarchive": e.archived = false; break;
                    case "tag": e.tag = tag == null ? "" : tag; break;
                    case "add_collection": if (!e.collections.contains(tag)) e.collections.add(tag); break;
                    case "remove_collection": e.collections.remove(tag); break;
                    case "trash": e.trashed = true; break;
                    default: continue;
                }
            }
            affected++;
        }
        if (persist()) {
            selectedIds.clear(); showLibrary(); Toast.makeText(this, affected + " readings updated", Toast.LENGTH_LONG).show();
        } else {
            try { shelf.restore(before); } catch (Exception e) { loadFailed = true; message("Could not restore the in-memory shelf. Reopen the app before editing."); }
            selectedIds.clear(); showLibrary();
        }
    }
    private boolean writable() {
        if (loadFailed) { message("Restore a valid backup before editing this library."); return false; }
        return true;
    }
    private boolean persist() {
        if (loadFailed) return false;
        try { shelf.save(); return true; }
        catch (Exception e) { message("Could not save: " + e.getMessage()); return false; }
    }
    private void message(String s) { new AlertDialog.Builder(this).setMessage(s).setPositiveButton("OK", null).show(); }
    private void addMenu() {
        if (!writable()) return;
        new AlertDialog.Builder(this).setTitle("Add a reading").setItems(new String[]{"Import TXT / Markdown", "Paste or write text"}, (d, i) -> {
            if (i == 0) pick(IMPORT, "text/*"); else editEntry(null, "", "");
        }).show();
    }
    private void editEntry(PannaEntry existing, String title, String body) {
        if (!writable()) return;
        LinearLayout fields = column(); fields.setPadding(dp(20), dp(12), dp(20), 0);
        EditText name = input("Title", title, false), content = input("Paste text here", body, true);
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(240)});
        fields.addView(name); gap(fields, 10); fields.addView(content);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(existing == null ? "New reading" : "Edit reading")
            .setMessage(existing == null ? "Save something for later." : "Changing the text resets progress and removes its bookmarks and highlights.")
            .setView(fields).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(-1).setOnClickListener(v -> {
            String n = name.getText().toString().trim(), t = content.getText().toString();
            if (n.isEmpty() || t.trim().isEmpty()) { name.setError("A title and text are required"); return; }
            if (n.length() > 240) { name.setError("Maximum 240 characters in the title"); return; }
            if (t.getBytes(StandardCharsets.UTF_8).length > PannaText.MAX_TEXT_BYTES) { content.setError("Maximum 2 MiB of UTF-8 text"); return; }
            if (existing == null && shelf.entries.size() >= 500) { message("Maximum 500 stored entries, including Trash. Export a backup, then permanently delete items from Trash to free space."); return; }
            PannaEntry e = existing == null ? new PannaEntry() : existing;
            String oldTitle = e.title, oldText = e.text; int oldOffset = e.offset; long oldUpdated = e.updated;
            ArrayList<PannaBookmark> oldMarks = new ArrayList<>(e.bookmarks);
            ArrayList<PannaHighlight> oldHighlights = new ArrayList<>(e.highlights);
            e.title = n; e.text = t; e.updated = System.currentTimeMillis();
            // Edited text has different character positions: restart instead of restoring a stale offset.
            if (!oldText.equals(t)) { e.offset = 0; e.bookmarks.clear(); e.highlights.clear(); }
            if (existing == null) shelf.entries.add(e);
            if (persist()) { dialog.dismiss(); if (existing == null) { filter = "All"; query = ""; tagFilter = null; collectionFilter = null; } if (current != null) showReader(); else showLibrary(); }
            else { if (existing == null) shelf.entries.remove(e); e.title = oldTitle; e.text = oldText; e.offset = oldOffset; e.updated = oldUpdated; e.bookmarks.clear(); e.bookmarks.addAll(oldMarks); e.highlights.clear(); e.highlights.addAll(oldHighlights); }
        })); dialog.show();
    }
    private void showReader() {
        selectedIds.clear(); voice.stop(); shell(); final PannaEntry e = current;
        if (prefs.getBoolean("keep_awake", false)) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout bar = row(); bar.addView(button("‹ Library", () -> { savePosition(); showLibrary(); }), new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(button(e.favorite ? "★ Saved" : "☆ Save", () -> {
            if (!writable()) return; savePosition(); e.favorite = !e.favorite;
            if (!persist()) e.favorite = !e.favorite; showReader();
        })); bar.addView(button("More", this::readerMenu)); root.addView(bar);
        TextView title = text(e.title, 26, ink); title.setTypeface(null, Typeface.BOLD); title.setMaxLines(3); root.addView(title);
        root.addView(text(e.words() + " words  ·  " + e.minutes() + " min read", 12, muted));
        LinearLayout tools = row();
        tools.addView(button("A−", () -> resize(-2)), new LinearLayout.LayoutParams(0, -2, 1));
        tools.addView(button("A+", () -> resize(2)), new LinearLayout.LayoutParams(0, -2, 1));
        tools.addView(button("Find", this::findInReading), new LinearLayout.LayoutParams(0, -2, 1));
        tools.addView(button("Notes", this::editNotes), new LinearLayout.LayoutParams(0, -2, 1)); root.addView(tools);
        LinearLayout extra = row();
        extra.addView(button("Bookmarks", this::bookmarks), new LinearLayout.LayoutParams(0, -2, 1));
        extra.addView(button("Listen", this::listenMenu), new LinearLayout.LayoutParams(0, -2, 1));
        extra.addView(button("Stop", () -> voice.stop()), new LinearLayout.LayoutParams(0, -2, 1)); root.addView(extra);
        readingProgress = text("", 12, accent); root.addView(readingProgress);
        readingScroll = new ScrollView(this); readingBody = text(e.text, prefs.getInt("font", 20), ink);
        readingBody.setTextIsSelectable(true); installSelectionActions(); readingBody.setPadding(dp(3), dp(12), dp(3), dp(70));
        readingBody.setLineSpacing(dp(7), 1.15f);
        readingBody.setTypeface(prefs.getBoolean("serif", true) ? Typeface.SERIF : Typeface.DEFAULT);
        readingScroll.addView(readingBody); root.addView(readingScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        renderReadingText(null);
        final int savedOffset = e.offset;
        readingScroll.post(() -> {
            if (current != e || readingBody.getLayout() == null) return;
            if (savedOffset >= e.text.length() && savedOffset > 0) readingScroll.fullScroll(View.FOCUS_DOWN);
            else { int line = readingBody.getLayout().getLineForOffset(PannaText.clamp(savedOffset, 0, e.text.length())); readingScroll.scrollTo(0, readingBody.getLayout().getLineTop(line)); }
            updateProgress();
        });
        readingScroll.setOnScrollChangeListener((v,x,y,ox,oy) -> updateProgress());
    }
    private void updateProgress() {
        if (readingProgress != null && current != null)
            readingProgress.setText(Math.round(PannaText.progress(position(), current.text.length()) * 100) + "% read  ·  Reading progress");
    }
    private int position() {
        if (current == null || readingScroll == null || readingBody.getLayout() == null) return current == null ? 0 : current.offset;
        if (readingScroll.getScrollY() + readingScroll.getHeight() >= readingBody.getHeight() - dp(4)) {
            // Short readings remain unread until explicitly marked finished or scrolled.
            if (readingBody.getHeight() > readingScroll.getHeight()) return current.text.length();
            return current.offset;
        }
        int y = Math.max(0, readingScroll.getScrollY() - readingBody.getTotalPaddingTop());
        return readingBody.getLayout().getLineStart(readingBody.getLayout().getLineForVertical(y));
    }
    private void savePosition() {
        if (current == null || loadFailed) return;
        int p = position();
        if (p != current.offset) { current.offset = p; current.updated = System.currentTimeMillis(); persist(); }
    }
    private void resize(int delta) {
        savePosition(); prefs.edit().putInt("font", PannaText.clamp(prefs.getInt("font", 20) + delta, 14, 34)).apply(); showReader();
    }
    private void readerMenu() {
        new AlertDialog.Builder(this).setTitle("Reading tools").setItems(new String[]{
            "Mark finished", "Start again", "Edit text", "Share text", "Export TXT", "Export study notes",
            current.pinned ? "Unpin reading" : "Pin reading",
            current.archived ? "Return from Archive" : "Archive reading", "Move to Trash", "Highlights & annotations", "Contents (Markdown headings)", "Source details", "Collections", "Open source in browser"}, (d,i) -> {
            if (i == 3) { share(); return; }
            if (i == 4) { exportReading(); return; }
            if (i == 5) { exportNotes(); return; }
            if (i == 9) { highlightsMenu(); return; }
            if (i == 10) { contentsMenu(); return; }
            if (i == 11) { editSource(); return; }
            if (i == 12) { readingCollections(); return; }
            if (i == 13) { openSource(); return; }
            if (!writable()) return;
            if (i == 0 || i == 1) {
                int before = current.offset; current.offset = i == 0 ? current.text.length() : 0;
                if (!persist()) current.offset = before; showReader();
            } else if (i == 2) { savePosition(); editEntry(current, current.title, current.text); }
            else if (i == 6) {
                savePosition(); current.pinned = !current.pinned;
                if (!persist()) current.pinned = !current.pinned; showReader();
            } else if (i == 7) {
                savePosition(); current.archived = !current.archived;
                if (persist()) { showLibrary(); Toast.makeText(this, "Archive updated. Use Filter to browse archived readings.", Toast.LENGTH_LONG).show(); }
                else current.archived = !current.archived;
            } else if (i == 8) {
                savePosition(); current.trashed = true;
                if (persist()) { showLibrary(); Toast.makeText(this, "Moved to Trash. Use Filter → Trash to restore.", Toast.LENGTH_LONG).show(); }
                else current.trashed = false;
            }
        }).show();
    }
    private void trashActions(PannaEntry entry) {
        new AlertDialog.Builder(this).setTitle(entry.title).setItems(new String[]{"Restore reading", "Delete permanently"}, (d,i) -> {
            if (!writable()) return;
            if (i == 0) {
                entry.trashed = false;
                if (persist()) { showLibrary(); Toast.makeText(this, entry.archived ? "Restored to Archived" : "Restored to the library", Toast.LENGTH_LONG).show(); }
                else entry.trashed = true;
            } else new AlertDialog.Builder(this).setTitle("Permanently delete this reading?")
                .setMessage("Its text, notes, bookmarks and highlights will be removed from this device. This cannot be undone without an earlier backup.")
                .setNegativeButton("Cancel", null).setPositiveButton("Delete permanently", (a,b) -> {
                    int index = shelf.entries.indexOf(entry); if (index < 0) return;
                    shelf.entries.remove(index);
                    if (persist()) showLibrary(); else shelf.entries.add(index, entry);
                }).show();
        }).show();
    }
    private void chooseTags() {
        TreeSet<String> tags = new TreeSet<>();
        for (PannaEntry entry : shelf.entries) if (PannaCatalog.inSection(entry.trashed, entry.archived, filter) && !entry.tag.isEmpty()) tags.add(entry.tag);
        ArrayList<String> labels = new ArrayList<>(); labels.add("All tags"); labels.add("Untagged"); labels.addAll(tags);
        new AlertDialog.Builder(this).setTitle("Browse by tag").setItems(labels.toArray(new String[0]), (d,i) -> {
            selectedIds.clear(); tagFilter = i == 0 ? null : i == 1 ? "" : labels.get(i); query = ""; showLibrary();
        }).show();
    }
    private void editNotes() {
        if (!writable()) return;
        savePosition(); LinearLayout fields = column(); fields.setPadding(dp(20), dp(12), dp(20), 0);
        EditText tag = input("Tag / topic", current.tag, false), note = input("Your reading notes", current.note, true);
        tag.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)}); note.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20000)});
        fields.addView(tag); gap(fields, 10); fields.addView(note);
        new AlertDialog.Builder(this).setTitle("Notes & tag").setView(fields).setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d,i) -> {
                String oldTag = current.tag, oldNote = current.note;
                current.tag = tag.getText().toString().trim(); current.note = note.getText().toString();
                if (!persist()) { current.tag = oldTag; current.note = oldNote; }
            }).show();
    }
    private void findInReading() {
        EditText find = input("Word or phrase", "", false);
        new AlertDialog.Builder(this).setTitle("Find in reading").setView(find).setNegativeButton("Cancel", null)
            .setNeutralButton("Clear search marks", (d,i) -> renderReadingText(null))
            .setPositiveButton("Find", (d,i) -> {
                String query = find.getText().toString(); if (query.isEmpty()) return;
                int first = renderReadingText(query);
                if (first < 0) Toast.makeText(this, "No matches", Toast.LENGTH_SHORT).show(); else jumpTo(first);
            }).show();
    }
    private int renderReadingText(String query) {
        if (current == null || readingBody == null) return -1;
        SpannableString marked = new SpannableString(current.text);
        int[] colors = dark ? new int[]{Color.rgb(91,79,28), Color.rgb(35,79,51), Color.rgb(34,63,101)}
            : new int[]{Color.rgb(255,238,145), Color.rgb(190,231,196), Color.rgb(195,222,252)};
        for (PannaHighlight h : current.highlights) marked.setSpan(new BackgroundColorSpan(colors[h.color]), h.start, h.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int first = -1, count = 0;
        if (query != null && !query.isEmpty()) for (int p = 0; p <= current.text.length() - query.length(); p++) {
            if (current.text.regionMatches(true, p, query, 0, query.length())) {
                if (first < 0) first = p;
                marked.setSpan(new BackgroundColorSpan(dark ? Color.rgb(115,64,47) : Color.rgb(255,201,160)), p, p + query.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                count++; p += query.length() - 1;
            }
        }
        int y = readingScroll == null ? 0 : readingScroll.getScrollY();
        readingBody.setText(marked);
        // Text changes preserve viewport without a posted scroll racing with Find/Contents jumps.
        if (readingScroll != null) readingScroll.scrollTo(0, y);
        if (query != null && count > 0) Toast.makeText(this, count + " matches highlighted", Toast.LENGTH_SHORT).show();
        return first;
    }
    private void jumpTo(int offset) {
        final PannaEntry entry = current;
        readingBody.post(() -> {
            if (current != entry || readingBody == null || readingBody.getLayout() == null || readingScroll == null) return;
            int line = readingBody.getLayout().getLineForOffset(PannaText.clamp(offset, 0, entry.text.length()));
            readingScroll.smoothScrollTo(0, readingBody.getLayout().getLineTop(line));
        });
    }
    private void installSelectionActions() {
        readingBody.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                menu.add(Menu.NONE, HIGHLIGHT_ACTION, 10, "Highlight & note"); return true;
            }
            @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
            @Override public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() != HIGHLIGHT_ACTION) return false;
                int a = readingBody.getSelectionStart(), b = readingBody.getSelectionEnd();
                int start = Math.min(a, b), end = Math.max(a, b); mode.finish();
                if (current != null && writable() && PannaStudy.validRange(current.text, start, end)) editHighlight(null, start, end);
                return true;
            }
            @Override public void onDestroyActionMode(ActionMode mode) {}
        });
    }
    private void editHighlight(PannaHighlight existing, int start, int end) {
        if (!writable()) return;
        if (existing == null) {
            if (current.highlights.size() >= 200) { message("Maximum 200 highlights per reading."); return; }
            for (PannaHighlight h : current.highlights) if (PannaStudy.overlaps(start, end, h.start, h.end)) {
                message("This overlaps a saved highlight. Open More → Highlights & annotations to edit or remove it first."); return;
            }
        }
        LinearLayout fields = column(); fields.setPadding(dp(18), dp(10), dp(18), 0);
        TextView quote = text(current.text.substring(start, Math.min(end, start + 180)), 15, ink); quote.setMaxLines(4); fields.addView(quote);
        EditText note = input("Optional note about this passage", existing == null ? "" : existing.note, true);
        note.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3000)}); fields.addView(note);
        Spinner color = new Spinner(this);
        color.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Yellow", "Green", "Blue"}));
        color.setSelection(existing == null ? 0 : existing.color); fields.addView(color);
        new AlertDialog.Builder(this).setTitle(existing == null ? "Save highlight" : "Edit annotation").setView(fields)
            .setNegativeButton("Cancel", null).setPositiveButton("Save", (d,i) -> {
                PannaHighlight h = existing == null ? new PannaHighlight(start, end, "", 0) : existing;
                String beforeNote = h.note; int beforeColor = h.color;
                h.note = note.getText().toString(); h.color = color.getSelectedItemPosition();
                if (existing == null) current.highlights.add(h);
                if (!persist()) { if (existing == null) current.highlights.remove(h); h.note = beforeNote; h.color = beforeColor; }
                renderReadingText(null);
            }).show();
    }
    private void highlightsMenu() {
        if (current.highlights.isEmpty()) { message("No highlights yet. Long-press a passage, select text, then choose Highlight & note from the selection menu (or its overflow menu)."); return; }
        ArrayList<PannaHighlight> ordered = new ArrayList<>(current.highlights);
        Collections.sort(ordered, (a,b) -> Integer.compare(a.start, b.start));
        ArrayList<String> labels = new ArrayList<>();
        for (PannaHighlight h : ordered) {
            String quote = current.text.substring(h.start, Math.min(h.end, h.start + 90)).replace('\n', ' ');
            labels.add(quote + (h.note.isEmpty() ? "" : " · note"));
        }
        new AlertDialog.Builder(this).setTitle("Highlights & annotations").setItems(labels.toArray(new String[0]), (d,i) -> {
            PannaHighlight h = ordered.get(i);
            new AlertDialog.Builder(this).setTitle("Saved passage").setItems(new String[]{"Jump to passage", "Edit note / color", "Delete highlight"}, (a,b) -> {
                if (b == 0) jumpTo(h.start);
                if (b == 1) editHighlight(h, h.start, h.end);
                if (b == 2 && writable()) {
                    int index = current.highlights.indexOf(h); if (index < 0) return;
                    current.highlights.remove(index); if (!persist()) current.highlights.add(index, h); renderReadingText(null);
                }
            }).show();
        }).show();
    }
    private void contentsMenu() {
        List<PannaStudy.Heading> headings = PannaStudy.headings(current.text);
        if (headings.isEmpty()) { message("No Markdown headings found. Use # Heading, ## Subheading, or a heading followed by === / ---. Fenced code blocks are skipped."); return; }
        ArrayList<String> labels = new ArrayList<>();
        for (PannaStudy.Heading h : headings) labels.add((h.level > 1 ? "  ↳ " : "") + h.title);
        new AlertDialog.Builder(this).setTitle("Contents (up to 300 headings)").setItems(labels.toArray(new String[0]), (d,i) -> jumpTo(headings.get(i).offset)).show();
    }
    private void share() {
        if (current.text.length() > 100000) { message("This reading is too large to share as a message. Export your library instead."); return; }
        Intent send = new Intent(Intent.ACTION_SEND); send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, current.title); send.putExtra(Intent.EXTRA_TEXT, current.title + "\n\n" + current.text);
        startActivity(Intent.createChooser(send, "Share reading"));
    }
    private void settings() {
        new AlertDialog.Builder(this).setTitle("Panna settings").setItems(new String[]{
            "Theme: light / dark / sepia",
            prefs.getBoolean("serif", true) ? "Use sans serif type" : "Use serif type",
            "Export library backup", "Restore library backup", "Reading statistics",
            prefs.getBoolean("keep_awake", false) ? "Keep screen awake: on" : "Keep screen awake: off",
            "About Panna Reader", "Merge a backup into this library", "Browse tags", "Open Trash", "Browse collections", "Search help", "Export catalog CSV"}, (d,i) -> {
            if (i == 0) new AlertDialog.Builder(this).setTitle("Reading theme")
                .setItems(new String[]{"Light", "Dark", "Sepia"}, (a,b) -> {
                    dark = b == 1; prefs.edit().putBoolean("dark", dark).putBoolean("sepia", b == 2).apply(); showLibrary();
                }).show();
            if (i == 1) { prefs.edit().putBoolean("serif", !prefs.getBoolean("serif", true)).apply(); }
            if (i == 2) {
                if (loadFailed) { message("Restore or reopen your library before exporting."); return; }
                Intent out = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE);
                out.putExtra(Intent.EXTRA_TITLE, "PannaReader-backup.json"); launch(out, EXPORT);
            }
            if (i == 3) new AlertDialog.Builder(this).setTitle("Replace your library?")
                .setMessage("Restore replaces the entire library, including Trash, Archive, bookmarks, highlights and notes. Export a backup first. Only a Panna Reader JSON backup is accepted.")
                .setNegativeButton("Cancel", null).setPositiveButton("Choose backup", (a,b) -> pick(RESTORE, "*/*")).show();
            if (i == 4) statistics();
            if (i == 5) {
                boolean next = !prefs.getBoolean("keep_awake", false); prefs.edit().putBoolean("keep_awake", next).apply();
                message(next ? "The screen will stay awake while the reader is visible." : "The reader will follow your device screen timeout.");
            }
            if (i == 6) message("Panna Reader 1.4\nAn offline research library with source metadata, collections, advanced search, annotations and catalog exports.\n\nTXT and Markdown (plain text), UTF-8, up to 2 MiB per entry. Up to 500 entries; 24 MiB total library. No PDF or EPUB support.\n\nNo accounts, analytics or internet permission in Panna Reader. Read Aloud sends the reading to your selected Android voice engine; its language support, downloads and data handling depend on that engine. Offline speech requires installed offline voice data.\n\nBackups contain readable text.\n\nOriginal CloudReader copyright (C) 2016 Bin Jing (youlookwhat). Apache License 2.0. See LICENSE and NOTICE in the source.");
            if (i == 7) {
                if (!writable()) return;
                new AlertDialog.Builder(this).setTitle("Merge a Panna backup")
                    .setMessage("Adds entries with new IDs. Existing entries, including Trash, win when IDs match. Your existing notes are kept. You will see a preview before saving.")
                    .setNegativeButton("Cancel", null).setPositiveButton("Choose backup", (a,b) -> pick(MERGE, "*/*")).show();
            }
            if (i == 8) chooseTags();
            if (i == 9) { selectedIds.clear(); filter = "Trash"; query = ""; tagFilter = null; collectionFilter = null; showLibrary(); }
            if (i == 10) chooseCollectionFilter();
            if (i == 11) searchHelp();
            if (i == 12) exportCatalog();
        }).show();
    }
    private void statistics() {
        long words = 0; int saved = 0, finished = 0, favorites = 0, marks = 0, annotated = 0, archived = 0, trash = 0, pinned = 0;
        for (PannaEntry e : shelf.entries) {
            if (e.trashed) { trash++; continue; }
            saved++; if (e.archived) archived++; if (e.pinned) pinned++;
            words += e.words();
            if (!e.text.isEmpty() && e.offset >= e.text.length()) finished++;
            if (e.favorite) favorites++;
            if (!e.note.trim().isEmpty()) annotated++;
            marks += e.bookmarks.size();
        }
        message("Your library\n\n" + saved + " saved readings (includes " + archived + " archived)\n" + words + " saved words\n" + finished + " finished readings\n" + favorites + " favorites\n" + pinned + " pinned\n" + marks + " bookmarks\n" + annotated + " readings with notes\n" + trash + " in Trash\n\nTrash is excluded from saved totals, but counts toward the 500-entry storage limit. These are library totals, not tracked reading time.");
    }
    private void bookmarks() {
        final PannaEntry entry = current;
        ArrayList<String> labels = new ArrayList<>(); labels.add("+ Bookmark this position");
        for (PannaBookmark mark : entry.bookmarks) labels.add(mark.name + " · " + Math.round(PannaText.progress(mark.offset, entry.text.length()) * 100) + "%");
        new AlertDialog.Builder(this).setTitle("Bookmarks (" + entry.bookmarks.size() + ")")
            .setItems(labels.toArray(new String[0]), (d,i) -> {
                if (i == 0) { addBookmark(); return; }
                PannaBookmark mark = entry.bookmarks.get(i - 1);
                new AlertDialog.Builder(this).setTitle(mark.name).setItems(new String[]{"Jump to position", "Delete bookmark"}, (a,b) -> {
                    if (b == 0) {
                        if (readingBody.getLayout() == null) return;
                        int line = readingBody.getLayout().getLineForOffset(mark.offset);
                        readingScroll.smoothScrollTo(0, readingBody.getLayout().getLineTop(line));
                    } else if (writable()) {
                        entry.bookmarks.remove(i - 1);
                        if (!persist()) entry.bookmarks.add(i - 1, mark);
                    }
                }).show();
            }).show();
    }
    private void addBookmark() {
        if (!writable()) return;
        if (current.bookmarks.size() >= 100) { message("Maximum 100 bookmarks per reading."); return; }
        int offset = position();
        EditText name = input("Bookmark name", "Bookmark " + (current.bookmarks.size() + 1), false);
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Save reading position").setView(name)
            .setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(-1).setOnClickListener(v -> {
            String label = name.getText().toString().trim();
            if (label.isEmpty()) { name.setError("Enter a name"); return; }
            PannaBookmark mark = new PannaBookmark(label, offset); current.bookmarks.add(mark);
            if (persist()) dialog.dismiss(); else current.bookmarks.remove(mark);
        })); dialog.show();
    }
    private void listenMenu() {
        new AlertDialog.Builder(this).setTitle("Read Aloud · device voice")
            .setItems(new String[]{"Play from visible position", "Voice language", "Reading speed", "Stop listening"}, (d,i) -> {
                if (i == 0) {
                    String tag = prefs.getString("voice_language", "device");
                    voice.start(current.text, position(), tag.equals("device") ? Locale.getDefault() : Locale.forLanguageTag(tag), prefs.getFloat("voice_rate", 1f));
                }
                if (i == 1) new AlertDialog.Builder(this).setTitle("Voice language")
                    .setItems(new String[]{"Device language", "English (India)", "ਪੰਜਾਬੀ (India)", "हिन्दी (India)"}, (a,b) -> {
                        voice.stop(); prefs.edit().putString("voice_language", new String[]{"device", "en-IN", "pa-IN", "hi-IN"}[b]).apply();
                    }).show();
                if (i == 2) new AlertDialog.Builder(this).setTitle("Reading speed")
                    .setItems(new String[]{"Slow · 0.75×", "Normal · 1×", "Fast · 1.25×"}, (a,b) -> {
                        voice.stop(); prefs.edit().putFloat("voice_rate", new float[]{.75f, 1f, 1.25f}[b]).apply();
                    }).show();
                if (i == 3) voice.stop();
            }).show();
    }
    private void exportReading() {
        exportText(current.title + "\n\n" + current.text, current.title);
    }
    private void exportNotes() {
        StringBuilder notes = new StringBuilder(current.title).append("\n");
        if (!current.author.isEmpty()) notes.append("Author: ").append(current.author).append("\n");
        if (!current.published.isEmpty()) notes.append("Published: ").append(current.published).append("\n");
        if (!current.sourceUrl.isEmpty()) notes.append("Source: ").append(current.sourceUrl).append("\n");
        if (!current.collections.isEmpty()) notes.append("Collections: ").append(TextUtils.join(" · ", current.collections)).append("\n");
        if (!current.tag.isEmpty()) notes.append("Tag: ").append(current.tag).append("\n");
        notes.append("\nNOTES\n").append(current.note.isEmpty() ? "(No notes)" : current.note).append("\n\nBOOKMARKS\n");
        if (current.bookmarks.isEmpty()) notes.append("(No bookmarks)\n");
        for (PannaBookmark mark : current.bookmarks)
            notes.append("• ").append(mark.name).append(" — ").append(Math.round(PannaText.progress(mark.offset, current.text.length()) * 100)).append("% (UTF-16 offset ").append(mark.offset).append(")\n");
        notes.append("\nHIGHLIGHTS & ANNOTATIONS\n");
        if (current.highlights.isEmpty()) notes.append("(No highlights)\n");
        ArrayList<PannaHighlight> ordered = new ArrayList<>(current.highlights);
        Collections.sort(ordered, (a,b) -> Integer.compare(a.start, b.start));
        for (PannaHighlight h : ordered) {
            notes.append("\nQUOTE [").append(h.start).append("–").append(h.end).append("]: ").append(current.text, h.start, h.end).append("\n");
            if (!h.note.isEmpty()) notes.append("NOTE: ").append(h.note).append("\n");
        }
        exportText(notes.toString(), current.title + " study notes");
    }
    private void exportText(String content, String title) { exportDocument(content, title, "text/plain", ".txt", EXPORT_TEXT); }
    private void exportDocument(String content, String title, String mime, String extension, int request) {
        if (busy) { message("A file operation is already in progress."); return; }
        pendingTextExport = content;
        String filename = title.replaceAll("[^\\p{L}\\p{M}\\p{N} _-]", "_");
        if (filename.length() > 60) filename = filename.substring(0, 60);
        Intent out = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType(mime).addCategory(Intent.CATEGORY_OPENABLE);
        out.putExtra(Intent.EXTRA_TITLE, filename + extension); launch(out, request);
    }
    private void exportCatalog() {
        new AlertDialog.Builder(this).setTitle("Export catalog CSV").setItems(new String[]{"Current matching results", "All saved readings (exclude Trash)", "Selected readings"}, (d,i) -> {
            if (loadFailed) { message("Restore or reopen the library first."); return; }
            if (i == 0) try { PannaSearch.parse(query); } catch (IllegalArgumentException error) { message(error.getMessage()); return; }
            HashSet<String> scope = i == 0 ? new HashSet<>(visibleIds) : new HashSet<>(selectedIds);
            if (i == 2 && scope.isEmpty()) { message("Hold library cards to select readings first."); return; }
            StringBuilder csv = new StringBuilder("\uFEFF");
            csv.append(PannaResearch.csvRow("id", "title", "author", "published", "source_url", "collections_json", "tag", "status", "favorite", "pinned", "words", "progress_percent", "bookmarks", "highlights", "updated_epoch_ms"));
            ArrayList<PannaEntry> rows = new ArrayList<>();
            if (i == 1) { for (PannaEntry e : shelf.entries) if (!e.trashed) rows.add(e); }
            else for (String id : visibleIds) { PannaEntry e = shelf.find(id); if (e != null && scope.contains(id)) rows.add(e); }
            for (PannaEntry e : rows) {
                csv.append(PannaResearch.csvRow(e.id, e.title, e.author, e.published, e.sourceUrl, new org.json.JSONArray(e.collections).toString(), e.tag,
                    e.trashed ? "trash" : e.archived ? "archived" : "active", String.valueOf(e.favorite), String.valueOf(e.pinned),
                    String.valueOf(e.words()), String.valueOf(Math.round(PannaText.progress(e.offset, e.text.length()) * 100)),
                    String.valueOf(e.bookmarks.size()), String.valueOf(e.highlights.size()), String.valueOf(e.updated)));
            }
            exportDocument(csv.toString(), "PannaReader catalog", "text/csv", ".csv", EXPORT_CSV);
        }).show();
    }
    private PannaSearch.Document searchDocument(PannaEntry e) {
        return new PannaSearch.Document() {
            @Override public String[] values(String field) {
                switch (field) {
                    case "title": return new String[]{e.title};
                    case "text": return new String[]{e.text};
                    case "tag": return new String[]{e.tag};
                    case "author": return new String[]{e.author};
                    case "source": return new String[]{e.sourceUrl};
                    case "published": return new String[]{e.published};
                    case "collection": return e.collections.toArray(new String[0]);
                }
                ArrayList<String> values = new ArrayList<>(); values.add(e.note);
                for (PannaHighlight h : e.highlights) values.add(h.note);
                if (!field.equals("note")) {
                    values.addAll(Arrays.asList(e.title, e.text, e.tag, e.author, e.sourceUrl, e.published)); values.addAll(e.collections);
                }
                return values.toArray(new String[0]);
            }
            @Override public boolean has(String kind) {
                switch (kind) {
                    case "notes": return !e.note.trim().isEmpty();
                    case "annotations": for (PannaHighlight h : e.highlights) if (!h.note.trim().isEmpty()) return true; return false;
                    case "highlights": return !e.highlights.isEmpty();
                    case "bookmarks": return !e.bookmarks.isEmpty();
                    case "source": return !e.sourceUrl.isEmpty();
                    default: return false;
                }
            }
            @Override public boolean is(String state) {
                switch (state) {
                    case "favorite": return e.favorite;
                    case "pinned": return e.pinned;
                    case "archived": return e.archived;
                    case "trash": return e.trashed;
                    case "finished": return !e.text.isEmpty() && e.offset >= e.text.length();
                    case "unread": return e.offset == 0;
                    case "inprogress": return e.offset > 0 && e.offset < e.text.length();
                    default: return false;
                }
            }
        };
    }
    private void searchHelp() {
        message("Advanced search\n\nWords are ANDed: solar energy\nExact phrase: \"solar energy\"\nExclude a term: -draft\nField phrase: author:\"Bin Jing\"\nFields: title, text, note, tag, author, source, published, collection\nExample: collection:Research has:highlights -tag:draft\n\nhas: notes / annotations / highlights / bookmarks / source\nis: favorite / pinned / archived / trash / finished / unread / inprogress\n\nSearch remains inside the current Filter, tag and collection. Use Archived/Trash Filter to search those sections. Dates use YYYY-MM-DD; search published:2026 for a year. Case-insensitive literal matching, max 500 characters / 16 terms. No regex or OR. Unknown field prefixes are literal text.");
    }
    private void editSource() {
        if (!writable()) return; savePosition();
        LinearLayout fields = column(); fields.setPadding(dp(18), dp(10), dp(18), 0);
        EditText author = input("Author", current.author, false), url = input("Source URL · https://…", current.sourceUrl, false), date = input("Published · YYYY-MM-DD", current.published, false);
        author.setFilters(new InputFilter[]{new InputFilter.LengthFilter(240)});
        url.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2048)}); date.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        fields.addView(text("Source details are entered manually. No page is downloaded.", 13, muted));
        fields.addView(author); gap(fields, 8); fields.addView(url); gap(fields, 8); fields.addView(date);
        ScrollView scroller = new ScrollView(this); scroller.addView(fields);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Source details").setView(scroller).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(-1).setOnClickListener(v -> {
            String nextAuthor = author.getText().toString().trim(), nextUrl = url.getText().toString().trim(), nextDate = date.getText().toString().trim();
            if (!PannaResearch.validSource(nextUrl)) { url.setError("Use a complete HTTP(S) URL without embedded credentials, or leave blank"); return; }
            if (!PannaResearch.validDate(nextDate)) { date.setError("Use a real date in YYYY-MM-DD format, or leave blank"); return; }
            String oldAuthor = current.author, oldUrl = current.sourceUrl, oldDate = current.published;
            current.author = nextAuthor; current.sourceUrl = nextUrl; current.published = nextDate;
            if (persist()) { dialog.dismiss(); showReader(); } else { current.author = oldAuthor; current.sourceUrl = oldUrl; current.published = oldDate; }
        })); dialog.show();
    }
    private void openSource() {
        if (current.sourceUrl.isEmpty()) { message("Add a source URL using More → Source details first."); return; }
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(current.sourceUrl))); }
        catch (ActivityNotFoundException e) { message("No browser is available to open this source."); }
    }
    private interface CollectionChoice { void choose(String name); }
    private void chooseCollectionName(CollectionChoice action) {
        TreeSet<String> names = new TreeSet<>(); for (PannaEntry e : shelf.entries) names.addAll(e.collections);
        ArrayList<String> labels = new ArrayList<>(); labels.add("+ New collection"); labels.addAll(names);
        new AlertDialog.Builder(this).setTitle("Choose collection").setItems(labels.toArray(new String[0]), (d,i) -> {
            if (i > 0) { action.choose(labels.get(i)); return; }
            EditText name = input("Collection name", "", false); name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
            AlertDialog dialog = new AlertDialog.Builder(this).setTitle("New collection").setView(name).setNegativeButton("Cancel", null).setPositiveButton("Use collection", null).create();
            dialog.setOnShowListener(x -> dialog.getButton(-1).setOnClickListener(v -> {
                String value = name.getText().toString().trim();
                if (!PannaResearch.validCollection(value)) { name.setError("Enter a name without control characters"); return; }
                dialog.dismiss(); action.choose(value);
            })); dialog.show();
        }).show();
    }
    private void chooseExistingCollection(CollectionChoice action) {
        TreeSet<String> names = new TreeSet<>(); for (PannaEntry e : shelf.entries) names.addAll(e.collections);
        if (names.isEmpty()) { message("No collections exist yet."); return; }
        String[] values = names.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("Remove from collection").setItems(values, (d,i) -> action.choose(values[i])).show();
    }
    private void readingCollections() {
        ArrayList<String> labels = new ArrayList<>(); labels.add("+ Add to collection"); labels.addAll(current.collections);
        new AlertDialog.Builder(this).setTitle("Reading collections").setItems(labels.toArray(new String[0]), (d,i) -> {
            if (i == 0) {
                if (!writable()) return;
                chooseCollectionName(name -> {
                    if (current.collections.contains(name)) { message("Already in this collection."); return; }
                    if (current.collections.size() >= 10) { message("Maximum 10 collections per reading."); return; }
                    current.collections.add(name); if (!persist()) current.collections.remove(name);
                });
            } else {
                String name = labels.get(i);
                new AlertDialog.Builder(this).setTitle(name).setItems(new String[]{"Browse this collection", "Remove membership"}, (a,b) -> {
                    if (b == 0) { savePosition(); selectedIds.clear(); filter = current.archived ? "Archived" : "All"; collectionFilter = name; query = ""; tagFilter = null; showLibrary(); }
                    else if (writable()) {
                        int index = current.collections.indexOf(name); if (index < 0) return;
                        current.collections.remove(index); if (!persist()) current.collections.add(index, name);
                    }
                }).show();
            }
        }).show();
    }
    private void chooseCollectionFilter() {
        TreeSet<String> names = new TreeSet<>();
        for (PannaEntry e : shelf.entries) if (PannaCatalog.inSection(e.trashed, e.archived, filter)) names.addAll(e.collections);
        ArrayList<String> labels = new ArrayList<>(); labels.add("All collections"); labels.add("Unfiled"); labels.addAll(names);
        new AlertDialog.Builder(this).setTitle("Browse collections").setItems(labels.toArray(new String[0]), (d,i) -> {
            selectedIds.clear(); collectionFilter = i == 0 ? null : i == 1 ? "" : labels.get(i); query = ""; tagFilter = null; showLibrary();
        }).show();
    }
    private void previewMerge(PannaShelf incoming) {
        if (!writable()) return;
        final PannaCatalog.Merge<PannaEntry> preview;
        try { preview = PannaCatalog.merge(shelf.entries, incoming.entries, e -> e.id, 500); }
        catch (IllegalArgumentException error) { message(error.getMessage()); return; }
        new AlertDialog.Builder(this).setTitle("Review backup merge")
            .setMessage(preview.added + " entries to add\n" + preview.skipped + " matching IDs to skip\n\nExisting text, notes, positions and Trash are kept. Different IDs are separate readings even when titles or text match. Imported Archive/Trash states are preserved.")
            .setNegativeButton("Cancel", null).setPositiveButton("Merge", (d,i) -> {
                // Recalculate at commit time so the review cannot overwrite an intervening local edit.
                try {
                    PannaCatalog.Merge<PannaEntry> plan = PannaCatalog.merge(shelf.entries, incoming.entries, e -> e.id, 500);
                    ArrayList<PannaEntry> before = new ArrayList<>(shelf.entries);
                    shelf.entries.clear(); shelf.entries.addAll(plan.entries);
                    if (persist()) { selectedIds.clear(); showLibrary(); message("Merge complete: " + plan.added + " added, " + plan.skipped + " skipped. Check Archived or Trash for entries imported in those states."); }
                    else { shelf.entries.clear(); shelf.entries.addAll(before); }
                } catch (IllegalArgumentException error) { message(error.getMessage()); }
            }).show();
    }
    private void receiveSharedText(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction()) || !"text/plain".equals(intent.getType())) return;
        CharSequence shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (shared == null || shared.toString().trim().isEmpty()) { message("No shared text was supplied."); return; }
        String value = shared.toString();
        if (value.getBytes(StandardCharsets.UTF_8).length > PannaText.MAX_TEXT_BYTES) { message("Shared text exceeds the 2 MiB limit."); return; }
        CharSequence subject = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT);
        editEntry(null, subject == null ? "Shared reading" : subject.toString(), value);
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); selectedIds.clear(); savePosition(); voice.stop(); setIntent(intent); showLibrary(); receiveSharedText(intent);
    }
    private void pick(int request, String type) { launch(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType(type).addCategory(Intent.CATEGORY_OPENABLE), request); }
    private void launch(Intent intent, int request) {
        if (busy) { message("A file operation is already in progress."); return; }
        try { startActivityForResult(intent, request); } catch (ActivityNotFoundException e) { message("No document picker is available on this device."); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) { if (request == EXPORT_TEXT || request == EXPORT_CSV) pendingTextExport = null; return; }
        if ((request == EXPORT_TEXT || request == EXPORT_CSV) && pendingTextExport == null) { message("The export was interrupted. Open the reading and export it again."); return; }
        Uri uri = data.getData(); busy = true;
        // Capture a validated snapshot on the UI thread. Stream I/O runs off the UI thread.
        String snapshot;
        try { snapshot = request == EXPORT ? shelf.backup() : (request == EXPORT_TEXT || request == EXPORT_CSV) ? pendingTextExport : ""; pendingTextExport = null; }
        catch (Exception e) { busy = false; message("Cannot prepare backup"); return; }
        io.execute(() -> {
            try {
                if (request == EXPORT || request == EXPORT_TEXT || request == EXPORT_CSV) {
                    try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                        if (out == null) throw new IOException("Cannot open destination");
                        out.write(snapshot.getBytes(StandardCharsets.UTF_8));
                    }
                    runOnUiThread(() -> { busy = false; if (!isFinishing() && !isDestroyed()) message(request == EXPORT ? "Backup exported. Keep it somewhere safe." : request == EXPORT_CSV ? "Catalog exported as CSV." : "Text exported as a TXT file."); });
                } else {
                    String content;
                    try (InputStream in = getContentResolver().openInputStream(uri)) {
                        content = PannaShelf.read(in, (request == RESTORE || request == MERGE) ? PannaShelf.MAX_BACKUP_BYTES : PannaText.MAX_TEXT_BYTES);
                    }
                    final String loaded = content;
                    String name = "Imported reading";
                    if (request == IMPORT) try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                        if (c != null && c.moveToFirst()) name = c.getString(0);
                    }
                    final String title = name == null ? "Imported reading" : name.replaceFirst("(?i)\\.(txt|md|markdown)$", "");
                    // Parse a backup before touching the live shelf; malformed input never wipes current data.
                    PannaShelf validated = null;
                    if (request == RESTORE || request == MERGE) { validated = new PannaShelf(this); validated.restore(loaded); }
                    final PannaShelf replacement = validated;
                    runOnUiThread(() -> {
                        busy = false; if (isFinishing() || isDestroyed()) return;
                        if (request == IMPORT) editEntry(null, title, loaded);
                        else if (request == MERGE) previewMerge(replacement);
                        else {
                            ArrayList<PannaEntry> old = new ArrayList<>(shelf.entries); boolean failedBefore = loadFailed;
                            shelf.entries.clear(); shelf.entries.addAll(replacement.entries); loadFailed = false;
                            if (persist()) { prefs.edit().putBoolean("welcomed", true).apply(); selectedIds.clear(); showLibrary(); message("Library restored."); }
                            else { shelf.entries.clear(); shelf.entries.addAll(old); loadFailed = failedBefore; }
                        }
                    });
                }
            } catch (Exception e) { runOnUiThread(() -> { busy = false; if (!isFinishing() && !isDestroyed()) message("File operation failed: " + e.getMessage()); }); }
        });
    }
    @Override public void onBackPressed() { if (current != null) { savePosition(); showLibrary(); } else super.onBackPressed(); }
    @Override protected void onPause() { voice.stop(); savePosition(); getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); super.onPause(); }
    @Override protected void onResume() { super.onResume(); if (current != null && prefs.getBoolean("keep_awake", false)) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putStringArray("batch_ids", selectedIds.toArray(new String[0])); state.putString("query", query); state.putString("filter", filter); state.putString("order", order); state.putString("tag_filter", tagFilter); state.putString("collection_filter", collectionFilter); if (current != null) { savePosition(); state.putString("reading_id", current.id); } super.onSaveInstanceState(state); }
    @Override protected void onDestroy() { voice.close(); io.shutdown(); super.onDestroy(); }
}
