package dev.lumaglass.api.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.lumaglass.client.GlassRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * API v1. One canvas per screen/overlay owner. Client/render thread only.
 * Reuse across frames; close when the owning screen/overlay is disposed.
 * Independent canvases cannot overwrite one another's background textures.
 */
public final class GlassCanvas implements AutoCloseable {
    public static final int API_VERSION = 1;
    private static final Set<GlassCanvas> LIVE = Collections.newSetFromMap(new WeakHashMap<>());
    private final GlassRenderer renderer = new GlassRenderer();
    private GlassFrame current;
    private boolean closed;

    public GlassCanvas() {
        RenderSystem.assertOnRenderThread();
        LIVE.add(this);
    }

    public static boolean isAvailable() { return GlassRenderer.available(); }

    /** Capture after drawing the background, before drawing any glass or foreground text. */
    public GlassFrame begin(GuiGraphics graphics, float blurRadius) {
        return begin(graphics, blurRadius, (float)Minecraft.getInstance().getWindow().getGuiScale());
    }

    /** pixelScale = window GUI scale multiplied by your uniform pose scale. */
    public GlassFrame begin(GuiGraphics graphics, float blurRadius, float pixelScale) {
        RenderSystem.assertOnRenderThread();
        if (closed) throw new IllegalStateException("Canvas is closed");
        if (current != null) throw new IllegalStateException("Close the previous GlassFrame first");
        if (graphics == null) throw new NullPointerException("graphics");
        if (!Float.isFinite(blurRadius) || blurRadius < 0 || blurRadius > 64)
            throw new IllegalArgumentException("blurRadius must be finite and within [0, 64]");
        if (!Float.isFinite(pixelScale) || pixelScale <= 0)
            throw new IllegalArgumentException("pixelScale must be finite and positive");
        renderer.capture(graphics, blurRadius, pixelScale);
        current = new GlassFrame(this, renderer, graphics, pixelScale);
        return current;
    }

    void end(GlassFrame frame) { if (current == frame) current = null; }

    @Override public void close() {
        RenderSystem.assertOnRenderThread();
        if (current != null) current.close();
        renderer.release();
        closed = true;
        LIVE.remove(this);
    }

    /** Internal resource-reload hook; open canvases lazily allocate fresh targets next frame. */
    public static void releaseAll() {
        RenderSystem.assertOnRenderThread();
        for (GlassCanvas canvas : LIVE) {
            if (canvas.current != null) canvas.current.close();
            canvas.renderer.release();
        }
    }
}
