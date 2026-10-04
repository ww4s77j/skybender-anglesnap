package com.skybender.anglesnap.client;

import com.skybender.anglesnap.DialLayout;
import com.skybender.anglesnap.DialProjection;
import com.skybender.anglesnap.PixelFont;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

/**
 * Draws the in-world input dial: one panel per encoder digit plus the master, fire and up switches,
 * each at the look direction the cannon decodes, with the input gauge around the crosshair.
 *
 * <p>Panels are projected by hand ({@link DialProjection}) and drawn as rectangles, so one renderer
 * serves every supported Minecraft version while still giving real parallax as the player turns.
 */
final class DialRenderer {
	private static final int GAUGE_SEGMENTS = 48;
	private static final float GAUGE_RADIUS = 13.0f;
	private static final float GAUGE_THICKNESS = 2.5f;
	private static final int GAUGE_EMPTY = 0x60303030;
	private static final int SELECTED_FRAME = 0xFFFFFFFF;
	private static final int MIN_PANEL_PIXELS = 6;
	private static final float RIBBON_CELL_WIDTH = 1.3f;
	private static final float RIBBON_ASPECT_RATIO = 11.0f;

	private DialRenderer() {
	}

	static void render(DialDraw draw) {
		if (!DialOptions.enabled()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}

		int width = client.getWindow().getGuiScaledWidth();
		int height = client.getWindow().getGuiScaledHeight();
		float fov = client.options.fov().get();
		float radius = DialOptions.radius();
		float cameraYaw = player.getYRot();
		float cameraPitch = player.getXRot();

		List<DialLayout.Panel> panels = DialLayout.panels(DialOptions.scale());
		DialLayout.Panel selected = nearest(panels, cameraYaw, cameraPitch);

		for (DialLayout.Panel panel : panels) {
			DialProjection.Projected projected = DialProjection.project(
				panel.yaw(), panel.pitch(), cameraYaw, cameraPitch, fov, width, height, radius, panel.size());
			if (projected.visible()) {
				drawPanel(draw, panel, projected, panel == selected);
			}
		}

		drawGauge(draw, width / 2, height / 2);
	}

	/** The panel the crosshair is closest to, which is the one the cannon would read. */
	static DialLayout.Panel nearest(List<DialLayout.Panel> panels, float yaw, float pitch) {
		DialLayout.Panel best = null;
		double bestDistance = Double.MAX_VALUE;
		for (DialLayout.Panel panel : panels) {
			double deltaYaw = DialProjection.wrapDegrees(panel.yaw() - yaw);
			double deltaPitch = panel.pitch() - pitch;
			double distance = deltaYaw * deltaYaw + deltaPitch * deltaPitch;
			if (distance < bestDistance) {
				bestDistance = distance;
				best = panel;
			}
		}
		return best;
	}

	private static void drawPanel(DialDraw draw, DialLayout.Panel panel, DialProjection.Projected projected, boolean selected) {
		boolean ribbonCell = panel.label().length() == 1;
		int width = Math.round(projected.size() * (ribbonCell ? RIBBON_CELL_WIDTH : 1.0f));
		int height = ribbonCell
			? Math.max(MIN_PANEL_PIXELS, Math.round(projected.size() * RIBBON_CELL_WIDTH / RIBBON_ASPECT_RATIO))
			: width;
		if (width < MIN_PANEL_PIXELS) {
			return;
		}

		int left = Math.round(projected.x()) - width / 2;
		int top = Math.round(projected.y()) - height / 2;
		int right = left + width;
		int bottom = top + height;

		draw.fillGradient(left, top, right, bottom,
			shade(panel.color(), projected.shade() * 1.15f),
			shade(panel.color(), projected.shade() * 0.7f));

		if (selected) {
			draw.fill(left - 1, top - 1, right + 1, top, SELECTED_FRAME);
			draw.fill(left - 1, bottom, right + 1, bottom + 1, SELECTED_FRAME);
			draw.fill(left - 1, top - 1, left, bottom + 1, SELECTED_FRAME);
			draw.fill(right, top - 1, right + 1, bottom + 1, SELECTED_FRAME);
		}

		drawLabel(draw, panel.label(), left, top, Math.min(width, height), DialLayout.TEXT_COLOR);
	}

	/** Draws a label as pixel-font rectangles, sized to stay inside its panel. */
	private static void drawLabel(DialDraw draw, String label, int panelLeft, int panelTop, int panelSize, int color) {
		int cells = PixelFont.textWidth(label);
		if (cells <= 0) {
			return;
		}

		int cell = Math.max(1, Math.min(panelSize / (PixelFont.HEIGHT + 1), panelSize / cells));
		int originX = panelLeft + (panelSize - cells * cell) / 2;
		int originY = panelTop + (panelSize - PixelFont.HEIGHT * cell) / 2;

		int penX = originX;
		for (int index = 0; index < label.length(); index++) {
			int[] rows = PixelFont.rows(label.charAt(index));
			if (rows != null) {
				for (int row = 0; row < rows.length; row++) {
					for (int column = 0; column < PixelFont.WIDTH; column++) {
						if ((rows[row] & (1 << (PixelFont.WIDTH - 1 - column))) == 0) {
							continue;
						}
						int pixelX = penX + column * cell;
						int pixelY = originY + row * cell;
						draw.fill(pixelX, pixelY, pixelX + cell, pixelY + cell, color);
					}
				}
			}
			penX += (PixelFont.WIDTH + 1) * cell;
		}
	}

	/** Input gauge around the crosshair: red, then yellow, then green once input is allowed again. */
	private static void drawGauge(DialDraw draw, int centerX, int centerY) {
		float progress = DialGauge.progress();
		int filledColor = DialProjection.inputGaugeColor(progress);
		int filled = Math.round(GAUGE_SEGMENTS * progress);

		for (int segment = 0; segment < GAUGE_SEGMENTS; segment++) {
			double angle = segment / (double) GAUGE_SEGMENTS * Math.PI * 2.0 - Math.PI / 2.0;
			float x = (float) (centerX + Math.cos(angle) * GAUGE_RADIUS);
			float y = (float) (centerY + Math.sin(angle) * GAUGE_RADIUS);
			int color = segment < filled ? filledColor : GAUGE_EMPTY;
			draw.fill(Math.round(x - GAUGE_THICKNESS), Math.round(y - GAUGE_THICKNESS),
				Math.round(x + GAUGE_THICKNESS), Math.round(y + GAUGE_THICKNESS), color);
		}
	}

	/** Multiplies a colour's brightness, keeping its alpha. */
	static int shade(int argb, float factor) {
		float scale = Math.max(0.0f, Math.min(1.0f, factor));
		return (DialProjection.alpha(argb) << 24)
			| (Math.round(DialProjection.red(argb) * scale) << 16)
			| (Math.round(DialProjection.green(argb) * scale) << 8)
			| Math.round(DialProjection.blue(argb) * scale);
	}
}
