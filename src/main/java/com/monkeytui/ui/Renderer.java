package com.monkeytui.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.Screen;
import com.monkeytui.typing.TypingSession;

import java.io.IOException;

public final class Renderer {
    // Prevents instantiation of this renderer.
    private Renderer() {
    }

    // Paints one complete frame into the screen's back buffer.
    public static void draw(Screen screen, ScreenModel model) throws IOException {
        TerminalSize size = prepare(screen);
        TextGraphics graphics = screen.newTextGraphics();
        paintFrame(graphics, size);
        drawContent(graphics, size, model);
        screen.refresh();
    }

    // Resizes the screen if needed and returns its current size.
    private static TerminalSize prepare(Screen screen) throws IOException {
        screen.doResizeIfNecessary();
        return screen.getTerminalSize();
    }

    // Draws either the typing test or the active panel.
    private static void drawContent(TextGraphics g, TerminalSize size, ScreenModel model) {
        if (model.panel() == Panel.NONE) drawTest(g, size, model);
        else PanelRenderer.draw(g, size, model);
    }

    // Clears the full terminal frame with the background color.
    private static void paintFrame(TextGraphics g, TerminalSize size) {
        String blankRow = " ".repeat(Math.max(0, size.getColumns()));
        for (int row = 0; row < size.getRows(); row++) paintRow(g, row, blankRow);
    }

    // Paints one blank row across the terminal width.
    private static void paintRow(TextGraphics g, int row, String blankRow) {
        g.setBackgroundColor(Theme.BACKGROUND);
        g.setForegroundColor(Theme.FOREGROUND);
        g.putString(0, row, blankRow);
    }

    // Draws the live typing test screen.
    private static void drawTest(TextGraphics g, TerminalSize size, ScreenModel model) {
        TestLayout layout = TestLayout.create(model, size.getColumns(), size.getRows());
        drawStats(g, layout, model);
        drawLines(g, layout, model);
        drawCursor(g, layout, model.session());
        drawProgress(g, layout, model);
    }

    // Draws the timer and live words-per-minute statistic.
    private static void drawStats(TextGraphics g, TestLayout layout, ScreenModel model) {
        String timer = Math.max(0, model.session().remainingSeconds()) + "s";
        String stats = statsText(model, timer);
        TextPainter.putCentered(g, layout.width(), layout.statsY(), stats, Theme.FOREGROUND);
    }

    // Builds the statistic line, showing speed after three completed words.
    private static String statsText(ScreenModel model, String timer) {
        boolean ready = completedWords(model.session().text(), model.session().position()) >= 3;
        if (!model.session().isStarted() || !ready) return timer;
        return timer + "     " + String.format("%.0f wpm", model.session().wpm());
    }

    // Counts words completed by whitespace before the current position.
    private static int completedWords(String text, int position) {
        int end = Math.max(0, Math.min(position, text.length()));
        int words = 0;
        for (int i = 1; i < end; i++) {
            if (endsWord(text, i)) words++;
        }
        return words;
    }

    // Reports whether a whitespace character ends a completed word.
    private static boolean endsWord(String text, int index) {
        return !Character.isWhitespace(text.charAt(index - 1)) && Character.isWhitespace(text.charAt(index));
    }

    // Draws the visible wrapped passage lines.
    private static void drawLines(TextGraphics g, TestLayout layout, ScreenModel model) {
        int end = Math.min(layout.lines().size(), layout.firstLine() + layout.visible());
        for (int index = layout.firstLine(); index < end; index++) drawLine(g, layout, model.session(), index);
    }

    // Draws one wrapped line, including a ghost caret tail if present.
    private static void drawLine(TextGraphics g, TestLayout layout, TypingSession session, int index) {
        LayoutLine line = layout.lines().get(index);
        int y = layout.y0() + index - layout.firstLine();
        for (int column = 0; column < line.text().length() && layout.startX() + column < layout.width() - 1; column++) {
            drawCell(g, layout, session, index, column, y);
        }
        drawGhostTail(g, layout, line, index, y);
    }

    // Draws one passage character with its state color and caret mark.
    private static void drawCell(TextGraphics g, TestLayout layout, TypingSession session, int lineIndex, int column, int y) {
        char current = cellCharacter(layout, session, lineIndex, column);
        g.setForegroundColor(cellColor(layout, session, lineIndex, column));
        g.setBackgroundColor(Theme.BACKGROUND);
        drawGlyph(g, layout, lineIndex, column, y, current);
    }

    // Draws a glyph, underlining the ghost caret cell when needed.
    private static void drawGlyph(TextGraphics g, TestLayout layout, int lineIndex, int column, int y, char current) {
        int x = layout.startX() + column;
        String value = String.valueOf(current);
        if (isGhostCell(layout, lineIndex, column)) g.putString(x, y, value, SGR.UNDERLINE);
        else g.putString(x, y, value);
    }

