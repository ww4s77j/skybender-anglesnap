package com.skybender.anglesnap.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Action-bar ETA countdown (hh:mm:ss), refreshed once per second.
 *
 * <p>Remaining time is converted from server ticks using the measured server tick rate, so the
 * countdown tracks the cannon's own clock instead of assuming 20 ticks per second.
 */
public final class EtaCountdown {
	private static final ServerTickClock CLOCK = new ServerTickClock();

	private static boolean active;
	private static int totalTicks;
	private static int lastDisplayedSeconds = -1;

	private EtaCountdown() {
	}

	public static void start(int totalTicks) {
		// Paced on the server clock rather than the client tick counter.
		EtaCountdown.totalTicks = Math.max(0, totalTicks);
		CLOCK.start();
		active = true;
		lastDisplayedSeconds = -1;
		onClientTick(Minecraft.getInstance());
	}

	public static void cancel() {
		if (active) {
			active = false;
			lastDisplayedSeconds = -1;
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null) {
				OverlayMessage.show(player, Component.empty());
			}
		}
	}

	public static boolean isActive() {
		return active;
	}

	public static void onClientTick(Minecraft client) {
		if (!active) {
			return;
		}
		LocalPlayer player = client.player;
		if (player == null) {
			cancel();
			return;
		}

		double elapsedTicks = CLOCK.ticks();
		int remaining = (int) Math.ceil(totalTicks - elapsedTicks);
		if (remaining <= 0) {
			if (lastDisplayedSeconds != 0) {
				OverlayMessage.show(player, Component.literal("ETA 00:00:00"));
				lastDisplayedSeconds = 0;
			}
			active = false;
			return;
		}

		int seconds = secondsFor(remaining);
		if (seconds != lastDisplayedSeconds) {
			lastDisplayedSeconds = seconds;
			OverlayMessage.show(player, Component.literal("ETA " + formatHms(seconds)));
		}
	}

	/** Wall-clock seconds the given number of server ticks currently takes. */
	static int secondsFor(int ticks) {
		double seconds = Math.max(0, ticks) / ServerTps.tps();
		return (int) Math.ceil(seconds - 1.0e-9);
	}

	/** hh:mm:ss for the given number of server ticks at the current server tick rate. */
	static String formatTicks(int ticks) {
		return formatHms(secondsFor(ticks));
	}

	static String formatHms(int totalSeconds) {
		int h = totalSeconds / 3600;
		int m = (totalSeconds % 3600) / 60;
		int s = totalSeconds % 60;
		return String.format("%02d:%02d:%02d", h, m, s);
	}
}
