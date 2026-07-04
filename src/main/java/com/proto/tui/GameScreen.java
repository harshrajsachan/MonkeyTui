package com.proto.tui;

import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.proto.game.GameState;
import com.proto.game.TypingEngine;

public class GameScreen {

  private final GameState state;

  private final TypingEngine engine;

  private final Renderer renderer;

  public GameScreen() {

    state = new GameState();

    engine = new TypingEngine(state);

    renderer = new Renderer();
  }

  public void run(Screen screen) throws Exception {

    while (true) {

      screen.clear();

      TextGraphics tg = screen.newTextGraphics();

      renderer.render(tg, state);

      screen.refresh();

      KeyStroke keyStroke = screen.readInput();

      /*
       * CTRL + C → EXIT APP
       */

      if (keyStroke.isCtrlDown()
          && keyStroke.getKeyType() == KeyType.Character
          && keyStroke.getCharacter() == 'c') {

        break;
      }

      /*
       * ESC → RESET CURRENT MODE
       */

      if (keyStroke.getKeyType() == KeyType.Escape) {

        state.loadMode(state.getCurrentMode());

        continue;
      }

      /*
       * CHARACTER INPUT
       */

      if (keyStroke.getKeyType() == KeyType.Character) {

        char typedChar = keyStroke.getCharacter();

        /*
         * MODE SWITCHING
         */

        if (typedChar == '1') {

          state.loadMode("easy.txt");

          continue;
        }

        if (typedChar == '2') {

          state.loadMode("medium.txt");

          continue;
        }

        if (typedChar == '3') {

          state.loadMode("hard.txt");

          continue;
        }

        if (typedChar == '4') {

          state.loadMode("java.txt");

          continue;
        }

        /*
         * NORMAL TYPING
         */

        engine.handleInput(typedChar);
      }
    }
  }
}