    // Returns the glyph shown at a passage cell.
    private static char cellCharacter(TestLayout layout, TypingSession session, int lineIndex, int column) {
        int position = layout.lines().get(lineIndex).start() + column;
        if (position < layout.cursor()) return session.typedCharAt(position);
        return layout.lines().get(lineIndex).text().charAt(column);
    }

    // Returns the color used for a passage cell.
    private static TextColor cellColor(TestLayout layout, TypingSession session, int lineIndex, int column) {
        int position = layout.lines().get(lineIndex).start() + column;
        if (position >= layout.cursor()) return Theme.MUTED;
        return session.isError(position) ? Theme.ERROR : Theme.FOREGROUND;
    }

    // Reports whether a cell is occupied by the ghost caret.
    private static boolean isGhostCell(TestLayout layout, int lineIndex, int column) {
        return layout.ghostCell() != null && layout.ghostCell()[0] == lineIndex && layout.ghostCell()[1] == column;
    }

    // Draws the ghost caret when it sits just past the end of a line.
    private static void drawGhostTail(TextGraphics g, TestLayout layout, LayoutLine line, int lineIndex, int y) {
        int column = ghostTailColumn(layout, line, lineIndex);
        if (column < 0) return;
        g.setForegroundColor(Theme.GHOST);
        g.setBackgroundColor(Theme.BACKGROUND);
        g.putString(layout.startX() + column, y, "▁");
    }

    // Returns the ghost caret tail column, or -1 when none applies.
    private static int ghostTailColumn(TestLayout layout, LayoutLine line, int lineIndex) {
        if (layout.ghostCell() == null || layout.ghostCell()[0] != lineIndex) return -1;
        int column = layout.ghostCell()[1];
        if (column != line.text().length() || layout.startX() + column >= layout.width() - 1) return -1;
        return column;
    }

    // Draws the solid caret at the current typing position.
    private static void drawCursor(TextGraphics g, TestLayout layout, TypingSession session) {
        int y = layout.y0() + layout.cursorLine() - layout.firstLine();
        int x = layout.startX() + layout.cursorCol();
        if (!isCursorVisible(layout, x, y)) return;
        paintCursor(g, x, y, cursorCharacter(layout.text(), layout.cursor()));
    }

    // Reports whether the caret falls inside the visible typing block.
    private static boolean isCursorVisible(TestLayout layout, int x, int y) {
        return y >= layout.y0() && y < layout.y0() + layout.visible() && x >= 0 && x < layout.width() - 1;
    }

    // Returns the character covered by the caret.
    private static char cursorCharacter(String text, int cursor) {
        return cursor < text.length() && text.charAt(cursor) != '\n' ? text.charAt(cursor) : ' ';
    }

    // Paints the inverted caret cell.
    private static void paintCursor(TextGraphics g, int x, int y, char current) {
        g.setBackgroundColor(Theme.FOREGROUND);
        g.setForegroundColor(Theme.BACKGROUND);
        g.putString(x, y, String.valueOf(current));
    }

    // Draws the progress bar, or the help hint before a run starts.
    private static void drawProgress(TextGraphics g, TestLayout layout, ScreenModel model) {
        int barY = Math.min(layout.height() - 3, layout.y0() + layout.visible() + 1);
        int barWidth = Math.max(10, Math.min(layout.width() - 12, 90));
        int barX = Math.max(1, (layout.width() - barWidth) / 2);
        if (model.session().isStarted()) drawBar(g, model.session(), layout, barX, barY, barWidth);
        else { g.setBackgroundColor(Theme.BACKGROUND); TextPainter.putCentered(g, layout.width(), Math.min(layout.height() - 1, barY + 1), "?  help", Theme.MUTED); }
    }

    // Draws the timed progress bar cells.
    private static void drawBar(TextGraphics g, TypingSession session, TestLayout layout, int x, int y, int width) {
        int filled = filledCells(session, width);
        for (int i = 0; i < width && x + i < layout.width(); i++) drawBarCell(g, x + i, y, i < filled);
    }

    // Paints one progress bar cell.
    private static void drawBarCell(TextGraphics g, int x, int y, boolean filled) {
        g.setBackgroundColor(Theme.BACKGROUND);
        g.setForegroundColor(filled ? Theme.FOREGROUND : Theme.MUTED);
        g.putString(x, y, "─");
    }

    // Computes how many progress bar cells should be filled.
    private static int filledCells(TypingSession session, int barWidth) {
        if (session.durationSeconds() <= 0) return positionProgress(session, barWidth);
        double elapsed = session.elapsedSeconds();
        return (int) Math.round(barWidth * Math.min(1.0, elapsed / session.durationSeconds()));
    }

    // Computes progress from typed characters when no duration exists.
    private static int positionProgress(TypingSession session, int barWidth) {
        if (session.text().isEmpty()) return 0;
        double ratio = (double) session.position() / session.text().length();
        return (int) Math.round(barWidth * Math.min(1.0, ratio));
    }
}
