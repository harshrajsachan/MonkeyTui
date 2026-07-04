package com.proto.game;

import com.proto.lang.SnippetLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameState {

  private final List<String> lines = new ArrayList<>();
  private String currentMode = "easy.txt";
  private int currentLine = 0;
  private int currentIndex = 0;
  private final Set<String> mistakes = new HashSet<>();
  private long startTime;
  private final SnippetLoader loader = new SnippetLoader();

  public GameState() {
    loadMode(currentMode);
  }

  public void loadMode(String mode) {

    currentMode = mode;

    lines.clear();

    for (int i = 0; i < 3; i++) {
      lines.add(loader.loadRandom(mode));
    }

    currentLine = 0;
    currentIndex = 0;
    mistakes.clear();
    startTime = System.currentTimeMillis();
  }

  public List<String> getLines() {
    return lines;
  }

  public String getCurrentText() {
    return lines.get(currentLine);
  }

  public int getCurrentLine() {
    return currentLine;
  }

  public int getCurrentIndex() {
    return currentIndex;
  }

  public String getCurrentMode() {
    return currentMode;
  }

  public void advance() {

    currentIndex++;

    if (currentIndex >= getCurrentText().length()) {
      lines.remove(0);
      lines.add(loader.loadRandom(currentMode));
      currentIndex = 0;
    }
  }

  public void addMistake(int line, int index) {
    mistakes.add(line + ":" + index);
  }

  public boolean isMistake(int line, int index) {
    return mistakes.contains(line + ":" + index);
  }

  public int getMistakeCount() {
    return mistakes.size();
  }

  public long getElapsedSeconds() {
    return (System.currentTimeMillis() - startTime) / 1000;
  }

  public int getTypedCharacters() {
    int typed = currentLine * 30;
    typed += currentIndex;
    return typed;
  }
}
