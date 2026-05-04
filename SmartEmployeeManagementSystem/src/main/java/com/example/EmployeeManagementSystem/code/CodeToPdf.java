package com.example.EmployeeManagementSystem.code;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.font.constants.StandardFonts;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * CodeToPdf - Extracts all source code files from the current, parent,
 * and immediate child directories into a formatted PDF document.
 *
 * Dependencies (add to your pom.xml or build.gradle):
 *   com.itextpdf:itext7-core:7.2.5  (or itext7 bundle)
 *
 * Compile:
 *   javac -cp itext7-core-7.2.5.jar:. CodeToPdf.java
 *
 * Run:
 *   java -cp itext7-core-7.2.5.jar:. CodeToPdf
 *
 * Or with Maven:
 *   mvn compile exec:java -Dexec.mainClass=CodeToPdf
 */
public class CodeToPdf {

    // ── Configuration ─────────────────────────────────────────────────────────

    /** Output PDF filename */
    private static final String OUTPUT_FILE = "code_export.pdf";

    /** Maximum file size to include (skip very large generated files) */
    private static final long MAX_FILE_SIZE_BYTES = 500_000; // 500 KB

    /** Source code file extensions to include */
    private static final Set<String> CODE_EXTENSIONS = new LinkedHashSet<>(Arrays.asList(
            // JVM languages
            ".java", ".kt", ".groovy", ".scala", ".clj",
            // Web
            ".js", ".ts", ".jsx", ".tsx", ".html", ".css", ".scss", ".less",
            // Python / Ruby / PHP
            ".py", ".rb", ".php",
            // Systems / low-level
            ".c", ".cpp", ".cc", ".h", ".hpp", ".rs", ".go", ".swift",
            // Data / config
            ".xml", ".json", ".yaml", ".yml", ".toml", ".properties", ".env",
            // Shell / scripts
            ".sh", ".bash", ".zsh", ".bat", ".ps1",
            // Build
            ".gradle", ".cmake", "Makefile", "Dockerfile",
            // Other
            ".sql", ".md", ".txt"
    ));

    /** Directories to skip entirely */
    private static final Set<String> SKIP_DIRS = new HashSet<>(Arrays.asList(
            ".git", ".svn", ".hg", "node_modules", "target", "build",
            "out", ".idea", ".vscode", "__pycache__", ".gradle", "dist", "vendor"
    ));

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final DeviceRgb COLOR_HEADER_BG    = new DeviceRgb(0x18, 0x5F, 0xA5); // blue
    private static final DeviceRgb COLOR_HEADER_TEXT  = new DeviceRgb(0xFF, 0xFF, 0xFF);
    private static final DeviceRgb COLOR_FILE_TITLE   = new DeviceRgb(0x18, 0x5F, 0xA5);
    private static final DeviceRgb COLOR_CODE_BG      = new DeviceRgb(0xF7, 0xF7, 0xF7);
    private static final DeviceRgb COLOR_LINE_NUM     = new DeviceRgb(0x99, 0x99, 0x99);
    private static final DeviceRgb COLOR_TOC_HEADER   = new DeviceRgb(0x18, 0x5F, 0xA5);

