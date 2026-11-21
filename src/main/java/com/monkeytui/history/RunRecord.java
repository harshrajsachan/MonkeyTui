package com.monkeytui.history;

import com.monkeytui.typing.ContentMode;

public record RunRecord(ContentMode mode, int duration, double wpm, String text) {
}
