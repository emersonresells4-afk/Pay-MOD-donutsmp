package com.example.donorfx;

import net.minecraft.util.Identifier;

/** Holds the on/off state for Cena mode. */
public final class CenaMode {
    public static boolean enabled = false;
    public static final Identifier TEXTURE = Identifier.of("donorfx", "textures/entity/cena.png");

    private CenaMode() {}
}
