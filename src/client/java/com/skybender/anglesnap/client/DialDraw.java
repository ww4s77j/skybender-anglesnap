package com.skybender.anglesnap.client;

/**
 * The handful of drawing calls the dial needs.
 *
 * <p>Both supported HUD API generations offer the same calls ({@code fill}, {@code fillGradient},
 * {@code enableScissor}, {@code disableScissor}) but pass them a different context type, so this
 * interface is the only thing that has to exist once per generation. Coordinates are GUI-scaled
 * pixels and colours are {@code 0xAARRGGBB}.
 */
interface DialDraw {
	void fill(int x1, int y1, int x2, int y2, int argb);

	void fillGradient(int x1, int y1, int x2, int y2, int topArgb, int bottomArgb);

	void enableScissor(int x1, int y1, int x2, int y2);

	void disableScissor();
}
