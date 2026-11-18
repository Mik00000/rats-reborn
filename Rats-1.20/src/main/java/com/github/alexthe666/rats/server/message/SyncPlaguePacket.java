package com.github.alexthe666.rats.server.message;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncPlaguePacket(int entityId, byte effectId, byte amplifier, int duration, byte flags) implements CustomPacketPayload {
   private static final int FLAG_AMBIENT = 1;
   private static final int FLAG_VISIBLE = 2;
   private static final int FLAG_SHOW_ICON = 4;
   public static final Type<SyncPlaguePacket> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("rats", "sync_plague"));
   public static final StreamCodec<FriendlyByteBuf, SyncPlaguePacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      SyncPlaguePacket::entityId,
      ByteBufCodecs.BYTE,
      SyncPlaguePacket::effectId,
      ByteBufCodecs.BYTE,
      SyncPlaguePacket::amplifier,
      ByteBufCodecs.VAR_INT,
      SyncPlaguePacket::duration,
      ByteBufCodecs.BYTE,
      SyncPlaguePacket::flags,
      SyncPlaguePacket::new
   );

   public SyncPlaguePacket(int id, MobEffectInstance effect) {
      this(
         id,
         (byte)(BuiltInRegistries.MOB_EFFECT.getId((MobEffect)effect.getEffect().value()) & 0xFF),
         (byte)(effect.getAmplifier() & 0xFF),
         Math.min(effect.getDuration(), 32767),
         getFlags(effect)
      );
   }

   private static byte getFlags(MobEffectInstance mobEffectInstance) {
      byte flags = 0;
      if (mobEffectInstance.isAmbient()) {
         flags = (byte)(flags | 1);
      }

      if (mobEffectInstance.isVisible()) {
         flags = (byte)(flags | 2);
      }

      if (mobEffectInstance.showIcon()) {
         flags = (byte)(flags | 4);
      }

      return flags;
   }

   public boolean isEffectVisible() {
      return (this.flags() & 2) == 2;
   }

   public boolean isEffectAmbient() {
      return (this.flags() & 1) == 1;
   }

   public boolean effectShowsIcon() {
      return (this.flags() & 4) == 4;
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static class Handler {
      public static void handle(SyncPlaguePacket packet, IPayloadContext context) {
         context.enqueueWork(
            () -> {
               if (context.player() != null && context.player().level() != null) {
                  if (context.player().level().getEntity(packet.entityId()) instanceof LivingEntity living) {
                     Holder<MobEffect> mobeffect = (Holder<MobEffect>)BuiltInRegistries.MOB_EFFECT.getHolder(packet.effectId() & 255).orElse(null);
                     if (mobeffect != null) {
                        if (packet.duration() == 0) {
                           living.removeEffect(mobeffect);
                        } else {
                           MobEffectInstance mobeffectinstance = new MobEffectInstance(
                              mobeffect, packet.duration(), packet.amplifier(), packet.isEffectAmbient(), packet.isEffectVisible(), packet.effectShowsIcon()
                           );
                           living.forceAddEffect(mobeffectinstance, null);
                        }
                     }
                  }
               }
            }
         );
      }
   }
}
