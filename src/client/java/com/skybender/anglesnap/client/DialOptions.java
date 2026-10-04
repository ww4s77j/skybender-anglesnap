package com.skybender.anglesnap.client;

import com.skybender.anglesnap.DialLayout;

/**
 * In-memory settings for the dial overlay, changed with {@code /skybend overlay}.
 *
 * <p>The dial is drawn by default so it is there without any setup; {@code /skybend overlay off}
 * hides it again. Like every other setting in this mod these live for the session only, and are
 * deliberately kept out of {@link SkybendSettings}, which describes the payload rather than the
 * display.
 */
final class DialOptions {
	private static boolean enabled = true;
	private static float radius = DialLayout.DEFAULT_RADIUS;
	private static float scale = 1.0f;

	private DialOptions() {
	}

	static boolean enabled() {
		return enabled;
	}

	static void setEnabled(boolean value) {
		enabled = value;
	}

	static float radius() {
		return radius;
	}

	static void setRadius(float value) {
		radius = DialLayout.clampRadius(value);
	}

	static float scale() {
		return scale;
	}

	static void setScale(float value) {
		scale = DialLayout.clampScale(value);
	}
}
