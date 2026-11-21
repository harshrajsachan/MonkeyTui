package com.monkeytui.ui;

import com.monkeytui.history.RunRecord;
import com.monkeytui.typing.TypingSession;

import java.util.List;

public record ScreenModel(
        TypingSession session,
        Panel panel,
        int panelIndex,
        double previousWpm,
        List<RunRecord> history) {
}
