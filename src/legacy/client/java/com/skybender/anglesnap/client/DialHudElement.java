package com.skybender.anglesnap.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Dial HUD layer for the 1.21.11 API, where the element is handed a {@link GuiGraphics}.
 */
final class DialHudElement implements HudElement {
	@Override
	public void render(GuiGraphics context, DeltaTracker tickCounter) {
		DialRenderer.render(new GuiGraphicsDraw(context));
	}

	/** Adapts the 1.21.11 draw context to {@link DialDraw}. */
	private record GuiGraphicsDraw(GuiGraphics context) implements DialDraw {
		@Override
		public void fill(int x1, int y1, int x2, int y2, int argb) {
			context.fill(x1, y1, x2, y2, argb);
		}

		@Override
		public void fillGradient(int x1, int y1, int x2, int y2, int topArgb, int bottomArgb) {
			context.fillGradient(x1, y1, x2, y2, topArgb, bottomArgb);
		}

		@Override
		public void enableScissor(int x1, int y1, int x2, int y2) {
			context.enableScissor(x1, y1, x2, y2);
		}

		@Override
		public void disableScissor() {
			context.disableScissor();
		}
	}
}
