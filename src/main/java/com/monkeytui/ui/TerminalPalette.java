package com.monkeytui.ui;

import com.googlecode.lanterna.TextColor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the active terminal's OSC colors (OSC 10, 11 and 4).
 *
 * Linux/Unix implementation intended for Kitty and other OSC-capable terminals.
 * Querying is best-effort: unsupported terminals, non-TTY launches, and timeouts
 * are handled by returning an empty result so Theme can use its fallback palette.
 */
public final class TerminalPalette {
    private static final Pattern RGB = Pattern.compile(
        "(?i)rgb:([0-9a-f]{1,4})/([0-9a-f]{1,4})/([0-9a-f]{1,4})"
    );
    private static final Pattern HEX = Pattern.compile("(?i)#([0-9a-f]{6})");
    private static final int QUERY_TIMEOUT_TENTHS = 2;

    private final Map<Integer, TextColor> colors;
    private final TextColor foreground;
    private final TextColor background;

    private TerminalPalette(Map<Integer, TextColor> colors, TextColor foreground,
                            TextColor background) {
        this.colors = Collections.unmodifiableMap(new HashMap<>(colors));
        this.foreground = foreground;
        this.background = background;
    }

    public static TerminalPalette detect() {
        Map<Integer, TextColor> palette = new HashMap<>();
        TextColor foreground = null;
        TextColor background = null;
        String savedMode = null;

        try {
            File tty = new File("/dev/tty");
            if (!tty.exists()) return empty();

            savedMode = runStty("-g");
            if (savedMode == null || savedMode.isEmpty()) return empty();

            if (!runStty("raw", "-echo", "min", "0", "time",
                         Integer.toString(QUERY_TIMEOUT_TENTHS))) return empty();

            try (FileInputStream input = new FileInputStream(tty);
                 FileOutputStream output = new FileOutputStream(tty)) {
                foreground = query(output, input, "10", -1);
                background = query(output, input, "11", -1);
                for (int i = 0; i < 16; i++) {
                    TextColor color = query(output, input, "4;" + i, i);
                    if (color != null) palette.put(i, color);
                }
            }
        } catch (Exception ignored) {
            // Detection is optional; Theme supplies safe defaults.
        } finally {
            if (savedMode != null && !savedMode.isEmpty()) runStty(savedMode);
        }

        return new TerminalPalette(palette, foreground, background);
    }

    private static TerminalPalette empty() {
        return new TerminalPalette(Collections.emptyMap(), null, null);
    }

