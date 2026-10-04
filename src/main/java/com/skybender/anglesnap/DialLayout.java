package com.skybender.anglesnap;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout of the in-world input dial: one panel per encoder digit, plus the master, fire and up
 * switches, each placed at the exact look direction the cannon decodes.
 *
 * <p>Because every panel sits at its own real yaw/pitch, looking at a panel means looking at the
 * direction that transmits that digit, which is what makes the dial usable for hand input as well
 * as a readout of an automatic sequence. The master switch is drawn at its first stage
 * ({@code yaw 90, pitch 32.5}); its second stage is "straight down facing west", which coincides
 * with the fire direction.
 *
 * <p>Pure model: no Minecraft types, so it is unit tested directly.
 */
public final class DialLayout {
	/** Distance of every panel from the eye, in blocks. */
	public static final float DEFAULT_RADIUS = 3.0f;
	/** Edge length of a digit panel, in blocks. Neighbours are only 22.5 degrees apart. */
	public static final float DEFAULT_DIGIT_SIZE = 0.9f;
	/** Switches are drawn smaller than the digit panels. */
	public static final float SWITCH_SCALE = 0.5f;
	/** Largest dial radius the settings will accept. */
	public static final float MAX_RADIUS = 8.0f;
	/** Smallest dial radius the settings will accept. */
	public static final float MIN_RADIUS = 1.5f;
	/** Largest multiplier applied to the digit panel size. */
	public static final float MAX_SCALE = 2.0f;
	/** Smallest multiplier applied to the digit panel size. */
	public static final float MIN_SCALE = 0.5f;

	/** Panel background colours, one per group of four digits. */
	private static final int[] QUADRANT_COLORS = {
		0xFF3F6212, 0xFF6B6B1A, 0xFF1F6B6B, 0xFF1E3A6E
	};
	private static final int MASTER_COLOR = 0xFF8A5A00;
	private static final int FIRE_COLOR = 0xFF8B1A1A;
	private static final int UP_COLOR = 0xFF3A3A3A;

	/** Fire is straight down in Minecraft's convention (pitch is positive when looking down). */
	public static final float FIRE_PITCH = 90.0f;
	/** The idle/blank pose the sequence returns to between digits. */
	public static final float UP_PITCH = -90.0f;

	public static final int TEXT_COLOR = 0xFFFFFFFF;

	private DialLayout() {
	}

	/** A panel of the dial, positioned by the look direction it occupies. */
	public record Panel(String label, float yaw, float pitch, float size, int color) {
	}

	/**
	 * Builds the dial for the given size multiplier.
	 *
	 * @param scale multiplier applied to the digit panel size
	 */
	public static List<Panel> panels(float scale) {
		float digitSize = DEFAULT_DIGIT_SIZE * clamp(scale, MIN_SCALE, MAX_SCALE);
		float switchSize = digitSize * SWITCH_SCALE;

		List<Panel> panels = new ArrayList<>(19);
		for (int digit = 0; digit < SkybendEncoder.ENCODER_ANGLES.length; digit++) {
			float[] angle = SkybendEncoder.ENCODER_ANGLES[digit];
			panels.add(new Panel(
				String.valueOf("0123456789ABCDEF".charAt(digit)),
				angle[0],
				angle[1],
				digitSize,
				QUADRANT_COLORS[(digit / 4) % QUADRANT_COLORS.length]
			));
		}

		float[] master = SkybendEncoder.ACTIVATE_ANGLES[0];
		panels.add(new Panel("MASTER", master[0], master[1], switchSize, MASTER_COLOR));
		panels.add(new Panel("FIRE", master[0], FIRE_PITCH, switchSize, FIRE_COLOR));
		panels.add(new Panel("UP", 0.0f, UP_PITCH, switchSize, UP_COLOR));
		return panels;
	}

	/** Clamps a dial radius into the supported range. */
	public static float clampRadius(float radius) {
		return clamp(radius, MIN_RADIUS, MAX_RADIUS);
	}

	/** Clamps a panel-size multiplier into the supported range. */
	public static float clampScale(float scale) {
		return clamp(scale, MIN_SCALE, MAX_SCALE);
	}

	private static float clamp(float value, float min, float max) {
		if (Float.isNaN(value)) {
			return min;
		}
		return Math.max(min, Math.min(max, value));
	}
}
