package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetRatNamePacket(int ratId, String name) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetRatNamePacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("rats", "set_rat_name"));
    public static final StreamCodec<FriendlyByteBuf, SetRatNamePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, SetRatNamePacket::ratId,
        ByteBufCodecs.STRING_UTF8, SetRatNamePacket::name,
        SetRatNamePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Handler {
        public static void handle(SetRatNamePacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                try {
                    Player player = context.player();
                    if (player == null) {
                        throw new IllegalStateException("Player absent from rat rename context");
                    }
                    Entity entity = player.level().getEntity(packet.ratId());
                    if (!(entity instanceof TamedRat)) {
                        throw new IllegalArgumentException("Rat to rename not found: id=" + packet.ratId());
                    }
                    TamedRat rat = (TamedRat) entity;
                    if (!rat.isOwnedBy(player) && !player.isCreative()) {
                        RatsMod.LOGGER.warn("Player {} attempted to rename another player's rat {}", player.getGameProfile().getName(), rat.getUUID());
                        player.displayClientMessage(Component.literal("Cannot rename another player's rat"), true);
                        return;
                    }
                    rat.setRatGivenName(packet.name());
                    rat.setPersistenceRequired();
                    player.displayClientMessage(Component.literal(packet.name().isBlank() ? "Rat name cleared" : "Rat name set to: " + packet.name().trim()), true);
                } catch (RuntimeException exception) {
                    RatsMod.LOGGER.error("Failed to process rat rename packet id={} name='{}'", packet.ratId(), packet.name(), exception);
                    throw exception;
                }
            });
        }
    }
}
