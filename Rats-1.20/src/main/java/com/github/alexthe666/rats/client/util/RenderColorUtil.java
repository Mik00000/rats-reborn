package com.github.alexthe666.rats.client.util;

/**
 * Packs color components into ARGB for 1.21.1 model rendering.
 */
public final class RenderColorUtil {

	private RenderColorUtil() {
	}

	// Packs float RGBA (0..1) into an ARGB int for renderToBuffer/render.
	public static int packRgba(float red, float green, float blue, float alpha) {
		int r = (int) (red * 255.0F);
		int g = (int) (green * 255.0F);
		int b = (int) (blue * 255.0F);
		int a = (int) (alpha * 255.0F);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	// Packs float RGB (0..1) with opaque alpha.
	public static int packRgb(float red, float green, float blue) {
		return packRgba(red, green, blue, 1.0F);
	}

	// Packs float[] RGB (0..1) with the given alpha.
	public static int packRgb(float[] rgb, float alpha) {
		return packRgba(rgb[0], rgb[1], rgb[2], alpha);
	}

	// Packs float[] RGB (0..1) with opaque alpha.
	public static int packRgb(float[] rgb) {
		return packRgb(rgb, 1.0F);
	}
}
