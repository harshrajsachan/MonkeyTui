package com.proto;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.proto.game.TypingSession;
import com.proto.lang.SnippetLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

/**
 * Keyboard-first terminal typing test. Screen changes are event-driven;
 * Lanterna's double buffer handles changed-cell rendering.
 */
public final class Main {
    private static final TextColor BG = new TextColor.RGB(0, 0, 0);
    private static final TextColor MUTED = new TextColor.RGB(92, 92, 92);
    private static final TextColor FG = new TextColor.RGB(225, 225, 225);
    private static final TextColor ERROR = new TextColor.RGB(235, 90, 100);

    private static final int[] DURATIONS = {15, 30, 60, 120, 300};
    private enum Panel { NONE, DIFFICULTY, TIME, HELP, LANGUAGE, LESSON, ERRORS, HISTORY, RESULTS }
    private record RunRecord(String mode, int duration, double wpm, String text) { }

    private Main() { }

    public static void main(String[] args) {
        String mode = normalizeMode(args.length > 0 ? args[0] : "easy.txt");
        try {
            var terminal = new DefaultTerminalFactory().createTerminal();
            try (Screen screen = new TerminalScreen(terminal)) {
                screen.startScreen();
                screen.setCursorPosition(null);
                SnippetLoader loader = new SnippetLoader();
                int durationIndex = 0;
                TypingSession session = new TypingSession(loader, mode);
                session.setDurationSeconds(DURATIONS[durationIndex]);
                boolean running = true;
                boolean dirty = true;
                Panel panel = Panel.NONE;
                int difficultyIndex = difficultyIndex(mode);
                int panelIndex = 0;
                long lastDisplayedSecond = -1;
                double previousWpm = 0.0;
                boolean resultCaptured = false;
                List<RunRecord> history = loadHistory();
                int historyIndex = 0;

                draw(screen, session, mode, panel, panelIndex, difficultyIndex, durationIndex, previousWpm);
                dirty = false;
                lastDisplayedSecond = session.elapsedSeconds();

                while (running) {
                    KeyStroke key = screen.pollInput();
                    if (key != null) {
                        KeyType type = key.getKeyType();
                        Character ch = type == KeyType.Character ? key.getCharacter() : null;
                        boolean ctrl = key.isCtrlDown();

                        // Ctrl+Q is the only application-exit shortcut.
                        if ((ctrl && ch != null && (ch == 'q' || ch == 'Q'))
                                || (type == KeyType.Character && ch != null && ch == 17)) {
                            running = false;
                            continue;
                        }

                        // History is accessible from the test or results screen.
                        if (ctrl && ch != null && (ch == 'h' || ch == 'H')
                                && (panel == Panel.NONE || panel == Panel.RESULTS)) {
                            panel = Panel.HISTORY;
                            history = loadHistory();
                            historyIndex = 0;
                            panelIndex = 0;
                            dirty = true;
                            continue;
                        }

                        if (panel != Panel.NONE) {
                            if (type == KeyType.Escape) {
                                if (panel == Panel.RESULTS) {
                                    session.restart();
                                    resultCaptured = false;
                                    previousWpm = 0.0;
                                    panel = Panel.NONE;
                                } else if (panel == Panel.ERRORS) {
                                    panel = Panel.RESULTS;
                                } else {
                                    panel = Panel.NONE;
                                }
                                dirty = true;
                            } else if (type == KeyType.Backspace && panel != Panel.RESULTS) {
                                panel = panel == Panel.ERRORS ? Panel.RESULTS : Panel.NONE;
                                dirty = true;
                            } else if (type == KeyType.ArrowUp || (ch != null && (ch == 'k' || ch == 'K'))) {
                                panelIndex = Math.max(0, panelIndex - 1);
                                dirty = true;
                            } else if (type == KeyType.ArrowDown || (ch != null && (ch == 'j' || ch == 'J'))) {
                                panelIndex = Math.min(panelSize(panel) - 1, panelIndex + 1);
                                dirty = true;
                            } else if (panel == Panel.RESULTS) {
                                // Results stay visible unless explicitly retaking or reviewing errors.
                                if (ch != null && !ctrl && (ch == 'e' || ch == 'E')) {
                                    panel = Panel.ERRORS;
                                    panelIndex = 0;
                                    dirty = true;
                                } else if (ctrl && ch != null && (ch == 'r' || ch == 'R')) {
                                    session.restartSameText();
                                    panel = Panel.NONE;
                                    resultCaptured = false;
                                    dirty = true;
                                } else if (ctrl && ch != null && (ch == 'h' || ch == 'H')) {
                                    panel = Panel.HISTORY;
                                    historyIndex = 0;
                                    panelIndex = 0;
                                    dirty = true;
                                }
                            } else if (panel == Panel.HISTORY && (type == KeyType.Enter || type == KeyType.Character && ch != null && ch == ' ')) {
                                if (!history.isEmpty()) {
                                    RunRecord record = history.get(Math.min(panelIndex, history.size() - 1));
                                    mode = record.mode();
                                    session = new TypingSession(loader, mode);
                                    session.setDurationSeconds(record.duration());
                                    session.restartWithText(record.text());
                                    durationIndex = durationIndexFor(record.duration());
                                    previousWpm = record.wpm();
                                    resultCaptured = false;
                                    panel = Panel.NONE;
                                } else {
                                    panel = Panel.RESULTS;
                                }
                                dirty = true;
                            } else if (type == KeyType.Enter || type == KeyType.Character && (ch == ' ')) {
                                switch (panel) {
                                    case DIFFICULTY -> {
                                        mode = new String[]{"easy.txt", "medium.txt", "hard.txt"}[panelIndex];
                                        difficultyIndex = panelIndex;
                                        session = new TypingSession(loader, mode);
                                        session.setDurationSeconds(DURATIONS[durationIndex]);
                                        panel = Panel.NONE;
                                    }
                                    case TIME -> {
                                        durationIndex = panelIndex;
                                        session.setDurationSeconds(DURATIONS[durationIndex]);
                                        session.restart();
                                        panel = Panel.NONE;
                                    }
                                    case LANGUAGE -> {
                                        mode = panelIndex == 0 ? "java.txt" : "cpp.txt";
                                        session = new TypingSession(loader, mode);
                                        session.setDurationSeconds(DURATIONS[durationIndex]);
                                        panel = Panel.NONE;
                                    }
                                    case LESSON -> {
                                        mode = panelIndex == 0 ? "java.txt" : "cpp.txt";
                                        session = new TypingSession(loader, mode);
                                        session.setDurationSeconds(DURATIONS[durationIndex]);
                                        panel = Panel.NONE;
                                    }
                                    case ERRORS -> panel = Panel.NONE;
                                    case HELP -> panel = Panel.NONE;
                                    default -> panel = Panel.NONE;
                                }
                                dirty = true;
                            } else if (type == KeyType.Character && ch != null && (ch == '?' || ch == 'q')) {
                                panel = Panel.NONE;
                                dirty = true;
                            }
                        } else if (type == KeyType.Escape) {
                            // Esc is the only test restart key.
                            session.restart();
                            resultCaptured = false;
                            previousWpm = 0.0;
                            dirty = true;
                        } else if (ctrl && ch != null && (ch == 'd' || ch == 'D')) {
                            panel = Panel.DIFFICULTY;
                            panelIndex = difficultyIndex;
                            dirty = true;
                        } else if (ctrl && ch != null && (ch == 't' || ch == 'T')) {
                            panel = Panel.TIME;
                            panelIndex = durationIndex;
                            dirty = true;
                        } else if (type == KeyType.Tab) {
                            panel = Panel.TIME;
                            panelIndex = durationIndex;
                            dirty = true;
                        } else if (ctrl && ch != null && (ch == 'w' || ch == 'W')) {
                            if (isCodeMode(mode)) mode = "easy.txt";
                            else mode = "java.txt";
                            session = new TypingSession(loader, mode);
                                        session.setDurationSeconds(DURATIONS[durationIndex]);
                            difficultyIndex = difficultyIndex(mode);
                            dirty = true;
                        } else if (ctrl && ch != null && (ch == 'l' || ch == 'L')) {
                            panel = Panel.LANGUAGE;
                            panelIndex = mode.equals("cpp.txt") ? 1 : 0;
                            dirty = true;
                        } else if (ctrl && ch != null && (ch == 'o' || ch == 'O')) {
                            panel = Panel.LESSON;
                            panelIndex = mode.equals("cpp.txt") ? 1 : 0;
                            dirty = true;
                        } else if (ctrl && ch != null && (ch == 't' || ch == 'T')) {
                            panel = Panel.TIME;
                            panelIndex = durationIndex;
                            dirty = true;
                        } else if (type == KeyType.Character && ch != null && ch == '?') {
                            panel = Panel.HELP;
                            panelIndex = 0;
                            dirty = true;
                        } else if (type == KeyType.Backspace) {
                            session.backspace();
                            dirty = true;
                        } else if (type == KeyType.Enter) {
                            session.enter();
                            dirty = true;
                        } else if (type == KeyType.Character && ch != null && !ctrl && !key.isAltDown()) {
                            session.type(ch);
                            dirty = true;
                        }
                    }

                    if (panel == Panel.NONE && session.isStarted() && session.isComplete()) {
                        if (!resultCaptured) {
                            previousWpm = session.wpm();
                            resultCaptured = true;
                            history.add(0, new RunRecord(mode, session.durationSeconds(), previousWpm, session.text()));
                            while (history.size() > 10) history.remove(history.size() - 1);
                            saveHistory(history);
                        }
                        panel = Panel.RESULTS;
                        dirty = true;
                    }
                    long displayedSecond = session.elapsedSeconds();
                    boolean statsTick = panel == Panel.NONE && session.isStarted()
                            && displayedSecond != lastDisplayedSecond;
                    if (dirty || statsTick) {
                        draw(screen, session, mode, panel, panelIndex, difficultyIndex, durationIndex, previousWpm);
                        dirty = false;
                        lastDisplayedSecond = displayedSecond;
                    }
                    if (key == null) Thread.sleep(12);
                }
                screen.stopScreen();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("MonkeyTui could not start: " + e.getMessage());
            System.err.println("Run it inside a real terminal, not an IDE output pane.");
        }
    }

    private static Path historyPath() {
        return Path.of(System.getProperty("user.home"), ".local", "share", "monkeytui", "history.tsv");
    }

    private static List<RunRecord> loadHistory() {
        List<RunRecord> records = new ArrayList<>();
        Path path = historyPath();
        if (!Files.isRegularFile(path)) return records;
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String[] fields = line.split("\\t", 4);
                if (fields.length != 4) continue;
                String text = new String(Base64.getDecoder().decode(fields[3]), StandardCharsets.UTF_8);
                records.add(new RunRecord(fields[0], Integer.parseInt(fields[1]),
                        Double.parseDouble(fields[2]), text));
                if (records.size() == 10) break;
            }
        } catch (Exception ignored) { }
        return records;
    }

    private static void saveHistory(List<RunRecord> records) {
        Path path = historyPath();
        try {
            Files.createDirectories(path.getParent());
            List<String> lines = new ArrayList<>();
            for (RunRecord record : records.subList(0, Math.min(10, records.size()))) {
                String encoded = Base64.getEncoder().encodeToString(record.text().getBytes(StandardCharsets.UTF_8));
                lines.add(record.mode() + "\t" + record.duration() + "\t" + record.wpm() + "\t" + encoded);
            }
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) { }
    }

    private static int durationIndexFor(int duration) {
        for (int i = 0; i < DURATIONS.length; i++) if (DURATIONS[i] == duration) return i;
        return 0;
    }

    private static int completedWords(String text, int position) {
        int end = Math.max(0, Math.min(position, text.length()));
        int words = 0;
        boolean inWord = false;
        for (int i = 0; i < end; i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                if (inWord) words++;
                inWord = false;
            } else {
                inWord = true;
            }
        }
        return words;
    }

    private static int panelSize(Panel p) {
        return switch (p) {
            case DIFFICULTY -> 3;
            case TIME -> DURATIONS.length;
            case LANGUAGE, LESSON -> 2;
            case HISTORY -> Math.max(1, loadHistory().size());
            case ERRORS -> 1;
            default -> 1;
        };
    }

    private static boolean isCodeMode(String mode) {
        return mode.equals("java.txt") || mode.equals("cpp.txt");
    }

    private static int difficultyIndex(String mode) {
        return switch (mode) {
            case "medium.txt" -> 1;
            case "hard.txt" -> 2;
            default -> 0;
        };
    }

    private static String normalizeMode(String mode) {
        if (mode == null) return "easy.txt";
        return switch (mode.toLowerCase()) {
            case "easy.txt", "medium.txt", "hard.txt", "cpp.txt", "java.txt" -> mode.toLowerCase();
            default -> "easy.txt";
        };
    }

    private static void draw(Screen screen, TypingSession session, String mode, Panel panel,
                             int panelIndex, int difficultyIndex, int durationIndex,
                             double previousWpm) throws IOException {
        TerminalSize size = screen.getTerminalSize();
        int width = size.getColumns();
        int height = size.getRows();
        TextGraphics g = screen.newTextGraphics();

        // Paint a complete frame into Lanterna's back buffer, without clear().
        String blankRow = " ".repeat(Math.max(0, width));
        for (int row = 0; row < height; row++) {
            g.setBackgroundColor(BG);
            g.setForegroundColor(FG);
            g.putString(0, row, blankRow);
        }

        if (panel != Panel.NONE) {
            drawPanel(g, width, height, panel, panelIndex, mode, session);
        } else {
            String timer = Math.max(0, session.remainingSeconds()) + "s";
            boolean metricsReady = completedWords(session.text(), session.position()) >= 3;
            String stats = session.isStarted() && metricsReady
                    ? timer + "     " + String.format("%.0f wpm", session.wpm())
                    : timer;
            int statsY = Math.max(1, height / 2 - 4);
            putCentered(g, width, statsY, stats, FG);

            String text = session.text();
            // Keep the typing block compact like Toofan: fewer words and more breathing room.
            int textWidth = Math.max(20, Math.min(68, width - 12));
            List<Line> lines = wrapPreservingPositions(text, textWidth);
            int cursor = session.position();
            int cursorLine = 0;
            int cursorCol = 0;
            for (int i = 0; i < lines.size(); i++) {
                Line line = lines.get(i);
                int lineEnd = line.start + line.text.length();
                boolean cursorAtNewline = cursor < text.length() && text.charAt(cursor) == '\n' && cursor == lineEnd;
                if (cursorAtNewline || (cursor >= line.start &&
                        (cursor < lineEnd || i == lines.size() - 1 || cursor == text.length()))) {
                    cursorLine = i;
                    cursorCol = cursor - line.start;
                    break;
                }
            }

            int visible = Math.max(1, Math.min(3, height - 12));
            int firstLine = Math.max(0, Math.min(cursorLine - 1, lines.size() - visible));
            int y0 = Math.max(statsY + 2, height / 2 - 1);
            int startX = Math.max(2, (width - textWidth) / 2);
            for (int li = firstLine; li < Math.min(lines.size(), firstLine + visible); li++) {
                Line line = lines.get(li);
                int y = y0 + li - firstLine;
                for (int j = 0; j < line.text.length() && startX + j < width - 1; j++) {
                    int p = line.start + j;
                    TextColor color = p < cursor ? (session.isError(p) ? ERROR : FG) : MUTED;
                    g.setForegroundColor(color);
                    g.setBackgroundColor(BG);
                    g.putString(startX + j, y, String.valueOf(line.text.charAt(j)));
                }
            }

            int cursorY = y0 + cursorLine - firstLine;
            int cursorX = startX + cursorCol;
            if (cursorY >= y0 && cursorY < y0 + visible && cursorX >= 0 && cursorX < width - 1) {
                char current = cursor < text.length() ? text.charAt(cursor) : ' ';
                g.setBackgroundColor(FG);
                g.setForegroundColor(BG);
                g.putString(cursorX, cursorY, String.valueOf(current));
            }

            // Ghost caret: estimates where the previous run's average speed would be
            // at this elapsed time, like Monkeytype's pace caret.
            if (previousWpm > 0 && session.isStarted() && !session.isComplete()) {
                int ghostPosition = Math.min(text.length() - 1,
                        Math.max(0, (int) Math.floor(session.elapsedSeconds() * previousWpm * 5.0 / 60.0)));
                for (int li = firstLine; li < Math.min(lines.size(), firstLine + visible); li++) {
                    Line line = lines.get(li);
                    if (ghostPosition >= line.start && ghostPosition < line.start + line.text.length()) {
                        int gx = startX + ghostPosition - line.start;
                        int gy = y0 + li - firstLine;
                        // Underline caret is drawn on the row below the target glyph,
                        // so it never overwrites the character being typed.
                        int markerY = gy + 1;
                        if (gx >= 0 && gx < width - 1 && markerY < height) {
                            g.setBackgroundColor(BG);
                            g.setForegroundColor(new TextColor.RGB(150, 150, 150));
                            g.putString(gx, markerY, "▁");
                        }
                        break;
                    }
                }
            }

            // Keep the idle screen minimal: help only. Once typing starts, replace
            // the help hint with the timed progress bar.
            int barY = Math.min(height - 3, y0 + visible + 1);
            int barWidth = Math.max(10, Math.min(width - 12, 90));
            int barX = Math.max(1, (width - barWidth) / 2);
            if (session.isStarted()) {
                int filled = 0;
                if (session.durationSeconds() > 0) {
                    double elapsed = session.elapsedSeconds();
                    filled = (int) Math.round(barWidth * Math.min(1.0, elapsed / session.durationSeconds()));
                } else {
                    filled = (int) Math.round(barWidth * Math.min(1.0,
                            session.text().isEmpty() ? 0 : (double) session.position() / session.text().length()));
                }
                for (int i = 0; i < barWidth && barX + i < width; i++) {
                    g.setBackgroundColor(BG);
                    g.setForegroundColor(i < filled ? FG : MUTED);
                    g.putString(barX + i, barY, "─");
                }
            } else {
                g.setBackgroundColor(BG);
                putCentered(g, width, Math.min(height - 1, barY + 1), "?  help", MUTED);
            }
        }
        screen.refresh();
    }

    private static void drawPanel(TextGraphics g, int width, int height, Panel panel, int selected,
                                  String mode, TypingSession session) {
        List<String> options = new ArrayList<>();
        String title;
        if (panel == Panel.ERRORS) {
            List<String> words = session.errorWords();
            putCentered(g, width, Math.max(1, height / 2 - (words.size() / 2) - 2), "mistyped words", FG);
            if (words.isEmpty()) {
                putCentered(g, width, height / 2, "no mistyped words", MUTED);
            } else {
                int firstY = Math.max(1, height / 2 - words.size() / 2);
                for (int i = 0; i < words.size(); i++) {
                    putCentered(g, width, firstY + i, words.get(i), FG);
                }
            }
            putCentered(g, width, Math.min(height - 1, height / 2 + words.size() / 2 + 3), "esc  back", MUTED);
            return;
        }
        switch (panel) {
            case DIFFICULTY -> {
                title = "difficulty";
                options.add("easy");
                options.add("medium");
                options.add("hard");
            }
            case TIME -> {
                title = "time";
                for (int duration : DURATIONS) options.add(duration + " seconds");
            }
            case LANGUAGE -> {
                title = "language";
                options.add("Java");
                options.add("C++");
            }
            case LESSON -> {
                title = "lesson";
                options.add("Java basics");
                options.add("C++ basics");
            }
            case ERRORS -> { title = "mistyped words"; }
            case HISTORY -> {
                title = "last 10 tests";
                for (RunRecord record : loadHistory()) {
                    options.add(String.format("%.0f wpm · %ds · %s", record.wpm(), record.duration(), record.mode().replace(".txt", "")));
                }
                if (options.isEmpty()) options.add("no saved tests");
            }
            case RESULTS -> {
                title = "results";
                String[] labels = {"wpm", "acc", "raw", "typos", "time"};
                String[] values = {
                        String.format("%.0f", session.wpm()),
                        String.format("%.0f%%", session.accuracy()),
                        String.format("%.0f", session.rawWpm()),
                        Integer.toString(session.totalMistakes()),
                        session.elapsedSeconds() + "s"
                };
                int row = Math.max(0, height / 2 - 1);
                int columnWidth = Math.max(7, Math.min(12, width / 9));
                int start = Math.max(0, (width - columnWidth * 5) / 2);
                for (int i = 0; i < labels.length; i++) {
                    String label = labels[i];
                    String value = values[i];
                    int x = start + i * columnWidth;
                    g.setBackgroundColor(BG);
                    g.setForegroundColor(MUTED);
                    g.putString(x + Math.max(0, (columnWidth - label.length()) / 2), row, label);
                    g.setForegroundColor(FG);
                    g.putString(x + Math.max(0, (columnWidth - value.length()) / 2), row + 1, value);
                }
                putCentered(g, width, row + 3, "esc restart  ·  ctrl+r same test  ·  ctrl+h history  ·  e errors", MUTED);
                return;
            }
            case HELP -> {
                title = "keybinds";
                String[] help = {
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
                        "esc          close panel / restart test"
                };
                options.addAll(List.of(help));
            }
            default -> {
                title = "";
            }
        }

        int panelWidth = Math.min(Math.max(34, longest(options) + 8), Math.max(34, width - 4));
        int totalHeight = options.size() + 5;
        int x = Math.max(0, (width - panelWidth) / 2);
        int y = Math.max(0, (height - totalHeight) / 2);
        putCentered(g, width, y, title, FG);
        for (int i = 0; i < options.size(); i++) {
            int row = y + 2 + i;
            if (row >= height - 1) break;
            String option = options.get(i);
            boolean active = i == selected;
            String line = panel == Panel.HELP ? option : (active ? "●  " : "○  ") + option;
            int lineX = panel == Panel.HELP ? x + 2 : Math.max(0, (width - line.length()) / 2);
            g.setForegroundColor(active && panel != Panel.HELP ? FG : MUTED);
            if (active && panel != Panel.HELP) {
                g.setForegroundColor(FG);
            }
            g.putString(lineX, row, line.substring(0, Math.min(line.length(), Math.max(0, width - lineX))));
        }
        String footer = panel == Panel.HELP ? "esc close" : "↑↓ / j k move  ·  enter select  ·  esc back";
        putCentered(g, width, Math.min(height - 1, y + totalHeight - 1), footer, MUTED);
    }

    private static int longest(List<String> values) {
        int max = 0;
        for (String value : values) max = Math.max(max, value.length());
        return max;
    }

    private static void putCentered(TextGraphics g, int width, int y, String text, TextColor color) {
        if (width <= 0 || y < 0) return;
        int visibleLength = Math.min(text.length(), width);
        int x = Math.max(0, (width - visibleLength) / 2);
        g.setForegroundColor(color);
        g.putString(x, y, text.substring(0, Math.min(text.length(), width - x)));
    }

    /** Wrap text without dropping source characters, preserving cursor offsets. */
    private static List<Line> wrapPreservingPositions(String text, int width) {
        List<Line> lines = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            if (text.charAt(start) == '\n') {
                lines.add(new Line(start, ""));
                start++;
                continue;
            }
            int end = Math.min(start + width, text.length());
            int newline = text.indexOf('\n', start);
            if (newline >= start && newline < end) end = newline;
            if (end < text.length() && end == start + width) {
                int space = text.lastIndexOf(' ', end);
                if (space > start + width / 2) end = space + 1;
            }
            if (end == start) end = Math.min(start + 1, text.length());
            lines.add(new Line(start, text.substring(start, end)));
            start = end;
            if (start < text.length() && text.charAt(start) == '\n') start++;
        }
        if (lines.isEmpty()) lines.add(new Line(0, ""));
        return lines;
    }

    private record Line(int start, String text) { }
}
