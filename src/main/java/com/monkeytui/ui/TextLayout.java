package com.monkeytui.ui;

import java.util.ArrayList;
import java.util.List;

public final class TextLayout {
    public static final int MAX_WIDTH = 68;

    // Prevents instantiation of this layout utility.
    private TextLayout() {
    }

    // Wraps text without dropping characters or changing logical positions.
    public static List<LayoutLine> wrap(String text, int width) {
        List<LayoutLine> lines = new ArrayList<>();
        int start = 0;
        while (start < text.length()) start = appendLine(lines, text, start, width);
        if (lines.isEmpty()) lines.add(new LayoutLine(0, ""));
        return lines;
    }

    // Appends one wrapped line and returns the next logical start.
    private static int appendLine(List<LayoutLine> lines, String text, int start, int width) {
        if (text.charAt(start) == '\n') return addEmptyLine(lines, start);
        int end = lineEnd(text, start, width);
        lines.add(new LayoutLine(start, text.substring(start, end)));
        return skipNewline(text, end);
    }

    // Appends an empty line for an explicit newline.
    private static int addEmptyLine(List<LayoutLine> lines, int start) {
        lines.add(new LayoutLine(start, ""));
        return start + 1;
    }

    // Finds the end offset of the next wrapped line.
    private static int lineEnd(String text, int start, int width) {
        int end = Math.min(start + width, text.length());
        int newline = text.indexOf('\n', start);
        if (newline >= start && newline < end) end = newline;
        if (shouldBreakAtSpace(text, start, end, width)) end = text.lastIndexOf(' ', end - 1) + 1;
        return end == start ? Math.min(start + 1, text.length()) : end;
    }

    // Reports whether a full-width line should break at its last space.
    private static boolean shouldBreakAtSpace(String text, int start, int end, int width) {
        return end < text.length() && end == start + width && text.lastIndexOf(' ', end - 1) > start + width / 2;
    }

    // Skips the newline consumed at the end of a wrapped line.
    private static int skipNewline(String text, int end) {
        return end < text.length() && text.charAt(end) == '\n' ? end + 1 : end;
    }

    // Maps a logical character offset to a visual row and column.
    public static int[] position(List<LayoutLine> lines, String text, int position) {
        int target = clamp(position, text.length());
        for (int i = 0; i < lines.size(); i++) {
            int[] cell = positionOnLine(lines.get(i), text, target, i, i == lines.size() - 1);
            if (cell != null) return cell;
        }
        return positionAfterLast(lines, target);
    }

    // Locates a target on one wrapped line when it belongs there.
    private static int[] positionOnLine(LayoutLine line, String text, int target, int index, boolean last) {
        int lineEnd = line.start() + line.text().length();
        if (target < line.start()) return null;
        if (target < lineEnd) return new int[]{index, target - line.start()};
        if (target == lineEnd && (atNewline(text, target) || last)) return new int[]{index, line.text().length()};
        return null;
    }

    // Reports whether a logical offset sits on a source newline.
    private static boolean atNewline(String text, int target) {
        return target < text.length() && text.charAt(target) == '\n';
    }

    // Places a target that lies after the final wrapped line.
    private static int[] positionAfterLast(List<LayoutLine> lines, int target) {
        LayoutLine last = lines.get(lines.size() - 1);
        int column = Math.max(0, Math.min(target - last.start(), last.text().length()));
        return new int[]{lines.size() - 1, column};
    }

    // Clamps a logical offset to the valid text range.
    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(value, max));
    }
}
