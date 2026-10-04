package com.skybender.anglesnap.client;

import com.skybender.anglesnap.SkybendServerClock;

/**
 * Client-side driver for a {@link SkybendServerClock}.
 *
 * <p>Reads are what advance the clock: they measure the real time since the previous read and add
 * that many server ticks, using the currently measured tick rate. While the server is stalled
 * nothing is added, so the sequence and the countdown wait for it rather than running ahead.
 */
final class ServerTickClock {
	private final SkybendServerClock clock = new SkybendServerClock();
	private long lastNanos;

	/** Server ticks accumulated since the last {@link #start()}. */
	double ticks() {
		advance();
		return clock.ticks();
	}

	/** Restarts the clock. */
	void start() {
		clock.reset();
		lastNanos = System.nanoTime();
	}

	/** Stops the clock and drops its progress. */
	void stop() {
		clock.reset();
		lastNanos = 0L;
	}

	private void advance() {
		long now = System.nanoTime();
		long previous = lastNanos;
		lastNanos = now;
		if (previous == 0L) {
			return;
		}
		double elapsedSeconds = (now - previous) / 1_000_000_000.0;
		clock.advance(elapsedSeconds, ServerTps.tps(), ServerTps.shouldFreezeClock());
	}
}
