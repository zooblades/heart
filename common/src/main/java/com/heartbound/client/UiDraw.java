package com.heartbound.client;

import net.minecraft.client.gui.GuiGraphics;

/** Small drawing helpers for flat, rounded UI built from filled rectangles (no textures needed). */
final class UiDraw {

    private UiDraw() {
    }

    /** How many pixels the given row of a rounded corner is inset (row 0 is the outermost one). */
    static int cornerInset(int radius, int row) {
        double d = radius - row - 0.5;
        return (int) Math.round(radius - Math.sqrt(radius * radius - d * d));
    }

    static int lerpColor(int a, int b, float t) {
        int aa = a >>> 24, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = b >>> 24, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int ra = Math.round(aa + (ba - aa) * t);
        int rr = Math.round(ar + (br - ar) * t);
        int rg = Math.round(ag + (bg - ag) * t);
        int rb = Math.round(ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    static void roundedFill(GuiGraphics g, int x1, int y1, int x2, int y2, int radius, int color) {
        int r = Math.max(0, Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));
        for (int row = 0; row < r; row++) {
            int inset = cornerInset(r, row);
            g.fill(x1 + inset, y1 + row, x2 - inset, y1 + row + 1, color);
            g.fill(x1 + inset, y2 - row - 1, x2 - inset, y2 - row, color);
        }
        g.fill(x1, y1 + r, x2, y2 - r, color);
    }

    /** A rounded rectangle filled with a vertical gradient. */
    static void roundedGradient(GuiGraphics g, int x1, int y1, int x2, int y2, int radius, int top, int bottom) {
        int h = y2 - y1;
        if (h <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min((x2 - x1) / 2, h / 2)));
        for (int row = 0; row < h; row++) {
            int inset = 0;
            if (row < r) {
                inset = cornerInset(r, row);
            } else if (row >= h - r) {
                inset = cornerInset(r, h - 1 - row);
            }
            float t = h <= 1 ? 0F : (float) row / (h - 1);
            g.fill(x1 + inset, y1 + row, x2 - inset, y1 + row + 1, lerpColor(top, bottom, t));
        }
    }

    /** A rounded rectangle with a 1px border. */
    static void roundedBox(GuiGraphics g, int x1, int y1, int x2, int y2, int radius, int border, int fill) {
        roundedFill(g, x1, y1, x2, y2, radius, border);
        roundedFill(g, x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, radius - 1), fill);
    }

    /** Draws a pixel-art icon: every '#' becomes one pixel. */
    static void pixels(GuiGraphics g, int x, int y, String[] rows, int color) {
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < rows[r].length(); c++) {
                if (rows[r].charAt(c) == '#') {
                    g.fill(x + c, y + r, x + c + 1, y + r + 1, color);
                }
            }
        }
    }
}
