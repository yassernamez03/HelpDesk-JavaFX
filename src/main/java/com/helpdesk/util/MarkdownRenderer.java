package com.helpdesk.util;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
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
 * Simple utility class to render basic Markdown formatting in JavaFX
 */
public class MarkdownRenderer {

    // Pattern for bold text: **text** or __text__
    private static final Pattern PATTERN_BOLD = Pattern.compile("(\\*\\*|__)(.+?)(\\*\\*|__)");

    // Pattern for italic text: *text* or _text_
    private static final Pattern PATTERN_ITALIC = Pattern.compile("(\\*|_)(.+?)(\\*|_)");

    // Pattern for links: [text](url)
    private static final Pattern PATTERN_LINK = Pattern.compile("\\[(.+?)\\]\\((.+?)\\)");

    // Pattern for bullet points: * item or - item
    private static final Pattern PATTERN_BULLET = Pattern.compile("^(\\s*)(\\*|-)\\s+(.+)$", Pattern.MULTILINE);

    // Pattern for code blocks: `code`
    private static final Pattern PATTERN_CODE = Pattern.compile("`([^`]+?)`");

    /**
     * Renders a markdown string to a styled JavaFX node
     *
     * @param markdown The markdown text to render
     * @return A Node containing the styled text
     */
    public static Node renderMarkdown(String markdown) {
        VBox container = new VBox(5);
        container.getStyleClass().add("markdown-container");

        // Split by double newlines to handle paragraphs
        String[] paragraphs = markdown.split("\n\n+");

        for (String paragraph : paragraphs) {
            // Handle bullet lists
            if (paragraph.trim().matches("(?s)^(\\s*)(\\*|-)\\s+.*")) {
                VBox listContainer = new VBox(3);
                listContainer.setPadding(new Insets(0, 0, 0, 20));

                Matcher bulletMatcher = PATTERN_BULLET.matcher(paragraph);
                while (bulletMatcher.find()) {
                    String bulletText = bulletMatcher.group(3);
                    TextFlow bulletPoint = new TextFlow();

                    Text bullet = new Text("• ");
                    bulletPoint.getChildren().add(bullet);

                    // Process formatting inside bullet point
                    parseAndAddFormattedText(bulletText, bulletPoint);

                    listContainer.getChildren().add(bulletPoint);
                }

                container.getChildren().add(listContainer);
            } else {
                // Normal paragraph
                TextFlow textFlow = new TextFlow();
                parseAndAddFormattedText(paragraph, textFlow);
                container.getChildren().add(textFlow);
            }
        }

        return container;
    }

    private static void parseAndAddFormattedText(String text, TextFlow parent) {
        List<TextSegment> segments = new ArrayList<>();
        segments.add(new TextSegment(0, text.length(), text, TextType.PLAIN));

        // Process bold
        applyPatternToSegments(segments, PATTERN_BOLD, TextType.BOLD);

        // Process italic
        applyPatternToSegments(segments, PATTERN_ITALIC, TextType.ITALIC);

        // Process code
        applyPatternToSegments(segments, PATTERN_CODE, TextType.CODE);

        // Process links
        applyPatternToSegments(segments, PATTERN_LINK, TextType.LINK);

        // Sort segments by start position
        segments.sort((a, b) -> Integer.compare(a.start, b.start));

        // Create nodes for each segment
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
            boolean found = false;

            while (matcher.find()) {
                found = true;
                int fullStart = matcher.start();
                int fullEnd = matcher.end();

                if (fullStart > lastEnd) {
                    newSegments.add(new TextSegment(
                            segment.start + lastEnd,
                            segment.start + fullStart,
                            text.substring(lastEnd, fullStart),
                            TextType.PLAIN
                    ));
                }

                if (type == TextType.LINK) {
                    String linkText = matcher.group(1);
                    String url = matcher.group(2);
                    newSegments.add(new TextSegment(
                            segment.start + fullStart,
                            segment.start + fullEnd,
                            linkText,
                            TextType.LINK,
                            url
                    ));
                } else {
                    // For BOLD, ITALIC, CODE
                    String content = matcher.group(2);
                    newSegments.add(new TextSegment(
                            segment.start + fullStart,
                            segment.start + fullEnd,
                            content,
                            type
                    ));
                }

                lastEnd = fullEnd;
            }

            if (!found) {
                newSegments.add(segment);
            } else if (lastEnd < text.length()) {
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
                Text boldText = new Text(segment.text);
                boldText.setFont(Font.font(null, FontWeight.BOLD, -1));
                boldText.getStyleClass().add("markdown-text");
                return boldText;

            case ITALIC:
                Text italicText = new Text(segment.text);
                italicText.setFont(Font.font(null, FontPosture.ITALIC, -1));
                italicText.getStyleClass().add("markdown-text");
                return italicText;

            case CODE:
                Text codeText = new Text(segment.text);
                codeText.getStyleClass().add("code-text");
                return codeText;

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
                Text plainText = new Text(segment.text);
                plainText.getStyleClass().add("markdown-text");
                return plainText;

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
}