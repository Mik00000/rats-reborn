package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateRatFluidPacket(int ratId, FluidStack fluid) implements CustomPacketPayload {

	public static final Type<UpdateRatFluidPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "update_rat_fluid"));
	public static final StreamCodec<RegistryFriendlyByteBuf, UpdateRatFluidPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, UpdateRatFluidPacket::ratId,
			RatsFluidStackCodecs.STREAM_CODEC, UpdateRatFluidPacket::fluid,
			UpdateRatFluidPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static class Handler {
		public static void handle(UpdateRatFluidPacket packet, IPayloadContext context) {
			context.enqueueWork(() -> {
				Entity entity = context.player().level().getEntity(packet.ratId());
				if (entity instanceof TamedRat rat) {
					rat.transportingFluid = packet.fluid();
				}
			});
		}
	}
}
