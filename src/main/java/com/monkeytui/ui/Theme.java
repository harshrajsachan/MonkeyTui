package com.monkeytui.ui;

import com.googlecode.lanterna.TextColor;

public final class Theme {
    public static final TextColor BACKGROUND = new TextColor.RGB(0, 0, 0);
    public static final TextColor MUTED = new TextColor.RGB(92, 92, 92);
    public static final TextColor FOREGROUND = new TextColor.RGB(225, 225, 225);
    public static final TextColor ERROR = new TextColor.RGB(235, 90, 100);
    public static final TextColor GHOST = new TextColor.RGB(150, 150, 150);

    // Prevents instantiation of this color palette.
    private Theme() {
    }
}
