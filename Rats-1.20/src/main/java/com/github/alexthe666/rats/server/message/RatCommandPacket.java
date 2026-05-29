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
		// Обрабатывает смену команды крысы; принимает пакет и сетевой контекст, ничего не отдаёт.
		public static void handle(RatCommandPacket packet, IPayloadContext context) {
			context.enqueueWork(() -> {
				Player player = context.player();
				if (player != null) {
					Entity entity = player.level().getEntity(packet.ratId());
					if (entity instanceof TamedRat rat) {
						if (!canPlayerControlRat(player, rat, "смена команды")) {
							return;
						}
						rat.setCommandInteger(packet.newCommand());
					}
				}
			});
		}

		// Проверяет владельца перед управлением крысой; принимает игрока, крысу и действие, отдаёт true при разрешении.
		private static boolean canPlayerControlRat(Player player, TamedRat rat, String action) {
			try {
				if (rat.getOwnerUUID() == null) {
					RatsMod.LOGGER.warn("Игрок {} попытался выполнить '{}' для крысы {} без владельца", player.getGameProfile().getName(), action, rat.getUUID());
					return false;
				}
				if (!rat.isOwnedBy(player)) {
					RatsMod.LOGGER.warn("Игрок {} попытался выполнить '{}' для чужой крысы {} владельца {}", player.getGameProfile().getName(), action, rat.getUUID(), rat.getOwnerUUID());
					return false;
				}
				return true;
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось проверить владельца крысы {} для игрока {}", rat.getUUID(), player.getGameProfile().getName(), exception);
				throw exception;
			}
		}
	}
}
