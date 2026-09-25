package dev.lumaglass.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.lumaglass.LumaGlass;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = LumaGlass.ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static boolean demoOpened;
    public static final KeyMapping OPEN = new KeyMapping("key.lumaglass.open", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.lumaglass");

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (OPEN.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new GlassScreen(null));
        }
        if (Boolean.getBoolean("lumaglass.demo") && !demoOpened
                && mc.screen instanceof TitleScreen && mc.getOverlay() == null && GlassRenderer.available()) {
            demoOpened = true;
            mc.setScreen(new GlassScreen(mc.screen));
        }
    }

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post e) {
        if (e.getScreen() instanceof TitleScreen screen) {
            e.addListener(Button.builder(Component.literal("LumaGlass"),
                    b -> Minecraft.getInstance().setScreen(new GlassScreen(screen)))
                    .bounds(8, 8, 88, 20).build());
        }
    }

    @Mod.EventBusSubscriber(modid = LumaGlass.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) { e.register(OPEN); }

        @SubscribeEvent
        public static void shaders(RegisterShadersEvent e) throws IOException {
            dev.lumaglass.api.client.GlassCanvas.releaseAll();
            e.registerShader(new ShaderInstance(e.getResourceProvider(),
                    new ResourceLocation(LumaGlass.ID, "glass"), DefaultVertexFormat.POSITION_TEX),
                    shader -> GlassRenderer.glassShader = shader);
            e.registerShader(new ShaderInstance(e.getResourceProvider(),
                    new ResourceLocation(LumaGlass.ID, "blur"), DefaultVertexFormat.POSITION_TEX),
                    shader -> GlassRenderer.blurShader = shader);
        }
    }
}

