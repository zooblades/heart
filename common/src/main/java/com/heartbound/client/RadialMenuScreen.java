package com.heartbound.client;

import com.heartbound.gesture.Gesture;
import com.heartbound.network.GesturePayload;
import com.heartbound.relationship.RelationshipStage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The circular gesture menu: hold the key, move the mouse to a sector, release the key to choose
 * (a click works too). Six sectors clockwise from the top: hold hands, hug, cheek kiss, kiss,
 * lie down (soon), back. Locked sectors are greyed out; the server validates every choice anyway.
 */
public class RadialMenuScreen extends Screen {

    private static final int OUTER = 88;
    private static final int INNER = 34;
    private static final int SECTORS = 6;
    private static final int BACK = 5;

    private static final int COLOR_NORMAL = 0xC03B4252;
    private static final int COLOR_LOCKED = 0xB0262B36;
    private static final int COLOR_HOVER = 0xF0D63A6A;
    private static final int COLOR_HOVER_BACK = 0xF05E81AC;
    private static final int COLOR_CENTER = 0xE01F2430;

    /** Rows of one-pixel-high runs: {x, y, width, sector}, relative to the centre. */
    private final List<int[]> ringRuns = new ArrayList<>();
    private final List<int[]> centerRuns = new ArrayList<>();

    private final int entityId;
    private final int affinity;
    private final boolean partner;

    private int lastMouseX;
    private int lastMouseY;

    public RadialMenuScreen(int entityId, int affinity, boolean partner) {
        super(Component.translatable("key.heartbound.gestures"));
        this.entityId = entityId;
        this.affinity = affinity;
        this.partner = partner;
        buildRuns();
    }

    // ---- geometry

    private static int sectorAt(int dx, int dy, boolean withGaps) {
        int d2 = dx * dx + dy * dy;
        if (d2 < INNER * INNER || d2 > OUTER * OUTER) {
            return -1;
        }
        double angle = (Math.toDegrees(Math.atan2(dx, -dy)) + 360.0D) % 360.0D;
        double shifted = (angle + 30.0D) % 360.0D;
        if (withGaps) {
            double within = shifted % 60.0D;
            if (within < 1.2D || within > 58.8D) {
                return -1;
            }
        }
        return (int) (shifted / 60.0D);
    }

    private void buildRuns() {
        int centerRadius = INNER - 5;
        for (int dy = -OUTER; dy <= OUTER; dy++) {
            int start = 0;
            int current = -1;
            for (int dx = -OUTER; dx <= OUTER + 1; dx++) {
                int sector = sectorAt(dx, dy, true);
                if (sector != current) {
                    if (current >= 0) {
                        ringRuns.add(new int[]{start, dy, dx - start, current});
                    }
                    start = dx;
                    current = sector;
                }
            }
            int half = (int) Math.floor(Math.sqrt((double) centerRadius * centerRadius - (double) dy * dy));
            if (Math.abs(dy) <= centerRadius) {
                centerRuns.add(new int[]{-half, dy, half * 2 + 1, 0});
            }
        }
    }

    private int hoveredSector() {
        return sectorAt(lastMouseX - width / 2, lastMouseY - height / 2, false);
    }

    // ---- state of a sector

    private Gesture gestureOf(int sector) {
        return sector >= 0 && sector < BACK ? Gesture.byId(sector) : null;
    }

    private boolean isEnabled(int sector) {
        if (sector == BACK) {
            return true;
        }
        Gesture gesture = gestureOf(sector);
        return gesture != null && gesture.implemented() && gesture.isUnlocked(affinity, partner);
    }

    // ---- rendering

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x7010141C);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        super.render(g, mouseX, mouseY, partialTick);

        int cx = width / 2;
        int cy = height / 2;
        int hovered = hoveredSector();

        for (int[] run : ringRuns) {
            int sector = run[3];
            int color;
            if (sector == hovered && isEnabled(sector)) {
                color = sector == BACK ? COLOR_HOVER_BACK : COLOR_HOVER;
            } else if (isEnabled(sector)) {
                color = COLOR_NORMAL;
            } else {
                color = COLOR_LOCKED;
            }
            g.fill(cx + run[0], cy + run[1], cx + run[0] + run[2], cy + run[1] + 1, color);
        }
        for (int[] run : centerRuns) {
            g.fill(cx + run[0], cy + run[1], cx + run[0] + run[2], cy + run[1] + 1, COLOR_CENTER);
        }

        double labelRadius = (INNER + OUTER) / 2.0D;
        for (int i = 0; i < SECTORS; i++) {
            double angle = Math.toRadians(i * 60.0D);
            int lx = cx + (int) Math.round(Math.sin(angle) * labelRadius);
            int ly = cy - (int) Math.round(Math.cos(angle) * labelRadius);
            boolean enabled = isEnabled(i);
            int color = enabled ? 0xFFFFFFFF : 0xFF7B8594;

            Component label = Component.translatable("gesture.heartbound." + (i == BACK ? "back" : gestureOf(i).key()));
            g.drawCenteredString(font, label, lx, ly - 8, color);

            Component sub = subLabel(i);
            if (sub != null) {
                g.pose().pushPose();
                g.pose().translate(lx, ly + 3, 0);
                g.pose().scale(0.75F, 0.75F, 1.0F);
                g.drawCenteredString(font, sub, 0, 0, 0xFF9AA3B2);
                g.pose().popPose();
            }
        }

        Entity entity = minecraft != null && minecraft.level != null ? minecraft.level.getEntity(entityId) : null;
        Component name = entity != null ? entity.getName() : Component.empty();
        RelationshipStage stage = partner ? RelationshipStage.PARTNERS : RelationshipStage.forAffinity(affinity);
        g.drawCenteredString(font, name, cx, cy - 8, 0xFFFFFFFF);
        g.pose().pushPose();
        g.pose().translate(cx, cy + 4, 0);
        g.pose().scale(0.75F, 0.75F, 1.0F);
        g.drawCenteredString(font, Component.translatable("stage.heartbound." + stage.name().toLowerCase(Locale.ROOT)),
                0, 0, partner ? 0xFFF48FB1 : 0xFF9AA3B2);
        g.pose().popPose();
    }

    private Component subLabel(int sector) {
        Gesture gesture = gestureOf(sector);
        if (gesture == null) {
            return null;
        }
        if (!gesture.implemented()) {
            return Component.translatable("gesture.heartbound.soon");
        }
        if (gesture.isUnlocked(affinity, partner)) {
            return null;
        }
        RelationshipStage needed = gesture.partnerOnly()
                ? RelationshipStage.PARTNERS
                : RelationshipStage.forAffinity(gesture.minAffinity());
        return Component.translatable("stage.heartbound." + needed.name().toLowerCase(Locale.ROOT));
    }

    // ---- input

    private void choose() {
        int sector = hoveredSector();
        if (sector >= 0 && sector != BACK && isEnabled(sector)) {
            ClientHooks.send(new GesturePayload(entityId, sector));
        }
        onClose();
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (KeyHandler.GESTURE_KEY.matches(keyCode, scanCode)) {
            choose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMouseX = (int) mouseX;
        lastMouseY = (int) mouseY;
        choose();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
