package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.RatsMod;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registration and sending of Rats network packets via CustomPacketPayload (NeoForge 1.21.1).
 */
public final class RatsNetworkHandler {

	public static final Channel CHANNEL = new Channel();

	private RatsNetworkHandler() {
	}

	// Registers all play packets of the mod; takes the NeoForge event, returns nothing.
	public static void registerPayloads(RegisterPayloadHandlersEvent event) {
		try {
			PayloadRegistrar registrar = event.registrar("1");
			registrar.playToClient(ChangeRatlantisStatusPacket.TYPE, ChangeRatlantisStatusPacket.STREAM_CODEC, ChangeRatlantisStatusPacket.Handler::handle);
			registrar.playToServer(ClearRatRecipePacket.TYPE, ClearRatRecipePacket.STREAM_CODEC, ClearRatRecipePacket.Handler::handle);
			registrar.playToServer(CycleRatRecipePacket.TYPE, CycleRatRecipePacket.STREAM_CODEC, CycleRatRecipePacket.Handler::handle);
			registrar.playToServer(DismountRatPacket.TYPE, DismountRatPacket.STREAM_CODEC, DismountRatPacket.Handler::handle);
			registrar.playToClient(ManageRatStaffPacket.TYPE, ManageRatStaffPacket.STREAM_CODEC, ManageRatStaffPacket.Handler::handle);
			registrar.playToClient(OpenRatScreenPacket.TYPE, OpenRatScreenPacket.STREAM_CODEC, OpenRatScreenPacket.Handler::handle);
			registrar.playToServer(RatCommandPacket.TYPE, RatCommandPacket.STREAM_CODEC, RatCommandPacket.Handler::handle);
			registrar.playToServer(RatUpgradeVisibilityPacket.TYPE, RatUpgradeVisibilityPacket.STREAM_CODEC, RatUpgradeVisibilityPacket.Handler::handle);
			registrar.playToServer(SetDancingRatPacket.TYPE, SetDancingRatPacket.STREAM_CODEC, SetDancingRatPacket.Handler::handle);
			registrar.playToServer(SetRatNamePacket.TYPE, SetRatNamePacket.STREAM_CODEC, SetRatNamePacket.Handler::handle);
			registrar.playToServer(SyncArmSwingPacket.TYPE, SyncArmSwingPacket.STREAM_CODEC, SyncArmSwingPacket.Handler::handle);
			registrar.playToClient(SyncPlaguePacket.TYPE, SyncPlaguePacket.STREAM_CODEC, SyncPlaguePacket.Handler::handle);
			registrar.playToServer(SyncRatStaffPacket.TYPE, SyncRatStaffPacket.STREAM_CODEC, SyncRatStaffPacket.Handler::handle);
			registrar.playToServer(SyncRatTagPacket.TYPE, SyncRatTagPacket.STREAM_CODEC, SyncRatTagPacket.Handler::handle);
			registrar.playToClient(SyncThrownBlockPacket.TYPE, SyncThrownBlockPacket.STREAM_CODEC, SyncThrownBlockPacket.Handler::handle);
			registrar.playToClient(UpdateCurdlerFluidPacket.TYPE, UpdateCurdlerFluidPacket.STREAM_CODEC, UpdateCurdlerFluidPacket.Handler::handle);
			registrar.playToServer(UpdateMobFilterPacket.TYPE, UpdateMobFilterPacket.STREAM_CODEC, UpdateMobFilterPacket.Handler::handle);
			registrar.playToClient(UpdateRatFluidPacket.TYPE, UpdateRatFluidPacket.STREAM_CODEC, UpdateRatFluidPacket.Handler::handle);
			registrar.playToClient(UpdateRatMusicPacket.TYPE, UpdateRatMusicPacket.STREAM_CODEC, UpdateRatMusicPacket.Handler::handle);
			registrar.playToClient(UpdateSelectedRatPacket.TYPE, UpdateSelectedRatPacket.STREAM_CODEC, UpdateSelectedRatPacket.Handler::handle);
			RatsMod.LOGGER.info("Rats network: registered 20 CustomPacketPayload play packets");
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to register the Rats network packets", exception);
			throw exception;
		}
	}

	// Compatibility entry point for the old init(); takes no parameters, returns nothing.
	public static void init() {
		RatsMod.LOGGER.debug("The Rats network is initialized via RegisterPayloadHandlersEvent");
	}

	// The "all players" target marker; takes no parameters, returns the marker.
	public static Object allTarget() {
		return AllPlayersTarget.INSTANCE;
	}

	// Send target: a single player; takes a player supplier, returns the player.
	public static ServerPlayer playerTarget(java.util.function.Supplier<ServerPlayer> player) {
		return player.get();
	}

	// Send target: players tracking an entity; takes an entity supplier, returns the entity.
	public static Entity trackingEntityTarget(java.util.function.Supplier<? extends Entity> entity) {
		return entity.get();
	}

	// Send target: players tracking an entity and the entity itself; takes an entity supplier, returns the target marker.
	public static EntityAndSelfTarget trackingEntityAndSelfTarget(java.util.function.Supplier<? extends Entity> entity) {
		return new EntityAndSelfTarget(entity.get());
	}

	// Send target: players tracking a chunk; takes a chunk supplier, returns the chunk.
	public static LevelChunk trackingChunkTarget(java.util.function.Supplier<LevelChunk> chunk) {
		return chunk.get();
	}

	public static final class Channel {

		// Sends a packet from the client to the server; takes the payload, returns nothing.
		public void sendToServer(CustomPacketPayload packet) {
			try {
				PacketDistributor.sendToServer(packet);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to send packet {} to the server", packet.type().id(), exception);
				throw exception;
			}
		}

		// Sends a packet from the server to the chosen target; takes the target and payload, returns nothing.
		public void send(Object target, CustomPacketPayload packet) {
			try {
				if (target instanceof AllPlayersTarget) {
					PacketDistributor.sendToAllPlayers(packet);
				} else if (target instanceof ServerPlayer player) {
					PacketDistributor.sendToPlayer(player, packet);
				} else if (target instanceof EntityAndSelfTarget wrapper) {
					PacketDistributor.sendToPlayersTrackingEntityAndSelf(wrapper.entity(), packet);
				} else if (target instanceof Entity entity) {
					PacketDistributor.sendToPlayersTrackingEntity(entity, packet);
				} else if (target instanceof LevelChunk chunk && chunk.getLevel() instanceof ServerLevel serverLevel) {
					PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunk.getPos(), packet);
				} else {
					throw new IllegalArgumentException("Unknown packet send target: " + target.getClass().getName());
				}
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to send packet {} to target {}", packet.type().id(), target, exception);
				throw exception;
			}
		}

		// Sends a packet to players tracking an entity and to the entity itself (if it is a player); takes the entity and packet.
		public void sendToTrackingEntityAndSelf(Entity entity, CustomPacketPayload packet) {
			try {
				PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, packet);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to send packet {} to players tracking {}", packet.type().id(), entity, exception);
				throw exception;
			}
		}
	}

	private enum AllPlayersTarget {
		INSTANCE
	}

	public record EntityAndSelfTarget(Entity entity) {
	}
}
