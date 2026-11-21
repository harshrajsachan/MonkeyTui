package com.monkeytui.app;

import com.monkeytui.history.RunRecord;
import com.monkeytui.typing.ContentMode;
import com.monkeytui.typing.TypingSession;
import com.monkeytui.ui.Panel;
import com.monkeytui.ui.ScreenModel;

import java.util.ArrayList;
import java.util.List;

public final class AppState {
    public TypingSession session;
    public ContentMode mode;
    public Panel panel = Panel.NONE;
    public int panelIndex;
    public int durationIndex;
    public int difficultyIndex;
    public double previousWpm;
    public boolean resultCaptured;
    public List<RunRecord> history;
    public long lastDisplayedSecond = -1;
    public boolean dirty = true;

    // Creates state for a mode, its session, and the loaded history.
    public AppState(ContentMode mode, TypingSession session, List<RunRecord> history) {
        this.mode = mode;
        this.session = session;
        this.history = new ArrayList<>(history);
        this.difficultyIndex = mode.isCode() ? 0 : mode.menuIndex();
    }

    // Builds the immutable snapshot consumed by the renderer.
    public ScreenModel screenModel() {
        return new ScreenModel(session, panel, panelIndex, previousWpm, history);
    }
}
