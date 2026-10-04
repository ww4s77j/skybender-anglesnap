package com.skybender.anglesnap;

/**
 * Turns a dial panel's world direction into screen space.
 *
 * <p>Every panel sits on a sphere of {@code radius} blocks around the eye, so its screen position
 * follows from the angular difference between the panel direction and the camera direction plus a
 * plain perspective divide. Doing the projection by hand keeps the drawing itself to filled
 * rectangles, which every supported Minecraft version can render, and gives real parallax when the
 * player turns.
 *
 * <p>Pure model: no Minecraft types, so it is unit tested directly.
 */
public final class DialProjection {
	/** Vertical field of view is clamped into a range that still produces a usable dial. */
	public static final float MIN_FOV = 30.0f;
	/** Upper clamp for the vertical field of view. */
	public static final float MAX_FOV = 110.0f;
	/** Angular offset, in degrees, at which a panel is fully darkened. */
	private static final double SHADE_SPAN = 60.0;
	/** How much of the panel colour is lost at the maximum offset. */
	private static final double SHADE_DEPTH = 0.55;
	/** Colours of the crosshair input gauge, from "just input" to "ready again". */
	private static final int SLOT_RED = 0xFFCC3333;
	private static final int SLOT_YELLOW = 0xFFE8E833;
	private static final int SLOT_GREEN = 0xFF33CC33;

	/** A panel's screen rectangle. */
	public record Projected(float x, float y, float size, float shade, boolean visible) {
	}

	private DialProjection() {
	}

	/**
	 * Projects one panel.
	 *
	 * @param panelYaw    the panel's yaw
	 * @param panelPitch  the panel's pitch
	 * @param cameraYaw   the camera's yaw
	 * @param cameraPitch the camera's pitch
	 * @param fovDegrees  the game's current vertical field of view
	 * @param screenWidth GUI-scaled screen width
	 * @param screenHeight GUI-scaled screen height
	 * @param radius      dial radius in blocks
	 * @param panelSize   panel edge length in blocks
	 * @return the screen rectangle, with {@code visible} false when the panel is behind the camera
	 */
	public static Projected project(
		float panelYaw,
		float panelPitch,
		float cameraYaw,
		float cameraPitch,
		float fovDegrees,
		int screenWidth,
		int screenHeight,
		float radius,
		float panelSize
	) {
		float dYaw = wrapDegrees(panelYaw - cameraYaw);
		float dPitch = panelPitch - cameraPitch;

		double yawRadians = Math.toRadians(dYaw);
		double pitchRadians = Math.toRadians(dPitch);
		double right = Math.sin(yawRadians) * Math.cos(pitchRadians);
		double up = -Math.sin(pitchRadians);
		double forward = Math.cos(yawRadians) * Math.cos(pitchRadians);

		// The panel is on the far side of the sphere when it is not roughly in front of us.
		if (forward < 0.08 || radius <= 0.0f) {
			return new Projected(0.0f, 0.0f, 0.0f, 0.0f, false);
		}

		float fov = Math.max(MIN_FOV, Math.min(MAX_FOV, fovDegrees));
		double focal = (screenHeight / 2.0) / Math.tan(Math.toRadians(fov) / 2.0);
		double depth = forward * radius;

		float x = (float) (screenWidth / 2.0 + right * radius * focal / depth);
		float y = (float) (screenHeight / 2.0 - up * radius * focal / depth);
		float size = (float) (panelSize * focal / depth);
		return new Projected(x, y, size, shade(dYaw, dPitch), true);
	}

	/** Brightness multiplier for a panel, decreasing with its angular distance from the crosshair. */
	static float shade(float dYaw, float dPitch) {
		double offset = Math.min(1.0, Math.hypot(dYaw, dPitch) / SHADE_SPAN);
		return (float) (1.0 - SHADE_DEPTH * offset);
	}

	/**
	 * Colour of the crosshair input gauge.
	 *
	 * @param progress zero right after an input was taken, one when the next input is allowed
	 * @return red, then yellow, then green
	 */
	public static int inputGaugeColor(float progress) {
		float t = Math.max(0.0f, Math.min(1.0f, progress));
		if (t < 0.5f) {
			return blend(SLOT_RED, SLOT_YELLOW, t * 2.0f);
		}
		return blend(SLOT_YELLOW, SLOT_GREEN, (t - 0.5f) * 2.0f);
	}

	/** Wraps an angle into {@code [-180, 180)}. */
	public static float wrapDegrees(float degrees) {
		float wrapped = degrees % 360.0f;
		if (wrapped >= 180.0f) {
			wrapped -= 360.0f;
		} else if (wrapped < -180.0f) {
			wrapped += 360.0f;
		}
		return wrapped;
	}

	private static int blend(int from, int to, float t) {
		int a = Math.round(alpha(from) + (alpha(to) - alpha(from)) * t);
		int r = Math.round(red(from) + (red(to) - red(from)) * t);
		int g = Math.round(green(from) + (green(to) - green(from)) * t);
		int b = Math.round(blue(from) + (blue(to) - blue(from)) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	public static int alpha(int argb) {
		return (argb >>> 24) & 0xFF;
	}

	public static int red(int argb) {
		return (argb >>> 16) & 0xFF;
	}

	public static int green(int argb) {
		return (argb >>> 8) & 0xFF;
	}

	public static int blue(int argb) {
		return argb & 0xFF;
	}
}
