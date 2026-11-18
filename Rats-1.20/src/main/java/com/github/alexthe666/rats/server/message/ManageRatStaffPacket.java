package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.registry.RatsCapabilityRegistry;
import com.github.alexthe666.rats.server.capability.SelectedRat;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ManageRatStaffPacket(int entityId, BlockPos pos, int dirOrd, boolean clear, boolean openGUI, int staffToOpen) implements CustomPacketPayload {
   public static final Type<ManageRatStaffPacket> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("rats", "manage_rat_staff"));
   public static final StreamCodec<FriendlyByteBuf, ManageRatStaffPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ManageRatStaffPacket::entityId,
      BlockPos.STREAM_CODEC,
      ManageRatStaffPacket::pos,
      ByteBufCodecs.VAR_INT,
      ManageRatStaffPacket::dirOrd,
      ByteBufCodecs.BOOL,
      ManageRatStaffPacket::clear,
      ByteBufCodecs.BOOL,
      ManageRatStaffPacket::openGUI,
      ByteBufCodecs.VAR_INT,
      ManageRatStaffPacket::staffToOpen,
      ManageRatStaffPacket::new
   );

   public ManageRatStaffPacket(int entityId, BlockPos pos, int dirOrd, boolean clear, boolean openGUI) {
      this(entityId, pos, dirOrd, clear, openGUI, 0);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static class Handler {
      public static void handle(ManageRatStaffPacket packet, IPayloadContext context) {
         context.enqueueWork(() -> {
            if (packet.clear()) {
               if (context.player() != null) {
                  SelectedRat selectedRat = (SelectedRat)context.player().getCapability(RatsCapabilityRegistry.SELECTED_RAT);
                  if (selectedRat != null) {
                     selectedRat.clearSelectedRat();
                  }
               }
            } else if (context.player() != null && context.player().level() != null
                       && context.player().level().getEntity(packet.entityId()) instanceof TamedRat rat && packet.openGUI()) {
               com.github.alexthe666.rats.client.ClientPacketHandlers.openStaffScreen(packet.staffToOpen(), rat, packet.pos(), packet.dirOrd());
            }
         });
      }
   }
}
