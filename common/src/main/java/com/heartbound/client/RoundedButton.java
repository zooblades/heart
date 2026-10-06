package com.heartbound.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** A flat button with rounded corners. Inactive buttons are drawn greyed out. */
public class RoundedButton extends Button {

    public RoundedButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int background;
        int border;
        int text;
        if (!active) {
            background = 0xFFB8C0CC;
            border = 0xFF8F99A8;
            text = 0xFF7B8594;
        } else if (isHoveredOrFocused()) {
            background = 0xFF81A1C1;
            border = 0xFF2E3440;
            text = 0xFFFFFFFF;
        } else {
            background = 0xFF5E81AC;
            border = 0xFF2E3440;
            text = 0xFFFFFFFF;
        }
        UiDraw.roundedBox(g, getX(), getY(), getX() + getWidth(), getY() + getHeight(), 3, border, background);
        g.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, text);
    }
}
