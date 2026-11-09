package com.example.donorfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * All the payment animations. Everything is drawn with plain rectangles and text,
 * so it needs no textures and works the same on any resource pack.
 */
public final class Effects {
    private static final Random R = new Random();

    private static final String[] CAPTIONS = {
            "WE ARE SO BACK", "HE IS HIM", "MONEY MOVES", "CHAT HE PAID", "W DONOR",
            "THE GOAT", "SHEEEESH", "RICH BEHAVIOR", "CALL THE BANK", "NO WAY", "ABSOLUTE CINEMA"
    };

    private static final int[] TIER_COLORS = {
            0x55FF55, 0xFFD700, 0xFFAA00, 0xFF55FF, 0xFF5555, 0x55FFFF
    };

    private static final long[] DURATIONS = {3000, 4000, 5000, 7000, 9000, 11000};

    private static final class Fx {
        final String name;
        final long amount;
        final int tier;
        final long start;
        final long duration;
        long lastBurst;

        Fx(String name, long amount, int tier) {
            this.name = name;
            this.amount = amount;
            this.tier = tier;
            this.start = System.currentTimeMillis();
            this.duration = DURATIONS[tier];
            this.lastBurst = this.start;
        }
    }

    private static final class Particle {
        float x, y, vx, vy, size;
        int rgb;
        long born, life;
    }

    private static final List<Fx> active = new ArrayList<>();
    private static final List<Particle> particles = new ArrayList<>();
    private static long lastFrame = 0;
    private static String lastName = null;
    private static long lastAmount = 0;

    public static boolean showBoard = false;

    private Effects() {}

    public static int tierFor(long amount) {
        if (amount >= 1_000_000_000L) return 5;
        if (amount >= 100_000_000L) return 4;
        if (amount >= 10_000_000L) return 3;
        if (amount >= 1_000_000L) return 2;
        if (amount >= 100_000L) return 1;
        return 0;
    }

    public static void replayLast() {
        if (lastName != null) trigger(lastName, lastAmount);
    }

