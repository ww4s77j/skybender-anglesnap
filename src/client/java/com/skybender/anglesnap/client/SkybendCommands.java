package com.skybender.anglesnap.client;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.skybender.anglesnap.DialLayout;
import com.skybender.anglesnap.SkybendEncoder;
import com.skybender.anglesnap.SkybendSequence;
import com.skybender.anglesnap.SkybendTpsModel;
import com.skybender.anglesnap.SkybenderTiming;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Locale;

public final class SkybendCommands {
	/** Long look-ray for targeting (vanilla crosshair is only interaction reach). */
	private static final double LOOK_TARGET_RANGE = 512.0;

	private SkybendCommands() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			LiteralArgumentBuilder<FabricClientCommandSource> root = literal("skybend")
				.then(literal("set")
					.then(literal("size")
						.then(argument("size", IntegerArgumentType.integer(1, 15))
							.executes(SkybendCommands::setSize)))
					.then(literal("origin")
						.then(argument("originX", IntegerArgumentType.integer())
							.then(argument("originZ", IntegerArgumentType.integer())
								.executes(SkybendCommands::setOrigin)))))
				.then(literal("overlay")
					.executes(SkybendCommands::overlayStatus)
					.then(literal("on")
						.executes(ctx -> setOverlay(ctx, true)))
					.then(literal("off")
						.executes(ctx -> setOverlay(ctx, false)))
					.then(literal("radius")
						.then(argument("blocks", DoubleArgumentType.doubleArg(DialLayout.MIN_RADIUS, DialLayout.MAX_RADIUS))
							.executes(SkybendCommands::overlayRadius)))
					.then(literal("scale")
						.then(argument("factor", DoubleArgumentType.doubleArg(DialLayout.MIN_SCALE, DialLayout.MAX_SCALE))
							.executes(SkybendCommands::overlayScale))))
				.then(literal("tps")
					.executes(SkybendCommands::tpsStatus)
					.then(literal("auto")
						.executes(SkybendCommands::tpsAuto))
					.then(argument("rate", DoubleArgumentType.doubleArg(SkybendTpsModel.MIN_TPS, SkybendTpsModel.MAX_TPS))
						.executes(SkybendCommands::tpsSet)))
				.then(literal("time")
					.executes(ctx -> timeOrFire(ctx, false, true))
					.then(argument("tx", IntegerArgumentType.integer())
						.then(argument("tz", IntegerArgumentType.integer())
							.executes(ctx -> timeOrFire(ctx, false, false)))))
				.then(literal("fire")
					.executes(ctx -> timeOrFire(ctx, true, true))
					.then(argument("tx", IntegerArgumentType.integer())
						.then(argument("tz", IntegerArgumentType.integer())
							.executes(ctx -> timeOrFire(ctx, true, false)))));

