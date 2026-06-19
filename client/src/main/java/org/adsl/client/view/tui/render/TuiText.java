package org.adsl.client.view.tui.render;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure text helpers for the terminal UI: terminal-column width that accounts for
 * emoji (wide / with variation selector) and box-drawing glyphs, padding to a
 * visible width, and word-wrapping. Shared by the TUI screens and their render
 * helpers so the width maths lives in one place.
 */
public final class TuiText {

    private TuiText() {}

    /** Visible terminal-column width of {@code s} (emoji ≈ 2–3 cols, box-drawing = 1). */
    public static int visualWidth(String s) {
        if (s == null) return 0;
        int width = 0;
        int i = 0;
        while (i < s.length()) {
            int ch = s.charAt(i);
            if (ch >= 0xD800 && ch <= 0xDBFF && i + 1 < s.length()) {
                i += 2;
                if (i < s.length() && s.charAt(i) == 0xFE0F) {
                    width += 3; i++;
                } else {
                    width += 2;
                }
            } else if (ch == 0xFE0F || ch == 0x200D || (ch >= 0x200B && ch <= 0x200F)) {
                i++;
            } else if (ch >= 0x2500 && ch <= 0x257F) {
                width += 1; i++;
            } else if (ch >= 0x23E9 && ch <= 0x23FA) {
                width += 1; i++;
            } else {
                width += 1; i++;
            }
        }
        return width;
    }

    /** Pads/truncates {@code s} to exactly {@code len} visible columns (right padding). */
    public static String padRight(String s, int len) {
        if (s == null) s = "";
        int vw = visualWidth(s);
        if (vw == len) return s;
        if (vw > len) {
            StringBuilder sb = new StringBuilder();
            int w = 0;
            int i = 0;
            while (i < s.length()) {
                int ch = s.charAt(i);
                int cw, advance;
                if (ch >= 0xD800 && ch <= 0xDBFF && i + 1 < s.length()) {
                    if (i + 2 < s.length() && s.charAt(i + 2) == 0xFE0F) {
                        cw = 3; advance = 3;
                    } else {
                        cw = 2; advance = 2;
                    }
                } else if (ch == 0xFE0F || ch == 0x200D || (ch >= 0x200B && ch <= 0x200F)) {
                    cw = 0; advance = 1;
                } else if (ch >= 0x2500 && ch <= 0x257F) {
                    cw = 1; advance = 1;
                } else if (ch >= 0x23E9 && ch <= 0x23FA) {
                    cw = 3; advance = 1;
                } else {
                    cw = 1; advance = 1;
                }
                if (w + cw > len) break;
                sb.append(s, i, i + advance);
                w += cw;
                i += advance;
            }
            if (w < len) sb.append(" ".repeat(len - w));
            return sb.toString();
        }
        return s + " ".repeat(len - vw);
    }

    /** Left-pads {@code s} to {@code len} visible columns (no truncation). */
    public static String padLeft(String s, int len) {
        if (s == null) s = "";
        int vw = visualWidth(s);
        if (vw >= len) return s;
        return " ".repeat(len - vw) + s;
    }

    /**
     * Word-wraps {@code text} to at most {@code maxW} visible characters per line.
     * Preserves intentional blank lines (paragraph breaks) and treats CRLF/LF uniformly.
     */
    public static List<String> wrapText(String text, int maxW) {
        if (text == null || text.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        text.lines().forEach(paragraph -> {
            if (paragraph.isBlank()) {
                result.add("");
                return;
            }
            StringBuilder current = new StringBuilder();
            for (String word : paragraph.split("\\s+")) {
                if (word.isEmpty()) continue;
                if (current.isEmpty()) {
                    current.append(word);
                } else if (current.length() + 1 + word.length() <= maxW) {
                    current.append(' ').append(word);
                } else {
                    result.add(current.toString());
                    current.setLength(0);
                    current.append(word);
                }
            }
            if (!current.isEmpty()) result.add(current.toString());
        });
        return result;
    }
}
