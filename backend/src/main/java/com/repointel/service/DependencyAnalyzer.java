package com.repointel.service;

import com.repointel.model.DependencyNode;
import com.repointel.model.DependencyEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Finds pom.xml, build.gradle, and package.json files anywhere in the repo tree
 * (not just the root), then extracts all declared dependencies.
 */
@Service
public class DependencyAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(DependencyAnalyzer.class);

    public static class DependencyResult {
        public final List<DependencyNode> nodes = new ArrayList<>();
        public final List<DependencyEdge> edges = new ArrayList<>();
    }

    public DependencyResult analyze(File repoDir, String jobId) {
        DependencyResult result = new DependencyResult();

        // Walk full tree for all build files (skip .git)
        List<File> pomFiles    = findFiles(repoDir, "pom.xml");
        List<File> gradleFiles = findFiles(repoDir, "build.gradle");
        List<File> pkgFiles    = findFiles(repoDir, "package.json");

        log.info("Found {} pom.xml, {} build.gradle, {} package.json files",
                pomFiles.size(), gradleFiles.size(), pkgFiles.size());

        for (File pom : pomFiles)    parseMaven(pom,    repoDir, jobId, result);
        for (File g   : gradleFiles) parseGradle(g,     repoDir, jobId, result);
        for (File pkg : pkgFiles)    parsePackageJson(pkg, repoDir, jobId, result);

        if (result.nodes.isEmpty()) {
            log.warn("No dependency files found anywhere in the repo");
        } else {
            log.info("Total dependency nodes found: {}", result.nodes.size());
        }
        return result;
    }

    // ── Maven ───────────────────────────────────────────────────────────────

    private void parseMaven(File pomFile, File repoDir, String jobId, DependencyResult result) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(pomFile);
            doc.getDocumentElement().normalize();

            String relPath = repoDir.toPath().relativize(pomFile.getParentFile().toPath()).toString();
            String rootArtifact = getChildTextFromParent(doc, "artifactId", "project");
            String rootGroup    = getChildTextFromParent(doc, "groupId",    "project");
            String rootVersion  = getChildTextFromParent(doc, "version",    "project");
            String label = rootArtifact.isEmpty() ? relPath : rootArtifact;

            DependencyNode root = DependencyNode.builder()
                    .jobId(jobId).groupId(rootGroup).artifactId(label)
                    .version(rootVersion).scope("root").nodeType("ROOT")
                    .build();
            result.nodes.add(root);

            NodeList deps = doc.getElementsByTagName("dependency");
            int count = 0;
            for (int i = 0; i < deps.getLength(); i++) {
                Element dep = (Element) deps.item(i);
                String groupId    = getChildText(dep, "groupId");
                String artifactId = getChildText(dep, "artifactId");
                String version    = getChildText(dep, "version");
                String scope      = getChildText(dep, "scope");
                if (groupId.isEmpty() || artifactId.isEmpty()) continue;
                if (scope.isEmpty()) scope = "compile";
                result.nodes.add(DependencyNode.builder()
                        .jobId(jobId).groupId(groupId).artifactId(artifactId)
                        .version(version.isEmpty() ? "managed" : version)
                        .scope(scope).nodeType("EXTERNAL").build());
                count++;
            }
            log.info("Maven [{}]: {} dependencies", relPath, count);
        } catch (Exception e) {
            log.error("Failed to parse {}: {}", pomFile.getAbsolutePath(), e.getMessage());
        }
    }

    // ── Gradle ──────────────────────────────────────────────────────────────

    private void parseGradle(File gradleFile, File repoDir, String jobId, DependencyResult result) {
        String relPath = repoDir.toPath().relativize(gradleFile.getParentFile().toPath()).toString();
        String label   = relPath.isEmpty() ? gradleFile.getParentFile().getName() : relPath;

        result.nodes.add(DependencyNode.builder()
                .jobId(jobId).groupId("project").artifactId(label)
                .version("local").scope("root").nodeType("ROOT").build());

        // Support both string notation and map notation
        Pattern depPattern = Pattern.compile(
                "(?:implementation|compile|testImplementation|runtimeOnly|provided|api|" +
                "compileOnly|testCompile|testRuntimeOnly|annotationProcessor|kapt)" +
                "\\s*(?:\\(['\"]([^'\"]+)['\"]\\)|['\"]([^'\"]+)['\"])",
                Pattern.MULTILINE);

        try {
            String content = Files.readString(gradleFile.toPath());
            Matcher m = depPattern.matcher(content);
            int count = 0;
            while (m.find()) {
                String decl = m.group(1) != null ? m.group(1) : m.group(2);
                if (decl == null) continue;
                String[] parts = decl.split(":");
                if (parts.length < 2) continue;
                String scope = extractGradleScope(m.group(0));
                result.nodes.add(DependencyNode.builder()
                        .jobId(jobId).groupId(parts[0]).artifactId(parts[1])
                        .version(parts.length > 2 ? parts[2] : "unknown")
                        .scope(scope).nodeType("EXTERNAL").build());
                count++;
            }
            log.info("Gradle [{}]: {} dependencies", relPath, count);
        } catch (Exception e) {
            log.error("Failed to parse {}: {}", gradleFile.getAbsolutePath(), e.getMessage());
        }
    }

    // ── package.json ────────────────────────────────────────────────────────

    private void parsePackageJson(File pkgFile, File repoDir, String jobId, DependencyResult result) {
        String relPath = repoDir.toPath().relativize(pkgFile.getParentFile().toPath()).toString();
        try {
            String content = Files.readString(pkgFile.toPath());

            // Skip node_modules
            if (pkgFile.getAbsolutePath().contains("node_modules")) return;

            // Extract "name" field
            String pkgName = extractJsonString(content, "name");
            if (pkgName.isEmpty()) pkgName = relPath.isEmpty() ? "frontend" : relPath;

            result.nodes.add(DependencyNode.builder()
                    .jobId(jobId).groupId("npm").artifactId(pkgName)
                    .version(extractJsonString(content, "version"))
                    .scope("root").nodeType("ROOT").build());

            // Parse dependencies and devDependencies
            int count = 0;
            count += extractNpmDeps(content, "\"dependencies\"",    "compile", jobId, pkgName, result);
            count += extractNpmDeps(content, "\"devDependencies\"", "dev",     jobId, pkgName, result);
            count += extractNpmDeps(content, "\"peerDependencies\"","peer",    jobId, pkgName, result);

            log.info("npm [{}]: {} dependencies", relPath, count);
        } catch (Exception e) {
            log.error("Failed to parse {}: {}", pkgFile.getAbsolutePath(), e.getMessage());
        }
    }

    private int extractNpmDeps(String content, String block, String scope,
                                String jobId, String pkgName, DependencyResult result) {
        int start = content.indexOf(block);
        if (start < 0) return 0;
        int brace = content.indexOf('{', start);
        if (brace < 0) return 0;
        int end = findClosingBrace(content, brace);
        if (end < 0) return 0;

        String section = content.substring(brace + 1, end);
        Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(section);
        int count = 0;
        while (m.find()) {
            String name    = m.group(1);
            String version = m.group(2);
            // group = @scope if scoped, else "npm"
            String group = name.startsWith("@") ? name.substring(0, name.indexOf('/')) : "npm";
            String artifact = name.startsWith("@") ? name.substring(name.indexOf('/') + 1) : name;
            result.nodes.add(DependencyNode.builder()
                    .jobId(jobId).groupId(group).artifactId(artifact)
                    .version(version).scope(scope).nodeType("EXTERNAL").build());
            count++;
        }
        return count;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private List<File> findFiles(File root, String filename) {
        List<File> found = new ArrayList<>();
        findRecursive(root, filename, found);
        return found;
    }

    private void findRecursive(File dir, String filename, List<File> found) {
        if (dir == null || !dir.exists()) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) {
                String name = f.getName();
                // Skip hidden dirs, node_modules, build output
                if (!name.startsWith(".") && !name.equals("node_modules")
                        && !name.equals("target") && !name.equals("build")
                        && !name.equals("dist") && !name.equals(".gradle")) {
                    findRecursive(f, filename, found);
                }
            } else if (f.getName().equals(filename)) {
                found.add(f);
            }
        }
    }

    private String extractGradleScope(String decl) {
        if (decl.contains("testImplementation") || decl.contains("testCompile") || decl.contains("testRuntimeOnly")) return "test";
        if (decl.contains("runtimeOnly")) return "runtime";
        if (decl.contains("compileOnly") || decl.contains("provided")) return "provided";
        if (decl.contains("annotationProcessor") || decl.contains("kapt")) return "processor";
        return "compile";
    }

    private String extractJsonString(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private int findClosingBrace(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            if (s.charAt(i) == '{') depth++;
            else if (s.charAt(i) == '}') { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    private String getChildTextFromParent(Document doc, String tag, String parentTag) {
        NodeList list = doc.getElementsByTagName(tag);
        for (int i = 0; i < list.getLength(); i++)
            if (list.item(i).getParentNode().getNodeName().equals(parentTag))
                return list.item(i).getTextContent().trim();
        return "";
    }

    private String getChildText(Element parent, String childTag) {
        NodeList list = parent.getElementsByTagName(childTag);
        return list.getLength() > 0 ? list.item(0).getTextContent().trim() : "";
    }
}
