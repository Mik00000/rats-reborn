package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record SyncRatTagPacket(int ratId, List<GlobalPos> nodes) implements CustomPacketPayload {

	public static final Type<SyncRatTagPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "sync_rat_tag"));
	public static final StreamCodec<FriendlyByteBuf, SyncRatTagPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SyncRatTagPacket::ratId,
			GlobalPos.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncRatTagPacket::nodes,
			SyncRatTagPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static class Handler {
		public static void handle(SyncRatTagPacket packet, IPayloadContext context) {
			context.enqueueWork(() -> {
				Player player = context.player();
				if (player != null) {
					Entity entity = player.level().getEntity(packet.ratId());
					if (entity instanceof TamedRat rat && rat.isOwnedBy(player)) {
						rat.setPatrolNodes(packet.nodes());
						RatsMod.LOGGER.info("Маршрут патруля крысы {} обновлён: {} точек", rat.getUUID(), packet.nodes().size());
					} else if (entity instanceof TamedRat rat) {
						RatsMod.LOGGER.error("Отклонена смена маршрута патруля: игрок {} не владелец крысы {}", player.getGameProfile().getName(), rat.getUUID());
					}
				}
			});
		}
	}
}
