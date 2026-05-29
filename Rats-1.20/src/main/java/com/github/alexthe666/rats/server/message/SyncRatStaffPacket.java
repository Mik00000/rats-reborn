package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.block.RatHouseBlock;
import com.github.alexthe666.rats.server.block.entity.RatHouseBlockEntity;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncRatStaffPacket(int entityId, long pos, int facing, int control, int extraData) implements CustomPacketPayload {

	public static final Type<SyncRatStaffPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "sync_rat_staff"));
	public static final StreamCodec<FriendlyByteBuf, SyncRatStaffPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SyncRatStaffPacket::entityId,
			ByteBufCodecs.VAR_LONG, SyncRatStaffPacket::pos,
			ByteBufCodecs.VAR_INT, SyncRatStaffPacket::facing,
			ByteBufCodecs.VAR_INT, SyncRatStaffPacket::control,
			ByteBufCodecs.VAR_INT, SyncRatStaffPacket::extraData,
			SyncRatStaffPacket::new
	);

	public SyncRatStaffPacket(int entityId, BlockPos pos, Direction facing, int control) {
		this(entityId, pos.asLong(), facing.ordinal(), control, 0);
	}

	public SyncRatStaffPacket(int entityId, BlockPos pos, Direction facing, int control, int extraData) {
		this(entityId, pos.asLong(), facing.ordinal(), control, extraData);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static class Handler {
		// Обрабатывает команду посоха крысы; принимает пакет и сетевой контекст, ничего не отдаёт.
		public static void handle(SyncRatStaffPacket packet, IPayloadContext context) {
			context.enqueueWork(() -> {
				Player player = context.player();
				if (player != null) {
					Entity e = player.level().getEntity(packet.entityId());
					if (e instanceof TamedRat rat) {
						if (!canPlayerControlRat(player, rat, packet.control())) {
							return;
						}
						Direction direction = readPacketDirection(packet);
						switch (packet.control()) {
							case 0 -> {
								rat.setDepositPos(GlobalPos.of(player.level().dimension(), BlockPos.of(packet.pos())));
								rat.depositFacing = direction;
							}
							case 1 -> {
								rat.setPickupPos(GlobalPos.of(player.level().dimension(), BlockPos.of(packet.pos())));
								rat.pickupFacing = direction;
							}
							case 2 -> markHomeOrHouse(player, rat, BlockPos.of(packet.pos()));
							case 3 -> clearHomeOrHouse(player, rat, BlockPos.of(packet.pos()));
							case 4 -> rat.setRadiusCenter(GlobalPos.of(player.level().dimension(), BlockPos.of(packet.pos())));
							case 5 -> rat.setRadius(Math.max(1, packet.extraData()));
							case 6 -> {
								rat.setRadiusCenter(null);
								rat.setRadius(RatConfig.defaultRatRadius);
							}
							case 7 -> {
								rat.setPickupPos(null);
								rat.setDepositPos(null);
							}
							case 8 -> {
								rat.setCreateWorkPos(GlobalPos.of(player.level().dimension(), BlockPos.of(packet.pos())));
								rat.createWorkFacing = direction;
							}
							case 9 -> rat.setCreateWorkPos(null);
							default -> RatsMod.LOGGER.warn("Игрок {} отправил неизвестное действие посоха {} для крысы {}", player.getGameProfile().getName(), packet.control(), rat.getUUID());
						}
					}
				}
			});
		}


		// Отмечает обычную домашнюю точку или привязывает rat_house; принимает игрока, крысу и позицию, ничего не отдаёт.
		private static void markHomeOrHouse(Player player, TamedRat rat, BlockPos pos) {
			try {
				GlobalPos target = GlobalPos.of(player.level().dimension(), pos);
				if (player.level().getBlockState(pos).getBlock() instanceof RatHouseBlock) {
					if (rat.getBoundHousePos().isPresent() && !rat.getBoundHousePos().get().equals(target)) {
						GlobalPos oldHouse = rat.getBoundHousePos().get();
						if (oldHouse.dimension().equals(player.level().dimension()) && player.level().getBlockState(oldHouse.pos()).getBlock() instanceof RatHouseBlock) {
							player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_HOUSE_BOUND_ELSEWHERE), true);
							RatsMod.LOGGER.warn("Игрок {} пытался перепривязать крысу {} с домика {} на {}", player.getGameProfile().getName(), rat.getUUID(), oldHouse.pos(), pos);
							return;
						}
					}
					BlockEntity blockEntity = player.level().getBlockEntity(pos);
					if (!(blockEntity instanceof RatHouseBlockEntity house)) {
						throw new IllegalStateException("У rat_house нет RatHouseBlockEntity в " + pos);
					}
					house.bindRat(rat, player);
					return;
				}
				rat.setWorkAnchorPoint(target);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось отметить домик или рабочую базу {} для крысы {} игроком {}", pos, rat.getUUID(), player.getGameProfile().getName(), exception);
				throw exception;
			}
		}


		// Сбрасывает рабочую базу или привязанный rat_house; принимает игрока, крысу и позицию экрана посоха, ничего не отдаёт.
		private static void clearHomeOrHouse(Player player, TamedRat rat, BlockPos pos) {
			try {
				if (!(player.level().getBlockState(pos).getBlock() instanceof RatHouseBlock)) {
					rat.setWorkAnchorPoint(null);
					return;
				}
				if (rat.getBoundHousePos().isEmpty()) {
					return;
				}
				GlobalPos housePos = rat.getBoundHousePos().get();
				if (!housePos.dimension().equals(player.level().dimension())) {
					rat.setBoundHousePos(null);
					return;
				}
				BlockEntity blockEntity = player.level().getBlockEntity(housePos.pos());
				if (blockEntity instanceof RatHouseBlockEntity house && house.clearBinding(rat, player)) {
					return;
				}
				rat.setBoundHousePos(null);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось сбросить домик или рабочую базу крысы {} игроком {}", rat.getUUID(), player.getGameProfile().getName(), exception);
				throw exception;
			}
		}

		// Проверяет владельца перед применением посоха; принимает игрока, крысу и код действия, отдаёт true при разрешении.
		private static boolean canPlayerControlRat(Player player, TamedRat rat, int control) {
			try {
				if (rat.getOwnerUUID() == null) {
					RatsMod.LOGGER.warn("Игрок {} попытался применить посох к крысе {} без владельца, действие {}", player.getGameProfile().getName(), rat.getUUID(), control);
					return false;
				}
				if (!rat.isOwnedBy(player)) {
					RatsMod.LOGGER.warn("Игрок {} попытался применить посох к чужой крысе {} владельца {}, действие {}", player.getGameProfile().getName(), rat.getUUID(), rat.getOwnerUUID(), control);
					return false;
				}
				return true;
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось проверить владельца крысы {} для посоха игрока {}", rat.getUUID(), player.getGameProfile().getName(), exception);
				throw exception;
			}
		}

		// Читает сторону из пакета; принимает пакет, отдаёт валидное направление или кидает явную ошибку.
		private static Direction readPacketDirection(SyncRatStaffPacket packet) {
			try {
				Direction[] directions = Direction.values();
				int facing = packet.facing();
				if (facing < 0 || facing >= directions.length) {
					throw new IllegalArgumentException("Недопустимая сторона в пакете посоха: " + facing);
				}
				return directions[facing];
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось прочитать сторону из пакета посоха для сущности {}", packet.entityId(), exception);
				throw exception;
			}
		}
	}
}
