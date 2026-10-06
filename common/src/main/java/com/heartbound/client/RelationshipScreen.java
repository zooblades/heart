package com.heartbound.client;

import com.heartbound.menu.RelationshipMenu;
import com.heartbound.relationship.RelationshipStage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * The relationship window: portrait, name, gender, stage, affinity bar with stage markers and buttons.
 * Drawn with flat rectangles, so it needs no texture files.
 */
public class RelationshipScreen extends AbstractContainerScreen<RelationshipMenu> {

    private static final int WIDTH = 256;
    private static final int HEIGHT = 168;

    private static final int BAR_X = 80;
    private static final int BAR_Y = 62;
    private static final int BAR_W = 166;
    private static final int BAR_H = 8;

    private static final int[] MARKERS = {
            RelationshipStage.ACQUAINTED.threshold(),
            RelationshipStage.FRIENDS.threshold(),
            RelationshipStage.CLOSE.threshold(),
            RelationshipStage.PARTNERS.threshold()
    };

    public RelationshipScreen(RelationshipMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;
        addRenderableWidget(Button.builder(Component.translatable("button.heartbound.gift"),
                button -> press(RelationshipMenu.BUTTON_GIFT)).bounds(x + 10, y + 116, 76, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("button.heartbound.pet"),
                button -> press(RelationshipMenu.BUTTON_PET)).bounds(x + 91, y + 116, 76, 20).build());
        addLocked(Component.translatable("button.heartbound.follow"), x + 172, y + 116);
        addLocked(Component.translatable("button.heartbound.home"), x + 10, y + 140);
        addLocked(Component.translatable("button.heartbound.propose"), x + 91, y + 140);
    }

    private void addLocked(Component label, int x, int y) {
        Button button = Button.builder(label, b -> { }).bounds(x, y, 76, 20).build();
        button.active = false;
        addRenderableWidget(button);
    }

    private void press(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    private Entity findTarget() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        return minecraft.level.getEntity(menu.getTargetId());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        g.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF373737);
        g.fill(x + 2, y + 2, x + WIDTH - 2, y + HEIGHT - 2, 0xFFC6C6C6);

        g.fill(x + 10, y + 12, x + 70, y + 104, 0xFF1E1E1E);
        if (findTarget() instanceof LivingEntity living) {
            float size = Math.max(living.getBbHeight(), living.getBbWidth());
            int scale = (int) Math.max(12, Math.min(70, 56 / Math.max(0.3F, size)));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 10, y + 12, x + 70, y + 104,
                    scale, 0.0625F, (float) mouseX, (float) mouseY, living);
        }

        int affinity = Math.max(0, Math.min(RelationshipStage.MAX_AFFINITY, menu.getAffinity()));
        int bx = x + BAR_X;
        int by = y + BAR_Y;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, 0xFF373737);
        g.fill(bx, by, bx + BAR_W, by + BAR_H, 0xFF555555);
        int filled = BAR_W * affinity / RelationshipStage.MAX_AFFINITY;
        g.fill(bx, by, bx + filled, by + BAR_H, 0xFFE0243A);
        for (int marker : MARKERS) {
            int mx = bx + BAR_W * marker / RelationshipStage.MAX_AFFINITY;
            g.fill(mx, by - 2, mx + 1, by + BAR_H + 2, 0xFF000000);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        Entity target = findTarget();
        Component name = target != null ? target.getName() : title;
        int affinity = Math.max(0, Math.min(RelationshipStage.MAX_AFFINITY, menu.getAffinity()));
        RelationshipStage stage = RelationshipStage.forAffinity(affinity);

        g.drawString(font, name, 80, 14, 0x404040, false);
        g.drawString(font, Component.translatable("gender.heartbound." + menu.getGender().key()), 80, 26, 0x606060, false);
        g.drawString(font, Component.translatable("stage.heartbound." + stage.name().toLowerCase(Locale.ROOT)), 80, 38, 0x2E7D32, false);
        g.drawString(font, Component.literal(affinity + " / " + RelationshipStage.MAX_AFFINITY), 80, 50, 0x404040, false);
    }
}
