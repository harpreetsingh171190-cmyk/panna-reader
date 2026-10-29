import com.amrik.panna.PannaResearch;
import com.amrik.panna.PannaSearch;
import java.util.*;

public final class PannaResearchCheck {
    private static int passed;
    private static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); passed++; }
    private static final class Doc implements PannaSearch.Document {
        final Map<String, String[]> fields = new HashMap<>();
        public String[] values(String field) {
            if (!field.equals("all")) return fields.getOrDefault(field, new String[0]);
            ArrayList<String> all = new ArrayList<>(); for (String[] values : fields.values()) all.addAll(Arrays.asList(values)); return all.toArray(new String[0]);
        }
        public boolean has(String kind) { return kind.equals("highlights") || kind.equals("source"); }
        public boolean is(String state) { return state.equals("favorite"); }
    }
    private static void rejects(String value, String label) {
        boolean failed = false; try { PannaSearch.parse(value); } catch (IllegalArgumentException expected) { failed = true; }
        check(failed, label);
    }
    public static void main(String[] args) {
        check(PannaResearch.validSource(""), "optional URL");
        check(PannaResearch.validSource("https://example.org/read?q=one%20two#part"), "complete source URL");
        check(PannaResearch.validSource("HTTP://example.org:8080/path"), "HTTP with explicit port");
        check(!PannaResearch.validSource("javascript:alert(1)"), "reject executable source scheme");
        check(!PannaResearch.validSource("file:///private/data"), "reject file scheme");
        check(!PannaResearch.validSource("//example.org/path"), "reject scheme-relative URL");
        check(!PannaResearch.validSource("https://user:secret@example.org"), "reject embedded source credentials");
        check(!PannaResearch.validSource("https://example.org/a b"), "reject unescaped source whitespace");
        check(!PannaResearch.validSource("https://example.org:99999"), "reject invalid source port");
        check(!PannaResearch.validSource("https://"), "reject missing URL host");
        check(PannaResearch.validDate(""), "optional publication date");
        check(PannaResearch.validDate("2024-02-29"), "valid leap day");
        check(PannaResearch.validDate("2000-02-29"), "century divisible by 400 leap day");
        check(!PannaResearch.validDate("1900-02-29"), "non-leap century");
        check(!PannaResearch.validDate("2026-02-29"), "invalid normal-year leap day");
        check(!PannaResearch.validDate("2026-04-31"), "invalid day in 30-day month");
        check(!PannaResearch.validDate("2026-13-01") && !PannaResearch.validDate("0000-01-01"), "invalid month and zero year");
        check(!PannaResearch.validDate("2026-1-1"), "require ISO date widths");
        check(PannaResearch.validCollection("ਪੰਜਾਬੀ ਪੜ੍ਹਾਈ"), "Unicode collection name");
        check(!PannaResearch.validCollection(" work") && !PannaResearch.validCollection(""), "no blank or padded collection names");
        check(!PannaResearch.validCollection("a\nb"), "no collection control characters");
        check(!PannaResearch.validCollection("x".repeat(81)), "collection name length limit");
        check(PannaResearch.inCollection(List.of("Work", "Study"), "Study"), "multiple memberships");
        check(!PannaResearch.inCollection(List.of("Study"), "study"), "collection picker is exact case-sensitive");
        check(PannaResearch.inCollection(List.of(), "") && !PannaResearch.inCollection(List.of("Work"), ""), "unfiled membership filter");
        check(PannaResearch.inCollection(List.of("Work"), null), "all collections filter");
        check(PannaResearch.csvCell("a,\"b\"\nc").equals("\"a,\"\"b\"\"\nc\""), "CSV commas, double quotes and newlines");
        check(PannaResearch.csvCell("=1+1").equals("\"'=1+1\""), "CSV formula-like cell escaped");
        check(PannaResearch.csvCell("  @SUM(A1)").equals("\"'  @SUM(A1)\""), "CSV leading-space formula guarded");
        check(PannaResearch.csvCell("ਪੰਜਾਬੀ").equals("\"ਪੰਜਾਬੀ\""), "Unicode CSV text");
        check(PannaResearch.csvCell(null).equals("\"\""), "CSV optional null cell");
        check(PannaResearch.csvRow("a", "b").equals("\"a\",\"b\"\r\n"), "CSV row uses CRLF");
        Doc doc = new Doc();
        doc.fields.put("title", new String[]{"Solar Energy Handbook"}); doc.fields.put("author", new String[]{"Bin Jing"});
        doc.fields.put("text", new String[]{"ਪੰਜਾਬੀ notes and thermal storage"}); doc.fields.put("tag", new String[]{"Research"});
        doc.fields.put("collection", new String[]{"Work", "Study group"}); doc.fields.put("source", new String[]{"https://example.org/read"});
        check(PannaSearch.parse("").matches(doc), "empty query matches");
        check(PannaSearch.parse("solar storage").matches(doc), "AND terms may match different fields");
        check(!PannaSearch.parse("solar missing").matches(doc), "all positive terms required");
        check(PannaSearch.parse("\"solar energy\"").matches(doc), "quoted phrase");
        check(!PannaSearch.parse("\"energy solar\"").matches(doc), "quoted phrase preserves order");
        check(PannaSearch.parse("author:\"BIN JING\" title:solar").matches(doc), "field phrase and case folding");
        check(!PannaSearch.parse("author:solar").matches(doc), "field restricts matching");
        check(PannaSearch.parse("solar -draft").matches(doc), "negative absent term passes");
        check(!PannaSearch.parse("-tag:research").matches(doc), "negative matching field excludes");
        check(PannaSearch.parse("collection:\"study group\" has:highlights is:favorite").matches(doc), "collection, has and is filters");
        check(!PannaSearch.parse("has:notes").matches(doc), "has predicate false");
        check(!PannaSearch.parse("is:trash").matches(doc), "is predicate false");
        check(PannaSearch.parse("ਪੰਜਾਬੀ").matches(doc), "Punjabi query");
        check(PannaSearch.parse("https://example.org/read").matches(doc), "unknown prefix is literal, preserving URL search");
        doc.fields.put("note", new String[]{"say \"hello\"", "C:\\work"});
        check(PannaSearch.parse("note:\"say \\\"hello\\\"\"").matches(doc), "escaped quote inside phrase");
        check(PannaSearch.parse("note:C:\\\\work").matches(doc), "escaped backslash in search");
        doc.fields.put("note", new String[]{"Ärger über ÖL", "Straße"});
        check(PannaSearch.parse("note:ärger").matches(doc) && PannaSearch.parse("note:ÜBER").matches(doc), "case-insensitive non-ASCII search");
        check(!PannaSearch.parse("note:ärgers").matches(doc) && !PannaSearch.parse("note:zzz").matches(doc), "needle longer than or absent from text");
        check(PannaSearch.parse("note:\"öl\"").matches(doc), "match at the very end of the text");
        rejects("\"unclosed", "reject unclosed phrase");
        rejects("author:", "reject missing field value");
        rejects("has:unknown", "reject unknown has predicate");
        rejects("is:unknown", "reject unknown state predicate");
        rejects("x".repeat(501), "search character limit");
        rejects("word ".repeat(17), "search term limit");
        System.out.println("PASS: " + passed + " metadata, collection, search and CSV checks");
    }
}
