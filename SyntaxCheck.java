import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.util.JavacTask;

/** Parser check only; Android type checking happens in the Android Gradle build. */
public final class SyntaxCheck {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> errors = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(errors, null, java.nio.charset.StandardCharsets.UTF_8)) {
            List<java.io.File> sources = new ArrayList<>();
            try (var paths = Files.list(Path.of("src/main/java/com/harpreet/panna"))) {
                paths.filter(p -> p.getFileName().toString().startsWith("Panna") && p.toString().endsWith(".java")).forEach(p -> sources.add(p.toFile()));
            }
            JavacTask task = (JavacTask) compiler.getTask(null, files, errors, List.of("-proc:none"), null, files.getJavaFileObjectsFromFiles(sources));
            task.parse();
            for (var e : errors.getDiagnostics()) if (e.getKind() == Diagnostic.Kind.ERROR) throw new AssertionError(e.toString());
            System.out.println("PASS: Java syntax parsed for " + sources.size() + " Android source files (not Android type checking)");
        }
    }
}
