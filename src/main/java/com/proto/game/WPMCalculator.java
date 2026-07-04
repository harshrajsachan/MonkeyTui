package com.proto.game;

public class WPMCalculator {

  public int calculate(GameState state) {

    int typedChars = state.getCurrentIndex();

    int words = typedChars / 5;

    long seconds = Math.max(state.getElapsedSeconds(), 1);

    double minutes = seconds / 60.0;

    return (int) (words / minutes);
  }
}
