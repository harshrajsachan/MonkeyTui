package com.proto.tui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.proto.game.AccuracyCalculator;
import com.proto.game.GameState;
import com.proto.game.WPMCalculator;
import java.util.List;

public class Renderer {

  private final WPMCalculator wpmCalculator = new WPMCalculator();

  private final AccuracyCalculator accuracyCalculator = new AccuracyCalculator();

  public void render(TextGraphics tg, GameState state) {

    tg.setForegroundColor(TextColor.ANSI.CYAN);

    tg.putString(5, 1, "[1] Easy  [2] Medium  [3] Hard  [4] Snippets");

    tg.putString(5, 2, "ESC = Reset   CTRL+C = Exit");

    tg.putString(55, 2, "Mode: " + state.getCurrentMode());

    tg.putString(5, 4, "WPM: " + wpmCalculator.calculate(state));

    tg.putString(25, 4, "Accuracy: " + accuracyCalculator.calculate(state) + "%");

    tg.putString(55, 4, "Time: " + state.getElapsedSeconds() + "s");

    List<String> lines = state.getLines();

    for (int line = 0; line < lines.size(); line++) {

      String text = lines.get(line);

      for (int i = 0; i < text.length(); i++) {

        char c = text.charAt(i);

        if (line == 0) {

          if (state.isMistake(line, i)) {

            tg.setForegroundColor(TextColor.ANSI.RED);

          } else if (i < state.getCurrentIndex()) {

            tg.setForegroundColor(TextColor.ANSI.GREEN);

          } else {

            tg.setForegroundColor(TextColor.ANSI.WHITE);
          }

        } else {

          tg.setForegroundColor(TextColor.ANSI.WHITE);
        }

        tg.putString(5 + i, 10 + (line * 2), String.valueOf(c));
      }
    }

    tg.setForegroundColor(TextColor.ANSI.YELLOW);

    tg.putString(5 + state.getCurrentIndex(), 12, "^");
  }
}
