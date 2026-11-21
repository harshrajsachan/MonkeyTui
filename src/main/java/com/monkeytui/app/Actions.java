package com.monkeytui.app;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.monkeytui.history.HistoryStore;
import com.monkeytui.history.RunRecord;
import com.monkeytui.typing.ContentMode;
import com.monkeytui.typing.Durations;
import com.monkeytui.typing.SnippetLoader;
import com.monkeytui.typing.TypingSession;
import com.monkeytui.ui.Panel;

public final class Actions {
    private final SnippetLoader loader;
    private final HistoryStore historyStore;
    private final AppState state;

    // Creates the keyboard action handler for the application state.
    public Actions(SnippetLoader loader, HistoryStore historyStore, AppState state) {
        this.loader = loader;
        this.historyStore = historyStore;
        this.state = state;
    }

    // Handles one key, returning false when the application should exit.
    public boolean handle(KeyStroke key) {
        if (Keys.isQuit(key)) return false;
        if (handleGlobal(key)) return true;
        if (state.panel == Panel.NONE) handleTest(key);
        else handlePanel(key);
        return true;
    }

    // Handles shortcuts available from the test and results screens.
    private boolean handleGlobal(KeyStroke key) {
        if (!canUseHistory()) return false;
        if (Keys.isCtrl(key, 'h')) return openHistory();
        if (Keys.isCtrl(key, 'r')) return retake();
        return false;
    }

    // Reports whether history shortcuts are available in the current panel.
    private boolean canUseHistory() {
        return state.panel == Panel.NONE || state.panel == Panel.RESULTS;
    }

    // Opens the history panel with freshly loaded records.
    private boolean openHistory() {
        state.history = historyStore.load();
        state.panel = Panel.HISTORY;
        state.panelIndex = 0;
        state.dirty = true;
        return true;
    }

    // Retakes the exact same passage with the current settings.
    private boolean retake() {
        state.session.restartSameText();
        state.panel = Panel.NONE;
        state.resultCaptured = false;
        state.dirty = true;
        return true;
    }

    // Routes a key while the typing test is active.
    private void handleTest(KeyStroke key) {
        if (!handleTestShortcut(key)) handleTestInput(key);
    }

    // Handles restart and configuration shortcuts on the test screen.
    private boolean handleTestShortcut(KeyStroke key) {
        if (handleRestartShortcut(key)) return true;
        return handleConfigShortcut(key);
    }

    // Handles escape, difficulty, and duration shortcuts.
    private boolean handleRestartShortcut(KeyStroke key) {
        if (key.getKeyType() == KeyType.Escape) {
            restartFresh();
            return true;
        }
        if (Keys.isCtrl(key, 'd')) return openPanel(Panel.DIFFICULTY, state.difficultyIndex);
        if (Keys.isCtrl(key, 't') || key.getKeyType() == KeyType.Tab) return openPanel(Panel.TIME, state.durationIndex);
        return false;
    }

    // Handles mode, language, lesson, and help shortcuts.
    private boolean handleConfigShortcut(KeyStroke key) {
        if (Keys.isCtrl(key, 'w')) return toggleMode();
        if (Keys.isCtrl(key, 'l')) return openPanel(Panel.LANGUAGE, codeSelection());
        if (Keys.isCtrl(key, 'o')) return openPanel(Panel.LESSON, codeSelection());
        if (Keys.isCharacter(key, '?')) return openPanel(Panel.HELP, 0);
        return false;
    }

    // Applies typing, backspace, and enter input.
    private void handleTestInput(KeyStroke key) {
        if (key.getKeyType() == KeyType.Backspace) backspace();
        else if (key.getKeyType() == KeyType.Enter) enter();
        else if (Keys.isPlainCharacter(key)) typeCharacter(key);
    }

    // Removes the previous character from the session.
    private void backspace() {
        state.session.backspace();
        state.dirty = true;
    }

    // Advances to the next line in the session.
    private void enter() {
        state.session.enter();
        state.dirty = true;
    }

    // Types a printable character into the session.
    private void typeCharacter(KeyStroke key) {
        state.session.type(key.getCharacter());
        state.dirty = true;
    }

