package com.monkeytui.typing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class SnippetLoader {
    private static final int WORD_COUNT = 90;
    private static final int CODE_LINES = 7;
    private final Random random = new Random();

    // Returns a randomized typing passage for a bundled resource.
    public String loadRandom(String fileName) {
        String safeName = sanitize(fileName);
        List<String> items = readItems(safeName);
        if (items.isEmpty()) return "No content found for " + safeName + ". Check src/main/resources/snippets/.";
        return isCodeFile(safeName) ? codePassage(items) : wordPassage(items, safeName);
    }

    // Builds a shuffled selection of source-code lines.
    private String codePassage(List<String> items) {
        List<String> lines = new ArrayList<>(items);
        Collections.shuffle(lines, random);
        List<String> selected = lines.subList(0, Math.min(CODE_LINES, lines.size()));
        return String.join("\n", selected);
    }

    // Builds a word passage, cycling the source words when needed.
    private String wordPassage(List<String> items, String safeName) {
        List<String> words = splitWords(items);
        if (words.isEmpty()) return "No words found in " + safeName + ".";
        return String.join(" ", fillPassage(words));
    }

    // Splits source lines into individual words.
    private List<String> splitWords(List<String> items) {
        List<String> words = new ArrayList<>();
        for (String item : items) Collections.addAll(words, item.trim().split("\\s+"));
        return words;
    }

    // Fills a passage without repeating a word until the source is exhausted.
    private List<String> fillPassage(List<String> words) {
        List<String> passage = new ArrayList<>(WORD_COUNT);
        while (passage.size() < WORD_COUNT) addShuffledWords(passage, words);
        return passage;
    }

    // Appends one freshly shuffled chunk to the passage.
    private void addShuffledWords(List<String> passage, List<String> words) {
        List<String> shuffled = new ArrayList<>(words);
        Collections.shuffle(shuffled, random);
        int count = Math.min(WORD_COUNT - passage.size(), shuffled.size());
        passage.addAll(shuffled.subList(0, count));
    }

    // Reads nonblank resource lines from the classpath.
    private List<String> readItems(String fileName) {
        try (InputStream stream = open(fileName)) {
            return stream == null ? List.of() : readLines(stream);
        } catch (IOException ignored) {
            return List.of();
        }
    }

    // Opens a bundled snippet resource.
    private InputStream open(String fileName) {
        return getClass().getResourceAsStream("/snippets/" + fileName);
    }

    // Reads nonblank lines from an input stream.
    private List<String> readLines(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            List<String> result = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) addLine(result, line);
            return result;
        }
    }

    // Adds a nonblank line after trimming trailing whitespace.
    private void addLine(List<String> items, String line) {
        String value = line.stripTrailing();
        if (!value.isBlank()) items.add(value);
    }

    // Converts any input into a safe bundled resource name.
    private String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank() || fileName.contains("/") || fileName.contains("\\")) return "easy.txt";
        return ContentMode.fromFileName(fileName.trim().toLowerCase(Locale.ROOT)).fileName();
    }

    // Reports whether the resource contains source code.
    private boolean isCodeFile(String fileName) {
        return ContentMode.fromFileName(fileName).isCode();
    }
}
