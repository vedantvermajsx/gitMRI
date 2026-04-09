package com.repointel.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.repointel.model.ComplexityMetric;
import com.repointel.service.visitor.CyclomaticComplexityVisitor;
import com.repointel.service.visitor.NestingDepthVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.*;

/**
 * Parses all source files in the repository.
 * Java files: full AST-based cyclomatic complexity analysis via JavaParser.
 * Frontend files (JS/TS/JSX/TSX/Vue/CSS/HTML/SCSS): line-count-based lightweight metrics.
 */
@Service
public class CodeParser {

    private static final Logger log = LoggerFactory.getLogger(CodeParser.class);

    private static final Set<String> JAVA_EXT = Set.of(".java");

    private static final Set<String> FRONTEND_EXT = Set.of(
            ".js", ".jsx", ".ts", ".tsx", ".vue", ".svelte",
            ".css", ".scss", ".sass", ".less",
            ".html", ".htm"
    );

    // Dirs to always skip
    private static final Set<String> SKIP_DIRS = Set.of(
            ".git", "node_modules", "target", "build", "dist",
            ".gradle", ".idea", "__pycache__", "vendor", "out"
    );

    private final JavaParser javaParser = new JavaParser();

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parse Java files deeply (AST complexity) and frontend files shallowly
     * (line-count heuristic).  Returns one ComplexityMetric per method/function.
     */
    public List<ComplexityMetric> parseRepository(File repoDir, String jobId) {
        List<ComplexityMetric> metrics = new ArrayList<>();

        List<File> javaFiles     = collectByExtension(repoDir, JAVA_EXT);
        List<File> frontendFiles = collectByExtension(repoDir, FRONTEND_EXT);

        log.info("Found {} Java files and {} frontend files in {}",
                javaFiles.size(), frontendFiles.size(), repoDir.getName());

        for (File f : javaFiles) {
            try { metrics.addAll(parseJavaFile(f, repoDir, jobId)); }
            catch (Exception e) { log.warn("Failed to parse {}: {}", f.getName(), e.getMessage()); }
        }

        for (File f : frontendFiles) {
            try { metrics.addAll(parseFrontendFile(f, repoDir, jobId)); }
            catch (Exception e) { log.warn("Failed to parse {}: {}", f.getName(), e.getMessage()); }
        }

        log.info("Total metrics: {} from {} source files",
                metrics.size(), javaFiles.size() + frontendFiles.size());
        return metrics;
    }

    /** Count ALL source files (Java + frontend). */
    public int countTotalSourceFiles(File repoDir) {
        return collectByExtension(repoDir, JAVA_EXT).size()
             + collectByExtension(repoDir, FRONTEND_EXT).size();
    }

    /** @deprecated use countTotalSourceFiles */
    public int countJavaFiles(File repoDir) {
        return collectByExtension(repoDir, JAVA_EXT).size();
    }

    /** Count total lines across ALL source files. */
    public long countTotalLines(File repoDir) {
        Set<String> all = new HashSet<>();
        all.addAll(JAVA_EXT);
        all.addAll(FRONTEND_EXT);
        return collectByExtension(repoDir, all).stream().mapToLong(f -> {
            try { return Files.lines(f.toPath()).count(); }
            catch (Exception e) { return 0; }
        }).sum();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Java parsing (full AST)
    // ─────────────────────────────────────────────────────────────────────────

    private List<ComplexityMetric> parseJavaFile(File file, File repoDir, String jobId) throws Exception {
        List<ComplexityMetric> metrics = new ArrayList<>();
        String relPath = repoDir.toPath().relativize(file.toPath()).toString();
        try (FileInputStream fis = new FileInputStream(file)) {
            ParseResult<CompilationUnit> result = javaParser.parse(fis);
            if (!result.isSuccessful() || result.getResult().isEmpty()) return metrics;
            CompilationUnit cu = result.getResult().get();
            String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
            cu.findAll(MethodDeclaration.class).forEach(m -> metrics.add(analyzeMethod(m, jobId, relPath, pkg)));
            cu.findAll(ConstructorDeclaration.class).forEach(c -> metrics.add(analyzeConstructor(c, jobId, relPath, pkg)));
        }
        return metrics;
    }

    private ComplexityMetric analyzeMethod(MethodDeclaration method, String jobId,
                                           String filePath, String packageName) {
        CyclomaticComplexityVisitor cc   = new CyclomaticComplexityVisitor();
        NestingDepthVisitor         nest = new NestingDepthVisitor();
        method.accept(cc,   null);
        method.accept(nest, null);
        String className = method.findAncestor(ClassOrInterfaceDeclaration.class)
                .map(ClassOrInterfaceDeclaration::getNameAsString).orElse("Unknown");
        int startLine = method.getBegin().map(p -> p.line).orElse(0);
        int endLine   = method.getEnd().map(p -> p.line).orElse(0);
        String params = method.getParameters().stream()
                .map(p -> p.getType().asString())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b);
        return ComplexityMetric.builder()
                .jobId(jobId).filePath(filePath).packageName(packageName)
                .className(className).methodName(method.getNameAsString() + "(" + params + ")")
                .ccScore(cc.getComplexity()).lineCount(endLine - startLine + 1)
                .nestingDepth(nest.getMaxDepth()).startLine(startLine).endLine(endLine)
                .build();
    }