    // ── Font sizes ────────────────────────────────────────────────────────────
    private static final float FONT_TITLE   = 20f;
    private static final float FONT_SECTION = 13f;
    private static final float FONT_FILE    = 11f;
    private static final float FONT_CODE    =  7.5f;
    private static final float FONT_TOC     = 10f;

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<FileEntry> fileEntries = new ArrayList<>();
    private int totalLines = 0;

    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        new CodeToPdf().run();
    }

    private void run() throws Exception {
        Path currentDir = Paths.get("").toAbsolutePath();
        Path parentDir  = currentDir.getParent();

        System.out.println("=== Code to PDF Extractor ===");
        System.out.println("Current directory : " + currentDir);
        System.out.println("Parent  directory : " + (parentDir != null ? parentDir : "(none)"));
        System.out.println();

        // 1. Collect files ────────────────────────────────────────────────────

        // Parent directory (top-level files only, not recursive)
        if (parentDir != null && Files.exists(parentDir)) {
            collectFiles(parentDir, false, "Parent");
        }

        // Current directory (recursive — includes all child subdirectories)
        collectFiles(currentDir, true, "Current");

        System.out.printf("%nFound %d code file(s) with %,d total lines.%n",
                fileEntries.size(), totalLines);

        if (fileEntries.isEmpty()) {
            System.out.println("No code files found. Exiting.");
            return;
        }

        // 2. Generate PDF ─────────────────────────────────────────────────────
        generatePdf(currentDir);

        System.out.println("PDF saved to: " + Paths.get(OUTPUT_FILE).toAbsolutePath());
    }

    // ── File Collection ───────────────────────────────────────────────────────

    private void collectFiles(Path root, boolean recursive, String scope) throws IOException {
        if (!Files.exists(root)) return;

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
                // Never recurse into skip-dirs; always allow the root itself
                if (!dir.equals(root) && SKIP_DIRS.contains(name)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                // If not recursive, skip any subdirectory of root
                if (!recursive && !dir.equals(root)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isCodeFile(file) && attrs.size() <= MAX_FILE_SIZE_BYTES) {
                    String content = Files.readString(file);
                    int lines = content.split("\n", -1).length;
                    String relativePath = scope + ": " + root.relativize(file);
                    fileEntries.add(new FileEntry(relativePath, file, content, lines));
                    totalLines += lines;
                    System.out.println("  + " + relativePath + "  (" + lines + " lines)");
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean isCodeFile(Path file) {
        String name = file.getFileName().toString();
        // Check for exact filename match (e.g. "Makefile", "Dockerfile")
        if (CODE_EXTENSIONS.contains(name)) return true;
        // Check extension
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            return CODE_EXTENSIONS.contains(name.substring(dot));
        }
        return false;
    }

    // ── PDF Generation ────────────────────────────────────────────────────────

    private void generatePdf(Path currentDir) throws IOException {
        PdfWriter writer   = new PdfWriter(OUTPUT_FILE);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc       = new Document(pdfDoc, PageSize.A4);
        doc.setMargins(40, 40, 40, 40);

        PdfFont sansFont  = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont boldFont  = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont monoFont  = PdfFontFactory.createFont(StandardFonts.COURIER);

        // Cover page ──────────────────────────────────────────────────────────
        addCoverPage(doc, sansFont, boldFont, currentDir);
        doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        // Table of contents ───────────────────────────────────────────────────
        addTableOfContents(doc, sansFont, boldFont);
        doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        // Code sections ───────────────────────────────────────────────────────
        for (int i = 0; i < fileEntries.size(); i++) {
            FileEntry entry = fileEntries.get(i);
            addCodeSection(doc, entry, i + 1, sansFont, boldFont, monoFont);
            if (i < fileEntries.size() - 1) {
                doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            }
        }

        doc.close();
    }

    private void addCoverPage(Document doc, PdfFont sansFont, PdfFont boldFont, Path dir)
            throws IOException {

        doc.add(new Paragraph("\n\n\n"));

        // Big title
        doc.add(new Paragraph("Code Export")
                .setFont(boldFont).setFontSize(FONT_TITLE + 10)
                .setFontColor(COLOR_FILE_TITLE)
                .setMarginBottom(6));

        doc.add(new Paragraph("Source Code PDF Report")
                .setFont(sansFont).setFontSize(FONT_SECTION)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setMarginBottom(24));

        // Stats table
        Table stats = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                .setWidth(UnitValue.createPercentValue(80));
        addStatRow(stats, "Generated",    LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), sansFont, boldFont);
        addStatRow(stats, "Root directory", dir.toString(), sansFont, boldFont);
        addStatRow(stats, "Files found",  String.valueOf(fileEntries.size()), sansFont, boldFont);
        addStatRow(stats, "Total lines",  String.format("%,d", totalLines), sansFont, boldFont);
        doc.add(stats);
    }

    private void addStatRow(Table t, String label, String value,
                            PdfFont sans, PdfFont bold) {
        t.addCell(new Cell().add(new Paragraph(label)
                        .setFont(bold).setFontSize(10)
                        .setFontColor(ColorConstants.DARK_GRAY))
                .setBorder(null).setPaddingBottom(6));
        t.addCell(new Cell().add(new Paragraph(value)
                        .setFont(sans).setFontSize(10))
                .setBorder(null).setPaddingBottom(6));
    }

    private void addTableOfContents(Document doc, PdfFont sansFont, PdfFont boldFont)
            throws IOException {
        doc.add(new Paragraph("Table of Contents")
                .setFont(boldFont).setFontSize(FONT_SECTION + 2)
                .setFontColor(COLOR_TOC_HEADER)
                .setMarginBottom(12));

        for (int i = 0; i < fileEntries.size(); i++) {
            FileEntry entry = fileEntries.get(i);
            String label = String.format("%d.  %s  (%,d lines)",
                    i + 1, entry.relativePath, entry.lineCount);
            doc.add(new Paragraph(label)
                    .setFont(sansFont).setFontSize(FONT_TOC)
                    .setMarginBottom(3));
        }
    }

    private void addCodeSection(Document doc, FileEntry entry, int index,
                                PdfFont sansFont, PdfFont boldFont, PdfFont monoFont)
            throws IOException {

        // Section header bar
        Table header = new Table(UnitValue.createPercentArray(new float[]{5, 75, 20}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(8);

        header.addCell(new Cell().add(new Paragraph(String.valueOf(index))
                        .setFont(boldFont).setFontSize(FONT_FILE)
                        .setFontColor(COLOR_HEADER_TEXT))
                .setBackgroundColor(COLOR_HEADER_BG)
                .setBorder(null).setPadding(6));

        header.addCell(new Cell().add(new Paragraph(entry.relativePath)
                        .setFont(boldFont).setFontSize(FONT_FILE)
                        .setFontColor(COLOR_HEADER_TEXT))
                .setBackgroundColor(COLOR_HEADER_BG)
                .setBorder(null).setPadding(6));

        header.addCell(new Cell().add(new Paragraph(entry.lineCount + " lines")
                        .setFont(sansFont).setFontSize(FONT_FILE - 1)
                        .setFontColor(COLOR_HEADER_TEXT))
                .setBackgroundColor(COLOR_HEADER_BG)
                .setBorder(null).setPadding(6));

        doc.add(header);

        // Code block with line numbers
        String[] lines = entry.content.split("\n", -1);
        int padWidth = String.valueOf(lines.length).length();

        for (int ln = 0; ln < lines.length; ln++) {
            String lineNum = String.format("%" + padWidth + "d", ln + 1);
            String codeLine = lines[ln]
                    .replace("\t", "    ")   // expand tabs
                    .replace("\r", "");       // strip CR

            // Truncate very long lines
            if (codeLine.length() > 200) {
                codeLine = codeLine.substring(0, 197) + "...";
            }

            Text numText  = new Text(lineNum + "  ").setFont(monoFont)
                    .setFontSize(FONT_CODE).setFontColor(COLOR_LINE_NUM);
            Text codeText = new Text(codeLine).setFont(monoFont)
                    .setFontSize(FONT_CODE).setFontColor(ColorConstants.BLACK);

            Paragraph p = new Paragraph().add(numText).add(codeText)
                    .setBackgroundColor(ln % 2 == 0 ? COLOR_CODE_BG : ColorConstants.WHITE)
                    .setMargin(0).setPaddingLeft(4).setPaddingRight(4)
                    .setMultipliedLeading(1.3f)
                    .setFixedLeading(FONT_CODE + 2.5f);

            doc.add(p);
        }
    }

    // ── Data class ────────────────────────────────────────────────────────────

    private static class FileEntry {
        final String relativePath;
        final Path   absolutePath;
        final String content;
        final int    lineCount;

        FileEntry(String relativePath, Path absolutePath, String content, int lineCount) {
            this.relativePath = relativePath;
            this.absolutePath = absolutePath;
            this.content      = content;
            this.lineCount    = lineCount;
        }
    }
}
