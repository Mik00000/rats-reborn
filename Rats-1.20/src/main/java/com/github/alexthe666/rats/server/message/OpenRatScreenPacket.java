package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.inventory.RatMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenRatScreenPacket(int containerId, int entityId) implements CustomPacketPayload {
   public static final Type<OpenRatScreenPacket> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("rats", "open_rat_screen"));
   public static final StreamCodec<FriendlyByteBuf, OpenRatScreenPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, OpenRatScreenPacket::containerId, ByteBufCodecs.VAR_INT, OpenRatScreenPacket::entityId, OpenRatScreenPacket::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static class Handler {
      public static void handle(OpenRatScreenPacket packet, IPayloadContext context) {
         context.enqueueWork(() -> {
            if (context.player() != null && context.player().level() != null
                && context.player().level().getEntity(packet.entityId()) instanceof TamedRat rat) {
               Player player = context.player();
               SimpleContainer container = new SimpleContainer(6);
               RatMenu menu = new RatMenu(packet.containerId(), container, player.getInventory());
               player.containerMenu = menu;
               com.github.alexthe666.rats.client.ClientPacketHandlers.openRatScreen(menu, player.getInventory(), rat);
            }
         });
      }
   }
}
