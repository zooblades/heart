package com.heartbound.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** A flat button with rounded corners, a soft gradient, a light edge and a drop shadow. */
public class RoundedButton extends Button {

    public RoundedButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int textColor;

        if (!active) {
            UiDraw.roundedBox(g, x, y, x + w, y + h, 3, 0xFF8F99A8, 0xFFB8C0CC);
            textColor = 0xFF7B8594;
        } else {
            boolean hover = isHoveredOrFocused();
            int top = hover ? 0xFF93B3D6 : 0xFF6F93BF;
            int bottom = hover ? 0xFF6189B6 : 0xFF4B6D99;
            UiDraw.roundedFill(g, x, y + 1, x + w, y + h + 1, 3, 0x55000000);
            UiDraw.roundedFill(g, x, y, x + w, y + h, 3, 0xFF2E3440);
            UiDraw.roundedGradient(g, x + 1, y + 1, x + w - 1, y + h - 1, 2, top, bottom);
            g.fill(x + 3, y + 1, x + w - 3, y + 2, 0x66FFFFFF);
            textColor = 0xFFFFFFFF;
        }
        g.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                x + w / 2, y + (h - 8) / 2, textColor);
    }
}
