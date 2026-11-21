package com.monkeytui.ui;
import com.googlecode.lanterna.TextColor;

 // Five semantic UI colors, detected once at application startup.
 // OSC detection is best-effort; defaults are used when unavailable.
 
public final class Theme {
    private static final TerminalPalette TERMINAL = TerminalPalette.detect();

    public static final TextColor BACKGROUND = choose(TERMINAL.getBackground(), TextColor.ANSI.DEFAULT);
    public static final TextColor FOREGROUND = choose(TERMINAL.getForeground(), TextColor.ANSI.DEFAULT);
    public static final TextColor MUTED = TERMINAL.selectMuted();
    public static final TextColor ERROR = TERMINAL.selectError();
    public static final TextColor GHOST = TERMINAL.selectGhost();

    private static TextColor choose(TextColor detected, TextColor fallback) {
        return detected == null ? fallback : detected;
    }

    private Theme() {}
}

