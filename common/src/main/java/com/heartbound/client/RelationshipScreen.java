package com.heartbound.client;

import com.heartbound.menu.RelationshipMenu;
import com.heartbound.relationship.Gender;
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
 * Drawn with flat rounded rectangles, so it needs no texture files.
 */
public class RelationshipScreen extends AbstractContainerScreen<RelationshipMenu> {

    private static final int WIDTH = 256;
    private static final int HEIGHT = 168;

    private static final int BAR_X = 80;
    private static final int BAR_Y = 62;
    private static final int BAR_W = 166;
    private static final int BAR_H = 8;

    // palette
    private static final int FRAME = 0xFF2E3440;
    private static final int BODY = 0xFFE5E9F0;
    private static final int PORTRAIT_BG = 0xFF1F2430;
    private static final int BAR_TRACK = 0xFF4C566A;
    private static final int BAR_FILL = 0xFFE0243A;
    private static final int BAR_SHINE = 0xFFF2778A;
    private static final int TEXT_MAIN = 0xFF2E3440;
    private static final int TEXT_STAGE = 0xFF2E7D32;
    private static final int MALE_COLOR = 0xFF2F6FDE;
    private static final int FEMALE_COLOR = 0xFFE0559C;

    private static final String[] MALE_ICON = {
            ".....####",
            ".......##",
            "......#.#",
            ".....#..#",
            ".####....",
            "#...#....",
            "#...#....",
            "#...#....",
            ".###....."
    };
    private static final String[] FEMALE_ICON = {
            "...###...",
            "..#...#..",
            "..#...#..",
            "..#...#..",
            "...###...",
            "....#....",
            "...###...",
            "....#....",
            "....#...."
    };

    private static final int[] MARKERS = {
            RelationshipStage.ACQUAINTED.threshold(),
            RelationshipStage.FRIENDS.threshold(),
            RelationshipStage.CLOSE.threshold(),
            RelationshipStage.PARTNERS.threshold()
    };

    private Button followButton;

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
        addRenderableWidget(new RoundedButton(x + 10, y + 116, 76, 20,
                Component.translatable("button.heartbound.gift"), button -> press(RelationshipMenu.BUTTON_GIFT)));
        addRenderableWidget(new RoundedButton(x + 91, y + 116, 76, 20,
                Component.translatable("button.heartbound.pet"), button -> press(RelationshipMenu.BUTTON_PET)));
        followButton = new RoundedButton(x + 172, y + 116, 76, 20,
                Component.translatable("button.heartbound.follow"), button -> press(RelationshipMenu.BUTTON_FOLLOW));
        addRenderableWidget(followButton);
        addLocked(Component.translatable("button.heartbound.home"), x + 10, y + 140);
        addLocked(Component.translatable("button.heartbound.propose"), x + 91, y + 140);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (followButton != null) {
            followButton.setMessage(Component.translatable(
                    menu.isFollowing() ? "button.heartbound.follow_stop" : "button.heartbound.follow"));
            followButton.active = menu.isFollowing()
                    || menu.getAffinity() >= RelationshipStage.FRIENDS.threshold();
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void addLocked(Component label, int x, int y) {
        Button button = new RoundedButton(x, y, 76, 20, label, b -> { });
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

        // panel
        UiDraw.roundedFill(g, x, y, x + WIDTH, y + HEIGHT, 7, FRAME);
        UiDraw.roundedFill(g, x + 2, y + 2, x + WIDTH - 2, y + HEIGHT - 2, 5, BODY);

        // portrait
        UiDraw.roundedBox(g, x + 10, y + 12, x + 70, y + 104, 4, FRAME, PORTRAIT_BG);
        if (findTarget() instanceof LivingEntity living) {
            float size = Math.max(living.getBbHeight(), living.getBbWidth());
            int scale = (int) Math.max(12, Math.min(70, 56 / Math.max(0.3F, size)));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 11, y + 13, x + 69, y + 103,
                    scale, 0.0625F, (float) mouseX, (float) mouseY, living);
        }

        // affinity bar
        int affinity = Math.max(0, Math.min(RelationshipStage.MAX_AFFINITY, menu.getAffinity()));
        int bx = x + BAR_X;
        int by = y + BAR_Y;
        UiDraw.roundedFill(g, bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, 5, FRAME);
        UiDraw.roundedFill(g, bx, by, bx + BAR_W, by + BAR_H, 4, BAR_TRACK);
        int filled = BAR_W * affinity / RelationshipStage.MAX_AFFINITY;
        if (filled > 0) {
            int width = Math.max(filled, 4);
            UiDraw.roundedFill(g, bx, by, bx + width, by + BAR_H, 4, BAR_FILL);
            if (width > 8) {
                g.fill(bx + 3, by + 1, bx + width - 3, by + 2, BAR_SHINE);
            }
        }
        for (int marker : MARKERS) {
            int mx = bx + BAR_W * marker / RelationshipStage.MAX_AFFINITY;
            g.fill(mx, by - 1, mx + 1, by + BAR_H + 1, FRAME);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        Entity target = findTarget();
        Component name = target != null ? target.getName() : title;
        int affinity = Math.max(0, Math.min(RelationshipStage.MAX_AFFINITY, menu.getAffinity()));
        RelationshipStage stage = RelationshipStage.forAffinity(affinity);

        g.drawString(font, name, 80, 14, TEXT_MAIN, false);

        Gender gender = menu.getGender();
        boolean male = gender == Gender.MALE;
        int genderColor = male ? MALE_COLOR : FEMALE_COLOR;
        UiDraw.pixels(g, 80, 25, male ? MALE_ICON : FEMALE_ICON, genderColor);
        g.drawString(font, Component.translatable("gender.heartbound." + gender.key()), 93, 26, genderColor, false);

        g.drawString(font, Component.translatable("stage.heartbound." + stage.name().toLowerCase(Locale.ROOT)),
                80, 38, TEXT_STAGE, false);
        g.drawString(font, Component.literal(affinity + " / " + RelationshipStage.MAX_AFFINITY), 80, 50, TEXT_MAIN, false);
    }
}
