package com.monkeytui.typing;

import java.util.List;

public final class Durations {
    public static final List<Integer> ALL = List.of(15, 30, 60, 120, 300);

    // Prevents instantiation of this duration catalog.
    private Durations() {
    }

    // Returns a duration by index, clamping invalid indices.
    public static int at(int index) {
        int safe = Math.max(0, Math.min(index, ALL.size() - 1));
        return ALL.get(safe);
    }

    // Returns the menu index for a duration, defaulting to the first entry.
    public static int indexOf(int seconds) {
        int index = ALL.indexOf(seconds);
        return index < 0 ? 0 : index;
    }

    // Returns the number of available durations.
    public static int count() {
        return ALL.size();
    }
}
