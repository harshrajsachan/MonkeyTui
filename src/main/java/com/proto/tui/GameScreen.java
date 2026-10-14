package com.proto.tui;

import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.proto.game.GameState;
import com.proto.game.TypingEngine;

/** Simple alternate screen retained for the GameState-based implementation. */
public final class GameScreen {
    private final GameState state = new GameState();
    private final TypingEngine engine = new TypingEngine(state);
    private final Renderer renderer = new Renderer();

    public void run(Screen screen) throws Exception {
        while (true) {
            screen.clear();
            TextGraphics graphics = screen.newTextGraphics();
            renderer.render(graphics, state);
            screen.refresh();

            KeyStroke key = screen.readInput();
            if (key == null) continue;
            KeyType type = key.getKeyType();
            Character ch = type == KeyType.Character ? key.getCharacter() : null;

            if (key.isCtrlDown() && ch != null
                    && Character.toLowerCase(ch) == 'q') break;
            if (type == KeyType.Escape) {
                state.loadMode(state.getCurrentMode());
                continue;
            }
            if (type == KeyType.F1) state.loadMode("easy.txt");
            else if (type == KeyType.F2) state.loadMode("medium.txt");
            else if (type == KeyType.F3) state.loadMode("hard.txt");
            else if (type == KeyType.F4) state.loadMode("java.txt");
            else if (type == KeyType.Character && ch != null && !key.isCtrlDown() && !key.isAltDown()) {
                engine.handleInput(ch);
            }
        }
    }
}
