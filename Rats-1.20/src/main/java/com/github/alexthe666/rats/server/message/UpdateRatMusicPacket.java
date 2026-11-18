package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateRatMusicPacket(int id, Item record) implements CustomPacketPayload {
   public static final Type<UpdateRatMusicPacket> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("rats", "update_rat_music"));
   public static final StreamCodec<RegistryFriendlyByteBuf, UpdateRatMusicPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, UpdateRatMusicPacket::id, ByteBufCodecs.registry(Registries.ITEM), UpdateRatMusicPacket::record, UpdateRatMusicPacket::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static class Handler {
      public static void handle(UpdateRatMusicPacket packet, IPayloadContext context) {
         context.enqueueWork(() -> {
            if (context.player() != null && context.player().level() != null
                && context.player().level().getEntity(packet.id()) instanceof TamedRat rat) {
               com.github.alexthe666.rats.client.ClientPacketHandlers.playRatMusic(rat, packet.record());
            }
         });
      }
   }
}
