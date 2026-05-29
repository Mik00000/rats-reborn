package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetRatNamePacket(int ratId, String name) implements CustomPacketPayload {
	public static final Type<SetRatNamePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "set_rat_name"));
	public static final StreamCodec<FriendlyByteBuf, SetRatNamePacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SetRatNamePacket::ratId,
			ByteBufCodecs.STRING_UTF8, SetRatNamePacket::name,
			SetRatNamePacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static class Handler {
		// Обрабатывает смену имени крысы; принимает пакет и сетевой контекст, ничего не отдаёт.
		public static void handle(SetRatNamePacket packet, IPayloadContext context) {
			context.enqueueWork(() -> {
				try {
					Player player = context.player();
					if (player == null) {
						throw new IllegalStateException("Игрок отсутствует в контексте смены имени крысы");
					}
					Entity entity = player.level().getEntity(packet.ratId());
					if (!(entity instanceof TamedRat rat)) {
						throw new IllegalArgumentException("Крыса для смены имени не найдена: id=" + packet.ratId());
					}
					if (!rat.isOwnedBy(player) && !player.isCreative()) {
						RatsMod.LOGGER.warn("Игрок {} попытался переименовать чужую крысу {}", player.getGameProfile().getName(), rat.getUUID());
						player.displayClientMessage(Component.literal("Нельзя переименовать чужую крысу"), true);
						return;
					}
					rat.setRatGivenName(packet.name());
					rat.setPersistenceRequired();
					player.displayClientMessage(Component.literal(packet.name().isBlank() ? "Имя крысы очищено" : "Имя крысы задано: " + packet.name().trim()), true);
				} catch (RuntimeException exception) {
					RatsMod.LOGGER.error("Не удалось обработать пакет имени крысы id={} name='{}'", packet.ratId(), packet.name(), exception);
					throw exception;
				}
			});
		}
	}
}
