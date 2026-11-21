package com.monkeytui.app;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.monkeytui.history.HistoryStore;
import com.monkeytui.typing.ContentMode;
import com.monkeytui.typing.Durations;
import com.monkeytui.typing.SnippetLoader;
import com.monkeytui.typing.TypingSession;
import com.monkeytui.ui.Panel;
import com.monkeytui.ui.Renderer;

import java.io.IOException;

public final class MonkeyTui {
    private final SnippetLoader loader = new SnippetLoader();
    private final HistoryStore historyStore = new HistoryStore();
    private final AppState state;
    private final Actions actions;

    // Prepares the initial state and keyboard actions for a mode argument.
    private MonkeyTui(String argument) {
        ContentMode mode = ContentMode.fromArgument(argument);
        state = new AppState(mode, createSession(mode, 0), historyStore.load());
        actions = new Actions(loader, historyStore, state);
    }

    // Starts MonkeyTui from command-line arguments.
    public static void launch(String[] args) {
        try {
            new MonkeyTui(argument(args)).run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            reportFailure(e);
        }
    }

    // Returns the requested mode argument or the default.
    private static String argument(String[] args) {
        return args.length > 0 ? args[0] : "easy.txt";
    }

    // Reports startup failures to the terminal.
    private static void reportFailure(Exception e) {
        System.err.println("MonkeyTui could not start: " + e.getMessage());
        System.err.println("Run it inside a real terminal, not an IDE output pane.");
    }

    // Runs the screen lifecycle and event loop.
    private void run() throws IOException, InterruptedException {
        try (Screen screen = TerminalSupport.openScreen()) {
            drawInitial(screen);
            eventLoop(screen);
            screen.stopScreen();
        }
    }

    // Draws the first frame and records the current displayed second.
    private void drawInitial(Screen screen) throws IOException {
        Renderer.draw(screen, state.screenModel());
        state.dirty = false;
        state.lastDisplayedSecond = state.session.elapsedSeconds();
    }

    // Pumps terminal events until the application exits.
    private void eventLoop(Screen screen) throws IOException, InterruptedException {
        boolean running = true;
        while (running) running = pump(screen);
    }

    // Processes one terminal input cycle.
    private boolean pump(Screen screen) throws IOException, InterruptedException {
        KeyStroke key = screen.pollInput();
        boolean running = key == null || actions.handle(key);
        if (running) refresh(screen);
        if (key == null) Thread.sleep(12);
        return running;
    }

    // Captures results, handles resizing, and redraws when needed.
    private void refresh(Screen screen) throws IOException {
        actions.captureResult();
        boolean resized = screen.doResizeIfNecessary() != null;
        boolean tick = statsTick();
        if (resized || tick || state.dirty) redraw(screen);
    }

    // Reports whether the visible second changed during an active run.
    private boolean statsTick() {
        return state.panel == Panel.NONE && state.session.isStarted()
                && state.session.elapsedSeconds() != state.lastDisplayedSecond;
    }

    // Paints a frame and records the current displayed second.
    private void redraw(Screen screen) throws IOException {
        Renderer.draw(screen, state.screenModel());
        state.dirty = false;
        state.lastDisplayedSecond = state.session.elapsedSeconds();
    }

    // Creates a session configured for the duration at an index.
    private TypingSession createSession(ContentMode mode, int durationIndex) {
        TypingSession session = new TypingSession(loader, mode);
        session.setDurationSeconds(Durations.at(durationIndex));
        return session;
    }
}
