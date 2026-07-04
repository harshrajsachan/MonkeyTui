package com.proto.game;

public class AccuracyCalculator {

  public int calculate(GameState state) {

    int typed = state.getCurrentIndex();

    int mistakes = state.getMistakeCount();

    if (typed == 0) {
      return 100;
    }

    double accuracy = ((typed - mistakes) / (double) typed) * 100;

    return (int) accuracy;
  }
}
