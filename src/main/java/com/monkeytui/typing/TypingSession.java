package com.monkeytui.typing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class TypingSession {
    private final SnippetLoader loader;
    private final ContentMode mode;
    private String text;
    private final StringBuilder input = new StringBuilder();
    private final Set<Integer> errors = new HashSet<>();
    private long startedAtNanos;
    private long finishedAtNanos;
    private int totalMistakes;
    private int durationSeconds = 300;

    // Creates a session loaded with a random passage for the mode.
    public TypingSession(SnippetLoader loader, ContentMode mode) {
        this.loader = loader;
        this.mode = mode;
        this.text = loader.loadRandom(mode.fileName());
    }

    // Records one typed character and its correctness.
    public void type(char c) {
        if (isComplete() || Character.isISOControl(c)) return;
        startClock();
        char expected = text.charAt(input.length());
        if (expected == '\n' || c == '\n') return;
        appendCharacter(c, expected);
    }

    // Appends a character and records a mistake when it is wrong.
    private void appendCharacter(char c, char expected) {
        int position = input.length();
        input.append(c);
        if (c != expected) recordMistake(position);
    }

    // Records an error at the given typed position.
    private void recordMistake(int position) {
        errors.add(position);
        totalMistakes++;
    }

    // Advances to the next line when a newline is expected.
    public void enter() {
        if (isComplete()) return;
        startClock();
        if (text.charAt(input.length()) == '\n') input.append('\n');
    }

    // Removes the last typed character and its error mark.
    public void backspace() {
        if (input.length() == 0 || isComplete()) return;
        input.deleteCharAt(input.length() - 1);
        errors.remove(input.length());
    }

    // Starts a new run with a random passage for the same mode.
    public void restart() {
        restartWithText(loader.loadRandom(mode.fileName()));
    }

    // Starts a new run with the exact same passage.
    public void restartSameText() {
        restartWithText(text);
    }

    // Resets the run, optionally using a supplied passage.
    public void restartWithText(String passage) {
        text = passage == null || passage.isEmpty() ? loader.loadRandom(mode.fileName()) : passage;
        input.setLength(0);
        resetMetrics();
    }

    // Clears all counters and clocks for a fresh run.
    private void resetMetrics() {
        errors.clear();
        totalMistakes = 0;
        startedAtNanos = 0;
        finishedAtNanos = 0;
    }

    // Returns the current target passage.
    public String text() {
        return text;
    }

    // Returns the number of typed characters.
    public int position() {
        return input.length();
    }

    // Returns the typed character at a position.
    public char typedCharAt(int position) {
        return input.charAt(position);
    }

    // Reports whether typing has started.
    public boolean isStarted() {
        return startedAtNanos != 0;
    }

    // Reports whether the passage or time limit has been completed.
    public boolean isComplete() {
        if (input.length() >= text.length()) return freezeAndComplete();
        if (hasTimedOut()) return freezeAndComplete();
        return false;
    }

    // Reports whether the configured duration has elapsed.
    private boolean hasTimedOut() {
        return durationSeconds > 0 && isStarted() && elapsedSeconds() >= durationSeconds;
    }

    // Freezes the clock and reports completion.
    private boolean freezeAndComplete() {
        freezeClock();
        return true;
    }

    // Freezes the finishing time once when a run ends.
    private void freezeClock() {
        if (startedAtNanos != 0 && finishedAtNanos == 0) finishedAtNanos = System.nanoTime();
    }

    // Starts the clock on the first accepted input.
    private void startClock() {
        if (startedAtNanos == 0) startedAtNanos = System.nanoTime();
    }

    // Sets the time limit for this session.
    public void setDurationSeconds(int seconds) {
        durationSeconds = Math.max(0, seconds);
    }

    // Returns the configured time limit.
    public int durationSeconds() {
        return durationSeconds;
    }

    // Returns the remaining seconds, or -1 when unlimited.
    public long remainingSeconds() {
        if (durationSeconds == 0) return -1;
        if (!isStarted()) return durationSeconds;
        return Math.max(0, durationSeconds - elapsedSeconds());
    }

    // Reports whether the character at a position was mistyped.
    public boolean isError(int position) {
        return errors.contains(position);
    }

    // Returns the total number of mistyped characters.
    public int totalMistakes() {
        return totalMistakes;
    }

    // Returns unique target words containing a mistyped character.
    public List<String> errorWords() {
        Set<String> words = new LinkedHashSet<>();
        for (int position : errors) addErrorWord(words, position);
        return new ArrayList<>(words);
    }

    // Adds the target word around an error position to the result set.
    private void addErrorWord(Set<String> words, int position) {
        int start = wordStart(position);
        int end = wordEnd(position);
        if (start < end) words.add(text.substring(start, end).replace("\\n", ""));
    }

    // Finds the start of the word containing a position.
    private int wordStart(int position) {
        int start = Math.max(0, Math.min(position, text.length()));
        while (start > 0 && !Character.isWhitespace(text.charAt(start - 1))) start--;
        return start;
    }

    // Finds the end of the word containing a position.
    private int wordEnd(int position) {
        int end = Math.max(0, Math.min(position, text.length()));
        while (end < text.length() && !Character.isWhitespace(text.charAt(end))) end++;
        return end;
    }

    // Returns the number of correctly typed characters.
    public int correctCharacters() {
        return Math.max(0, input.length() - errors.size());
    }

    // Returns the elapsed whole seconds for the current run.
    public long elapsedSeconds() {
        if (!isStarted()) return 0;
        long endpoint = finishedAtNanos == 0 ? System.nanoTime() : finishedAtNanos;
        return Math.max(0, (endpoint - startedAtNanos) / 1_000_000_000L);
    }

    // Returns the correct-character words-per-minute rate.
    public double wpm() {
        if (!isStarted()) return 0;
        return correctCharacters() / 5.0 / minutesElapsed();
    }

    // Returns the raw typed-character words-per-minute rate.
    public double rawWpm() {
        if (!isStarted()) return 0;
        return input.length() / 5.0 / minutesElapsed();
    }

    // Returns elapsed minutes with a minimum measuring window.
    private double minutesElapsed() {
        long endpoint = finishedAtNanos == 0 ? System.nanoTime() : finishedAtNanos;
        double minutes = (endpoint - startedAtNanos) / 60_000_000_000.0;
        return Math.max(minutes, 1.0 / 60.0);
    }

    // Returns the percentage of typed characters that were correct.
    public double accuracy() {
        int typed = input.length();
        return typed == 0 ? 100.0 : Math.max(0.0, (typed - errors.size()) * 100.0 / typed);
    }
}
