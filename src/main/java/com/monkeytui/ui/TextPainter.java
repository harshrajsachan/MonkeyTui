package com.monkeytui.ui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;

import java.util.List;

public final class TextPainter {
    // Prevents instantiation of this painting utility.
    private TextPainter() {
    }

    // Draws horizontally centered text within the given width.
    public static void putCentered(TextGraphics g, int width, int y, String text, TextColor color) {
        if (width <= 0 || y < 0) return;
        int visibleLength = Math.min(text.length(), width);
        int x = Math.max(0, (width - visibleLength) / 2);
        g.setForegroundColor(color);
        g.putString(x, y, text.substring(0, Math.min(text.length(), width - x)));
    }

    // Returns the length of the longest value.
    public static int longest(List<String> values) {
        int longest = 0;
        for (String value : values) longest = Math.max(longest, value.length());
        return longest;
    }
}
