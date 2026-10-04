package com.skybender.anglesnap.client;

import com.skybender.anglesnap.SkybendTpsModel;
import com.skybender.anglesnap.SkybenderAnglesnap;

/**
 * Client-side view of the server tick rate the cannon actually runs on.
 *
 * <p>Server time packets are forwarded here by the packet hook; everything else in the mod paces
 * against {@link #tps()} so a lagging server stretches the input sequence and the ETA instead of
 * silently desynchronising them. A manual rate can be pinned with {@code /skybend tps <number>}
 * and released with {@code /skybend tps auto}.
 */
public final class ServerTps {
	/** A server that has sent no time packet for this long is treated as stalled. */
	public static final long STALL_MILLIS = 1500L;

	private static final SkybendTpsModel MODEL = new SkybendTpsModel();
	private static boolean hookReported;

	private ServerTps() {
	}

	/** Called for every {@code ClientboundSetTimePacket}; {@code gameTime} is the server's own tick count. */
	public static void onTimePacket(long gameTime) {
		MODEL.onTimePacket(gameTime, System.currentTimeMillis());
		if (!hookReported) {
			hookReported = true;
			// Proof that the packet hook loaded on this Minecraft version: if this line is missing
			// from the log, the tick rate will stay at the 20 TPS default.
			SkybenderAnglesnap.LOGGER.info("Server time hook active — measuring the server tick rate");
		}
	}

	/** Drops measurement history, e.g. after changing worlds. A manual override is kept. */
	public static void onWorldChanged() {
		MODEL.reset();
		hookReported = false;
	}

	public static double tps() {
		return MODEL.tps();
	}

	public static double measuredTps() {
		return MODEL.measuredTps();
	}

	/** Real time one server tick currently takes, in milliseconds. */
	public static double tickMillis() {
		return MODEL.tickMillis();
	}

	/** Real time one server tick currently takes, in seconds. */
	public static double tickSeconds() {
		return MODEL.tickMillis() / 1000.0;
	}

	public static boolean isOverridden() {
		return MODEL.isOverridden();
	}

	public static void setOverride(double tps) {
		MODEL.setOverride(tps);
		SkybendConfig.save();
	}

	public static void clearOverride() {
		MODEL.clearOverride();
		SkybendConfig.save();
	}

	public static int sampleCount() {
		return MODEL.sampleCount();
	}

	public static boolean hasHeardFromServer() {
		return MODEL.hasHeardFromServer();
	}

	public static long millisSinceLastPacket() {
		return MODEL.millisSinceLastPacket(System.currentTimeMillis());
	}

	/**
	 * True while the server has stopped ticking. The cannon's Scarpet clock is frozen at the same
	 * time, so the sequence and the countdown must stop advancing rather than run ahead of it.
	 */
	public static boolean shouldFreezeClock() {
		if (!MODEL.hasHeardFromServer()) {
			return false;
		}
		return millisSinceLastPacket() > STALL_MILLIS;
	}

	/** Snapshot for command feedback. */
	public static Status status() {
		return new Status(
			MODEL.tps(),
			MODEL.measuredTps(),
			MODEL.isOverridden(),
			MODEL.sampleCount(),
			MODEL.tickMillis(),
			MODEL.millisSinceLastPacket(System.currentTimeMillis()),
			MODEL.hasHeardFromServer()
		);
	}

	public record Status(
		double tps,
		double measuredTps,
		boolean overridden,
		int samples,
		double tickMillis,
		long millisSinceLastPacket,
		boolean heardFromServer
	) {
	}
}