    // Starts a fresh randomized run and clears the previous pace.
    private void restartFresh() {
        state.session.restart();
        state.resultCaptured = false;
        state.previousWpm = 0;
        state.panel = Panel.NONE;
        state.dirty = true;
    }

    // Opens a panel at the given selection index.
    private boolean openPanel(Panel panel, int index) {
        state.panel = panel;
        state.panelIndex = index;
        state.dirty = true;
        return true;
    }

    // Switches between words and code content.
    private boolean toggleMode() {
        ContentMode next = state.mode.isCode() ? ContentMode.EASY : ContentMode.JAVA;
        replaceSession(next, state.durationIndex);
        state.difficultyIndex = next.isCode() ? 0 : next.menuIndex();
        state.dirty = true;
        return true;
    }

    // Replaces the session for a mode while keeping a duration index.
    private void replaceSession(ContentMode mode, int durationIndex) {
        state.mode = mode;
        state.durationIndex = durationIndex;
        state.session = createSession(mode, durationIndex);
    }

    // Creates a session configured with the duration at an index.
    private TypingSession createSession(ContentMode mode, int durationIndex) {
        TypingSession session = new TypingSession(loader, mode);
        session.setDurationSeconds(Durations.at(durationIndex));
        return session;
    }

    // Returns the initial language selection for the current mode.
    private int codeSelection() {
        return state.mode.isCode() ? state.mode.menuIndex() : 0;
    }

    // Routes a key while a panel is open.
    private void handlePanel(KeyStroke key) {
        if (handlePanelDismiss(key)) return;
        if (handlePanelNavigation(key)) return;
        if (handleResultsReview(key)) return;
        handlePanelChoice(key);
    }

    // Applies confirm or close keys for non-results panels.
    private void handlePanelChoice(KeyStroke key) {
        if (state.panel == Panel.RESULTS) return;
        if (isConfirmKey(key)) confirmPanel();
        else if (Keys.isCharacter(key, '?') || Keys.isCharacter(key, 'q')) closePanel();
    }

    // Handles escape and backspace dismissal semantics.
    private boolean handlePanelDismiss(KeyStroke key) {
        if (key.getKeyType() == KeyType.Escape) {
            dismissPanel();
            return true;
        }
        if (key.getKeyType() != KeyType.Backspace || state.panel == Panel.RESULTS) return false;
        backOutOfPanel();
        return true;
    }

    // Dismisses the active panel according to its type.
    private void dismissPanel() {
        if (state.panel == Panel.RESULTS) restartFresh();
        else if (state.panel == Panel.ERRORS) showResults();
        else closePanel();
    }

    // Moves back one level from the errors panel.
    private void backOutOfPanel() {
        if (state.panel == Panel.ERRORS) showResults();
        else closePanel();
    }

    // Returns from a secondary panel to the results screen.
    private void showResults() {
        state.panel = Panel.RESULTS;
        state.dirty = true;
    }

    // Closes the active panel and returns to the test screen.
    private void closePanel() {
        state.panel = Panel.NONE;
        state.dirty = true;
    }

    // Handles arrow and j/k selection movement.
    private boolean handlePanelNavigation(KeyStroke key) {
        int delta = navigationDelta(key);
        if (delta == 0) return false;
        moveSelection(delta);
        return true;
    }

    // Returns the requested selection movement for a key.
    private int navigationDelta(KeyStroke key) {
        if (Keys.isUp(key)) return -1;
        if (Keys.isDown(key)) return 1;
        return 0;
    }

    // Moves the panel selection within valid bounds.
    private void moveSelection(int delta) {
        int last = panelSize() - 1;
        state.panelIndex = Math.max(0, Math.min(last, state.panelIndex + delta));
        state.dirty = true;
    }

    // Returns the number of selectable entries in the active panel.
    private int panelSize() {
        if (state.panel == Panel.DIFFICULTY) return ContentMode.WORD_MODES.size();
        if (state.panel == Panel.TIME) return Durations.count();
        if (isCodePanel(state.panel)) return ContentMode.CODE_MODES.size();
        if (state.panel == Panel.HISTORY) return Math.max(1, state.history.size());
        return 1;
    }

