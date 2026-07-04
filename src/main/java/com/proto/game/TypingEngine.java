package com.proto.game;

public class TypingEngine {

  private final GameState state;

  public TypingEngine(GameState state) {
    this.state = state;
  }

  public void handleInput(char typedChar) {

    String text = state.getCurrentText();

    int index = state.getCurrentIndex();

    if (index >= text.length()) {
      return;
    }

    char expected = text.charAt(index);

    if (typedChar != expected) {

      state.addMistake(state.getCurrentLine(), index);
    }

    state.advance();
  }
}
