package com.example.donorfx;

import com.mojang.brigadier.arguments.LongArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class DonorFxClient implements ClientModInitializer {
    public static final String MOD_ID = "donorfx";

    private static KeyBinding cenaKey;
    private static KeyBinding replayKey;
    private static KeyBinding boardKey;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

        cenaKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.donorfx.cena", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, category));
        replayKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.donorfx.replay", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
        boardKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.donorfx.board", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_L, category));

        // Draw everything on the HUD
        HudElementRegistry.addLast(Identifier.of(MOD_ID, "effects"), Effects::render);

        // Watch chat for "Name paid you $X"
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            Payments.Payment p = Payments.parse(message.getString());
            if (p != null) {
                Payments.record(p);
                Effects.trigger(p.name(), p.amount());
            }
        });

        // Key presses
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (cenaKey.wasPressed()) {
                CenaMode.enabled = !CenaMode.enabled;
                toast(client, CenaMode.enabled ? "Cena mode: ON (you can't see me)" : "Cena mode: OFF");
            }
            while (replayKey.wasPressed()) {
                Effects.replayLast();
            }
            while (boardKey.wasPressed()) {
                Effects.showBoard = !Effects.showBoard;
            }
        });

        // /donorfx test <amount>   and   /donorfx reset
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("donorfx")
                        .then(ClientCommandManager.literal("test")
                                .then(ClientCommandManager.argument("amount", LongArgumentType.longArg(1))
                                        .executes(c -> {
                                            long amount = LongArgumentType.getLong(c, "amount");
                                            Effects.trigger("TestPlayer", amount);
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("reset")
                                .executes(c -> {
                                    Payments.reset();
                                    c.getSource().sendFeedback(Text.literal("Leaderboard cleared."));
                                    return 1;
                                }))));
    }

    private static void toast(MinecraftClient client, String msg) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(msg), true);
        }
    }
}
