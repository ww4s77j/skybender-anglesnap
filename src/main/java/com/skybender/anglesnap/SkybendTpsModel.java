package com.skybender.anglesnap;

/**
 * Rolling server tick-rate estimate built from authoritative game-time samples.
 *
 * <p>Samples are the {@code gameTime} values carried by {@code ClientboundSetTimePacket}, which a
 * vanilla server broadcasts once every 20 of its own ticks. Dividing the ticks observed by the
 * wall-clock time they took yields the server's real tick rate, which is the clock every Scarpet
 * cannon routine runs on. The estimate is taken over a rolling window, so it tracks sustained load
 * changes instead of individual jittery packets.
 *
 * <p>A manual override can be pinned with the in-game {@code /skybend tps <number>} command and
 * released again with {@code /skybend tps auto}.
 *
 * <p>Pure model: no Minecraft types, so it is unit tested directly.
 */
public final class SkybendTpsModel {
	/** Assumed rate before enough samples exist, and while a server never sends time packets. */
	public static final double DEFAULT_TPS = 20.0;
	/** Lower clamp; guards against infinite step spacing and division by zero. */
	public static final double MIN_TPS = 1.0;
	/** Upper clamp; a healthy server broadcasts a sample per second, so this is only a sanity bound. */
	public static final double MAX_TPS = 100.0;
	/** Samples kept in the rolling window (a vanilla server produces one per second). */
	public static final int WINDOW = 20;
	/** Estimates stay at {@link #DEFAULT_TPS} until this much time has been observed. */
	public static final long WARMUP_MILLIS = 4000L;

	private final long[] tickDeltas = new long[WINDOW];
	private final long[] wallDeltas = new long[WINDOW];
	private int nextIndex;
	private int samples;
	private long firstPacketWallMillis = -1L;
	private long lastGameTime = Long.MIN_VALUE;
	private long lastWallMillis = -1L;
	private Double override;

	/**
	 * Feeds one server time packet into the window.
	 *
	 * @param gameTime    the authoritative game time carried by the packet
	 * @param wallMillis  local wall-clock time the packet was handled, in milliseconds
	 */
	public void onTimePacket(long gameTime, long wallMillis) {
		if (firstPacketWallMillis < 0L) {
			firstPacketWallMillis = wallMillis;
			lastGameTime = gameTime;
			lastWallMillis = wallMillis;
			return;
		}

		long tickDelta = gameTime - lastGameTime;
		long wallDelta = wallMillis - lastWallMillis;
		lastGameTime = gameTime;
		lastWallMillis = wallMillis;

		// Duplicated, replayed or reordered packets carry no rate information.
		if (tickDelta <= 0L || wallDelta <= 0L) {
			return;
		}

		tickDeltas[nextIndex] = tickDelta;
		wallDeltas[nextIndex] = wallDelta;
		nextIndex = (nextIndex + 1) % WINDOW;
		if (samples < WINDOW) {
			samples++;
		}
	}

	/** Measured server tick rate; {@link #DEFAULT_TPS} until {@link #WARMUP_MILLIS} of history exists. */
	public double measuredTps() {
		if (samples == 0 || firstPacketWallMillis < 0L) {
			return DEFAULT_TPS;
		}
		if (lastWallMillis - firstPacketWallMillis < WARMUP_MILLIS) {
			return DEFAULT_TPS;
		}

		long ticks = 0L;
		long millis = 0L;
		for (int i = 0; i < samples; i++) {
			ticks += tickDeltas[i];
			millis += wallDeltas[i];
		}
		if (ticks <= 0L || millis <= 0L) {
			return DEFAULT_TPS;
		}
		return clamp(ticks * 1000.0 / millis);
	}

	/** The rate everything else should pace against: the manual override when set, else the measurement. */
	public double tps() {
		return override != null ? clamp(override) : measuredTps();
	}

	/** Real time one server tick currently takes, in milliseconds. */
	public double tickMillis() {
		return 1000.0 / tps();
	}

	public boolean isOverridden() {
		return override != null;
	}

	public double overrideValue() {
		return override != null ? override : Double.NaN;
	}

	public void setOverride(double value) {
		override = clamp(value);
	}

	public void clearOverride() {
		override = null;
	}

	public int sampleCount() {
		return samples;
	}

	/** True once at least one server time packet has been seen for the current world. */
	public boolean hasHeardFromServer() {
		return firstPacketWallMillis >= 0L;
	}

	/**
	 * Milliseconds since the last server time packet, or {@code -1} when none has been seen.
	 * Used to notice a stalled server, whose Scarpet clock is stalled too.
	 */
	public long millisSinceLastPacket(long nowMillis) {
		if (lastWallMillis < 0L) {
			return -1L;
		}
		return Math.max(0L, nowMillis - lastWallMillis);
	}

	/** Drops all samples, e.g. after changing worlds. A manual override is deliberately kept. */
	public void reset() {
		for (int i = 0; i < WINDOW; i++) {
			tickDeltas[i] = 0L;
			wallDeltas[i] = 0L;
		}
		nextIndex = 0;
		samples = 0;
		firstPacketWallMillis = -1L;
		lastGameTime = Long.MIN_VALUE;
		lastWallMillis = -1L;
	}

	private static double clamp(double value) {
		if (Double.isNaN(value)) {
			return DEFAULT_TPS;
		}
		return Math.min(MAX_TPS, Math.max(MIN_TPS, value));
	}
}
