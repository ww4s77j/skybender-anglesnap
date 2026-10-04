package com.skybender.anglesnap.client;

import com.skybender.anglesnap.DialLayout;

/**
 * In-memory settings for the dial overlay, changed with {@code /skybend overlay}.
 *
 * <p>The dial is hidden by default and can be shown with {@code /skybend overlay on}. Display
 * settings are persisted separately from {@link SkybendSettings}, which describes the payload.
 */
final class DialOptions {
	private static boolean enabled;
	private static float radius = DialLayout.DEFAULT_RADIUS;
	private static float scale = 1.0f;

	private DialOptions() {
	}

	static boolean enabled() {
		return enabled;
	}

	static void setEnabled(boolean value) {
		enabled = value;
		SkybendConfig.save();
	}

	static float radius() {
		return radius;
	}

	static void setRadius(float value) {
		radius = DialLayout.clampRadius(value);
		SkybendConfig.save();
	}

	static float scale() {
		return scale;
	}

	static void setScale(float value) {
		scale = DialLayout.clampScale(value);
		SkybendConfig.save();
	}

	static void reset() {
		enabled = false;
		radius = DialLayout.DEFAULT_RADIUS;
		scale = 1.0f;
	}
}
