package com.skybender.anglesnap.client;

import com.skybender.anglesnap.SkybendSequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

/**
 * Plays a precomputed angle sequence on the server's tick clock.
 *
 * <p>The schedule is expressed in server ticks, so it is paced by {@link ServerTickClock} rather
 * than by the client's own tick counter: a server running at 10 TPS holds each angle twice as long,
 * which is exactly what the cannon expects.
 */
public final class SequencePlayer {
	private static final ServerTickClock CLOCK = new ServerTickClock();

	private static List<SkybendSequence.AngleStep> steps;
	private static int nextIndex;
	private static boolean active;

	private SequencePlayer() {
	}

	public static void start(List<SkybendSequence.AngleStep> sequence) {
		steps = List.copyOf(sequence);
		CLOCK.start();
		nextIndex = 0;
		active = !steps.isEmpty();
		if (active) {
			applyDue(Minecraft.getInstance());
		}
	}

	public static void cancel() {
		active = false;
		steps = null;
		nextIndex = 0;
		CLOCK.stop();
	}

	public static boolean isActive() {
		return active;
	}

	public static void onClientTick(Minecraft client) {
		if (!active) {
			return;
		}
		applyDue(client);
	}

	private static void applyDue(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || steps == null) {
			cancel();
			return;
		}

		double elapsed = CLOCK.ticks();
		while (nextIndex < steps.size()) {
			SkybendSequence.AngleStep step = steps.get(nextIndex);
			if (step.tick() > elapsed) {
				break;
			}
			snap(player, step.yaw(), step.pitch());
			nextIndex++;
		}

		if (nextIndex >= steps.size()) {
			cancel();
		}
	}

	private static void snap(LocalPlayer player, float yaw, float pitch) {
		player.setYRot(yaw);
		player.setXRot(pitch);
		player.yRotO = yaw;
		player.xRotO = pitch;
	}
}
