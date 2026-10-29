import com.harpreet.panna.PannaCatalog;
import java.util.*;

public final class CatalogCheck {
    private static int passed;
    private static void check(boolean valid, String label) {
        if (!valid) throw new AssertionError(label);
        passed++;
    }
    private static final class Item {
        final String id, content;
        Item(String id, String content) { this.id = id; this.content = content; }
    }
    private static PannaCatalog.Merge<Item> merge(List<Item> current, List<Item> incoming, int limit) {
        return PannaCatalog.merge(current, incoming, e -> e.id, limit);
    }
    public static void main(String[] args) {
        Item local = new Item("a", "new local notes"), stale = new Item("a", "old backup notes");
        Item added = new Item("b", "new entry"), copy = new Item("c", "new local notes");
        List<Item> current = new ArrayList<>(List.of(local));
        List<Item> incoming = new ArrayList<>(List.of(stale, added, added, copy));
        var plan = merge(current, incoming, 500);
        check(plan.entries.get(0) == local && plan.entries.get(0).content.equals("new local notes"), "existing ID keeps local data");
        check(plan.added == 2 && plan.skipped == 2, "count new IDs and duplicates");
        check(plan.entries.get(1) == added && plan.entries.get(2) == copy, "preserve incoming order for new IDs");
        check(plan.entries.size() == 3, "same content and different IDs remain separate readings");
        check(current.size() == 1 && incoming.size() == 4, "merge preview never changes either source");
        var again = merge(plan.entries, incoming, 500);
        check(again.added == 0 && again.skipped == 4, "repeated merge does not duplicate readings");
        boolean rejected = false;
        try { merge(current, List.of(added, copy), 2); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected && current.size() == 1, "over-limit merge fails without partial mutation");
        check(merge(current, List.of(stale), 1).entries.size() == 1, "full library accepts duplicates-only merge");
        check(merge(List.of(), List.of(), 0).entries.isEmpty(), "empty merge");
        rejected = false;
        try { merge(List.of(local, stale), List.of(), 500); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "reject duplicate IDs in local shelf");
        rejected = false;
        try { merge(current, List.of(new Item("", "bad")), 500); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "reject empty incoming ID");
        check(PannaCatalog.inSection(false, false, "All"), "active reading appears in All");
        check(!PannaCatalog.inSection(false, true, "All"), "archive hidden from All");
        check(!PannaCatalog.inSection(true, false, "All"), "trash hidden from All");
        check(PannaCatalog.inSection(false, true, "Archived"), "archive included in Archived");
        check(!PannaCatalog.inSection(true, true, "Archived"), "trashed archive hidden from Archived");
        check(PannaCatalog.inSection(true, true, "Trash"), "trashed archive visible in Trash");
        check(!PannaCatalog.inSection(false, false, "Trash"), "active entry absent from Trash");
        check(!PannaCatalog.inSection(false, true, "Favorites"), "archived item excluded from active special filters");
        check(PannaCatalog.pinPriority(true, false) < 0, "pinned comes first");
        check(PannaCatalog.pinPriority(false, true) > 0, "unpinned comes after pinned");
        check(PannaCatalog.pinPriority(true, true) == 0 && PannaCatalog.pinPriority(false, false) == 0, "same pin status permits secondary sort");
        check(PannaCatalog.tagMatches("ਪੰਜਾਬੀ", "ਪੰਜਾਬੀ"), "exact Punjabi tag match");
        check(PannaCatalog.tagMatches("topic", null), "all tags selection");
        check(PannaCatalog.tagMatches("", "") && !PannaCatalog.tagMatches("topic", ""), "untagged filter");
        check(!PannaCatalog.tagMatches("topic", "other"), "different tag excluded");
        System.out.println("PASS: " + passed + " catalog and merge checks");
    }
}
