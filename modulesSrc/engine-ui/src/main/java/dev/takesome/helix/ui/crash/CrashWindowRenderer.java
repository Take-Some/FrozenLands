package dev.takesome.helix.ui.crash;

import dev.takesome.helix.logging.EngineLog;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.render.awt.AwtUiRenderContext;
import org.apache.logging.log4j.Logger;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/** Back-buffered AWT renderer for the crash window canvas. */
final class CrashWindowRenderer {
    private static final Logger LOG = EngineLog.logger(CrashWindowRenderer.class);

    private final Node root;
    private final CrashWindowCopyFeedback copyFeedback;
    private final AwtUiRenderContext renderContext = new AwtUiRenderContext();
    private BufferedImage backBuffer;
    private long lastFrameNanos = System.nanoTime();

    CrashWindowRenderer(Node root, CrashWindowCopyFeedback copyFeedback) {
        this.root = root;
        this.copyFeedback = copyFeedback;
    }

    void render(Graphics2D graphics, int width, int height) {
        int logicalWidth = Math.max(1, width);
        int logicalHeight = Math.max(1, height);
        double scaleX = deviceScale(graphics, true);
        double scaleY = deviceScale(graphics, false);
        int bufferWidth = Math.max(1, (int) Math.ceil(logicalWidth * scaleX));
        int bufferHeight = Math.max(1, (int) Math.ceil(logicalHeight * scaleY));

        // AWT paint graphics is already HiDPI-scaled. A logical-size BufferedImage gets
        // scaled up on 125%/150% displays, which blurs text. The back buffer must be
        // device-sized while the UI tree still renders in logical coordinates.
        if (backBuffer == null || backBuffer.getWidth() != bufferWidth || backBuffer.getHeight() != bufferHeight) {
            backBuffer = new BufferedImage(bufferWidth, bufferHeight, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D buffer = backBuffer.createGraphics();
        try {
            buffer.scale(scaleX, scaleY);
            AwtUiRenderContext.applyUiRenderingHints(buffer);
            root.update(frameDeltaSeconds());
            buffer.setColor(Color.WHITE);
            buffer.fillRect(0, 0, logicalWidth, logicalHeight);
            renderContext.begin(buffer, logicalHeight);
            Throwable renderFailure = null;
            try {
                root.render(renderContext);
            } catch (Throwable error) {
                renderFailure = error;
                LOG.warn("Crash window UI render failed; drawing fallback", error);
            } finally {
                renderContext.end();
            }
            if (renderFailure != null) {
                drawRenderFallback(buffer, renderFailure);
            } else {
                copyFeedback.draw(buffer, logicalHeight);
            }
        } finally {
            buffer.dispose();
        }

        Object oldInterpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.drawImage(backBuffer, 0, 0, logicalWidth, logicalHeight, null);
        if (oldInterpolation != null) {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
        }
    }

    private static double deviceScale(Graphics2D graphics, boolean xAxis) {
        double scale = Double.NaN;
        if (graphics != null) {
            AffineTransform current = graphics.getTransform();
            scale = xAxis ? current.getScaleX() : current.getScaleY();
            if ((!Double.isFinite(scale) || scale == 0d) && graphics.getDeviceConfiguration() != null) {
                AffineTransform device = graphics.getDeviceConfiguration().getDefaultTransform();
                scale = xAxis ? device.getScaleX() : device.getScaleY();
            }
        }
        if (!Double.isFinite(scale) || scale == 0d) {
            return 1d;
        }
        return Math.max(1d, Math.abs(scale));
    }

    private float frameDeltaSeconds() {
        long now = System.nanoTime();
        long delta = Math.max(0L, now - lastFrameNanos);
        lastFrameNanos = now;
        return Math.min(0.10f, delta / 1_000_000_000f);
    }

    private static void drawRenderFallback(Graphics2D graphics, Throwable error) {
        graphics.setColor(new Color(255, 255, 255));
        graphics.fillRect(24, 24, CrashWindowLayout.WIDTH - 48, CrashWindowLayout.HEIGHT - 48);
        graphics.setColor(new Color(17, 24, 39));
        graphics.drawString("Crash window render failed", 48, 64);
        graphics.setColor(new Color(180, 83, 9));
        graphics.drawString(String.valueOf(error == null ? "unknown" : error.getMessage()), 48, 88);
    }
}