    public static void trigger(String name, long amount) {
        lastName = name;
        lastAmount = amount;
        int tier = tierFor(amount);
        active.add(new Fx(name, amount, tier));

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
            if (tier >= 2) {
                mc.player.playSound(SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 1.0F);
            }
            if (tier >= 4) {
                mc.player.playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.0F);
            }
        }

        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        if (tier >= 1) burst(w / 2f, h / 2f, 20 + tier * 25, tier);
    }

    // ---------------------------------------------------------------- rendering

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();

        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;

        active.removeIf(f -> now - f.start > f.duration);

        if (showBoard) drawBoard(ctx, tr, w);

        for (Fx f : new ArrayList<>(active)) {
            drawFx(ctx, tr, f, now, w, h);
        }

        updateAndDrawParticles(ctx, now, dt, w, h);
    }

    private static void drawFx(DrawContext ctx, TextRenderer tr, Fx f, long now, int w, int h) {
        float t = (now - f.start) / (float) f.duration; // 0..1
        float ms = now - f.start;
        int tier = f.tier;
        int base = TIER_COLORS[tier];
        float fadeIn = Math.min(1f, ms / 300f);
        float fadeOut = Math.min(1f, (f.duration - ms) / 600f);
        float vis = Math.max(0f, Math.min(fadeIn, fadeOut));

        // Continuous particle rain
        if (tier >= 1 && R.nextFloat() < 0.35f + tier * 0.12f) {
            spawnCoin(w);
            if (tier >= 3) { spawnCoin(w); spawnCoin(w); }
        }
        // Repeated firework bursts for the big tiers
        if (tier >= 3 && now - f.lastBurst > (tier == 5 ? 350 : 700)) {
            f.lastBurst = now;
            burst(40 + R.nextInt(Math.max(1, w - 80)), 30 + R.nextInt(Math.max(1, h - 80)), 30 + tier * 10, tier);
        }

        var m = ctx.getMatrices();
        m.pushMatrix();

        // Screen shake on tier 2+
        if (tier >= 2 && t < 0.85f) {
            float power = (tier - 1) * 1.2f * vis;
            m.translate((R.nextFloat() - 0.5f) * power, (R.nextFloat() - 0.5f) * power);
        }

        // Full-screen white flash right at the start
        if (tier >= 2 && ms < 450) {
            int a = (int) (180 * (1f - ms / 450f));
            ctx.fill(0, 0, w, h, (a << 24) | 0xFFFFFF);
        }

        // Rainbow tint for 10M+
        if (tier >= 3) {
            int a = (int) ((40 + 25 * Math.sin(ms / 150.0)) * vis);
            ctx.fill(0, 0, w, h, (a << 24) | rainbow(ms / 400f));
        }

        // Strobe on 1B+
        if (tier >= 5 && ((int) (ms / 120)) % 2 == 0) {
            ctx.fill(0, 0, w, h, ((int) (60 * vis) << 24) | rainbow(ms / 200f + 2f));
        }

        // Cinema bars for 100M+
        if (tier >= 4) {
            int bar = (int) (h * 0.12f * vis);
            ctx.fill(0, 0, w, bar, 0xFF000000);
            ctx.fill(0, h - bar, w, h, 0xFF000000);
        }

        // Scrolling meme captions for 100M+
        if (tier >= 4) {
            int rows = tier == 5 ? 6 : 3;
            for (int i = 0; i < rows; i++) {
                String cap = CAPTIONS[(i * 3 + (int) (ms / 1500)) % CAPTIONS.length];
                float speed = 140f + i * 35f;
                float x = ((ms / 1000f) * speed * (i % 2 == 0 ? 1 : -1)) % (w + 300);
                if (x < 0) x += w + 300;
                float cx = x - 150;
                float cy = h * 0.2f + i * (h * 0.11f);
                bigText(ctx, tr, cap, cx, cy, 2.5f, 0xFF000000 | rainbow(ms / 300f + i));
            }
        }

        // Headline for 10M+
        if (tier >= 3) {
            String head = switch (tier) {
                case 3 -> "BIG PAYMENT";
                case 4 -> "LEGENDARY PAYMENT";
                default -> "THE ECONOMY IS BROKEN";
            };
            float pulse = 1f + 0.12f * (float) Math.sin(ms / 90.0);
            float scale = (tier == 3 ? 4.5f : tier == 4 ? 5f : 4f) * pulse * vis;
            if (scale > 0.05f) {
                bigText(ctx, tr, head, w / 2f + 2, h * 0.30f + 2, scale, 0xFF000000);
                bigText(ctx, tr, head, w / 2f, h * 0.30f, scale, 0xFF000000 | rainbow(ms / 200f));
            }
        }


        // ---- EXTRA FX (v2) ----
        // Spinning ring of sparks for 1M+
        if (tier >= 2) {
            float cx = w / 2f, cy = h * 0.5f;
            int dots = 16 + tier * 4;
            float radius = (40 + tier * 18) * (0.6f + 0.4f * vis);
            for (int i = 0; i < dots; i++) {
                double a = (ms / (900.0 - tier * 100.0)) + i * (Math.PI * 2 / dots);
                int dx = (int) (cx + Math.cos(a) * radius);
                int dy = (int) (cy + Math.sin(a) * radius * 0.6f);
                int col = ((int) (220 * vis) << 24) | rainbow(i * 0.4f + ms / 200f);
                ctx.fill(dx, dy, dx + 3, dy + 3, col);
            }
        }
        // Sweeping light beams for 10M+
        if (tier >= 3) {
            int beams = tier;
            for (int i = 0; i < beams; i++) {
                float bx = ((ms / 6f) + i * (w / (float) beams)) % w;
                int bwid = 6 + tier * 2;
                int col = ((int) (70 * vis) << 24) | rainbow(i + ms / 300f);
                ctx.fill((int) bx, 0, (int) bx + bwid, h, col);
            }
        }
        // Lightning bolts for 100M+
        if (tier >= 4 && R.nextFloat() < 0.18f) {
            int x = R.nextInt(Math.max(1, w));
            int y = 0;
            int col = 0xFFFFFFFF;
            while (y < h) {
                int seg = 8 + R.nextInt(14);
                int nx = x + R.nextInt(25) - 12;
                ctx.fill(Math.min(x, nx), y, Math.max(x, nx) + 2, y + seg, col);
                x = nx;
                y += seg;
            }
            ctx.fill(0, 0, w, h, 0x30FFFFFF);
        }
        // Crazy zoom-pulse border for 1B
        if (tier >= 5) {
            int th = (int) (10 + 8 * Math.sin(ms / 70.0));
            int col = 0xFF000000 | rainbow(ms / 100f);
            ctx.fill(0, 0, w, th, col);
            ctx.fill(0, h - th, w, h, col);
            ctx.fill(0, 0, th, h, col);
            ctx.fill(w - th, 0, w, h, col);
        }
        // ---- END EXTRA FX ----

        // Banner with amount (every tier)
        drawBanner(ctx, tr, f, t, ms, w, h, vis, base);

        m.popMatrix();
    }

    private static void drawBanner(DrawContext ctx, TextRenderer tr, Fx f, float t, float ms, int w, int h, float vis, int base) {
        int tier = f.tier;
        float slide = Math.min(1f, ms / 350f);
        slide = 1f - (1f - slide) * (1f - slide);
        float out = Math.min(1f, (f.duration - ms) / 400f);
        float shown = Math.max(0f, Math.min(slide, out));

        int bw = Math.min(w - 20, 220 + tier * 20);
        int bh = 34 + tier * 4;
        int bx = (w - bw) / 2;
        float yOffset = tier >= 3 ? h * 0.52f : 8f;
        int by = (int) (yOffset - (1f - shown) * (bh + 20));

        ctx.fill(bx - 2, by - 2, bx + bw + 2, by + bh + 2, 0xFF000000 | (tier >= 2 ? rainbow(ms / 250f) : base));
        ctx.fill(bx, by, bx + bw, by + bh, 0xE0101010);

        String amount = "+$" + Payments.format(f.amount);
        float pulse = 1f + (tier >= 2 ? 0.08f * (float) Math.sin(ms / 100.0) : 0f);
        bigText(ctx, tr, amount, w / 2f, by + bh * 0.42f, (1.8f + tier * 0.25f) * pulse, 0xFF000000 | base);
        String from = "from " + f.name;
        ctx.drawText(tr, from, (w - tr.getWidth(from)) / 2, by + bh - 11, 0xFFCCCCCC, true);
    }

    private static void drawBoard(DrawContext ctx, TextRenderer tr, int w) {
        List<Map.Entry<String, Long>> top = Payments.top(5);
        int x = w - 118;
        int y = 40;
        int rowsShown = Math.max(1, top.size());
        ctx.fill(x - 4, y - 4, w - 4, y + 14 + rowsShown * 11, 0x90000000);
        ctx.drawText(tr, "TOP DONORS", x, y, 0xFFFFD700, true);
        if (top.isEmpty()) {
            ctx.drawText(tr, "No donors yet", x, y + 15, 0xFFAAAAAA, true);
            return;
        }
        int i = 0;
        for (Map.Entry<String, Long> e : top) {
            i++;
            int color = i == 1 ? 0xFFFFD700 : i == 2 ? 0xFFC0C0C0 : i == 3 ? 0xFFCD7F32 : 0xFFFFFFFF;
            String line = i + ". " + e.getKey();
            ctx.drawText(tr, line, x, y + 4 + i * 11, color, true);
            String amt = "$" + Payments.format(e.getValue());
            ctx.drawText(tr, amt, w - 8 - tr.getWidth(amt), y + 4 + i * 11, color, true);
        }
    }

    // ---------------------------------------------------------------- particles

    private static void spawnCoin(int w) {
        Particle p = new Particle();
        p.x = R.nextFloat() * w;
        p.y = -6;
        p.vx = (R.nextFloat() - 0.5f) * 30f;
        p.vy = 90f + R.nextFloat() * 140f;
        p.size = 3 + R.nextInt(4);
        p.rgb = R.nextBoolean() ? 0xFFD700 : 0xFFAA00;
        p.born = System.currentTimeMillis();
        p.life = 4000;
        particles.add(p);
    }

    private static void burst(float cx, float cy, int count, int tier) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            double ang = R.nextDouble() * Math.PI * 2;
            float speed = 60f + R.nextFloat() * (120f + tier * 25f);
            Particle p = new Particle();
            p.x = cx;
            p.y = cy;
            p.vx = (float) Math.cos(ang) * speed;
            p.vy = (float) Math.sin(ang) * speed;
            p.size = 2 + R.nextInt(3);
            p.rgb = rainbow(R.nextFloat() * 6.28f);
            p.born = now;
            p.life = 900 + R.nextInt(900);
            particles.add(p);
        }
    }

    private static void updateAndDrawParticles(DrawContext ctx, long now, float dt, int w, int h) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            long age = now - p.born;
            if (age > p.life || p.y > h + 10) {
                particles.remove(i);
                continue;
            }
            p.vy += 120f * dt; // gravity
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            int a = (int) (255 * Math.max(0f, 1f - age / (float) p.life));
            int s = (int) p.size;
            ctx.fill((int) p.x, (int) p.y, (int) p.x + s, (int) p.y + s, (a << 24) | p.rgb);
        }
        if (particles.size() > 1500) particles.subList(0, particles.size() - 1500).clear();
    }

    // ---------------------------------------------------------------- helpers

    private static void bigText(DrawContext c, TextRenderer tr, String s, float cx, float cy, float scale, int color) {
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(cx, cy);
        m.scale(scale, scale);
        c.drawText(tr, s, -tr.getWidth(s) / 2, -tr.fontHeight / 2, color, true);
        m.popMatrix();
    }

    /** Smoothly cycling RGB (no alpha). */
    private static int rainbow(float phase) {
        int r = (int) (127 + 127 * Math.sin(phase));
        int g = (int) (127 + 127 * Math.sin(phase + 2.094f));
        int b = (int) (127 + 127 * Math.sin(phase + 4.188f));
        return (r << 16) | (g << 8) | b;
    }
}
