package com.repointel.service;

import com.repointel.model.DependencyEdge;
import com.repointel.model.DependencyNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Maven pom.xml and Gradle build files to extract the dependency graph.
 */
@Service
@Slf4j
public class DependencyAnalyzer {

    public static class DependencyResult {
        public List<DependencyNode> nodes = new ArrayList<>();
        public List<DependencyEdge> edges = new ArrayList<>();
    }

    public DependencyResult analyze(File repoDir, String jobId) {
        DependencyResult result = new DependencyResult();

        File pomFile = new File(repoDir, "pom.xml");
        File gradleFile = new File(repoDir, "build.gradle");

        if (pomFile.exists()) {
            log.info("Detected Maven project - parsing pom.xml");
            parseMaven(pomFile, repoDir, jobId, result);
        } else if (gradleFile.exists()) {
            log.info("Detected Gradle project - parsing build.gradle");
            parseGradle(gradleFile, repoDir, jobId, result);
        } else {
            log.warn("No pom.xml or build.gradle found in {}", repoDir.getAbsolutePath());
        }

        return result;
    }

    private void parseMaven(File pomFile, File repoDir, String jobId, DependencyResult result) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(pomFile);
            doc.getDocumentElement().normalize();

            // Extract project artifactId as root node
            String rootArtifact = getTextContent(doc, "artifactId", "project");
            String rootGroup = getTextContent(doc, "groupId", "project");
            String rootVersion = getTextContent(doc, "version", "project");

            DependencyNode rootNode = DependencyNode.builder()
                    .jobId(jobId)
                    .groupId(rootGroup)
                    .artifactId(rootArtifact)
                    .version(rootVersion)
                    .scope("root")
                    .nodeType("ROOT")
                    .build();
            result.nodes.add(rootNode);

            // Extract dependencies
            NodeList dependencies = doc.getElementsByTagName("dependency");
            for (int i = 0; i < dependencies.getLength(); i++) {
                Element dep = (Element) dependencies.item(i);
                String groupId = getChildText(dep, "groupId");
                String artifactId = getChildText(dep, "artifactId");
                String version = getChildText(dep, "version");
                String scope = getChildText(dep, "scope");

                if (groupId.isEmpty() || artifactId.isEmpty()) continue;
                if (scope.isEmpty()) scope = "compile";

                DependencyNode node = DependencyNode.builder()
                        .jobId(jobId)
                        .groupId(groupId)
                        .artifactId(artifactId)
                        .version(version.isEmpty() ? "managed" : version)
                        .scope(scope)
                        .nodeType("EXTERNAL")
                        .build();
                result.nodes.add(node);
            }

            log.info("Maven: found {} dependencies", result.nodes.size() - 1);

        } catch (Exception e) {
            log.error("Failed to parse pom.xml: {}", e.getMessage());
        }
    }

    private void parseGradle(File gradleFile, File repoDir, String jobId, DependencyResult result) {
        // Root node
        DependencyNode rootNode = DependencyNode.builder()
                .jobId(jobId)
                .groupId("project")
                .artifactId(repoDir.getName())
                .version("local")
                .scope("root")
                .nodeType("ROOT")
                .build();
        result.nodes.add(rootNode);

        // Regex patterns for Gradle dependency formats
        Pattern depPattern = Pattern.compile(
                "(?:implementation|compile|testImplementation|runtimeOnly|provided|api|" +
                "compileOnly|testCompile|testRuntimeOnly)\\s+" +
                "(?:['\"]([^'\"]+)['\"]|group:\\s*['\"]([^'\"]+)['\"],\\s*name:\\s*['\"]([^'\"]+)['\"])",
                Pattern.MULTILINE);

        try (BufferedReader reader = new BufferedReader(new FileReader(gradleFile))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) content.append(line).append("\n");

            Matcher matcher = depPattern.matcher(content.toString());
            while (matcher.find()) {
                String scope = extractScope(matcher.group(0));
                DependencyNode node;

                if (matcher.group(1) != null) {
                    // Format: 'group:artifact:version'
                    String[] parts = matcher.group(1).split(":");
                    if (parts.length < 2) continue;
                    node = DependencyNode.builder()
                            .jobId(jobId)
                            .groupId(parts[0])
                            .artifactId(parts[1])
                            .version(parts.length > 2 ? parts[2] : "unknown")
                            .scope(scope)
                            .nodeType("EXTERNAL")
                            .build();
                } else {
                    // Format: group: 'x', name: 'y'
                    node = DependencyNode.builder()
                            .jobId(jobId)
                            .groupId(matcher.group(2))
                            .artifactId(matcher.group(3))
                            .version("unknown")
                            .scope(scope)
                            .nodeType("EXTERNAL")
                            .build();
                }
                result.nodes.add(node);
            }

            log.info("Gradle: found {} dependencies", result.nodes.size() - 1);

        } catch (Exception e) {
            log.error("Failed to parse build.gradle: {}", e.getMessage());
        }
    }

    private String extractScope(String declaration) {
        if (declaration.startsWith("testImplementation") || declaration.startsWith("testCompile")
                || declaration.startsWith("testRuntimeOnly")) return "test";
        if (declaration.startsWith("runtimeOnly")) return "runtime";
        if (declaration.startsWith("compileOnly") || declaration.startsWith("provided")) return "provided";
        return "compile";
    }

    private String getTextContent(Document doc, String tagName, String parentTag) {
        NodeList list = doc.getElementsByTagName(tagName);
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getParentNode().getNodeName().equals(parentTag)) {
                return list.item(i).getTextContent().trim();
            }
        }
        return "";
    }

    private String getChildText(Element parent, String childTag) {
        NodeList list = parent.getElementsByTagName(childTag);
        if (list.getLength() > 0) return list.item(0).getTextContent().trim();
        return "";
    }
}
