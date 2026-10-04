package com.skybender.anglesnap.client;

import com.skybender.anglesnap.SkybenderTiming;

/**
 * Free-running clock behind the crosshair gauge.
 *
 * <p>It measures how much of the current input slot has elapsed. The slot is one digit period of
 * the cannon's own schedule (the data phase plus its strobe), converted from server ticks to real
 * time with the measured tick rate, so the gauge reads "wait" longer on a lagging server instead of
 * telling the player to input before the cannon is ready.
 */
final class DialGauge {
	private static final ServerTickClock CLOCK = new ServerTickClock();
	private static boolean started;

	private DialGauge() {
	}

	/** Fraction of the current input slot that has elapsed; zero right after an input. */
	static float progress() {
		if (!started) {
			reset();
		}
		double slot = SkybenderTiming.INPUT_DIGIT_PERIOD;
		if (slot <= 0.0) {
			return 1.0f;
		}
		double elapsed = CLOCK.ticks() % slot;
		return (float) (elapsed / slot);
	}

	/** Restarts the gauge, e.g. when a new world is joined. */
	static void reset() {
		CLOCK.start();
		started = true;
	}
}
