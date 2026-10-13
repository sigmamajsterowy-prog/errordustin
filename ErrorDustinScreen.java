package pl.errordustin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class ErrorDustinScreen extends Screen {
    private static final int PW = 300, PH = 210;
    private final ItemStack web = new ItemStack(Items.COBWEB);
    private long openTime;
    private int px, py;

    private KeyBinding listening = null;
    private GreenButton webToggle, waterToggle, leverToggle, webBind, waterBind, guiBind;

    public ErrorDustinScreen() {
        super(Text.literal("errordustin"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        px = (width - PW) / 2;
        py = (height - PH) / 2;

        webToggle = addDrawableChild(new GreenButton(px + 165, py + 54, 64, 22, Text.empty(), b -> {
            ErrorDustinConfig.webEnabled = !ErrorDustinConfig.webEnabled;
            ErrorDustinConfig.save();
            refresh();
        }));
        webBind = addDrawableChild(new GreenButton(px + 233, py + 54, 52, 22, Text.empty(),
                b -> startListening(ErrorDustinClient.webKey)));

        waterToggle = addDrawableChild(new GreenButton(px + 165, py + 80, 64, 22, Text.empty(), b -> {
            ErrorDustinConfig.waterEnabled = !ErrorDustinConfig.waterEnabled;
            ErrorDustinConfig.save();
            refresh();
        }));
        waterBind = addDrawableChild(new GreenButton(px + 233, py + 80, 52, 22, Text.empty(),
                b -> startListening(ErrorDustinClient.waterKey)));

        leverToggle = addDrawableChild(new GreenButton(px + 165, py + 106, 120, 22, Text.empty(), b -> {
            ErrorDustinConfig.leverEnabled = !ErrorDustinConfig.leverEnabled;
            ErrorDustinConfig.save();
            refresh();
        }));

        guiBind = addDrawableChild(new GreenButton(px + 165, py + 132, 120, 22, Text.empty(),
                b -> startListening(ErrorDustinClient.guiKey)));

        addDrawableChild(new GreenButton(px + 165, py + PH - 32, 120, 22, Text.literal("Zamknij"), b -> close()));
        refresh();
    }

    private void startListening(KeyBinding key) {
        listening = key;
        refresh();
    }

    private String keyName(KeyBinding key, int maxWidth) {
        if (listening == key) return "...";
        return textRenderer.trimToWidth(key.getBoundKeyLocalizedText().getString(), maxWidth);
    }

    private void refresh() {
        webToggle.setMessage(Text.literal("Web: " + (ErrorDustinConfig.webEnabled ? "ON" : "OFF")));
        waterToggle.setMessage(Text.literal("Woda: " + (ErrorDustinConfig.waterEnabled ? "ON" : "OFF")));
        leverToggle.setMessage(Text.literal("Dzwignia: " + (ErrorDustinConfig.leverEnabled ? "ON" : "OFF")));
        webBind.setMessage(Text.literal(keyName(ErrorDustinClient.webKey, 44)));
        waterBind.setMessage(Text.literal(keyName(ErrorDustinClient.waterKey, 44)));
        guiBind.setMessage(Text.literal("GUI: " + keyName(ErrorDustinClient.guiKey, 80)));
    }

    private void applyKey(InputUtil.Key newKey) {
        listening.setBoundKey(newKey);
        KeyBinding.updateKeysByCode();
        MinecraftClient.getInstance().options.write();
        listening = null;
        refresh();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listening = null;
                refresh();
            } else {
                applyKey(InputUtil.fromKeyCode(keyCode, scanCode));
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listening != null) {
            applyKey(InputUtil.Type.MOUSE.createFromCode(button));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private float time() {
        return (System.currentTimeMillis() - openTime) / 1000f;
    }

    private float intro() {
        float t = MathHelper.clamp((System.currentTimeMillis() - openTime) / 450f, 0f, 1f);
        return 1f - (float) Math.pow(1f - t, 3);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float a = intro();
        ctx.fill(0, 0, width, height, ((int) (150 * a) << 24));
        ctx.fillGradient(px, py, px + PW, py + PH, 0xF00B1F10, 0xF0143D1E);
        ctx.drawBorder(px, py, PW, PH, 0xFF2ECC55);
        ctx.drawBorder(px + 2, py + 2, PW - 4, PH - 4, 0x552ECC55);
        ctx.fillGradient(px + 10, py + 30, px + 150, py + PH - 12, 0x80102A16, 0x80195A2A);
        ctx.drawBorder(px + 10, py + 30, 140, PH - 42, 0xFF1F9B42);

        float t = time();
        for (int i = 0; i < 22; i++) {
            float speed = 12 + (i % 5) * 6;
            float yy = py + PH - 14 - ((t * speed + i * 17) % (PH - 30));
            float xx = px + 14 + ((i * 37) % 132) + MathHelper.sin(t * 1.5f + i) * 4;
            int alpha = (int) (160 * (1f - (py + PH - yy) / (float) PH));
            ctx.fill((int) xx, (int) yy, (int) xx + 2, (int) yy + 2,
                    (MathHelper.clamp(alpha, 20, 200) << 24) | 0x7DFF9B);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        float t = time();
        float a = intro();

        int g = 180 + (int) (75 * (0.5f + 0.5f * MathHelper.sin(t * 3f)));
        int color = 0xFF000000 | (0x30 << 16) | (g << 8) | 0x60;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(px + PW / 2f, py + 10, 0);
        ctx.getMatrices().scale(1.6f, 1.6f, 1f);
        ctx.drawCenteredTextWithShadow(textRenderer, "errordustin", 0, 0, color);
        ctx.getMatrices().pop();
        ctx.drawCenteredTextWithShadow(textRenderer, "v1.0 | 1.21.4", px + 225, py + 30, 0xFF7DFF9B);
        ctx.drawTextWithShadow(textRenderer, "Funkcja / klawisz", px + 165, py + 42, 0xFFB6FFC9);

        if (listening != null) {
            ctx.drawCenteredTextWithShadow(textRenderer, "Nacisnij klawisz (Esc = anuluj)",
                    px + 225, py + 162, 0xFFFFE066);
        }

        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p != null) {
            var inv = p.getInventory();
            int slot = inv.selectedSlot;
            ItemStack old = inv.getStack(slot);
            inv.setStack(slot, web.copy());
            try {
                float cx = px + 80, cy = py + 110;
                float fakeX = cx + MathHelper.sin(t * 1.2f) * 70f;
                float fakeY = cy - 45 + MathHelper.cos(t * 0.8f) * 12f;
                int size = (int) (52 * a);
                if (size > 2) {
                    InventoryScreen.drawEntity(ctx, px + 10, py + 30, px + 150, py + PH - 12,
                            size, 0.0625f, fakeX, fakeY, p);
                }
            } finally {
                inv.setStack(slot, old);
            }
        }

        float cx = px + 80, cy = py + 100;
        float radius = 62 * a;
        for (int i = 0; i < 3; i++) {
            double ang = t * 1.4 + i * (Math.PI * 2 / 3);
            float ix = cx + (float) Math.cos(ang) * radius - 8;
            float iy = cy + (float) Math.sin(ang) * radius * 0.35f - 8 + MathHelper.sin(t * 3 + i) * 3;
            ctx.drawItem(web, (int) ix, (int) iy);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
