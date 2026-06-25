package dev.takesome.helix.ui.render;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import dev.takesome.helix.ui.model.UiRect;

/** Converts UI world-space clip rectangles into GL scissor rectangles. */
final class UiScissorRectResolver {
    private UiScissorRectResolver() {
    }

    static UiScissorRect resolve(UiRect rect, Matrix4 projection, int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        if (rect == null || rect.w <= 0f || rect.h <= 0f || viewportWidth <= 0 || viewportHeight <= 0) return null;
        if (projection == null) {
            return clipped(
                    Math.round(rect.x),
                    Math.round(rect.y),
                    Math.round(rect.w),
                    Math.round(rect.h),
                    viewportX,
                    viewportY,
                    viewportWidth,
                    viewportHeight
            );
        }

        Vector3 a = new Vector3(rect.x, rect.y, 0f).prj(projection);
        Vector3 b = new Vector3(rect.x + rect.w, rect.y + rect.h, 0f).prj(projection);

        float minX = Math.min(a.x, b.x);
        float maxX = Math.max(a.x, b.x);
        float minY = Math.min(a.y, b.y);
        float maxY = Math.max(a.y, b.y);

        int x0 = (int) Math.floor(viewportX + (minX + 1f) * 0.5f * viewportWidth);
        int y0 = (int) Math.floor(viewportY + (minY + 1f) * 0.5f * viewportHeight);
        int x1 = (int) Math.ceil(viewportX + (maxX + 1f) * 0.5f * viewportWidth);
        int y1 = (int) Math.ceil(viewportY + (maxY + 1f) * 0.5f * viewportHeight);

        return clipped(x0, y0, x1 - x0, y1 - y0, viewportX, viewportY, viewportWidth, viewportHeight);
    }

    private static UiScissorRect clipped(int x, int y, int width, int height, int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        int x0 = Math.max(viewportX, x);
        int y0 = Math.max(viewportY, y);
        int x1 = Math.min(viewportX + viewportWidth, x + Math.max(0, width));
        int y1 = Math.min(viewportY + viewportHeight, y + Math.max(0, height));
        int w = x1 - x0;
        int h = y1 - y0;
        if (w <= 0 || h <= 0) return null;
        return new UiScissorRect(x0, y0, w, h);
    }

    record UiScissorRect(int x, int y, int width, int height) {
    }
}
