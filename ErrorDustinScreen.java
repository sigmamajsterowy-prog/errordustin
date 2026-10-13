package pl.errordustin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class ErrorDustinScreen extends Screen {
    private static final int PW = 300, PH = 190;
    private final ItemStack web = new ItemStack(Items.COBWEB);
    private long openTime;
    private int px, py;

    public ErrorDustinScreen() {
        super(Text.literal("errordustin"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        px = (width - PW) / 2;
        py = (height - PH) / 2;

        addDrawableChild(new GreenButton(px + 165, py + 62, 120, 22, label("Pajęczyna", ErrorDustinConfig.webEnabled), b -> {
            ErrorDustinConfig.webEnabled = !ErrorDustinConfig.webEnabled;
            b.setMessage(label("Pajęczyna", ErrorDustinConfig.webEnabled));
        }));
        addDrawableChild(new GreenButton(px + 165, py + 92, 120, 22, label("Woda", ErrorDustinConfig.waterEnabled), b -> {
            ErrorDustinConfig.waterEnabled = !ErrorDustinConfig.waterEnabled;
            b.setMessage(label("Woda", ErrorDustinConfig.waterEnabled));
        }));
        addDrawableChild(new GreenButton(px + 165, py + PH - 36, 120, 22, Text.literal("Zamknij"), b -> close()));
    }

    private Text label(String name, boolean on) {
        return Text.literal(name + ": " + (on ? "ON" : "OFF"));
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
        ctx.drawCenteredTextWithShadow(textRenderer, "v1.0 | 1.21.4", px + 225, py + 42, 0xFF7DFF9B);

        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p != null) {
            var inv = p.getInventory();
            int slot = inv.selectedSlot;
            ItemStack old = inv.getStack(slot);
            inv.setStack(slot, web.copy());
            try {
                float cx = px + 80, cy = py + 100;
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

        float cx = px + 80, cy = py + 92;
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
