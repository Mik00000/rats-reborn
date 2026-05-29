package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.projectile.ThrownBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncThrownBlockPacket(int blockEntityId, long blockPos) implements CustomPacketPayload {

	public static final Type<SyncThrownBlockPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "sync_thrown_block"));
	public static final StreamCodec<FriendlyByteBuf, SyncThrownBlockPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SyncThrownBlockPacket::blockEntityId,
			ByteBufCodecs.VAR_LONG, SyncThrownBlockPacket::blockPos,
			SyncThrownBlockPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static class Handler {
		public static void handle(SyncThrownBlockPacket packet, IPayloadContext context) {
			context.enqueueWork(() -> {
				Entity entity = context.player().level().getEntity(packet.blockEntityId());
				if (entity instanceof ThrownBlock blocc) {
					BlockPos pos = BlockPos.of(packet.blockPos());
					blocc.setHeldBlockState(context.player().level().getBlockState(pos));
				}
			});
		}
	}
}
