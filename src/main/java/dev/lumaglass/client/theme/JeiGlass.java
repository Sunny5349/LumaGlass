package dev.lumaglass.client.theme;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.lumaglass.GlassConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Optional JEI 15 adapter. Only known furniture is replaced; recipe artwork stays native. */
public final class JeiGlass {
    private JeiGlass() {}

    public static boolean draw(GuiGraphics g,ResourceLocation location,int x,int y,int w,int h) {
        if (!VanillaGlass.active() || !GlassConfig.OTHER_MOD_UI.get() || !location.getNamespace().equals("jei")) return false;
        String path=location.getPath();
        if (path.equals("scrollbar_background") || path.equals("scrollbar_marker"))
            return VanillaGlass.scrollbar(g,x,y,w,h,path.equals("scrollbar_background"),false);
        boolean known=switch(path) {
            case "gui_background", "single_recipe_background", "recipe_preview_background",
                    "ingredient_list_background", "bookmark_list_background", "search_background",
                    "slot", "output_slot", "ingredient_list_slot_background", "bookmark_list_slot_background",
                    "recipe_catalyst_slot_background", "catalyst_tab", "recipe_options_tab",
                    "tab_selected", "tab_unselected", "button_enabled", "button_disabled",
                    "button_highlight", "button_pressed", "button_pressed_highlight" -> true;
            default -> false;
        };
        return known && VanillaGlass.panel(g,x,y,w,h,Math.min(8,Math.min(w,h)*.22f),
                path.equals("tab_selected") || path.contains("highlight"),
                RenderSystem.getShaderColor()[3]*(path.equals("button_disabled")?.5f:1));
    }
}
