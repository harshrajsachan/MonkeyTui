package com.monkeytui.ui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.monkeytui.history.RunRecord;
import com.monkeytui.typing.ContentMode;
import com.monkeytui.typing.Durations;
import com.monkeytui.typing.TypingSession;

import java.util.ArrayList;
import java.util.List;

public final class PanelRenderer {
    private static final List<String> HELP_OPTIONS = List.of(
            "ctrl+w       toggle words/code",
            "ctrl+l       change language (code mode)",
            "ctrl+o       change lesson (code mode)",
            "ctrl+t       configure test duration",
            "ctrl+d       change difficulty (words mode)",
            "tab          configure duration",
            "esc          restart test",
            "ctrl+r       retake same test",
            "ctrl+h       choose from last 10 tests",
            "ctrl+q       quit application",
            "e            view mistyped words (results)",
            "?            show this help",
            "",
            "↑/↓ or j/k   move selection",
            "enter        confirm selection",
            "esc          close panel / restart test");
    private static final String RESULTS_FOOTER = "esc restart  ·  ctrl+r same test  ·  ctrl+h history  ·  e errors";

    // Prevents instantiation of this panel renderer.
    private PanelRenderer() {
    }

    // Draws the active panel for the current screen model.
    static void draw(TextGraphics g, TerminalSize size, ScreenModel model) {
        if (hasDedicatedScreen(model.panel())) {
            drawDedicatedScreen(g, size, model);
            return;
        }
        drawMenu(g, size, model, menuOptions(model));
    }

    // Reports whether the panel uses a dedicated non-menu layout.
    private static boolean hasDedicatedScreen(Panel panel) {
        return panel == Panel.ERRORS || panel == Panel.RESULTS;
    }

    // Draws the results or errors screen for the active panel.
    private static void drawDedicatedScreen(TextGraphics g, TerminalSize size, ScreenModel model) {
        if (model.panel() == Panel.ERRORS) drawErrors(g, size, model.session());
        else drawResults(g, size, model.session());
    }

    // Draws the mistyped-words review screen.
    private static void drawErrors(TextGraphics g, TerminalSize size, TypingSession session) {
        List<String> words = session.errorWords();
        int width = size.getColumns();
        int center = size.getRows() / 2;
        drawErrorsTitle(g, width, center, words.size());
        drawErrorsBody(g, width, center, words);
        drawErrorsFooter(g, width, size.getRows(), center, words.size());
    }

    // Draws the mistyped-words heading.
    private static void drawErrorsTitle(TextGraphics g, int width, int center, int count) {
        TextPainter.putCentered(g, width, Math.max(1, center - (count / 2) - 2), "mistyped words", Theme.FOREGROUND);
    }

    // Draws the error words or the empty-state message.
    private static void drawErrorsBody(TextGraphics g, int width, int center, List<String> words) {
        if (words.isEmpty()) {
            TextPainter.putCentered(g, width, center, "no mistyped words", Theme.MUTED);
            return;
        }
        int firstY = Math.max(1, center - words.size() / 2);
        for (int i = 0; i < words.size(); i++) TextPainter.putCentered(g, width, firstY + i, words.get(i), Theme.FOREGROUND);
    }

    // Draws the errors screen footer.
    private static void drawErrorsFooter(TextGraphics g, int width, int height, int center, int count) {
        TextPainter.putCentered(g, width, Math.min(height - 1, center + count / 2 + 3), "esc  back", Theme.MUTED);
    }

    // Draws the result metrics and footer.
    private static void drawResults(TextGraphics g, TerminalSize size, TypingSession session) {
        int row = Math.max(0, size.getRows() / 2 - 1);
        int columnWidth = Math.max(7, Math.min(12, size.getColumns() / 9));
        int start = Math.max(0, (size.getColumns() - columnWidth * 5) / 2);
        drawResultColumns(g, session, start, row, columnWidth);
        TextPainter.putCentered(g, size.getColumns(), row + 3, RESULTS_FOOTER, Theme.MUTED);
    }

    // Draws all five result columns.
    private static void drawResultColumns(TextGraphics g, TypingSession session, int start, int row, int columnWidth) {
        String[] labels = {"wpm", "acc", "raw", "typos", "time"};
        String[] values = resultValues(session);
        for (int i = 0; i < labels.length; i++) drawResultColumn(g, start, row, columnWidth, labels[i], values[i], i);
    }

    // Builds the result values in display order.
    private static String[] resultValues(TypingSession session) {
        return new String[]{String.format("%.0f", session.wpm()), String.format("%.0f%%", session.accuracy()), String.format("%.0f", session.rawWpm()), Integer.toString(session.totalMistakes()), session.elapsedSeconds() + "s"};
    }

