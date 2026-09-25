package dev.lumaglass.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

/** Render-thread-only glass surface API. Capture once before drawing a group of panels. */
public final class GlassRenderer {
    public static ShaderInstance glassShader;
    public static ShaderInstance blurShader;
    private RenderTarget scene, ping, blurred;
    private boolean captured;
    private boolean hasBlur;

    public GlassRenderer() {}

    public static boolean available() { return glassShader != null && blurShader != null; }

    public void release() {
        RenderSystem.assertOnRenderThread();
        if (scene != null) scene.destroyBuffers();
        if (ping != null) ping.destroyBuffers();
        if (blurred != null) blurred.destroyBuffers();
        scene = ping = blurred = null;
        captured = false;
    }

    private static RenderTarget target(int w, int h) {
        RenderTarget t = new TextureTarget(w, h, false, Minecraft.ON_OSX);
        t.setFilterMode(GL11.GL_LINEAR);
        return t;
    }

    /** Radius and pixelScale use the same local coordinate system as panel(). */
    public void capture(GuiGraphics gui, float radius, float pixelScale) {
        gui.flush();
        captured = false;
        if (!available()) return;
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        State state = new State();
        try {
            if (scene == null || scene.width != main.width || scene.height != main.height) {
                release();
                scene = target(main.width, main.height);
                ping = target(Math.max(1, main.width / 2), Math.max(1, main.height / 2));
                blurred = target(ping.width, ping.height);
            }
            RenderSystem.disableScissor();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            // Copy first: never read a texture while simultaneously writing into it.
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scene.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, scene.width, scene.height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            hasBlur = radius > 0;
            if (hasBlur) {
                pass(scene, ping, radius * pixelScale / scene.width, 0);
                pass(ping, blurred, 0, radius * pixelScale / scene.height);
            }
            captured = true;
        } finally {
            main.bindWrite(true);
            state.restore();
        }
    }

    private static void pass(RenderTarget source, RenderTarget destination, float dx, float dy) {
        // Both JSON blend declarations match vanilla's core shaders. Different declarations
        // poison BlendMode's global cache and overwrite the vignette's explicit ZERO blend.
        destination.bindWrite(true);
        RenderSystem.setShader(() -> blurShader);
        blurShader.setSampler("Source", source.getColorTextureId());
        blurShader.safeGetUniform("Direction").set(dx, dy);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.vertex(-1, -1, 0).uv(0, 0).endVertex();
        b.vertex(1, -1, 0).uv(1, 0).endVertex();
        b.vertex(1, 1, 0).uv(1, 1).endVertex();
        b.vertex(-1, 1, 0).uv(0, 1).endVertex();
        BufferUploader.drawWithShader(b.end());
    }

    /** Draw in gui.pose() local coordinates. pixelScale = window GUI scale * local pose scale. */
    public void panel(GuiGraphics gui, float x, float y, float w, float h, float corner,
                             float refraction, float frost, float tint, float highlight,
                             float mouseX, float mouseY, float pixelScale, float opacity,
                             ResourceLocation artwork, float u0, float v0, float u1, float v1, int glowColor) {
        if (w <= 0 || h <= 0 || opacity <= 0) return;
        gui.flush();
        if (!captured || !available()) {
            gui.fill((int)x, (int)y, (int)(x+w), (int)(y+h), ((int)(176*opacity)<<24)|0x233045);
            return;
        }
        State state = new State();
        try {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(() -> glassShader);
            glassShader.setSampler("Scene", scene.getColorTextureId());
            glassShader.setSampler("Blurred", (hasBlur ? blurred : scene).getColorTextureId());
            glassShader.setSampler("Artwork", artwork == null ? scene.getColorTextureId()
                    : Minecraft.getInstance().getTextureManager().getTexture(artwork).getId());
            glassShader.safeGetUniform("UseArtwork").set(artwork == null ? 0f : 1f);
            glassShader.safeGetUniform("ArtworkUv").set(u0, v0, u1, v1);
            glassShader.safeGetUniform("Opacity").set(opacity);
            glassShader.safeGetUniform("FrameSize").set((float)scene.width, (float)scene.height);
            glassShader.safeGetUniform("PanelSize").set(w, h);
            glassShader.safeGetUniform("Mouse").set(mouseX-x, mouseY-y);
            glassShader.safeGetUniform("Radius").set(Math.min(corner, Math.min(w, h) * .5f));
            glassShader.safeGetUniform("Refraction").set(refraction);
            glassShader.safeGetUniform("Frost").set(frost);
            glassShader.safeGetUniform("Tint").set(tint);
            glassShader.safeGetUniform("Highlight").set(highlight);
            boolean configured = dev.lumaglass.GlassConfig.SPEC.isLoaded();
            int glow = configured ? dev.lumaglass.GlassConfig.GLOW_COLOR.get() : 0xffffff;
            if (glowColor != dev.lumaglass.api.client.GlassStyle.GLOBAL_GLOW_COLOR) glow = glowColor;
            float glowStrength = configured ? (dev.lumaglass.GlassConfig.GLOW_ENABLED.get()
                    ? dev.lumaglass.GlassConfig.GLOW_STRENGTH.get().floatValue() : 0) : 1;
            glassShader.safeGetUniform("GlowColor").set(((glow >> 16) & 255)/255f, ((glow >> 8) & 255)/255f, (glow & 255)/255f);
            glassShader.safeGetUniform("GlowStrength").set(glowStrength);
            glassShader.safeGetUniform("PixelScale").set(pixelScale);
            float pad = 12;
            Matrix4f matrix = gui.pose().last().pose();
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            b.vertex(matrix, x-pad, y+h+pad, 0).uv(-pad, h+pad).endVertex();
            b.vertex(matrix, x+w+pad, y+h+pad, 0).uv(w+pad, h+pad).endVertex();
            b.vertex(matrix, x+w+pad, y-pad, 0).uv(w+pad, -pad).endVertex();
            b.vertex(matrix, x-pad, y-pad, 0).uv(-pad, -pad).endVertex();
            BufferUploader.drawWithShader(b.end());
        } finally { state.restore(); }
    }

    /** Preserve the caller's GUI state, including blend factors and scissor. */
    private static final class State {
        final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        final int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        final int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        final int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        final ShaderInstance shader = RenderSystem.getShader();

        void restore() {
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            // Capture only disables the test; it does not change the rectangle.
            if (scissor) GlStateManager._enableScissorTest(); else RenderSystem.disableScissor();
            RenderSystem.depthMask(depthMask);
            GlStateManager._blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            RenderSystem.activeTexture(activeTexture);
            RenderSystem.setShader(() -> shader);
        }
    }
}

