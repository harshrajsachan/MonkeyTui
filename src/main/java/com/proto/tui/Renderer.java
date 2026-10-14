package com.proto.tui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.proto.game.AccuracyCalculator;
import com.proto.game.GameState;
import com.proto.game.WPMCalculator;

import java.util.List;

/** Renderer retained for the small GameState-based implementation. */
public final class Renderer {
    private final WPMCalculator wpmCalculator = new WPMCalculator();
    private final AccuracyCalculator accuracyCalculator = new AccuracyCalculator();

    public void render(TextGraphics tg, GameState state) {
        tg.setBackgroundColor(TextColor.ANSI.BLACK);
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.putString(3, 1, "F1 Easy  F2 Medium  F3 Hard  F4 Java");
        tg.putString(3, 2, "Esc restart   Ctrl+Q quit");
        tg.putString(3, 4, "WPM: " + wpmCalculator.calculate(state));
        tg.putString(22, 4, "Accuracy: " + accuracyCalculator.calculate(state) + "%");
        tg.putString(48, 4, "Time: " + state.getElapsedSeconds() + "s");

        List<String> lines = state.getLines();
        for (int line = 0; line < lines.size(); line++) {
            String text = lines.get(line);
            int y = 8 + line * 2;
            for (int i = 0; i < text.length(); i++) {
                if (line == 0 && state.isMistake(line, i)) {
                    tg.setForegroundColor(TextColor.ANSI.RED);
                } else if (line == 0 && i < state.getCurrentIndex()) {
                    tg.setForegroundColor(TextColor.ANSI.WHITE);
                } else {
                    tg.setForegroundColor(new TextColor.RGB(105, 105, 105));
                }

                if (line == 0 && i == state.getCurrentIndex()) {
                    tg.setBackgroundColor(TextColor.ANSI.WHITE);
                    tg.setForegroundColor(TextColor.ANSI.BLACK);
                } else {
                    tg.setBackgroundColor(TextColor.ANSI.BLACK);
                }
                tg.putString(3 + i, y, String.valueOf(text.charAt(i)));
            }
        }
        tg.setBackgroundColor(TextColor.ANSI.BLACK);
        tg.setForegroundColor(TextColor.ANSI.WHITE);
    }
}
