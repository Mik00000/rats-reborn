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
 * Регистрация и отправка сетевых пакетов Rats через CustomPacketPayload (NeoForge 1.21.1).
 */
public final class RatsNetworkHandler {

	public static final Channel CHANNEL = new Channel();

	private RatsNetworkHandler() {
	}

	// Регистрирует все play-пакеты мода; принимает событие NeoForge, ничего не возвращает.
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
			RatsMod.LOGGER.info("Сеть Rats: зарегистрировано 20 play-пакетов CustomPacketPayload");
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось зарегистрировать сетевые пакеты Rats", exception);
			throw exception;
		}
	}

	// Точка совместимости со старым init(); параметры не принимает, ничего не возвращает.
	public static void init() {
		RatsMod.LOGGER.debug("Сеть Rats инициализируется через RegisterPayloadHandlersEvent");
	}

	// Маркер цели «все игроки»; параметры не принимает, отдаёт маркер.
	public static Object allTarget() {
		return AllPlayersTarget.INSTANCE;
	}

	// Цель отправки одному игроку; принимает поставщик игрока, отдаёт игрока.
	public static ServerPlayer playerTarget(java.util.function.Supplier<ServerPlayer> player) {
		return player.get();
	}

	// Цель отправки игрокам, отслеживающим сущность; принимает поставщик сущности, отдаёт сущность.
	public static Entity trackingEntityTarget(java.util.function.Supplier<? extends Entity> entity) {
		return entity.get();
	}

	// Цель отправки игрокам, отслеживающим сущность, и самой сущности; принимает поставщик сущности, отдаёт маркер цели.
	public static EntityAndSelfTarget trackingEntityAndSelfTarget(java.util.function.Supplier<? extends Entity> entity) {
		return new EntityAndSelfTarget(entity.get());
	}

	// Цель отправки игрокам, отслеживающим чанк; принимает поставщик чанка, отдаёт чанк.
	public static LevelChunk trackingChunkTarget(java.util.function.Supplier<LevelChunk> chunk) {
		return chunk.get();
	}

	public static final class Channel {

		// Отправляет пакет на сервер с клиента; принимает полезную нагрузку, ничего не возвращает.
		public void sendToServer(CustomPacketPayload packet) {
			try {
				PacketDistributor.sendToServer(packet);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось отправить пакет {} на сервер", packet.type().id(), exception);
				throw exception;
			}
		}

		// Отправляет пакет выбранной цели с сервера; принимает цель и полезную нагрузку, ничего не возвращает.
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
					throw new IllegalArgumentException("Неизвестная цель отправки пакета: " + target.getClass().getName());
				}
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось отправить пакет {} цели {}", packet.type().id(), target, exception);
				throw exception;
			}
		}

		// Отправляет пакет отслеживающим сущность и самой сущности (если игрок); принимает сущность и пакет.
		public void sendToTrackingEntityAndSelf(Entity entity, CustomPacketPayload packet) {
			try {
				PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, packet);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось отправить пакет {} отслеживающим {}", packet.type().id(), entity, exception);
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
