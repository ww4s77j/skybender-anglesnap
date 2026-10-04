package com.skybender.anglesnap;

/**
 * Virtual server-tick clock.
 *
 * <p>The angle schedule is expressed in server ticks (the clock the Scarpet cannon runs on), but it
 * has to be played back on the client's frame loop. This clock converts real elapsed time into
 * server ticks using the currently measured tick rate, so a server running below 20 TPS stretches
 * the sequence instead of desynchronising it, and a stalled server freezes it.
 *
 * <p>Pure model: no Minecraft types, so it is unit tested directly.
 */
public final class SkybendServerClock {
	private double ticks;

	/** Restarts the clock at zero ticks. */
	public void reset() {
		ticks = 0.0;
	}

	/** Server ticks that have accumulated so far. */
	public double ticks() {
		return ticks;
	}

	/**
	 * Advances the clock.
	 *
	 * @param elapsedSeconds real seconds since the previous call
	 * @param tps            current server tick rate
	 * @param serverStalled  true while the server is not ticking at all
	 */
	public void advance(double elapsedSeconds, double tps, boolean serverStalled) {
		if (serverStalled || elapsedSeconds <= 0.0 || !Double.isFinite(elapsedSeconds)) {
			return;
		}
		ticks += elapsedSeconds * tps;
	}
}
