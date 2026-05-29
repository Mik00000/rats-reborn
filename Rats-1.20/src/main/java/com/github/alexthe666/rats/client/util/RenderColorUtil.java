package com.github.alexthe666.rats.client.util;

/**
 * Упаковка компонентов цвета в ARGB для рендера моделей 1.21.1.
 */
public final class RenderColorUtil {

	private RenderColorUtil() {
	}

	// Упаковывает float RGBA (0..1) в int ARGB для renderToBuffer/render.
	public static int packRgba(float red, float green, float blue, float alpha) {
		int r = (int) (red * 255.0F);
		int g = (int) (green * 255.0F);
		int b = (int) (blue * 255.0F);
		int a = (int) (alpha * 255.0F);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	// Упаковывает float RGB (0..1) с непрозрачной альфой.
	public static int packRgb(float red, float green, float blue) {
		return packRgba(red, green, blue, 1.0F);
	}

	// Упаковывает float[] RGB (0..1) с заданной альфой.
	public static int packRgb(float[] rgb, float alpha) {
		return packRgba(rgb[0], rgb[1], rgb[2], alpha);
	}

	// Упаковывает float[] RGB (0..1) с непрозрачной альфой.
	public static int packRgb(float[] rgb) {
		return packRgb(rgb, 1.0F);
	}
}
