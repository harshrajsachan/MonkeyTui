package com.monkeytui.ui;

import java.util.List;

record TestLayout(
        int width,
        int height,
        int textWidth,
        String text,
        List<LayoutLine> lines,
        int cursor,
        int cursorLine,
        int cursorCol,
        int[] ghostCell) {

    // Builds all geometry needed to paint the typing test.
    static TestLayout create(ScreenModel model, int width, int height) {
        String text = model.session().text();
        int textWidth = Math.max(1, Math.min(TextLayout.MAX_WIDTH, width - 12));
        List<LayoutLine> lines = TextLayout.wrap(text, textWidth);
        int cursor = model.session().position();
        int[] cell = TextLayout.position(lines, text, cursor);
        return new TestLayout(width, height, textWidth, text, lines, cursor, cell[0], cell[1], ghostCell(model, lines, text, cursor));
    }

    // Computes the ghost caret cell, or null when it should not be shown.
    private static int[] ghostCell(ScreenModel model, List<LayoutLine> lines, String text, int cursor) {
        if (!shouldShowGhost(model, text)) return null;
        int position = ghostPosition(model, text);
        return position == cursor ? null : TextLayout.position(lines, text, position);
    }

    // Reports whether a ghost caret is applicable for this frame.
    private static boolean shouldShowGhost(ScreenModel model, String text) {
        return model.previousWpm() > 0 && model.session().isStarted() && !model.session().isComplete() && !text.isEmpty();
    }

    // Estimates the ghost caret position from the previous run's speed.
    private static int ghostPosition(ScreenModel model, String text) {
        double estimate = model.session().elapsedSeconds() * model.previousWpm() * 5.0 / 60.0;
        int position = (int) Math.floor(estimate);
        return Math.min(text.length() - 1, Math.max(0, position));
    }

    // Returns the number of text lines that fit on screen.
    int visible() {
        return Math.max(1, Math.min(3, height - 12));
    }

    // Returns the first wrapped line visible in the viewport.
    int firstLine() {
        return Math.max(0, Math.min(cursorLine - 1, lines.size() - visible()));
    }

    // Returns the vertical position of the live statistics.
    int statsY() {
        return Math.max(1, height / 2 - 4);
    }

    // Returns the vertical position of the typing block.
    int y0() {
        return Math.max(statsY() + 2, height / 2 - 1);
    }

    // Returns the horizontal start of the typing block.
    int startX() {
        return Math.max(1, (width - textWidth) / 2);
    }
}
