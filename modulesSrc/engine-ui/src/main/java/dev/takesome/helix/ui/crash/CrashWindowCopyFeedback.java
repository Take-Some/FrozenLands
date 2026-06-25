package dev.takesome.helix.ui.crash;

import dev.takesome.helix.concurrent.EngineExecutors;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/** Timed visual confirmation overlay after copy-to-clipboard attempts. */
final class CrashWindowCopyFeedback {
    private long untilNanos;
    private int generation;
    private boolean success = true;

    void mark(boolean copySucceeded, Runnable repaint) {
        success = copySucceeded;
        untilNanos = System.nanoTime() + CrashWindowLayout.COPY_FEEDBACK_NANOS;
        int currentGeneration = ++generation;
        repaint(repaint);
        scheduleClear(currentGeneration, repaint);
    }

    void draw(Graphics2D graphics, int canvasHeight) {
        if (!active()) {
            return;
        }
        drawButton(graphics, canvasHeight, 518, 288, 86, 28, success ? "OK" : "FAIL");
        drawButton(graphics, canvasHeight, 254, 8, 220, 32, success ? "COPIED" : "COPY FAILED");
    }

    private boolean active() {
        return untilNanos > 0L && System.nanoTime() < untilNanos;
    }

    private void scheduleClear(int expectedGeneration, Runnable repaint) {
        Thread thread = EngineExecutors.daemonThreadFactory("helix-crash-copy-feedback").newThread(() -> {
            try {
                Thread.sleep(CrashWindowLayout.COPY_FEEDBACK_MILLIS + 80L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
            EventQueue.invokeLater(() -> {
                if (generation == expectedGeneration) {
                    untilNanos = 0L;
                    repaint(repaint);
                }
            });
        });
        thread.start();
    }

    private static void repaint(Runnable repaint) {
        if (repaint != null) {
            repaint.run();
        }
    }

    private void drawButton(Graphics2D graphics, int canvasHeight, int x, int uiY, int width, int height, String label) {
        int y = canvasHeight - uiY - height;
        int arc = Math.max(8, Math.min(14, height / 2));
        Color fill = success ? new Color(22, 163, 74) : new Color(220, 38, 38);
        Color border = success ? new Color(134, 239, 172) : new Color(254, 202, 202);
        Color glyph = success ? new Color(22, 163, 74) : new Color(220, 38, 38);
        Color text = success ? new Color(240, 253, 244) : new Color(255, 241, 242);

        graphics.setColor(fill);
        graphics.fillRoundRect(x, y, width, height, arc, arc);
        graphics.setColor(border);
        graphics.drawRoundRect(x, y, width, height, arc, arc);

        Font previousFont = graphics.getFont();
        Color previousColor = graphics.getColor();
        try {
            int iconSize = Math.max(14, Math.min(18, height - 10));
            int iconX = x + Math.max(9, height / 2 - 2);
            int iconY = y + (height - iconSize) / 2;
            graphics.setColor(new Color(240, 253, 244));
            graphics.fillOval(iconX, iconY, iconSize, iconSize);

            Font iconFont = previousFont.deriveFont(Font.BOLD, height >= 32 ? 14f : 12f);
            graphics.setFont(iconFont);
            graphics.setColor(glyph);
            FontMetrics iconMetrics = graphics.getFontMetrics();
            String icon = success ? "\u2713" : "!";
            int checkX = iconX + Math.max(0, (iconSize - iconMetrics.stringWidth(icon)) / 2);
            int checkY = iconY + (iconSize - iconMetrics.getHeight()) / 2 + iconMetrics.getAscent();
            graphics.drawString(icon, checkX, checkY);

            Font labelFont = previousFont.deriveFont(Font.BOLD, height >= 32 ? 12f : 10.5f);
            graphics.setFont(labelFont);
            graphics.setColor(text);
            FontMetrics labelMetrics = graphics.getFontMetrics();
            int labelX = iconX + iconSize + Math.max(6, height / 5);
            int labelY = y + (height - labelMetrics.getHeight()) / 2 + labelMetrics.getAscent();
            graphics.drawString(label, labelX, labelY);
        } finally {
            graphics.setFont(previousFont);
            graphics.setColor(previousColor);
        }
    }
}
