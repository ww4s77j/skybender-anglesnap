package com.skybender.anglesnap.client;

import com.skybender.anglesnap.SkybenderAnglesnap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

public class SkybenderAnglesnapClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SkybendCommands.register();
		// The dial is one extra HUD layer; it draws by default and /skybend overlay off hides it.
		HudElementRegistry.addLast(
			Identifier.fromNamespaceAndPath(SkybenderAnglesnap.MOD_ID, "dial"),
			new DialHudElement()
		);

		// Tick-rate samples and any in-flight schedule belong to one server session only.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			ServerTps.onWorldChanged();
			DialGauge.reset();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ServerTps.onWorldChanged();
			SequencePlayer.cancel();
			EtaCountdown.cancel();
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			SequencePlayer.onClientTick(client);
			EtaCountdown.onClientTick(client);
		});
		SkybenderAnglesnap.LOGGER.info("Skybender AngleSnap client ready (/skybend set|tps|overlay|time|fire)");
	}
}