    // Draws one labeled result column.
    private static void drawResultColumn(TextGraphics g, int start, int row, int columnWidth, String label, String value, int index) {
        int x = start + index * columnWidth;
        g.setBackgroundColor(Theme.BACKGROUND);
        g.setForegroundColor(Theme.MUTED);
        g.putString(x + Math.max(0, (columnWidth - label.length()) / 2), row, label);
        g.setForegroundColor(Theme.FOREGROUND);
        g.putString(x + Math.max(0, (columnWidth - value.length()) / 2), row + 1, value);
    }

    // Returns the options shown for the active menu panel.
    private static List<String> menuOptions(ScreenModel model) {
        if (model.panel() == Panel.HISTORY || model.panel() == Panel.HELP) return specialOptions(model);
        return selectionOptions(model.panel());
    }

    // Returns options for the history and help panels.
    private static List<String> specialOptions(ScreenModel model) {
        return model.panel() == Panel.HELP ? helpOptions() : historyOptions(model.history());
    }

    // Returns options for the selection panels.
    private static List<String> selectionOptions(Panel panel) {
        if (panel == Panel.DIFFICULTY) return difficultyOptions();
        if (panel == Panel.TIME) return durationOptions();
        if (panel == Panel.LANGUAGE) return codeOptions();
        if (panel == Panel.LESSON) return lessonOptions();
        return List.of();
    }

    // Returns difficulty labels.
    private static List<String> difficultyOptions() {
        return ContentMode.WORD_MODES.stream().map(ContentMode::label).toList();
    }

    // Returns duration labels.
    private static List<String> durationOptions() {
        List<String> options = new ArrayList<>();
        for (int duration : Durations.ALL) options.add(duration + " seconds");
        return options;
    }

    // Returns language labels.
    private static List<String> codeOptions() {
        return ContentMode.CODE_MODES.stream().map(ContentMode::label).toList();
    }

    // Returns lesson labels.
    private static List<String> lessonOptions() {
        List<String> options = new ArrayList<>();
        for (ContentMode mode : ContentMode.CODE_MODES) options.add(mode.label() + " basics");
        return options;
    }

    // Returns the fixed keyboard-help lines.
    private static List<String> helpOptions() {
        return HELP_OPTIONS;
    }

    // Returns formatted recent-run labels.
    private static List<String> historyOptions(List<RunRecord> history) {
        List<String> options = new ArrayList<>();
        for (RunRecord record : history) options.add(formatRecord(record));
        if (options.isEmpty()) options.add("no saved tests");
        return options;
    }

    // Formats one history record for the menu.
    private static String formatRecord(RunRecord record) {
        return String.format("%.0f wpm · %ds · %s", record.wpm(), record.duration(), record.mode().fileName().replace(".txt", ""));
    }

    // Draws a menu panel with its title, options, and footer.
    private static void drawMenu(TextGraphics g, TerminalSize size, ScreenModel model, List<String> options) {
        int panelWidth = panelWidth(size.getColumns(), options);
        int totalHeight = options.size() + 5;
        int x = Math.max(0, (size.getColumns() - panelWidth) / 2);
        int y = Math.max(0, (size.getRows() - totalHeight) / 2);
        drawMenuBody(g, size, model, options, x, y, totalHeight);
    }

    // Draws the menu frame and option rows.
    private static void drawMenuBody(TextGraphics g, TerminalSize size, ScreenModel model, List<String> options, int x, int y, int totalHeight) {
        TextPainter.putCentered(g, size.getColumns(), y, model.panel().title(), Theme.FOREGROUND);
        drawOptions(g, size, model, options, x, y);
        String footer = model.panel() == Panel.HELP ? "esc close" : "↑↓ / j k move  ·  enter select  ·  esc back";
        TextPainter.putCentered(g, size.getColumns(), Math.min(size.getRows() - 1, y + totalHeight - 1), footer, Theme.MUTED);
    }

    // Draws all visible menu options.
    private static void drawOptions(TextGraphics g, TerminalSize size, ScreenModel model, List<String> options, int x, int y) {
        for (int i = 0; i < options.size(); i++) {
            if (y + 2 + i >= size.getRows() - 1) return;
            drawOption(g, size.getColumns(), model.panel(), options.get(i), i, model.panelIndex(), x, y + 2 + i);
        }
    }

    // Draws one menu option with its selection marker.
    private static void drawOption(TextGraphics g, int width, Panel panel, String option, int index, int selected, int x, int y) {
        boolean active = index == selected;
        String line = panel == Panel.HELP ? option : (active ? "●  " : "○  ") + option;
        int lineX = panel == Panel.HELP ? x + 2 : Math.max(0, (width - line.length()) / 2);
        g.setForegroundColor(active && panel != Panel.HELP ? Theme.FOREGROUND : Theme.MUTED);
        g.putString(lineX, y, line.substring(0, Math.min(line.length(), Math.max(0, width - lineX))));
    }

    // Computes the menu width from the longest option.
    private static int panelWidth(int width, List<String> options) {
        int preferred = Math.max(34, TextPainter.longest(options) + 8);
        return Math.min(preferred, Math.max(34, width - 4));
    }
}
