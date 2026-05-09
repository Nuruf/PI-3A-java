package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ProjectStructureGenerator {

    private static final String OUTPUT_FILE = "project-structure.txt";
    private static final List<String> IGNORE_DIRS = List.of(
            ".git", ".idea", "target", "out", "build",
            ".vscode", ".settings", "node_modules"
    );
    private static final List<String> IGNORE_FILES = List.of(
            ".DS_Store", "Thumbs.db", ".gitignore"
    );

    public static void main(String[] args) {
        String projectRoot = System.getProperty("user.dir");
        System.out.println("📁 Analyzing project: " + projectRoot);

        try {
            generateStructure(projectRoot);
            System.out.println("✅ Structure saved to: " + OUTPUT_FILE);
            System.out.println("\n📋 You can now copy the content and share it!");
        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void generateStructure(String projectRoot) throws IOException {
        StringBuilder output = new StringBuilder();

        // Header
        output.append("=" .repeat(70)).append("\n");
        output.append("PROJECT STRUCTURE\n");
        output.append("=" .repeat(70)).append("\n\n");

        File root = new File(projectRoot);
        output.append(root.getName()).append("/\n");

        // Generate tree
        generateTree(root, "", output, true);

        // Summary
        output.append("\n").append("=".repeat(70)).append("\n");
        output.append("SUMMARY\n");
        output.append("=".repeat(70)).append("\n");
        output.append(generateSummary(projectRoot));

        // Key Files Content
        output.append("\n").append("=".repeat(70)).append("\n");
        output.append("KEY FILES CONTENT\n");
        output.append("=".repeat(70)).append("\n\n");
        output.append(extractKeyFiles(projectRoot));

        // Write to file
        try (FileWriter writer = new FileWriter(OUTPUT_FILE)) {
            writer.write(output.toString());
        }
    }

    private static void generateTree(File dir, String prefix, StringBuilder output, boolean isRoot) {
        File[] files = dir.listFiles();
        if (files == null) return;

        // Sort: directories first, then files
        List<File> dirs = new ArrayList<>();
        List<File> regularFiles = new ArrayList<>();

        for (File file : files) {
            if (shouldIgnore(file)) continue;

            if (file.isDirectory()) {
                dirs.add(file);
            } else {
                regularFiles.add(file);
            }
        }

        dirs.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        regularFiles.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        List<File> allFiles = new ArrayList<>();
        allFiles.addAll(dirs);
        allFiles.addAll(regularFiles);

        for (int i = 0; i < allFiles.size(); i++) {
            File file = allFiles.get(i);
            boolean isLast = (i == allFiles.size() - 1);

            String connector = isLast ? "└── " : "├── ";
            String newPrefix = isLast ? "    " : "│   ";

            output.append(prefix).append(connector).append(file.getName());

            if (file.isDirectory()) {
                output.append("/");
            } else {
                // Add file size
                long size = file.length();
                String sizeStr = formatFileSize(size);
                output.append(" (").append(sizeStr).append(")");
            }
            output.append("\n");

            if (file.isDirectory()) {
                generateTree(file, prefix + newPrefix, output, false);
            }
        }
    }

    private static boolean shouldIgnore(File file) {
        String name = file.getName();

        if (file.isDirectory() && IGNORE_DIRS.contains(name)) {
            return true;
        }

        if (file.isFile() && IGNORE_FILES.contains(name)) {
            return true;
        }

        // Ignore class files
        if (name.endsWith(".class")) {
            return true;
        }

        return false;
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private static String generateSummary(String projectRoot) throws IOException {
        StringBuilder summary = new StringBuilder();

        int javaFiles = 0;
        int fxmlFiles = 0;
        int xmlFiles = 0;
        int sqlFiles = 0;
        int cssFiles = 0;
        int totalFiles = 0;
        int totalDirs = 0;

        Path root = Paths.get(projectRoot);

        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.toList()) {
                File file = path.toFile();
                if (shouldIgnore(file)) continue;

                if (file.isDirectory()) {
                    totalDirs++;
                } else {
                    totalFiles++;
                    String name = file.getName().toLowerCase();

                    if (name.endsWith(".java")) javaFiles++;
                    else if (name.endsWith(".fxml")) fxmlFiles++;
                    else if (name.endsWith(".xml")) xmlFiles++;
                    else if (name.endsWith(".sql")) sqlFiles++;
                    else if (name.endsWith(".css")) cssFiles++;
                }
            }
        }

        summary.append("Total Directories: ").append(totalDirs).append("\n");
        summary.append("Total Files: ").append(totalFiles).append("\n\n");
        summary.append("File Types:\n");
        summary.append("  - Java files: ").append(javaFiles).append("\n");
        summary.append("  - FXML files: ").append(fxmlFiles).append("\n");
        summary.append("  - XML files: ").append(xmlFiles).append("\n");
        summary.append("  - SQL files: ").append(sqlFiles).append("\n");
        summary.append("  - CSS files: ").append(cssFiles).append("\n");

        return summary.toString();
    }

    private static String extractKeyFiles(String projectRoot) throws IOException {
        StringBuilder content = new StringBuilder();

        String[] keyFiles = {
                "pom.xml",
                "src/main/resources/application.properties",
                "src/main/java/module-info.java"
        };

        for (String relativePath : keyFiles) {
            Path filePath = Paths.get(projectRoot, relativePath);

            if (Files.exists(filePath)) {
                content.append(">>> ").append(relativePath).append("\n");
                content.append("-".repeat(70)).append("\n");

                try {
                    String fileContent = Files.readString(filePath);
                    // Limit to 50 lines
                    String[] lines = fileContent.split("\n");
                    int maxLines = Math.min(lines.length, 50);

                    for (int i = 0; i < maxLines; i++) {
                        content.append(lines[i]).append("\n");
                    }

                    if (lines.length > 50) {
                        content.append("... (").append(lines.length - 50).append(" more lines)\n");
                    }
                } catch (IOException e) {
                    content.append("[Error reading file: ").append(e.getMessage()).append("]\n");
                }

                content.append("\n");
            }
        }

        return content.toString();
    }
}