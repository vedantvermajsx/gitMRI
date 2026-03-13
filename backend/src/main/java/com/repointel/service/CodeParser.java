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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses all .java files in a cloned repository and extracts
 * per-method complexity metrics using JavaParser AST visitors.
 */
@Service
@Slf4j
public class CodeParser {

    private final JavaParser javaParser = new JavaParser();

    /**
     * Walk the repository directory, parse every .java file,
     * and return a list of per-method ComplexityMetric records.
     */
    public List<ComplexityMetric> parseRepository(File repoDir, String jobId) {
        List<ComplexityMetric> metrics = new ArrayList<>();
        List<File> javaFiles = collectJavaFiles(repoDir);

        log.info("Found {} Java files in {}", javaFiles.size(), repoDir.getAbsolutePath());

        for (File javaFile : javaFiles) {
            try {
                List<ComplexityMetric> fileMetrics = parseFile(javaFile, repoDir, jobId);
                metrics.addAll(fileMetrics);
            } catch (Exception e) {
                log.warn("Failed to parse {}: {}", javaFile.getName(), e.getMessage());
            }
        }

        log.info("Parsed {} method metrics from {} files", metrics.size(), javaFiles.size());
        return metrics;
    }

    private List<ComplexityMetric> parseFile(File javaFile, File repoDir, String jobId) throws Exception {
        List<ComplexityMetric> metrics = new ArrayList<>();
        String relativePath = repoDir.toPath().relativize(javaFile.toPath()).toString();

        try (FileInputStream fis = new FileInputStream(javaFile)) {
            ParseResult<CompilationUnit> result = javaParser.parse(fis);
            if (!result.isSuccessful() || result.getResult().isEmpty()) {
                return metrics;
            }

            CompilationUnit cu = result.getResult().get();
            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");

            // Count lines in the file
            long fileLines = Files.lines(javaFile.toPath()).count();

            // Visit all method declarations
            cu.findAll(MethodDeclaration.class).forEach(method -> {
                ComplexityMetric metric = analyzeMethod(method, jobId, relativePath, packageName);
                metrics.add(metric);
            });

            // Visit constructors too
            cu.findAll(ConstructorDeclaration.class).forEach(ctor -> {
                ComplexityMetric metric = analyzeConstructor(ctor, jobId, relativePath, packageName);
                metrics.add(metric);
            });
        }

        return metrics;
    }

    private ComplexityMetric analyzeMethod(MethodDeclaration method, String jobId,
                                            String filePath, String packageName) {
        CyclomaticComplexityVisitor ccVisitor = new CyclomaticComplexityVisitor();
        NestingDepthVisitor nestVisitor = new NestingDepthVisitor();

        method.accept(ccVisitor, null);
        method.accept(nestVisitor, null);

        String className = method.findAncestor(ClassOrInterfaceDeclaration.class)
                .map(c -> c.getNameAsString()).orElse("Unknown");

        int startLine = method.getBegin().map(p -> p.line).orElse(0);
        int endLine = method.getEnd().map(p -> p.line).orElse(0);
        int lineCount = endLine - startLine + 1;

        // Build method signature with params
        String methodSig = method.getNameAsString() + "(" +
                method.getParameters().stream()
                        .map(p -> p.getType().asString())
                        .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b)
                + ")";

        return ComplexityMetric.builder()
                .jobId(jobId)
                .filePath(filePath)
                .packageName(packageName)
                .className(className)
                .methodName(methodSig)
                .ccScore(ccVisitor.getComplexity())
                .lineCount(lineCount)
                .nestingDepth(nestVisitor.getMaxDepth())
                .startLine(startLine)
                .endLine(endLine)
                .build();
    }

    private ComplexityMetric analyzeConstructor(ConstructorDeclaration ctor, String jobId,
                                                  String filePath, String packageName) {
        CyclomaticComplexityVisitor ccVisitor = new CyclomaticComplexityVisitor();
        NestingDepthVisitor nestVisitor = new NestingDepthVisitor();

        ctor.accept(ccVisitor, null);
        ctor.accept(nestVisitor, null);

        int startLine = ctor.getBegin().map(p -> p.line).orElse(0);
        int endLine = ctor.getEnd().map(p -> p.line).orElse(0);

        return ComplexityMetric.builder()
                .jobId(jobId)
                .filePath(filePath)
                .packageName(packageName)
                .className(ctor.getNameAsString())
                .methodName("<init>(" +
                        ctor.getParameters().stream()
                                .map(p -> p.getType().asString())
                                .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b)
                        + ")")
                .ccScore(ccVisitor.getComplexity())
                .lineCount(endLine - startLine + 1)
                .nestingDepth(nestVisitor.getMaxDepth())
                .startLine(startLine)
                .endLine(endLine)
                .build();
    }

    private List<File> collectJavaFiles(File dir) {
        List<File> files = new ArrayList<>();
        collectJavaFilesRecursive(dir, files);
        return files;
    }

    private void collectJavaFilesRecursive(File dir, List<File> files) {
        if (dir == null || !dir.exists()) return;
        for (File f : dir.listFiles() != null ? dir.listFiles() : new File[0]) {
            if (f.isDirectory() && !f.getName().equals(".git")) {
                collectJavaFilesRecursive(f, files);
            } else if (f.isFile() && f.getName().endsWith(".java")) {
                files.add(f);
            }
        }
    }

    public int countJavaFiles(File repoDir) {
        return collectJavaFiles(repoDir).size();
    }

    public long countTotalLines(File repoDir) {
        return collectJavaFiles(repoDir).stream()
                .mapToLong(f -> {
                    try { return Files.lines(f.toPath()).count(); }
                    catch (Exception e) { return 0; }
                }).sum();
    }
}