			dispatcher.register(root);
		});
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
		return LiteralArgumentBuilder.literal(name);
	}

	private static <T> RequiredArgumentBuilder<FabricClientCommandSource, T> argument(String name, ArgumentType<T> type) {
		return RequiredArgumentBuilder.argument(name, type);
	}

	private static int setSize(CommandContext<FabricClientCommandSource> ctx) {
		int size = IntegerArgumentType.getInteger(ctx, "size");
		SkybendSettings.setSize(size);
		ctx.getSource().sendFeedback(Component.literal("skybend set size " + size));
		return 1;
	}

	private static int setOrigin(CommandContext<FabricClientCommandSource> ctx) {
		int originX = IntegerArgumentType.getInteger(ctx, "originX");
		int originZ = IntegerArgumentType.getInteger(ctx, "originZ");
		SkybendSettings.setOrigin(originX, originZ);
		ctx.getSource().sendFeedback(Component.literal(
			String.format("skybend set origin %d %d", originX, originZ)
		));
		return 1;
	}

	/**
	 * @param fire       true = run sequence + countdown; false = time only
	 * @param useLookRay true = resolve target from look ray; false = use tx/tz args
	 */
	private static int timeOrFire(CommandContext<FabricClientCommandSource> ctx, boolean fire, boolean useLookRay) {
		if (!SkybendSettings.isSet()) {
			String missing = !SkybendSettings.hasSize() && !SkybendSettings.hasOrigin()
				? "size and origin"
				: SkybendSettings.hasSize() ? "origin" : "size";
			ctx.getSource().sendError(Component.literal(
				"No " + missing + " configured — use /skybend set size <1-15> and /skybend set origin <originX> <originZ>"));
			return 0;
		}

		int tx;
		int tz;
		if (useLookRay) {
			int[] look = resolveLookTarget(ctx);
			if (look == null) {
				return 0;
			}
			tx = look[0];
			tz = look[1];
		} else {
			tx = IntegerArgumentType.getInteger(ctx, "tx");
			tz = IntegerArgumentType.getInteger(ctx, "tz");
		}

		int n = SkybendSettings.nukeSize();
		int ox = SkybendSettings.originX();
		int oz = SkybendSettings.originZ();

		SkybendEncoder.EncodeResult encoded = SkybendEncoder.encode(n, ox, oz, tx, tz);
		int eta = SkybenderTiming.predictTime(n, encoded.machineX(), encoded.machineZ());

		ctx.getSource().sendFeedback(Component.literal(
			"Time Estimate: " + EtaCountdown.formatTicks(eta)
		));

		if (fire && !ServerTps.isOverridden() && ServerTps.hasHeardFromServer() && ServerTps.tps() < 19.0) {
			ctx.getSource().sendFeedback(Component.literal(String.format(Locale.ROOT,
				"Server is running at %.2f TPS — the sequence is paced to match", ServerTps.tps())));
		}

		if (fire) {
			SequencePlayer.start(SkybendSequence.build(encoded.hexDigits()));
			EtaCountdown.start(eta);
		}
		return 1;
	}

	private static int overlayStatus(CommandContext<FabricClientCommandSource> ctx) {
		ctx.getSource().sendFeedback(Component.literal(String.format(Locale.ROOT,
			"Dial overlay is %s — radius %.2f, scale %.2f (/skybend overlay on|off|radius|scale)",
			DialOptions.enabled() ? "on" : "off", DialOptions.radius(), DialOptions.scale())));
		return 1;
	}

	private static int setOverlay(CommandContext<FabricClientCommandSource> ctx, boolean enabled) {
		DialOptions.setEnabled(enabled);
		ctx.getSource().sendFeedback(Component.literal("skybend overlay " + (enabled ? "on" : "off")));
		return 1;
	}

	private static int overlayRadius(CommandContext<FabricClientCommandSource> ctx) {
		DialOptions.setRadius((float) DoubleArgumentType.getDouble(ctx, "blocks"));
		ctx.getSource().sendFeedback(Component.literal(
			String.format(Locale.ROOT, "skybend overlay radius %.2f", DialOptions.radius())));
		return 1;
	}

	private static int overlayScale(CommandContext<FabricClientCommandSource> ctx) {
		DialOptions.setScale((float) DoubleArgumentType.getDouble(ctx, "factor"));
		ctx.getSource().sendFeedback(Component.literal(
			String.format(Locale.ROOT, "skybend overlay scale %.2f", DialOptions.scale())));
		return 1;
	}

	private static int tpsStatus(CommandContext<FabricClientCommandSource> ctx) {
		ServerTps.Status status = ServerTps.status();
		String text;
		if (!status.heardFromServer()) {
			text = String.format(Locale.ROOT, "TPS %.2f (no server time packets yet — using the default)", status.tps());
		} else if (status.overridden()) {
			text = String.format(Locale.ROOT, "TPS %.2f (manual override) — measured %.2f, tick %.1f ms",
				status.tps(), status.measuredTps(), status.tickMillis());
		} else {
			text = String.format(Locale.ROOT, "TPS %.2f (measured) — %d samples, tick %.1f ms",
				status.tps(), status.samples(), status.tickMillis());
		}
		ctx.getSource().sendFeedback(Component.literal(text));
		return 1;
	}

	private static int tpsAuto(CommandContext<FabricClientCommandSource> ctx) {
		ServerTps.clearOverride();
		ctx.getSource().sendFeedback(Component.literal(
			String.format(Locale.ROOT, "skybend tps auto (measured %.2f)", ServerTps.tps())
		));
		return 1;
	}

	private static int tpsSet(CommandContext<FabricClientCommandSource> ctx) {
		double rate = DoubleArgumentType.getDouble(ctx, "rate");
		ServerTps.setOverride(rate);
		ctx.getSource().sendFeedback(Component.literal(
			String.format(Locale.ROOT, "skybend tps %.2f (manual)", ServerTps.tps())
		));
		return 1;
	}

	private static int[] resolveLookTarget(CommandContext<FabricClientCommandSource> ctx) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			ctx.getSource().sendError(Component.literal("No local player"));
			return null;
		}

		HitResult hit = player.pick(LOOK_TARGET_RANGE, 1.0f, false);
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			ctx.getSource().sendError(Component.literal(
				"No block along look ray (within " + (int) LOOK_TARGET_RANGE + " blocks) — aim at terrain or pass tx tz"
			));
			return null;
		}

		BlockPos pos = blockHit.getBlockPos();
		return new int[] {pos.getX(), pos.getZ()};
	}
}
