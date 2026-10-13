package pl.errordustin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class GreenButton extends ButtonWidget {
    public GreenButton(int x, int y, int w, int h, Text msg, PressAction onPress) {
        super(x, y, w, h, msg, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean hover = isHovered();
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        ctx.fillGradient(x, y, x + w, y + h,
                hover ? 0xFF3FE06A : 0xFF1F9B42,
                hover ? 0xFF1F9B42 : 0xFF0F5A24);
        ctx.drawBorder(x, y, w, h, hover ? 0xFFB6FFC9 : 0xFF2ECC55);
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawCenteredTextWithShadow(tr, getMessage(), x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
    }
}
