package pl.errordustin;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public class ErrorDustinClient implements ClientModInitializer {
    private static KeyBinding webKey, waterKey, guiKey;
    private int prevSlot = -1;
    private int stage = 0;

    @Override
    public void onInitializeClient() {
        webKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.errordustin.web", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.errordustin"));
        waterKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.errordustin.water", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.errordustin"));
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.errordustin.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.errordustin"));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null) return;

        while (guiKey.wasPressed()) mc.setScreen(new ErrorDustinScreen());

        if (stage == 1) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            selectSlot(mc, prevSlot);
            stage = 0;
            return;
        }

        while (webKey.wasPressed()) {
            if (!ErrorDustinConfig.webEnabled) continue;
            int slot = findHotbar(mc, true);
            if (slot == -1) continue;
            int old = mc.player.getInventory().selectedSlot;
            selectSlot(mc, slot);
            if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                        (BlockHitResult) mc.crosshairTarget);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            selectSlot(mc, old);
        }

        while (waterKey.wasPressed()) {
            if (!ErrorDustinConfig.waterEnabled) continue;
            int slot = findHotbar(mc, false);
            if (slot == -1) continue;
            prevSlot = mc.player.getInventory().selectedSlot;
            selectSlot(mc, slot);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            stage = 1;
        }
    }

    private int findHotbar(MinecraftClient mc, boolean web) {
        for (int i = 0; i < 9; i++) {
            var item = mc.player.getInventory().getStack(i).getItem();
            if (web ? item == Items.COBWEB : item == Items.WATER_BUCKET) return i;
        }
        return -1;
    }

    private void selectSlot(MinecraftClient mc, int slot) {
        if (slot < 0) return;
        mc.player.getInventory().selectedSlot = slot;
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }
}
