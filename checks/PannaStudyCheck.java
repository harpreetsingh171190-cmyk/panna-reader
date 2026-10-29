import com.amrik.panna.PannaStudy;
import java.util.List;

public final class PannaStudyCheck {
    private static int passed;
    private static void check(boolean valid, String label) {
        if (!valid) throw new AssertionError(label);
        passed++;
    }
    public static void main(String[] args) {
        String source = "# Main\nintro\n## ਪੰਜਾਬੀ ###\ntext\n";
        List<PannaStudy.Heading> headings = PannaStudy.headings(source);
        check(headings.size() == 2, "ATX headings detected");
        check(headings.get(0).title.equals("Main") && headings.get(0).level == 1, "ATX level and title");
        check(headings.get(1).title.equals("ਪੰਜਾਬੀ") && headings.get(1).level == 2, "Punjabi title and closing hashes");
        check(headings.get(1).offset == source.indexOf("## ਪੰਜਾਬੀ"), "ATX heading source offset");
        source = "😀 intro\r\n# ਪੰਜਾਬੀ\r\nNext chapter\r\n===\r\nSubsection\r\n---\r\n";
        headings = PannaStudy.headings(source);
        check(headings.size() == 3, "CRLF headings detected");
        check(headings.get(0).offset == source.indexOf("# ਪੰਜਾਬੀ"), "CRLF and emoji preserve UTF-16 offsets");
        check(headings.get(1).title.equals("Next chapter") && headings.get(1).level == 1, "setext top-level heading");
        check(headings.get(1).offset == source.indexOf("Next chapter"), "setext jump points to title, not underline");
        check(headings.get(2).level == 2, "setext subsection level");
        check(PannaStudy.headings("plain text\nno heading").isEmpty(), "plain text has no guessed headings");
        check(PannaStudy.headings("a # hashtag\n#missing-space\n    # indented").isEmpty(), "inline and indented hashes ignored");
        headings = PannaStudy.headings("```java\n# hidden\n```\n# shown\n");
        check(headings.size() == 1 && headings.get(0).title.equals("shown"), "backtick fenced headings ignored");
        headings = PannaStudy.headings("~~~~\n# hidden\n~~~\n# still hidden\n~~~~\n# shown");
        check(headings.size() == 1 && headings.get(0).title.equals("shown"), "short fence does not close longer tilde fence");
        check(PannaStudy.headings("```\n# hidden\n").isEmpty(), "unclosed fence hides following headings");
        check(PannaStudy.headings("\n---\n").isEmpty(), "horizontal rule without title is not a heading");
        check(PannaStudy.headings("# Final heading").get(0).offset == 0, "final heading without newline");
        check(PannaStudy.headings("# Chapter\n".repeat(350)).size() == 300, "contents limit bounds long documents");
        check(PannaStudy.headings("").isEmpty(), "empty contents");
        String text = "a😀ਪੰਜਾਬੀz";
        check(PannaStudy.validRange(text, 0, text.length()), "whole Unicode text range");
        check(PannaStudy.validRange(text, 1, 3), "complete emoji range");
        check(!PannaStudy.validRange(text, 1, 2), "reject end splitting surrogate pair");
        check(!PannaStudy.validRange(text, 2, 3), "reject start splitting surrogate pair");
        check(!PannaStudy.validRange(text, -1, 3), "reject negative start");
        check(!PannaStudy.validRange(text, 0, text.length() + 1), "reject end beyond source");
        check(!PannaStudy.validRange(text, 3, 3), "reject empty highlight");
        check(!PannaStudy.validRange(text, 5, 3), "reject reversed highlight");
        check(PannaStudy.overlaps(0, 5, 4, 8), "detect partial overlap");
        check(PannaStudy.overlaps(0, 8, 2, 3), "detect contained highlight");
        check(!PannaStudy.overlaps(0, 5, 5, 8), "adjacent highlights are allowed");
        check(!PannaStudy.overlaps(0, 2, 4, 6), "disjoint highlights are allowed");
        System.out.println("PASS: " + passed + " study annotation and heading checks");
    }
}
