package dev.takesome.helix.ui.render;

import com.badlogic.gdx.math.Matrix4;
import dev.takesome.helix.ui.model.UiRect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UiScissorRectResolverTest {
    @Test
    void mapsVirtualClipRectThroughProjectionAndLetterboxedViewport() {
        Matrix4 projection = new Matrix4().setToOrtho2D(0f, 0f, 1280f, 720f);

        UiScissorRectResolver.UiScissorRect full = UiScissorRectResolver.resolve(
                new UiRect(0f, 0f, 1280f, 720f), projection, 30, 67, 860, 484
        );

        assertNotNull(full);
        assertEquals(30, full.x());
        assertEquals(67, full.y());
        assertEquals(860, full.width());
        assertEquals(484, full.height());
    }

    @Test
    void clipsProjectedRectToCurrentViewport() {
        Matrix4 projection = new Matrix4().setToOrtho2D(0f, 0f, 1280f, 720f);

        UiScissorRectResolver.UiScissorRect clipped = UiScissorRectResolver.resolve(
                new UiRect(-128f, -72f, 512f, 288f), projection, 0, 0, 1280, 720
        );

        assertNotNull(clipped);
        assertEquals(0, clipped.x());
        assertEquals(0, clipped.y());
        assertEquals(384, clipped.width());
        assertEquals(217, clipped.height());
    }
}
