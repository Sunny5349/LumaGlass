package dev.lumaglass.smoke;

import com.mojang.logging.LogUtils;
import dev.lumaglass.GlassConfig;
import dev.lumaglass.api.client.*;
import example.glass.ExampleGlassScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

/** Test harness only; never included in the LumaGlass release or sources JAR. */
@Mod.EventBusSubscriber(value=Dist.CLIENT)
public final class UiSmokeSuite {
    private static int stage=-1,age;
    private static boolean captured,finished,reloaded;
    private static final String[] NAMES={"title","options","sound-sliders","controls","language-list","container",
            "widgets-tooltips","hud-fixture","api-example","api-isolation","disabled","resized","reloaded",
            "world-hud","world-inventory","world-pause","world-chat","world-studio","world-disabled",
            "creative-inventory","language-scrolled","return-title","singleplayer","singleplayer-filter",
            "multiplayer","singleplayer-disabled","glow-palette-red","glow-palette-blue","glow-palette-off",
            "glow-red-probe","glow-blue-probe","glow-off-probe","neutral-tint-probe",
            "api-panel-colors","api-panel-colors-disabled","creative-tabs-probe"};
    private static boolean worldPrepared;

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if (!Boolean.getBoolean("lumaglass.smoke") || e.phase!=TickEvent.Phase.END || finished) return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.getOverlay()!=null || !GlassCanvas.isAvailable() || !GlassConfig.SPEC.isLoaded())return;
        if(stage<0) { checkContracts(); next(mc); return; }
        if(stage==13) {
            if(mc.level==null || mc.player==null || mc.screen!=null)return;
            if(!worldPrepared) {
                worldPrepared=true;
                mc.player.setXRot(12);
                var id=mc.player.getUUID();
                var server=mc.getSingleplayerServer();
                server.execute(()->{
                    var player=server.getPlayerList().getPlayer(id);
                    player.getInventory().setItem(0,new ItemStack(Items.DIAMOND_SWORD));
                    player.getInventory().setItem(1,new ItemStack(Items.APPLE,16));
                    player.inventoryMenu.broadcastChanges();
                });
            }
        }
        age++;
        if(age>=40 && captured) {
            if(stage==NAMES.length-1) {
                finished=true;
                GlassConfig.VANILLA_UI.set(true);
                GlassConfig.GLOW_ENABLED.set(true); GlassConfig.GLOW_COLOR.set(0xffffff); GlassConfig.GLOW_STRENGTH.set(1.0);
                GlassConfig.SPEC.save();
                LogUtils.getLogger().info("LUMAGLASS_SMOKE_PASS: {} screens, API contracts, reload and resize",NAMES.length);
                mc.stop();
            } else next(mc);
        }
    }

    private static void next(Minecraft mc) {
        stage++;age=0;captured=false;
        if(stage==13) {
            String worldName="LumaGlass-smoke-"+System.currentTimeMillis();
            var settings=new net.minecraft.world.level.LevelSettings(worldName,
                    net.minecraft.world.level.GameType.SURVIVAL,false,net.minecraft.world.Difficulty.PEACEFUL,
                    true,new net.minecraft.world.level.GameRules(),net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(worldName,settings,
                    new net.minecraft.world.level.levelgen.WorldOptions(81283L,false,false),
                    registries->registries.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                            .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        Screen parent=new TitleScreen();
        Screen screen=switch(stage) {
            case 0 -> parent;
            case 1 -> new OptionsScreen(parent,mc.options);
            case 2 -> new SoundOptionsScreen(parent,mc.options);
            case 3 -> new ControlsScreen(parent,mc.options);
            case 4 -> new LanguageSelectScreen(parent,mc.options,mc.getLanguageManager());
            case 5 -> new ContainerHost();
            case 6 -> new WidgetHost();
            case 7 -> new HudHost();
            case 8 -> new ExampleGlassScreen();
            case 9 -> new IsolationHost();
            case 10 -> {GlassConfig.VANILLA_UI.set(false);yield new OptionsScreen(parent,mc.options);}
            case 11 -> {
                GlassConfig.VANILLA_UI.set(true);
                GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1200,800);
                yield new OptionsScreen(parent,mc.options);
            }
            case 12 -> new OptionsScreen(parent,mc.options);
            case 14 -> new InventoryScreen(mc.player);
            case 15 -> new PauseScreen(true);
            case 16 -> new ChatScreen("");
            case 18 -> {GlassConfig.VANILLA_UI.set(false);yield null;}
            case 19 -> {
                GlassConfig.VANILLA_UI.set(true);
                mc.gameMode.setLocalMode(net.minecraft.world.level.GameType.CREATIVE);
                yield new CreativeModeInventoryScreen(mc.player,mc.level.enabledFeatures(),true);
            }
            case 20 -> new LanguageSelectScreen(parent,mc.options,mc.getLanguageManager());
            case 21 -> {mc.level.disconnect();mc.clearLevel(parent);yield parent;}
            case 22,23 -> new SelectWorldScreen(parent);
            case 24 -> new JoinMultiplayerScreen(parent);
            case 25 -> {GlassConfig.VANILLA_UI.set(false);yield new SelectWorldScreen(parent);}
            case 26,27,28 -> {
                GlassConfig.VANILLA_UI.set(true);
                yield new dev.lumaglass.client.GlowPaletteScreen(parent);
            }
            case 29,30,31 -> new GlowProbeHost();
            case 32 -> new NeutralTintHost();
            case 33,34 -> new PanelColorHost();
            case 35 -> new CreativeTabsHost();
            default -> new dev.lumaglass.client.GlassScreen(null);
        };
        mc.setScreen(screen);
        if(stage==33 || stage==34) {
            GlassConfig.GLOW_ENABLED.set(stage==33); GlassConfig.GLOW_COLOR.set(0x00ff00); GlassConfig.GLOW_STRENGTH.set(1.0);
        }
        if(stage==35) GlassConfig.GLOW_ENABLED.set(false);
        if(stage==26 || stage==27) {
            var field=(EditBox)screen.children().stream().filter(c->c instanceof EditBox).findFirst().orElseThrow();
            int old=GlassConfig.GLOW_COLOR.get();
            field.setValue("#F");
            if(GlassConfig.GLOW_COLOR.get()!=old)throw new AssertionError("Partial hex changed color");
            if(stage==26) {
                var square=(AbstractWidget)screen.children().get(0);
                square.mouseClicked(square.getX()+50,square.getY()+30,0);
                square.mouseDragged(square.getX()+90,square.getY()+10,0,40,-20);
                square.mouseReleased(square.getX()+90,square.getY()+10,0);
                int before=GlassConfig.GLOW_COLOR.get();
                square.keyPressed(GLFW.GLFW_KEY_LEFT,0,0);
                if(before==GlassConfig.GLOW_COLOR.get())throw new AssertionError("Palette keyboard input lost");
                LogUtils.getLogger().info("LUMAGLASS_PALETTE_INPUT_PASS");
            }
            field.setValue(stage==26?"#FF4020":"#3070FF");
            if(GlassConfig.GLOW_COLOR.get()!=(stage==26?0xff4020:0x3070ff))throw new AssertionError("Hex input did not apply");
        }
        if(stage==28) {
            var toggle=screen.children().stream().filter(c->c instanceof Button)
                    .map(c->(Button)c).filter(b->b.getMessage().getString().startsWith(Component.translatable("screen.lumaglass.glow").getString()+":"))
                    .findFirst().orElseThrow();
            toggle.onPress();
            if(GlassConfig.GLOW_ENABLED.get())throw new AssertionError("Glow toggle failed");
        }
        if(stage==29) {
            try {
                String saved=java.nio.file.Files.readString(mc.gameDirectory.toPath().resolve("config/lumaglass-client.toml"));
                if(!saved.contains("glowEnabled = false") || !saved.contains("glowColor = "+0x3070ff))
                    throw new AssertionError("Palette settings not saved");
            } catch(java.io.IOException e) {throw new RuntimeException(e);}
            LogUtils.getLogger().info("LUMAGLASS_GLOW_SAVE_PASS");
        }
        if(stage>=29 && stage<=31) {
            GlassConfig.GLOW_COLOR.set(stage==30?0x0000ff:0xff0000);
            GlassConfig.GLOW_ENABLED.set(stage!=31); GlassConfig.GLOW_STRENGTH.set(1.0);
        }
        if(stage==20) {
            var list=(AbstractSelectionList<?>)screen.children().stream().filter(c->c instanceof AbstractSelectionList).findFirst().orElseThrow();
            list.setScrollAmount(0);
            list.mouseScrolled(screen.width/2.0,80,-4);
            if(list.getScrollAmount()<=0)throw new AssertionError("List wheel input lost");
            double barX=screen.width/2+146,barY=list.getTop()+20;
            list.mouseClicked(barX,barY,0);
            double before=list.getScrollAmount();
            list.mouseDragged(barX,barY+50,0,0,50);
            list.mouseReleased(barX,barY+50,0);
            if(list.getScrollAmount()<=before)throw new AssertionError("List drag input lost");
            list.setScrollAmount(list.getMaxScroll()*.6);
            LogUtils.getLogger().info("LUMAGLASS_SCROLL_INPUT_PASS");
        }
        if(stage==23) screen.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c)
                .findFirst().orElseThrow().setValue("no-matching-world");
        if(stage==12) mc.reloadResourcePacks().thenRun(()->{reloaded=true;LogUtils.getLogger().info("LUMAGLASS_RELOAD_PASS");});
    }

    @SubscribeEvent public static void render(ScreenEvent.Render.Post e) {
        capture(e.getGuiGraphics());
    }
    @SubscribeEvent public static void beforeScreen(ScreenEvent.Render.Pre e) {
        if(Boolean.getBoolean("lumaglass.smoke") && (stage==22 || stage==23)) {
            // Deliberately poison the preceding frame. A transparent list must not sample it.
            e.getGuiGraphics().fill(0,0,e.getScreen().width,e.getScreen().height,0xffff00ff);
            e.getGuiGraphics().flush();
        }
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e) {
        if((stage==13 || stage==18) && Minecraft.getInstance().screen==null) capture(e.getGuiGraphics());
    }
    private static void capture(GuiGraphics graphics) {
        if(stage<0 || finished || captured || age<25 || (stage==12&&!reloaded))return;
        if(Minecraft.getInstance().getOverlay()!=null)return;
        graphics.flush();
        int error=GL11.glGetError();
        if(error!=GL11.GL_NO_ERROR)throw new IllegalStateException("OpenGL error "+error+" at "+NAMES[stage]);
        if(stage==13) {
            try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                int sky=image.getPixelRGBA(image.getWidth()/2,image.getHeight()/4);
                if(((sky>>16)&255)<80 || ((sky>>16)&255)<(sky&255)+5)
                    throw new AssertionError("World sky was obscured: "+Integer.toHexString(sky));
            }
            LogUtils.getLogger().info("LUMAGLASS_WORLD_COMPOSITE_PASS");
        }
        if(stage==22 || stage==23) {
            try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                int stale=0;
                for(int y=0;y<image.getHeight();y+=8)for(int x=0;x<image.getWidth();x+=8) {
                    int p=image.getPixelRGBA(x,y);
                    if((p&255)>170 && ((p>>16)&255)>170 && ((p>>8)&255)<60)stale++;
                }
                if(stale>10)throw new AssertionError("Singleplayer sampled stale frame: "+stale);
            }
            LogUtils.getLogger().info("LUMAGLASS_FRESH_BACKGROUND_PASS {}",NAMES[stage]);
        }
        captured=true;
        Minecraft mc=Minecraft.getInstance();
        String name=String.format("%02d-%s.png",stage,NAMES[stage]);
        Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->LogUtils.getLogger().info("LUMAGLASS_SCREENSHOT {}",name));
    }

    private static final class IsolationHost extends Screen implements GlassThemeExempt {
        private GlassCanvas a,b;
        private boolean checked;
        IsolationHost(){super(Component.literal("API isolation test"));}
        @Override public void render(GuiGraphics g,int mx,int my,float dt) {
            if(a==null){a=new GlassCanvas();b=new GlassCanvas();}
            g.fill(0,0,width,height,0xff2040d0);
            try(GlassFrame fa=a.begin(g,0)) {
                if(!checked)try{a.begin(g,0);throw new AssertionError("Nested same-canvas begin accepted");}catch(IllegalStateException expected){}
                g.fill(0,0,width,height,0xffd04020);
                try(GlassFrame fb=b.begin(g,0)) {
                    GlassStyle clear=new GlassStyle(8,0,0,0,0,1);
                    fa.panel(20,30,100,100,clear);
                    fb.panel(140,30,100,100,clear);
                }
            }
            if(!checked) {
                g.flush();
                try(var image=Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
                    double s=minecraft.getWindow().getGuiScale();
                    int left=image.getPixelRGBA((int)(70*s),(int)(80*s));
                    int right=image.getPixelRGBA((int)(190*s),(int)(80*s));
                    if(((left>>16)&255)<150 || (left&255)>80 || (right&255)<150 || ((right>>16)&255)>80)
                        throw new AssertionError("Canvas backgrounds interfered: "+Integer.toHexString(left)+" / "+Integer.toHexString(right));
                }
                checked=true;
                LogUtils.getLogger().info("LUMAGLASS_API_ISOLATION_PASS");
            }
            g.drawString(font,"Canvas A: blue",25,40,0xffffffff);
            g.drawString(font,"Canvas B: red",145,40,0xffffffff);
        }
        @Override public void removed(){if(a!=null)a.close();if(b!=null)b.close();}
    }

    private static final class GlowProbeHost extends Screen implements GlassThemeExempt {
        private GlassCanvas canvas;
        private boolean checked;
        GlowProbeHost(){super(Component.literal("Glow pixel probe"));}
        @Override public void render(GuiGraphics g,int mx,int my,float dt) {
            g.fill(0,0,width,height,0xff202020);
            if(canvas==null)canvas=new GlassCanvas();
            try(var frame=canvas.begin(g,0)) {
                frame.panel(width/2-100,40,200,100,new GlassStyle(15,0,0,0,0,1));
            }
            g.flush();
            if(!checked)try(var image=Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
                double scale=minecraft.getWindow().getGuiScale();
                int p=image.getPixelRGBA((int)(width/2*scale),(int)(41*scale));
                int r=p&255,b=(p>>16)&255;
                if(stage==29 && r<b+25 || stage==30 && b<r+25 || stage==31 && (Math.abs(r-b)>3 || r>40))
                    throw new AssertionError("Glow color/off pixels incorrect: stage="+stage+" pixel="+Integer.toHexString(p));
                checked=true;LogUtils.getLogger().info("LUMAGLASS_GLOW_PIXEL_PASS {} pixel={}",NAMES[stage],Integer.toHexString(p));
            }
        }
        @Override public void removed(){if(canvas!=null)canvas.close();}
    }

    private static final class NeutralTintHost extends Screen implements GlassThemeExempt {
        private GlassCanvas canvas;
        private boolean checked;
        NeutralTintHost(){super(Component.literal("Neutral material probe"));}
        @Override public void render(GuiGraphics g,int mx,int my,float dt) {
            GlassConfig.GLOW_ENABLED.set(false);
            g.fill(0,0,width,height,0xff606060);
            if(canvas==null)canvas=new GlassCanvas();
            try(var frame=canvas.begin(g,5)) {
                frame.panel(20,30,140,140,new GlassStyle(12,9,.65f,.4f,1,1));
            }
            g.flush();
            if(!checked)try(var image=Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
                double scale=minecraft.getWindow().getGuiScale();
                for(int y=28;y<=180;y+=4)for(int x=18;x<=170;x+=4) {
                    int p=image.getPixelRGBA((int)(x*scale),(int)(y*scale));
                    int r=p&255,green=(p>>8)&255,b=(p>>16)&255;
                    if(Math.max(r,Math.max(green,b))-Math.min(r,Math.min(green,b))>2)
                        throw new AssertionError("Neutral glass adds color: "+Integer.toHexString(p));
                }
                checked=true;LogUtils.getLogger().info("LUMAGLASS_NEUTRAL_TINT_PASS: surface, rim and shadow");
            }
        }
        @Override public void removed(){if(canvas!=null)canvas.close();}
    }

    private static final class PanelColorHost extends Screen implements GlassThemeExempt {
        private GlassCanvas canvas;
        private boolean checked;
        PanelColorHost(){super(Component.literal("Independent panel glow"));}
        @Override public void render(GuiGraphics g,int mx,int my,float dt) {
            g.fill(0,0,width,height,0xff202020);
            if(canvas==null)canvas=new GlassCanvas();
            GlassStyle base=new GlassStyle(8,0,0,0,0,1);
            try(var frame=canvas.begin(g,0)) {
                frame.panel(10,40,100,80,base.withGlowColor(0xff0000));
                frame.panel(120,40,100,80,base.withGlowColor(0x0000ff).withOpacity(1).withRadius(8));
                frame.panel(230,40,100,80,base.withGlowColor(0xff0000).withGlobalGlowColor());
                // Test both the plain and textured public drawing paths in the same capture.
                ResourceLocation opaque=new ResourceLocation("lumaglass_smoke","textures/white.png");
                frame.texturedPanel(10,145,100,70,base.withGlowColor(0xff0000),opaque,0,0,1,1);
                frame.texturedPanel(120,145,100,70,base.withGlowColor(0x0000ff),opaque,0,0,1,1);
                frame.texturedPanel(230,145,100,70,base,opaque,0,0,1,1);
            }
            g.flush();
            if(!checked)try(var image=Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
                double scale=minecraft.getWindow().getGuiScale();
                for(int y:new int[]{41,146})for(int i=0;i<3;i++) {
                    int p=image.getPixelRGBA((int)((60+i*110)*scale),(int)(y*scale));
                    int[] c={p&255,(p>>8)&255,(p>>16)&255};
                    int channel=i==0?0:i==1?2:1;
                    if(stage==33) {
                        for(int k=0;k<3;k++)if(k!=channel && c[channel]<c[k]+25)
                            throw new AssertionError("Panel glow colors leaked: "+i+" / "+Integer.toHexString(p));
                    } else if(Math.max(c[0],Math.max(c[1],c[2]))-Math.min(c[0],Math.min(c[1],c[2]))>2)
                        throw new AssertionError("Panel override bypassed global off: "+Integer.toHexString(p));
                }
                checked=true; LogUtils.getLogger().info("LUMAGLASS_PANEL_COLOR_PASS {}",NAMES[stage]);
            }
        }
        @Override public void removed(){if(canvas!=null)canvas.close();}
    }

    private static final class CreativeTabsHost extends Screen implements GlassThemedScreen {
        private boolean checked;
        CreativeTabsHost(){super(Component.literal("Creative tab atlas regression"));}
        @Override public void render(GuiGraphics g,int mx,int my,float dt) {
            g.fillGradient(0,0,width,height,0xff808080,0xff808080);
            ResourceLocation atlas=new ResourceLocation("minecraft","textures/gui/container/creative_inventory/tabs.png");
            // Top inactive/active and bottom inactive/active without icons obscuring the faces.
            for(int i=0;i<4;i++)g.blit(atlas,30+70*i,55,0,i*32,26,32);
            g.flush();
            if(!checked)try(var image=Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
                double scale=minecraft.getWindow().getGuiScale();
                for(int i=0;i<4;i++)for(int y=65;y<=76;y++)for(int x=37+70*i;x<49+70*i;x++) {
                    int p=image.getPixelRGBA((int)(x*scale),(int)(y*scale));
                    if((p&255)<118)throw new AssertionError("Creative tab retains dark atlas face: "+i+" / "+Integer.toHexString(p));
                }
                checked=true;LogUtils.getLogger().info("LUMAGLASS_CREATIVE_TAB_FACE_PASS");
            }
        }
    }

    private static void checkContracts() {
        GlassConfig.VANILLA_UI.set(true);
        try {new GlassStyle(Float.NaN,1,1,1,1,1);throw new AssertionError("NaN accepted");}catch(IllegalArgumentException expected){}
        try {GlassStyle.CLEAR.withOpacity(2);throw new AssertionError("Invalid opacity accepted");}catch(IllegalArgumentException expected){}
        if(GlassStyle.CLEAR.withRadius(3).radius()!=3 || GlassStyle.CLEAR.radius()!=12)throw new AssertionError("Style mutated");
        GlassStyle red=GlassStyle.CLEAR.withGlowColor(0xff0000);
        if(red.withRadius(3).withOpacity(.5f).glowColor()!=0xff0000
                || GlassStyle.CLEAR.glowColor()!=GlassStyle.GLOBAL_GLOW_COLOR
                || red.withGlobalGlowColor().glowColor()!=GlassStyle.GLOBAL_GLOW_COLOR)
            throw new AssertionError("Glow style copy contract violated");
        for(int invalid:new int[]{-1,0x1000000,0xffaabbcc})
            try{red.withGlowColor(invalid);throw new AssertionError("Invalid RGB accepted");}catch(IllegalArgumentException expected){}
        GlassCanvas canvas=new GlassCanvas();canvas.close();canvas.close();
        LogUtils.getLogger().info("LUMAGLASS_API_CONTRACT_PASS");
    }

    private static final class ContainerHost extends Screen implements GlassThemedScreen {
        private ContainerScreen container;
        ContainerHost(){super(Component.literal("Container fixture"));}
        @Override protected void init() {
            Inventory inventory=new Inventory(null);
            ChestMenu menu=ChestMenu.threeRows(0,inventory);
            menu.slots.get(0).set(new ItemStack(Items.DIAMOND,12));
            menu.slots.get(1).set(new ItemStack(Items.APPLE,8));
            menu.slots.get(2).set(new ItemStack(Items.IRON_PICKAXE));
            container=new ContainerScreen(menu,inventory,Component.literal("Liquid glass chest"));
            container.init(minecraft,width,height);
        }
        @Override public void render(GuiGraphics g,int mx,int my,float dt) {
            container.render(g,container.getGuiLeft()+12,container.getGuiTop()+22,dt);
        }
    }

    private static final class WidgetHost extends Screen implements GlassThemedScreen {
        WidgetHost(){super(Component.literal("Widgets fixture"));}
        @Override protected void init() {
            addRenderableWidget(Button.builder(Component.literal("Native button"),b->{}).bounds(width/2-100,45,200,20).build());
            Button disabled=Button.builder(Component.literal("Disabled"),b->{}).bounds(width/2-100,70,200,20).build();
            disabled.active=false;addRenderableWidget(disabled);
            EditBox field=new EditBox(font,width/2-100,98,200,20,Component.literal("Text field"));
            field.setValue("Editable text / cursor / selection");addRenderableWidget(field);
            addRenderableWidget(new Checkbox(width/2-100,128,200,20,Component.literal("Selected checkbox"),true));
        }
        @Override public void render(GuiGraphics g,int mx,int my,float dt) {
            renderBackground(g);super.render(g,mx,my,dt);
            g.renderTooltip(font,Component.literal("Glass tooltip / original text"),width/2-80,160);
        }
    }

    private static final class HudHost extends Screen implements GlassThemedScreen {
        HudHost(){super(Component.literal("HUD render fixture"));}
        @Override public void render(GuiGraphics g,int mx,int my,float dt) {
            renderBackground(g);
            ResourceLocation widgets=new ResourceLocation("textures/gui/widgets.png");
            g.blit(widgets,width/2-91,height-30,0,0,182,22);
            g.blit(widgets,width/2-92+40,height-31,0,22,24,22);
            g.renderItem(new ItemStack(Items.DIAMOND_SWORD),width/2-88+40,height-27);
            g.fill(8,height-70,175,height-58,0x80000000);
            g.drawString(font,"Chat and scoreboard backplates",10,height-68,0xffffffff);
            g.blit(new ResourceLocation("textures/gui/bars.png"),width/2-91,24,0,0,182,5);
            g.drawCenteredString(font,"HUD texture fixture (not a world)",width/2,8,0xffffffff);
        }
    }
}