    private ComplexityMetric analyzeConstructor(ConstructorDeclaration ctor, String jobId,
                                                String filePath, String packageName) {
        CyclomaticComplexityVisitor cc   = new CyclomaticComplexityVisitor();
        NestingDepthVisitor         nest = new NestingDepthVisitor();
        ctor.accept(cc,   null);
        ctor.accept(nest, null);
        int startLine = ctor.getBegin().map(p -> p.line).orElse(0);
        int endLine   = ctor.getEnd().map(p -> p.line).orElse(0);
        String params = ctor.getParameters().stream()
                .map(p -> p.getType().asString())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b);
        return ComplexityMetric.builder()
                .jobId(jobId).filePath(filePath).packageName(packageName)
                .className(ctor.getNameAsString()).methodName("<init>(" + params + ")")
                .ccScore(cc.getComplexity()).lineCount(endLine - startLine + 1)
                .nestingDepth(nest.getMaxDepth()).startLine(startLine).endLine(endLine)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Frontend parsing (heuristic: count function/arrow declarations)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * For JS/TS/JSX/TSX: find function declarations and arrow functions,
     * estimate CC by counting branches (if/else/ternary/&&/||/switch/catch).
     * For CSS/HTML: emit one file-level entry with line count as the metric.
     */
    private List<ComplexityMetric> parseFrontendFile(File file, File repoDir, String jobId) throws Exception {
        List<ComplexityMetric> metrics = new ArrayList<>();
        String relPath = repoDir.toPath().relativize(file.toPath()).toString();
        String name    = file.getName();
        String ext     = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
        List<String> lines = Files.readAllLines(file.toPath());

        boolean isScriptFile = Set.of(".js",".jsx",".ts",".tsx",".vue",".svelte").contains(ext);

        if (isScriptFile) {
            metrics.addAll(extractFunctions(lines, relPath, jobId, name));
            // If nothing extracted, add a file-level entry
            if (metrics.isEmpty()) {
                metrics.add(fileMetric(jobId, relPath, name, lines));
            }
        } else {
            // CSS / HTML — single file-level entry
            metrics.add(fileMetric(jobId, relPath, name, lines));
        }
        return metrics;
    }