    // Reports whether the panel chooses a code language or lesson.
    private boolean isCodePanel(Panel panel) {
        return panel == Panel.LANGUAGE || panel == Panel.LESSON;
    }

    // Opens the error review from the results panel.
    private boolean handleResultsReview(KeyStroke key) {
        if (!isResultsErrorKey(key)) return false;
        openErrors();
        return true;
    }

    // Reports whether the key opens error review from results.
    private boolean isResultsErrorKey(KeyStroke key) {
        if (state.panel != Panel.RESULTS || key.isCtrlDown()) return false;
        return Keys.isCharacter(key, 'e') || Keys.isCharacter(key, 'E');
    }

    // Opens the mistyped words panel.
    private void openErrors() {
        state.panel = Panel.ERRORS;
        state.panelIndex = 0;
        state.dirty = true;
    }

    // Reports whether the key confirms the current selection.
    private boolean isConfirmKey(KeyStroke key) {
        return key.getKeyType() == KeyType.Enter || Keys.isCharacter(key, ' ');
    }

    // Applies the confirmation behavior of the active panel.
    private void confirmPanel() {
        if (state.panel == Panel.HISTORY) selectHistory();
        else if (state.panel == Panel.DIFFICULTY) chooseDifficulty();
        else if (state.panel == Panel.TIME) chooseDuration();
        else if (isCodePanel(state.panel)) chooseCode();
        else closePanel();
    }

    // Loads the selected history record or closes the empty panel.
    private void selectHistory() {
        if (state.history.isEmpty()) {
            closePanel();
            return;
        }
        applyRecord(state.history.get(Math.min(state.panelIndex, state.history.size() - 1)));
    }

    // Applies a saved run as the current test.
    private void applyRecord(RunRecord record) {
        prepareRecordSession(record);
        state.previousWpm = record.wpm();
        state.resultCaptured = false;
        state.panel = Panel.NONE;
        state.dirty = true;
    }

    // Recreates a saved session with its mode, duration, and passage.
    private void prepareRecordSession(RunRecord record) {
        state.mode = record.mode();
        state.durationIndex = Durations.indexOf(record.duration());
        state.difficultyIndex = record.mode().isCode() ? 0 : record.mode().menuIndex();
        state.session = new TypingSession(loader, record.mode());
        state.session.setDurationSeconds(record.duration());
        state.session.restartWithText(record.text());
    }

    // Applies the selected difficulty mode.
    private void chooseDifficulty() {
        int index = clampSelection(ContentMode.WORD_MODES.size());
        replaceSession(ContentMode.WORD_MODES.get(index), state.durationIndex);
        state.difficultyIndex = index;
        closePanel();
    }

    // Applies the selected test duration.
    private void chooseDuration() {
        state.durationIndex = clampSelection(Durations.count());
        state.session.setDurationSeconds(Durations.at(state.durationIndex));
        state.session.restart();
        closePanel();
    }

    // Applies the selected code language or lesson.
    private void chooseCode() {
        ContentMode mode = ContentMode.CODE_MODES.get(clampSelection(ContentMode.CODE_MODES.size()));
        replaceSession(mode, state.durationIndex);
        closePanel();
    }

    // Clamps the panel selection to a valid list index.
    private int clampSelection(int size) {
        return Math.max(0, Math.min(state.panelIndex, size - 1));
    }

    // Captures a completed run and moves to the results panel.
    public void captureResult() {
        boolean finished = state.panel == Panel.NONE && state.session.isStarted() && state.session.isComplete();
        if (finished) captureFinished();
    }

    // Captures the result once and displays the results panel.
    private void captureFinished() {
        if (!state.resultCaptured) rememberResult();
        state.panel = Panel.RESULTS;
        state.dirty = true;
    }

    // Stores the completed run at the front of the history.
    private void rememberResult() {
        state.previousWpm = state.session.wpm();
        state.resultCaptured = true;
        RunRecord record = new RunRecord(state.mode, state.session.durationSeconds(), state.previousWpm, state.session.text());
        state.history.add(0, record);
        trimHistory();
        historyStore.save(state.history);
    }

    // Keeps only the ten newest history records.
    private void trimHistory() {
        while (state.history.size() > 10) state.history.remove(state.history.size() - 1);
    }
}
