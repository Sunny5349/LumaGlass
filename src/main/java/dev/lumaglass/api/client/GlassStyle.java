package dev.lumaglass.api.client;

/** Immutable material, measured in local GUI units. All values must be finite. */
public record GlassStyle(float radius, float refraction, float frost, float tint,
                         float highlight, float opacity, int glowColor) {
    /** Inherit the user's current palette color. Explicit colors are 24-bit RGB without alpha. */
    public static final int GLOBAL_GLOW_COLOR = -1;
    public static final GlassStyle CLEAR = new GlassStyle(12, 8, .25f, .08f, 1, 1);
    public static final GlassStyle FROSTED = new GlassStyle(12, 8, .8f, .18f, 1, 1);

    /** Original API v1 constructor; continues to inherit the global glow palette. */
    public GlassStyle(float radius, float refraction, float frost, float tint, float highlight, float opacity) {
        this(radius, refraction, frost, tint, highlight, opacity, GLOBAL_GLOW_COLOR);
    }

    public GlassStyle {
        range("radius", radius, 0, 256);
        range("refraction", refraction, 0, 64);
        range("frost", frost, 0, 1);
        range("tint", tint, 0, 1);
        range("highlight", highlight, 0, 4);
        range("opacity", opacity, 0, 1);
        if (glowColor < GLOBAL_GLOW_COLOR || glowColor > 0xffffff)
            throw new IllegalArgumentException("glowColor must be GLOBAL_GLOW_COLOR or 24-bit RGB");
    }

    private static void range(String name, float value, float min, float max) {
        if (!Float.isFinite(value) || value < min || value > max)
            throw new IllegalArgumentException(name + " must be finite and within [" + min + ", " + max + "]");
    }

    public GlassStyle withRadius(float value) { return new GlassStyle(value, refraction, frost, tint, highlight, opacity, glowColor); }
    public GlassStyle withOpacity(float value) { return new GlassStyle(radius, refraction, frost, tint, highlight, value, glowColor); }

    /** Override only this material's glow color. Global glow enable/strength still apply. */
    public GlassStyle withGlowColor(int rgb) {
        if (rgb < 0 || rgb > 0xffffff) throw new IllegalArgumentException("Expected 0xRRGGBB without alpha");
        return new GlassStyle(radius, refraction, frost, tint, highlight, opacity, rgb);
    }

    /** Return a material that follows the user's palette again. */
    public GlassStyle withGlobalGlowColor() {
        return new GlassStyle(radius, refraction, frost, tint, highlight, opacity, GLOBAL_GLOW_COLOR);
    }
}
