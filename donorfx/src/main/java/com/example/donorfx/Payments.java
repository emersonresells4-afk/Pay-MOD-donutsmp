package com.example.donorfx;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses "Name paid you $X" chat lines and keeps a saved leaderboard. */
public final class Payments {
    // Must match the WHOLE line, so a player typing "Name: Bob paid you $1B" in chat can't fake it.
    private static final Pattern PAID_YOU =
            Pattern.compile("^\\W*(\\w{1,16}) paid you \\$([\\d,]*\\.?\\d+)\\s*([kKmMbBtT]?)\\W*$");

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("donorfx-leaderboard.json");
    private static final Gson GSON = new Gson();
    private static Map<String, Long> totals = new HashMap<>();

    public record Payment(String name, long amount) {}

    static {
        load();
    }

    private Payments() {}

    public static Payment parse(String line) {
        Matcher m = PAID_YOU.matcher(line.trim());
        if (!m.matches()) return null;
        try {
            double base = Double.parseDouble(m.group(2).replace(",", ""));
            double mult = switch (m.group(3).toLowerCase()) {
                case "k" -> 1_000d;
                case "m" -> 1_000_000d;
                case "b" -> 1_000_000_000d;
                case "t" -> 1_000_000_000_000d;
                default -> 1d;
            };
            long amount = (long) (base * mult);
            if (amount <= 0) return null;
            return new Payment(m.group(1), amount);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void record(Payment p) {
        totals.merge(p.name(), p.amount(), Long::sum);
        save();
    }

    public static void reset() {
        totals.clear();
        save();
    }

    /** Top donors, biggest first. */
    public static List<Map.Entry<String, Long>> top(int n) {
        List<Map.Entry<String, Long>> list = new ArrayList<>(totals.entrySet());
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return list.subList(0, Math.min(n, list.size()));
    }

    public static String format(long v) {
        if (v >= 1_000_000_000_000L) return trim(v / 1_000_000_000_000d) + "T";
        if (v >= 1_000_000_000L) return trim(v / 1_000_000_000d) + "B";
        if (v >= 1_000_000L) return trim(v / 1_000_000d) + "M";
        if (v >= 1_000L) return trim(v / 1_000d) + "K";
        return Long.toString(v);
    }

    private static String trim(double d) {
        String s = String.format("%.2f", d);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    private static void load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE)) {
                    Map<String, Long> loaded = GSON.fromJson(r, new TypeToken<Map<String, Long>>() {}.getType());
                    if (loaded != null) totals = new HashMap<>(loaded);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void save() {
        try (Writer w = Files.newBufferedWriter(FILE)) {
            GSON.toJson(totals, w);
        } catch (Exception ignored) {
        }
    }
}
