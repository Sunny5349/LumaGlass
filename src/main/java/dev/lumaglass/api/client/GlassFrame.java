package dev.lumaglass.api.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.lumaglass.client.GlassRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** A captured background shared by panels in this frame. Use try-with-resources. */
public final class GlassFrame implements AutoCloseable {
    private final GlassCanvas owner;
    private final GlassRenderer renderer;
    private final GuiGraphics graphics;
    private final float pixelScale;
    private boolean closed;

    GlassFrame(GlassCanvas owner, GlassRenderer renderer, GuiGraphics graphics, float pixelScale) {
        this.owner = owner; this.renderer = renderer; this.graphics = graphics; this.pixelScale = pixelScale;
    }

    public void panel(float x, float y, float width, float height, GlassStyle style) {
        panel(x, y, width, height, style, -10000, -10000);
    }

    /** Pointer coordinates must use the same local coordinate system as the panel. */
    public void panel(float x, float y, float width, float height, GlassStyle style, float mouseX, float mouseY) {
        check(x, y, width, height, style);
        if (!Float.isFinite(mouseX) || !Float.isFinite(mouseY))
            throw new IllegalArgumentException("Pointer coordinates must be finite");
        renderer.panel(graphics, x, y, width, height, style.radius(), style.refraction(), style.frost(),
                style.tint(), style.highlight(), mouseX, mouseY, pixelScale, style.opacity(), null, 0, 0, 1, 1, style.glowColor());
    }

    /** Glass with the original texture's alpha, interior slots and symbols. UVs are normalized. */
    public void texturedPanel(float x, float y, float width, float height, GlassStyle style,
                              ResourceLocation texture, float u0, float v0, float u1, float v1) {
        check(x, y, width, height, style);
        if (texture == null) throw new NullPointerException("texture");
        if (!Float.isFinite(u0) || !Float.isFinite(v0) || !Float.isFinite(u1) || !Float.isFinite(v1))
            throw new IllegalArgumentException("UVs must be finite");
        renderer.panel(graphics, x, y, width, height, style.radius(), style.refraction(), style.frost(),
                style.tint(), style.highlight(), -10000, -10000, pixelScale, style.opacity(), texture, u0, v0, u1, v1, style.glowColor());
    }

    private void check(float x, float y, float w, float h, GlassStyle style) {
        RenderSystem.assertOnRenderThread();
        if (closed) throw new IllegalStateException("Frame is closed");
        if (style == null) throw new NullPointerException("style");
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(w) || !Float.isFinite(h) || w < 0 || h < 0)
            throw new IllegalArgumentException("Panel bounds must be finite; sizes must be nonnegative");
    }

    @Override public void close() {
        RenderSystem.assertOnRenderThread();
        closed = true;
        owner.end(this);
    }
}
