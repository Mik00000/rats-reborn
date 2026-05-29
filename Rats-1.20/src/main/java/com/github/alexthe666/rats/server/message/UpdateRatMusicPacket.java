package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.util.RatRecordSoundInstance;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateRatMusicPacket(int id, Item record) implements CustomPacketPayload {

	public static final Type<UpdateRatMusicPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "update_rat_music"));
	public static final StreamCodec<RegistryFriendlyByteBuf, UpdateRatMusicPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, UpdateRatMusicPacket::id,
			ByteBufCodecs.registry(Registries.ITEM), UpdateRatMusicPacket::record,
			UpdateRatMusicPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static class Handler {
		public static void handle(UpdateRatMusicPacket packet, IPayloadContext context) {
			context.enqueueWork(() -> {
				if (Minecraft.getInstance().level != null) {
					Entity entity = Minecraft.getInstance().level.getEntity(packet.id());
					if (entity instanceof TamedRat rat) {
						Minecraft.getInstance().getSoundManager().queueTickingSound(new RatRecordSoundInstance(rat, packet.record()));
						Minecraft.getInstance().gui.setNowPlaying(packet.record().getDescription());
					}
				}
			});
		}
	}
}
