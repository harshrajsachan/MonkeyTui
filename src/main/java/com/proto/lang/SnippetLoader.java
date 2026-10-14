package com.proto.lang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Loads bundled word lists and code snippets from the classpath. */
public final class SnippetLoader {
    private static final int WORD_COUNT = 90;
    private static final int CODE_LINES = 7;
    private final Random random = new Random();

    /** Returns a randomized typing passage for a known bundled resource. */
    public String loadRandom(String fileName) {
        String safeName = sanitize(fileName);
        List<String> items = readItems(safeName);
        if (items.isEmpty()) {
            return "No content found for " + safeName + ". Check src/main/resources/snippets/.";
        }

        if (isCodeFile(safeName)) {
            List<String> lines = new ArrayList<>(items);
            Collections.shuffle(lines, random);
            return String.join("\n", lines.subList(0, Math.min(CODE_LINES, lines.size())));
        }

        // A list may have one word per line or several words on each line.
        List<String> words = new ArrayList<>();
        for (String item : items) {
            for (String word : item.trim().split("\\s+")) {
                if (!word.isBlank()) words.add(word);
            }
        }
        if (words.isEmpty()) {
            return "No words found in " + safeName + ".";
        }

        // Avoid repeats until every source word has been used once. If the file
        // has fewer than WORD_COUNT words, reshuffle and cycle only as necessary.
        List<String> passage = new ArrayList<>(WORD_COUNT);
        while (passage.size() < WORD_COUNT) {
            List<String> shuffled = new ArrayList<>(words);
            Collections.shuffle(shuffled, random);
            int count = Math.min(WORD_COUNT - passage.size(), shuffled.size());
            passage.addAll(shuffled.subList(0, count));
        }
        return String.join(" ", passage);
    }

    private List<String> readItems(String fileName) {
        List<String> items = new ArrayList<>();
        try (InputStream stream = getClass().getResourceAsStream("/snippets/" + fileName)) {
            if (stream == null) return items;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String value = line.stripTrailing();
                    if (!value.isBlank()) items.add(value);
                }
            }
        } catch (IOException ignored) {
            // Empty content is converted into a readable message by loadRandom().
        }
        return items;
    }

    private String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()
                || fileName.contains("/") || fileName.contains("\\")) {
            return "easy.txt";
        }
        String candidate = fileName.trim().toLowerCase();
        return switch (candidate) {
            case "easy.txt", "medium.txt", "hard.txt", "java.txt", "cpp.txt" -> candidate;
            default -> "easy.txt";
        };
    }

    private boolean isCodeFile(String fileName) {
        return "java.txt".equals(fileName) || "cpp.txt".equals(fileName);
    }
}