    /**
     * Heuristic JS/TS function extractor.
     * Identifies named functions, arrow functions, and React component-style exports.
     * Estimates CC from branch keywords within each function's body.
     */
    private List<ComplexityMetric> extractFunctions(List<String> lines, String relPath,
                                                    String jobId, String fileName) {
        List<ComplexityMetric> metrics = new ArrayList<>();
        String className = fileName.replace(".jsx","").replace(".tsx","")
                                   .replace(".js","").replace(".ts","")
                                   .replace(".vue","").replace(".svelte","");

        // Patterns to detect function starts
        java.util.regex.Pattern funcPat = java.util.regex.Pattern.compile(
            "(?:^|\\s)" +
            "(?:export\\s+(?:default\\s+)?)?(?:async\\s+)?" +
            "(?:" +
              "function\\s+(\\w+)\\s*\\(" +                         // function foo(
              "|const\\s+(\\w+)\\s*=\\s*(?:async\\s*)?\\(" +        // const foo = async (
              "|const\\s+(\\w+)\\s*=\\s*(?:async\\s*)?\\w+\\s*=>"  // const foo = x =>
            + ")"
        );

        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i).trim();
            java.util.regex.Matcher m = funcPat.matcher(lines.get(i));
            if (m.find()) {
                // Pick whichever capture group matched
                String fnName = m.group(1) != null ? m.group(1)
                              : m.group(2) != null ? m.group(2)
                              : m.group(3) != null ? m.group(3) : null;
                if (fnName != null) {
                    int start = i + 1;
                    int end   = findFunctionEnd(lines, i);
                    List<String> body = lines.subList(i, Math.min(end + 1, lines.size()));
                    int cc      = estimateCC(body);
                    int nesting = estimateNesting(body);
                    metrics.add(ComplexityMetric.builder()
                            .jobId(jobId).filePath(relPath).packageName("")
                            .className(className).methodName(fnName + "()")
                            .ccScore(cc).lineCount(end - start + 1)
                            .nestingDepth(nesting).startLine(start).endLine(end)
                            .build());
                    i = end > i ? end : i + 1;
                    continue;
                }
            }
            i++;
        }
        return metrics;
    }

    /** Walk lines forward to find the closing brace of the function body. */
    private int findFunctionEnd(List<String> lines, int startIdx) {
        int depth = 0;
        boolean entered = false;
        for (int i = startIdx; i < lines.size(); i++) {
            for (char c : lines.get(i).toCharArray()) {
                if (c == '{') { depth++; entered = true; }
                else if (c == '}') { depth--; if (entered && depth <= 0) return i; }
            }
        }
        return Math.min(startIdx + 50, lines.size() - 1); // fallback: 50-line window
    }

    /** Count branch keywords to estimate CC for a block of lines. */
    private int estimateCC(List<String> lines) {
        int cc = 1;
        for (String line : lines) {
            String l = line.trim();
            // Each keyword adds 1
            cc += countOccurrences(l, "if (") + countOccurrences(l, "if(");
            cc += countOccurrences(l, "else if");
            cc += countOccurrences(l, "? ")   ; // ternary
            cc += countOccurrences(l, "&&")   ;
            cc += countOccurrences(l, "||")   ;
            cc += countOccurrences(l, "for (") + countOccurrences(l, "for(");
            cc += countOccurrences(l, "while (") + countOccurrences(l, "while(");
            cc += countOccurrences(l, "catch (") + countOccurrences(l, "catch(");
            cc += countOccurrences(l, "case ");
            cc += countOccurrences(l, ".map(") + countOccurrences(l, ".filter(")
                + countOccurrences(l, ".reduce(") + countOccurrences(l, ".forEach(");
        }
        return Math.min(cc, 50); // cap for display
    }

    private int estimateNesting(List<String> lines) {
        int max = 0, cur = 0;
        for (String line : lines) {
            for (char c : line.toCharArray()) {
                if (c == '{') { cur++; max = Math.max(max, cur); }
                else if (c == '}' && cur > 0) cur--;
            }
        }
        return max;
    }

    private int countOccurrences(String text, String token) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(token, idx)) != -1) { count++; idx += token.length(); }
        return count;
    }

    private ComplexityMetric fileMetric(String jobId, String relPath,
                                        String name, List<String> lines) {
        // Estimate a file-level complexity from total branch count
        int cc = estimateCC(lines);
        return ComplexityMetric.builder()
                .jobId(jobId).filePath(relPath).packageName("")
                .className(name).methodName("<file>")
                .ccScore(Math.max(1, cc / 5)) // scale down for file-level
                .lineCount(lines.size())
                .nestingDepth(estimateNesting(lines))
                .startLine(1).endLine(lines.size())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  File collection
    // ─────────────────────────────────────────────────────────────────────────

    private List<File> collectByExtension(File dir, Set<String> extensions) {
        List<File> files = new ArrayList<>();
        collectRecursive(dir, extensions, files);
        return files;
    }

    private void collectRecursive(File dir, Set<String> extensions, List<File> files) {
        if (dir == null || !dir.exists()) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) {
                if (!SKIP_DIRS.contains(f.getName())) collectRecursive(f, extensions, files);
            } else if (f.isFile()) {
                String name = f.getName();
                String ext  = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
                if (extensions.contains(ext)) files.add(f);
            }
        }
    }
}
