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
 * Drawn with flat rounded shapes, gradients, shadows and highlights, so it needs no texture files.
 */
public class RelationshipScreen extends AbstractContainerScreen<RelationshipMenu> {

    private static final int WIDTH = 256;
    private static final int HEIGHT = 184;

    private static final int BAR_X = 80;
    private static final int BAR_Y = 62;
    private static final int BAR_W = 166;
    private static final int BAR_H = 8;

    // palette
    private static final int FRAME = 0xFF2E3440;
    private static final int FRAME_PARTNER = 0xFF7A2250;
    private static final int TEXT_MAIN = 0xFF2E3440;
    private static final int TEXT_STAGE = 0xFF2E7D32;
    private static final int TEXT_PARTNER = 0xFFC2185B;
    private static final int TEXT_HINT = 0xFF6B7587;
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
    private Button proposeButton;
    private Button homeButton;

    /** The bar value currently drawn; it glides towards the real affinity. */
    private float shownAffinity = -1F;

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
        followButton = new RoundedButton(x + 91, y + 116, 76, 20,
                Component.translatable("button.heartbound.follow"), button -> press(RelationshipMenu.BUTTON_FOLLOW));
        addRenderableWidget(followButton);
        homeButton = new RoundedButton(x + 172, y + 116, 76, 20,
                Component.translatable("button.heartbound.home"), button -> press(RelationshipMenu.BUTTON_HOME));
        addRenderableWidget(homeButton);
        proposeButton = new RoundedButton(x + 10, y + 142, 76, 20,
                Component.translatable("button.heartbound.propose"), button -> press(RelationshipMenu.BUTTON_PROPOSE));
        addRenderableWidget(proposeButton);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int affinity = menu.getAffinity();
        if (shownAffinity < 0F) {
            shownAffinity = 0F;
        }
        shownAffinity += (affinity - shownAffinity) * 0.18F;
        if (Math.abs(affinity - shownAffinity) < 0.5F) {
            shownAffinity = affinity;
        }

        if (followButton != null) {
            followButton.setMessage(Component.translatable(
                    menu.isFollowing() ? "button.heartbound.follow_stop" : "button.heartbound.follow"));
            followButton.active = menu.isFollowing() || affinity >= RelationshipStage.FRIENDS.threshold();
        }
        if (proposeButton != null) {
            proposeButton.setMessage(Component.translatable(
                    menu.isPartner() ? "button.heartbound.partner" : "button.heartbound.propose"));
            proposeButton.active = !menu.isPartner() && RelationshipStage.canPropose(affinity);
        }
        if (homeButton != null) {
            homeButton.active = menu.isPartner();
        }
        super.render(g, mouseX, mouseY, partialTick);
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
        boolean partner = menu.isPartner();

        // soft drop shadow
        UiDraw.roundedFill(g, x + 3, y + 6, x + WIDTH + 3, y + HEIGHT + 6, 9, 0x1E000000);
        UiDraw.roundedFill(g, x + 2, y + 4, x + WIDTH + 2, y + HEIGHT + 4, 8, 0x2E000000);
        // frame and body
        UiDraw.roundedFill(g, x, y, x + WIDTH, y + HEIGHT, 7, partner ? FRAME_PARTNER : FRAME);
        UiDraw.roundedGradient(g, x + 2, y + 2, x + WIDTH - 2, y + HEIGHT - 2, 5, 0xFFF4F7FB, 0xFFD2D9E5);
        // inner light on top, inner shade on the bottom
        g.fill(x + 8, y + 3, x + WIDTH - 8, y + 4, 0xAAFFFFFF);
        g.fill(x + 8, y + HEIGHT - 4, x + WIDTH - 8, y + HEIGHT - 3, 0x22000000);

        // portrait
        UiDraw.roundedFill(g, x + 10, y + 12, x + 70, y + 104, 4, FRAME);
        UiDraw.roundedGradient(g, x + 11, y + 13, x + 69, y + 103, 3, 0xFF2D3549, 0xFF141822);
        g.fill(x + 15, y + 14, x + 65, y + 15, 0x33FFFFFF);
        UiDraw.roundedFill(g, x + 17, y + 92, x + 63, y + 100, 4, 0x66000000);
        if (findTarget() instanceof LivingEntity living) {
            float size = Math.max(living.getBbHeight(), living.getBbWidth());
            int scale = (int) Math.max(12, Math.min(70, 56 / Math.max(0.3F, size)));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 11, y + 13, x + 69, y + 103,
                    scale, 0.0625F, (float) mouseX, (float) mouseY, living);
        }

        // affinity bar
        int bx = x + BAR_X;
        int by = y + BAR_Y;
        UiDraw.roundedFill(g, bx - 1, by - 2, bx + BAR_W + 1, by + BAR_H + 2, 5, 0x22000000);
        UiDraw.roundedFill(g, bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, 5, FRAME);
        UiDraw.roundedGradient(g, bx, by, bx + BAR_W, by + BAR_H, 4, 0xFF353C4C, 0xFF5B667A);
        int filled = (int) (BAR_W * Math.max(0F, Math.min(RelationshipStage.MAX_AFFINITY, shownAffinity))
                / RelationshipStage.MAX_AFFINITY);
        if (filled > 0) {
            int width = Math.max(filled, 4);
            UiDraw.roundedGradient(g, bx, by, bx + width, by + BAR_H, 4, 0xFFF2778A, 0xFFC0182E);
            if (width > 8) {
                g.fill(bx + 3, by + 1, bx + width - 3, by + 2, 0x77FFFFFF);
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
        RelationshipStage stage = menu.isPartner() ? RelationshipStage.PARTNERS : RelationshipStage.forAffinity(affinity);

        // name, lightly embossed
        g.drawString(font, name, 81, 15, 0x99FFFFFF, false);
        g.drawString(font, name, 80, 14, TEXT_MAIN, false);

        Gender gender = menu.getGender();
        boolean male = gender == Gender.MALE;
        int genderColor = male ? MALE_COLOR : FEMALE_COLOR;
        UiDraw.pixels(g, 80, 25, male ? MALE_ICON : FEMALE_ICON, genderColor);
        g.drawString(font, Component.translatable("gender.heartbound." + gender.key()), 93, 26, genderColor, false);

        g.drawString(font, Component.translatable("stage.heartbound." + stage.name().toLowerCase(Locale.ROOT)),
                80, 38, menu.isPartner() ? TEXT_PARTNER : TEXT_STAGE, false);
        g.drawString(font, Component.literal(affinity + " / " + RelationshipStage.MAX_AFFINITY), 80, 50, TEXT_MAIN, false);

        g.drawString(font, Component.translatable("hint.heartbound.pet"), 10, 168, TEXT_HINT, false);
    }
}
