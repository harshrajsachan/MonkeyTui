package com.monkeytui.app;

import com.googlecode.lanterna.input.BasicCharacterPattern;
import com.googlecode.lanterna.input.CharacterPattern;
import com.googlecode.lanterna.input.InputDecoder;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.ansi.StreamBasedTerminal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class TerminalSupport {
    // Prevents instantiation of this terminal helper.
    private TerminalSupport() {
    }

    // Opens a full-screen terminal with the Ctrl+H shortcut preserved.
    public static Screen openScreen() throws IOException {
        Screen screen = createScreen();
        startSafely(screen);
        return screen;
    }

    // Creates a terminal screen with the Ctrl+H decoder mapping installed.
    private static Screen createScreen() throws IOException {
        Terminal terminal = new DefaultTerminalFactory().createTerminal();
        installCtrlH(terminal);
        return new TerminalScreen(terminal);
    }

    // Starts the screen, closing it if startup fails.
    private static void startSafely(Screen screen) throws IOException {
        try {
            start(screen);
        } catch (IOException | RuntimeException e) {
            closeQuietly(screen);
            throw e;
        }
    }

    // Starts the screen and hides the terminal cursor.
    private static void start(Screen screen) throws IOException {
        screen.startScreen();
        screen.setCursorPosition(null);
    }

    // Closes a screen while preserving the original startup failure.
    private static void closeQuietly(Screen screen) {
        try {
            screen.close();
        } catch (IOException ignored) {
        }
    }

    // Remaps the terminal decoder so Ctrl+H reaches the event loop.
    private static void installCtrlH(Terminal terminal) {
        if (!(terminal instanceof StreamBasedTerminal streamTerminal)) return;
        InputDecoder decoder = streamTerminal.getInputDecoder();
        removeBackspaceMapping(decoder);
        addCtrlHMapping(decoder);
    }

    // Removes the decoder mapping that turns Ctrl+H into Backspace.
    private static void removeBackspaceMapping(InputDecoder decoder) {
        for (CharacterPattern pattern : new ArrayList<>(decoder.getPatterns())) removeIfBackspace(decoder, pattern);
    }

    // Removes a matching Ctrl+H backspace pattern.
    private static void removeIfBackspace(InputDecoder decoder, CharacterPattern pattern) {
        if (pattern instanceof BasicCharacterPattern basic && isCtrlHBackspace(basic)) decoder.removePattern(pattern);
    }

    // Reports whether a pattern is the one-byte Ctrl+H backspace mapping.
    private static boolean isCtrlHBackspace(BasicCharacterPattern pattern) {
        return pattern.getResult().getKeyType() == KeyType.Backspace
                && pattern.getPattern().length == 1
                && pattern.getPattern()[0] == '\b';
    }

    // Adds the Ctrl+H key stroke mapping to the decoder.
    private static void addCtrlHMapping(InputDecoder decoder) {
        KeyStroke ctrlH = new KeyStroke(Character.valueOf('h'), true, false);
        decoder.addProfile(() -> List.of(new BasicCharacterPattern(ctrlH, '\b')));
    }
}
