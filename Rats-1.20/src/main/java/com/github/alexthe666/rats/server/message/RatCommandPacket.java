package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RatCommandPacket(int ratId, int newCommand) implements CustomPacketPayload {

	public static final Type<RatCommandPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "rat_command"));
	public static final StreamCodec<FriendlyByteBuf, RatCommandPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, RatCommandPacket::ratId,
			ByteBufCodecs.VAR_INT, RatCommandPacket::newCommand,
			RatCommandPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static class Handler {
		// Handles a rat command change; takes the packet and network context, returns nothing.
		public static void handle(RatCommandPacket packet, IPayloadContext context) {
			context.enqueueWork(() -> {
				Player player = context.player();
				if (player != null) {
					Entity entity = player.level().getEntity(packet.ratId());
					if (entity instanceof TamedRat rat) {
						if (!canPlayerControlRat(player, rat, "command change")) {
							return;
						}
						rat.setCommandInteger(packet.newCommand());
					}
				}
			});
		}

		// Checks the owner before controlling a rat; takes the player, rat and action, returns true if allowed.
		private static boolean canPlayerControlRat(Player player, TamedRat rat, String action) {
			try {
				if (rat.getOwnerUUID() == null) {
					RatsMod.LOGGER.warn("Player {} tried to perform '{}' on rat {} that has no owner", player.getGameProfile().getName(), action, rat.getUUID());
					return false;
				}
				if (!rat.isOwnedBy(player)) {
					RatsMod.LOGGER.warn("Player {} tried to perform '{}' on someone else's rat {} owned by {}", player.getGameProfile().getName(), action, rat.getUUID(), rat.getOwnerUUID());
					return false;
				}
				return true;
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to check the owner of rat {} for player {}", rat.getUUID(), player.getGameProfile().getName(), exception);
				throw exception;
			}
		}
	}
}
