package com.proto;

import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.proto.tui.GameScreen;

public class Main {

  public static void main(String[] args) {

    try {

      DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();

      Screen screen = new TerminalScreen(terminalFactory.createTerminal());

      screen.startScreen();

      GameScreen gameScreen = new GameScreen();

      gameScreen.run(screen);

      screen.stopScreen();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
