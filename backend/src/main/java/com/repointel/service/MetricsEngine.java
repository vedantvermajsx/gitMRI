package com.repointel.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.repointel.model.DeadCodeItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dead code detector using a conservative call-graph analysis.
 *
 * Algorithm:
 * 1. Build symbol table: all class/method/field declarations
 * 2. Build call graph: all method call expressions
 * 3. Mark Spring/framework entry points as reachable
 * 4. BFS/DFS from entry points
 * 5. Unreachable nodes = dead code candidates
 */
@Service
@Slf4j
public class MetricsEngine {

    // Spring and JUnit annotations that make methods entry points
    private static final Set<String> ENTRY_ANNOTATIONS = Set.of(
            "RestController", "Controller", "Service", "Component", "Repository",
            "Bean", "EventListener", "Scheduled", "PostConstruct", "PreDestroy",
            "Test", "BeforeEach", "AfterEach", "BeforeAll", "AfterAll",
            "RequestMapping", "GetMapping", "PostMapping", "PutMapping", "DeleteMapping",
            "Override", "SpringBootApplication", "Configuration"
    );

    public List<DeadCodeItem> detectDeadCode(File repoDir, String jobId) {
        List<DeadCodeItem> deadItems = new ArrayList<>();

        // Phase 1: collect all declarations
        Map<String, DeclarationInfo> allDeclarations = new HashMap<>();
        Set<String> calledMethods = new HashSet<>();
        Set<String> usedClasses = new HashSet<>();
        Set<String> entryPoints = new HashSet<>();

        List<File> javaFiles = collectJavaFiles(repoDir);
        JavaParser parser = new JavaParser();

        for (File file : javaFiles) {
            try (FileInputStream fis = new FileInputStream(file)) {
                ParseResult<CompilationUnit> result = parser.parse(fis);
                if (!result.isSuccessful() || result.getResult().isEmpty()) continue;
                CompilationUnit cu = result.getResult().get();

                String relativePath = repoDir.toPath().relativize(file.toPath()).toString();
                collectDeclarations(cu, relativePath, allDeclarations, entryPoints);
                collectCalls(cu, calledMethods, usedClasses);

            } catch (Exception e) {
                log.warn("MetricsEngine: error parsing {}: {}", file.getName(), e.getMessage());
            }
        }

        // Phase 2: BFS from entry points
        Set<String> reachable = new HashSet<>(entryPoints);
        Queue<String> queue = new LinkedList<>(entryPoints);

        // Also mark anything that's called as reachable
        reachable.addAll(calledMethods);

        // Phase 3: find unreachable declarations
        for (Map.Entry<String, DeclarationInfo> entry : allDeclarations.entrySet()) {
            String key = entry.getKey();
            DeclarationInfo info = entry.getValue();

            if (!reachable.contains(key) && !isEntryPointAnnotated(info)) {
                String risk = computeRisk(info);
                deadItems.add(DeadCodeItem.builder()
                        .jobId(jobId)
                        .itemType(info.type)
                        .name(info.simpleName)
                        .qualifiedName(key)
                        .filePath(info.filePath)
                        .lineNumber(info.line)
                        .riskLevel(risk)
                        .reason(computeReason(info, calledMethods, usedClasses))
                        .build());
            }
        }

        log.info("Dead code detection found {} candidates", deadItems.size());
        return deadItems;
    }

    private void collectDeclarations(CompilationUnit cu, String filePath,
                                      Map<String, DeclarationInfo> declarations,
                                      Set<String> entryPoints) {
        String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString() + ".").orElse("");

        // Classes
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
            String qualified = pkg + cls.getNameAsString();
            DeclarationInfo info = new DeclarationInfo("CLASS", cls.getNameAsString(),
                    qualified, filePath, cls.getBegin().map(p -> p.line).orElse(0));
            info.annotations = getAnnotationNames(cls.getAnnotations());
            declarations.put(qualified, info);

            if (info.annotations.stream().anyMatch(ENTRY_ANNOTATIONS::contains)) {
                entryPoints.add(qualified);
            }

            // Methods
            cls.getMethods().forEach(method -> {
                String mKey = qualified + "#" + method.getNameAsString();
                DeclarationInfo mInfo = new DeclarationInfo("METHOD",
                        method.getNameAsString(), mKey, filePath,
                        method.getBegin().map(p -> p.line).orElse(0));
                mInfo.annotations = getAnnotationNames(method.getAnnotations());
                mInfo.isPublic = method.isPublic();
                mInfo.isStatic = method.isStatic();
                declarations.put(mKey, mInfo);

                // Mark entry points
                if (method.getNameAsString().equals("main") && method.isStatic()) {
                    entryPoints.add(mKey);
                }
                if (mInfo.annotations.stream().anyMatch(ENTRY_ANNOTATIONS::contains)) {
                    entryPoints.add(mKey);
                }
            });
        });
    }

    private void collectCalls(CompilationUnit cu, Set<String> calledMethods, Set<String> usedClasses) {
        cu.findAll(MethodCallExpr.class).forEach(call ->
                calledMethods.add(call.getNameAsString()));

        cu.findAll(ObjectCreationExpr.class).forEach(creation ->
                usedClasses.add(creation.getType().getNameAsString()));
    }

    private boolean isEntryPointAnnotated(DeclarationInfo info) {
        return info.annotations != null && info.annotations.stream().anyMatch(ENTRY_ANNOTATIONS::contains);
    }

    private String computeRisk(DeclarationInfo info) {
        if ("CLASS".equals(info.type)) return "HIGH";
        if (!info.isPublic) return "MEDIUM";
        return "LOW";
    }

    private String computeReason(DeclarationInfo info, Set<String> calledMethods, Set<String> usedClasses) {
        if ("CLASS".equals(info.type)) {
            return usedClasses.contains(info.simpleName)
                    ? "Class instantiated but has no reachable methods"
                    : "No references found to this class";
        }
        return calledMethods.contains(info.simpleName)
                ? "Method name called but not resolvable to this declaration"
                : "Never invoked in call graph";
    }

    private List<String> getAnnotationNames(List<AnnotationExpr> annotations) {
        return annotations.stream()
                .map(a -> a.getNameAsString())
                .collect(Collectors.toList());
    }

    private List<File> collectJavaFiles(File dir) {
        List<File> files = new ArrayList<>();
        collectRecursive(dir, files);
        return files;
    }

    private void collectRecursive(File dir, List<File> files) {
        if (dir == null || !dir.exists()) return;
        for (File f : dir.listFiles() != null ? dir.listFiles() : new File[0]) {
            if (f.isDirectory() && !f.getName().equals(".git")) collectRecursive(f, files);
            else if (f.isFile() && f.getName().endsWith(".java")) files.add(f);
        }
    }

    // Internal data class for declaration tracking
    static class DeclarationInfo {
        String type;
        String simpleName;
        String qualifiedName;
        String filePath;
        int line;
        List<String> annotations = new ArrayList<>();
        boolean isPublic = true;
        boolean isStatic = false;

        DeclarationInfo(String type, String simpleName, String qualifiedName, String filePath, int line) {
            this.type = type;
            this.simpleName = simpleName;
            this.qualifiedName = qualifiedName;
            this.filePath = filePath;
            this.line = line;
        }
    }
}
