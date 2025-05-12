package com.helpdesk.util;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced Markdown renderer for basic formatting in JavaFX.
 * Supports bold, italic, code blocks, inline code, links, and bullet points.
 */
public class MarkdownRenderer {

    private static final Pattern PATTERN_BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*|__(.+?)__");
    private static final Pattern PATTERN_ITALIC = Pattern.compile("(?<![\\*_])\\*(?!\\*)(.+?)(?<!\\*)\\*(?![\\*_])|_(?!_)(.+?)(?<!_)_(?!_)");
    private static final Pattern PATTERN_LINK = Pattern.compile("\\[(.+?)\\]\\((.+?)\\)");
    private static final Pattern PATTERN_BULLET = Pattern.compile("^(\\s*)(\\*|-)\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern PATTERN_INLINE_CODE = Pattern.compile("(?<!`)(`{1})(?!`)(.*?)(?<!`)\\1(?!`)");

    // New pattern for code blocks with triple backticks
    private static final Pattern PATTERN_CODE_BLOCK = Pattern.compile("```(?:([a-zA-Z0-9]+)\\n)?([\\s\\S]*?)```", Pattern.MULTILINE);

    // Pattern for headers
    private static final Pattern PATTERN_HEADER = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);

    public static Node renderMarkdown(String markdown) {
        VBox container = new VBox(5);
        container.getStyleClass().add("markdown-container");

        List<CodeBlockPlaceholder> codeBlocks = new ArrayList<>();
        String processedMarkdown = extractCodeBlocks(markdown, codeBlocks);
        processedMarkdown = processHeaders(processedMarkdown);

        String[] paragraphs = processedMarkdown.split("\\n\\n+");

        for (String paragraph : paragraphs) {
            boolean handled = false;

            // Handle code block placeholders
            Matcher codePlaceholderMatcher = Pattern.compile("CODE_BLOCK_\\d+").matcher(paragraph);
            if (codePlaceholderMatcher.find()) {
                int lastIndex = 0;
                TextFlow mixedFlow = new TextFlow();

                do {
                    int start = codePlaceholderMatcher.start();
                    int end = codePlaceholderMatcher.end();
                    String before = paragraph.substring(lastIndex, start);
                    String placeholder = paragraph.substring(start, end);

                    if (!before.isEmpty()) {
                        parseAndAddFormattedText(before, mixedFlow);
                    }

                    CodeBlockPlaceholder cb = findCodeBlockPlaceholder(placeholder, codeBlocks);
                    if (cb != null) {
                        if (!mixedFlow.getChildren().isEmpty()) {
                            container.getChildren().add(mixedFlow);
                            mixedFlow = new TextFlow(); // reset
                        }

                        VBox codeContainer = new VBox(2);
                        codeContainer.setPadding(new Insets(10));
                        codeContainer.setBackground(new Background(new BackgroundFill(
                                Color.web("#2D3748"), new CornerRadii(5), Insets.EMPTY)));

                        if (cb.language != null && !cb.language.isEmpty()) {
                            Text lang = new Text(cb.language.toUpperCase());
                            lang.setFill(Color.web("#A0AEC0"));
                            lang.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
                            codeContainer.getChildren().add(new TextFlow(lang));
                        }

                        Text codeText = new Text(cb.content);
                        codeText.setFont(Font.font("Monospace", 12));
                        codeText.setFill(Color.web("#F7FAFC"));
                        TextFlow codeFlow = new TextFlow(codeText);
                        codeFlow.getStyleClass().add("code-block");

                        codeContainer.getChildren().add(codeFlow);
                        container.getChildren().add(codeContainer);
                    }

                    lastIndex = end;
                } while (codePlaceholderMatcher.find());

                if (lastIndex < paragraph.length()) {
                    String after = paragraph.substring(lastIndex);
                    parseAndAddFormattedText(after, mixedFlow);
                }

                if (!mixedFlow.getChildren().isEmpty()) {
                    container.getChildren().add(mixedFlow);
                }

                continue;
            }

            // Bullet list
            if (paragraph.trim().matches("(?s)^(\\s*)(\\*|-)\\s+.*")) {
                VBox listBox = new VBox(3);
                listBox.setPadding(new Insets(0, 0, 0, 20));

                Matcher bulletMatcher = PATTERN_BULLET.matcher(paragraph);
                while (bulletMatcher.find()) {
                    String bulletText = bulletMatcher.group(3);
                    TextFlow bulletFlow = new TextFlow(new Text("• "));
                    parseAndAddFormattedText(bulletText, bulletFlow);
                    listBox.getChildren().add(bulletFlow);
                }

                container.getChildren().add(listBox);
                continue;
            }

            // Headers
            if (paragraph.startsWith("<h")) {
                int level = Character.getNumericValue(paragraph.charAt(2));
                String headerText = paragraph.substring(4);
                Text header = new Text(headerText);
                header.setFont(Font.font(null, FontWeight.BOLD, 18 - (level - 1) * 2));
                header.getStyleClass().add("markdown-header");
                TextFlow headerFlow = new TextFlow(header);
                headerFlow.setPadding(new Insets(5, 0, 5, 0));
                container.getChildren().add(headerFlow);
                continue;
            }

            // Plain paragraph
            TextFlow paragraphFlow = new TextFlow();
            parseAndAddFormattedText(paragraph, paragraphFlow);
            container.getChildren().add(paragraphFlow);
        }

        return container;
    }

    private static String extractCodeBlocks(String markdown, List<CodeBlockPlaceholder> codeBlocks) {
        StringBuilder processed = new StringBuilder();
        Matcher matcher = PATTERN_CODE_BLOCK.matcher(markdown);
        int lastEnd = 0;
        int placeholderCount = 0;

        while (matcher.find()) {
            processed.append(markdown, lastEnd, matcher.start());

            String language = matcher.group(1);
            String content = matcher.group(2);

            // Create placeholder and add to list
            String placeholder = String.format("CODE_BLOCK_%d", placeholderCount++);
            codeBlocks.add(new CodeBlockPlaceholder(placeholder, content, language));

            processed.append(placeholder);
            lastEnd = matcher.end();
        }

        if (lastEnd < markdown.length()) {
            processed.append(markdown.substring(lastEnd));
        }

        return processed.toString();
    }

    private static String processHeaders(String markdown) {
        StringBuilder processed = new StringBuilder();
        Matcher matcher = PATTERN_HEADER.matcher(markdown);
        int lastEnd = 0;

        while (matcher.find()) {
            processed.append(markdown, lastEnd, matcher.start());

            String headerMarkers = matcher.group(1);
            String headerContent = matcher.group(2);
            int level = headerMarkers.length();

            processed.append(String.format("<h%d>%s", level, headerContent));
            lastEnd = matcher.end();
        }

        if (lastEnd < markdown.length()) {
            processed.append(markdown.substring(lastEnd));
        }

        return processed.toString();
    }

    private static CodeBlockPlaceholder findCodeBlockPlaceholder(String text, List<CodeBlockPlaceholder> codeBlocks) {
        for (CodeBlockPlaceholder placeholder : codeBlocks) {
            if (text.equals(placeholder.placeholder)) {
                return placeholder;
            }
        }
        return null;
    }

    private static void parseAndAddFormattedText(String text, TextFlow parent) {
        List<TextSegment> segments = new ArrayList<>();
        segments.add(new TextSegment(0, text.length(), text, TextType.PLAIN));

        // Apply patterns in a specific order to handle overlapping
        applyPatternToSegments(segments, PATTERN_BOLD, TextType.BOLD);
        applyPatternToSegments(segments, PATTERN_ITALIC, TextType.ITALIC);
        applyPatternToSegments(segments, PATTERN_INLINE_CODE, TextType.CODE);
        applyPatternToSegments(segments, PATTERN_LINK, TextType.LINK);

        segments.sort((a, b) -> Integer.compare(a.start, b.start));

        for (TextSegment segment : segments) {
            if (segment.type != TextType.REMOVED) {
                Node node = createNodeForSegment(segment);
                if (node != null) {
                    parent.getChildren().add(node);
                }
            }
        }
    }

    private static void applyPatternToSegments(List<TextSegment> segments, Pattern pattern, TextType type) {
        List<TextSegment> newSegments = new ArrayList<>();

        for (TextSegment segment : segments) {
            if (segment.type != TextType.PLAIN) {
                newSegments.add(segment);
                continue;
            }

            String text = segment.text;
            Matcher matcher = pattern.matcher(text);
            int lastEnd = 0;

            while (matcher.find()) {
                int matchStart = matcher.start();
                int matchEnd = matcher.end();

                if (matchStart > lastEnd) {
                    newSegments.add(new TextSegment(
                            segment.start + lastEnd,
                            segment.start + matchStart,
                            text.substring(lastEnd, matchStart),
                            TextType.PLAIN
                    ));
                }

                try {
                    switch (type) {
                        case LINK:
                            if (matcher.groupCount() >= 2) {
                                String linkText = matcher.group(1);
                                String url = matcher.group(2);
                                newSegments.add(new TextSegment(
                                        segment.start + matchStart,
                                        segment.start + matchEnd,
                                        linkText,
                                        TextType.LINK,
                                        url
                                ));
                            }
                            break;
                        case CODE:
                            if (matcher.groupCount() >= 2) {
                                String code = matcher.group(2) != null ? matcher.group(2) : matcher.group(1);
                                newSegments.add(new TextSegment(
                                        segment.start + matchStart,
                                        segment.start + matchEnd,
                                        code,
                                        TextType.CODE
                                ));
                            }
                            break;
                        case BOLD:
                            String boldContent = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                            newSegments.add(new TextSegment(
                                    segment.start + matchStart,
                                    segment.start + matchEnd,
                                    boldContent,
                                    TextType.BOLD
                            ));
                            break;
                        case ITALIC:
                            String italicContent = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                            newSegments.add(new TextSegment(
                                    segment.start + matchStart,
                                    segment.start + matchEnd,
                                    italicContent,
                                    TextType.ITALIC
                            ));
                            break;
                        default:
                            newSegments.add(new TextSegment(
                                    segment.start + matchStart,
                                    segment.start + matchEnd,
                                    matcher.group(),
                                    TextType.PLAIN
                            ));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    newSegments.add(new TextSegment(
                            segment.start + matchStart,
                            segment.start + matchEnd,
                            matcher.group(),
                            TextType.PLAIN
                    ));
                }

                lastEnd = matchEnd;
            }

            if (lastEnd < text.length()) {
                newSegments.add(new TextSegment(
                        segment.start + lastEnd,
                        segment.start + text.length(),
                        text.substring(lastEnd),
                        TextType.PLAIN
                ));
            }
        }

        segments.clear();
        segments.addAll(newSegments);
    }

    private static Node createNodeForSegment(TextSegment segment) {
        switch (segment.type) {
            case BOLD:
                Text bold = new Text(segment.text);
                bold.setFont(Font.font(null, FontWeight.BOLD, -1));
                bold.getStyleClass().add("markdown-text");
                return bold;

            case ITALIC:
                Text italic = new Text(segment.text);
                italic.setFont(Font.font(null, FontPosture.ITALIC, -1));
                italic.getStyleClass().add("markdown-text");
                return italic;

            case CODE:
                Text code = new Text(segment.text);
                code.getStyleClass().add("code-text");
                code.setFont(Font.font("Monospace", -1));
                return code;

            case LINK:
                Hyperlink link = new Hyperlink(segment.text);
                link.setOnAction(e -> {
                    try {
                        Desktop.getDesktop().browse(new URI(segment.url));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                return link;

            case PLAIN:
                Text plain = new Text(segment.text);
                plain.getStyleClass().add("markdown-text");
                return plain;

            default:
                return null;
        }
    }

    private enum TextType {
        PLAIN,
        BOLD,
        ITALIC,
        CODE,
        LINK,
        REMOVED
    }

    private static class TextSegment {
        final int start;
        final int end;
        final String text;
        final TextType type;
        final String url;

        TextSegment(int start, int end, String text, TextType type) {
            this(start, end, text, type, null);
        }

        TextSegment(int start, int end, String text, TextType type, String url) {
            this.start = start;
            this.end = end;
            this.text = text;
            this.type = type;
            this.url = url;
        }
    }

    private static class CodeBlockPlaceholder {
        final String placeholder;
        final String content;
        final String language;

        CodeBlockPlaceholder(String placeholder, String content, String language) {
            this.placeholder = placeholder;
            this.content = content;
            this.language = language;
        }
    }
}
