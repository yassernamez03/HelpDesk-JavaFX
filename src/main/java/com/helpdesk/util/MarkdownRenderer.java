package com.helpdesk.util;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
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
 * Enhanced Markdown renderer for basic formatting in JavaFX.
 * Supports bold, italic, code, links, and bullet points.
 */
public class MarkdownRenderer {

    private static final Pattern PATTERN_BOLD = Pattern.compile("(\\*\\*|__)(.+?)(\\*\\*|__)");
    private static final Pattern PATTERN_ITALIC = Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)|_(.+?)_");
    private static final Pattern PATTERN_LINK = Pattern.compile("\\[(.+?)\\]\\((.+?)\\)");
    private static final Pattern PATTERN_BULLET = Pattern.compile("^(\\s*)(\\*|-)\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern PATTERN_CODE = Pattern.compile("`([^`]+?)`");

    public static Node renderMarkdown(String markdown) {
        VBox container = new VBox(5);
        container.getStyleClass().add("markdown-container");

        String[] paragraphs = markdown.split("\\n\\n+");
        for (String paragraph : paragraphs) {
            if (paragraph.trim().matches("(?s)^(\\s*)(\\*|-)\\s+.*")) {
                VBox listContainer = new VBox(3);
                listContainer.setPadding(new Insets(0, 0, 0, 20));

                Matcher bulletMatcher = PATTERN_BULLET.matcher(paragraph);
                while (bulletMatcher.find()) {
                    String bulletText = bulletMatcher.group(3);
                    TextFlow bulletFlow = new TextFlow();
                    bulletFlow.getChildren().add(new Text("• "));
                    parseAndAddFormattedText(bulletText, bulletFlow);
                    listContainer.getChildren().add(bulletFlow);
                }

                container.getChildren().add(listContainer);
            } else {
                TextFlow paragraphFlow = new TextFlow();
                parseAndAddFormattedText(paragraph, paragraphFlow);
                container.getChildren().add(paragraphFlow);
            }
        }

        return container;
    }

    private static void parseAndAddFormattedText(String text, TextFlow parent) {
        List<TextSegment> segments = new ArrayList<>();
        segments.add(new TextSegment(0, text.length(), text, TextType.PLAIN));

        applyPatternToSegments(segments, PATTERN_BOLD, TextType.BOLD);
        applyPatternToSegments(segments, PATTERN_ITALIC, TextType.ITALIC);
        applyPatternToSegments(segments, PATTERN_CODE, TextType.CODE);
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
                            if (matcher.groupCount() >= 1) {
                                String code = matcher.group(1);
                                newSegments.add(new TextSegment(
                                        segment.start + matchStart,
                                        segment.start + matchEnd,
                                        code,
                                        TextType.CODE
                                ));
                            }
                            break;
                        case BOLD:
                        case ITALIC:
                            if (matcher.groupCount() >= 2) {
                                String content = matcher.group(2) != null ? matcher.group(2) : matcher.group(1);
                                newSegments.add(new TextSegment(
                                        segment.start + matchStart,
                                        segment.start + matchEnd,
                                        content,
                                        type
                                ));
                            }
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
}
