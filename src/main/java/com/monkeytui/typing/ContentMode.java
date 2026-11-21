package com.monkeytui.typing;

import java.util.List;
import java.util.Locale;

public enum ContentMode {
    EASY("easy.txt", "easy", false),
    MEDIUM("medium.txt", "medium", false),
    HARD("hard.txt", "hard", false),
    JAVA("java.txt", "Java", true),
    CPP("cpp.txt", "C++", true);

    public static final List<ContentMode> WORD_MODES = List.of(EASY, MEDIUM, HARD);
    public static final List<ContentMode> CODE_MODES = List.of(JAVA, CPP);

    private final String fileName;
    private final String label;
    private final boolean code;

    // Defines the bundled file, menu label, and content category for a mode.
    ContentMode(String fileName, String label, boolean code) {
        this.fileName = fileName;
        this.label = label;
        this.code = code;
    }

    // Returns the bundled resource file for this mode.
    public String fileName() {
        return fileName;
    }

    // Returns the menu label for this mode.
    public String label() {
        return label;
    }

    // Reports whether this mode contains source code.
    public boolean isCode() {
        return code;
    }

    // Returns this mode's index within its word or code category.
    public int menuIndex() {
        return (isCode() ? CODE_MODES : WORD_MODES).indexOf(this);
    }

    // Resolves a file name to a known mode, falling back to easy words.
    public static ContentMode fromFileName(String name) {
        for (ContentMode mode : values()) {
            if (mode.fileName.equals(name)) return mode;
        }
        return EASY;
    }

    // Normalizes a command-line argument into a supported mode.
    public static ContentMode fromArgument(String argument) {
        if (argument == null) return EASY;
        return fromFileName(argument.toLowerCase(Locale.ROOT));
    }
}
