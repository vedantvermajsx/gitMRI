package com.repointel.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.repointel.model.DeadCodeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MetricsEngine {

    private static final Logger log = LoggerFactory.getLogger(MetricsEngine.class);

    private static final Set<String> ENTRY_ANNOTATIONS = Set.of(
            "RestController","Controller","Service","Component","Repository",
            "Bean","EventListener","Scheduled","PostConstruct","PreDestroy",
            "Test","BeforeEach","AfterEach","BeforeAll","AfterAll",
            "RequestMapping","GetMapping","PostMapping","PutMapping","DeleteMapping",
            "Override","SpringBootApplication","Configuration");

    public List<DeadCodeItem> detectDeadCode(File repoDir, String jobId) {
        List<DeadCodeItem> deadItems = new ArrayList<>();
        Map<String, DeclInfo> allDeclarations = new HashMap<>();
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

        Set<String> reachable = new HashSet<>(entryPoints);
        reachable.addAll(calledMethods);

        for (Map.Entry<String, DeclInfo> entry : allDeclarations.entrySet()) {
            String key = entry.getKey();
            DeclInfo info = entry.getValue();
            if (!reachable.contains(key) && !isEntryAnnotated(info)) {
                deadItems.add(DeadCodeItem.builder()
                        .jobId(jobId).itemType(info.type).name(info.simpleName)
                        .qualifiedName(key).filePath(info.filePath).lineNumber(info.line)
                        .riskLevel(computeRisk(info))
                        .reason(computeReason(info, calledMethods, usedClasses))
                        .build());
            }
        }

        log.info("Dead code detection found {} candidates", deadItems.size());
        return deadItems;
    }

    private void collectDeclarations(CompilationUnit cu, String filePath,
                                      Map<String, DeclInfo> declarations, Set<String> entryPoints) {
        String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString() + ".").orElse("");
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
            String qualified = pkg + cls.getNameAsString();
            DeclInfo info = new DeclInfo("CLASS", cls.getNameAsString(), qualified, filePath,
                    cls.getBegin().map(p -> p.line).orElse(0));
            info.annotations = cls.getAnnotations().stream().map(AnnotationExpr::getNameAsString).collect(Collectors.toList());
            declarations.put(qualified, info);
            if (info.annotations.stream().anyMatch(ENTRY_ANNOTATIONS::contains)) entryPoints.add(qualified);

            cls.getMethods().forEach(method -> {
                String mKey = qualified + "#" + method.getNameAsString();
                DeclInfo mInfo = new DeclInfo("METHOD", method.getNameAsString(), mKey, filePath,
                        method.getBegin().map(p -> p.line).orElse(0));
                mInfo.annotations = method.getAnnotations().stream().map(AnnotationExpr::getNameAsString).collect(Collectors.toList());
                mInfo.isPublic = method.isPublic();
                declarations.put(mKey, mInfo);
                if (method.getNameAsString().equals("main") && method.isStatic()) entryPoints.add(mKey);
                if (mInfo.annotations.stream().anyMatch(ENTRY_ANNOTATIONS::contains)) entryPoints.add(mKey);
            });
        });
    }

    private void collectCalls(CompilationUnit cu, Set<String> calledMethods, Set<String> usedClasses) {
        cu.findAll(MethodCallExpr.class).forEach(c -> calledMethods.add(c.getNameAsString()));
        cu.findAll(ObjectCreationExpr.class).forEach(c -> usedClasses.add(c.getType().getNameAsString()));
    }

    private boolean isEntryAnnotated(DeclInfo info) {
        return info.annotations != null && info.annotations.stream().anyMatch(ENTRY_ANNOTATIONS::contains);
    }

    private String computeRisk(DeclInfo info) {
        if ("CLASS".equals(info.type)) return "HIGH";
        if (!info.isPublic) return "MEDIUM";
        return "LOW";
    }

    private String computeReason(DeclInfo info, Set<String> calledMethods, Set<String> usedClasses) {
        if ("CLASS".equals(info.type))
            return usedClasses.contains(info.simpleName) ? "Class instantiated but no reachable methods" : "No references found";
        return calledMethods.contains(info.simpleName) ? "Method name called but unresolvable" : "Never invoked in call graph";
    }

    private List<File> collectJavaFiles(File dir) {
        List<File> files = new ArrayList<>();
        collectRecursive(dir, files);
        return files;
    }

    private void collectRecursive(File dir, List<File> files) {
        if (dir == null || !dir.exists() || dir.listFiles() == null) return;
        for (File f : dir.listFiles()) {
            if (f.isDirectory() && !f.getName().equals(".git")) collectRecursive(f, files);
            else if (f.isFile() && f.getName().endsWith(".java")) files.add(f);
        }
    }

    static class DeclInfo {
        String type, simpleName, qualifiedName, filePath;
        int line;
        List<String> annotations = new ArrayList<>();
        boolean isPublic = true;
        DeclInfo(String type, String simpleName, String qualifiedName, String filePath, int line) {
            this.type = type; this.simpleName = simpleName;
            this.qualifiedName = qualifiedName; this.filePath = filePath; this.line = line;
        }
    }
}
