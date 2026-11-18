package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import java.util.ArrayList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DismountRatPacket(int ratId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DismountRatPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("rats", "dismount_rat"));
    public static final StreamCodec<FriendlyByteBuf, DismountRatPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, DismountRatPacket::ratId,
        DismountRatPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Handler {
        public static void handle(DismountRatPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                Player player = context.player();
                if (player == null) return;

                if (packet.ratId() <= 0) {
                    for (Entity passenger : new ArrayList<>(player.getPassengers())) {
                        if (passenger instanceof TamedRat rat) {
                            dismountRat(rat, player);
                        }
                    }
                } else {
                    Entity entity = player.level().getEntity(packet.ratId());
                    if (entity instanceof TamedRat rat) {
                        dismountRat(rat, player);
                    }
                }

                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(new ClientboundSetPassengersPacket(serverPlayer));
                }
            });
        }

        private static void dismountRat(TamedRat rat, Player player) {
            rat.stopRiding();
            Vec3 look = player.getLookAngle();
            Vec3 dismountPos = player.position().add(look.x * 0.5, 0.0, look.z * 0.5);
            rat.setPos(dismountPos.x(), dismountPos.y(), dismountPos.z());
            rat.setDeltaMovement(0, 0.1, 0);
        }
    }
}
