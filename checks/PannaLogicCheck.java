import com.amrik.panna.PannaText;

public final class PannaLogicCheck {
    private static int passed;
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        passed++;
    }
    public static void main(String[] args) {
        check(PannaText.words("") == 0, "empty word count");
        check(PannaText.words(" \n\t ") == 0, "whitespace word count");
        check(PannaText.words("ਸਤਿ ਸ੍ਰੀ ਅਕਾਲ") == 3, "Punjabi word count");
        check(PannaText.words("one\n two\tthree") == 3, "mixed separators");
        check(PannaText.minutes("word ".repeat(200)) == 1, "200-word estimate");
        check(PannaText.minutes("word ".repeat(201)) == 2, "round up estimate");
        check(PannaText.progress(-5, 20) == 0, "negative progress");
        check(PannaText.progress(100, 20) == 1, "progress upper bound");
        check(PannaText.progress(3, 0) == 0, "empty document progress");
        check(PannaText.progress(50, 200) == .25, "partial progress");
        check(PannaText.matches("READER", "Panna Reader"), "case-insensitive search");
        check(PannaText.matches("ਪੰਜਾਬੀ", "notes", "ਮੇਰੀ ਪੰਜਾਬੀ ਕਿਤਾਬ"), "Punjabi search");
        check(!PannaText.matches("missing", "title", "body"), "absent search");
        check(PannaText.clamp(99, 14, 34) == 34, "font upper limit");
        check(PannaText.clamp(4, 14, 34) == 14, "font lower limit");
        check(PannaText.speechChunkEnd("short", 0, 3000) == 5, "short speech chunk");
        check(PannaText.speechChunkEnd("abcdef", 0, 4) == 4, "hard speech limit");
        check(PannaText.speechChunkEnd("alpha beta gamma", 0, 12) == 11, "prefer complete word");
        check(PannaText.speechChunkEnd("ab😀cd", 0, 3) == 2, "do not split emoji surrogate pair");
        check(PannaText.speechChunkEnd("", 0, 10) == 0, "empty speech input");
        String multilingual = "ਪੰਜਾਬੀ text 😀 हिन्दी\n".repeat(1000);
        boolean valid = true;
        for (int limit : new int[]{2, 3, 7, 31, 3000}) {
            StringBuilder joined = new StringBuilder(); int at = 0;
            while (at < multilingual.length()) {
                int end = PannaText.speechChunkEnd(multilingual, at, limit);
                if (end <= at || end - at > limit) { valid = false; break; }
                if (end < multilingual.length() && Character.isHighSurrogate(multilingual.charAt(end - 1)) && Character.isLowSurrogate(multilingual.charAt(end))) valid = false;
                joined.append(multilingual, at, end); at = end;
            }
            valid &= joined.toString().equals(multilingual);
        }
        check(valid, "long multilingual speech has bounded chunks, no split pairs, no missing or duplicated text");
        boolean invalidLimit = false;
        try { PannaText.speechChunkEnd("abc", 0, 1); } catch (IllegalArgumentException expected) { invalidLimit = true; }
        check(invalidLimit, "reject unusable speech chunk limit");
        boolean invalidStart = false;
        try { PannaText.speechChunkEnd("abc", 4, 10); } catch (IllegalArgumentException expected) { invalidStart = true; }
        check(invalidStart, "reject invalid speech start");
        System.out.println("PASS: " + passed + " reading logic checks");
    }
}
