package com.monkeytui.ui;

public enum Panel {
    NONE(""),
    DIFFICULTY("difficulty"),
    TIME("time"),
    HELP("keybinds"),
    LANGUAGE("language"),
    LESSON("lesson"),
    ERRORS("mistyped words"),
    HISTORY("last 10 tests"),
    RESULTS("results");

    private final String title;

    // Stores the menu title for this panel.
    Panel(String title) {
        this.title = title;
    }

    // Returns the menu title for this panel.
    public String title() {
        return title;
    }
}