    private static boolean runStty(String... args) {
        try {
            List<String> command = new ArrayList<>();
            command.add("stty");
            command.addAll(Arrays.asList(args));
            Process process = new ProcessBuilder(command)
                .redirectInput(new File("/dev/tty"))
                .redirectErrorStream(true)
                .start();
            return process.waitFor() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String runStty(String arg) {
        try {
            Process process = new ProcessBuilder("stty", arg)
                .redirectInput(new File("/dev/tty"))
                .redirectErrorStream(true)
                .start();
            byte[] bytes = process.getInputStream().readAllBytes();
            return process.waitFor() == 0
                ? new String(bytes, StandardCharsets.UTF_8).trim() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static TextColor query(OutputStream output, InputStream input,
                                   String request, int paletteIndex) throws IOException {
        String sequence = "\u001b]" + request + ";?\u001b\\";
        output.write(sequence.getBytes(StandardCharsets.US_ASCII));
        output.flush();

        ByteArrayOutputStream response = new ByteArrayOutputStream();
        long deadline = System.nanoTime() + 350_000_000L;
        while (System.nanoTime() < deadline) {
            if (input.available() > 0) {
                int value = input.read();
                if (value < 0) break;
                response.write(value);
                String text = response.toString(StandardCharsets.US_ASCII);
                if (text.contains("\u001b\\") || text.indexOf('\u0007') >= 0) break;
            } else {
                try { Thread.sleep(5); }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return parseColor(response.toString(StandardCharsets.US_ASCII));
    }

    private static TextColor parseColor(String response) {
        Matcher hex = HEX.matcher(response);
        if (hex.find()) return fromHex(hex.group(1));

        Matcher rgb = RGB.matcher(response);
        if (!rgb.find()) return null;
        return new TextColor.RGB(
            scale(rgb.group(1)), scale(rgb.group(2)), scale(rgb.group(3))
        );
    }

    private static int scale(String component) {
        int value = Integer.parseInt(component, 16);
        int max = (1 << (component.length() * 4)) - 1;
        return (int) Math.round(value * 255.0 / max);
    }

    private static TextColor fromHex(String hex) {
        return new TextColor.RGB(
            Integer.parseInt(hex.substring(0, 2), 16),
            Integer.parseInt(hex.substring(2, 4), 16),
            Integer.parseInt(hex.substring(4, 6), 16)
        );
    }

    public TextColor getForeground() { return foreground; }
    public TextColor getBackground() { return background; }
    public TextColor get(int index) { return colors.get(index); }
    public boolean hasPalette() { return !colors.isEmpty(); }

    /** Selects semantic accents from the terminal's 16 theme-defined ANSI colors. */
    public TextColor selectMuted() {
        return selectByScore(c -> {
            double contrast = contrast(c, background);
            double saturation = saturation(c);
            return contrast >= 2.0 && contrast <= 5.0
                ? Math.abs(contrast - 3.0) + saturation * 0.35 : 100.0;
        }, new TextColor.RGB(128, 128, 128));
    }

    public TextColor selectError() {
        return selectByScore(c -> {
            double[] rgb = rgb(c);
            double contrast = contrast(c, background);
            return contrast < 2.5 || rgb[0] < rgb[1] * 1.25
                || rgb[0] < rgb[2] * 1.15 ? 100.0
                : (rgb[1] + rgb[2]) / 510.0 + Math.abs(contrast - 4.0) * 0.1;
        }, new TextColor.RGB(235, 75, 90));
    }

    public TextColor selectGhost() {
        return selectByScore(c -> {
            double[] rgb = rgb(c);
            double contrast = contrast(c, background);
            boolean cool = rgb[2] > rgb[0] * 1.12 || rgb[1] > rgb[0] * 1.12;
            return !cool || contrast < 2.0 ? 100.0
                : Math.abs(contrast - 3.2) + Math.abs(rgb[1] - rgb[2]) / 510.0;
        }, new TextColor.RGB(100, 150, 165));
    }

    private TextColor selectByScore(java.util.function.ToDoubleFunction<TextColor> score,
                                    TextColor fallback) {
        TextColor best = null;
        double bestScore = Double.MAX_VALUE;
        for (int i = 1; i < 16; i++) {
            TextColor candidate = colors.get(i);
            if (candidate == null) continue;
            double candidateScore = score.applyAsDouble(candidate);
            if (candidateScore < bestScore) {
                best = candidate;
                bestScore = candidateScore;
            }
        }
        return best == null || bestScore >= 100.0 ? fallback : best;
    }

    private static double[] rgb(TextColor color) {
        if (color instanceof TextColor.RGB) {
            TextColor.RGB c = (TextColor.RGB) color;
            return new double[] { c.getRed(), c.getGreen(), c.getBlue() };
        }
        return new double[] { 128, 128, 128 };
    }

    private static double luminance(TextColor color) {
        double[] c = rgb(color);
        for (int i = 0; i < 3; i++) {
            c[i] /= 255.0;
            c[i] = c[i] <= 0.04045 ? c[i] / 12.92
                : Math.pow((c[i] + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
    }

    private static double contrast(TextColor a, TextColor b) {
        if (a == null || b == null) return 1.0;
        double x = luminance(a), y = luminance(b);
        return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
    }

    private static double saturation(TextColor color) {
        double[] c = rgb(color);
        double max = Math.max(c[0], Math.max(c[1], c[2]));
        double min = Math.min(c[0], Math.min(c[1], c[2]));
        return max == 0 ? 0 : (max - min) / max;
    }
}
