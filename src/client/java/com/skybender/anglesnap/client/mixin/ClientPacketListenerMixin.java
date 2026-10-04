package com.skybender.anglesnap.client.mixin;

import com.skybender.anglesnap.client.ServerTps;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feeds the authoritative server game time into the tick-rate model.
 *
 * <p>A vanilla server broadcasts {@link ClientboundSetTimePacket} once every 20 of its own ticks, so
 * the game time it carries is the only trustworthy view a client has of how fast the server is
 * actually running. Fabric's play-networking receivers cannot be used for this: on 26.x they only
 * accept {@code CustomPacketPayload} types, never vanilla packets.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	@Inject(method = "handleSetTime", at = @At("HEAD"))
	private void skybend$onSetTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
		ServerTps.onTimePacket(packet.gameTime());
	}
}
