package com.proto.game;

import com.proto.lang.SnippetLoader;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Session state is independent of rendering so the game logic can be tested. */
public final class TypingSession {
    private final SnippetLoader loader;
    private final String mode;
    private String text;
    private final StringBuilder input = new StringBuilder();
    private final Set<Integer> errors = new HashSet<>();
    private long startedAtNanos;
    private long finishedAtNanos;
    private int totalMistakes;
    private int durationSeconds = 300; // default to five minutes; no unlimited mode

    public TypingSession(SnippetLoader loader, String mode) {
        this.loader = loader;
        this.mode = mode;
        this.text = loader.loadRandom(mode);
    }

    public void type(char c) {
        if (isComplete() || Character.isISOControl(c)) return;
        if (startedAtNanos == 0) startedAtNanos = System.nanoTime();
        int position = input.length();
        char expected = text.charAt(position);
        // Newlines in code snippets are advanced with Enter; spaces cannot silently skip them.
        if (expected == '\n' || c == '\n') return;
        input.append(c);
        if (c != expected) {
            errors.add(position);
            totalMistakes++;
        }
    }

    public void enter() {
        if (isComplete()) return;
        if (startedAtNanos == 0) startedAtNanos = System.nanoTime();
        int position = input.length();
        if (text.charAt(position) == '\n') input.append('\n');
        // Ignore Enter unless the expected source character is a newline.
        else return;
    }

    public void backspace() {
        if (input.length() == 0 || isComplete()) return;
        int position = input.length() - 1;
        input.deleteCharAt(position);
        errors.remove(position);
    }

    public void restart() {
        restartWithText(loader.loadRandom(mode));
    }

    /** Retake the exact same passage, resetting metrics but preserving the target text. */
    public void restartSameText() {
        restartWithText(text);
    }

    public void restartWithText(String passage) {
        text = passage == null || passage.isEmpty() ? loader.loadRandom(mode) : passage;
        input.setLength(0);
        errors.clear();
        totalMistakes = 0;
        startedAtNanos = 0;
        finishedAtNanos = 0;
    }

    public String text() { return text; }
    public int position() { return input.length(); }
    public boolean isStarted() { return startedAtNanos != 0; }
    public boolean isComplete() {
        if (input.length() >= text.length()) {
            freezeClock();
            return true;
        }
        if (durationSeconds > 0 && isStarted() && elapsedSeconds() >= durationSeconds) {
            freezeClock();
            return true;
        }
        return false;
    }

    private void freezeClock() {
        if (startedAtNanos != 0 && finishedAtNanos == 0) {
            finishedAtNanos = System.nanoTime();
        }
    }

    public void setDurationSeconds(int seconds) {
        durationSeconds = Math.max(0, seconds);
    }

    public int durationSeconds() {
        return durationSeconds;
    }

    public long remainingSeconds() {
        if (durationSeconds == 0) return -1;
        if (!isStarted()) return durationSeconds;
        return Math.max(0, durationSeconds - elapsedSeconds());
    }
    public boolean isError(int position) { return errors.contains(position); }
    public int totalMistakes() { return totalMistakes; }

    /** Unique target words containing at least one mistyped character. */
    public List<String> errorWords() {
        Set<String> words = new java.util.LinkedHashSet<>();
        for (int position : errors) {
            int start = Math.max(0, Math.min(position, text.length()));
            while (start > 0 && !Character.isWhitespace(text.charAt(start - 1))) start--;
            int end = Math.max(0, Math.min(position, text.length()));
            while (end < text.length() && !Character.isWhitespace(text.charAt(end))) end++;
            if (start < end) words.add(text.substring(start, end).replace("\\n", ""));
        }
        return new ArrayList<>(words);
    }

    public int correctCharacters() { return Math.max(0, input.length() - errors.size()); }
    public long elapsedSeconds() {
        if (!isStarted()) return 0;
        long endpoint = finishedAtNanos == 0 ? System.nanoTime() : finishedAtNanos;
        return Math.max(0, (endpoint - startedAtNanos) / 1_000_000_000L);
    }
    public double wpm() {
        if (!isStarted()) return 0;
        long endpoint = finishedAtNanos == 0 ? System.nanoTime() : finishedAtNanos;
        double minutes = Math.max((endpoint - startedAtNanos) / 60_000_000_000.0, 1.0 / 60.0);
        return correctCharacters() / 5.0 / minutes;
    }
    public double accuracy() {
        int typed = input.length();
        return typed == 0 ? 100.0 : Math.max(0.0, (typed - errors.size()) * 100.0 / typed);
    }

    public double rawWpm() {
        if (!isStarted()) return 0;
        long endpoint = finishedAtNanos == 0 ? System.nanoTime() : finishedAtNanos;
        double minutes = Math.max((endpoint - startedAtNanos) / 60_000_000_000.0, 1.0 / 60.0);
        return input.length() / 5.0 / minutes;
    }
}
