package com.monkeytui.history;

import com.monkeytui.typing.ContentMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class HistoryStore {
    private static final int MAX_RECORDS = 10;
    private final Path path;

    // Uses the standard per-user MonkeyTui history file.
    public HistoryStore() {
        this(defaultPath());
    }

    // Uses an explicit history file, mainly for tests.
    public HistoryStore(Path path) {
        this.path = path;
    }

    // Returns the standard history path under the user's data directory.
    private static Path defaultPath() {
        return Path.of(System.getProperty("user.home"), ".local", "share", "monkeytui", "history.tsv");
    }

    // Loads up to ten records, silently preserving data parsed before an error.
    public List<RunRecord> load() {
        List<RunRecord> records = new ArrayList<>();
        if (!Files.isRegularFile(path)) return records;
        try {
            readInto(records, Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
        return records;
    }

    // Reads records until the history limit is reached.
    private void readInto(List<RunRecord> records, List<String> lines) {
        for (String line : lines) {
            addRecord(records, line);
            if (records.size() == MAX_RECORDS) return;
        }
    }

    // Adds one valid TSV line to the record list.
    private void addRecord(List<RunRecord> records, String line) {
        String[] fields = line.split("\\t", 4);
        if (fields.length == 4) records.add(parse(fields));
    }

    // Parses a single history record from TSV fields.
    private RunRecord parse(String[] fields) {
        String text = new String(Base64.getDecoder().decode(fields[3]), StandardCharsets.UTF_8);
        return new RunRecord(ContentMode.fromFileName(fields[0]), Integer.parseInt(fields[1]), Double.parseDouble(fields[2]), text);
    }

    // Saves at most ten records, silently ignoring write failures.
    public void save(List<RunRecord> records) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, encode(records), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    // Encodes the newest records as TSV lines.
    private List<String> encode(List<RunRecord> records) {
        List<String> lines = new ArrayList<>();
        for (RunRecord record : records.subList(0, Math.min(MAX_RECORDS, records.size()))) lines.add(encode(record));
        return lines;
    }

    // Encodes one record as a tab-separated line.
    private String encode(RunRecord record) {
        String text = Base64.getEncoder().encodeToString(record.text().getBytes(StandardCharsets.UTF_8));
        return record.mode().fileName() + "\t" + record.duration() + "\t" + record.wpm() + "\t" + text;
    }
}
